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

package net.sagebits.tmp.isaac.rest.api1.mapping;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
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
import net.sagebits.tmp.isaac.rest.ExpandUtil;
import net.sagebits.tmp.isaac.rest.Util;
import net.sagebits.tmp.isaac.rest.api.exceptions.RestException;
import net.sagebits.tmp.isaac.rest.api1.RestPaths;
import net.sagebits.tmp.isaac.rest.api1.concept.ConceptAPIs;
import net.sagebits.tmp.isaac.rest.api1.data.enumerations.MapSetItemComponent;
import net.sagebits.tmp.isaac.rest.api1.data.enumerations.RestMapSetItemComponentType;
import net.sagebits.tmp.isaac.rest.api1.data.mapping.RestMappingItemVersion;
import net.sagebits.tmp.isaac.rest.api1.data.mapping.RestMappingItemVersionPage;
import net.sagebits.tmp.isaac.rest.api1.data.mapping.RestMappingSetDisplayField;
import net.sagebits.tmp.isaac.rest.api1.data.mapping.RestMappingSetDisplayFieldCreate;
import net.sagebits.tmp.isaac.rest.api1.data.mapping.RestMappingSetVersion;
import net.sagebits.tmp.isaac.rest.api1.data.semantic.RestDynamicSemanticColumnInfo;
import net.sagebits.tmp.isaac.rest.api1.semantic.SemanticAPIs;
import net.sagebits.tmp.isaac.rest.api1.semantic.SemanticAPIs.SemanticVersions;
import net.sagebits.tmp.isaac.rest.session.MapSetDisplayFieldsService;
import net.sagebits.tmp.isaac.rest.session.RequestInfo;
import net.sagebits.tmp.isaac.rest.session.RequestInfoUtils;
import net.sagebits.tmp.isaac.rest.session.RequestParameters;
import net.sagebits.tmp.isaac.rest.session.SecurityUtils;
import sh.isaac.api.ConceptProxy;
import sh.isaac.api.Get;
import sh.isaac.api.LookupService;
import sh.isaac.api.Status;
import sh.isaac.api.chronicle.LatestVersion;
import sh.isaac.api.component.concept.ConceptChronology;
import sh.isaac.api.component.concept.ConceptVersion;
import sh.isaac.api.component.semantic.SemanticChronology;
import sh.isaac.api.component.semantic.version.DynamicVersion;
import sh.isaac.api.component.semantic.version.SemanticVersion;
import sh.isaac.api.component.semantic.version.dynamic.DynamicData;
import sh.isaac.api.component.semantic.version.dynamic.DynamicUsageDescription;
import sh.isaac.api.coordinate.StampCoordinate;
import sh.isaac.mapping.constants.IsaacMappingConstants;
import sh.isaac.misc.security.SystemRoleConstants;
import sh.isaac.model.semantic.DynamicUsageDescriptionImpl;
import sh.isaac.model.semantic.types.DynamicArrayImpl;
import sh.isaac.model.semantic.types.DynamicStringImpl;
import sh.isaac.utility.Frills;

/**
 * {@link MappingAPIs}
 * 
 * @author <a href="mailto:daniel.armbrust.list@sagebits.net">Dan Armbrust</a>
 */
@Path(RestPaths.mappingAPIsPathComponent)
@RolesAllowed({ SystemRoleConstants.AUTOMATED, SystemRoleConstants.SUPER_USER, SystemRoleConstants.ADMINISTRATOR, SystemRoleConstants.READ_ONLY,
		SystemRoleConstants.EDITOR, SystemRoleConstants.REVIEWER, SystemRoleConstants.APPROVER, SystemRoleConstants.DEPLOYMENT_MANAGER })
public class MappingAPIs
{
	private static Logger log = LogManager.getLogger(MappingAPIs.class);

	@Context
	private SecurityContext securityContext;

