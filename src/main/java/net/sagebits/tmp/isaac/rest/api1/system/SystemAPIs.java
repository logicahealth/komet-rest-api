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

package net.sagebits.tmp.isaac.rest.api1.system;

import java.util.ArrayList;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
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
import net.sagebits.tmp.isaac.rest.ApplicationConfig;
import net.sagebits.tmp.isaac.rest.ExpandUtil;
import net.sagebits.tmp.isaac.rest.Util;
import net.sagebits.tmp.isaac.rest.api.exceptions.RestException;
import net.sagebits.tmp.isaac.rest.api1.RestPaths;
import net.sagebits.tmp.isaac.rest.api1.concept.ConceptAPIs;
import net.sagebits.tmp.isaac.rest.api1.data.RestSystemInfo;
import net.sagebits.tmp.isaac.rest.api1.data.RestUserInfo;
import net.sagebits.tmp.isaac.rest.api1.data.concept.RestConceptChronology;
import net.sagebits.tmp.isaac.rest.api1.data.concept.RestConceptVersion;
import net.sagebits.tmp.isaac.rest.api1.data.concept.RestConceptVersionPage;
import net.sagebits.tmp.isaac.rest.api1.data.concept.RestTerminologyConcept;
import net.sagebits.tmp.isaac.rest.api1.data.enumerations.DescriptionStyle;
import net.sagebits.tmp.isaac.rest.api1.data.enumerations.RestConcreteDomainOperatorsType;
import net.sagebits.tmp.isaac.rest.api1.data.enumerations.RestDescriptionStyle;
import net.sagebits.tmp.isaac.rest.api1.data.enumerations.RestDynamicSemanticDataType;
import net.sagebits.tmp.isaac.rest.api1.data.enumerations.RestDynamicSemanticValidatorType;
import net.sagebits.tmp.isaac.rest.api1.data.enumerations.RestNodeSemanticType;
import net.sagebits.tmp.isaac.rest.api1.data.enumerations.RestObjectChronologyType;
import net.sagebits.tmp.isaac.rest.api1.data.enumerations.RestSemanticType;
import net.sagebits.tmp.isaac.rest.api1.data.enumerations.RestSupportedIdType;
import net.sagebits.tmp.isaac.rest.api1.data.semantic.RestSemanticChronology;
import net.sagebits.tmp.isaac.rest.api1.data.systeminfo.RestIdentifiedObjectsResult;
import net.sagebits.tmp.isaac.rest.api1.taxonomy.TaxonomyAPIs;
import net.sagebits.tmp.isaac.rest.session.RequestInfo;
import net.sagebits.tmp.isaac.rest.session.RequestInfoUtils;
import net.sagebits.tmp.isaac.rest.session.RequestParameters;
import net.sagebits.uts.auth.data.UserRole.SystemRoleConstants;
import sh.isaac.MetaData;
import sh.isaac.api.Get;
import sh.isaac.api.TaxonomySnapshot;
import sh.isaac.api.chronicle.LatestVersion;
import sh.isaac.api.collections.NidSet;
import sh.isaac.api.component.concept.ConceptChronology;
import sh.isaac.api.component.concept.ConceptVersion;
import sh.isaac.api.component.semantic.version.DynamicVersion;
import sh.isaac.api.component.semantic.version.dynamic.types.DynamicUUID;
import sh.isaac.api.coordinate.PremiseType;
import sh.isaac.api.externalizable.IsaacObjectType;
import sh.isaac.api.util.NumericUtils;
import sh.isaac.api.util.UUIDUtil;
import sh.isaac.model.configuration.StampCoordinates;
import sh.isaac.model.coordinate.ManifoldCoordinateImpl;
import sh.isaac.utility.Frills;

/**
 * {@link SystemAPIs}
 * 
 * @author <a href="mailto:daniel.armbrust.list@sagebits.net">Dan Armbrust</a>
 */
@Path(RestPaths.systemAPIsPathComponent)
@RolesAllowed({ SystemRoleConstants.AUTOMATED, SystemRoleConstants.ADMINISTRATOR, SystemRoleConstants.SYSTEM_MANAGER, SystemRoleConstants.CONTENT_MANAGER,
	SystemRoleConstants.EDITOR, SystemRoleConstants.READ })
public class SystemAPIs
{
	private static Logger log = LogManager.getLogger();

	@Context
	private SecurityContext securityContext;

