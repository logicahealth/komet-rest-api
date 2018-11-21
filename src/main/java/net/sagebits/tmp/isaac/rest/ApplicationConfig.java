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
 * 
 * Contributions from 2015-2017 where performed either by US government
 * employees, or under US Veterans Health Administration contracts.
 *
 * US Veterans Health Administration contributions by government employees
 * are work of the U.S. Government and are not subject to copyright
 * protection in the United States. Portions contributed by government
 * employees are USGovWork (17USC §105). Not subject to copyright.
 * 
 * Contribution by contractors to the US Veterans Health Administration
 * during this period are contractually contributed under the
 * Apache License, Version 2.0.
 *
 * See: https://www.usa.gov/government-works
 */
package net.sagebits.tmp.isaac.rest;

import static sh.isaac.api.constants.SystemPropertyConstants.DATA_STORE_ROOT_LOCATION_PROPERTY;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.servlet.ServletContext;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerLifecycleListener;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Task;
import net.sagebits.tmp.isaac.rest.api1.RestPaths;
import net.sagebits.tmp.isaac.rest.api1.data.RestSystemInfo;
import net.sagebits.tmp.isaac.rest.session.RestConfig;
import sh.isaac.MetaData;
import sh.isaac.api.ConfigurationService;
import sh.isaac.api.Get;
import sh.isaac.api.LookupService;
import sh.isaac.api.RemoteServiceInfo;
import sh.isaac.api.chronicle.Version;
import sh.isaac.api.component.semantic.SemanticChronology;
import sh.isaac.api.component.semantic.version.DescriptionVersion;
import sh.isaac.api.constants.DatabaseImplementation;
import sh.isaac.api.constants.DatabaseInitialization;
import sh.isaac.api.constants.SystemPropertyConstants;
import sh.isaac.api.util.DownloadUnzipTask;
import sh.isaac.api.util.RecursiveDelete;
import sh.isaac.api.util.WorkExecutors;
import sh.isaac.convert.mojo.turtle.TurtleImportMojoDirect;
import sh.isaac.dbConfigBuilder.artifacts.MavenArtifactUtils;
import sh.isaac.metadata.source.IsaacMetadataAuxiliary;

@ApplicationPath(RestPaths.appPathComponent)
public class ApplicationConfig extends ResourceConfig implements ContainerLifecycleListener
{
	private static final AtomicInteger startup = new AtomicInteger(1);
	private Logger log = LogManager.getLogger();

	private static ApplicationConfig instance_;

	private StringProperty status_ = new SimpleStringProperty("Not Started");
	private boolean debugMode = true;
	private boolean shutdown = false;

	// Note - this injection works fine, when deployed as a war to tomcat. However, when launched in the localGrizzleyRunner from eclipse,
	// this remains null.
	@Context
	ServletContext context_;

	private String contextPath;

	private static byte[] secret_;

	private RestSystemInfo systemInfo_;

	private File dbLocation;
	private static final DateTimeFormatter fileDateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

	// TODO we need to deal with contradictions properly whenever we pull things from a LatestVersion object. See code in RestConceptChonology
	// for extracting the latest description.

	public ApplicationConfig()
	{
		// If we leave everything to annotations, is picks up the eclipse moxy gson writer, which doesn't handle abstract classes properly.
		// The goal here is to force it to use Jackson, but it seems that registering jackson disables scanning, so also have to re-enable
		// scanning. It also seems ot forget to scan this class... so register itself..
		super(new ResourceConfig().packages("net.sagebits.tmp.isaac.rest").register(JacksonFeature.class).register(ApplicationConfig.class));
		
		//This is for supporting .xml and .json extensions for changing the return type
		HashMap<String, Object> uriTypeMapProperties = new HashMap<>();
		uriTypeMapProperties.put(ServerProperties.MEDIA_TYPE_MAPPINGS, "xml : " + MediaType.APPLICATION_XML + ", json : " + MediaType.APPLICATION_JSON);
		this.addProperties(uriTypeMapProperties);
	}

	public static ApplicationConfig getInstance()
	{
		return instance_;
	}

	@Override
	public void onReload(Container arg0)
	{
		// noop
	}

	@Override
	public void onShutdown(Container arg0)
	{
		shutdown = true;
		log.info("Stopping ISAAC");
		LookupService.shutdownIsaac();
		log.info("ISAAC stopped");
		instance_ = null;
		startup.set(1);
	}

