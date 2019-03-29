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

package net.sagebits.uts.auth.rest.session;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.sagebits.uts.auth.rest.api.exceptions.RestException;

/**
 * 
 * {@link AuthRequestParameters}
 *
 * @author <a href="mailto:joel.kniaz.list@gmail.com">Joel Kniaz</a>
 *
 *         All parameters should be added to ALL_VALID_PARAMETERS, whether grouped or individually
 * 
 */
public class AuthRequestParameters
{
	private AuthRequestParameters()
	{
		//prevent construction
	}

	public final static String ssoToken = "ssoToken";
	public final static String userName = "userName";
	public final static String email = "email";
	public final static String password = "password";
	public final static String googleToken = "googleToken";
	public final static String dbUUID = "dbUUID";

	/**
	 * Set of all known parameters usable to detect malformed or incorrect parameters
	 */
	public final static Set<String> ALL_VALID_PARAMETERS;
	static
	{
		Set<String> params = new HashSet<>();
		params.addAll(unmodifiableSet(ssoToken, userName, email, password, googleToken, dbUUID));
		ALL_VALID_PARAMETERS = params;
	}

	/**
	 * @param parameters
	 * @param supportedParameterNames
	 * @throws RestException
	 * 
	 *             This method validates the context request parameters against passed valid parameters
	 *             It takes multiple parameter types in order to allow passing the constant parameter sets
	 *             from RequestParameters as well as any individual parameters passed in specific methods
	 */
	public final static void validateParameterNamesAgainstSupportedNames(Map<String, List<String>> parameters, Object... supportedParameterNames)
			throws RestException
	{
		Set<String> supportedParameterNamesSet = new HashSet<>();
		if (supportedParameterNames != null && supportedParameterNames.length > 0)
		{
			for (Object parameter : supportedParameterNames)
			{
				if (parameter instanceof Iterable)
				{
					for (Object obj : (Iterable<?>) parameter)
					{
						supportedParameterNamesSet.add(obj.toString());
					}
				}
				else if (parameter.getClass().isArray())
				{
					for (Object obj : (Object[]) parameter)
					{
						supportedParameterNamesSet.add(obj.toString());
					}
				}
				else
				{
					supportedParameterNamesSet.add(parameter.toString());
				}
			}
		}
		for (String parameterName : parameters.keySet())
		{
			String parameterNameToCompare = parameterName.toUpperCase(Locale.ENGLISH);
			boolean foundMatch = false;
			for (String supportedParameterName : supportedParameterNamesSet)
			{
				String supportedParameterNameToCompare = supportedParameterName.toUpperCase(Locale.ENGLISH);
				if (supportedParameterNameToCompare.equals(parameterNameToCompare))
				{
					foundMatch = true;
					break;
				}
			}
			if (!foundMatch)
			{
				throw new RestException(parameterName, Arrays.toString(parameters.get(parameterName).toArray()),
						"Invalid or unsupported parameter name.  Must be one of "
								+ Arrays.toString(supportedParameterNamesSet.toArray(new String[supportedParameterNamesSet.size()])));
			}
		}
	}

	@SafeVarargs
	private final static <T> Set<T> unmodifiableSet(T... elements)
	{
		Set<T> list = new HashSet<>(elements.length);
		for (T element : elements)
		{
			list.add(element);
		}
		return Collections.unmodifiableSet(list);
	}
}