	/**
	 * 
	 * @param processId if set, specifies that retrieved components should be checked against the specified active
	 *            workflow process, and if existing in the process, only the version of the corresponding object prior to the version referenced
	 *            in the workflow process should be returned or referenced. If no version existed prior to creation of the workflow process,
	 *            then either no object will be returned or an exception will be thrown, depending on context.
	 * @param coordToken specifies an explicit serialized CoordinatesToken string specifying all coordinate parameters. A CoordinatesToken may
	 *            be obtained by a separate (prior) call to getCoordinatesToken().
	 * @param expand - A comma separated list of fields to expand. Supports 'comments'. When comments is passed, the latest comment(s) attached to
	 *            each mapSet are included.
	 * @param altId - (optional) the altId type(s) to populate in any returned RestIdentifiedObject structures.  By default, no alternate IDs are 
	 *     returned.  This can be set to one or more names or ids from the /1/id/types or the value 'ANY'.  Requesting IDs that are unneeded will harm 
	 *     performance. 
	 * @return the latest version of each unique mapping set definition found in the system on the specified coordinates.
	 * 
	 *         TODO add parameters to this method to allow the return of all versions (current + historical)
	 * 
	 * @throws RestException
	 */
	@GET
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Path(RestPaths.mappingSetsComponent)
	public RestMappingSetVersion[] getMappingSets(@QueryParam(RequestParameters.processId) String processId,
			@QueryParam(RequestParameters.coordToken) String coordToken,
			@QueryParam(RequestParameters.altId) String altId) throws RestException
	{
		SecurityUtils.validateRole(securityContext, getClass());

		RequestParameters.validateParameterNamesAgainstSupportedNames(RequestInfo.get().getParameters(), RequestParameters.expand, RequestParameters.processId,
				RequestParameters.COORDINATE_PARAM_NAMES, RequestParameters.altId);

		ArrayList<RestMappingSetVersion> results = new ArrayList<>();
		UUID processIdUUID = Util.validateWorkflowProcess(processId);

		Get.assemblageService().getSemanticChronologyStream(IsaacMappingConstants.get().DYNAMIC_SEMANTIC_MAPPING_SEMANTIC_TYPE.getNid()).forEach(semanticC -> {
			// We don't change the state / care about the state on the semantic. We update the state on the concept.
			@SuppressWarnings({ "rawtypes" })
			LatestVersion<DynamicVersion> latest = semanticC
					.getLatestVersion(RequestInfo.get().getStampCoordinate().makeCoordinateAnalog(Status.ACTIVE, Status.INACTIVE));
			Util.logContradictions(log, latest);

			if (latest.isPresent())
			{
				ConceptChronology cc = Get.conceptService().getConceptChronology(latest.get().getReferencedComponentNid());

				StampCoordinate conceptCoord = Util.getPreWorkflowStampCoordinate(processIdUUID, cc.getNid());
				LatestVersion<ConceptVersion> cv = cc.getLatestVersion(conceptCoord);
				Util.logContradictions(log, cv);

				if (cv.isPresent())
				{
					// TODO handle contradictions
					Util.logContradictions(log, cv);
					results.add(new RestMappingSetVersion(cv.get(), latest.get(), conceptCoord, RequestInfo.get().shouldExpand(ExpandUtil.comments),
							processIdUUID));
				}
			}
		});
		return results.toArray(new RestMappingSetVersion[results.size()]);
	}

	/**
	 * @param id - A UUID or nid of a concept that identifies the map set.
	 * @param coordToken specifies an explicit serialized CoordinatesToken string specifying all coordinate parameters. A CoordinatesToken may
	 *            be obtained by a separate (prior) call to getCoordinatesToken().
	 * @param processId if set, specifies that retrieved components should be checked against the specified active
	 *            workflow process, and if existing in the process, only the version of the corresponding object prior to the version referenced
	 *            in the workflow process should be returned or referenced. If no version existed prior to creation of the workflow process,
	 *            then either no object will be returned or an exception will be thrown, depending on context.
	 * @param expand - A comma separated list of fields to expand. Supports 'comments'. When comments is passed, the latest comment(s) attached to
	 *            each mapSet are included.
	 * @param altId - (optional) the altId type(s) to populate in any returned RestIdentifiedObject structures.  By default, no alternate IDs are 
	 *     returned.  This can be set to one or more names or ids from the /1/id/types or the value 'ANY'.  Requesting IDs that are unneeded will harm 
	 *     performance. 
	 * @return the latest version of the specified mapping set.
	 * 
	 *         TODO add parameters to this method to allow the return of all versions (current + historical)
	 * 
	 * @throws RestException
	 */
	@GET
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Path(RestPaths.mappingSetComponent + "{" + RequestParameters.id + "}")
	public RestMappingSetVersion getMappingSet(@PathParam(RequestParameters.id) String id, @QueryParam(RequestParameters.coordToken) String coordToken,
			@QueryParam(RequestParameters.processId) String processId, @QueryParam(RequestParameters.expand) String expand,
			@QueryParam(RequestParameters.altId) String altId) throws RestException
	{
		SecurityUtils.validateRole(securityContext, getClass());

		RequestParameters.validateParameterNamesAgainstSupportedNames(RequestInfo.get().getParameters(), RequestParameters.id, RequestParameters.expand,
				RequestParameters.processId, RequestParameters.COORDINATE_PARAM_NAMES, RequestParameters.altId);

		return getMappingSet(id, processId);
	}

