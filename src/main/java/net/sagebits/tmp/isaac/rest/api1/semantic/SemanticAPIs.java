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

package net.sagebits.tmp.isaac.rest.api1.semantic;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.PrimitiveIterator;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;
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
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.sagebits.tmp.isaac.rest.ExpandUtil;
import net.sagebits.tmp.isaac.rest.Util;
import net.sagebits.tmp.isaac.rest.api.data.PaginationUtils;
import net.sagebits.tmp.isaac.rest.api.exceptions.RestException;
import net.sagebits.tmp.isaac.rest.api1.RestPaths;
import net.sagebits.tmp.isaac.rest.api1.data.enumerations.RestSemanticType;
import net.sagebits.tmp.isaac.rest.api1.data.semantic.RestDynamicSemanticDefinition;
import net.sagebits.tmp.isaac.rest.api1.data.semantic.RestSemanticChronology;
import net.sagebits.tmp.isaac.rest.api1.data.semantic.RestSemanticVersion;
import net.sagebits.tmp.isaac.rest.api1.data.semantic.RestSemanticVersionPage;
import net.sagebits.tmp.isaac.rest.session.RequestInfo;
import net.sagebits.tmp.isaac.rest.session.RequestInfoUtils;
import net.sagebits.tmp.isaac.rest.session.RequestParameters;
import net.sagebits.uts.auth.data.UserRole.SystemRoleConstants;
import sh.isaac.api.AssemblageService;
import sh.isaac.api.Get;
import sh.isaac.api.chronicle.LatestVersion;
import sh.isaac.api.chronicle.VersionType;
import sh.isaac.api.collections.NidSet;
import sh.isaac.api.component.semantic.SemanticChronology;
import sh.isaac.api.component.semantic.version.SemanticVersion;
import sh.isaac.api.util.NumericUtils;
import sh.isaac.api.util.UUIDUtil;
import sh.isaac.misc.associations.AssociationUtilities;
import sh.isaac.model.semantic.DynamicUsageDescriptionImpl;
import sh.isaac.utility.Frills;

/**
 * {@link SemanticAPIs}
 *
 * @author <a href="mailto:daniel.armbrust.list@sagebits.net">Dan Armbrust</a>
 */
@Path(RestPaths.semanticAPIsPathComponent)
@RolesAllowed({ SystemRoleConstants.AUTOMATED, SystemRoleConstants.ADMINISTRATOR, SystemRoleConstants.SYSTEM_MANAGER, SystemRoleConstants.CONTENT_MANAGER,
	SystemRoleConstants.EDITOR, SystemRoleConstants.READ })
public class SemanticAPIs
{
	private static Logger log = LogManager.getLogger(SemanticAPIs.class);

	@Context
	private SecurityContext securityContext;

	/**
	 * Return the RestSemanticType of the semantic corresponding to the passed id
	 * 
	 * @param id The id for which to determine RestSemanticType
	 *            If an int then assumed to be a semantic NID
	 *            If a String then parsed and handled as a semantic UUID
	 * @param coordToken specifies an explicit serialized CoordinatesToken string specifying all coordinate parameters. A
	 *            CoordinatesToken may be obtained by a separate (prior) call to getCoordinatesToken().
	 * 
	 * @return RestSemanticType of the semantic corresponding to the passed id. if no corresponding semantic found a RestException is thrown.
	 * @throws RestException
	 */
	@GET
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Path(RestPaths.semanticTypeComponent + "{" + RequestParameters.id + "}")
	public RestSemanticType getVersionType(@PathParam(RequestParameters.id) String id, @QueryParam(RequestParameters.coordToken) String coordToken)
			throws RestException
	{
		RequestParameters.validateParameterNamesAgainstSupportedNames(RequestInfo.get().getParameters(), RequestParameters.id,
				RequestParameters.COORDINATE_PARAM_NAMES);

		OptionalInt intId = NumericUtils.getInt(id);
		if (intId.isPresent())
		{
			if (Get.assemblageService().hasSemantic(intId.getAsInt()))
			{
				return new RestSemanticType(Get.assemblageService().getSemanticChronology(intId.getAsInt()).getVersionType());
			}
			else
			{
				throw new RestException(RequestParameters.id, id, "Specified semantic int id NID does not correspond to"
						+ " an existing semantic chronology. Must pass a UUID or integer NID that corresponds to an existing semantic chronology.");
			}
		}
		else
		{
			Optional<UUID> uuidId = UUIDUtil.getUUID(id);
			if (uuidId.isPresent())
			{
				// id is uuid
				Integer semanticNid = null;
				if (Get.identifierService().hasUuid(uuidId.get()) && (semanticNid = Get.identifierService().getNidForUuids(uuidId.get())) != 0
						&& Get.assemblageService().hasSemantic(semanticNid))
				{
					return new RestSemanticType(Get.assemblageService().getSemanticChronology(semanticNid).getVersionType());
				}
				else
				{
					throw new RestException(RequestParameters.id, id,
							"Specified semantic UUID does not correspond to an existing semantic chronology. Must pass a "
									+ "UUID or integer NID that corresponds to an existing semantic chronology.");
				}
			}
			else
			{
				throw new RestException(RequestParameters.id, id,
						"Specified semantic string id is not a valid UUID identifier.  Must be a UUID, or integer NID");
			}
		}
	}