	private void configureSecret()
	{
		File tempDirName = new File(System.getProperty("java.io.tmpdir"));
		File file = new File(tempDirName, contextPath.replaceAll("/", "_") + "-tokenSecret");

		log.debug("Secret file for token encoding " + file.getAbsolutePath() + " " + (file.exists() ? "exists" : "does not exist"));

		if (file.exists())
		{
			try
			{
				byte[] temp = Files.readAllBytes(file.toPath());
				if (temp.length == 20)
				{
					secret_ = temp;
					log.info("Restored token secret");
				}
				else
				{
					log.warn("Unexpected data in token secret file.  Will calculate a new token. " + file.getCanonicalPath());
				}
			}
			catch (IOException e1)
			{
				log.warn("Failed opening token secret file.  Will calculate a new token.", e1);
			}
		}
		if (secret_ == null)
		{
			byte[] temp = new byte[20];

			log.info("Calculating a new token");
			// Don't use SecureRandom().getInstanceStrong() here, it hangs on linux, and we don't need that level of security.
			// Is supposed to be fixed in Java 9.
			new SecureRandom().nextBytes(temp);
			secret_ = temp;
			try
			{
				Files.write(file.toPath(), secret_);
			}
			catch (IOException e)
			{
				log.warn("Unexpected error storing token secret file", e);
			}
		}
	}

	@Override
	public void onStartup(Container container)
	{
		log.info("onStartup called");
		if (instance_ != null)
		{
			throw new RuntimeException("Unexpected for the instance to not be null!");
		}
		instance_ = this;

		// context is null when run from eclipse with the local grizzly runner.
		if (context_ == null)
		{
			debugMode = true;
			contextPath = "rest";
		}
		else
		{
			contextPath = context_.getContextPath().replace("/", "");
			debugMode = (contextPath.contains("SNAPSHOT") ? true : false);
		}

		log.info("Context path of this deployment is '" + contextPath + "' and debug mode is " + debugMode);

		configureSecret();

		issacInit();
	}

	public boolean isIsaacReady()
	{
		return LookupService.isIsaacStarted();
	}

	public String getStatusMessage()
	{
		return status_.get();
	}

