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
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import net.sagebits.tmp.isaac.rest.api.exceptions.RestException;
import net.sagebits.tmp.isaac.rest.api1.coordinate.CoordinateAPIs;
import net.sagebits.tmp.isaac.rest.tokens.CoordinatesToken;
import net.sagebits.tmp.isaac.rest.tokens.CoordinatesTokens;
import sh.isaac.MetaData;
import sh.isaac.api.Get;
import sh.isaac.api.Status;
import sh.isaac.api.TaxonomySnapshot;
import sh.isaac.api.collections.NidSet;
import sh.isaac.api.coordinate.PremiseType;
import sh.isaac.api.coordinate.StampPrecedence;
import sh.isaac.api.externalizable.IsaacObjectType;
import sh.isaac.api.util.NumericUtils;
import sh.isaac.api.util.UUIDUtil;
import sh.isaac.model.configuration.LanguageCoordinates;
import sh.isaac.model.configuration.ManifoldCoordinates;
import sh.isaac.model.configuration.StampCoordinates;
import sh.isaac.model.coordinate.ManifoldCoordinateImpl;
import sh.isaac.utility.Frills;

/**
 * 
 * {@link CoordinatesUtil}
 *
 * @author <a href="mailto:joel.kniaz.list@gmail.com">Joel Kniaz</a>
 *
 */
public class CoordinatesUtil
{
	private CoordinatesUtil()
	{
	}

	private static Set<Integer> validModuleNids_ = null;

	private static HashMap<Integer, Set<Integer>> nestedModules_ = new HashMap<>();

	/**
	 * Used to hash CoordinatesToken object and serialized string by request parameters
	 * 
	 * @param params parameter name to value-list map provided in UriInfo by ContainerRequestContext
	 * @return string representation of parameter name to value-list map
	 */
	public static String encodeCoordinateParameters(Map<String, List<String>> params)
	{
		Map<String, List<String>> coordinateParams = getCoordinateParameters(params);

		StringBuilder sb = new StringBuilder(coordinateParams.size() * 32);
		for (Map.Entry<String, List<String>> entry : coordinateParams.entrySet())
		{
			String key = entry.getKey();
			List<String> parameterValueList = entry.getValue();
			Collections.sort(parameterValueList);

			sb.append(key + ':');
			for (int i = 0; i < parameterValueList.size(); ++i)
			{
				sb.append(parameterValueList.get(i));
				if (i < (parameterValueList.size() - 1))
				{
					sb.append(',');
				}
			}
			sb.append(';');
		}

		return sb.toString();
	}

	/**
	 * 
	 * Returns subset of parameter map relevant to CoordinatesToken,
	 * including the coordToken parameter itself
	 * 
	 * @param params parameter name to value-list map provided in UriInfo by ContainerRequestContext
	 * @return subset of parameter map relevant to CoordinatesToken
	 */
	public static Map<String, List<String>> getCoordinateParameters(Map<String, List<String>> params)
	{
		Map<String, List<String>> coordinateParams = new TreeMap<>();

		coordinateParams.putAll(RequestInfoUtils.getParametersSubset(params, RequestParameters.COORDINATE_PARAM_NAMES));

		return coordinateParams;
	}

	/**
	 * 
	 * This method returns an Optional containing a CoordinatesToken object if its parameter exists in the parameters map.
	 * If the parameter exists, it automatically attempts to construct and cache the CoordinatesToken object before returning it
	 *
	 * @param allParams parameter name to value-list map provided in UriInfo by ContainerRequestContext
	 * @return an Optional containing a CoordinatesToken string if it exists in the parameters map
	 * @throws RestException 
	 */
	public static Optional<CoordinatesToken> getCoordinatesTokenParameterTokenObjectValue(Map<String, List<String>> allParams) throws RestException
	{
		Optional<String> tokenStringOptional = getCoordinatesTokenParameterStringValue(allParams);

		if (!tokenStringOptional.isPresent())
		{
			return Optional.empty();
		}
		else
		{
			return Optional.of(CoordinatesTokens.getOrCreate(tokenStringOptional.get()));
		}
	}

	/**
	 * 
	 * This method returns an Optional containing a CoordinatesToken string if it exists in the parameters map.
	 *
	 * @param allParams parameter name to value-list map provided in UriInfo by ContainerRequestContext
	 * @return an Optional containing a CoordinatesToken string if it exists in the parameters map
	 * @throws RestException
	 */
	public static Optional<String> getCoordinatesTokenParameterStringValue(Map<String, List<String>> allParams) throws RestException
	{
		List<String> coordinateTokenParameterValues = allParams.get(RequestParameters.coordToken);

		if (coordinateTokenParameterValues == null || coordinateTokenParameterValues.size() == 0 || StringUtils.isBlank(coordinateTokenParameterValues.get(0)))
		{
			return Optional.empty();
		}
		else if (coordinateTokenParameterValues.size() > 1)
		{
			throw new RestException(RequestParameters.coordToken, "\"" + coordinateTokenParameterValues + "\"",
					"too many (" + coordinateTokenParameterValues.size() + " values - should only be passed with one value");
		}
		return Optional.of(coordinateTokenParameterValues.get(0));
	}