	/**
	 * Returns the chronology of a semantic.
	 * 
	 * @param id - A UUID or nid of a semantic
	 * @param expand - A comma separated list of fields to expand. Supports 'versionsAll', 'versionsLatestOnly', 'nestedSemantics',
	 *            'referencedDetails'
	 *            If latest only is specified in combination with versionsAll, it is ignored (all versions are returned)
	 *            'referencedDetails' causes it to include the type for the referencedComponent, and, if it is a concept or a description semantic,
	 *            the description of that
	 *            concept - or the description value.
	 * @param processId if set, specifies that retrieved components should be checked against the specified active
	 *            workflow process, and if existing in the process, only the version of the corresponding object prior to the version referenced
	 *            in the workflow process should be returned or referenced. If no version existed prior to creation of the workflow process,
	 *            then either no object will be returned or an exception will be thrown, depending on context.
	 * @param coordToken specifies an explicit serialized CoordinatesToken string specifying all coordinate parameters. A CoordinatesToken may be
	 *            obtained
	 *            by a separate (prior) call to getCoordinatesToken().
	 * @param altId - (optional) the altId type(s) to populate in any returned RestIdentifiedObject structures.  By default, no alternate IDs are 
	 *     returned.  This can be set to one or more names or ids from the /1/id/types or the value 'ANY'.  Requesting IDs that are unneeded will harm 
	 *     performance. 
	 * 
	 * @return the semantic chronology object
	 * @throws RestException
	 */
	@GET
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Path(RestPaths.chronologyComponent + "{" + RequestParameters.id + "}")
	public RestSemanticChronology getSemanticChronology(@PathParam(RequestParameters.id) String id, @QueryParam(RequestParameters.expand) String expand,
			@QueryParam(RequestParameters.processId) String processId, @QueryParam(RequestParameters.coordToken) String coordToken,
			@QueryParam(RequestParameters.altId) String altId) throws RestException
	{
		RequestParameters.validateParameterNamesAgainstSupportedNames(RequestInfo.get().getParameters(), RequestParameters.id, RequestParameters.expand,
				RequestParameters.processId, RequestParameters.COORDINATE_PARAM_NAMES, RequestParameters.altId);

		RestSemanticChronology chronology = new RestSemanticChronology(findSemanticChronology(id),
				RequestInfo.get().shouldExpand(ExpandUtil.versionsAllExpandable), RequestInfo.get().shouldExpand(ExpandUtil.versionsLatestOnlyExpandable),
				RequestInfo.get().shouldExpand(ExpandUtil.nestedSemanticsExpandable), RequestInfo.get().shouldExpand(ExpandUtil.referencedDetails),
				Util.validateWorkflowProcess(processId));

		return chronology;
	}

