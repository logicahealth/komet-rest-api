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
import java.net.URLDecoder;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import sh.isaac.api.Get;
import sh.isaac.api.LookupService;
import sh.isaac.misc.security.SystemRole;
import sh.isaac.misc.security.User;

/**
 * 
 * {@link UserServiceUtils}
 *
 * @author <a href="mailto:joel.kniaz.list@gmail.com">Joel Kniaz</a>
 *
 */
public class UserServiceUtils
{
	private static Logger log = LogManager.getLogger(UserServiceUtils.class);

	public static final String TEST_JSON1 = "{\"roles\":["
			+ "{\"id\":19990,\"name\":\"read_only\",\"resource_id\":null,\"resource_type\":null,\"created_at\":\"2016-09-13T14:48:18.000Z\",\"updated_at\":\"2016-09-13T14:48:18.000Z\"}"
			+ ","
			+ "{\"id\":19991,\"name\":\"editor\",\"resource_id\":null,\"resource_type\":null,\"created_at\":\"2016-09-13T14:48:18.000Z\",\"updated_at\":\"2016-09-13T14:48:18.000Z\"}"
			+ ","
			+ "{\"id\":19992,\"name\":\"reviewer\",\"resource_id\":null,\"resource_type\":null,\"created_at\":\"2016-09-13T14:48:18.000Z\",\"updated_at\":\"2016-09-13T14:48:18.000Z\"}"
			+ ","
			+ "{\"id\":19993,\"name\":\"administrator\",\"resource_id\":null,\"resource_type\":null,\"created_at\":\"2016-09-13T14:48:18.000Z\",\"updated_at\":\"2016-09-13T14:48:18.000Z\"}"
			+ ","
			+ "{\"id\":19994,\"name\":\"deployment_manager\",\"resource_id\":null,\"resource_type\":null,\"created_at\":\"2016-09-13T14:48:18.000Z\",\"updated_at\":\"2016-09-13T14:48:18.000Z\"}"
			+ ","
			+ "{\"id\":19995,\"name\":\"vuid_requestor\",\"resource_id\":null,\"resource_type\":null,\"created_at\":\"2016-09-13T14:48:18.000Z\",\"updated_at\":\"2016-09-13T14:48:18.000Z\"}"
			+ ","
			+ "{\"id\":19996,\"name\":\"approver\",\"resource_id\":null,\"resource_type\":null,\"created_at\":\"2016-09-13T14:48:18.000Z\",\"updated_at\":\"2016-09-13T14:48:18.000Z\"}"
			+ "],\"token_parsed?\":true,\"user\":\"TEST\",\"type\":\"ssoi\",\"id\":10005}";
	public static final String TEST_JSON2 = "{\"roles\":["
			+ "{\"id\":10000,\"name\":\"read_only\",\"resource_id\":null,\"resource_type\":null,\"created_at\":\"2016-09-13T14:48:18.000Z\",\"updated_at\":\"2016-09-13T14:48:18.000Z\"}"
			+ "],\"token_parsed?\":true,\"user\":\"VHAISBKniazJ\",\"type\":\"ssoi\",\"id\":10005}";
	public static final String TEST_JSON3 = "{\"roles\":["
			+ "{\"id\":10000,\"name\":\"read_only\",\"resource_id\":null,\"resource_type\":null,\"created_at\":\"2016-09-13T14:48:18.000Z\",\"updated_at\":\"2016-09-13T14:48:18.000Z\"}"
			+ ","
			+ "{\"id\":19991,\"name\":\"editor\",\"resource_id\":null,\"resource_type\":null,\"created_at\":\"2016-09-13T14:48:18.000Z\",\"updated_at\":\"2016-09-13T14:48:18.000Z\"}"
			+ "],\"token_parsed?\":true,\"user\":\"VHAISHEfronJ\",\"type\":\"ssoi\",\"id\":10005}";

	private UserServiceUtils()
	{
	}

