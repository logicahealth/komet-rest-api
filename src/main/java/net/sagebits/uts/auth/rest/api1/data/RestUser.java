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

package net.sagebits.uts.auth.rest.api1.data;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import net.sagebits.uts.auth.data.User;
import net.sagebits.uts.auth.data.UserRole;
import net.sagebits.uts.auth.rest.api1.enumerations.RestUserRoleType;

/**
 * {@link RestUser}
 * 
 * All of the details for the given user
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
@XmlRootElement
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE)
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY)
public class RestUser
{
	RestUser()
	{
		//for jaxb
	}
	
	/**
	 * The short user name which may be used for authentication.  This may not be populated, if email is present.
	 */
	@XmlElement
	public String userName;
	
	/**
	 * The email address of the user - may be used for authentication.  This may not be populated, if userName is present.
	 */
	@XmlElement
	public String email;
	
	/**
	 * The user specified preferred name for display
	 */
	@XmlElement
	public String displayName;
	
	/**
	 * The token that should be submitted with any request to identify the user making the request.
	 */
	@XmlElement
	public String ssoToken;
	
	/**
	 * The UUID that uniquely identifies the user.  Also will correspond to a UUID for a concept in the database.
	 */
	@XmlElement
	public UUID userId;
	
	/**
	 * The UUID that uniquely identifies the database, if a dbUUID was supplied in the request for user information.
	 */
	@XmlElement
	public UUID dbUUID;	
	
	/**
	 * The currently effective roles for this user - which is a combination of global roles and any roles specific
	 * to the current database, if a database identifier was supplied with the request.
	 */
	@XmlElement
	public Set<RestUserRoleType> effectiveRoles = new HashSet<>();
	
	
	public RestUser(User user, UUID dbId)
	{
		this(user, dbId, true);
	}
	
	/**
	 * @param user
	 * @param dbId
	 * @param setSsoToken set this to false, to skip calculating an SSOToken - this has very limited use, and should only be used
	 * where the RestUser is being used internally, not where it is being returned back to a caller.  
	 * Use {@link #RestUser(User, UUID)} for most calls.
	 */
	public RestUser(User user, UUID dbId, boolean setSsoToken)
	{
		this.userName = user.getUserName();
		this.displayName = user.getDisplayName();
		this.email = user.getEmail();
		if (setSsoToken)
		{
			this.ssoToken = user.getSSOToken();
		}
		this.userId = user.getId();
		for (UserRole ur : user.getGlobalRoles())
		{
			this.effectiveRoles.add(new RestUserRoleType(ur));
		}
		for (UserRole ur : user.getDBRoles(dbId))
		{
			this.effectiveRoles.add(new RestUserRoleType(ur));
		}
	}
}
