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

package net.sagebits.tmp.isaac.rest.api1.association;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
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
import sh.isaac.api.Get;
import sh.isaac.misc.security.SystemRoleConstants;
import sh.isaac.api.chronicle.LatestVersion;
import sh.isaac.api.component.semantic.version.DynamicVersion;
import sh.isaac.api.coordinate.StampCoordinate;
import sh.isaac.api.util.AlphanumComparator;
import sh.isaac.misc.associations.AssociationInstance;
import sh.isaac.misc.associations.AssociationType;
import sh.isaac.misc.associations.AssociationUtilities;
import net.sagebits.tmp.isaac.rest.Util;
import net.sagebits.tmp.isaac.rest.api.exceptions.RestException;
import net.sagebits.tmp.isaac.rest.api1.RestPaths;
import net.sagebits.tmp.isaac.rest.api1.data.association.RestAssociationItemVersion;
import net.sagebits.tmp.isaac.rest.api1.data.association.RestAssociationItemVersionPage;
import net.sagebits.tmp.isaac.rest.api1.data.association.RestAssociationTypeVersion;
import net.sagebits.tmp.isaac.rest.session.RequestInfo;
import net.sagebits.tmp.isaac.rest.session.RequestInfoUtils;
import net.sagebits.tmp.isaac.rest.session.RequestParameters;
import net.sagebits.tmp.isaac.rest.session.SecurityUtils;

/**
 * {@link AssociationAPIs}
 * 
 * @author <a href="mailto:daniel.armbrust.list@sagebits.net">Dan Armbrust</a>
 */
@Path(RestPaths.associationAPIsPathComponent)
@RolesAllowed({ SystemRoleConstants.SUPER_USER, SystemRoleConstants.ADMINISTRATOR, SystemRoleConstants.READ_ONLY, SystemRoleConstants.EDITOR,
		SystemRoleConstants.REVIEWER, SystemRoleConstants.APPROVER, SystemRoleConstants.DEPLOYMENT_MANAGER })
public class AssociationAPIs
{
	@Context
	private SecurityContext securityContext;

	/**
	 * Get all defined association types in the system.
	 * 
	 * @param processId if set, specifies that retrieved components should be checked against the specified active
	 *            workflow process, and if existing in the process, only the version of the corresponding object prior to the version referenced
	 *            in the workflow process should be returned or referenced. If no version existed prior to creation of the workflow process,
	 *            then either no object will be returned or an exception will be thrown, depending on context.
	 * @param coordToken specifies an explicit serialized CoordinatesToken string specifying all coordinate parameters. A CoordinatesToken may
	 *            be obtained by a separate (prior) call to getCoordinatesToken().
	 * @param expand - the optional items to be expanded. Supports 'referencedConcept' If 'referencedConcept' is passed, you can also pass
	 *            'versionsAll' or 'versionsLatestOnly'
	 * @param altId - (optional) the altId type(s) to populate in any returned RestIdentifiedObject structures.  By default, no alternate IDs are 
	 *     returned.  This can be set to one or more names or ids from the /1/id/types or the value 'ANY'.  Requesting IDs that are unneeded will harm 
	 *     performance. 
	 * @return the latest version of each unique association definition found in the system on the specified coordinates, sorted by the association
	 *         name.
	 * 
	 * @throws RestException
	 */
	// TODO add a filter capability to this by terminology
	@GET
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Path(RestPaths.associationsComponent)
	public RestAssociationTypeVersion[] getAssociations(@QueryParam(RequestParameters.processId) String processId,
			@QueryParam(RequestParameters.coordToken) String coordToken, @QueryParam(RequestParameters.expand) String expand,
			@QueryParam(RequestParameters.altId) String altId) throws RestException
	{
		SecurityUtils.validateRole(securityContext, getClass());

		RequestParameters.validateParameterNamesAgainstSupportedNames(RequestInfo.get().getParameters(), RequestParameters.expand, RequestParameters.processId,
				RequestParameters.COORDINATE_PARAM_NAMES, RequestParameters.altId);

		ArrayList<RestAssociationTypeVersion> results = new ArrayList<>();

		Set<Integer> associationConcepts = AssociationUtilities.getAssociationConceptNids();

		UUID processIdUUID = Util.validateWorkflowProcess(processId);

		for (int nid : associationConcepts)
		{
			StampCoordinate sc = Util.getPreWorkflowStampCoordinate(processId, nid);
			AssociationType associationTypeToAdd = AssociationType.read(nid, sc, RequestInfo.get().getLanguageCoordinate());

			if (associationTypeToAdd != null)
			{
				results.add(new RestAssociationTypeVersion(associationTypeToAdd, processIdUUID));
			}
		}

		Collections.sort(results, new Comparator<RestAssociationTypeVersion>()
		{

			@Override
			public int compare(RestAssociationTypeVersion o1, RestAssociationTypeVersion o2)
			{
				return AlphanumComparator.compare(o1.associationName, o2.associationName, true);
			}
		});

		return results.toArray(new RestAssociationTypeVersion[results.size()]);
	}