	/**
	 * @param id The id for which to retrieve objects. May be a UUID or NID
	 * @param expand comma separated list of fields to expand. Support depends on type of object identified by the passed id.
	 *     Both concepts and semantics support:
	 *       <br> 'versionsAll' - returns all versions of the concept.  Note that, this only includes all versions for the top level concept chronology.
	 *         For nested objects, the most appropriate version is returned, relative to the version of the concept being returned.  In other words, the STAMP 
	 *         of the concept version being returned is used to calculate the appropriate stamp for the referenced component versions, when they are looked up.
	 *         Sorted newest to oldest
	 *       <br> 'versionsLatestOnly' - ignored if specified in combination with versionsAll
	 *       <br>
	 *       <br> In addition, if the passed in id is a semantic, then it also supports:
	 *       <br> 'referencedDetails' - When referencedDetails is passed, nids will include type information, and certain nids will also include their descriptions,
	 *            if they represent a concept or a description semantic.
	 *       <br> 'nestedSemantics' - 
	 * @param coordToken specifies an explicit serialized CoordinatesToken string specifying all coordinate parameters. A CoordinatesToken
	 *            may be obtained by a separate (prior) call to getCoordinatesToken().
	 * @param altId - (optional) the altId type(s) to populate in any returned RestIdentifiedObject structures.  By default, no alternate IDs are 
	 *     returned.  This can be set to one or more names or ids from the /1/id/types or the value 'ANY'.  Requesting IDs that are unneeded will harm 
	 *     performance. 
	 * @return the found object
	 * @throws RestException
	 */
	@GET
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Path(RestPaths.identifiedObjectsComponent + "{" + RequestParameters.id + "}")
	public RestIdentifiedObjectsResult getIdentifiedObjects(@PathParam(RequestParameters.id) String id, @QueryParam(RequestParameters.expand) String expand,
			@QueryParam(RequestParameters.coordToken) String coordToken, @QueryParam(RequestParameters.altId) String altId) throws RestException
	{
		RequestParameters.validateParameterNamesAgainstSupportedNames(RequestInfo.get().getParameters(), RequestParameters.id, RequestParameters.expand,
				RequestParameters.COORDINATE_PARAM_NAMES, RequestParameters.altId);
		
		RequestInfo.get().validateMethodExpansions(ExpandUtil.versionsAllExpandable, ExpandUtil.versionsLatestOnlyExpandable, ExpandUtil.nestedSemanticsExpandable, 
				ExpandUtil.referencedDetails);

		RestConceptChronology concept = null;
		RestSemanticChronology semantic = null;
		OptionalInt intId = NumericUtils.getInt(id);
		if (intId.isPresent())
		{
			if (intId.getAsInt() < 0)
			{
				// id is NID
				IsaacObjectType objectChronologyType = Get.identifierService().getObjectTypeForComponent(intId.getAsInt());
				switch (objectChronologyType)
				{
					case CONCEPT:
					{
						concept = new RestConceptChronology(Get.conceptService().getConceptChronology(intId.getAsInt()),
								RequestInfo.get().shouldExpand(ExpandUtil.versionsAllExpandable),
								RequestInfo.get().shouldExpand(ExpandUtil.versionsLatestOnlyExpandable), true);
						break;
					}
					case SEMANTIC:
						semantic = new RestSemanticChronology(Get.assemblageService().getSemanticChronology(intId.getAsInt()),
								RequestInfo.get().shouldExpand(ExpandUtil.versionsAllExpandable),
								RequestInfo.get().shouldExpand(ExpandUtil.versionsLatestOnlyExpandable),
								RequestInfo.get().shouldExpand(ExpandUtil.nestedSemanticsExpandable),
								RequestInfo.get().shouldExpand(ExpandUtil.referencedDetails));
						break;
					case UNKNOWN:
					default :
						throw new RestException(RequestParameters.id, id, "Specified NID is for unsupported ObjectChronologyType " + objectChronologyType);
				}

				if (concept == null && semantic == null)
				{
					throw new RestException(RequestParameters.id, id, "Specified NID does not correspond to an existing concept or semantic");
				}

				return new RestIdentifiedObjectsResult(concept, semantic);
			}

			if (concept == null && semantic == null)
			{
				throw new RestException(RequestParameters.id, id, "Specified id does not correspond to an existing concept or semantic");
			}

			return new RestIdentifiedObjectsResult(concept, semantic);
		}
		else
		{
			Optional<UUID> uuidId = UUIDUtil.getUUID(id);
			if (uuidId.isPresent())
			{
				// id is uuid

				Integer nid = null;

				if (Get.identifierService().hasUuid(uuidId.get()) && (nid = Get.identifierService().getNidForUuids(uuidId.get())) != 0)
				{
					IsaacObjectType objectChronologyType = Get.identifierService().getObjectTypeForComponent(nid);

					switch (objectChronologyType)
					{
						case CONCEPT:
							concept = new RestConceptChronology(Get.conceptService().getConceptChronology(nid),
									RequestInfo.get().shouldExpand(ExpandUtil.versionsAllExpandable),
									RequestInfo.get().shouldExpand(ExpandUtil.versionsLatestOnlyExpandable), true);
							break;
						case SEMANTIC:
							semantic = new RestSemanticChronology(Get.assemblageService().getSemanticChronology(nid),
									RequestInfo.get().shouldExpand(ExpandUtil.versionsAllExpandable),
									RequestInfo.get().shouldExpand(ExpandUtil.versionsLatestOnlyExpandable),
									RequestInfo.get().shouldExpand(ExpandUtil.nestedSemanticsExpandable),
									RequestInfo.get().shouldExpand(ExpandUtil.referencedDetails));
							break;
						case UNKNOWN:
						default :
							throw new RestException(RequestParameters.id, id,
									"Specified UUID is for NID " + nid + " for unsupported ObjectChronologyType " + objectChronologyType);
					}

					if (concept == null && semantic == null)
					{
						throw new RestException(RequestParameters.id, id,
								"Specified UUID is for NID " + nid + " that does not correspond to an existing concept or semantic");
					}

					return new RestIdentifiedObjectsResult(concept, semantic);
				}
				else
				{
					throw new RestException(RequestParameters.id, id, "No concept or semantic exists corresponding to the passed UUID id.");
				}
			}
			else
			{
				throw new RestException(RequestParameters.id, id, "Specified string id is not a valid identifier.  Must be a UUID, or integer NID.");
			}
		}
	}