	/**
	 * @param params list of values for parameter. Blank or empty values specify default,
	 *            otherwise valid IFF single boolean-parseable string
	 * @param token underlying token providing default values, if present
	 * @return true if the parameters indicate stated
	 * @throws RestException if contains multiple values or non boolean-parseable non-empty string
	 */
	public static boolean getStatedFromParameter(List<String> params, Optional<CoordinatesToken> token) throws RestException
	{
		boolean defaultValue = token.isPresent() ? (token.get().getTaxonomyType() == PremiseType.STATED)
				: CoordinatesTokens.getDefaultCoordinatesToken().getTaxonomyType() == PremiseType.STATED;

		List<String> statedParamStrs = RequestInfoUtils.expandCommaDelimitedElements(params);

		if (statedParamStrs == null || statedParamStrs.size() == 0)
		{
			return defaultValue;
		}
		else if (statedParamStrs.size() == 1)
		{
			String statedParamStr = statedParamStrs.iterator().next();

			if (StringUtils.isBlank(statedParamStr))
			{
				return defaultValue;
			}

			return Boolean.parseBoolean(statedParamStrs.get(0).trim());
		}

		throw new RestException(RequestParameters.stated, (params != null ? params.toString() : null), "invalid stated/inferred value");
	}

	public static int getLanguageCoordinateLanguageNidFromParameter(List<String> unexpandedLanguageParamStrs, Optional<CoordinatesToken> token)
			throws RestException
	{
		int defaultValue = token.isPresent() ? token.get().getLangCoord() : CoordinatesTokens.getDefaultCoordinatesToken().getLangCoord();

		List<String> languageParamStrs = RequestInfoUtils.expandCommaDelimitedElements(unexpandedLanguageParamStrs);

		if (languageParamStrs == null || languageParamStrs.size() == 0)
		{
			return defaultValue;
		}
		else if (languageParamStrs.size() == 1)
		{
			String languageParamStr = languageParamStrs.iterator().next();

			if (StringUtils.isBlank(languageParamStr))
			{
				return defaultValue;
			}

			Optional<UUID> languageUuidOptional = Optional.empty();
			OptionalInt languageIntIdOptional = NumericUtils.getInt(languageParamStr.trim());
			if (languageIntIdOptional.isPresent())
			{
				int nid = languageIntIdOptional.getAsInt();
				if (Get.identifierService().getObjectTypeForComponent(nid) == IsaacObjectType.CONCEPT)
				{
					if (isChildOf(MetaData.LANGUAGE____SOLOR.getNid(), nid))
					{
						return nid;
					}
				}
			}
			else if ((languageUuidOptional = UUIDUtil.getUUID(languageParamStr.trim())).isPresent())
			{

				if (languageUuidOptional.isPresent() && Get.identifierService().hasUuid(languageUuidOptional.get()) && Get.identifierService()
						.getObjectTypeForComponent(Get.identifierService().getNidForUuids(languageUuidOptional.get())) == IsaacObjectType.CONCEPT)
				{
					int nid = Get.identifierService().getNidForUuids(languageUuidOptional.get());
					if (isChildOf(MetaData.LANGUAGE____SOLOR.getNid(), nid))
					{
						return nid;
					}
				}
			}
			else if (languageParamStr.trim().toLowerCase(Locale.ENGLISH).startsWith("english"))
			{
				return MetaData.ENGLISH_LANGUAGE____SOLOR.getNid();
			}
			else if (languageParamStr.trim().toLowerCase(Locale.ENGLISH).startsWith("spanish"))
			{
				return MetaData.SPANISH_LANGUAGE____SOLOR.getNid();
			}
			else if (languageParamStr.trim().toLowerCase(Locale.ENGLISH).startsWith("french"))
			{
				return MetaData.FRENCH_LANGUAGE____SOLOR.getNid();
			}
			else if (languageParamStr.trim().toLowerCase(Locale.ENGLISH).startsWith("danish"))
			{
				return MetaData.DANISH_LANGUAGE____SOLOR.getNid();
			}
			else if (languageParamStr.trim().toLowerCase(Locale.ENGLISH).startsWith("polish"))
			{
				return MetaData.POLISH_LANGUAGE____SOLOR.getNid();
			}
			else if (languageParamStr.trim().toLowerCase(Locale.ENGLISH).startsWith("dutch"))
			{
				return MetaData.DUTCH_LANGUAGE____SOLOR.getNid();
			}
			else if (languageParamStr.trim().toLowerCase(Locale.ENGLISH).startsWith("lithuanian"))
			{
				return MetaData.LITHUANIAN_LANGUAGE____SOLOR.getNid();
			}
			else if (languageParamStr.trim().toLowerCase(Locale.ENGLISH).startsWith("chinese"))
			{
				return MetaData.CHINESE_LANGUAGE____SOLOR.getNid();
			}
			else if (languageParamStr.trim().toLowerCase(Locale.ENGLISH).startsWith("japanese"))
			{
				return MetaData.JAPANESE_LANGUAGE____SOLOR.getNid();
			}
			else if (languageParamStr.trim().toLowerCase(Locale.ENGLISH).startsWith("swedish"))
			{
				return MetaData.SWEDISH_LANGUAGE____SOLOR.getNid();
			}
		}

		throw new RestException(RequestParameters.language, languageParamStrs.toString(), "Invalid language coordinate language value.  The Language must "
				+ "be a direct child of the concept 'Language (Solor)' - " + MetaData.LANGUAGE____SOLOR.getPrimordialUuid());
	}