	/**
	 * Return the description of a particular association type
	 * 
	 * @param id - A UUID or nid of a concept that defines an association type
	 * @param coordToken specifies an explicit serialized CoordinatesToken string specifying all coordinate parameters. A CoordinatesToken may
	 *            be obtained by a separate (prior) call to getCoordinatesToken().
	 * @param processId if set, specifies that retrieved components should be checked against the specified active
	 *            workflow process, and if existing in the process, only the version of the corresponding object prior to the version referenced
	 *            in the workflow process should be returned or referenced. If no version existed prior to creation of the workflow process,
	 *            then either no object will be returned or an exception will be thrown, depending on context.
	 * @param expand - the optional items to be expanded. Supports 'referencedConcept' If 'referencedConcept' is passed, you can also pass
	 *            'versionsAll' or 'versionsLatestOnly'
	 * @param altId - (optional) the altId type(s) to populate in any returned RestIdentifiedObject structures.  By default, no alternate IDs are 
	 *     returned.  This can be set to one or more names or ids from the /1/id/types or the value 'ANY'.  Requesting IDs that are unneeded will harm 
	 *     performance. 
	 * @return the latest version of the specified associationType
	 * 
	 * @throws RestException
	 */
	@GET
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Path(RestPaths.associationComponent + "{" + RequestParameters.id + "}")
	public RestAssociationTypeVersion getAssociationType(@PathParam(RequestParameters.id) String id,
			@QueryParam(RequestParameters.coordToken) String coordToken, @QueryParam(RequestParameters.processId) String processId,
			@QueryParam(RequestParameters.expand) String expand,
			@QueryParam(RequestParameters.altId) String altId) throws RestException
	{
		SecurityUtils.validateRole(securityContext, getClass());

		RequestParameters.validateParameterNamesAgainstSupportedNames(RequestInfo.get().getParameters(), RequestParameters.expand, RequestParameters.processId,
				RequestParameters.PAGINATION_PARAM_NAMES, RequestParameters.COORDINATE_PARAM_NAMES, RequestParameters.altId);

		int nid = RequestInfoUtils.getConceptNidFromParameter(RequestParameters.id, id);
		StampCoordinate conceptVersionStampCoordinate = Util.getPreWorkflowStampCoordinate(processId, nid);
		return new RestAssociationTypeVersion(AssociationType.read(nid, conceptVersionStampCoordinate, RequestInfo.get().getLanguageCoordinate()),
				Util.validateWorkflowProcess(processId));
	}

