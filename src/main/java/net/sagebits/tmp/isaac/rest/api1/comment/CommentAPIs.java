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

package net.sagebits.tmp.isaac.rest.api1.comment;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.UUID;
import javax.annotation.security.RolesAllowed;
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
import net.sagebits.tmp.isaac.rest.Util;
import net.sagebits.tmp.isaac.rest.api.exceptions.RestException;
import net.sagebits.tmp.isaac.rest.api1.RestPaths;
import net.sagebits.tmp.isaac.rest.api1.data.comment.RestCommentVersion;
import net.sagebits.tmp.isaac.rest.api1.semantic.SemanticAPIs;
import net.sagebits.tmp.isaac.rest.session.RequestInfo;
import net.sagebits.tmp.isaac.rest.session.RequestInfoUtils;
import net.sagebits.tmp.isaac.rest.session.RequestParameters;
import net.sagebits.uts.auth.data.UserRole.SystemRoleConstants;
import sh.isaac.api.Get;
import sh.isaac.api.chronicle.LatestVersion;
import sh.isaac.api.component.semantic.SemanticChronology;
import sh.isaac.api.component.semantic.version.DynamicVersion;
import sh.isaac.api.constants.DynamicConstants;
import sh.isaac.api.coordinate.StampCoordinate;

/**
 * {@link CommentAPIs}
 * 
 * @author <a href="mailto:daniel.armbrust.list@sagebits.net">Dan Armbrust</a>
 */
@Path(RestPaths.commentAPIsPathComponent)
@RolesAllowed({ SystemRoleConstants.AUTOMATED, SystemRoleConstants.ADMINISTRATOR, SystemRoleConstants.SYSTEM_MANAGER, SystemRoleConstants.CONTENT_MANAGER,
	SystemRoleConstants.EDITOR, SystemRoleConstants.READ })
public class CommentAPIs
{
	private static Logger log = LogManager.getLogger(CommentAPIs.class);

	@Context
	private SecurityContext securityContext;

	/**
	 * Returns a single version of a comment {@link RestCommentVersion}.
	 * 
	 * @param id - A UUID or nid that identifies the comment.
	 * @param processId if set, specifies that retrieved components should be checked against the specified active
	 *            workflow process, and if existing in the process, only the version of the corresponding object prior to the version referenced
	 *            in the workflow process should be returned or referenced. If no version existed prior to creation of the workflow process,
	 *            then either no object will be returned or an exception will be thrown, depending on context.
	 * @param coordToken specifies an explicit serialized CoordinatesToken string specifying all coordinate parameters. A CoordinatesToken may
	 *            be obtained by a separate (prior) call to getCoordinatesToken().
	 * @return the comment version object {@link RestCommentVersion}.
	 * 
	 * @throws RestException
	 */
	@GET
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Path(RestPaths.versionComponent + "{" + RequestParameters.id + "}")
	public RestCommentVersion getCommentVersion(@PathParam(RequestParameters.id) String id, @QueryParam(RequestParameters.processId) String processId,
			@QueryParam(RequestParameters.coordToken) String coordToken) throws RestException
	{
		RequestParameters.validateParameterNamesAgainstSupportedNames(RequestInfo.get().getParameters(), RequestParameters.id, RequestParameters.processId,
				RequestParameters.COORDINATE_PARAM_NAMES);

		SemanticChronology sc = SemanticAPIs.findSemanticChronology(id);
		LatestVersion<DynamicVersion<?>> sv = sc.getLatestVersion(Util.getPreWorkflowStampCoordinate(processId, sc.getNid()));
		if (sv.isPresent())
		{
			// TODO handle contradictions
			Util.logContradictions(log, sv);
			return new RestCommentVersion(sv.get());
		}

		throw new RestException(RequestParameters.id, id, "No comment was found on the given coordinate");
	}

	/**
	 * Returns an array containing current version of any and all comments attached to a referenced component. Note, this is not multiple versions
	 * of a single comment, rather, multiple comments (0 to n) at the version specified by the view coordinate.
	 * 
	 * @param id - A UUID or nid of the component being referenced by a comment
	 * @param processId if set, specifies that retrieved components should be checked against the specified active
	 *            workflow process, and if existing in the process, only the version of the corresponding object prior to the version referenced
	 *            in the workflow process should be returned or referenced. If no version existed prior to creation of the workflow process,
	 *            then either no object will be returned or an exception will be thrown, depending on context.
	 * @param coordToken specifies an explicit serialized CoordinatesToken string specifying all coordinate parameters. A CoordinatesToken may
	 *            be obtained by a separate (prior) call to getCoordinatesToken().
	 * @return the comment version object.
	 * 
	 * @throws RestException
	 */
	@GET
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Path(RestPaths.versionComponent + RestPaths.forReferencedComponentComponent + "{" + RequestParameters.id + "}")
	public RestCommentVersion[] getCommentsForReferencedItem(@PathParam(RequestParameters.id) String id,
			@QueryParam(RequestParameters.processId) String processId, @QueryParam(RequestParameters.coordToken) String coordToken) throws RestException
	{
		RequestParameters.validateParameterNamesAgainstSupportedNames(RequestInfo.get().getParameters(), RequestParameters.id, RequestParameters.processId,
				RequestParameters.COORDINATE_PARAM_NAMES);

		ArrayList<RestCommentVersion> temp = readComments(id, Util.validateWorkflowProcess(processId));
		return temp.toArray(new RestCommentVersion[temp.size()]);
	}

	/**
	 * Return the latest version of each unique comment attached to an object, sorted from oldest to newest.
	 * 
	 * @param id the nid of UUID of the object to check for comments on
	 * @param processId 
	 * @return The comment(s)
	 * @throws RestException
	 */
	public static ArrayList<RestCommentVersion> readComments(String id, UUID processId) throws RestException
	{
		return readComments(id, processId, RequestInfo.get().getStampCoordinate());
	}

	public static ArrayList<RestCommentVersion> readComments(String id, UUID processId, StampCoordinate sc) throws RestException
	{
		ArrayList<RestCommentVersion> results = new ArrayList<>();

		Get.assemblageService()
				.getSemanticChronologyStreamForComponentFromAssemblage(RequestInfoUtils.getNidFromParameter(RequestParameters.id, id), DynamicConstants.get().DYNAMIC_COMMENT_ATTRIBUTE.getNid())
				.forEach(semanticChronology -> {
					@SuppressWarnings({ "rawtypes"})
					LatestVersion<DynamicVersion> sv = semanticChronology
							.getLatestVersion(Util.getPreWorkflowStampCoordinate(processId, semanticChronology.getNid()));
					if (sv.isPresent())
					{
						// TODO handle contradictions
						Util.logContradictions(log, sv);
						results.add(new RestCommentVersion(sv.get()));
					}

				});

		results.sort(new Comparator<RestCommentVersion>()
		{
			@Override
			public int compare(RestCommentVersion o1, RestCommentVersion o2)
			{
				return Long.compare(o1.commentStamp.time, o2.commentStamp.time);
			}
		});
		return results;

	}
}