	private void issacInit()
	{
		log.info("Isaac Init called");
		if (startup.getAndDecrement() == 1)
		{
			log.info("Executing initial ISAAC Init in background thread");
			System.setProperty(SystemPropertyConstants.EXTRA_PACKAGES_TO_SEARCH, "net.sagebits");
			// do startup in this thread
			LookupService.get();
			LookupService.startupWorkExecutors();

			Runnable r = new Runnable()
			{
				@Override
				public void run()
				{
					try
					{
						log.info("ISAAC Init thread begins");

						RestConfig rc = RestConfig.getInstance();
						RemoteServiceInfo gitConfig = null;
						if (StringUtils.isNotBlank(rc.getGitRootURL()))
						{
							// Place git config info into the configuration service
							gitConfig = new RemoteServiceInfo()
							{
								@Override
								public String getUsername()
								{
									return rc.getGitUsername();
								}
	
								@Override
								public String getURL()
								{
									return rc.getGitRootURL();
								}
	
								@Override
								public char[] getPassword()
								{
									return rc.getGitPassword();
								}
							};
						}
						
						boolean defaultInit = false;

						String databaseRootLocation = System.getProperty(DATA_STORE_ROOT_LOCATION_PROPERTY);
						if (StringUtils.isBlank(databaseRootLocation) || !Files.isDirectory(Paths.get(databaseRootLocation)))
						{
							// if there isn't an official system property set, check this one or if the directory does not exist
							String sysProp = System.getProperty("isaacDatabaseLocation");
							if (StringUtils.isBlank(sysProp) || !Files.isDirectory(Paths.get(sysProp)))
							{
								// No ISAAC default property set, nor the isaacDatabaseLocation property is set. If we have coordinates
								// for a DB, lets download it.
								if (StringUtils.isNotBlank(RestConfig.getInstance().getDbArtifactId()))
								{
									log.info("Downloading a database for use");
									status_.set("Downloading DB");
									try
									{
										dbLocation = downloadDB();
									}
									catch (Exception e)
									{
										status_.unbind();
										status_.set("Download Failed: " + e);
										throw new RuntimeException(e);
									}
								}
								else
								{
									//No coordinates, we will just create a simple metadata-only DB
									dbLocation = new File(System.getProperty("java.io.tmpdir"), "UTS." + contextPath + ".data");
									RecursiveDelete.delete(dbLocation);
									dbLocation.mkdirs();
									defaultInit = true;
								}
							}
							else
							{
								dbLocation = new File(sysProp);
							}

							if (shutdown)
							{
								return;
							}

							//Go down a directory, if necessary.
							for (File f : dbLocation.listFiles())
							{
								if (f.isDirectory() && f.getName().endsWith(".data")) {
									dbLocation = f;
									break;
								}
							}

							if (!dbLocation.exists())
							{
								throw new RuntimeException(
										"Couldn't find a data store from the input of '" + dbLocation.getAbsoluteFile().getAbsolutePath() + "'");
							}
							if (!dbLocation.isDirectory())
							{
								throw new RuntimeException("The specified data store: '" + dbLocation.getAbsolutePath() + "' is not a folder");
							}

							// use the passed in JVM parameter location
							LookupService.getService(ConfigurationService.class).setDataStoreFolderPath(dbLocation.toPath());
							System.out.println("  Setup AppContext, data store location = " + dbLocation.getAbsolutePath());
						}

						if (shutdown)
						{
							return;
						}

						try
						{
							if (defaultInit)
							{
								Get.configurationService().setDatabaseInitializationMode(DatabaseInitialization.LOAD_METADATA);
								Get.configurationService().setDatabaseImplementation(DatabaseImplementation.MV);
							}
							
							if (gitConfig != null)
							{
								//bring us up to the metadata level, so we can set this:
								LookupService.startupMetadataStore();
								Get.configurationService().getGlobalDatastoreConfiguration().setGitConfiguration(gitConfig);
							}
							
							status_.set("Starting ISAAC");
							LookupService.startupIsaac();

							// log metadata versions of codebase and database
							String auxiliaryMetadataVersion = getDatabaseIsaacMetadataVersion();

							log.info("Isaac metadata versions - Codebase {} - Database {}.", IsaacMetadataAuxiliary.AUXILIARY_METADATA_VERSION,
									auxiliaryMetadataVersion);

							if (!IsaacMetadataAuxiliary.AUXILIARY_METADATA_VERSION.equals(auxiliaryMetadataVersion))
							{
								log.warn("Isaac Metadata Auxiliary versions do not match between code and database!");
							}
							
							if (defaultInit)
							{
								File beer = new File("../ISAAC/integration/tests/src/test/resources/turtle/bevontology-0.8.ttl");
								if (beer.isFile())
								{
									log.info("Loading beer....");
									TurtleImportMojoDirect timd = new TurtleImportMojoDirect();
									timd.configure(null, beer.toPath(), "0.8", null);
									timd.convertContent(update -> {}, (work, total) -> {});
									Get.indexDescriptionService().refreshQueryEngine();
									log.info("Loaded Beer");
								}
							}

						}
						catch (Exception e)
						{
							log.error("Startup failed due to ", e);
							// rename directory of existing database and restart startup.
							// if database exists, rename folder
							if (dbLocation != null && dbLocation.exists())
							{
								log.info(
										"Database deemed corrupted.  Shutdown database, move it to CORRUPT directory, download new database, and re-startup system.");

								// Shutdown ISAAC
								log.info("Shutting down database");
								LookupService.shutdownIsaac();

								// Move corrupted database
								log.info("Moving corrupted database");
								File corruptDbLocation = new File(dbLocation.getParent() + File.separator + "CORRUPT" + File.separator
										+ LocalDateTime.now().format(fileDateTimeFormatter));
								log.info("DB Location: " + dbLocation.getAbsolutePath() + " to " + corruptDbLocation.getAbsolutePath());
								FileUtils.moveDirectoryToDirectory(dbLocation, corruptDbLocation, true);
								FileUtils.deleteDirectory(dbLocation);

								// download a new database
								log.info("Downloading new database");
								dbLocation = downloadDB();
								for (File f : dbLocation.listFiles())
								{
									if (f.isDirectory() && f.getName().endsWith(".data")) {
										dbLocation = f;
										break;
									}
								}
								LookupService.getService(ConfigurationService.class).setDataStoreFolderPath(dbLocation.toPath());
								log.info("Setup AppContext, data store location = " + dbLocation.getAbsolutePath());

								// Re-start ISAAC
								log.info("Re-starting ISAAC");
								status_.set("Starting ISAAC");
								LookupService.startupIsaac();

								log.info("Directories containing the corrupt database were renamed.  A new database was downloaded and ibdf file(s) loaded");
							}
						}

						systemInfo_ = new RestSystemInfo();
						log.info(systemInfo_.toString());

						try
						{
							String warFileVersion = RestConfig.getInstance().getApplicationWarFileVersion();
							if (StringUtils.isNotBlank(warFileVersion) && !warFileVersion.equals(systemInfo_.getApiImplementationVersion()))
							{
								log.warn(
										"The WAR file version found in the prisme.properties file does not match the version from the pom.xml in the war file!  Found "
												+ systemInfo_.getApiImplementationVersion() + " and " + warFileVersion);
							}
						}
						catch (Exception e)
						{
							log.error("Unexpected error validating war file versions!", e);
						}

						status_.set("Ready");
						System.out.println("Done setting up ISAAC");

						System.out.println(String.format("Application started.\nTry out %s%s\nStop the application by pressing enter.",
								"http://localhost:8180/", RestPaths.conceptVersionAppPathComponent + MetaData.CONCRETE_DOMAIN_OPERATOR____SOLOR.getNid()));
					}
					catch (Exception e)
					{
						log.error("Failure starting ISAAC", e);
						status_.unbind();
						status_.set("FAILED!");
					}
				}

			};

			LookupService.get().getService(WorkExecutors.class).getExecutor().execute(r);
		}
	}

