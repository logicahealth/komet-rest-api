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

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.mahout.math.Arrays;

import net.sagebits.tmp.isaac.rest.api.exceptions.RestException;

/**
 * 
 * {@link RequestParameters}
 *
 * @author <a href="mailto:joel.kniaz.list@gmail.com">Joel Kniaz</a>
 *
 *         All parameters should be added to ALL_VALID_PARAMETERS, whether grouped or individually
 * 
 */
public class RequestParameters {
	private RequestParameters() {}
	
	// CoordinatesToken
	public final static String ssoToken = "ssoToken";

	// CoordinatesToken
	public final static String coordToken = "coordToken";
	
	// Taxonomy Coordinate
	public final static String stated = "stated";

	// Language Coordinate
	public final static String language = "language";
	public final static String dialectPrefs = "dialectPrefs";
	public final static String descriptionTypePrefs = "descriptionTypePrefs";
	public final static Set<String> LANGUAGE_COORDINATE_PARAM_NAMES =
			unmodifiableSet(language, dialectPrefs, descriptionTypePrefs);

	// Stamp Coordinate
	public final static String time = "time";
	public final static String path = "path";
	public final static String precedence = "precedence";
	public final static String modules = "modules";
	public final static String allowedStates = "allowedStates";
	public final static Set<String> STAMP_COORDINATE_PARAM_NAMES =
			unmodifiableSet(
					time,
					path,
					precedence,
					modules,
					allowedStates);

	// Logic Coordinate
	public final static String logicStatedAssemblage = "logicStatedAssemblage";
	public final static String logicInferredAssemblage = "logicInferredAssemblage";
	public final static String descriptionLogicProfile = "descriptionLogicProfile";
	public final static String classifier = "classifier";
	public final static Set<String> LOGIC_COORDINATE_PARAM_NAMES =
			unmodifiableSet(
					logicStatedAssemblage,
					logicInferredAssemblage,
					descriptionLogicProfile,
					classifier);
	
	// All Coordinates
	public final static Set<String> COORDINATE_PARAM_NAMES;
	static
	{
		Set<String> params = new HashSet<>();
		params.add(coordToken);
		params.add(stated);
		params.addAll(LANGUAGE_COORDINATE_PARAM_NAMES);
		params.addAll(STAMP_COORDINATE_PARAM_NAMES);
		params.addAll(LOGIC_COORDINATE_PARAM_NAMES);

		COORDINATE_PARAM_NAMES = Collections.unmodifiableSet(params);
	}
	
	// This parameter set excludes time and allowedStates, as these are hardcoded for updates
	public final static Set<String> UPDATE_COORDINATE_PARAM_NAMES;
	static
	{
		Set<String> params = new HashSet<>();
		params.add(coordToken);
		params.add(stated);
		params.addAll(LANGUAGE_COORDINATE_PARAM_NAMES);
		params.addAll(LOGIC_COORDINATE_PARAM_NAMES);
		params.add(path);
		params.add(precedence);
		params.add(modules);

		UPDATE_COORDINATE_PARAM_NAMES = Collections.unmodifiableSet(params);
	}
	
	public final static String active = "active";

	public final static String id = "id";
	public final static String nid = "nid";
	
	public final static String objType = "objType";
	
	public final static String availableOnly = "availableOnly";
	
	// Expandables
	public final static String expand = "expand";
	public final static String returnExpandableLinks = "expandables";

	// Pagination
	public final static String pageNum = "pageNum";
	public final static String pageNumDefault = "1";
	public final static String maxPageSize = "maxPageSize";
	public final static String maxPageSizeDefault = "10";
	public final static Set<String> PAGINATION_PARAM_NAMES = unmodifiableSet(pageNum, maxPageSize);
	
	public final static String assemblage = "assemblage";
	public final static String includeDescriptions = "includeDescriptions";
	public final static String includeAssociations = "includeAssociations";
	public final static String includeMappings = "includeMappings";
	public final static String includeAttributes = "includeAttributes";
	public final static String includeAttributesDefault = "true";

	public final static String query = "query";
	public final static String treatAsString = "treatAsString";
	public final static String descriptionTypes = "descriptionTypes";
	public final static String extendedDescriptionTypes = "extendedDescriptionTypes";
	public final static String dynamicSemanticColumns = "dynamicSemanticColumns";
	public final static String semanticAssemblageId = "semanticAssemblageId";
	public final static String restrictTo = "restrictTo";
	public final static String mergeOnConcept = "mergeOnConcept";
	
	// Taxonomy
	public final static String childDepth = "childDepth";
	public final static String parentHeight = "parentHeight";
	public final static String countParents = "countParents";
	public final static String countChildren = "countChildren";
	public final static String semanticMembership = "semanticMembership";
	public final static String terminologyType = "terminologyType";
	
