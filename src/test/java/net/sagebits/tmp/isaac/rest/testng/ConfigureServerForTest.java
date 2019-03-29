/*
 * Copyright 2018 VetsEZ Inc, Sagebits LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.sagebits.tmp.isaac.rest.testng;

import static sh.isaac.api.constants.SystemPropertyConstants.DATA_STORE_ROOT_LOCATION_PROPERTY;
import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Paths;
import java.util.concurrent.ExecutionException;
import javax.ws.rs.core.Application;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTestNg;
import org.testng.Assert;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import net.sagebits.tmp.isaac.rest.ApplicationConfig;
import net.sagebits.tmp.isaac.rest.LocalGrizzlyRunner;
import net.sagebits.uts.auth.data.User;
import net.sagebits.uts.auth.data.UserRole;
import net.sagebits.uts.auth.users.UserService;
import sh.isaac.MetaData;
import sh.isaac.api.Get;
import sh.isaac.api.LookupService;
import sh.isaac.api.Status;
import sh.isaac.api.bootstrap.TermAux;
import sh.isaac.api.commit.CommitService;
import sh.isaac.api.externalizable.BinaryDataReaderService;
import sh.isaac.api.index.IndexBuilderService;
import sh.isaac.api.util.RecursiveDelete;
import sh.isaac.api.util.UuidT5Generator;
import sh.isaac.convert.directUtils.DirectWriteHelper;
import sh.isaac.converters.sharedUtils.stats.ConverterUUID;
import sh.isaac.misc.constants.VHATConstants;
import sh.isaac.misc.modules.vhat.VHATIsAHasParentSynchronizingChronologyChangeListener;

/**
 * {@link ConfigureServerForTest}
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a> 
 */
@Test(suiteName="testSuite", groups="first")
public class ConfigureServerForTest extends JerseyTestNg.ContainerPerClassTest
{
	private static Logger log = LogManager.getLogger(ReadOnlyRestTest.class);
	
	File userStoreFile;

	@Override
	protected Application configure()
	{
		try
		{
			System.out.println("Launching Jersey within Grizzley for tests");
			File file = new File("target/test.data");
			RecursiveDelete.delete(file);
			file.mkdirs();
			System.setProperty(DATA_STORE_ROOT_LOCATION_PROPERTY, "target/test.data");
			ResourceConfig rc = LocalGrizzlyRunner.configureJerseyServer();
			rc.setApplicationName("testing1234");
			return rc;
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}
	
	@BeforeClass
	public void testDataLoad() throws Exception
	{
		// Load in the test data
		try
		{
			while (!ApplicationConfig.getInstance().isIsaacReady())
			{
				Thread.sleep(50);
			}

			userStoreFile = new File(new File(System.getProperty("java.io.tmpdir")), "rest-test-users.json");
			
			//Create a user service, make sure these users are in it, then toss it.  They will get read again when the RestUserServiceLocal starts
			UserService us = new UserService(userStoreFile, false);
			User u = new User(UuidT5Generator.get("readOnly"), "readOnly", "read only", new UserRole[] {UserRole.READ}, null);
			u.setPassword("readOnly".toCharArray());
			us.addOrUpdate(u);
			u = new User(UuidT5Generator.get("admin"), "admin", "admin", new UserRole[] {UserRole.READ, UserRole.ADMINISTRATOR}, null);
			u.setPassword("admin".toCharArray());
			us.addOrUpdate(u);
			us = null;
	
			BinaryDataReaderService reader = Get.binaryDataReader(Paths.get("target", "data", "IsaacMetadataAuxiliary.ibdf"));
			CommitService commitService = Get.commitService();
			reader.getStream().forEach((object) -> {
				commitService.importNoChecks(object);
			});
			commitService.postProcessImportNoChecks();

			Get.startIndexTask((Class<IndexBuilderService>[]) null).get();

			createVHATHasParentAssociation();
			BaseTestCode.configure(this);
		}
		catch (FileNotFoundException | InterruptedException | ExecutionException e)
		{
			Assert.fail("Test data file not found", e);
		}
		Assert.assertEquals(Get.conceptDescriptionText(MetaData.ASSEMBLAGE____SOLOR.getNid()), "Assemblage (SOLOR)");
	}
	
	// VHAT-specific metadata
	private void createVHATHasParentAssociation() throws Exception
	{
		ConverterUUID converterUUID = new ConverterUUID(UuidT5Generator.PATH_ID_FROM_FS_DESC, false);
		
		DirectWriteHelper dwh = new DirectWriteHelper(TermAux.USER.getNid(), MetaData.VHAT_MODULES____SOLOR.getNid(), 
				MetaData.DEVELOPMENT_PATH____SOLOR.getNid(), converterUUID, "VHAT", false);

		dwh.makeConceptEnNoDialect(VHATConstants.VHAT_HAS_PARENT_ASSOCIATION_TYPE.getPrimordialUuid(), "has_parent", 
				MetaData.REGULAR_NAME_DESCRIPTION_TYPE____SOLOR.getPrimordialUuid(), null, Status.ACTIVE, System.currentTimeMillis());
		dwh.configureConceptAsAssociation(VHATConstants.VHAT_HAS_PARENT_ASSOCIATION_TYPE.getPrimordialUuid(), "something", null, 
				null, null, System.currentTimeMillis());
		LookupService.get().getService(VHATIsAHasParentSynchronizingChronologyChangeListener.class).enable();
	}

	@Override
	public void tearDown() throws Exception
	{
		// do not shut down at this point, we need it up for the rest of the test cases.
		//Will have a 'end test' which actually does the shutdown.
	}
	
	@Test(priority=Integer.MAX_VALUE)
	@AfterSuite
	public void realShutDown() throws Exception
	{
		log.info("realShutDown executing, which will stop isaac");
		super.tearDown();
		userStoreFile.delete();
		new File(userStoreFile.getAbsolutePath() + ".bak").delete();
		new File(new File(System.getProperty("java.io.tmpdir")), "rest-test-tokenSecret").delete();
	}
}