	/**
	 * Return all instances of a particular type of association. This may return a very large result. The returned pagination data
	 * will return an exact number of total results (not an estimate, as indicated in the API)
	 * 
	 * @param id - A UUID or nid of a concept that defines an association type
	 * @param pageNum The pagination page number >= 1 to return
	 * @param maxPageSize The maximum number of results to return per page, must be greater than 0
	 * @param coordToken specifies an explicit serialized CoordinatesToken string specifying all coordinate parameters. A CoordinatesToken may
	 *            be obtained by a separate (prior) call to getCoordinatesToken().
	 * @param processId if set, specifies that retrieved components should be checked against the specified active
	 *            workflow process, and if existing in the process, only the version of the corresponding object prior to the version referenced
	 *            in the workflow process should be returned or referenced. If no version existed prior to creation of the workflow process,
	 *            then either no object will be returned or an exception will be thrown, depending on context.
	 * @param expand - the optional items to be expanded. Supports 'source', 'target', 'nestedSemantics'
	 *            When 'source' or 'target' is expanded, the following expand options are supported for expanded source and/or target: 'versionsAll',
	 *            'versionsLatestOnly'
	 *            When 'nestedSemantics' is expanded, the following expand options are supported for the nested semantics: 'referencedDetails',
	 *            'chronology'
	 * @param altId - (optional) the altId type(s) to populate in any returned RestIdentifiedObject structures.  By default, no alternate IDs are 
	 *     returned.  This can be set to one or more names or ids from the /1/id/types or the value 'ANY'.  Requesting IDs that are unneeded will harm 
	 *     performance. 
	 * @return the latest version of each unique association instance of type associationType
	 * 
	 * @throws RestException
	 */
	@GET
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Path(RestPaths.associationsWithTypeComponent + "{" + RequestParameters.id + "}")
	public RestAssociationItemVersionPage getAssociationsOfType(@PathParam(RequestParameters.id) String id,
			@QueryParam(RequestParameters.pageNum) @DefaultValue(RequestParameters.pageNumDefault) int pageNum,
			@QueryParam(RequestParameters.maxPageSize) @DefaultValue(RequestParameters.maxPageSizeDefault) int maxPageSize,
			@QueryParam(RequestParameters.coordToken) String coordToken, @QueryParam(RequestParameters.processId) String processId,
			@QueryParam(RequestParameters.expand) String expand,
			@QueryParam(RequestParameters.altId) String altId) throws RestException
	{
		SecurityUtils.validateRole(securityContext, getClass());

		RequestParameters.validateParameterNamesAgainstSupportedNames(RequestInfo.get().getParameters(), RequestParameters.expand, RequestParameters.processId,
				RequestParameters.PAGINATION_PARAM_NAMES, RequestParameters.COORDINATE_PARAM_NAMES, RequestParameters.altId);

		UUID processIdUUID = Util.validateWorkflowProcess(processId);

		ArrayList<RestAssociationItemVersion> results;
		try
		{
			results = new ArrayList<>();
			AtomicInteger total = new AtomicInteger(0);
			int start = (pageNum * maxPageSize) - maxPageSize;
			Get.assemblageService().getSemanticChronologyStream(RequestInfoUtils.getConceptNidFromParameter(RequestParameters.id, id)).forEach(associationC -> {
				total.incrementAndGet();
				try
				{
					LatestVersion<DynamicVersion<?>> latest = associationC.getLatestVersion(RequestInfo.get().getStampCoordinate());
					if (latest.isPresent())
					{
						if (total.get() <= start || results.size() >= maxPageSize)
						{
							// noop - can't short-circuit (plus want the count)
						}
						else
						{
							results.add(new RestAssociationItemVersion(AssociationInstance.read(latest.get(), RequestInfo.get().getStampCoordinate()),
									processIdUUID));
						}
					}
				}
				catch (Exception e)
				{
					throw new RuntimeException(e);
				}
			});
			return new RestAssociationItemVersionPage(pageNum, maxPageSize, total.get(), true, total.get() > (pageNum * maxPageSize),
					RestPaths.associationAPIsPathComponent + RestPaths.associationsWithTypeComponent + id,
					results.toArray(new RestAssociationItemVersion[results.size()]));
		}
		catch (RuntimeException e)
		{
			if (e.getCause() != null && e.getCause() instanceof RestException)
			{
				throw (RestException) e.getCause();
			}
			else
			{
				throw e;
			}
		}
	}

