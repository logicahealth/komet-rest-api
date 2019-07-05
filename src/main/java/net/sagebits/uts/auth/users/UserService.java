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

package net.sagebits.uts.auth.users;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.cedarsoftware.util.io.JsonReader;
import com.cedarsoftware.util.io.JsonWriter;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import net.sagebits.uts.auth.data.User;
import net.sagebits.uts.auth.data.UserRole;
import sh.isaac.api.util.PasswordHasher;
import sh.isaac.api.util.UuidT5Generator;


/**
 * {@link UserService}
 * 
 * Our store of users.  
 * 
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */

public class UserService
{
	private transient static Logger log = LogManager.getLogger();
	
	public static final String AUTH_USER_IMPORT = "AUTH_USER_IMPORT";
	
	private final ConcurrentHashMap<UUID, User> users_ = new ConcurrentHashMap<>();
	private final transient HashMap<String, UUID> uniqueUserName_ = new HashMap<>();
	private final transient HashMap<String, UUID> uniqueEmail_ = new HashMap<>();
	
	private final File storageLocation_;

	/**
	 * @param storageLocation Where to store the users (as a json file).  Will also create a file with this name, 
	 *     plus an additional ".bak" extension.
	 * @param enableUserImport true to allow import of file at the AUTH_USER_IMPORT location, false otherwise.
	 */
	public UserService(File storageLocation, boolean enableUserImport)
	{
		storageLocation_ = storageLocation;
		
		if (storageLocation_.isDirectory())
		{
			throw new RuntimeException("storage location should not be a directory");
		}
		
		if (storageLocation_.isFile())
		{
			log.info("Reading in user store from {}", storageLocation_);
			try
			{
				JsonReader jr = new JsonReader(new FileInputStream(storageLocation_));
				@SuppressWarnings("unchecked")
				ConcurrentHashMap<UUID, User> readUsers = (ConcurrentHashMap<UUID, User>)jr.readObject();
				jr.close();
				for (Entry<UUID, User> x : readUsers.entrySet())
				{
					users_.put(x.getKey(), x.getValue());
					if (x.getValue().getEmail() != null)
					{
						uniqueEmail_.put(x.getValue().getEmail().toLowerCase(), x.getKey());
					}
					if (x.getValue().getUserName() != null)
					{
						uniqueUserName_.put(x.getValue().getUserName().toLowerCase(), x.getKey());
					}
				}
			}
			catch (FileNotFoundException e)
			{
				// this should be impossible
				log.error("error reading user store", e);
			}
		}
		else
		{
			log.info("Creating a new user store at {}", storageLocation_);
		}
		
		if (enableUserImport && System.getenv(AUTH_USER_IMPORT) != null || System.getProperty(AUTH_USER_IMPORT) != null)
		{
			try
			{
				File importFile = new File(System.getProperty(AUTH_USER_IMPORT, System.getenv(AUTH_USER_IMPORT)));
				if (importFile.isFile())
				{
					log.info("Importing users from {} because system property {} is set", importFile.getAbsolutePath(), AUTH_USER_IMPORT);
					try(CSVReader reader =  new CSVReaderBuilder(new BufferedReader(new InputStreamReader(new FileInputStream(importFile))))
							.withCSVParser(new CSVParserBuilder().withSeparator('\t').build()).build())
					{
						for (String[] line : reader.readAll())
						{
							if ((line.length > 0 && line[0].startsWith("#")) || line.length < 1 || line[0].length() == 0)
							{
								//skip comment line, lines missing usernames
								continue;
							}
							
							String userName = line[0];
							String displayName = (line.length < 2 || StringUtils.isBlank(line[1])) ? line[0] : line[1];
							UUID id = (line.length < 3 || StringUtils.isBlank(line[2])) ? UuidT5Generator.get(UuidT5Generator.PATH_ID_FROM_FS_DESC, userName) 
									: UUID.fromString(line[2]);
							String roles = (line.length < 4 || StringUtils.isBlank(line[3])) ? "" : line[3];
							char[] password = (line.length < 5 || StringUtils.isBlank(line[4])) ? new char[0] : PasswordHasher.decryptPropFileValueIfEncrypted(line[4]);
							ArrayList<UserRole> globalRoles = new ArrayList<>();
							
							for (String roleString : roles.split(","))
							{
								if (roleString.length() > 0)
								{
									Optional<UserRole> ur = UserRole.parse(roleString);
									if (!ur.isPresent())
									{
										log.error("Invalid role string on user {} : {}, skipping role", roleString, userName);
										continue;
									}
									globalRoles.add(ur.get());
								}
							}
							
							if (users_.containsKey(id))
							{
								log.info("Not adding user {} from the import file, because the user already exists in the store", userName);
							}
							else
							{
								User u = new User(id, userName, displayName, globalRoles.toArray(new UserRole[globalRoles.size()]), null);
								if (password.length > 0)
								{
									u.setPassword(password);
								}
								log.info("Adding user {} from import file", u);
								addOrUpdate(u);
							}
						}
					}
					
				}
				else
				{
					log.info("System property of {} was set to {}, but does not point to a file, will not import users", AUTH_USER_IMPORT, importFile.getAbsolutePath());
				}
			}
			catch (Exception e)
			{
				log.error("Error importing users from system property specified file", e);
			}
		}
	}
	