	/**
	 * Returns a single version of a semantic.
	 * If no version parameter is specified, returns the latest version.
	 * 
	 * @param id - A UUID or nid of a semantic 
	 * @param expand - comma separated list of fields to expand. Supports 'chronology', 'nestedSemantics', 'referencedDetails'
	 *            When referencedDetails is passed, nids will include type information, and certain nids will also include their descriptions,
	 *            if they represent a concept or a description semantic.
	 * @return the semantic version object. Note that the returned type here - RestSemanticVersion is actually an abstract base class,
	 *         the actual return type will be either a RestDynamicSemanticVersion or a RestSemanticDescriptionVersion.
	 * @param processId if set, specifies that retrieved components should be checked against the specified active
	 *            workflow process, and if existing in the process, only the version of the corresponding object prior to the version referenced
	 *            in the workflow process should be returned or referenced. If no version existed prior to creation of the workflow process,
	 *            then either no object will be returned or an exception will be thrown, depending on context.
	 * @param coordToken specifies an explicit serialized CoordinatesToken string specifying all coordinate parameters. A CoordinatesToken may
	 *            be obtained by a separate (prior) call to getCoordinatesToken().
	 * @param altId - (optional) the altId type(s) to populate in any returned RestIdentifiedObject structures.  By default, no alternate IDs are 
	 *     returned.  This can be set to one or more names or ids from the /1/id/types or the value 'ANY'.  Requesting IDs that are unneeded will harm 
	 *     performance. 
	 * 
	 * @throws RestException
	 */
	@GET
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Path(RestPaths.versionComponent + "{" + RequestParameters.id + "}")
	public RestSemanticVersion getSemanticVersion(@PathParam(RequestParameters.id) String id, @QueryParam(RequestParameters.expand) String expand,
			@QueryParam(RequestParameters.processId) String processId, @QueryParam(RequestParameters.coordToken) String coordToken,
			@QueryParam(RequestParameters.altId) String altId) throws RestException
	{
		RequestParameters.validateParameterNamesAgainstSupportedNames(RequestInfo.get().getParameters(), RequestParameters.id, RequestParameters.expand,
				RequestParameters.processId, RequestParameters.COORDINATE_PARAM_NAMES, RequestParameters.altId);

		UUID processIdUUID = Util.validateWorkflowProcess(processId);

		SemanticChronology sc = findSemanticChronology(id);
		LatestVersion<SemanticVersion> sv = sc.getLatestVersion(Util.getPreWorkflowStampCoordinate(processIdUUID, sc.getNid()));
		Util.logContradictions(log, sv);
		if (sv.isPresent())
		{
			// TODO handle contradictions
			return RestSemanticVersion.buildRestSemanticVersion(sv.get(), RequestInfo.get().shouldExpand(ExpandUtil.chronologyExpandable),
					RequestInfo.get().shouldExpand(ExpandUtil.nestedSemanticsExpandable), RequestInfo.get().shouldExpand(ExpandUtil.referencedDetails),
					processIdUUID);
		}
		else
		{
			throw new RestException(RequestParameters.id, id, "No semantic was found");
		}
	}

	public static SemanticChronology findSemanticChronology(String id) throws RestException
	{
		AssemblageService semanticService = Get.assemblageService();

		Optional<UUID> uuidId = UUIDUtil.getUUID(id);
		OptionalInt intId = OptionalInt.empty();
		if (uuidId.isPresent())
		{
			if (Get.identifierService().hasUuid(uuidId.get()))
			{
				intId = OptionalInt.of(Get.identifierService().getNidForUuids(uuidId.get()));
			}
			else
			{
				throw new RestException("id", id, "Is not known by the system");
			}
		}
		else
		{
			intId = NumericUtils.getInt(id);
		}

		if (intId.isPresent())
		{
			Optional<? extends SemanticChronology> sc = semanticService.getOptionalSemanticChronology(intId.getAsInt());
			if (sc.isPresent())
			{
				return sc.get();
			}
			else
			{
				throw new RestException("id", id, "No Semantic was located with the given identifier");
			}
		}
		throw new RestException("id", id, "Is not a semantic identifier.  Must be a UUID or an integer");
	}