	/**
	 * Return all association instances that involve the specified source component.
	 * 
	 * @param id - A UUID or nid (of a concept or semantic) that must be the source portion of the returned association item.
	 * @param coordToken specifies an explicit serialized CoordinatesToken string specifying all coordinate parameters. A CoordinatesToken may
	 *            be obtained by a separate (prior) call to getCoordinatesToken().
	 * @param processId if set, specifies that retrieved components should be checked against the specified active
	 *            workflow process, and if existing in the process, only the version of the corresponding object prior to the version referenced
	 *            in the workflow process should be returned or referenced. If no version existed prior to creation of the workflow process,
	 *            then either no object will be returned or an exception will be thrown, depending on context.
	 * @param expand - the optional items to be expanded. Supports 'source', 'target', 'nestedSemantics'
	 *            When 'source' or 'target' is expanded, the following expand options are supported for expanded source and/or target: 'versionsAll',
	 *            'versionsLatestOnly'
	 *            When 'nestedSemantics' is expanded, the following expand options are supported for the nested semantics: 'referencedDetails',
	 *            'chronology'
	 * @param altId - (optional) the altId type(s) to populate in any returned RestIdentifiedObject structures.  By default, no alternate IDs are 
	 *     returned.  This can be set to one or more names or ids from the /1/id/types or the value 'ANY'.  Requesting IDs that are unneeded will harm 
	 *     performance. 
	 * @return the latest version of each unique association that has a source component equal to 'id'
	 * 
	 * @throws RestException
	 */
	@GET
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Path(RestPaths.associationsWithSourceComponent + "{" + RequestParameters.id + "}")
	public RestAssociationItemVersion[] getSourceAssociations(@PathParam(RequestParameters.id) String id,
			@QueryParam(RequestParameters.coordToken) String coordToken, @QueryParam(RequestParameters.processId) String processId,
			@QueryParam(RequestParameters.expand) String expand,
			@QueryParam(RequestParameters.altId) String altId) throws RestException
	{
		SecurityUtils.validateRole(securityContext, getClass());

		RequestParameters.validateParameterNamesAgainstSupportedNames(RequestInfo.get().getParameters(), RequestParameters.expand, RequestParameters.processId,
				RequestParameters.COORDINATE_PARAM_NAMES, RequestParameters.altId);

		UUID processIdUUID = Util.validateWorkflowProcess(processId);

		List<AssociationInstance> results = AssociationUtilities.getSourceAssociations(RequestInfoUtils.getNidFromParameter(RequestParameters.id, id),
				RequestInfo.get().getStampCoordinate());
		RestAssociationItemVersion[] finalResult = new RestAssociationItemVersion[results.size()];
		for (int i = 0; i < results.size(); i++)
		{
			finalResult[i] = new RestAssociationItemVersion(results.get(i), processIdUUID);
		}
		return finalResult;

	}

	/**
	 * Return all association instances that involve the specified target component.
	 * 
	 * @param id - A UUID or nid (of a concept or semantic) that must be the target portion of the returned association item.
	 * @param coordToken specifies an explicit serialized CoordinatesToken string specifying all coordinate parameters. A CoordinatesToken may
	 *            be obtained by a separate (prior) call to getCoordinatesToken().
	 * @param processId if set, specifies that retrieved components should be checked against the specified active
	 *            workflow process, and if existing in the process, only the version of the corresponding object prior to the version referenced
	 *            in the workflow process should be returned or referenced. If no version existed prior to creation of the workflow process,
	 *            then either no object will be returned or an exception will be thrown, depending on context.
	 * @param expand - the optional items to be expanded. Supports 'source', 'target', 'nestedSemantics'
	 *            When 'source' or 'target' is expanded, the following expand options are supported for expanded source and/or target: 'versionsAll',
	 *            'versionsLatestOnly'
	 *            When 'nestedSemantics' is expanded, the following expand options are supported for the nested semantics: 'referencedDetails',
	 *            'chronology'
	 * @param altId - (optional) the altId type(s) to populate in any returned RestIdentifiedObject structures.  By default, no alternate IDs are 
	 *     returned.  This can be set to one or more names or ids from the /1/id/types or the value 'ANY'.  Requesting IDs that are unneeded will harm 
	 *     performance. 
	 * @return the latest version of each unique association that has a source component equal to 'id'
	 * 
	 * @throws RestException
	 */
	@GET
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Path(RestPaths.associationsWithTargetComponent + "{" + RequestParameters.id + "}")
	public RestAssociationItemVersion[] getTargetAssociations(@PathParam(RequestParameters.id) String id,
			@QueryParam(RequestParameters.coordToken) String coordToken, @QueryParam(RequestParameters.processId) String processId,
			@QueryParam(RequestParameters.expand) String expand,
			@QueryParam(RequestParameters.altId) String altId) throws RestException
	{
		SecurityUtils.validateRole(securityContext, getClass());

		RequestParameters.validateParameterNamesAgainstSupportedNames(RequestInfo.get().getParameters(), RequestParameters.expand, RequestParameters.processId,
				RequestParameters.COORDINATE_PARAM_NAMES, RequestParameters.altId);

		// TODO Dan lookup by target performance is not good at the moment, not sure why
		UUID processIdUUID = Util.validateWorkflowProcess(processId);
		List<AssociationInstance> results = AssociationUtilities.getTargetAssociations(RequestInfoUtils.getNidFromParameter(RequestParameters.id, id),
				RequestInfo.get().getStampCoordinate());
		RestAssociationItemVersion[] finalResult = new RestAssociationItemVersion[results.size()];
		for (int i = 0; i < results.size(); i++)
		{
			finalResult[i] = new RestAssociationItemVersion(results.get(i), processIdUUID);
		}
		return finalResult;

	}