	/**
	 * Return the RestObjectChronologyType of the component corresponding to the passed id
	 * 
	 * @param id The id for which to determine RestObjectChronologyType
	 *            If an int < 0 then assumed to be a NID
	 *            If a String then parsed and handled as a UUID of either a concept or semantic
	 * @return Map of RestObjectChronologyType to RestId. Will contain exactly one entry if passed a UUID or NID
	 *         if no corresponding ids found a RestException is thrown.
	 * @param coordToken specifies an explicit serialized CoordinatesToken string specifying all coordinate parameters. A CoordinatesToken may be
	 *            obtained by a separate (prior) call to getCoordinatesToken().
	 * 
	 * @throws RestException
	 */
	@GET
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Path(RestPaths.objectChronologyTypeComponent + "{" + RequestParameters.id + "}")
	public RestObjectChronologyType getObjectChronologyType(@PathParam(RequestParameters.id) String id,
			@QueryParam(RequestParameters.coordToken) String coordToken) throws RestException
	{
		RequestParameters.validateParameterNamesAgainstSupportedNames(RequestInfo.get().getParameters(), RequestParameters.id,
				RequestParameters.COORDINATE_PARAM_NAMES);

		OptionalInt intId = NumericUtils.getInt(id);
		if (intId.isPresent())
		{
			if (intId.getAsInt() < 0)
			{
				// id is NID
				return new RestObjectChronologyType(Get.identifierService().getObjectTypeForComponent(intId.getAsInt()));
			}
			throw new RestException(RequestParameters.id, id,
					"Specified int id is not a valid NID. Must be a UUID, or integer NID " + "that uniquely identifies either a semantic or concept.");
		}
		else
		{
			Optional<UUID> uuidId = UUIDUtil.getUUID(id);
			if (uuidId.isPresent())
			{
				// id is uuid
				Integer nid = null;

				if (Get.identifierService().hasUuid(uuidId.get()) && (nid = Get.identifierService().getNidForUuids(uuidId.get())) != 0)
				{
					return new RestObjectChronologyType(Get.identifierService().getObjectTypeForComponent(nid));
				}
				else
				{
					throw new RestException(RequestParameters.id, id, "No concept or semantic exists corresponding to the passed UUID id.");
				}
			}
			else
			{
				throw new RestException(RequestParameters.id, id,
						"Specified string id is not a valid identifier.  Must be a UUID, or integer NID.");
			}
		}
	}

	/**
	 * Enumerate the valid types for the system. These values can be cached for the life of the connection.
	 * @return the data types
	 * 
	 * @throws RestException
	 */
	@GET
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Path(RestPaths.enumerationRestDynamicSemanticDataTypeComponent)
	public RestDynamicSemanticDataType[] getRestDynamicSemanticDataTypes() throws RestException
	{
		RequestParameters.validateParameterNamesAgainstSupportedNames(RequestInfo.get().getParameters(), RequestParameters.COORDINATE_PARAM_NAMES);

		return RestDynamicSemanticDataType.getAll();
	}

	/**
	 * Enumerate the valid types for the system. These values can be cached for the life of the connection.
	 * @return the validator types
	 * 
	 * @throws RestException
	 */
	@GET
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Path(RestPaths.enumerationRestDynamicValidatorTypeComponent)
	public RestDynamicSemanticValidatorType[] getRestDynamicValidatorTypes() throws RestException
	{
		RequestParameters.validateParameterNamesAgainstSupportedNames(RequestInfo.get().getParameters(), RequestParameters.COORDINATE_PARAM_NAMES);

		return RestDynamicSemanticValidatorType.getAll();
	}

	/**
	 * Enumerate the valid types for the system. These values can be cached for the life of the connection.
	 * @return the chronology types
	 * 
	 * @throws RestException
	 */
	@GET
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Path(RestPaths.enumerationRestObjectChronologyTypeComponent)
	public RestObjectChronologyType[] getRestObjectChronologyTypes() throws RestException
	{
		RequestParameters.validateParameterNamesAgainstSupportedNames(RequestInfo.get().getParameters(), RequestParameters.COORDINATE_PARAM_NAMES);

		return RestObjectChronologyType.getAll();
	}

	/**
	 * Enumerate the valid types for the system. These values can be cached for the life of the connection.
	 * @return the semantic types
	 * 
	 * @throws RestException
	 */
	@GET
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Path(RestPaths.enumerationRestSemanticTypeComponent)
	public RestSemanticType[] getRestSemanticVersionTypes() throws RestException
	{
		RequestParameters.validateParameterNamesAgainstSupportedNames(RequestInfo.get().getParameters(), RequestParameters.COORDINATE_PARAM_NAMES);

		return RestSemanticType.getAll();
	}