	private boolean validateExistingDb(File targetDBLocation, String groupId, String artifactId, String version, String classifier)
	{
		// We need to read the pom.xml file that we find inside of targetDBLocation - and validate that each and every
		// parameter perfectly matches. If it doesn't match, then the DB must be deleted, and downloaded.
		// If we don't do this, we won't catch the case where the isaac-rest server was undeployed, then redeployed with a different DB configuration.
		// The pom file we need to read will be at targetDbLocation\*.data\META-INF\maven\{groupId}\{artifactId}\pom.xml
		// We need to validate <groupId>, <artifactId>, <version> and <resultArtifactClassifier> keeping in mind that classifer
		// is optional

		log.info("Checking specified parameters against existing db in folder: " + targetDBLocation.getAbsolutePath() + " - expecting to find content for "
				+ "group {}, artifact {}, version {} and classifier {}", groupId, artifactId, version, classifier);

		status_.set("Validating existing DB directory");

		if (!targetDBLocation.isDirectory())
		{
			log.warn("Validation of existing DB failed. Invalid DB directory: {}", targetDBLocation.getAbsoluteFile());

			return false;
		}

		File pomFile = null;
		for (File file : targetDBLocation.listFiles())
		{
			if (file.isDirectory() && file.getName().endsWith(".data"))
			{
				pomFile = new File(file.getAbsolutePath() + File.separatorChar + "META-INF" + File.separatorChar + "maven" + File.separatorChar + groupId
						+ File.separatorChar + artifactId + File.separatorChar + "pom.xml");
				if (pomFile.exists() && pomFile.isFile())
				{
					log.info("Found the expected existing pom file at: {}", pomFile.getAbsoluteFile());
					break;
				}
				else
				{
					pomFile = null;
				}
			}
		}
		if (pomFile == null || !pomFile.isFile())
		{
			log.warn("Validation of existing DB failed.  "
					+ (pomFile == null ? "The expected pom file was not found." : "The expected pom file location is not a file"));
			return false;
		}

		DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
		try
		{
			domFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			DocumentBuilder builder = domFactory.newDocumentBuilder();
			Document dDoc = builder.parse(pomFile);
			XPath xPath = XPathFactory.newInstance().newXPath();

			String existingDbGroupId = ((Node) xPath.evaluate("/project/groupId", dDoc, XPathConstants.NODE)).getTextContent();
			String existingDbArtifactId = ((Node) xPath.evaluate("/project/artifactId", dDoc, XPathConstants.NODE)).getTextContent();
			String existingDbVersion = ((Node) xPath.evaluate("/project/version", dDoc, XPathConstants.NODE)).getTextContent();

			Node existingDbClassifierNode = (Node) xPath.evaluate("/project/properties/resultArtifactClassifier", dDoc, XPathConstants.NODE);
			String existingDbClassifier = existingDbClassifierNode != null ? existingDbClassifierNode.getTextContent() : null;

			if (!existingDbGroupId.trim().equals(groupId.trim()))
			{
				log.warn("Validation of existing DB pom file failed. Existing groupId {} != {}", existingDbGroupId, groupId);

				return false;
			}
			if (!existingDbArtifactId.trim().equals(artifactId.trim()))
			{
				log.warn("Validation of existing DB pom file failed. Existing artifactId {} != {}", existingDbArtifactId, artifactId);
				return false;
			}
			if (!existingDbVersion.trim().equals(version.trim()))
			{
				log.warn("Validation of existing DB pom file failed. Existing version {} != {}", existingDbVersion, version);
				return false;
			}

			if (StringUtils.isBlank(classifier) && StringUtils.isBlank(existingDbClassifier))
			{
				return true;
			}
			else if (classifier == null || existingDbClassifier == null)
			{
				log.warn("Validation of existing DB pom file failed. Existing classifier {} != {}", existingDbClassifier, classifier);

				return false;
			}
			else if (classifier.trim().equals(existingDbClassifier.trim()))
			{
				return true;
			}
			else
			{
				log.warn("Validation of existing DB pom file failed. Existing classifier {} != {}", existingDbClassifier, classifier);
				return false;
			}
		}
		catch (Exception e)
		{
			log.warn("Validation of existing DB pom file failed", e);
		}

		return false;
	}