	/**
	 * Returns all semantic instances with the given assemblage
	 * If no version parameter is specified, returns the latest version.
	 * 
	 * @param id - A UUID or nid of an assemblage concept
	 * @param pageNum The pagination page number >= 1 to return
	 * @param maxPageSize The maximum number of results to return per page, must be greater than 0
	 * @param expand - comma separated list of fields to expand. Supports 'chronology', 'nested', 'referencedDetails'
	 *            When referencedDetails is passed, nids will include type information, and certain nids will also include their descriptions,
	 *            if they represent a concept or a description semantic.
	 * @param processId if set, specifies that retrieved components should be checked against the specified active
	 *            workflow process, and if existing in the process, only the version of the corresponding object prior to the version referenced
	 *            in the workflow process should be returned or referenced. If no version existed prior to creation of the workflow process,
	 *            then either no object will be returned or an exception will be thrown, depending on context.
	 * @param coordToken specifies an explicit serialized CoordinatesToken string specifying all coordinate parameters. A CoordinatesToken
	 *            may be obtained by a separate (prior) call to getCoordinatesToken().
	 * @param altId - (optional) the altId type(s) to populate in any returned RestIdentifiedObject structures.  By default, no alternate IDs are 
	 *     returned.  This can be set to one or more names or ids from the /1/id/types or the value 'ANY'.  Requesting IDs that are unneeded will harm 
	 *     performance. 
	 * 
	 * @return the semantic version objects. Note that the returned type here - RestSemanticVersion is actually an abstract base class,
	 *         the actual return type will be either a RestDynamicSemanticVersion or a RestSemanticDescriptionVersion.
	 * @throws RestException
	 */
	@GET
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Path(RestPaths.forAssemblageComponent + "{" + RequestParameters.id + "}")
	public RestSemanticVersionPage getForAssemblage(@PathParam(RequestParameters.id) String id,
			@QueryParam(RequestParameters.pageNum) @DefaultValue(RequestParameters.pageNumDefault) int pageNum,
			@QueryParam(RequestParameters.maxPageSize) @DefaultValue(RequestParameters.maxPageSizeDefault) int maxPageSize,
			@QueryParam(RequestParameters.expand) String expand, @QueryParam(RequestParameters.processId) String processId,
			@QueryParam(RequestParameters.coordToken) String coordToken,
			@QueryParam(RequestParameters.altId) String altId) throws RestException
	{
		RequestParameters.validateParameterNamesAgainstSupportedNames(RequestInfo.get().getParameters(), RequestParameters.id, RequestParameters.expand,
				RequestParameters.processId, RequestParameters.PAGINATION_PARAM_NAMES, RequestParameters.COORDINATE_PARAM_NAMES, RequestParameters.altId);

		HashSet<Integer> singleAllowedAssemblage = new HashSet<>();
		singleAllowedAssemblage.add(RequestInfoUtils.getConceptNidFromParameter(RequestParameters.id, id));

		UUID processIdUUID = Util.validateWorkflowProcess(processId);

		// we don't have a referenced component - our id is assemblage
		SemanticVersions versions = get(null, singleAllowedAssemblage, pageNum, maxPageSize, true, processIdUUID);

		List<RestSemanticVersion> restSemanticVersions = new ArrayList<>();
		for (SemanticVersion sv : versions.getValues())
		{
			restSemanticVersions.add(RestSemanticVersion.buildRestSemanticVersion(sv, RequestInfo.get().shouldExpand(ExpandUtil.chronologyExpandable),
					RequestInfo.get().shouldExpand(ExpandUtil.nestedSemanticsExpandable), RequestInfo.get().shouldExpand(ExpandUtil.referencedDetails),
					processIdUUID));
		}
		RestSemanticVersionPage results = new RestSemanticVersionPage(pageNum, maxPageSize, versions.getTotal(), true,
				versions.getTotal() > (pageNum * maxPageSize), RestPaths.semanticByAssemblageAppPathComponent + id,
				restSemanticVersions.toArray(new RestSemanticVersion[restSemanticVersions.size()]));

		return results;
	}