	/**
	 * See {@link CoordinateAPIs#getCoordinatesToken(String, String, String, String, String, String, String, String, String, String, String, String, String, String)}
	 * for documentation on how dialects are parsed and handled.
	 * @param unexpandedDialectsStrs the input strings from the user
	 * @param token the current token
	 * @return the default or specified dialect nids as an ordered array
	 * @throws RestException
	 */
	public static int[] getLanguageCoordinateDialectAssemblagePreferenceNidFromParameter(List<String> unexpandedDialectsStrs, Optional<CoordinatesToken> token)
			throws RestException
	{
		int[] defaultValues = token.isPresent() ? token.get().getLangDialects() : CoordinatesTokens.getDefaultCoordinatesToken().getLangDialects();
		List<Integer> nidList = new ArrayList<>();
		
		boolean recurse = false;

		List<String> dialectsStrs = RequestInfoUtils.expandCommaDelimitedElements(unexpandedDialectsStrs);
		if (dialectsStrs != null && dialectsStrs.size() > 0)
		{
			//See if the 'recurse' flag was specified:
			for (String dialectId : dialectsStrs)
			{
				if (StringUtils.isNotBlank(dialectId) && "recurse".equals(dialectId.trim().toLowerCase()))
				{
					recurse = true;
					break;
				}
			}

			//We need to check parents, but have to use defaults, because we haven't yet parsed their entire requested coordinate.
			//This is only checking things in the metadata tree anyway, so stated, latest is likely the best answer.
			TaxonomySnapshot tss = Get.taxonomyService().getSnapshotNoTree(ManifoldCoordinates.getStatedManifoldCoordinate(StampCoordinates.getDevelopmentLatestActiveOnly(), 
					LanguageCoordinates.getUsEnglishLanguageFullySpecifiedNameCoordinate()));
			
			for (String dialectId : dialectsStrs)
			{
				if (StringUtils.isNotBlank(dialectId))
				{
					final String trimedLowerCase = dialectId.trim().toLowerCase(Locale.ENGLISH);
					Optional<UUID> dialectUuidOptional = Optional.empty();
					OptionalInt dialectIdIntIdOptional = NumericUtils.getInt(trimedLowerCase);
					
					int nid = 0;
					
					if (trimedLowerCase.equals("recurse"))
					{
						//already processed this flag
						continue;
					}
					else if (dialectIdIntIdOptional.isPresent())
					{
						nid = dialectIdIntIdOptional.getAsInt();
					}
					else if ((dialectUuidOptional = UUIDUtil.getUUID(trimedLowerCase)).isPresent() && Get.identifierService().hasUuid(dialectUuidOptional.get()))
					{
						nid = Get.identifierService().getNidForUuids(dialectUuidOptional.get());
					}
					else if (trimedLowerCase.startsWith("us"))
					{
						nid = MetaData.US_ENGLISH_DIALECT____SOLOR.getNid();
					}
					else if (trimedLowerCase.startsWith("gb"))
					{
						nid = MetaData.GB_ENGLISH_DIALECT____SOLOR.getNid();
					}
					
					if (nid != 0 && Get.identifierService().getObjectTypeForComponent(nid) == IsaacObjectType.CONCEPT)
					{
						//ensure the nid is a child of Dialect assemblage
						if (tss.isKindOf(nid, MetaData.DIALECT_ASSEMBLAGE____SOLOR.getNid()))
						{
							nidList.add(nid);
						}
						else
						{
							throw new RestException(RequestParameters.dialectPrefs, dialectId, "Invalid language coordinate dialect value - must be a child of "
									+ "Dialect assemblage (SOLOR)");
						}
					}
					else
					{
						throw new RestException(RequestParameters.dialectPrefs, dialectId, "Invalid language coordinate dialect value");
					}
				}
			}
			
			if (recurse)
			{
				//Find all children of each nid in the nidlist, but don't add duplicates.
				NidSet nestedNids = new NidSet();
				for (int nid : nidList)
				{
					Frills.getAllChildrenOfConcept(nid, true, false, tss.getManifoldCoordinate().getStampCoordinate()).forEach(nested -> nestedNids.add(nested));
				}
				for (int nested : nestedNids.asArray())
				{
					if (!nidList.contains(nested))
					{
						nidList.add(nested);
					}
				}
			}
		}

		if (nidList.size() == 0)
		{
			return defaultValues;
		}
		else
		{
			int[] seqArray = new int[nidList.size()];
			for (int i = 0; i < nidList.size(); ++i)
			{
				seqArray[i] = nidList.get(i);
			}
			return seqArray;
		}
	}

