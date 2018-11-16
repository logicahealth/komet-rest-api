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

package net.sagebits.tmp.isaac.rest.api1.concept;

import java.util.ArrayList;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Semaphore;
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
import net.sagebits.tmp.isaac.rest.ExpandUtil;
import net.sagebits.tmp.isaac.rest.Util;
import net.sagebits.tmp.isaac.rest.api.exceptions.RestException;
import net.sagebits.tmp.isaac.rest.api1.RestPaths;
import net.sagebits.tmp.isaac.rest.api1.data.concept.RestConceptChronology;
import net.sagebits.tmp.isaac.rest.api1.data.concept.RestConceptVersion;
import net.sagebits.tmp.isaac.rest.api1.data.semantic.RestSemanticDescriptionVersion;
import net.sagebits.tmp.isaac.rest.api1.data.semantic.RestSemanticVersion;
import net.sagebits.tmp.isaac.rest.api1.semantic.SemanticAPIs;
import net.sagebits.tmp.isaac.rest.session.RequestInfo;
import net.sagebits.tmp.isaac.rest.session.RequestParameters;
import sh.isaac.MetaData;
import sh.isaac.api.Get;
import sh.isaac.api.chronicle.LatestVersion;
import sh.isaac.api.component.concept.ConceptChronology;
import sh.isaac.api.component.concept.ConceptService;
import sh.isaac.api.component.concept.ConceptVersion;
import sh.isaac.api.util.NumericUtils;
import sh.isaac.api.util.UUIDUtil;
import sh.isaac.utility.Frills;

/**
 * {@link ConceptAPIs}
 *
 * @author <a href="mailto:daniel.armbrust.list@sagebits.net">Dan Armbrust</a>
 */
@Path(RestPaths.conceptAPIsPathComponent)
public class ConceptAPIs
{
	private static Logger log = LogManager.getLogger();

	@Context
	private SecurityContext securityContext;

	// TODO this cache needs to be invalidated if they change
	private Set<Integer> allDescriptionAssemblageTypes = null;
	
	private Semaphore s = new Semaphore(1);

	/**
	 * Returns a single version of a concept.
	 * If no version parameter is specified, returns the latest version.
	 * 
	 * @param id - A UUID or nid
	 * @param includeParents - Include the direct parent concepts of the requested concept in the response. Defaults to false.
	 * @param countParents - true to count the number of parents above this node. May be used with or without the includeParents parameter
	 *            - it works independently. When used in combination with the parentHeight parameter, only the last level of items returned will
	 *            return
	 *            parent counts. This parameter also applies to the expanded children - if childDepth is requested, and countParents is set, this will
	 *            return a count of parents of each child, which can be used to determine if a child has multiple parents. Defaults to false if not
	 *            provided.
	 * @param includeChildren - Include the direct child concepts of the request concept in the response. Defaults to false.
	 * @param countChildren - true to count the number of children below this node. May be used with or without the includeChildren parameter
	 *            - it works independently. When used in combination with the childDepth parameter, only the last level of items returned will return
	 *            child counts. Defaults to false.
	 * @param semanticMembership - when true, the semanticMembership field of the RestConceptVersion object will be populated with the set of unique
	 *            concept nids that describe semantics that this concept is referenced by. (there exists a semantic instance where the
	 *            referencedComponent
	 *            is the RestConceptVersion being returned here, then the value of the assemblage is also included in the RestConceptVersion)
	 *            This will not include the membership information for any assemblage of type logic graph or descriptions.
	 * @param terminologyType - when true, the concept nids of the terminologies that this concept is part of on the current stamp are returned.
	 *            This is determined by whether or not there is version of this concept present with a module that extends from one of the children of
	 *            the
	 *            {@link MetaData#MODULE____SOLOR} concepts. This is returned as an array, as a concept may exist in multiple terminologies at the same time.
	 *            Note that this takes into account the current view coordinate, so it will not return membership in terminologies (even if present)
	 *            if the terminology
	 *            module is excluded from the view coordinate.
	 * @param expand - comma separated list of fields to expand. Supports 'chronology'
	 * @param processId if set, specifies that retrieved components should be checked against the specified active
	 *            workflow process, and if existing in the process, only the version of the corresponding object prior to the version referenced
	 *            in the workflow process should be returned or referenced. If no version existed prior to creation of the workflow process,
	 *            then either no object will be returned or an exception will be thrown, depending on context.
	 * @param coordToken specifies an explicit serialized CoordinatesToken string specifying all coordinate parameters. A CoordinatesToken may be
	 *            obtained by a separate (prior) call to getCoordinatesToken().
	 *
	 * @return the concept version object
	 * @throws RestException
	 */
	@GET
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Path(RestPaths.versionComponent + "{" + RequestParameters.id + "}")
	public RestConceptVersion getConceptVersion(@PathParam(RequestParameters.id) String id,
			@QueryParam(RequestParameters.includeParents) @DefaultValue("false") String includeParents,
			@QueryParam(RequestParameters.countParents) @DefaultValue("false") String countParents,
			@QueryParam(RequestParameters.includeChildren) @DefaultValue("false") String includeChildren,
			@QueryParam(RequestParameters.countChildren) @DefaultValue("false") String countChildren,
			@QueryParam(RequestParameters.semanticMembership) @DefaultValue("false") String semanticMembership,
			@QueryParam(RequestParameters.terminologyType) @DefaultValue("false") String terminologyType, @QueryParam(RequestParameters.expand) String expand,
			@QueryParam(RequestParameters.processId) String processId, @QueryParam(RequestParameters.coordToken) String coordToken) throws RestException
	{
		RequestParameters.validateParameterNamesAgainstSupportedNames(RequestInfo.get().getParameters(), RequestParameters.id, RequestParameters.includeParents,
				RequestParameters.countParents, RequestParameters.includeChildren, RequestParameters.countChildren, RequestParameters.semanticMembership,
				RequestParameters.terminologyType, RequestParameters.expand, RequestParameters.processId, RequestParameters.COORDINATE_PARAM_NAMES);

		UUID processIdUUID = Util.validateWorkflowProcess(processId);

		ConceptChronology concept = findConceptChronology(id);
		LatestVersion<ConceptVersion> cv = concept.getLatestVersion(Util.getPreWorkflowStampCoordinate(processIdUUID, concept.getNid()));
		if (cv.isPresent())
		{
			// TODO handle contradictions
			Util.logContradictions(log, cv);
			return new RestConceptVersion(cv.get(), RequestInfo.get().shouldExpand(ExpandUtil.chronologyExpandable),
					Boolean.parseBoolean(includeParents.trim()), Boolean.parseBoolean(countParents.trim()), Boolean.parseBoolean(includeChildren.trim()),
					Boolean.parseBoolean(countChildren.trim()), RequestInfo.get().getStated(), Boolean.parseBoolean(semanticMembership.trim()),
					Boolean.parseBoolean(terminologyType.trim()), processIdUUID);
		}
		throw new RestException(RequestParameters.id, id, "No version on coordinate path for concept with the specified id");
	}