	/**
	 * Returns all semantic instances attached to the specified referenced component
	 * If no version parameter is specified, returns the latest version.
	 * 
	 * @param id - A UUID or nid of a component. Note that this could be a concept or a semantic reference.
	 * @param assemblage - An optional assemblage UUID or nid of a concept to restrict the type of semantics returned. If ommitted, assemblages
	 *            of all types will be returned. May be specified multiple times to allow multiple assemblages
	 * @param includeDescriptions - an optional flag to request that description type semantics are returned. By default, description type
	 *            semantics are not returned, as these are typically retrieved via a getDescriptions call on the Concept APIs.
	 * @param includeAssociations - an optional flag to request that semantics that represent associations are returned. By default, semantics that
	 *            represent
	 *            associations are not returned, as these are typically retrieved via a getSourceAssociations call on the Association APIs.
	 * @param includeMappings - an optional flag to request that semantics that represent mappings are returned. By default, semantics that represent
	 *            mappings are not returned, as these are typically retrieved via a the Mapping APIs.
	 * @param expand - comma separated list of fields to expand. Supports 'chronology', 'nestedSemantics', 'referencedDetails'
	 *            When referencedDetails is passed, nids will include type information, and certain nids will also include their descriptions,
	 *            if they represent a concept or a description semantic.
	 * @param processId if set, specifies that retrieved components should be checked against the specified active
	 *            workflow process, and if existing in the process, only the version of the corresponding object prior to the version referenced
	 *            in the workflow process should be returned or referenced. If no version existed prior to creation of the workflow process,
	 *            then either no object will be returned or an exception will be thrown, depending on context.
	 * @param coordToken specifies an explicit serialized CoordinatesToken string specifying all coordinate parameters. A CoordinatesToken may be
	 *            obtained by a separate (prior) call to getCoordinatesToken().
	 * @param altId - (optional) the altId type(s) to populate in any returned RestIdentifiedObject structures.  By default, no alternate IDs are 
	 *     returned.  This can be set to one or more names or ids from the /1/id/types or the value 'ANY'.  Requesting IDs that are unneeded will harm 
	 *     performance. 
	 *     
	 * @return the semantic version objects. Note that the returned type here - RestSemanticVersion is actually an abstract base class,
	 *         the actual return type will be either a RestDynamicSemanticVersion or a RestSemanticDescriptionVersion.
	 * @throws RestException
	 */
	@GET
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Path(RestPaths.forReferencedComponentComponent + "{" + RequestParameters.id + "}")
	public RestSemanticVersion[] getForReferencedComponent(@PathParam(RequestParameters.id) String id,
			@QueryParam(RequestParameters.assemblage) Set<String> assemblage,
			@QueryParam(RequestParameters.includeDescriptions) @DefaultValue("false") String includeDescriptions,
			@QueryParam(RequestParameters.includeAssociations) @DefaultValue("false") String includeAssociations,
			@QueryParam(RequestParameters.includeMappings) @DefaultValue("false") String includeMappings, @QueryParam(RequestParameters.expand) String expand,
			@QueryParam(RequestParameters.processId) String processId, @QueryParam(RequestParameters.coordToken) String coordToken,
			@QueryParam(RequestParameters.altId) String altId) throws RestException
	{
		RequestParameters.validateParameterNamesAgainstSupportedNames(RequestInfo.get().getParameters(), RequestParameters.id, RequestParameters.assemblage,
				RequestParameters.includeDescriptions, RequestParameters.includeAssociations, RequestParameters.includeMappings, RequestParameters.expand,
				RequestParameters.processId, RequestParameters.COORDINATE_PARAM_NAMES, RequestParameters.altId);

		HashSet<Integer> allowedAssemblages = new HashSet<>();
		for (String a : assemblage)
		{
			allowedAssemblages.add(RequestInfoUtils.getConceptNidFromParameter(RequestParameters.assemblage, a));
		}

		return get(id, allowedAssemblages, null,  // TODO add API support for the new skip assemblage feature
				RequestInfo.get().shouldExpand(ExpandUtil.chronologyExpandable), RequestInfo.get().shouldExpand(ExpandUtil.nestedSemanticsExpandable),
				RequestInfo.get().shouldExpand(ExpandUtil.referencedDetails), Boolean.parseBoolean(includeDescriptions.trim()),
				Boolean.parseBoolean(includeAssociations.trim()), Boolean.parseBoolean(includeMappings.trim()), Util.validateWorkflowProcess(processId));
	}