	/**
	 * Enumerate the valid types for the system. These values can be cached for the life of the connection.
	 * @return the values
	 * 
	 * @throws RestException
	 */
	@GET
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Path(RestPaths.enumerationRestConcreteDomainOperatorTypes)
	public RestConcreteDomainOperatorsType[] getRestConcreteDomainOperatorTypes() throws RestException
	{
		RequestParameters.validateParameterNamesAgainstSupportedNames(RequestInfo.get().getParameters(), RequestParameters.COORDINATE_PARAM_NAMES);

		return RestConcreteDomainOperatorsType.getAll();
	}

	/**
	 * Enumerate the valid types for the system. These values can be cached for the life of the connection.
	 * @return the node semantic types
	 * 
	 * @throws RestException
	 */
	@GET
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Path(RestPaths.enumerationRestNodeSemanticTypes)
	public RestNodeSemanticType[] getRestNodeSemanticTypes() throws RestException
	{
		RequestParameters.validateParameterNamesAgainstSupportedNames(RequestInfo.get().getParameters(), RequestParameters.COORDINATE_PARAM_NAMES);

		return RestNodeSemanticType.getAll();
	}

	/**
	 * Enumerate the valid types for the system. These values can be cached for the life of the connection.
	 * @return the supported id types
	 * 
	 * @throws RestException
	 */
	@GET
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Path(RestPaths.enumerationRestSupportedIdTypes)
	public RestSupportedIdType[] getRestSupportedIdTypes() throws RestException
	{
		RequestParameters.validateParameterNamesAgainstSupportedNames(RequestInfo.get().getParameters(), RequestParameters.COORDINATE_PARAM_NAMES);

		return RestSupportedIdType.getAll();
	}

	/**
	 * ISAAC, REST API and related DB metadata. These values are cached.
	 * @return the system info
	 * 
	 * This method optionally requires user credentials, or token information.  Anonymous access is 
	 * always allowed to this method, though, that may change in the future.
	 * 
	 * @throws RestException
	 */
	@GET
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Path(RestPaths.systemInfoComponent)
	public RestSystemInfo getSystemInfo() throws RestException
	{
		RequestParameters.validateParameterNamesAgainstSupportedNames(RequestInfo.get().getParameters(), RequestParameters.COORDINATE_PARAM_NAMES);

		return ApplicationConfig.getInstance().getSystemInfo();
	}

	/**
	 * Return information about a particular user (utilized to tie back session information to what was passed via SSO)
	 * 
	 * @param id - a nid or UUID of a concept that represents a user in the system.
	 * @return info about the user
	 * @throws RestException if no user concept can be identified.
	 */
	@GET
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Path(RestPaths.userComponent + "{" + RequestParameters.id + "}")
	public RestUserInfo getUserInfo(@PathParam(RequestParameters.id) String id) throws RestException
	{
		RequestParameters.validateParameterNamesAgainstSupportedNames(RequestInfo.get().getParameters(), RequestParameters.id, 
				RequestParameters.COORDINATE_PARAM_NAMES);
		return new RestUserInfo(RequestInfoUtils.getConceptNidFromParameter(RequestParameters.id, id));
	}

	/**
	 * Return the (sorted) general terminology types that are currently supported in this system. These are the terminology
	 * types that can be passed into the extendedDescriptionTypes call.
	 * For extended, specific details on the terminologies supported by the system, see 1/system/systemInfo.
	 * @return the terminology types
	 * @throws RestException 
	 */
	@GET
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Path(RestPaths.terminologyTypes)
	public RestTerminologyConcept[] getTerminologyTypes() throws RestException
	{
		RequestParameters.validateParameterNamesAgainstSupportedNames(RequestInfo.get().getParameters(), RequestParameters.COORDINATE_PARAM_NAMES);

		TreeSet<RestTerminologyConcept> terminologies = new TreeSet<>();

		TaxonomySnapshot tss = Get.taxonomyService().getSnapshotNoTree(RequestInfo.get().getManifoldCoordinate());

		for (int nid : tss.getTaxonomyChildConceptNids(MetaData.MODULE____SOLOR.getNid()))
		{
			ConceptChronology cc = Get.conceptService().getConceptChronology(nid);
			terminologies.add(new RestTerminologyConcept(cc, getDescriptionStyleForTerminologyInternal(cc)));
		} ;

		return terminologies.toArray(new RestTerminologyConcept[terminologies.size()]);
	}

