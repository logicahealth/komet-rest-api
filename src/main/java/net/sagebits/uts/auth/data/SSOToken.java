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

package net.sagebits.uts.auth.data;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sh.isaac.api.externalizable.ByteArrayDataBuffer;
import sh.isaac.api.util.PasswordHasher;

/**
 * 
 * {@link SSOToken}
 *
 * @author <a href="mailto:daniel.armbrust.list@sagebits.net">Dan Armbrust</a>
 */
public class SSOToken
{
	private static final Logger log = LoggerFactory.getLogger(SSOToken.class);

	private static final byte tokenVersion = 1;
	private static final int hashRounds = 2048;
	private static final int hashLength = 64;
	private static final int encodedHashLength = (int) Math.ceil(hashLength / 8f / 3f) * 4;  // http://stackoverflow.com/a/4715480

	private static volatile transient byte[] secret_;
	
	/*
	 * This controls the file naming of the file that is made in the temp folder, to contain the secret that is used to encode and decode tokens
	 */
	private static String secretPathPrefix_ = null;

	// when an sso token comes in that is older than an hour, it is invalid, and they must request a new one.
	private static final long tokenMaxAge = 1000l * 60l * 60l;

	
	// Transient - non-serialized variables
	private transient String serialization;
	
	//data actually put into the token
	private long createTime;

	private UUID tokenForUser;
	
	/**
	 * Create a new token for the specified user
	 * @param userIdentity the user id this token is being created for.
	 */
	public SSOToken(UUID userIdentity)
	{
		this.createTime = System.currentTimeMillis();
		this.tokenForUser = userIdentity;
		this.serialization = serialize();
	}

	/**
	 * Parse a serialized SSOToken.  Updates the internal token time to current, if the token is not yet expired.
	 * 
	 * @param encodedData
	 * @throws SecurityException if the token is invalid or expired
	 */
	public SSOToken(String encodedData) throws SecurityException
	{
		try
		{
			long time = System.currentTimeMillis();
			if (encodedData.length() < encodedHashLength)
			{
				throw new SecurityException("Invalid token");
			}
			String readHash = encodedData.substring(0, encodedHashLength);
			String calculatedHash = PasswordHasher.hash(encodedData.substring(encodedHashLength, encodedData.length()).toCharArray(), getSecret(), hashRounds, hashLength);

			if (!readHash.equals(calculatedHash))
			{
				throw new SecurityException("Invalid token!");
			}

			byte[] readBytes = Base64.getUrlDecoder().decode(encodedData.substring(encodedHashLength, encodedData.length()));
			ByteArrayDataBuffer buffer = new ByteArrayDataBuffer(readBytes);
			byte version = buffer.getByte();
			if (version != tokenVersion)
			{
				throw new SecurityException("Expected token version " + tokenVersion + " but read " + version);
			}

			createTime = buffer.getLong();
			if ((System.currentTimeMillis() - createTime) > tokenMaxAge)
			{
				throw new SecurityException("SSO Token Expired");
			}
			long msb = buffer.getLong();
			long lsb = buffer.getLong();
			this.tokenForUser = new UUID(msb, lsb);

			log.debug("token decode time " + (System.currentTimeMillis() - time) + "ms");
			
			this.createTime = System.currentTimeMillis();
			serialization = serialize();
		}
		catch (SecurityException e)
		{
			log.debug("Token '{}' is not valid because {}", encodedData, e.getMessage());
			throw e;
		}
		catch (Exception e)
		{
			log.warn("Failed creating SSOToken from \"" + encodedData + "\"", e);
			throw new SecurityException("Unexpected internal error parsing token");
		}
	}

	private String serialize()
	{
		try
		{
			String data = Base64.getUrlEncoder().encodeToString(getBytesToWrite());
			return PasswordHasher.hash(data.toCharArray(), getSecret(), hashRounds, hashLength) + data;
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}

	/**
	 * The URL safe encoded bytes that represent this user in token form.  The provided token may only be 
	 * @return
	 */
	public String getSerialized()
	{
		return serialization;
	}
	
	/**
	 * The UUID of the user allowed by this token
	 * @return
	 */
	public UUID getUser()
	{
		return tokenForUser;
	}
	
	/**
	 * The time this token was most recently parsed or created
	 * @return
	 */
	public long getCreationTime()
	{
		return createTime;
	}

	private byte[] getBytesToWrite()
	{
		ByteArrayDataBuffer buffer = new ByteArrayDataBuffer(25);
		buffer.putByte(tokenVersion);
		buffer.putLong(createTime);
		buffer.putLong(tokenForUser.getMostSignificantBits());
		buffer.putLong(tokenForUser.getLeastSignificantBits());
		buffer.trimToSize();
		return buffer.getData();
	}

	/**
	 * Read an existing, or create a new secret, as necessary, for encrypting / decrypting tokens.  Secret is stored on disk, 
	 * so that the tokens can be decrypted following a stop/start of the service.
	 * @return
	 */
	private byte[] getSecret()
	{
		if (secret_ == null)
		{
			synchronized (SSOToken.class)
			{
				if (secret_ == null)
				{
					File tempDirName = new File(System.getProperty("java.io.tmpdir"));
					if (secretPathPrefix_ == null)
					{
						throw new RuntimeException("Need to configure the secretPathPrefix_ prior to use");
					}
					//If a pathPrefix has been set, use that, otherwise, use the context path
					File file = new File(tempDirName, secretPathPrefix_ + "-tokenSecret");
			
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
			}
		}
		return secret_;
	}
	
	public static void setSecretPathPrefix(String secretPathPrefix)
	{
		secretPathPrefix_ = secretPathPrefix;
	}
}