	public static int[] getLanguageCoordinateDescriptionTypePreferenceNidsFromParameter(List<String> unexpandedDescTypesStrs, Optional<CoordinatesToken> token)
			throws RestException
	{
		int[] defaultValues = token.isPresent() ? token.get().getLangDescTypePrefs() : CoordinatesTokens.getDefaultCoordinatesToken().getLangDescTypePrefs();

		List<Integer> nidList = new ArrayList<>();

		List<String> descTypesStrs = RequestInfoUtils.expandCommaDelimitedElements(unexpandedDescTypesStrs);

		if (descTypesStrs != null && descTypesStrs.size() > 0)
		{
			for (String descTypeId : descTypesStrs)
			{
				if (StringUtils.isNotBlank(descTypeId))
				{
					Optional<UUID> descTypeUuidOptional = Optional.empty();
					OptionalInt descTypeIdIntIdOptional = NumericUtils.getInt(descTypeId.trim());

					if (descTypeIdIntIdOptional.isPresent())
					{
						int nid = descTypeIdIntIdOptional.getAsInt();
						if (Get.identifierService().getObjectTypeForComponent(nid) == IsaacObjectType.CONCEPT)
						{
							if (isChildOf(MetaData.DESCRIPTION_TYPE____SOLOR.getNid(), nid))
							{
								nidList.add(nid);
								continue;
							}
						}
					}
					else if ((descTypeUuidOptional = UUIDUtil.getUUID(descTypeId.trim())).isPresent())
					{
						if (descTypeUuidOptional.isPresent() && Get.identifierService()
								.getObjectTypeForComponent(Get.identifierService().getNidForUuids(descTypeUuidOptional.get())) == IsaacObjectType.CONCEPT)
						{
							int nid = Get.identifierService().getNidForUuids(descTypeUuidOptional.get());
							if (isChildOf(MetaData.DESCRIPTION_TYPE____SOLOR.getNid(), nid))
							{
								nidList.add(nid);
								continue;
							}
						}
					}
					else if (descTypeId.trim().toLowerCase(Locale.ENGLISH).startsWith("fqn") ||
							descTypeId.trim().toLowerCase(Locale.ENGLISH).startsWith("fsn"))
					{
						nidList.add(MetaData.FULLY_QUALIFIED_NAME_DESCRIPTION_TYPE____SOLOR.getNid());
						continue;
					}
					else if (descTypeId.trim().toLowerCase(Locale.ENGLISH).startsWith("synonym")
							|| descTypeId.trim().toLowerCase(Locale.ENGLISH).startsWith("regular"))
					{
						nidList.add(MetaData.REGULAR_NAME_DESCRIPTION_TYPE____SOLOR.getNid());
						continue;
					}
					else if (descTypeId.trim().toLowerCase(Locale.ENGLISH).startsWith("definition"))
					{
						nidList.add(MetaData.DEFINITION_DESCRIPTION_TYPE____SOLOR.getNid());
						continue;
					}

					throw new RestException(RequestParameters.descriptionTypePrefs, descTypeId, "Invalid language description type value");
				}
			}
		}

		if (nidList.size() == 0)
		{
			return defaultValues;
		}
		else
		{
			int[] seqArray = new int[nidList.size()];
			int i = 0;
			for (int seq : nidList)
			{
				seqArray[i++] = seq;
			}

			return seqArray;
		}
	}