	/**
	 * Returns the chronology of a concept.
	 * 
	 * @param id - A UUID, or nid
	 * @param expand - comma separated list of fields to expand. Supports 'versionsAll', 'versionsLatestOnly'
	 *            If latest only is specified in combination with versionsAll, it is ignored (all versions are returned)
	 * @param terminologyType - when true, the concept nids of the terminologies that this concept is part of on any stamp is returned. This
	 *            is determined by whether or not there is version of this concept present with a module that extends from one of the children of the
	 *            {@link MetaData#MODULE____SOLOR} concepts. This is returned as an array, as a concept may exist in multiple terminologies at the same time.
	 *            This is calculated WITHOUT taking into account view coordinates, or active / inactive states of the concept.
	 * @param processId if set, specifies that retrieved components should be checked against the specified active
	 *            workflow process, and if existing in the process, only the version of the corresponding object prior to the version referenced
	 *            in the workflow process should be returned or referenced. If no version existed prior to creation of the workflow process,
	 *            then either no object will be returned or an exception will be thrown, depending on context.
	 * @param coordToken specifies an explicit serialized CoordinatesToken string specifying all coordinate parameters. A CoordinatesToken may be
	 *            obtained by a separate (prior) call to getCoordinatesToken().
	 * @return the concept chronology object
	 * @throws RestException
	 */
	@GET
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Path(RestPaths.chronologyComponent + "{" + RequestParameters.id + "}")
	public RestConceptChronology getConceptChronology(@PathParam(RequestParameters.id) String id, @QueryParam(RequestParameters.expand) String expand,
			@QueryParam(RequestParameters.terminologyType) @DefaultValue("false") String terminologyType,
			@QueryParam(RequestParameters.processId) String processId, @QueryParam(RequestParameters.coordToken) String coordToken) throws RestException
	{
		RequestParameters.validateParameterNamesAgainstSupportedNames(RequestInfo.get().getParameters(), RequestParameters.id, RequestParameters.expand,
				RequestParameters.processId, RequestParameters.terminologyType, RequestParameters.COORDINATE_PARAM_NAMES);

		ConceptChronology concept = findConceptChronology(id);

		RestConceptChronology chronology = new RestConceptChronology(concept, RequestInfo.get().shouldExpand(ExpandUtil.versionsAllExpandable),
				RequestInfo.get().shouldExpand(ExpandUtil.versionsLatestOnlyExpandable), Boolean.parseBoolean(terminologyType.trim()),
				Util.validateWorkflowProcess(processId));

		return chronology;
	}