	static RestMappingSetVersion getMappingSet(String id, String processId) throws RestException
	{
		Optional<SemanticChronology> semantic = Get.assemblageService().getSemanticChronologyStreamForComponentFromAssemblage(
				ConceptAPIs.findConceptChronology(id).getNid(), IsaacMappingConstants.get().DYNAMIC_SEMANTIC_MAPPING_SEMANTIC_TYPE.getNid()).findAny();

		if (!semantic.isPresent())
		{
			throw new RestException("The map set identified by '" + id + "' is not present");
		}

		UUID processIdUUID = Util.validateWorkflowProcess(processId);

		@SuppressWarnings({ "rawtypes" })
		LatestVersion<DynamicVersion> latest = semantic.get().getLatestVersion(Util.getPreWorkflowStampCoordinate(processIdUUID, semantic.get().getNid()));
		Util.logContradictions(log, latest);
		if (latest.isPresent())
		{
			ConceptChronology cc = Get.conceptService().getConceptChronology(latest.get().getReferencedComponentNid());

			StampCoordinate conceptCoord = Util.getPreWorkflowStampCoordinate(processIdUUID, cc.getNid());
			LatestVersion<ConceptVersion> cv = cc.getLatestVersion(conceptCoord);
			Util.logContradictions(log, cv);

			if (cv.isPresent())
			{
				// TODO handle contradictions
				return new RestMappingSetVersion(cv.get(), latest.get(), conceptCoord, RequestInfo.get().shouldExpand(ExpandUtil.comments), processIdUUID);
			}
			else
			{
				throw new RestException("The map set identified by '" + id + "' is not present at the given stamp");
			}
		}
		else
		{
			throw new RestException("The map set identified by '" + id + "' is not present at the given stamp");
		}
	}

	/**
	 * @return array of {@link RestMappingSetDisplayField} available for ordering and displaying mapping set item fields
	 * 
	 * @throws RestException
	 */
	@GET
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Path(RestPaths.mappingFieldsComponent)
	public RestMappingSetDisplayField[] getAvailableMappingSetDisplayFields() throws RestException
	{
		SecurityUtils.validateRole(securityContext, getClass());

		MapSetDisplayFieldsService service = LookupService.getService(MapSetDisplayFieldsService.class);
		return service.getAllFields();
	}

	/**
	 * @return array of {@link RestMapSetItemComponentType} values available for use in constructing a {@link RestMappingSetDisplayFieldCreate}
	 * @throws RestException
	 */
	@GET
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Path(RestPaths.mappingFieldComponentTypesComponent)
	public RestMapSetItemComponentType[] getAvailableMappingSetDisplayFieldComponentTypes() throws RestException
	{
		SecurityUtils.validateRole(securityContext, getClass());

		return RestMapSetItemComponentType.getAll();
	}

