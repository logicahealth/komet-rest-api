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
 * Return a RestConfig object, which has been read from the classpath, by reading the uts-rest-api.properties file.
 * If the uts-rest-api.properties file isn't present, or can't be read, most of the getters on the RestConfig will return
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
	
	private String gitRootURL;
	private String gitUsername;
	private char[] gitPassword;
	
	private String authURL;
	private boolean allowAnonRead = false;
	
	private RestConfig()
	{
		init();
		restConfig_ = this;
	}

	private void init()
	{
		try (InputStream stream = RestConfig.class.getResourceAsStream("/uts-rest-api.properties"))
		{
			final URL propertiesFile = RestConfig.class.getResource("/uts-rest-api.properties");
			Properties props = new Properties();
			if (stream == null)
			{
				log.info("No uts-rest-api.properties file was found on the classpath.  Will start with developer defaults");
			}
			else
			{
				log.info("Reading PRISME configuration from uts-rest-api.properties file " + propertiesFile);
				props.load(stream);
			}
			
			artifactBaseURL = props.getProperty("nexus_repository_url");
			artifactUsername = props.getProperty("nexus_user", "");
			
			artifactPassword = PasswordHasher.decryptPropFileValueIfEncrypted(props.getProperty("nexus_pwd"));
			
			dbGroupId = props.getProperty("db_group_id");
			dbArtifactId = props.getProperty("db_artifact_id");
			dbVersion = props.getProperty("db_version");
			dbClassifier = props.getProperty("db_classifier");
			
			gitRootURL = props.getProperty("git_root");
			gitUsername = props.getProperty("git_user", "");
			gitPassword = PasswordHasher.decryptPropFileValueIfEncrypted(props.getProperty("git_pwd"));
			
			authURL = props.getProperty("auth_url", "");
			
			if (props.getProperty("anonymous_read") != null)
			{
				allowAnonRead = Boolean.parseBoolean(props.getProperty("anonymous_read"));
			}
		}
		catch (Exception e)
		{
			String msg = "Unexpected error trying to read properties from the uts-rest-api.properties file";
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

	public String getAuthURL()
	{
		return authURL;
	}

	public boolean allowAnonymousRead()
	{
		return allowAnonRead;
	}
}