	/**
	 * 
	 * Attempt to construct a user from a string of the following format:
	 * 
	 * {name}:{role1}[{,role2}[{,role3}[...]]]
	 * 
	 * @param arg
	 * @return
	 */
	static Optional<String> constructTestUser(String arg)
	{
		try
		{
			String[] components = arg.split(":");

			String name = null;
			Set<SystemRole> roles = new HashSet<>();
			if (components.length == 2)
			{
				if (components[0].matches("[A-Za-z][A-Za-z0-9_]*"))
				{
					name = components[0].trim();

					String[] roleStrings = components[1].split(",");

					for (int i = 0; i < roleStrings.length; ++i)
					{
						final String roleString = roleStrings[i].trim();
						if (SystemRole.safeValueOf(roleString).isPresent())
						{
							roles.add(SystemRole.safeValueOf(roleString).get());
						}
						else
						{
							log.warn("Not adding unsupported role \"" + roleString + "\"");
						}
					}
				}
			}

			if (name != null && name.length() > 0 && roles.size() > 0)
			{
				StringBuilder builder = new StringBuilder();
				builder.append("{\"roles\":[");
				boolean addedRole = false;
				for (SystemRole role : roles)
				{
					if (addedRole)
					{
						builder.append(",");
					}
					builder.append("{\"id\":19990,\"name\":\"" + role.toString()
							+ "\",\"resource_id\":null,\"resource_type\":null,\"created_at\":\"2016-09-13T14:48:18.000Z\",\"updated_at\":\"2016-09-13T14:48:18.000Z\"}");

					addedRole = true;
				}

				builder.append("],\"token_parsed?\":true,\"user\":\"" + name + "\",\"type\":\"ssoi\",\"id\":10005}");

				return Optional.of(builder.toString());
			}
		}
		catch (Exception e)
		{
			// ignore
		}

		return Optional.empty();
	}

	public static Optional<User> getUserFromTestToken(String ssoToken) throws JsonParseException, JsonMappingException, IOException
	{
		String jsonToUse = null;

		// We no longer automatically decode ssoTokens - but when the test class sends in raw json, it comes with ':' encoded.
		Optional<String> createdJson = constructTestUser(URLDecoder.decode(ssoToken, "UTF-8"));
		if (createdJson.isPresent())
		{
			jsonToUse = createdJson.get();
		}
		else
		{
			if (ssoToken.equals(TEST_JSON1))
			{
				jsonToUse = TEST_JSON1;
			}
			else if (ssoToken.equals(TEST_JSON2))
			{
				jsonToUse = TEST_JSON2;
			}
			else if (ssoToken.equals(TEST_JSON3))
			{
				jsonToUse = TEST_JSON3;
			}
			else if (ssoToken.equals("TEST_JSON1") || ssoToken.equals("TEST"))
			{
				jsonToUse = TEST_JSON1;
			}
			else if (ssoToken.equals("TEST_JSON2"))
			{
				jsonToUse = TEST_JSON2;
			}
			else if (ssoToken.equals("TEST_JSON3"))
			{
				jsonToUse = TEST_JSON3;
			}
			else
			{
				// Either a real SSO token or custom JSON
				jsonToUse = ssoToken;
			}
		}

		/*
		 * Example URL for get_roles_by_token
		 * URL url = new URL("https://vaauscttweb81.aac.va.gov/rails_prisme/roles/get_roles_by_token.json?token=" + token);
		 */
		User newUser = null;
		try
		{
			newUser = getUserFromJson(jsonToUse, ssoToken);
		}
		catch (Exception e)
		{
			try
			{
				newUser = getUserFromJson(TEST_JSON1, ssoToken);
			}
			catch (IOException e1)
			{
				throw new RuntimeException(e1);
			}
		}

		return Optional.of(newUser);
	}

//	private static void testToken(String token) {
//		PrismeIntegratedUserService service = LookupService.getService(PrismeIntegratedUserService.class);
//
//		PrismeServiceUtils.getPrismeProperties().setProperty(
//				"prisme_roles_by_token_url",
//				"https://vadev.mantech.com:4848/rails_prisme/roles/get_roles_by_token.json");
//		PrismeServiceUtils.getPrismeProperties().setProperty(
//				"prisme_roles_by_token_url",
//				"https://vadev.mantech.com:4848/rails_prisme/roles/get_roles_by_token.json");
//		try {
//			URL url = new URL(service.getSystemRolesByTokenUrl());
//
//			Client client = ClientBuilder.newClient();
//			
//			Response response = client.target(PrismeServiceUtils.getTargetFromUrl(url))
//					.path(url.getPath())
//					.queryParam("token", token)
//					.request().get();
//
//			String responseJson = response.readEntity(String.class);
//			
//			ObjectMapper mapper = new ObjectMapper();
//			Map<?, ?> map = null;
//			try {
//				map = mapper.readValue(responseJson, Map.class);
//			} catch (Exception e) {
//				throw new Exception("Failed parsing json \"" + responseJson);
//			} finally {
//				client.close();
//			}
//
//			System.out.println("Output: \"" + map + "\"");
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//	}

