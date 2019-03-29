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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import net.sagebits.tmp.isaac.rest.api.exceptions.RestException;
import sh.isaac.api.Get;
import sh.isaac.api.component.concept.ConceptChronology;

/**
 * 
 * {@link RequestInfoUtils}
 *
 * @author <a href="mailto:joel.kniaz.list@gmail.com">Joel Kniaz</a>
 *
 */
public class RequestInfoUtils
{
	private RequestInfoUtils()
	{
	}

	public static void validateSingleParameterValue(Map<String, List<String>> params, String parameterName) throws RestException
	{
		if (params == null || params.get(parameterName) == null || params.get(parameterName).size() != 1)
		{
			throw new RestException(parameterName, params.get(parameterName) + "", "incorrect number ("
					+ (params.get(parameterName) != null ? params.get(parameterName).size() : 0) + " of values. Expected exactly 1 value.");
		}
	}

	public static void validateSingleParameterValue(String parameterName, List<String> value) throws RestException
	{
		if (value == null || value.size() != 1)
		{
			throw new RestException(parameterName, value + "",
					"incorrect number (" + (value != null ? value.size() : 0) + " of values. Expected exactly 1 value.");
		}
	}

	public static UUID parseUuidParameter(String parameterName, List<String> parameterValues) throws RestException
	{
		validateSingleParameterValue(parameterName, parameterValues);
		String value = parameterValues.iterator().next();
		try
		{
			return UUID.fromString(value);
		}
		catch (Exception e)
		{
			throw new RestException(parameterName, value, "invalid UUID parameter value");
		}
	}

	public static UUID parseUuidParameter(String parameterName, String str) throws RestException
	{
		try
		{
			return UUID.fromString(str);
		}
		catch (Exception e)
		{
			throw new RestException(parameterName, str, "invalid UUID parameter value");
		}
	}

	public static Optional<UUID> parseUuidParameterIfNonBlank(String parameterName, String str) throws RestException
	{
		if (StringUtils.isBlank(str))
		{
			return Optional.empty();
		}

		return Optional.of(parseUuidParameter(parameterName, str));
	}

	public static int parseIntegerParameter(String parameterName, String str) throws RestException
	{
		try
		{
			return Integer.parseInt(str);
		}
		catch (Exception e)
		{
			throw new RestException(parameterName, str, "invalid integer parameter value");
		}
	}

	public static int getNidFromParameter(String parameterName, String str) throws RestException
	{
		try
		{
			UUID uuid = null;
			try
			{
				uuid = UUID.fromString(str);

				if (Get.identifierService().hasUuid(uuid))
				{
					Optional<? extends ConceptChronology> concept = Get.conceptService().getOptionalConcept(uuid);
					if (concept.isPresent())
					{
						return concept.get().getNid();
					}
					else
					{
						int semanticNid = Get.identifierService().getNidForUuids(uuid);
						if (Get.assemblageService().hasSemantic(semanticNid))
						{
							return semanticNid;
						}
					}
				}
				else
				{
					throw new RestException(parameterName, str, "no concept or semantic exists corresponding to parameter");
				}
			}
			catch (RestException ex)
			{
				throw ex;
			}
			catch (Exception e)
			{
				// ignore
			}
			int id = Integer.parseInt(str);
			if (!Get.conceptService().hasConcept(id) && !Get.assemblageService().hasSemantic(id))
			{
				throw new RestException(parameterName, str, "no concept or semantic exists corresponding to " + parameterName + " parameter value: " + str);
			}
			else
			{
				return id;
			}
		}
		catch (RestException e)
		{
			throw e;
		}
		catch (Exception e)
		{
			throw new RestException(parameterName, str, "invalid " + parameterName + " parameter value: " + str);
		}
	}

	/**
	 * Same as {@link #getNidFromParameter(String, String)} except it validates it is a concept.
	 * @param parameterName 
	 * @param str 
	 * @return  the nid
	 * 
	 * @throws RestException
	 */
	public static int getConceptNidFromParameter(String parameterName, String str) throws RestException
	{
		int nid = getNidFromParameter(parameterName, str);
		if (!Get.conceptService().hasConcept(nid))
		{
			throw new RestException(parameterName, str, "The supplied identifier " + parameterName + " - " + str
					+ " does not correspond to a concept, perhaps it is a semantic, or it may not be loaded.");
		}
		return nid;
	}

	public static int getConceptNidFromParameter(Map<String, List<String>> params, String parameterName) throws RestException
	{
		validateSingleParameterValue(params, parameterName);

		return getConceptNidFromParameter(parameterName, params.get(parameterName).iterator().next());
	}