	/**
	 * Return a particular association item
	 * 
	 * @param id - A UUID or nid of a semantic association instance.
	 * @param coordToken specifies an explicit serialized CoordinatesToken string specifying all coordinate parameters. A CoordinatesToken may
	 *            be obtained by a separate (prior) call to getCoordinatesToken().
	 * @param processId if set, specifies that retrieved components should be checked against the specified active
	 *            workflow process, and if existing in the process, only the version of the corresponding object prior to the version referenced
	 *            in the workflow process should be returned or referenced. If no version existed prior to creation of the workflow process,
	 *            then either no object will be returned or an exception will be thrown, depending on context.
	 * @param expand - the optional items to be expanded. Supports 'source', 'target', 'nestedSemantics'
	 *            When 'source' or 'target' is expanded, the following expand options are supported for expanded source and/or target: 'versionsAll',
	 *            'versionsLatestOnly'
	 *            When 'nestedSemantics' is expanded, the following expand options are supported for the nested semantics: 'referencedDetails',
	 *            'chronology'
	 * @param altId - (optional) the altId type(s) to populate in any returned RestIdentifiedObject structures.  By default, no alternate IDs are 
	 *     returned.  This can be set to one or more names or ids from the /1/id/types or the value 'ANY'.  Requesting IDs that are unneeded will harm 
	 *     performance. 
	 *            
	 * @return the latest version of the requested association on the provided coordinate. May return null, if the association isn't available on
	 *         the specified coordinate.
	 * 
	 * @throws RestException
	 */
	@GET
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Path(RestPaths.associationItemComponent + "{" + RequestParameters.id + "}")
	public RestAssociationItemVersion getAssociation(@PathParam(RequestParameters.id) String id, @QueryParam(RequestParameters.coordToken) String coordToken,
			@QueryParam(RequestParameters.processId) String processId, @QueryParam(RequestParameters.expand) String expand,
			@QueryParam(RequestParameters.altId) String altId) throws RestException
	{
		SecurityUtils.validateRole(securityContext, getClass());

		RequestParameters.validateParameterNamesAgainstSupportedNames(RequestInfo.get().getParameters(), RequestParameters.expand, RequestParameters.processId,
				RequestParameters.COORDINATE_PARAM_NAMES, RequestParameters.altId);

		Optional<AssociationInstance> result = AssociationUtilities.getAssociation(RequestInfoUtils.getSemanticNidFromParameter(RequestParameters.id, id),
				RequestInfo.get().getStampCoordinate());

		return result.isPresent() ? new RestAssociationItemVersion(result.get(), Util.validateWorkflowProcess(processId)) : null;

	}
}