	public static void main(String... argv) throws Exception
	{
//		 * Example URL for get_roles_by_token
//		 * URL url = new URL("https://vaauscttweb81.aac.va.gov/rails_prisme/roles/get_roles_by_token.json?token=" + token);

		// {"user":"joel.kniaz@vetsez.com","roles":["read_only","super_user","administrator","vuid_requestor"],"token":"%5B%22u%5Cf%5Cx8F%5CxB1X%5C%22%5CxC7%5CxF2%5CxE8%5CxA5%5CxD8%5CxE3t%5CxFFUK%22%2C+%22%2CJ%5Cx83%5CxA3%5Cx13k%5Cx96%5CxFC%5CxE6%5CxF3%5CxCF%5CxF2%7C%5CxB8MK%22%2C+%224%5Cf%5Cx8C%5CxBA%5Cx1Ft%5CxDD%5CxB5%5CxA4%5CxB8%5CxC0%5CxE9Q%5CxAB%5CnK%22%2C+%22z%5D%5Cx83%5CxAFT%7B%5Cx9C%5CxB3%5CxE8%5CxAC%5CxA7%5Cx95%5Cx17%5CxDBiL%22%5D"}
		// final String ssoToken =
		// "%5B%22u%5Cf%5Cx8F%5CxB1X%5C%22%5CxC2%5CxEE%5CxFA%5CxE1%5Cx91%5CxBF3%5CxA9%5Cx16K%22%2C+%22%7EK%5CxC4%5CxEFX%7C%5Cx96%5CxA8%5CxA3%5CxA2%5CxC4%5CxB1%3D%5CxFF%5Cx01K%22%2C+%22oC%5Cx83%5CxF7%40%3A%5Cx94%5CxAC%5CxAF%5CxB6%5CxE1%5CxF4c%5CxB8%5CbK%22%2C+%22+M%5Cx89%5CxB8Xe%5CxF9%5CxD4%5CxC0%5CxDB%5CxAB%5Cx99%5Ce%5CxD7e%40%22%5D";

		// {"user":"readonly@readonly.com","roles":["read_only"],"token":"%5B%22u%5Cf%5Cx8F%5CxB1X%5C%22%5CxC6%5CxF2%5CxE8%5CxA5%5CxD8%5CxE3t%5CxFFUK%22%2C+%22%2CJ%5Cx83%5CxA3%5Cx13k%5Cx96%5CxFC%5CxE6%5CxF3%5CxCF%5CxF2%7C%5CxB8MK%22%2C+%224%5Cf%5Cx94%5CxB0%5Ce%7C%5Cx9C%5CxB0%5CxA6%5CxA8%5CxE1%5CxE1t%5CxBC%5CvK%22%2C+%22a%40%5Cx8A%5CxACT%7B%5Cx9C%5CxB3%5CxE8%5CxAC%5CxA7%5Cx95%5Cx17%5CxDBiL%22%5D"}
		// final String readOnlySsoToken =
		// "%5B%22u%5Cf%5Cx8F%5CxB1X%5C%22%5CxC6%5CxF2%5CxE8%5CxA5%5CxD8%5CxE3t%5CxFFUK%22%2C+%22%2CJ%5Cx83%5CxA3%5Cx13k%5Cx96%5CxFC%5CxE6%5CxF3%5CxCF%5CxF2%7C%5CxB8MK%22%2C+%224%5Cf%5Cx94%5CxB0%5Ce%7C%5Cx9C%5CxB0%5CxA6%5CxA8%5CxE1%5CxE1t%5CxBC%5CvK%22%2C+%22a%40%5Cx8A%5CxACT%7B%5Cx9C%5CxB3%5CxE8%5CxAC%5CxA7%5Cx95%5Cx17%5CxDBiL%22%5D";

		// Dan (read_only)
		// final String ssoToken =
		// "%5B%22u%5Cf%5Cx8F%5CxB1X%5C%22%5CxC0%5CxF2%5CxE8%5CxA5%5CxD8%5CxE3t%5CxFFUK%22%2C+%22%2CJ%5Cx83%5CxA3%5Cx13k%5Cx96%5CxFC%5CxE6%5CxF3%5CxCF%5CxF2%7C%5CxB8MK%22%2C+%224%5Cf%5Cx85%5CxA7%5Cx13k%5CxB3%5CxBD%5CxB8%5CxB8%5CxD2%5CxBDr%5CxB2%5Cx02K%22%2C+%22%2CS%5CxE8%5CxDBt%5Cx16%5CxFD%5CxD0%5CxC4%5CxDF%5CxAF%5Cx9D%5Cx1F%5CxD3aD%22%5D";
		// Greg (super_user)
		// final String ssoToken =
		// "%5B%22u%5Cf%5Cx8F%5CxB1X%5C%22%5CxC2%5CxEE%5CxFA%5CxE1%5Cx91%5CxBF3%5CxA9%5Cx16K%22%2C+%22%7EK%5CxC4%5CxEFX%7C%5Cx96%5CxA8%5CxA3%5CxA2%5CxC4%5CxB1%3D%5CxFF%5Cx01K%22%2C+%22oC%5Cx83%5CxF7%40%3A%5Cx94%5CxAC%5CxAF%5CxB6%5CxE1%5CxF4c%5CxB8%5CbK%22%2C+%22+M%5Cx89%5CxB8Xe%5CxF9%5CxD4%5CxC0%5CxDB%5CxAB%5Cx99%5Ce%5CxD7e%40%22%5D";

		// read_only (read_only)
		// final String ssoToken =
		// "%5B%22u%5Cf%5Cx8F%5CxB1X%5C%22%5CxC2%5CxEE%5CxFA%5CxE1%5Cx91%5CxBF3%5CxA9%5Cx16K%22%2C+%22%7EK%5CxC4%5CxEFX%7C%5Cx96%5CxA8%5CxA3%5CxA2%5CxC4%5CxB1%3D%5CxFF%5Cx01K%22%2C+%22oC%5Cx83%5CxF7%40%3A%5Cx94%5CxAC%5CxAF%5CxB6%5CxE1%5CxF4c%5CxB8%5CbK%22%2C+%22+M%5Cx89%5CxB8Xe%5CxF9%5CxD4%5CxC0%5CxDB%5CxAB%5Cx99%5Ce%5CxD7e%40%22%5D";

		// final String urlStr =
		// "https://vadev.mantech.com:4848/rails_prisme/roles/get_roles_by_token.json?id=cris@cris.com&password=cris@cris.com&token=" + ssoToken;
		// final String url = "https://vadev.mantech.com:4848/rails_prisme/roles/get_roles_by_token.json?token=" + ssoToken;

//		PrismeUserService PRISME_USER_SERVICE = LookupService.getService(PrismeIntegratedUserService.class);
//
//		testToken(PRISME_USER_SERVICE.getToken("joel.kniaz@vetsez.com", "joel.kniaz@vetsez.com"));
//		testToken(PRISME_USER_SERVICE.getToken("readonly@readonly.com", "readonly@readonly.com"));
	}