	/**
	 * Same as {@link #getNidFromParameter(String, String)} except it validates it is a semantic.
	 * @param parameterName 
	 * @param str 
	 * @return the nid
	 * 
	 * @throws RestException
	 */
	public static int getSemanticNidFromParameter(String parameterName, String str) throws RestException
	{
		int nid = getNidFromParameter(parameterName, str);
		if (!Get.assemblageService().hasSemantic(nid))
		{
			throw new RestException(parameterName, str, "The supplied identifier " + parameterName + " - " + str
					+ " does not correspond to a semantic, perhaps it is a concept, or it may not be loaded.");
		}
		return nid;
	}

	public static boolean parseBooleanParameter(String parameterName, String str) throws RestException
	{
		if (str == null || (!str.equalsIgnoreCase("false") && !str.equalsIgnoreCase("true")))
		{
			throw new RestException(parameterName, str, "invalid boolean parameter value");
		}
		else
		{
			return Boolean.parseBoolean(str);
		}
	}

	public static boolean getBooleanFromParameters(String parameterName, Map<String, List<String>> parameters) throws RestException
	{
		if (parameters.get(parameterName) == null || parameters.get(parameterName).size() != 1)
		{
			throw new RestException(parameterName, parameters.get(parameterName) + "", "invalid boolean parameter value");
		}
		return parseBooleanParameter(parameterName, parameters.get(parameterName).iterator().next());
	}

	public static List<String> expandCommaDelimitedElements(String list)
	{
		List<String> expandedList = new ArrayList<>();

		if (list == null)
		{
			return null;
		}
		for (String s : list.trim().split(","))
		{
			if (StringUtils.isNotBlank(s))
			{
				expandedList.add(s.trim());
			}
		}

		return expandedList;
	}

	public static List<String> expandCommaDelimitedElements(List<String> list)
	{
		List<String> expandedList = new ArrayList<>();

		if (list == null)
		{
			return null;
		}
		for (String element : list)
		{
			if (element != null && element.contains(","))
			{
				for (String s : element.trim().split(","))
				{
					if (StringUtils.isNotBlank(s))
					{
						expandedList.add(s.trim());
					}
				}
			}
			else
			{
				expandedList.add(element.trim());
			}
		}

		return expandedList;
	}

	/**
	 * @param params parameter name to value-list map provided in UriInfo by ContainerRequestContext
	 * @param names array of parameter collections, names or objects for which toString() is used
	 * @return the subset
	 */
	public static Map<String, List<String>> getParametersSubset(Map<String, List<String>> params, Object... names)
	{
		Map<String, List<String>> paramSubset = new HashMap<>();

		for (Object param : names)
		{
			if (param instanceof Iterable)
			{
				// Passed a collection
				for (Object paramName : (Iterable<?>) param)
				{
					if (params.containsKey(paramName.toString()) && params.get(paramName.toString()) != null && params.get(paramName.toString()).size() > 0)
					{
						paramSubset.put(paramName.toString(), params.get(paramName.toString()));
					}
				}
			}
			else if (params.containsKey(param.toString()) && params.get(param.toString()) != null && params.get(param.toString()).size() > 0)
			{
				paramSubset.put(param.toString(), params.get(param.toString()));
			}
		}

		return paramSubset;
	}

	/**
	 *
	 * This method returns an Optional containing a EditToken serialized string if it exists in the parameters map.
	 *
	 * @param allParams parameter name to value-list map provided in UriInfo by ContainerRequestContext
	 * @return an Optional containing a EditToken string if it exists in the parameters map
	 * @throws RestException
	 */
	protected static Optional<String> getEditTokenParameterStringValue(Map<String, List<String>> allParams) throws RestException
	{
		List<String> editTokenParameterValues = allParams.get(RequestParameters.editToken);

		if (editTokenParameterValues == null || editTokenParameterValues.size() == 0 || StringUtils.isBlank(editTokenParameterValues.get(0)))
		{
			return Optional.empty();
		}
		else if (editTokenParameterValues.size() > 1)
		{
			throw new RestException(RequestParameters.editToken, "\"" + editTokenParameterValues + "\"",
					"too many (" + editTokenParameterValues.size() + " values - should only be passed with one value");
		}
		return Optional.of(editTokenParameterValues.get(0));
	}
	
	/**
	 * @param allParams
	 * @param paramName
	 * @return
	 */
	public static String getFirstParameterValue(Map<String, List<String>> allParams, String paramName)
	{
		if (allParams.containsKey(paramName))
		{
			List<String> values = allParams.get(paramName);
			if (values.size() > 0)
			{
				return values.get(0);
			}
		}
		return null;
	}
}