	public static StampPrecedence getStampCoordinatePrecedenceFromParameter(List<String> unexpandedPrecedenceStrs, Optional<CoordinatesToken> token)
			throws RestException
	{
		StampPrecedence defaultValue = token.isPresent() ? token.get().getStampPrecedence()
				: CoordinatesTokens.getDefaultCoordinatesToken().getStampPrecedence();

		List<String> precedenceStrs = RequestInfoUtils.expandCommaDelimitedElements(unexpandedPrecedenceStrs);

		if (precedenceStrs == null || precedenceStrs.size() == 0)
		{
			return defaultValue;
		}
		else if (precedenceStrs.size() == 1)
		{
			String precedenceStr = precedenceStrs.iterator().next();

			if (StringUtils.isBlank(precedenceStr))
			{
				return defaultValue;
			}

			for (StampPrecedence value : StampPrecedence.values())
			{
				if (value.name().equalsIgnoreCase(precedenceStr.trim()) || value.toString().equalsIgnoreCase(precedenceStr.trim()))
				{
					return value;
				}
			}

			OptionalInt stampPrecedenceOrdinalOptional = NumericUtils.getInt(precedenceStr.trim());
			if (stampPrecedenceOrdinalOptional.isPresent())
			{
				for (StampPrecedence value : StampPrecedence.values())
				{
					if (value.ordinal() == stampPrecedenceOrdinalOptional.getAsInt())
					{
						return value;
					}
				}
			}
		}

		throw new RestException("precedence", "\"" + precedenceStrs + "\"", "Invalid stamp coordinate precedence value");
	}

	public static EnumSet<Status> getStampCoordinateAllowedStatesFromParameter(List<String> unexpandedStatesStrs, Optional<CoordinatesToken> token)
			throws RestException
	{
		EnumSet<Status> defaultValues = token.isPresent() ? token.get().getStampStates() : CoordinatesTokens.getDefaultCoordinatesToken().getStampStates();

		EnumSet<Status> allowedStates = EnumSet.allOf(Status.class);
		allowedStates.clear();

		List<String> statesStrs = RequestInfoUtils.expandCommaDelimitedElements(unexpandedStatesStrs);

		if (statesStrs == null || statesStrs.size() == 0)
		{
			return defaultValues; // default
		}
		else
		{
			for (String stateStr : statesStrs)
			{
				if (StringUtils.isNotBlank(stateStr))
				{
					boolean foundMatch = false;
					for (Status value : Status.values())
					{
						if (value.name().equalsIgnoreCase(stateStr.trim()) || value.getAbbreviation().equalsIgnoreCase(stateStr.trim())
								|| value.toString().equalsIgnoreCase(stateStr.trim()))
						{
							allowedStates.add(value);
							foundMatch = true;
							break;
						}
					}

					if (!foundMatch)
					{
						OptionalInt stateOrdinalOptional = NumericUtils.getInt(stateStr.trim());
						if (stateOrdinalOptional.isPresent())
						{
							for (Status value : Status.values())
							{
								if (value.ordinal() == stateOrdinalOptional.getAsInt())
								{
									allowedStates.add(value);
									foundMatch = true;
									break;
								}
							}
						}

						if (!foundMatch)
						{
							throw new RestException("allowedStates", stateStr, "Invalid stamp coordinate state value");
						}
					}
				}
			}

			if (allowedStates.isEmpty())
			{
				return defaultValues;
			}
			else
			{
				return allowedStates;
			}
		}
	}