	/**
	 * @param id - A UUID or nid of the concept that identifies the map set to list items for. Should be from
	 *            {@link RestMappingSetVersion#identifiers}}
	 * @param pageNum The pagination page number >= 1 to return
	 * @param maxPageSize The maximum number of results to return per page, must be greater than 0, defaults to 250
	 * @param expand - A comma separated list of fields to expand. Supports 'referencedDetails,comments'. When referencedDetails is passed,
	 *            descriptions will be included for all referenced concepts which align with your current coordinates. When comments is passed, all
	 *            comments attached to each mapItem are included.
	 * @param processId if set, specifies that retrieved components should be checked against the specified active
	 *            workflow process, and if existing in the process, only the version of the corresponding object prior to the version referenced
	 *            in the workflow process should be returned or referenced. If no version existed prior to creation of the workflow process,
	 *            then either no object will be returned or an exception will be thrown, depending on context.
	 * @param coordToken specifies an explicit serialized CoordinatesToken string specifying all coordinate parameters. A CoordinatesToken may
	 *            be obtained by a separate (prior) call to getCoordinatesToken().
	 * @param altId - (optional) the altId type(s) to populate in any returned RestIdentifiedObject structures.  By default, no alternate IDs are 
	 *     returned.  This can be set to one or more names or ids from the /1/id/types or the value 'ANY'.  Requesting IDs that are unneeded will harm 
	 *     performance. 
	 * @return the mapping items versions object.
	 * 
	 * @throws RestException
	 */
	@GET
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Path(RestPaths.mappingItemsComponent + "{" + RequestParameters.id + "}")
	public RestMappingItemVersionPage getMappingItemPage(@PathParam(RequestParameters.id) String id,
			@QueryParam(RequestParameters.pageNum) @DefaultValue(RequestParameters.pageNumDefault) int pageNum,
			@QueryParam(RequestParameters.maxPageSize) @DefaultValue(250 + "") int maxPageSize, @QueryParam(RequestParameters.expand) String expand,
			@QueryParam(RequestParameters.processId) String processId, @QueryParam(RequestParameters.coordToken) String coordToken,
			@QueryParam(RequestParameters.altId) String altId) throws RestException
	{
		SecurityUtils.validateRole(securityContext, getClass());

		RequestParameters.validateParameterNamesAgainstSupportedNames(RequestInfo.get().getParameters(), RequestParameters.id,
				RequestParameters.PAGINATION_PARAM_NAMES, RequestParameters.expand, RequestParameters.processId, RequestParameters.COORDINATE_PARAM_NAMES, 
				RequestParameters.altId);

		ArrayList<RestMappingItemVersion> items = new ArrayList<>();

		int semanticConceptNid = RequestInfoUtils.getConceptNidFromParameter(RequestParameters.id, id);

		Positions positions = Positions.getPositions(semanticConceptNid);

		UUID processIdUUID = Util.validateWorkflowProcess(processId);

		List<RestMappingSetDisplayField> displayFields = MappingAPIs.getMappingSetDisplayFieldsFromMappingSet(semanticConceptNid,
				RequestInfo.get().getStampCoordinate());

		Set<Integer> allowedAssemblages = new HashSet<>();
		allowedAssemblages.add(semanticConceptNid);
		SemanticVersions semantics = SemanticAPIs.get(null, allowedAssemblages, pageNum, maxPageSize, false, processIdUUID);

		for (SemanticVersion semanticVersion : semantics.getValues())
		{
			items.add(new RestMappingItemVersion(((DynamicVersion<?>) semanticVersion), positions.targetPos, positions.qualfierPos,
					RequestInfo.get().shouldExpand(ExpandUtil.referencedDetails), RequestInfo.get().shouldExpand(ExpandUtil.comments), processIdUUID,
					displayFields));
		}
		RestMappingItemVersionPage results = new RestMappingItemVersionPage(pageNum, maxPageSize, semantics.getTotal(), true,
				semantics.getTotal() > (pageNum * maxPageSize), RestPaths.mappingItemsComponent + id, items.toArray(new RestMappingItemVersion[items.size()]));
		return results;
	}