	private static User getUserFromJson(String jsonToUse, String ssoToken) throws JsonParseException, JsonMappingException, IOException
	{
		ObjectMapper mapper = new ObjectMapper();
		Map<?, ?> map = null;
		map = mapper.readValue(jsonToUse, Map.class);

		String userName = (String) map.get("user");
		if (StringUtils.isBlank(userName))
		{
			throw new RuntimeException("Failed extracting 'user' field from json '" + jsonToUse + "'");
		}

		Set<SystemRole> roleSet = new HashSet<>();
		Collection<?> roles = (Collection<?>) map.get("roles");
		for (Object roleMapObject : roles)
		{
			Map<?, ?> roleMap = (Map<?, ?>) roleMapObject;
			String roleName = (String) roleMap.get("name");

			if (SystemRole.safeValueOf(roleName).isPresent())
			{
				roleSet.add(SystemRole.safeValueOf(roleName).get());
			}
			else
			{
				log.warn("Not adding to user \"" + userName + "\" unsupported role \"" + roleName + "\"");
			}
		}

		User newUser = new User(userName, UserProvider.getUuidFromUserName(userName), ssoToken, roleSet);
		LookupService.get().getService(UserProvider.class).addUser(newUser);

		return newUser;
	}

	static Set<SystemRole> getAllRolesFromUrl(URL url) throws JsonParseException, JsonMappingException, IOException
	{
		String jsonResultString = PrismeServiceUtils.getResultJsonFromPrisme(PrismeServiceUtils.getTargetFromUrl(url), url.getPath());

		Set<SystemRole> roles = new HashSet<>();

		ObjectMapper mapper = new ObjectMapper();
		Object returnedObject = mapper.readValue(jsonResultString, List.class);

		for (Object roleFromPrisme : (List<?>) returnedObject)
		{
			if (SystemRole.safeValueOf(roleFromPrisme.toString()).isPresent())
			{
				roles.add(SystemRole.safeValueOf(roleFromPrisme.toString()).get());
			}
			else
			{
				log.warn("Not adding unsupported role \"" + roleFromPrisme.toString() + "\"");
			}
			roles.add(SystemRole.valueOf(roleFromPrisme.toString()));
		}

		return Collections.unmodifiableSet(roles);
	}