	/**
	 * Return the taxonomy tree, rooted at the metadata concept MODULE, of the available modules in this
	 * current database.
	 * 
	 * @param availableOnly - if missing or true, only return modules that have content in this instance. If false,
	 *            return all module types present in the metadata, whether any content exists or not.
	 * 
	 *            This is determined by whether or not there is a module concept that extends from one of the children of the {@link MetaData#MODULE____SOLOR}
	 *            concepts.
	 * @return the modules
	 * @throws RestException 
	 */
	@GET
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Path(RestPaths.modules)
	public RestConceptVersion getAvailableModules(@QueryParam(RequestParameters.availableOnly) @DefaultValue("true") String availableOnly) throws RestException
	{
		RequestParameters.validateParameterNamesAgainstSupportedNames(RequestInfo.get().getParameters(), 
				RequestParameters.COORDINATE_PARAM_NAMES,
				RequestParameters.availableOnly);

		LatestVersion<ConceptVersion> cv = Get.conceptService().getConceptChronology(MetaData.MODULE____SOLOR.getNid())
				.getLatestVersion(StampCoordinates.getDevelopmentLatest());
		Util.logContradictions(log, cv);

		if (cv.isPresent())
		{
			RestConceptVersion rcv = new RestConceptVersion(cv.get(), true, false, false, true, true, true, false, false, true);

			TaxonomySnapshot tss = Get.taxonomyService()
					.getSnapshotNoTree(new ManifoldCoordinateImpl(PremiseType.STATED, StampCoordinates.getDevelopmentLatest(),
							Get.configurationService().getGlobalDatastoreConfiguration().getDefaultLanguageCoordinate(), 
							Get.configurationService().getGlobalDatastoreConfiguration().getDefaultLogicCoordinate()));

			// TODO there is a bug here - the addChildren reads coords from the RequestInfo, if they pass in weird coords, it will mess up this call.
			TaxonomyAPIs.addChildren(MetaData.MODULE____SOLOR.getNid(), rcv, tss, true, false, false, 3, false, false, new NidSet(), 1, 500);

			if (availableOnly == null || Boolean.parseBoolean(availableOnly))
			{
				ArrayList<RestConceptVersion> filteredResults = new ArrayList<>();
				// Trim out any 2nd level tree items that don't have children.
				for (RestConceptVersion x : rcv.children.results)
				{
					if (x.getChildCount() > 0)
					{
						filteredResults.add(x);
					}
				}
				rcv.children.results = filteredResults.toArray(new RestConceptVersion[filteredResults.size()]);
				rcv.children.paginationData.approximateTotal = filteredResults.size();
			}
			return rcv;
		}

		log.error("Couldn't find MODULE??");
		throw new RestException("Unexpected internal error");
	}

	/**
	 * Return the (sorted) extended description types that are allowable by a particular terminology.
	 * 
	 * @param id - a nid or UUID of a concept that represents a terminology in the system. This should be a direct child of
	 *            {@link MetaData#MODULE____SOLOR}
	 * @return the extended description types for the specified terminology 
	 * @throws RestException if the input can't be identified, or if input parameters are incorrect
	 */
	@GET
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Path(RestPaths.extendedDescriptionTypes + "{" + RequestParameters.id + "}")
	public RestConceptChronology[] getExtendedDescriptionTypesForTerminology(@PathParam(RequestParameters.id) String id) throws RestException
	{
		RequestParameters.validateParameterNamesAgainstSupportedNames(RequestInfo.get().getParameters(), RequestParameters.id, 
				RequestParameters.COORDINATE_PARAM_NAMES);

		TreeSet<RestConceptChronology> results = new TreeSet<>();

		ConceptChronology cc = ConceptAPIs.findConceptChronology(id);

		TaxonomySnapshot tss = Get.taxonomyService()
				.getSnapshotNoTree(new ManifoldCoordinateImpl(StampCoordinates.getDevelopmentLatest(), 
						Get.configurationService().getGlobalDatastoreConfiguration().getDefaultLanguageCoordinate()));

		if (!tss.isChildOf(cc.getNid(), MetaData.MODULE____SOLOR.getNid()))
		{
			throw new RestException("The passed in concept '" + id + "' is not a child of the MODULE constant.  " + "It should be a direct child of "
					+ MetaData.MODULE____SOLOR.getPrimordialUuid());
		}

		Frills.getAllChildrenOfConcept(MetaData.DESCRIPTION_TYPE_IN_SOURCE_TERMINOLOGY____SOLOR.getNid(), true, true, RequestInfo.get().getStampCoordinate())
				.forEach(descType -> {
					ConceptChronology concept = Get.conceptService().getConceptChronology(descType);
					//TODO I think this MOdule_solor should have changed to the metadata module, but not sure.  need to figure out why I added the test.
					if (cc.getNid() != MetaData.MODULE____SOLOR.getNid() && Frills.getTerminologyTypes(concept, null).contains(cc.getNid()))
					{
						results.add(new RestConceptChronology(concept, false, false, false));
					}
				});

		return results.toArray(new RestConceptChronology[results.size()]);
	}
	