	/**
	 * @param id - A UUID or nid of a semantic that identifies a map item.
	 * @param expand - A comma separated list of fields to expand. Supports 'referencedDetails,comments'. When referencedDetails is passed,
	 *            descriptions will be included for all referenced concepts which align with your current coordinates. When comments is passed, all
	 *            comments attached to each mapItem are included.
	 * @param processId if set, specifies that retrieved components should be checked against the specified active
	 *            workflow process, and if existing in the process, only the version of the corresponding object prior to the version referenced
	 *            in the workflow process should be returned or referenced. If no version existed prior to creation of the workflow process,
	 *            then either no object will be returned or an exception will be thrown, depending on context.
	 * @param coordToken specifies an explicit serialized CoordinatesToken string specifying all coordinate parameters. A CoordinatesToken may
	 *            be obtained by a separate (prior) call to getCoordinatesToken().
	 * @param altId - (optional) the altId type(s) to populate in any returned RestIdentifiedObject structures.  By default, no alternate IDs are 
	 *     returned.  This can be set to one or more names or ids from the /1/id/types or the value 'ANY'.  Requesting IDs that are unneeded will harm 
	 *     performance. 
	 * @return the mapping item version object.
	 * 
	 * @throws RestException
	 */
	@GET
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Path(RestPaths.mappingItemComponent + "{" + RequestParameters.id + "}")
	public RestMappingItemVersion getMappingItem(@PathParam(RequestParameters.id) String id, @QueryParam(RequestParameters.expand) String expand,
			@QueryParam(RequestParameters.processId) String processId, @QueryParam(RequestParameters.coordToken) String coordToken,
			@QueryParam(RequestParameters.altId) String altId) throws RestException
	{
		SecurityUtils.validateRole(securityContext, getClass());

		RequestParameters.validateParameterNamesAgainstSupportedNames(RequestInfo.get().getParameters(), RequestParameters.id, RequestParameters.expand,
				RequestParameters.processId, RequestParameters.COORDINATE_PARAM_NAMES, RequestParameters.altId);

		int nid = RequestInfoUtils.getSemanticNidFromParameter(RequestParameters.id, id);

		UUID processIdUUID = Util.validateWorkflowProcess(processId);
		SemanticChronology semantic = Get.assemblageService().getSemanticChronology(nid);

		Positions positions = Positions.getPositions(semantic.getAssemblageNid());

		LatestVersion<DynamicVersion<?>> latest = semantic.getLatestVersion(Util.getPreWorkflowStampCoordinate(processIdUUID, semantic.getNid()));
		Util.logContradictions(log, latest);

		List<RestMappingSetDisplayField> displayFields = MappingAPIs.getMappingSetDisplayFieldsFromMappingSet(semantic.getAssemblageNid(),
				RequestInfo.get().getStampCoordinate());

		if (latest.isPresent())
		{
			// TODO handle contradictions
			return new RestMappingItemVersion(latest.get(), positions.targetPos, positions.qualfierPos,
					RequestInfo.get().shouldExpand(ExpandUtil.referencedDetails), RequestInfo.get().shouldExpand(ExpandUtil.comments), processIdUUID,
					displayFields);
		}
		else
		{
			throw new RestException("The specified map item is not available on the specified coordinate");
		}
	}