	/**
	 * Return the full description of a particular semantic - including its intended use, the types of any data columns that will be attached, etc.
	 * 
	 * @param id - The UUID or nid of the concept that represents the semantic assemblage.
	 * @param coordToken specifies an explicit serialized CoordinatesToken string specifying all coordinate parameters. A CoordinatesToken may be
	 *            obtained by a separate (prior) call to getCoordinatesToken().
	 * 
	 * @return - the full description
	 * @throws RestException
	 */
	@GET
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Path(RestPaths.semanticDefinitionComponent + "{" + RequestParameters.id + "}")
	public RestDynamicSemanticDefinition getSemanticDefinition(@PathParam(RequestParameters.id) String id,
			@QueryParam(RequestParameters.coordToken) String coordToken) throws RestException
	{
		RequestParameters.validateParameterNamesAgainstSupportedNames(RequestInfo.get().getParameters(), RequestParameters.id,
				RequestParameters.COORDINATE_PARAM_NAMES);

		int conceptNid = RequestInfoUtils.getConceptNidFromParameter(RequestParameters.id, id);
		if (DynamicUsageDescriptionImpl.isDynamicSemantic(conceptNid))
		{
			return new RestDynamicSemanticDefinition(DynamicUsageDescriptionImpl.read(conceptNid));
		}
		else if (Frills.definesIdentifierSemantic(conceptNid))
		{
			return new RestDynamicSemanticDefinition(DynamicUsageDescriptionImpl.mockIdentifierType(conceptNid));
		}
		else
		{
			// Not annotated as a dynamic semantic. We have to find a real value to determine if this is used as a static semantic.
			// TODO 3 Dan someday, we will fix the underlying APIs to allow us to know the static semantic typing up front....
			Optional<SemanticChronology> sc = Get.assemblageService().getSemanticChronologyStream(conceptNid).findAny();
			if (sc.isPresent())
			{
				return new RestDynamicSemanticDefinition(DynamicUsageDescriptionImpl.mockOrRead(sc.get()));
			}
		}
		throw new RestException("The specified concept identifier is not configured as a dynamic semantic, and it is not used as a static semantic");
	}

	public static Stream<SemanticChronology> getSemanticChronologyStreamForComponentFromAssemblagesFilteredByVersionType(int componentNid,
			Set<Integer> allowedAssemblageNids, Set<VersionType> typesToExclude)
	{
		NidSet semanticNids = Get.assemblageService().getSemanticNidsForComponentFromAssemblages(componentNid, allowedAssemblageNids);
		if (typesToExclude == null || typesToExclude.size() == 0)
		{
			return semanticNids.stream().mapToObj((int semanticNid) -> Get.assemblageService().getSemanticChronology(semanticNid));
		}
		else
		{
			final ArrayList<SemanticChronology> filteredList = new ArrayList<>();
			for (PrimitiveIterator.OfInt it = semanticNids.getIntIterator(); it.hasNext();)
			{
				SemanticChronology chronology = Get.assemblageService().getSemanticChronology(it.nextInt());
				boolean exclude = false;
				for (VersionType type : typesToExclude)
				{
					if (chronology.getVersionType() == type)
					{
						exclude = true;
						break;
					}
				}

				if (!exclude)
				{
					filteredList.add(chronology);
				}
			}

			return filteredList.stream();
		}
	}

	public static class SemanticVersions
	{
		private final List<SemanticVersion> values;
		private final int approximateTotal;

		public SemanticVersions(List<SemanticVersion> values, int approximateTotal)
		{
			this.values = values;
			this.approximateTotal = approximateTotal;
		}

		public SemanticVersion[] getValues()
		{
			return values.toArray(new SemanticVersion[values.size()]);
		}

		public int getTotal()
		{
			return approximateTotal;
		}
	}