	private File downloadDB() throws Exception
	{
		File tempDbFolder = null;
		try
		{
			log.info("Checking for existing DB");

			File targetDBLocation = new File(System.getProperty("java.io.tmpdir"), "UTS." + contextPath + ".db");

			RestConfig rc = RestConfig.getInstance();
			
			if (targetDBLocation.isDirectory())
			{
				if (validateExistingDb(targetDBLocation, rc.getDbGroupId(), rc.getDbArtifactId(), rc.getDbVersion(), rc.getDbClassifier()))
				{
					log.info("Using existing db folder: " + targetDBLocation.getAbsolutePath());

					return targetDBLocation;
				}
				else
				{
					log.warn("Removing existing db because consistency validation failed");
					FileUtils.deleteDirectory(targetDBLocation);
				}
			}

			tempDbFolder = File.createTempFile("UTS-DATA", "");
			tempDbFolder.delete();
			tempDbFolder.mkdirs();
			log.info("Downloading DB to " + tempDbFolder.getAbsolutePath());
			URL cradle = MavenArtifactUtils.makeFullURL(rc.getArtifactBaseURL(), rc.getArtifactUsername(), rc.getArtifactPassword(), rc.getDbGroupId(), 
					rc.getDbArtifactId(), rc.getDbVersion(), rc.getDbClassifier(), "isaac.zip");
			Task<File> task = new DownloadUnzipTask( rc.getArtifactUsername(), rc.getArtifactPassword(), cradle, true, true, tempDbFolder);
			status_.bind(task.messageProperty());
			Get.workExecutors().getExecutor().submit(task);
			try
			{
				task.get();
			}
			catch (InterruptedException e)
			{
				task.cancel(true);
				throw e;
			}
			status_.unbind();
			status_.set("Download complete");

			log.debug("Renaming " + tempDbFolder.getCanonicalPath() + " to " + targetDBLocation.getCanonicalPath());
			if (tempDbFolder.renameTo(targetDBLocation))
			{
				if (validateExistingDb(targetDBLocation, rc.getDbGroupId(), rc.getDbArtifactId(), rc.getDbVersion(), rc.getDbClassifier()))
				{
					log.info("Using db folder created from download: " + targetDBLocation.getAbsolutePath());
					return targetDBLocation;
				}
				else
				{
					log.error("Failed to validate the new database");
					throw new RuntimeException("Failed to validate the new DB");
				}
			}
			else
			{
				log.error("Failed to rename the database");
				throw new RuntimeException("Failed to rename the DB folder");
			}
		}
		catch (Exception e)
		{
			log.error("existing downloadDB method with error: " + e);
			// cleanup
			try
			{
				if (tempDbFolder != null)
				{
					FileUtils.deleteDirectory(tempDbFolder);
				}
			}
			catch (Exception e1)
			{
				log.error("Unexpected error during cleanup", e1);
			}
			throw e;
		}
	}

	/**
	 * @return true if this is a debug deployment (in eclipse, or context contains SNAPSHOT)
	 */
	public boolean isDebugDeploy()
	{
		return debugMode;
	}

	/**
	 * @return String context path, which is a hard-coded value if in eclipse Jetty
	 */
	public String getContextPath()
	{
		return contextPath;
	}

	public static byte[] getSecret()
	{
		return secret_;
	}

	public RestSystemInfo getSystemInfo()
	{
		return systemInfo_;
	}

	public ServletContext getServletContext()
	{
		return context_;
	}

	private String getDatabaseIsaacMetadataVersion()
	{
		List<SemanticChronology> descriptions = Get.assemblageService().getDescriptionsForComponent(MetaData.METADATA____SOLOR.getNid());
		for (SemanticChronology descChron : descriptions)
		{
			for (Version v : descChron.getVersionList())
			{
				DescriptionVersion dv = (DescriptionVersion) v;
				if (StringUtils.isNotBlank(dv.getText()) && dv.getText().startsWith("version:"))
				{
					return dv.getText().substring(dv.getText().lastIndexOf(":") + 1);
				}
			}
		}
		return "";
	}
}