	/**
	 * This method retrieves the map set display fields on the respective semantic attached to the map set concept,
	 * if it exists
	 * 
	 * @param mappingConceptNid the NID for the map set concept from which to extract the list of RestMappingSetDisplayField
	 * @param stampCoord the StampCoordinate with which to request the latest version of the display fields semantic, if it exists
	 * @return The results
	 */
	@SuppressWarnings("unchecked")
	public static List<RestMappingSetDisplayField> getMappingSetDisplayFieldsFromMappingSet(int mappingConceptNid, StampCoordinate stampCoord)
	{
		List<RestMappingSetDisplayField> fields = new ArrayList<>();
		Optional<SemanticChronology> mapSetFieldsSemantic = Frills.getAnnotationSemantic(mappingConceptNid,
				IsaacMappingConstants.get().DYNAMIC_SEMANTIC_MAPPING_DISPLAY_FIELDS.getNid());
		List<RestDynamicSemanticColumnInfo> itemFieldDefinitions = getItemFieldDefinitions(mappingConceptNid);
		if (mapSetFieldsSemantic.isPresent())
		{
			LatestVersion<DynamicVersion<?>> existingVersionOptionalLatest = mapSetFieldsSemantic.get().getLatestVersion(stampCoord);
			Util.logContradictions(log, existingVersionOptionalLatest);
			if (!existingVersionOptionalLatest.isPresent())
			{ // TODO Handle contradictions
				throw new RuntimeException(
						"No latest version of mapSetFieldsSemantic " + mapSetFieldsSemantic.get().getNid() + " found for specified stamp coordinate " + stampCoord);
			}
			DynamicData[] existingData = existingVersionOptionalLatest.get().getData();
			DynamicArrayImpl<DynamicStringImpl> mapSetFieldsSemanticDataArray = (existingData != null && existingData.length > 0)
					? (DynamicArrayImpl<DynamicStringImpl>) existingData[0]
					: null;
			if (mapSetFieldsSemanticDataArray != null && mapSetFieldsSemanticDataArray.getDataArray() != null
					&& mapSetFieldsSemanticDataArray.getDataArray().length > 0)
			{
				for (DynamicStringImpl stringSemantic : (DynamicStringImpl[]) mapSetFieldsSemanticDataArray.getDataArray())
				{
					String[] fieldComponents = stringSemantic.getDataString().split(":");
					MapSetItemComponent componentType = MapSetItemComponent.valueOf(fieldComponents[1]);
					if (componentType == MapSetItemComponent.ITEM_EXTENDED)
					{
						// If ITEM_EXTENDED then description is from itemFieldDefinitions
						int col = Integer.parseUnsignedInt(fieldComponents[0]);
						UUID id = null;
						for (RestDynamicSemanticColumnInfo def : itemFieldDefinitions)
						{
							if (def.columnOrder == col)
							{
								id = def.columnLabelConcept.getFirst();
								break;
							}
						}
						if (id == null)
						{
							String msg = "Failed correlating item display field id " + col + " for item display field of type " + componentType
									+ " with any existing extended field definition in map set";
							log.error(msg);
							throw new RuntimeException(msg);
						}
						fields.add(new RestMappingSetDisplayField(id, col));
					}
					else
					{
						UUID id = UUID.fromString(fieldComponents[0]);
						fields.add(new RestMappingSetDisplayField(new ConceptProxy("", id), componentType));
					}
				}
			}
		}

		// Add defaults if empty
		if (fields.size() == 0)
		{
			fields.addAll(getDefaultDisplayFields(itemFieldDefinitions));
		}

		return fields;
	}

	public static List<RestDynamicSemanticColumnInfo> getItemFieldDefinitions(int mappingConceptNid)
	{
		List<RestDynamicSemanticColumnInfo> mapItemFieldsDefinition = new ArrayList<>();

		// read the extended field definition information
		DynamicUsageDescription dsud = DynamicUsageDescriptionImpl.read(mappingConceptNid);
		// There are two columns used to store the target concept and qualifier, we shouldn't return them here.
		Positions positions;
		try
		{
			positions = Positions.getPositions(dsud);
		}
		catch (RestException e1)
		{
			throw new RuntimeException(e1);
		}

		int offset = 0;

		for (int i = 0; i < dsud.getColumnInfo().length; i++)
		{
			if (i == positions.targetPos || i == positions.qualfierPos)
			{
				offset++;
			}
			else
			{
				mapItemFieldsDefinition.add(new RestDynamicSemanticColumnInfo(dsud.getColumnInfo()[i]));
				mapItemFieldsDefinition.get(i - offset).columnOrder = mapItemFieldsDefinition.get(i - offset).columnOrder - offset;
			}
		}

		return mapItemFieldsDefinition;
	}

	protected static List<RestMappingSetDisplayField> getDefaultDisplayFields(List<RestDynamicSemanticColumnInfo> mapItemFieldsDefinition)
	{
		List<RestMappingSetDisplayField> displayFields = new ArrayList<>();
		displayFields.add(new RestMappingSetDisplayField(IsaacMappingConstants.get().MAPPING_CODE_DESCRIPTION, MapSetItemComponent.SOURCE));
		displayFields.add(new RestMappingSetDisplayField(IsaacMappingConstants.get().MAPPING_CODE_DESCRIPTION, MapSetItemComponent.TARGET));
		displayFields.add(new RestMappingSetDisplayField(IsaacMappingConstants.get().MAPPING_CODE_DESCRIPTION, MapSetItemComponent.EQUIVALENCE_TYPE));
		if (mapItemFieldsDefinition != null)
		{
			for (RestDynamicSemanticColumnInfo itemExtendedFieldCol : mapItemFieldsDefinition)
			{
				displayFields.add(new RestMappingSetDisplayField(itemExtendedFieldCol.columnLabelConcept.uuids.get(0), itemExtendedFieldCol.columnOrder));
			}
		}

		return displayFields;
	}
}