	/**
	 * The UUIDs of all currently known users
	 * @return
	 */
	public Set<UUID> getUsers()
	{
		return users_.keySet();
	}
	
	/**
	 * Add a user to the set of known users.  Username and email, if provided, must be unique.
	 * @param user 
	 */
	public void addOrUpdate(User user)
	{
		if (StringUtils.isNotBlank(user.getEmail()))
		{
			UUID test = uniqueEmail_.get(user.getEmail().toLowerCase());
			if (test != null && !user.getId().equals(test))
			{
				throw new IllegalArgumentException("The provided email address is already in use by a user");
			}
		}
		
		if (StringUtils.isNotBlank(user.getUserName()))
		{
			UUID test = uniqueUserName_.get(user.getUserName().toLowerCase());
			if (test != null && !user.getId().equals(test))
			{
				throw new IllegalArgumentException("The provided user name '" + user.getUserName() + "' is in use by a user");
			}
		}
		
		User oldUserData = users_.put(user.getId(), user);
		if (oldUserData != null)
		{
			log.info("Replaced user information for previously existing user, old: {}, new: {}", oldUserData, user);
			if (StringUtils.isNotBlank(oldUserData.getEmail()))
			{
				uniqueEmail_.remove(oldUserData.getEmail().toLowerCase());
			}
			if (StringUtils.isNotBlank(oldUserData.getUserName()))
			{
				uniqueUserName_.remove(oldUserData.getUserName().toLowerCase());
			}
		}
		else
		{
			log.info("Stored new user: {}", user);
		}
		
		if (StringUtils.isNotBlank(user.getEmail()))
		{
			uniqueEmail_.put(user.getEmail().toLowerCase(), user.getId());
		}
		if (StringUtils.isNotBlank(user.getUserName()))
		{
			uniqueUserName_.put(user.getUserName().toLowerCase(), user.getId());
		}
		
		try
		{
			save();
		}
		catch (IOException e)
		{
			log.error("Error writing user store", e);
		}
	}
	
	/**
	 * @param user The user to remove
	 * @return true if the user existed, and was removed.  False if the user did not exist
	 */
	public boolean removeUser(UUID user)
	{
		Optional<User> userObject = getUser(user);
		if (userObject.isPresent())
		{
			users_.remove(user);
			if (StringUtils.isNotBlank(userObject.get().getUserName()))
			{
				uniqueUserName_.remove(userObject.get().getUserName().toLowerCase());
			}
			if (StringUtils.isNotBlank(userObject.get().getEmail()))
			{
				uniqueEmail_.remove(userObject.get().getEmail().toLowerCase());
			}
			try
			{
				save();
			}
			catch (IOException e)
			{
				log.error("Error writing user store", e);
			}
			log.info("Removed user {}", userObject);
			return true;
		}
		else
		{
			log.info("Remove called with user that isn't present: {}");
			return false;
		}
	}
	
	/**
	 * Lookup a user via either their username (login name) or their email address.
	 * @param userNameOrEmail
	 * @return
	 */
	public Optional<User> findUser(String ... userNameOrEmail)
	{
		for (String s : userNameOrEmail)
		{
			if (StringUtils.isBlank(s))
			{
				continue;
			}
			UUID temp = uniqueEmail_.get(s.toLowerCase());
			if (temp != null)
			{
				Optional<User> u =  Optional.ofNullable(users_.get(temp));
				return u.isPresent() ? Optional.of(u.get().clone()) : u;
			}
			
			temp = uniqueUserName_.get(s.toLowerCase());
			if (temp != null)
			{
				Optional<User> u =  Optional.ofNullable(users_.get(temp));
				return u.isPresent() ? Optional.of(u.get().clone()) : u;
			}
		}
		return Optional.empty();
	}
	

	/**
	 * Return all of the user information, if present, for the specified user.
	 * @param userId 
	 * @return
	 */
	public Optional<User> getUser(UUID userId)
	{
		Optional<User> u = Optional.ofNullable(users_.get(userId));
		return u.isPresent() ? Optional.of(u.get().clone()) : u;
	}

	private void save() throws IOException
	{
		if (storageLocation_.isFile())
		{
			Files.move(storageLocation_.toPath(), new File(storageLocation_.getAbsolutePath() + ".bak").toPath(), StandardCopyOption.REPLACE_EXISTING);
		}
		
		final Map<String, Object> args = new HashMap<>();
		args.put(JsonWriter.PRETTY_PRINT, true);
		
		JsonWriter jsonWriter = new JsonWriter(new FileOutputStream(storageLocation_), args);
		
		jsonWriter.write(users_);
		jsonWriter.close();
	}
}
