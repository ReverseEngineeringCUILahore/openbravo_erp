/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.1  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html 
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License. 
 * The Original Code is Openbravo ERP. 
 * The Initial Developer of the Original Code is Openbravo SLU 
 * All portions are Copyright (C) 2008-2014 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.test.xml;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.Ignore;
import org.junit.Test;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.businesspartner.Greeting;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.model.common.geography.Region;
import org.openbravo.service.db.DataImportService;
import org.openbravo.service.db.ImportResult;

/**
 * Test if warnings are generated by the xml import.
 * 
 * @author mtaal
 */

public class EntityXMLImportTestWarning extends XMLBaseTest {

  /**
   * Test that a warning is given that an object (in this case {@link Greeting} is not writable
   * because of access definitions for the user.
   */
  @Test
  public void testNotWritableUpdate() {
    cleanRefDataLoaded();
    setTestUserContext();
    addReadWriteAccess(Region.class);

    final List<Region> gs = getList(Region.class);
    String xml = getXML(gs);

    // change the xml to force an update
    xml = xml.replaceAll("</name>", "t</name>");

    final Client c = OBDal.getInstance().get(Client.class, TEST_CLIENT_ID);
    final Organization o = OBDal.getInstance().get(Organization.class, TEST_ORG_ID);
    setTestUserContext();
    OBContext.getOBContext().setCurrentOrganization(o);
    OBContext.getOBContext().setCurrentClient(c);

    // remove the entity to force a not-write situation
    final Entity entity = ModelProvider.getInstance().getEntity(Region.class);
    OBContext.getOBContext().getEntityAccessChecker().getWritableEntities().remove(entity);
    OBContext.getOBContext().getEntityAccessChecker().getReadableEntities().add(entity);

    final ImportResult ir = DataImportService.getInstance().importDataFromXML(c, o, xml);
    if (ir.getException() != null) {
      ir.getException().printStackTrace(System.err);
      fail(ir.getException().getMessage());
    } else if (ir.getErrorMessages() != null) {
      fail(ir.getErrorMessages());
    } else {
      // assertEquals(0, ir.getUpdatedObjects().size());
      // assertEquals(0, ir.getUpdatedObjects().size());
      assertTrue(ir.getWarningMessages() != null);
      assertTrue(ir.getWarningMessages().indexOf("updating") != -1);
      assertTrue(ir.getWarningMessages().indexOf(" because it is not writable") != -1);
    }
    // force a rollback, so that the db is not changed
    rollback();
  }

  /**
   * Tests that an error message is given that an object is new but the user is not allowed to write
   * it (because of access definitions).
   */
  @Test
  public void testNotWritableInsertError() {
    cleanRefDataLoaded();
    setTestUserContext();
    addReadWriteAccess(Warehouse.class);

    final List<Warehouse> ws = getList(Warehouse.class);
    String xml = getXML(ws);

    // change the xml to force an update
    xml = xml.replaceAll("</name>", "t</name>");
    xml = xml.replaceAll("</id>", "new</id>");
    final Client c = OBDal.getInstance().get(Client.class, QA_TEST_CLIENT_ID);
    final Organization o = OBDal.getInstance().get(Organization.class, QA_TEST_ORG_ID);
    setUserContext(QA_TEST_ADMIN_USER_ID);

    // remove the entity from the writable entities to force an access error
    final Entity entity = ModelProvider.getInstance().getEntity(Warehouse.class);
    OBContext.getOBContext().getEntityAccessChecker().getWritableEntities().remove(entity);

    final ImportResult ir = DataImportService.getInstance().importDataFromXML(c, o, xml);
    assertTrue("No error messages, error messages expected", ir.getErrorMessages() != null);
    assertTrue("Incorrect error", ir.getErrorMessages().indexOf("Object Warehouse") != -1);
    assertTrue("Incorrect error", ir.getErrorMessages().indexOf("is new but not writable") != -1);
    // force a rollback, so that the db is not changed
    rollback();
  }