	/**
	 * Return the (sorted) external description types that are allowable by a particular terminology.
	 * 
	 * These are returned as a two level hierarchy.  At the top level, we will return the three system core types.
	 * The children variable of the returned {@link RestConceptVersion} object will contain the external types that
	 * are linked the the parent core type.  The three core types are {@link MetaData#REGULAR_NAME_DESCRIPTION_TYPE____SOLOR}, 
	 * {@link MetaData#FULLY_QUALIFIED_NAME_DESCRIPTION_TYPE____SOLOR} and {@link MetaData#DEFINITION_DESCRIPTION_TYPE____SOLOR}
	 * 
	 * If this returns only the three top level items, with no children, then this is a terminology that uses native types only, so you should use
	 * system core types.
	 * 
	 * @param id - a nid or UUID of a concept that represents a terminology in the system. This should be a direct child of
	 *            {@link MetaData#MODULE____SOLOR}
	 * @return the external description types for the specified terminology, in a structure that organizes them under the core type
	 *            that the external type is linked to. 
	 * @throws RestException if the input can't be identified, or if input parameters are incorrect
	 */
	@GET
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Path(RestPaths.externalDescriptionTypes + "{" + RequestParameters.id + "}")
	public RestConceptVersion[] getExternalDescriptionTypesForTerminology(@PathParam(RequestParameters.id) String id) throws RestException
	{
		RequestParameters.validateParameterNamesAgainstSupportedNames(RequestInfo.get().getParameters(), RequestParameters.id, 
				RequestParameters.COORDINATE_PARAM_NAMES);

		TreeSet<RestConceptVersion> fqns = new TreeSet<>();
		TreeSet<RestConceptVersion> regName = new TreeSet<>();
		TreeSet<RestConceptVersion> definition = new TreeSet<>();

		ConceptChronology termType = ConceptAPIs.findConceptChronology(id);

		TaxonomySnapshot tss = Get.taxonomyService()
				.getSnapshotNoTree(new ManifoldCoordinateImpl(StampCoordinates.getDevelopmentLatest(), 
						Get.configurationService().getGlobalDatastoreConfiguration().getDefaultLanguageCoordinate()));

		if (!tss.isChildOf(termType.getNid(), MetaData.MODULE____SOLOR.getNid()))
		{
			throw new RestException("The passed in concept '" + id + "' is not a child of the MODULE constant.  " + "It should be a direct child of "
					+ MetaData.MODULE____SOLOR.getPrimordialUuid());
		}
		
		//Look up the description types that have a module of the requested type
		Frills.getAllChildrenOfConcept(MetaData.DESCRIPTION_TYPE____SOLOR.getNid(), true, true, RequestInfo.get().getStampCoordinate())
				.forEach(descType -> {
					ConceptChronology cc = Get.conceptService().getConceptChronology(descType);
					Set<Integer> modules = Frills.getAllModuleSequences(cc);
					for (int m : modules)
					{
						if (tss.isKindOf(m, termType.getNid()))
						{
							NidSet typeInfo = Get.assemblageService().getSemanticNidsForComponentFromAssemblage(cc.getNid(), 
									MetaData.DESCRIPTION_CORE_TYPE____SOLOR.getNid());
							if (typeInfo.size() != 1)
							{
								log.error("There should be one and only one Description Core Type on a external description type.  Found {} on {}", typeInfo.size(), cc);
							}
							if (typeInfo.size() > 0)
							{
								DynamicVersion<?> dv = (DynamicVersion<?>)Get.assemblageService()
										.getSemanticChronology(typeInfo.findFirst().getAsInt()).getLatestVersion(RequestInfo.get().getStampCoordinate()).get();
								DynamicUUID type = (DynamicUUID)dv.getData(0);
								
								RestConceptVersion rcv = new RestConceptVersion((ConceptVersion)cc
										.getLatestVersion(RequestInfo.get().getStampCoordinate()).get(), true);
								rcv.setChildCount(0);
								if (MetaData.FULLY_QUALIFIED_NAME_DESCRIPTION_TYPE____SOLOR.getUuidList().contains(type.getDataUUID()))
								{
									fqns.add(rcv);
								}
								else if (MetaData.REGULAR_NAME_DESCRIPTION_TYPE____SOLOR.getUuidList().contains(type.getDataUUID()))
								{
									regName.add(rcv);
								}
								else if (MetaData.DEFINITION_DESCRIPTION_TYPE____SOLOR.getUuidList().contains(type.getDataUUID()))
								{
									definition.add(rcv);
								}
								else
								{
									log.error("Unexpected core type linkage of {} on {}", type.getDataUUID(), cc);
								}
							}
						}
					}
				});

		RestConceptVersion[] finalResult = new RestConceptVersion[3];
		finalResult[0] = new RestConceptVersion((ConceptVersion)Get.concept(MetaData.FULLY_QUALIFIED_NAME_DESCRIPTION_TYPE____SOLOR.getNid())
				.getLatestVersion(RequestInfo.get().getStampCoordinate()).get(), true);
		finalResult[0].children = new RestConceptVersionPage(1, Integer.MAX_VALUE, fqns.size(), true, false, "", fqns.toArray(new RestConceptVersion[fqns.size()]));
		finalResult[0].setChildCount(fqns.size());
		
		finalResult[1] = new RestConceptVersion((ConceptVersion)Get.concept(MetaData.REGULAR_NAME_DESCRIPTION_TYPE____SOLOR.getNid())
				.getLatestVersion(RequestInfo.get().getStampCoordinate()).get(), true);
		finalResult[1].children = new RestConceptVersionPage(1, Integer.MAX_VALUE, regName.size(), true, false, "", 
				regName.toArray(new RestConceptVersion[regName.size()]));
		finalResult[1].setChildCount(regName.size());
		
		finalResult[2] = new RestConceptVersion((ConceptVersion)Get.concept(MetaData.DEFINITION_DESCRIPTION_TYPE____SOLOR.getNid())
				.getLatestVersion(RequestInfo.get().getStampCoordinate()).get(), true);
		finalResult[2].children = new RestConceptVersionPage(1, Integer.MAX_VALUE, definition.size(), true, false, "", 
				definition.toArray(new RestConceptVersion[definition.size()]));
		finalResult[2].setChildCount(definition.size());

		return finalResult;
	}
	