	static String getUserSsoTokenFromUrl(URL url, String id, String password) throws Exception
	{
		Map<String, String> params = new HashMap<>();
		params.put("id", id);
		params.put("password", password);
		String jsonResultString = PrismeServiceUtils.getResultJsonFromPrisme(PrismeServiceUtils.getTargetFromUrl(url), url.getPath(), params);

		ObjectMapper mapper = new ObjectMapper();
		Map<?, ?> map = null;
		try
		{
			map = mapper.readValue(jsonResultString, Map.class);
		}
		catch (Exception e)
		{
			throw new Exception("Failed parsing json \"" + jsonResultString);
		}
		// System.out.println("Output for id=\"" + id + "\", password=\"" + password + "\": \"" + map + "\"");

		return (String) map.get("token");
	}

	static Optional<User> getUserFromUrl(URL url, String ssoToken) throws JsonParseException, JsonMappingException, IOException
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

		Map<String, String> params = new HashMap<>();
		params.put("token", ssoToken);
		params.put("isaac_db_uuid", Get.conceptService().getDataStoreId().toString());
		log.debug("Using token '{}' and DB '{}'", ssoToken, Get.conceptService().getDataStoreId().toString());
		String jsonResultString = PrismeServiceUtils.getResultJsonFromPrisme(PrismeServiceUtils.getTargetFromUrl(url), url.getPath(), params);

		log.debug("PRISME returned '" + jsonResultString + "'");
		return Optional.of(UserServiceUtils.getUserFromJson(jsonResultString, ssoToken));
	}
}
