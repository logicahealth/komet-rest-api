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

package net.sagebits.tmp.isaac.rest.testng;

import java.io.IOException;
import java.util.Optional;
import javax.inject.Singleton;
import org.glassfish.hk2.api.Rank;
import org.jvnet.hk2.annotations.Service;
import net.sagebits.tmp.isaac.rest.session.PrismeIntegratedUserService;
import net.sagebits.tmp.isaac.rest.session.PrismeUserService;
import net.sagebits.tmp.isaac.rest.session.UserServiceUtils;
import sh.isaac.misc.security.User;

/**
 * 
 * {@link TestPrismeIntegratedUserService}
 *
 * @author <a href="mailto:joel.kniaz.list@gmail.com">Joel Kniaz</a>
 *
 */
@Service(name = "rest-prismeUserService")
@Rank(value = 20)
@Singleton
public class TestPrismeIntegratedUserService extends PrismeIntegratedUserService implements PrismeUserService
{
	/**
	 * 
	 */
	public TestPrismeIntegratedUserService()
	{
		super();
	}

	/**
	 * Return a user and roles available for that user
	 *
	 * @param ssoToken
	 *            the user's SSO token string
	 * @return the user and roles available to the user
	 * 
	 *         This implementation gets a User from PRISME IFF prisme.properties is in classpath
	 *         and contains a value for property "prisme_roles_by_token_url", otherwise it attempts to parse
	 *         the token as a test keyword from UserServiceutils (i.e. TEST_JSON1) OR to parse a test
	 *         string of the form {name}:{role1}[{,role2}[{,role3}[...]]]
	 */
	public Optional<User> getUser(String ssoToken)
	{
		if (usePrismeForRolesByToken())
		{
			try
			{
				return getUserFromPrisme(ssoToken);
			}
			catch (IOException e)
			{
				throw new RuntimeException(e);
			}
		}
		else
		{
			try
			{
				return UserServiceUtils.getUserFromTestToken(ssoToken);
			}
			catch (IOException e)
			{
				throw new RuntimeException(e);
			}
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
			System.err.println(e);
			e.printStackTrace();
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
}