	public static NidSet getStampCoordinateModuleNidsFromParameter(List<String> unexpandedModulesStrs, Optional<CoordinatesToken> token) throws RestException
	{
		NidSet defaultValue = token.isPresent() ? token.get().getStampModules() : CoordinatesTokens.getDefaultCoordinatesToken().getStampModules();

		NidSet valuesFromParameters = new NidSet();
		List<String> modulesStrs = RequestInfoUtils.expandCommaDelimitedElements(unexpandedModulesStrs);

		if (modulesStrs == null || modulesStrs.size() == 0)
		{
			return defaultValue; // default
		}
		else
		{
			for (String moduleId : modulesStrs)
			{
				if (StringUtils.isNotBlank(moduleId))
				{
					if (validModuleNids_ == null || validModuleNids_.isEmpty())
					{
						// TODO need to determine if this should be a leaf-only read, or if we can read all. I think we will get to leaf only,
						// when we finish refactoring things, but we haven't yet finished refactoring everything that way.
						validModuleNids_ = Frills.getAllChildrenOfConcept(MetaData.MODULE____SOLOR.getNid(), true, false, null);
					}

					OptionalInt moduleIdIntIdOptional = NumericUtils.getInt(moduleId.trim());
					String descForFail = "";
					if (moduleIdIntIdOptional.isPresent())
					{
						int nid = moduleIdIntIdOptional.getAsInt();
						if (Get.identifierService().getObjectTypeForComponent(nid) == IsaacObjectType.CONCEPT)
						{
							if (validModuleNids_.contains(nid))
							{
								valuesFromParameters.add(nid);
								getNestedModules(nid).forEach(value -> valuesFromParameters.add(value));
								continue;
							}
							descForFail = Get.conceptDescriptionText(nid);
						}
					}
					else
					{
						Optional<UUID> moduleUuidOptional = UUIDUtil.getUUID(moduleId.trim());
						if (moduleUuidOptional.isPresent() && Get.identifierService().hasUuid(moduleUuidOptional.get()) && Get.identifierService()
								.getObjectTypeForComponent(Get.identifierService().getNidForUuids(moduleUuidOptional.get())) == IsaacObjectType.CONCEPT)
						{
							int nid = Get.identifierService().getNidForUuids(moduleUuidOptional.get());
							if (validModuleNids_.contains(nid))
							{
								valuesFromParameters.add(nid);
								getNestedModules(nid).forEach(value -> valuesFromParameters.add(value));
								continue;
							}
							descForFail = Get.conceptDescriptionText(nid);
						}
					}

					throw new RestException("modules", "\"" + moduleId + "\"", "Invalid stamp coordinate module value: " + descForFail);
				}
			}
		}

		return valuesFromParameters;
	}

	private static Set<Integer> getNestedModules(int moduleNid)
	{
		Set<Integer> nested = nestedModules_.get(moduleNid);
		if (nested == null)
		{
			// TODO need to determine if this should be a leaf-only read, or if we can read all. I think we will get to leaf only,
			// when we finish refatoring things, but we haven't yet finished refactoring everything that way.
			nested = Frills.getAllChildrenOfConcept(moduleNid, true, false, null);

			nestedModules_.put(moduleNid, nested);
		}
		return nested;
	}

	public static int getStampCoordinatePathNidsFromParameter(List<String> unexpandedPathStrs, Optional<CoordinatesToken> token) throws RestException
	{
		int defaultValue = token.isPresent() ? token.get().getStampPath() : CoordinatesTokens.getDefaultCoordinatesToken().getStampPath();

		List<String> pathStrs = RequestInfoUtils.expandCommaDelimitedElements(unexpandedPathStrs);

		if (pathStrs == null || pathStrs.size() == 0)
		{
			return defaultValue;
		}
		else if (pathStrs.size() == 1)
		{
			String pathStr = pathStrs.iterator().next();

			if (StringUtils.isBlank(pathStr))
			{
				return defaultValue;
			}

			Optional<UUID> pathUuidOptional = Optional.empty();
			OptionalInt pathIntIdOptional = NumericUtils.getInt(pathStr.trim());
			if (pathIntIdOptional.isPresent())
			{
				int nid = pathIntIdOptional.getAsInt();
				if (isChildOf(MetaData.PATH____SOLOR.getNid(), nid))
				{
					return nid;
				}
			}
			else if ((pathUuidOptional = UUIDUtil.getUUID(pathStr.trim())).isPresent())
			{
				if (pathUuidOptional.isPresent() && Get.identifierService().hasUuid(pathUuidOptional.get()) && Get.identifierService()
						.getObjectTypeForComponent(Get.identifierService().getNidForUuids(pathUuidOptional.get())) == IsaacObjectType.CONCEPT)
				{
					int nid = Get.identifierService().getNidForUuids(pathUuidOptional.get());
					if (isChildOf(MetaData.PATH____SOLOR.getNid(), nid))
					{
						return nid;
					}
				}
			}
			else if (pathStr.trim().equalsIgnoreCase("development"))
			{
				return MetaData.DEVELOPMENT_PATH____SOLOR.getNid();
			}
			else if (pathStr.trim().equalsIgnoreCase("master"))
			{
				return MetaData.MASTER_PATH____SOLOR.getNid();
			}
		}

		throw new RestException("path", "\"" + pathStrs + "\"", "Invalid stamp coordinate path value");
	}

	public static long getStampCoordinateTimeFromParameter(List<String> unexpandedTimeStrs, Optional<CoordinatesToken> token) throws RestException
	{
		long defaultValue = token.isPresent() ? token.get().getStampTime() : CoordinatesTokens.getDefaultCoordinatesToken().getStampTime();

		List<String> timeStrs = RequestInfoUtils.expandCommaDelimitedElements(unexpandedTimeStrs);

		if (timeStrs == null || timeStrs.size() == 0)
		{
			return defaultValue; // default
		}
		else if (timeStrs.size() == 1)
		{
			String timeStr = timeStrs.iterator().next();

			if (StringUtils.isBlank(timeStr))
			{
				return defaultValue;
			}

			Optional<Long> longTimeOptional = NumericUtils.getLong(timeStr.trim());
			if (longTimeOptional.isPresent())
			{
				return longTimeOptional.get();
			}

			if (timeStr.trim().equalsIgnoreCase("latest"))
			{
				return Long.MAX_VALUE;
			}
		}

		throw new RestException("time", "\"" + timeStrs + "\"", "invalid stamp coordinate time value");
	}

