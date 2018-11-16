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

package net.sagebits.tmp.isaac.rest.session;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import javax.inject.Singleton;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.hk2.api.Rank;
import org.jvnet.hk2.annotations.Service;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import net.sagebits.tmp.isaac.rest.api.exceptions.RestException;
import sh.isaac.api.LookupService;
import sh.isaac.misc.security.SystemRole;
import sh.isaac.misc.security.User;

/**
 * The Class PrismeIntegratedUserService
 *
 * {@link PrismeIntegratedUserService}
 * 
 * @author <a href="mailto:joel.kniaz.list@gmail.com">Joel Kniaz</a>
 */
@Service(name = "rest-prismeUserService")
@Rank(value = 10)
@Singleton
public class PrismeIntegratedUserService implements PrismeUserService
{
	private static Logger log = LogManager.getLogger(PrismeIntegratedUserService.class);

	Map<String, UUID> ssoTokenCache = new LinkedHashMap<String, UUID>(5000, .75F, true)
	{
		private static final long serialVersionUID = 1L;

		public boolean removeEldestEntry(Map.Entry<String, UUID> eldest)
		{
			return size() > 5000;
		}
	};

	protected PrismeIntegratedUserService()
	{
		// for HK2
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.sagebits.tmp.isaac.rest.session.PrismeUserService#getUser(java.lang.String)
	 * 
	 * This implementation will fail if PRISME is not configured
	 */
	@Override
	public Optional<User> getUser(String ssoToken)
	{
		if (usePrismeForRolesByToken())
		{

			UUID userId = ssoTokenCache.get(ssoToken);
			if (userId != null)
			{
				log.debug("SSOToken cache hit for token " + ssoToken);
				Optional<User> user = LookupService.getService(UserProvider.class).get(userId);
				if (user.isPresent())
				{
					if (user.get().rolesStillValid())
					{
						log.debug("User cache hit, roles still valid, skipping prisme lookup");
						return user;
					}
					else
					{
						log.debug("User cache hit, but roles expired - last read at {} now {}", user.get().rolesCheckedAt(), System.currentTimeMillis());
					}
				}
			}

			try
			{
				Optional<User> user = getUserFromPrisme(ssoToken);
				if (user.isPresent())
				{
					ssoTokenCache.put(ssoToken, user.get().getId());
				}
				return user;
			}
			catch (IOException e)
			{
				throw new RuntimeException(e);
			}
		}
		else
		{
			// To run without PRISME (for testing only),
			// comment-out the below "throw"
			// and uncomment the following try/catch block
			final String msg = "Not properly configured to retrieve roles from PRISME";
			throw new RuntimeException(msg, new RestException(msg));

			// To run without PRISME (for testing only),
			// comment-out the above "throw"
			// and uncomment the following try/catch block
//			try {
//				return UserServiceUtils.getUserFromTestToken(ssoToken);
//			} catch (IOException e) {
//				throw new RuntimeException(e);
//			}
		}
	}

	public Optional<String> safeGetToken(String id, String password)
	{
		try
		{
			return Optional.of(getToken(id, password));
		}
		catch (Exception e)
		{
			log.error("Unexpected error in getSafeToken", e);
			return Optional.empty();
		}
	}

	public String getToken(String id, String password) throws Exception
	{
		if (usePrismeForSsoTokenByName())
		{
			return getUserSsoTokenFromPrisme(id, password);
		}
		else
		{
			throw new RuntimeException("Cannot generate SSO token for " + id + " without access to PRISME");
		}
	}

	@Override
	public User getUser(UUID userId)
	{
		Optional<User> user = LookupService.getService(UserProvider.class).get(userId);
		if (!user.isPresent())
		{
			throw new RuntimeException("User '" + userId + "' not present in role-checked cache!");
		}
		return user.get();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see gov.vha.isaac.ochre.api.SystemRoleService#getAllSystemRoles()
	 * 
	 * This implementation gets all roles from PRISME IFF prisme.properties is in classpath
	 * and contains a value for property "prisme_all_roles_url", otherwise it returns all of the
	 * SystemRole text values except for "automated"
	 */
	/**
	 * {@inheritDoc}
	 */
	@Override
	public Set<SystemRole> getAllPossibleUserRoles()
	{
		if (usePrismeForAllRoles())
		{
			try
			{
				return getAllRolesFromPrisme();
			}
			catch (IOException e)
			{
				throw new RuntimeException(e);
			}
		}
		else
		{
			Set<SystemRole> availableRoles = new HashSet<>();

			for (SystemRole role : SystemRole.values())
			{
				availableRoles.add(role);
			}

			return Collections.unmodifiableSet(availableRoles);
		}
	}

	protected String getPrismeAllRolesUrl()
	{
		return RestConfig.getInstance().getPrismeAllRolesURL();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean usePrismeForAllRoles()
	{
		return getPrismeAllRolesUrl() != null;
	}

	protected String getSystemRolesByTokenUrl()
	{
		return RestConfig.getInstance().getPrismeRolesByTokenURL();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean usePrismeForRolesByToken()
	{
		return StringUtils.isNotBlank(getSystemRolesByTokenUrl());
	}

	//TODO this naming obviously looks like a bug, need to get to the bottom of it.
	protected String getSsoTokenByNameUrl()
	{
		return RestConfig.getInstance().getPrismeRolesUserURL();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean usePrismeForSsoTokenByName()
	{
		return getSsoTokenByNameUrl() != null;
	}

	// Private helpers
	protected Optional<User> getUserFromPrisme(String ssoToken) throws JsonParseException, JsonMappingException, IOException
	{
//		/*
//		 * Example URL for get_roles_by_token
//		 * URL url = new URL("https://vaauscttweb81.aac.va.gov/rails_prisme/roles/get_roles_by_token.json?token=" + token);
//		 */
//		/*
//		 * Example SSO Token
//		 * %5B%22u%5Cf%5Cx8F%5CxB1X%5C%22%5CxC2%5CxEE%5CxFA%5CxE1%5Cx94%5CxBF3%5CxA9%5Cx16K%22%2C+%22%7EK%5CxC4%5CxEFXk%5Cx80%5CxB1%5CxA3%5CxF3%5Cx8D%5CxB1%5Cx7F%5CxBC%5Cx02K%22%2C+%22k%5Cf%5CxDC%5CxF7%2CP%5CxB2%5Cx97%5Cx99%5Cx99%5CxE0%5CxE1%7C%5CxBF%5Cx1DK%22%2C+%22J%5Cf%5Cx9B%5CxD8w%5Cx15%5CxFE%5CxD3%5CxC7%5CxDC%5CxAC%5Cx9E%5Cx1C%5CxD0bG%22%5D
//		 */
//		//String json = "{\"roles\":[{\"id\":10000,\"name\":\"read_only\",\"resource_id\":null,\"resource_type\":null,\"created_at\":\"2016-09-13T14:48:18.000Z\",\"updated_at\":\"2016-09-13T14:48:18.000Z\"}],\"token_parsed?\":true,\"user\":\"VHAISHArmbrD\",\"type\":\"ssoi\",\"id\":10005}";
		String prismeRolesByTokenUrlStr = getSystemRolesByTokenUrl();
		log.trace("Retrieved from prisme.properties prismeRolesByTokenUrlStr=\"" + prismeRolesByTokenUrlStr + "\"");
		URL url = new URL(prismeRolesByTokenUrlStr);
		Optional<User> user = UserServiceUtils.getUserFromUrl(url, ssoToken);
		log.trace("Retrieved from " + prismeRolesByTokenUrlStr + " user=\"" + user + "\"");

		if (!user.isPresent())
		{
			log.error("FAILED retrieving User from " + prismeRolesByTokenUrlStr);
		}
		return user;
	}

	protected Set<SystemRole> getAllRolesFromPrisme() throws JsonParseException, JsonMappingException, IOException
	{
		String prismeAllRolesUrlStr = getPrismeAllRolesUrl();
		log.trace("Retrieved from prisme.properties prismeAllRolesUrlStr=\"" + prismeAllRolesUrlStr + "\"");
		URL url = new URL(prismeAllRolesUrlStr);
		Set<SystemRole> allRolesFromFromPrisme = UserServiceUtils.getAllRolesFromUrl(url);
		log.trace("Retrieved from " + prismeAllRolesUrlStr + " allRolesFromFromPrisme=" + allRolesFromFromPrisme);
		return allRolesFromFromPrisme;
	}

	protected String getUserSsoTokenFromPrisme(String id, String password) throws Exception
	{
		String ssoTokenByNameUrlStr = getSsoTokenByNameUrl();
		log.trace("Retrieved from prisme.properties ssoTokenByNameUrlStr=\"" + ssoTokenByNameUrlStr + "\"");
		URL url = new URL(ssoTokenByNameUrlStr);
		String ssoToken = UserServiceUtils.getUserSsoTokenFromUrl(url, id, password);
		log.trace("Retrieved from " + ssoTokenByNameUrlStr + " ssoToken=\"" + ssoToken + "\"");
		return ssoToken;
	}
}