	/**
	 * @param referencedComponent - optional - if provided - takes precedence
	 * @param allowedAssemblages - optional - if provided, either limits the referencedComponent search by this type, or, if
	 *            referencedComponent is not provided - focuses the search on just this assemblage
	 * @param pageNum 
	 * @param maxPageSize 
	 * @param allowDescriptions true to include description type semantics, false to skip
	 * @param processId if set, specifies that retrieved components should be checked against the specified active
	 *            workflow process, and if existing in the process, only the version of the corresponding object prior to the version referenced
	 *            in the workflow process should be returned or referenced. If no version existed prior to creation of the workflow process,
	 *            then either no object will be returned or an exception will be thrown, depending on context.
	 * @return the semantic versions wrapped for paging
	 * @throws RestException
	 */
	public static SemanticVersions get(String referencedComponent, Set<Integer> allowedAssemblages, final int pageNum, final int maxPageSize,
			boolean allowDescriptions, UUID processId) throws RestException
	{
		PaginationUtils.validateParameters(pageNum, maxPageSize);

		Set<VersionType> excludedVersionTypes = new HashSet<>();
		excludedVersionTypes.add(VersionType.LOGIC_GRAPH);
		// TODO see if any other new types should be excluded
		if (!allowDescriptions)
		{
			excludedVersionTypes.add(VersionType.DESCRIPTION);
		}

		final List<SemanticVersion> ochreResults = new ArrayList<>();

		if (StringUtils.isNotBlank(referencedComponent))
		{
			Optional<UUID> uuidId = UUIDUtil.getUUID(referencedComponent);
			OptionalInt refCompNid = OptionalInt.empty();
			if (uuidId.isPresent())
			{
				if (Get.identifierService().hasUuid(uuidId.get()))
				{
					refCompNid = OptionalInt.of(Get.identifierService().getNidForUuids(uuidId.get()));
				}
				else
				{
					throw new RestException("referencedComponent", referencedComponent, "Is not known by the system");
				}
			}
			else
			{
				refCompNid = NumericUtils.getInt(referencedComponent);
			}

			if (refCompNid.isPresent() && refCompNid.getAsInt() < 0)
			{
				Stream<SemanticChronology> semantics = getSemanticChronologyStreamForComponentFromAssemblagesFilteredByVersionType(refCompNid.getAsInt(),
						allowedAssemblages, excludedVersionTypes);

				int approximateTotal = 0;
				for (Iterator<SemanticChronology> it = semantics.iterator(); it.hasNext();)
				{
					if (ochreResults.size() >= (pageNum * maxPageSize))
					{
						it.next();
						continue;
					}
					else
					{
						SemanticChronology chronology = it.next();
						LatestVersion<SemanticVersion> sv = chronology.getLatestVersion(Util.getPreWorkflowStampCoordinate(processId, chronology.getNid()));
						Util.logContradictions(log, sv);
						if (sv.isPresent())
						{
							// TODO handle contradictions
							ochreResults.add(sv.get());
						}
					}

					approximateTotal++;
				}

				return new SemanticVersions(PaginationUtils.getResults(PaginationUtils.getResults(ochreResults, pageNum, maxPageSize), pageNum, maxPageSize),
						approximateTotal);
			}
			else
			{
				throw new RestException("referencedComponent", referencedComponent, "Must be a NID or a UUID");
			}
		}
		else
		{
			if (allowedAssemblages == null || allowedAssemblages.size() == 0)
			{
				throw new RestException("If a referenced component is not provided, then an allowedAssemblage must be provided");
			}

			NidSet allSemanticNids = new NidSet();
			for (int assemblageId : allowedAssemblages)
			{
				allSemanticNids.addAll(Get.assemblageService().getSemanticNidsFromAssemblage(assemblageId).stream());
			}

			for (PrimitiveIterator.OfInt it = allSemanticNids.getIntIterator(); it.hasNext();)
			{
				if (ochreResults.size() >= (pageNum * maxPageSize))
				{
					break;
				}
				else
				{
					SemanticChronology chronology = Get.assemblageService().getSemanticChronology(it.nextInt());
					LatestVersion<SemanticVersion> sv = ((SemanticChronology) chronology)
							.getLatestVersion(Util.getPreWorkflowStampCoordinate(processId, chronology.getNid()));
					Util.logContradictions(log, sv);
					if (sv.isPresent())
					{
						ochreResults.add(sv.get());
					}
				}
			}

			return new SemanticVersions(PaginationUtils.getResults(ochreResults, pageNum, maxPageSize), allSemanticNids.size());
		}
	}