	public static int getLogicCoordinateStatedAssemblageFromParameter(List<String> unexpandedAssemblageStrs, Optional<CoordinatesToken> token)
			throws RestException
	{
		final int defaultSeq = token.isPresent() ? token.get().getLogicStatedAssemblage()
				: CoordinatesTokens.getDefaultCoordinatesToken().getLogicStatedAssemblage();

		List<String> assemblageStrs = RequestInfoUtils.expandCommaDelimitedElements(unexpandedAssemblageStrs);

		if (assemblageStrs == null || assemblageStrs.size() == 0)
		{
			return defaultSeq; // default
		}
		else if (assemblageStrs.size() == 1)
		{
			String assemblageStr = assemblageStrs.iterator().next();

			if (StringUtils.isBlank(assemblageStr))
			{
				return defaultSeq;
			}

			Optional<UUID> pathUuidOptional = Optional.empty();
			OptionalInt pathIntIdOptional = NumericUtils.getInt(assemblageStr.trim());
			if (pathIntIdOptional.isPresent())
			{
				int nid = pathIntIdOptional.getAsInt();
				if (isChildOf(MetaData.LOGIC_ASSEMBLAGE____SOLOR.getNid(), nid))
				{
					return nid;
				}
			}
			else if ((pathUuidOptional = UUIDUtil.getUUID(assemblageStr.trim())).isPresent())
			{
				if (pathUuidOptional.isPresent() && Get.identifierService().hasUuid(pathUuidOptional.get()) && Get.identifierService()
						.getObjectTypeForComponent(Get.identifierService().getNidForUuids(pathUuidOptional.get())) == IsaacObjectType.CONCEPT)
				{
					int nid = Get.identifierService().getNidForUuids(pathUuidOptional.get());
					if (isChildOf(MetaData.LOGIC_ASSEMBLAGE____SOLOR.getNid(), nid))
					{
						return nid;
					}
				}
			}
		}

		throw new RestException(RequestParameters.logicStatedAssemblage, "\"" + assemblageStrs + "\"", "Invalid logic coordinate stated assemblage value");
	}

	public static int getLogicCoordinateInferredAssemblageFromParameter(List<String> unexpandedAssemblageStrs, Optional<CoordinatesToken> token)
			throws RestException
	{
		final int defaultSeq = token.isPresent() ? token.get().getLogicInferredAssemblage()
				: CoordinatesTokens.getDefaultCoordinatesToken().getLogicInferredAssemblage();

		List<String> assemblageStrs = RequestInfoUtils.expandCommaDelimitedElements(unexpandedAssemblageStrs);

		if (assemblageStrs == null || assemblageStrs.size() == 0)
		{
			return defaultSeq; // default
		}
		else if (assemblageStrs.size() == 1)
		{
			String assemblageStr = assemblageStrs.iterator().next();

			if (StringUtils.isBlank(assemblageStr))
			{
				return defaultSeq;
			}

			Optional<UUID> pathUuidOptional = Optional.empty();
			OptionalInt pathIntIdOptional = NumericUtils.getInt(assemblageStr.trim());
			if (pathIntIdOptional.isPresent())
			{
				int nid = pathIntIdOptional.getAsInt();
				if (isChildOf(MetaData.LOGIC_ASSEMBLAGE____SOLOR.getNid(), nid))
				{
					return nid;
				}
			}
			else if ((pathUuidOptional = UUIDUtil.getUUID(assemblageStr.trim())).isPresent())
			{
				if (pathUuidOptional.isPresent() && Get.identifierService().hasUuid(pathUuidOptional.get()) && Get.identifierService()
						.getObjectTypeForComponent(Get.identifierService().getNidForUuids(pathUuidOptional.get())) == IsaacObjectType.CONCEPT)
				{
					int nid = Get.identifierService().getNidForUuids(pathUuidOptional.get());
					if (isChildOf(MetaData.LOGIC_ASSEMBLAGE____SOLOR.getNid(), nid))
					{
						return nid;
					}
				}
			}
		}

		throw new RestException(RequestParameters.logicInferredAssemblage, "\"" + assemblageStrs + "\"", "Invalid logic coordinate inferred assemblage value");
	}