	// Concept
	public final static String includeParents = "includeParents";
	public final static String includeChildren = "includeChildren";
	
	// IdAPIs
	// Parsable individual or comma-delimited list of IdType enum values: i.e.: uuid, nid, sctid, vuid
	public final static String inputType = "inputType";
	public final static String outputType = "outputType";
	public final static String altId = "altId";

	// Workflow
	public final static String definitionId = "definitionId"; // UUID string
	public final static String processId = "processId"; // UUID string
	public final static String userId = "userId"; // UUID string
	public final static String acquireLock = "acquireLock"; // string, "true" or "false"
	public final static String status = "status"; //String, workflow status
	public final static String includeActive = "includeActive";
	public final static String includeCompleted = "includeCompleted";
	public final static String includeCanceled = "includeCanceled";
	
	// Edit Token
	public final static String editToken = "editToken";
	public final static String editPath = "editPath";
	public final static String editModule = "editModule";
	public final static Set<String> EDIT_TOKEN_PARAM_NAMES =
			unmodifiableSet(
					editToken,
					editModule,
					editPath);
	
	//export
	public final static String changedAfter = "changedAfter";
	public final static String changedBefore = "changedBefore";
	
	public final static String field = "field";

	// VUID
	public final static String blockSize = "blockSize";
	public final static String reason = "reason";
	public final static String vuid = "vuid";
	public final static Set<String> VUID_PARAM_NAMES =
			unmodifiableSet(
					ssoToken,
					blockSize,
					reason,
					vuid);
		
	public final static String vuidGeneration = "vuidGeneration";
	
	
	//Validation calls
	public final static String fqn = "fqn";
	public final static String parentConceptId = "parentConceptId";
	
	
	//classification
	public final static String defaultModule = "defaultModule";
	public final static String largeResults = "largeResults";
	
	
	/**
	 * Set of all known parameters usable to detect malformed or incorrect parameters
	 */
	public final static Set<String> ALL_VALID_PARAMETERS;
	static
	{
		Set<String> params = new HashSet<>();
		params.addAll(COORDINATE_PARAM_NAMES);
		params.addAll(PAGINATION_PARAM_NAMES);
		params.addAll(EDIT_TOKEN_PARAM_NAMES);
		params.addAll(VUID_PARAM_NAMES);
		params.addAll(unmodifiableSet(
			expand,
			ssoToken,
				
			id,
			nid,

			assemblage,
			includeDescriptions,
			includeAttributes,

			query,
			treatAsString,
			descriptionTypes,
			extendedDescriptionTypes,
			dynamicSemanticColumns,
			semanticAssemblageId,

			// Taxonomy
			childDepth,
			parentHeight,
			countParents,

			countChildren,
			semanticMembership,
			
			// Concept
			includeParents,
			includeChildren,
			
			//IdAPIs
			inputType,
			outputType,
			altId,
			
			active,
			
			//workflow
			definitionId,
			processId,
			userId,
			acquireLock,
			status,
			includeActive,
			includeCompleted,
			includeCanceled,
			
			field,
			
			vuidGeneration,
			
			fqn,
			defaultModule,
			largeResults,
			parentConceptId
			
			));
		ALL_VALID_PARAMETERS = params;
	}

	// Parameter default constants
	public final static String ISAAC_ROOT_UUID = "7c21b6c5-cf11-5af9-893b-743f004c97f5";

	/**
	 * This should only be modified for testing purposes. Otherwise should always be IGNORE_CASE_VALIDATING_PARAM_NAMES_DEFAULT
	 * 
	 * Changing this to ignore case will probably break lots of things,
	 * as most comparisons do not ignore case, as currenlt coded
	 */
	public final static boolean IGNORE_CASE_VALIDATING_PARAM_NAMES_DEFAULT = false;
	public static boolean IGNORE_CASE_VALIDATING_PARAM_NAMES = IGNORE_CASE_VALIDATING_PARAM_NAMES_DEFAULT;

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
		supportedParameterNamesSet.add(returnExpandableLinks);
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
			String parameterNameToCompare = IGNORE_CASE_VALIDATING_PARAM_NAMES ? parameterName.toUpperCase(Locale.ENGLISH) : parameterName;
			boolean foundMatch = false;
			for (String supportedParameterName : supportedParameterNamesSet)
			{
				String supportedParameterNameToCompare = IGNORE_CASE_VALIDATING_PARAM_NAMES ? supportedParameterName.toUpperCase(Locale.ENGLISH)
						: supportedParameterName;
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
