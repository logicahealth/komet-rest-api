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

package net.sagebits.tmp.isaac.rest.api1.user;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentMap;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.sagebits.tmp.isaac.rest.api.exceptions.RestException;
import net.sagebits.tmp.isaac.rest.api1.RestPaths;
import net.sagebits.tmp.isaac.rest.api1.data.user.RestUserData;
import net.sagebits.tmp.isaac.rest.session.RequestInfo;
import net.sagebits.tmp.isaac.rest.session.RequestParameters;
import net.sagebits.uts.auth.data.UserRole.SystemRoleConstants;
import net.sagebits.uts.auth.rest.session.AuthRequestParameters;
import sh.isaac.MetaData;
import sh.isaac.api.Get;

/**
 * 
 * {@link UserStoreAPIs}
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
@Path(RestPaths.userDataStorePathComponent)
@RolesAllowed({ SystemRoleConstants.AUTOMATED, SystemRoleConstants.ADMINISTRATOR, SystemRoleConstants.SYSTEM_MANAGER, SystemRoleConstants.CONTENT_MANAGER,
	SystemRoleConstants.EDITOR, SystemRoleConstants.READ })
public class UserStoreAPIs
{
	private static Logger log = LogManager.getLogger(UserStoreAPIs.class);
	protected static final String userStoreKey = "userStore";

	@Context
	private SecurityContext securityContext;

	/**
	 * Get a blob of string data that was stored on the users behalf.  This is typically used by client GUI's to store blobs of JSON that 
	 *     describe user preferences.  This should not be used to store large amounts of data, or terminology data.
	 *     This call requires user information to perform the read, so you must pass an sso token or an edit token, as you would with a 
	 *     typical write call.  
	 *     
	 *     The default behavior it to return local data, if present, followed by global data if present.  So local data stored in this instance
	 *     overrides the global value (if any).  You can specify global or local specifically, via parameter, if desired.  Specifying global
	 *     and local is illegal.
	 *      
	 * @param id The id key of the data to retrieve.  Throws an exception if the data doesn't exist, otherwise, 
	 *     returns the data as it was provided.
	 * @param local - optional - true to force this method to ignore global data, and only return this data if it is present locally.
	 * @param global - optional - true to force this method to ignore local data, and only return the data if it is present globally.
	 * @param editToken -
	 *            EditToken string returned by previous call to getEditToken() or as renewed EditToken returned by previous write API call in a 
	 *            RestWriteResponse.  Used for user identity.  Alternatively, provide the ssoToken.
	 * @param ssoToken specifies an explicit serialized SSO token string. Not valid with use of editToken.
	 * @return 
	 * @throws RestException
	 */
	@GET
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Path(RestPaths.associationItemComponent + "{" + RequestParameters.id + "}")
	public RestUserData getData(@PathParam(RequestParameters.id) String id, @QueryParam(RequestParameters.local)  @DefaultValue("false") String local, 
			@QueryParam(RequestParameters.global) @DefaultValue("false") String global, @QueryParam(RequestParameters.editToken) String editToken, 
			@QueryParam(AuthRequestParameters.ssoToken) String ssoToken) throws RestException
	{
		RequestParameters.validateParameterNamesAgainstSupportedNames(RequestInfo.get().getParameters(), RequestParameters.id, RequestParameters.local, 
				RequestParameters.global, AuthRequestParameters.ssoToken, RequestParameters.editToken);
		
		if (!RequestInfo.get().getUser().isPresent() || MetaData.USER____SOLOR.getPrimordialUuid().equals(RequestInfo.get().getUser().get().userId))
		{
			throw new RestException("An sso token or an edit token must be passed to provide user information  You provided: " + RequestInfo.get().getUser());
		}
		
		boolean localOnly = Boolean.parseBoolean(local);
		boolean globalOnly = Boolean.parseBoolean(global);
		
		if (localOnly && globalOnly)
		{
			throw new RestException("Cannot specify true for both local and global parameters");
		}
		
		if (!localOnly)
		{
			//TODO read global
			if (globalOnly)
			{
				log.info("Returning user data of length {} for {} from global", 0, id);
				return new RestUserData(id, null);
			}
		}

		//Not returning global, see if we have it local.
		ConcurrentMap<String, String> dataStore = Get.metaContentService().<String, String>openStore(userStoreKey + "-" + RequestInfo.get().getUser().get().userId);
		
		String storedValue = dataStore.get(id);
		log.info("Returning user data of length {} for {} from local for user {}", storedValue == null ? 0 : storedValue.length(), id, RequestInfo.get().getUser().get().userId);
		return new RestUserData(id, storedValue);
	}
	
	/**
	 * Get all stored blobs of string data that were stored on the users behalf.  This is typically used by client GUI's to store blobs of JSON that 
	 *     describe user preferences.  This should not be used to store large amounts of data, or terminology data.
	 *     This call requires user information to perform the read, so you must pass an sso token or an edit token, as you would with a 
	 *     typical write call.  
	 *     
	 *     The default behavior it to return local data, if present, followed by global data if present.  So local data stored in this instance
	 *     overrides the global value (if any).  You can specify global or local specifically, via parameter, if desired.  Specifying global
	 *     and local is illegal.
	 *      
	 * @param id The id key of the data to retrieve.  Throws an exception if the data doesn't exist, otherwise, 
	 *     returns the data as it was provided.
	 * @param local - optional - true to force this method to ignore global data, and only return this data if it is present locally.
	 * @param global - optional - true to force this method to ignore local data, and only return the data if it is present globally.
	 * @param editToken -
	 *            EditToken string returned by previous call to getEditToken() or as renewed EditToken returned by previous write API call in a 
	 *            RestWriteResponse.  Used for user identity.  Alternatively, provide the ssoToken.
	 * @param ssoToken specifies an explicit serialized SSO token string. Not valid with use of editToken.
	 * @return 
	 * @throws RestException
	 */
	@GET
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Path(RestPaths.itemsComponent)
	public List<RestUserData> getAllData(@QueryParam(RequestParameters.local)  @DefaultValue("false") String local, 
			@QueryParam(RequestParameters.global) @DefaultValue("false") String global, @QueryParam(RequestParameters.editToken) String editToken, 
			@QueryParam(AuthRequestParameters.ssoToken) String ssoToken) throws RestException
	{
		RequestParameters.validateParameterNamesAgainstSupportedNames(RequestInfo.get().getParameters(), RequestParameters.local, RequestParameters.global, 
				AuthRequestParameters.ssoToken, RequestParameters.editToken);
		
		if (!RequestInfo.get().getUser().isPresent() || MetaData.USER____SOLOR.getPrimordialUuid().equals(RequestInfo.get().getUser().get().userId))
		{
			throw new RestException("An sso token or an edit token must be passed to provide user information.  You provided: " + RequestInfo.get().getUser());
		}
		
		boolean localOnly = Boolean.parseBoolean(local);
		boolean globalOnly = Boolean.parseBoolean(global);
		
		if (localOnly && globalOnly)
		{
			throw new RestException("Cannot specify true for both local and global parameters");
		}
		
		if (!localOnly)
		{
			//TODO read global
			if (globalOnly)
			{
				log.info("Returning {} user data items for from global", 0);
				return new ArrayList<RestUserData>();
			}
		}

		//Not returning global, see if we have it local.
		ConcurrentMap<String, String> dataStore = Get.metaContentService().<String, String>openStore(userStoreKey + "-" + RequestInfo.get().getUser().get().userId);
		
		ArrayList<RestUserData> result = new ArrayList<>();
		for (Entry<String, String> entry : dataStore.entrySet())
		{
			result.add(new RestUserData(entry.getKey(), entry.getValue()));
		}
		log.info("Returning {} user data items from local for user {}", result.size(), RequestInfo.get().getUser().get().userId);
		return result;
	}
}