	public static int getLogicCoordinateDescProfileAssemblageFromParameter(List<String> unexpandedAssemblageStrs, Optional<CoordinatesToken> token)
			throws RestException
	{
		final int defaultSeq = token.isPresent() ? token.get().getLogicDescLogicProfile()
				: CoordinatesTokens.getDefaultCoordinatesToken().getLogicDescLogicProfile();

		List<String> assemblageStrs = RequestInfoUtils.expandCommaDelimitedElements(unexpandedAssemblageStrs);

		if (assemblageStrs == null || assemblageStrs.size() == 0)
		{
			return defaultSeq; // default
		}
		else if (assemblageStrs.size() == 1)
		{
			String assemblageStr = assemblageStrs.iterator().next();

			if (StringUtils.isBlank(assemblageStr))
			{
				return defaultSeq;
			}

			OptionalInt pathIntIdOptional = NumericUtils.getInt(assemblageStr.trim());
			if (pathIntIdOptional.isPresent())
			{
				int nid = pathIntIdOptional.getAsInt();
				if (isChildOf(MetaData.DESCRIPTION_LOGIC_PROFILE____SOLOR.getNid(), nid))
				{
					return nid;
				}
			}

			Optional<UUID> pathUuidOptional = UUIDUtil.getUUID(assemblageStr.trim());
			if (pathUuidOptional.isPresent() && Get.identifierService()
					.getObjectTypeForComponent(Get.identifierService().getNidForUuids(pathUuidOptional.get())) == IsaacObjectType.CONCEPT)
			{
				int nid = Get.identifierService().getNidForUuids(pathUuidOptional.get());
				if (isChildOf(MetaData.DESCRIPTION_LOGIC_PROFILE____SOLOR.getNid(), nid))
				{
					return nid;
				}
			}
		}

		throw new RestException(RequestParameters.descriptionLogicProfile, "\"" + assemblageStrs + "\"",
				"Invalid logic coordinate description profile assemblage value");
	}

	public static int getLogicCoordinateClassifierAssemblageFromParameter(List<String> unexpandedAssemblageStrs, Optional<CoordinatesToken> token)
			throws RestException
	{
		final int defaultSeq = token.isPresent() ? token.get().getLogicClassifier() : CoordinatesTokens.getDefaultCoordinatesToken().getLogicClassifier();

		List<String> assemblageStrs = RequestInfoUtils.expandCommaDelimitedElements(unexpandedAssemblageStrs);

		if (assemblageStrs == null || assemblageStrs.size() == 0)
		{
			return defaultSeq; // default
		}
		else if (assemblageStrs.size() == 1)
		{
			String assemblageStr = assemblageStrs.iterator().next();

			if (StringUtils.isBlank(assemblageStr))
			{
				return defaultSeq;
			}

			OptionalInt pathIntIdOptional = NumericUtils.getInt(assemblageStr.trim());
			if (pathIntIdOptional.isPresent())
			{
				int nid = pathIntIdOptional.getAsInt();
				if (isChildOf(MetaData.DESCRIPTION_LOGIC_CLASSIFIER____SOLOR.getNid(), nid))
				{
					return nid;
				}
			}
			else
			{
				Optional<UUID> pathUuidOptional = UUIDUtil.getUUID(assemblageStr.trim());
				if (pathUuidOptional.isPresent() && Get.identifierService().hasUuid(pathUuidOptional.get()) && Get.identifierService()
						.getObjectTypeForComponent(Get.identifierService().getNidForUuids(pathUuidOptional.get())) == IsaacObjectType.CONCEPT)
				{
					int nid = Get.identifierService().getNidForUuids(pathUuidOptional.get());
					if (isChildOf(MetaData.DESCRIPTION_LOGIC_CLASSIFIER____SOLOR.getNid(), nid))
					{
						return nid;
					}
				}
			}
		}

		throw new RestException(RequestParameters.classifier, "\"" + assemblageStrs + "\"", "Invalid logic coordinate classifier assemblage value");
	}

	private static boolean isChildOf(int parentNid, int childNid)
	{
		TaxonomySnapshot tss = Get.taxonomyService()
				.getSnapshotNoTree(new ManifoldCoordinateImpl(StampCoordinates.getDevelopmentLatest(), 
						Get.configurationService().getGlobalDatastoreConfiguration().getDefaultLanguageCoordinate()));

		for (int child : tss.getTaxonomyChildConceptNids(parentNid))
		{
			if (child == childNid)
			{
				return true;
			}
		}
		return false;
	}
	
	public static void clearCache()
	{
		nestedModules_.clear();
		if (validModuleNids_ != null)
		{
			validModuleNids_.clear();
		}
	}
}