	public static ConceptChronology findConceptChronology(String id) throws RestException
	{
		ConceptService conceptService = Get.conceptService();
		OptionalInt intId = NumericUtils.getInt(id);
		if (intId.isPresent())
		{
			Optional<? extends ConceptChronology> c = conceptService.getOptionalConcept(intId.getAsInt());
			if (c.isPresent())
			{
				return c.get();
			}
			else
			{
				throw new RestException(RequestParameters.id, id, "No concept is available with the specified id \"" + id + "\"");
			}
		}
		else
		{
			Optional<UUID> uuidId = UUIDUtil.getUUID(id);
			if (uuidId.isPresent())
			{
				Optional<? extends ConceptChronology> c = conceptService.getOptionalConcept(uuidId.get());
				if (c.isPresent())
				{
					return c.get();
				}
				else
				{
					throw new RestException(RequestParameters.id, id, "No concept is available with the specified id");
				}
			}
			else
			{
				throw new RestException(RequestParameters.id, id, "Is not a concept identifier.  Must be a UUID or an integer");
			}
		}
	}

	/**
	 * @param id - A UUID or nid of a CONCEPT
	 * @param includeAttributes - true to include the (nested) attributes, which includes the dialect information, false to ommit
	 *            Dialects and other types of attributes will be returned in different structures - all attributes that represent dialects will
	 *            be in the RestSemanticDescriptionVersion object, in the dialects fields, while any other type of attribute will be in the
	 *            RestSemanticVersion in the nestedAttributes field.
	 * @param expand - A comma separated list of fields to expand. Supports 'referencedDetails'.
	 *            When referencedDetails is passed, nids will include type information, and certain nids will also include their descriptions,
	 *            if they represent a concept or a description semantic.
	 * @param processId if set, specifies that retrieved components should be checked against the specified active
	 *            workflow process, and if existing in the process, only the version of the corresponding object prior to the version referenced
	 *            in the workflow process should be returned or referenced. If no version existed prior to creation of the workflow process,
	 *            then either no object will be returned or an exception will be thrown, depending on context.
	 * @param coordToken specifies an explicit serialized CoordinatesToken string specifying all coordinate parameters. A CoordinatesToken may be
	 *            obtained by a separate (prior) call to getCoordinatesToken().
	 * 
	 * @return The descriptions associated with the concept
	 * @throws RestException
	 */
	@GET
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Path(RestPaths.descriptionsComponent + "{" + RequestParameters.id + "}")
	public RestSemanticDescriptionVersion[] getDescriptions(@PathParam(RequestParameters.id) String id,
			@QueryParam(RequestParameters.includeAttributes) @DefaultValue(RequestParameters.includeAttributesDefault) String includeAttributes,
			@QueryParam(RequestParameters.expand) String expand, @QueryParam(RequestParameters.processId) String processId,
			@QueryParam(RequestParameters.coordToken) String coordToken) throws RestException
	{
		RequestParameters.validateParameterNamesAgainstSupportedNames(RequestInfo.get().getParameters(), RequestParameters.id,
				RequestParameters.includeAttributes, RequestParameters.processId, RequestParameters.expand, RequestParameters.COORDINATE_PARAM_NAMES);

		ArrayList<RestSemanticDescriptionVersion> result = new ArrayList<>();
		RestSemanticVersion[] descriptions = SemanticAPIs.get(findConceptChronology(id).getNid() + "", getAllDescriptionTypes(), null, true,
				Boolean.parseBoolean(includeAttributes.trim()), RequestInfo.get().shouldExpand(ExpandUtil.referencedDetails), true, false, false,
				Util.validateWorkflowProcess(processId));

		for (RestSemanticVersion d : descriptions)
		{
			// This cast is expected to be safe, if not, the data model is messed up
			if (!(d instanceof RestSemanticDescriptionVersion))
			{
				log.warn("SemanticAPIs.get(...) didn't filter properly (encountered " + d.getClass().getName() + ")!  Is the DB broken again?");
			}
			else
			{
				result.add((RestSemanticDescriptionVersion) d);
			}
		}
		return result.toArray(new RestSemanticDescriptionVersion[result.size()]);
	}

	private Set<Integer> getAllDescriptionTypes()
	{
		if (allDescriptionAssemblageTypes == null)
		{
			try
			{
				s.acquire();
				if (allDescriptionAssemblageTypes == null)
				{
					allDescriptionAssemblageTypes = Frills.getAllChildrenOfConcept(MetaData.DESCRIPTION_ASSEMBLAGE____SOLOR.getNid(), false, true, 
						null);
				}
			}
			catch (InterruptedException e)
			{
				return getAllDescriptionTypes();
			}
			s.release();
		}
		return allDescriptionAssemblageTypes;
	}
}