	/**
	 * Return the (sorted) core description types and external description types that are allowable loaded into the system.
	 * 
	 * These are returned as a two level hierarchy.  At the top level, we will return the three system core types.
	 * The children variable of the returned {@link RestConceptVersion} object will contain the external types that
	 * are linked the the parent core type.  The three core types are {@link MetaData#REGULAR_NAME_DESCRIPTION_TYPE____SOLOR}, 
	 * {@link MetaData#FULLY_QUALIFIED_NAME_DESCRIPTION_TYPE____SOLOR} and {@link MetaData#DEFINITION_DESCRIPTION_TYPE____SOLOR}
	 * 
	 * If this returns only the three top level items, with no children, then only core types are loaded.
	 * 
	 * @return the external description types loaded in they system, in a structure that organizes them under the core type
	 *            that the external type is linked to. 
	 * @throws RestException  in any supplied parameters are invalid
	 */
	@GET
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Path(RestPaths.descriptionTypes)
	public RestConceptVersion[] getAllDescriptionTypes() throws RestException
	{
		RequestParameters.validateParameterNamesAgainstSupportedNames(RequestInfo.get().getParameters(), RequestParameters.COORDINATE_PARAM_NAMES);

		TreeSet<RestConceptVersion> fqns = new TreeSet<>();
		TreeSet<RestConceptVersion> regName = new TreeSet<>();
		TreeSet<RestConceptVersion> definition = new TreeSet<>();

		//Look up the description types that have a module of the requested type
		Frills.getAllChildrenOfConcept(MetaData.DESCRIPTION_TYPE____SOLOR.getNid(), true, true, RequestInfo.get().getStampCoordinate())
				.forEach(descType -> {
					ConceptChronology cc = Get.conceptService().getConceptChronology(descType);
					NidSet typeInfo = Get.assemblageService().getSemanticNidsForComponentFromAssemblage(cc.getNid(), 
							MetaData.DESCRIPTION_CORE_TYPE____SOLOR.getNid());
					if (typeInfo.size() == 0)
					{
						//should be a core type
						if (descType != MetaData.FULLY_QUALIFIED_NAME_DESCRIPTION_TYPE____SOLOR.getNid() 
								&& descType != MetaData.REGULAR_NAME_DESCRIPTION_TYPE____SOLOR.getNid()
								&& descType != MetaData.DEFINITION_DESCRIPTION_TYPE____SOLOR.getNid())
						{
							log.warn("Unexpected core description type {}", cc);
						}
					}
					else if (typeInfo.size() > 1)
					{
						log.error("There should be one and only one Description Core Type on a external description type.  Found {} on {}", typeInfo.size(), cc);
					}
					else
					{
						DynamicVersion<?> dv = (DynamicVersion<?>)Get.assemblageService()
								.getSemanticChronology(typeInfo.findFirst().getAsInt()).getLatestVersion(RequestInfo.get().getStampCoordinate()).get();
						DynamicUUID type = (DynamicUUID)dv.getData(0);
						
						RestConceptVersion rcv = new RestConceptVersion((ConceptVersion)cc
								.getLatestVersion(RequestInfo.get().getStampCoordinate()).get(), true);
						rcv.setChildCount(0);
						if (MetaData.FULLY_QUALIFIED_NAME_DESCRIPTION_TYPE____SOLOR.getUuidList().contains(type.getDataUUID()))
						{
							fqns.add(rcv);
						}
						else if (MetaData.REGULAR_NAME_DESCRIPTION_TYPE____SOLOR.getUuidList().contains(type.getDataUUID()))
						{
							regName.add(rcv);
						}
						else if (MetaData.DEFINITION_DESCRIPTION_TYPE____SOLOR.getUuidList().contains(type.getDataUUID()))
						{
							definition.add(rcv);
						}
						else
						{
							log.error("Unexpected core type linkage of {} on {}", type.getDataUUID(), cc);
						}
					}
				});

		RestConceptVersion[] finalResult = new RestConceptVersion[3];
		finalResult[0] = new RestConceptVersion((ConceptVersion)Get.concept(MetaData.FULLY_QUALIFIED_NAME_DESCRIPTION_TYPE____SOLOR.getNid())
				.getLatestVersion(RequestInfo.get().getStampCoordinate()).get(), true);
		finalResult[0].children = new RestConceptVersionPage(1, Integer.MAX_VALUE, fqns.size(), true, false, "", fqns.toArray(new RestConceptVersion[fqns.size()]));
		finalResult[0].setChildCount(fqns.size());
		
		finalResult[1] = new RestConceptVersion((ConceptVersion)Get.concept(MetaData.REGULAR_NAME_DESCRIPTION_TYPE____SOLOR.getNid())
				.getLatestVersion(RequestInfo.get().getStampCoordinate()).get(), true);
		finalResult[1].children = new RestConceptVersionPage(1, Integer.MAX_VALUE, regName.size(), true, false, "", 
				regName.toArray(new RestConceptVersion[regName.size()]));
		finalResult[1].setChildCount(regName.size());
		
		finalResult[2] = new RestConceptVersion((ConceptVersion)Get.concept(MetaData.DEFINITION_DESCRIPTION_TYPE____SOLOR.getNid())
				.getLatestVersion(RequestInfo.get().getStampCoordinate()).get(), true);
		finalResult[2].children = new RestConceptVersionPage(1, Integer.MAX_VALUE, definition.size(), true, false, "", 
				definition.toArray(new RestConceptVersion[definition.size()]));
		finalResult[2].setChildCount(definition.size());

		return finalResult;
	}
	