  /**
   * Tests that a warning is given that during an import, an update occured in another organization
   * then the one passed in during the import. This can happen if an object belongs in organization
   * * (0), while the update/import is in another organization.
   */
  @Ignore("This test is currently disabled because it didn't work with the new Openbravo demo data. More info: https://issues.openbravo.com/view.php?id=20264")
  @Test
  public void testUpdatingOtherOrganizationWarning() {
    cleanRefDataLoaded();
    setTestUserContext();
    addReadWriteAccess(Warehouse.class);

    final List<Warehouse> ws = getList(Warehouse.class);
    String xml = getXML(ws);

    // change the xml to force an update
    xml = xml.replaceAll("</name>", "t</name>");
    xml = xml.replaceAll("</id>", "new</id>");
    setSystemAdministratorContext();
    final ImportResult ir = DataImportService.getInstance()
        .importDataFromXML(OBDal.getInstance().get(Client.class, TEST_CLIENT_ID),
            OBDal.getInstance().get(Organization.class, "B9C7088AB859483A9B1FB342AC2BE17A"), xml); // FIXME
    if (ir.getException() != null) {
      ir.getException().printStackTrace(System.err);
      fail(ir.getException().getMessage());
    } else if (ir.getErrorMessages() != null) {
      fail(ir.getErrorMessages());
    } else {
      assertTrue(ir.getWarningMessages() != null);
      assertTrue(ir.getWarningMessages().indexOf("Updating entity") != -1);
      assertTrue(ir.getWarningMessages()
          .indexOf("eventhough it does not belong to the target organization ") != -1);
    }
    // force a rollback, so that the db is not changed
    rollback();
  }

  // // works also but disabled for now
  // public void _testUpdateOtherOrganizationWarning() {
  // cleanRefDataLoaded();
  // setBigBazaarUserContext();
  // addReadWriteAccess(Location.class);
  //
  // final List<Location> cs = getList(Location.class);
  // String xml = getXML(cs);
  //
  // // change the xml to force an update
  // xml = xml.replaceAll("</cityName>", "t</cityName>");
  //
  // // the following should result in creation of location
  // // xml = xml.replaceAll("location id=\"", "location id=\"new");
  // // xml = xml.replaceAll("CoreLocation id=\"", "CoreLocation id=\"new");
  // setSystemAdministratorContext();
  // final ImportResult ir = DataImportService.getInstance().importDataFromXML(
  // OBDal.getInstance().get(Client.class, "1000000"),
  // OBDal.getInstance().get(Organization.class, "1000001"), xml);
  // if (ir.getException() != null) {
  // ir.getException().printStackTrace(System.err);
  // fail(ir.getException().getMessage());
  // } else if (ir.getErrorMessages() != null) {
  // fail(ir.getErrorMessages());
  // } else {
  // assertTrue(ir.getWarningMessages() != null);
  // assertTrue(ir.getWarningMessages().indexOf("Updating entity") != -1);
  // assertTrue(ir.getWarningMessages().indexOf(
  // "eventhough it does not belong to the target organization") != -1);
  // }
  // // force a rollback, so that the db is not changed
  // rollback();
  // }

  // public void testInsertOtherClientWarning() {
  // cleanRefDataLoaded();
  // setSystemAdministratorContext();
  //
  // final List<Region> cs = getList(Region.class);
  // String xml = getXML(cs);
  //
  // // clears the session and ensures that it starts with a new one
  // SessionHandler.getInstance().rollback();
  //
  // // the following should result in creation of location
  // xml = xml.replaceAll("<name>", "<name>new");
  // xml = xml.replaceAll("region id=\"", "region id=\"new");
  // xml = xml.replaceAll("Region id=\"", "Region id=\"new");
  // setSystemAdministratorContext();
  // final ImportResult ir = DataImportService.getInstance().importDataFromXML(
  // OBDal.getInstance().get(Client.class, "1000000"),
  // OBDal.getInstance().get(Organization.class, "1000001"), xml);
  // if (ir.getException() != null) {
  // ir.getException().printStackTrace(System.err);
  // fail(ir.getException().getMessage());
  // } else {
  // assertTrue(ir.getWarningMessages() != null);
  // assertTrue(ir.getWarningMessages().indexOf("Creating entity ") != -1);
  // assertTrue(ir.getWarningMessages().indexOf(
  // "eventhough it does not belong to the target organization") != -1);
  // }
  // // force a rollback, so that the db is not changed
  // rollback();
  // }

}
