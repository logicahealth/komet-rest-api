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
package net.sagebits.tmp.isaac.rest.session;

import java.io.InputStream;
import java.net.URL;
import java.util.Properties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import sh.isaac.api.util.PasswordHasher;

/**
 * Return a RestConfig object, which has been read from the classpath, by reading the prisme.properties file.
 * If the prisme.properties file isn't present, or can't be read, most of the getters on the RestConfig will return
 * null.
 * 
 * Encrypted passwords inside the properties are decrypted during this process, using the password from the file
 * at a location specified by the environment variable "DECRYPTION_FILE". If the DECRYPTION_FILE environment variable
 * is not set, it looks for a file named "decryption.password" in the JVM start location.
 *
 * @author <a href="mailto:daniel.armbrust.list@sagebits.net">Dan Armbrust</a>
 */
public class RestConfig
{
	private static Logger log = LogManager.getLogger(RestConfig.class);
	private static RestConfig restConfig_ = null;

	
	private String artifactBaseURL;
	private String artifactUsername;
	private char[] artifactPassword;
	
	private String dbGroupId;
	private String dbArtifactId;
	private String dbVersion;
	private String dbClassifier;
	
	private String applicationWarFileVersion;
	private String applicationWarFileUUID;
	private String applicationWarGroupId;
	private String applicationWarArtifactId; 
	private String applicationWarClassifier;
	private String applicationWarPackage; 
	
	private String gitRootURL;
	private String gitUsername;
	private char[] gitPassword;
	
	private String prismeAllRolesURL;
	private String prismeRolesByTokenURL;
	private String prismeRolesUserURL;
	private String prismeRolesSSOIURL;
	private String prismeNotifyURL;
	private String prismeRootURL;
	
	private RestConfig()
	{
		init();
		restConfig_ = this;
	}

	private void init()
	{
		try (InputStream stream = PrismeServiceUtils.class.getResourceAsStream("/prisme.properties"))
		{
			final URL propertiesFile = PrismeServiceUtils.class.getResource("/prisme.properties");
			Properties props = new Properties();
			if (stream == null)
			{
				log.info("No prisme.properties file was found on the classpath.  Will start with developer defaults");
			}
			else
			{
				log.info("Reading PRISME configuration from prisme.properties file " + propertiesFile);
				props.load(stream);
			}
			
			artifactBaseURL = props.getProperty("nexus_repository_url");
			artifactUsername = props.getProperty("nexus_user", "");
			
			artifactPassword = PasswordHasher.decryptPropFileValueIfEncrypted(props.getProperty("nexus_pwd"));
			
			dbGroupId = props.getProperty("db_group_id");
			dbArtifactId = props.getProperty("db_artifact_id");
			dbVersion = props.getProperty("db_version");
			dbClassifier = props.getProperty("db_classifier");
			
			applicationWarFileVersion = props.getProperty("war_version");
			applicationWarFileUUID = props.getProperty("war_uuid");
			applicationWarGroupId = props.getProperty("war_group_id");
			applicationWarArtifactId = props.getProperty("war_artifact_id");
			applicationWarClassifier = props.getProperty("war_classifier");
			applicationWarPackage = props.getProperty("war_package");
			
			gitRootURL = props.getProperty("git_root");
			gitUsername = props.getProperty("git_user", "");
			gitPassword = PasswordHasher.decryptPropFileValueIfEncrypted(props.getProperty("git_pwd"));
			
			prismeAllRolesURL = props.getProperty("prisme_all_roles_url");
			prismeRolesByTokenURL = props.getProperty("prisme_roles_by_token_url");
			prismeRolesUserURL = props.getProperty("prisme_roles_user_url");
			prismeRolesSSOIURL = props.getProperty("prisme_roles_ssoi_url");
			prismeNotifyURL = props.getProperty("prisme_notify_url");
			prismeRootURL = props.getProperty("prisme_root");
		}
		catch (Exception e)
		{
			String msg = "Unexpected error trying to read properties from the prisme.properties file";
			log.error(msg, e);
			throw new RuntimeException(msg, e);
		}
	}

	public static RestConfig getInstance()
	{
		if (restConfig_ == null)
		{
			restConfig_ = new RestConfig();
		}
		return restConfig_;
	}

	public String getArtifactBaseURL()
	{
		return artifactBaseURL;
	}

	public String getArtifactUsername()
	{
		return artifactUsername;
	}

	public char[] getArtifactPassword()
	{
		return artifactPassword;
	}

	public String getDbGroupId()
	{
		return dbGroupId;
	}

	public String getDbArtifactId()
	{
		return dbArtifactId;
	}

	public String getDbVersion()
	{
		return dbVersion;
	}

	public String getDbClassifier()
	{
		return dbClassifier;
	}

	public String getApplicationWarFileVersion()
	{
		return applicationWarFileVersion;
	}

	public String getApplicationWarFileUUID()
	{
		return applicationWarFileUUID;
	}

	public String getApplicationWarGroupId()
	{
		return applicationWarGroupId;
	}

	public String getApplicationWarArtifactId()
	{
		return applicationWarArtifactId;
	}

	public String getApplicationWarClassifier()
	{
		return applicationWarClassifier;
	}

	public String getApplicationWarPackage()
	{
		return applicationWarPackage;
	}

	public String getGitRootURL()
	{
		return gitRootURL;
	}

	public String getGitUsername()
	{
		return gitUsername;
	}

	public char[] getGitPassword()
	{
		return gitPassword;
	}

	public String getPrismeAllRolesURL()
	{
		return prismeAllRolesURL;
	}

	public String getPrismeRolesByTokenURL()
	{
		return prismeRolesByTokenURL;
	}

	public String getPrismeRolesUserURL()
	{
		return prismeRolesUserURL;
	}

	public String getPrismeRolesSSOIURL()
	{
		return prismeRolesSSOIURL;
	}

	public String getPrismeNotifyURL()
	{
		return prismeNotifyURL;
	}

	public String getPrismeRootURL()
	{
		return prismeRootURL;
	}

}