	/**
	 * Return the {@link RestDescriptionStyle} that is utilized by a particular terminology.
	 * 
	 * @param id - a nid or UUID of a concept that represents a terminology in the system. This should be a direct child of
	 *            {@link MetaData#MODULE____SOLOR}
	 * @return the {@link RestDescriptionStyle} for the specified terminology 
	 * @throws RestException if the input can't be identified, or if input parameters are incorrect
	 */
	@GET
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Path(RestPaths.descriptionStyle + "{" + RequestParameters.id + "}")
	public RestDescriptionStyle getDescriptionStyleForTerminology(@PathParam(RequestParameters.id) String id) throws RestException
	{
		RequestParameters.validateParameterNamesAgainstSupportedNames(RequestInfo.get().getParameters(), RequestParameters.id, 
				RequestParameters.COORDINATE_PARAM_NAMES);
		
		ConceptChronology termType = ConceptAPIs.findConceptChronology(id);
		
		TaxonomySnapshot tss = Get.taxonomyService()
				.getSnapshotNoTree(new ManifoldCoordinateImpl(StampCoordinates.getDevelopmentLatest(), 
						Get.configurationService().getGlobalDatastoreConfiguration().getDefaultLanguageCoordinate()));

		if (!tss.isChildOf(termType.getNid(), MetaData.MODULE____SOLOR.getNid()))
		{
			throw new RestException("The passed in concept '" + id + "' is not a child of the MODULE constant.  " + "It should be a direct child of "
					+ MetaData.MODULE____SOLOR.getPrimordialUuid());
		}
		return getDescriptionStyleForTerminologyInternal(termType);
	}
	
	/**
	 * This internal method makes the assumption that you have passed in a valid terminology type
	 * @param id
	 * @return
	 * @throws RestException
	 */
	private RestDescriptionStyle getDescriptionStyleForTerminologyInternal(ConceptChronology termType) throws RestException
	{
		//these are always native, even if some extended types got created for testing or something along those lines.
		if (termType.getNid() == MetaData.SNOMED_CT_CORE_MODULES____SOLOR.getNid() || termType.getNid() == MetaData.CORE_METADATA_MODULE____SOLOR.getNid())
		{
			return new RestDescriptionStyle(DescriptionStyle.NATIVE);
		}
		//Iterate over the core types that were created, and if any match the passed in term type, then this is a native style
		if (Get.assemblageService().getSemanticChronologyStream(MetaData.DESCRIPTION_CORE_TYPE____SOLOR.getNid()).filter(sc ->
		{
			AtomicBoolean returnVal = new AtomicBoolean(false);
			sc.getLatestVersion(RequestInfo.get().getStampCoordinate()).ifPresent(version -> 
			{
				int foundTermType = Frills.getTerminologyTypeForModule(version.getModuleNid(), RequestInfo.get().getStampCoordinate());
				if (foundTermType == termType.getNid())
				{
					returnVal.set(true);
				}
			});
			return returnVal.get();
		}).findFirst().isPresent())
		{
			return new RestDescriptionStyle(DescriptionStyle.EXTERNAL);
		}
		
		//See if it has extended types
		
		if (Frills.getAllChildrenOfConcept(MetaData.DESCRIPTION_TYPE_IN_SOURCE_TERMINOLOGY____SOLOR.getNid(), true, true, RequestInfo.get().getStampCoordinate())
		.stream().filter(descType -> {
			ConceptChronology concept = Get.conceptService().getConceptChronology(descType);
			//TODO I think this MOdule_solor should have changed to the metadata module, but not sure.  need to figure out why I added the test.
			if (termType.getNid() != MetaData.MODULE____SOLOR.getNid() && Frills.getTerminologyTypes(concept, null).contains(termType.getNid()))
			{
				return true;
			}
			return false;
		}).findFirst().isPresent())
		{
			return new RestDescriptionStyle(DescriptionStyle.EXTENDED);
		}
		
		//No external or extended types, must be native
		return new RestDescriptionStyle(DescriptionStyle.NATIVE);
	}
}