	/**
	 * @param referencedComponent - optional - if provided - takes precedence
	 * @param allowedAssemblages - optional - if provided, either limits the referencedComponent search by this type, or, if
	 *            referencedComponent is not provided - focuses the search on just this assemblage
	 * @param skipAssemblages - optional - if provided, any assemblage listed here will not be part of the return. This takes priority over the
	 *            allowedAssemblages.
	 * @param expandChronology
	 * @param expandNested
	 * @param expandReferenced
	 * @param allowDescriptions true to include description type semantics, false to skip
	 * @param allowAssociations true to include semantics that represent associations, false to skip
	 * @param allowMappings true to include semantics that represent mappings, false to skip
	 * @param processId if set, specifies that retrieved components should be checked against the specified active
	 *            workflow process, and if existing in the process, only the version of the corresponding object prior to the version referenced
	 *            in the workflow process should be returned or referenced. If no version existed prior to creation of the workflow process,
	 *            then either no object will be returned or an exception will be thrown, depending on context.
	 * @return the semantics
	 * @throws RestException
	 */
	public static RestSemanticVersion[] get(String referencedComponent, Set<Integer> allowedAssemblages, Set<Integer> skipAssemblages, boolean expandChronology,
			boolean expandNested, boolean expandReferenced, boolean allowDescriptions, boolean allowAssociations, boolean allowMappings, UUID processId)
			throws RestException
	{
		final ArrayList<RestSemanticVersion> results = new ArrayList<>();
		Consumer<SemanticChronology> consumer = new Consumer<SemanticChronology>()
		{
			@Override
			public void accept(SemanticChronology sc)
			{
				if (sc.getVersionType() != VersionType.LOGIC_GRAPH && (allowDescriptions || sc.getVersionType() != VersionType.DESCRIPTION))
				{
					if (!allowAssociations && AssociationUtilities.isAssociation(sc))
					{
						return;
					}
					if (!allowMappings && Frills.isMapping(sc))
					{
						return;
					}
					if (skipAssemblages != null && skipAssemblages.contains(sc.getAssemblageNid()))
					{
						return;
					}
					LatestVersion<SemanticVersion> sv = sc.getLatestVersion(Util.getPreWorkflowStampCoordinate(processId, sc.getNid()));
					Util.logContradictions(log, sv);
					if (sv.isPresent())
					{
						try
						{
							// TODO handle contradictions
							results.add(RestSemanticVersion.buildRestSemanticVersion(sv.get(), expandChronology, expandNested, expandReferenced, processId));
						}
						catch (RestException e)
						{
							throw new RuntimeException("Unexpected error", e);
						}
					}
				}
			}
		};

		if (StringUtils.isNotBlank(referencedComponent))
		{
			Optional<UUID> uuidId = UUIDUtil.getUUID(referencedComponent);
			OptionalInt refCompNid = OptionalInt.empty();
			if (uuidId.isPresent())
			{
				if (Get.identifierService().hasUuid(uuidId.get()))
				{
					refCompNid = OptionalInt.of(Get.identifierService().getNidForUuids(uuidId.get()));
				}
				else
				{
					throw new RestException("referencedComponent", referencedComponent, "Is not known by the system");
				}
			}
			else
			{
				refCompNid = NumericUtils.getInt(referencedComponent);
			}

			if (refCompNid.isPresent() && refCompNid.getAsInt() < 0)
			{
				Stream<SemanticChronology> semantics = Get.assemblageService().getSemanticChronologyStreamForComponentFromAssemblages(refCompNid.getAsInt(),
						allowedAssemblages);
				semantics.forEach(consumer);
			}
			else
			{
				throw new RestException("referencedComponent", referencedComponent, "Must be a NID or a UUID");
			}
		}
		else
		{
			if (allowedAssemblages == null || allowedAssemblages.size() == 0)
			{
				throw new RestException("If a referenced component is not provided, then an allowedAssemblage must be provided");
			}
			for (int assemblageId : allowedAssemblages)
			{
				Get.assemblageService().getSemanticChronologyStream(assemblageId).forEach(consumer);
			}
		}
		return results.toArray(new RestSemanticVersion[results.size()]);
	}
}
