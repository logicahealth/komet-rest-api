/**
 * Copyright Notice
 *
 * This is a work of the U.S. Government and is not subject to copyright
 * protection in the United States. Foreign copyrights may apply.
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
 */
package net.sagebits.tmp.isaac.rest.api1.classifier;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
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
import org.apache.commons.lang3.StringUtils;
import net.sagebits.tmp.isaac.rest.api.exceptions.RestException;
import net.sagebits.tmp.isaac.rest.api1.RestPaths;
import net.sagebits.tmp.isaac.rest.api1.data.classifier.ClassifierResult;
import net.sagebits.tmp.isaac.rest.api1.data.classifier.ClassifierRunStorage;
import net.sagebits.tmp.isaac.rest.api1.data.classifier.RestClassifierResult;
import net.sagebits.tmp.isaac.rest.session.RequestInfo;
import net.sagebits.tmp.isaac.rest.session.RequestParameters;
import net.sagebits.tmp.isaac.rest.session.SecurityUtils;
import sh.isaac.api.util.UUIDUtil;
import sh.isaac.misc.security.SystemRoleConstants;

/**
 * {@link ClassifierReadAPIs}
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
@Path(RestPaths.classifierAPIsPathComponent)
@RolesAllowed({ SystemRoleConstants.AUTOMATED, SystemRoleConstants.SUPER_USER, SystemRoleConstants.ADMINISTRATOR, SystemRoleConstants.READ_ONLY,
	SystemRoleConstants.EDITOR, SystemRoleConstants.REVIEWER, SystemRoleConstants.APPROVER, SystemRoleConstants.DEPLOYMENT_MANAGER })
public class ClassifierReadAPIs
{
	@Context
	private SecurityContext securityContext;

	/**
	 * Get the current information on the specified classifier run
	 * @param id - The ID of a classifier run.  These UUIDs are returned via the call that launches the classifier.
	 * @param largeResults - If false, or unspecified, all parts of the result that return lists or arrays will be limited to 100.  
	 *     To include all details,set this to true.
	 * @param coordToken specifies an explicit serialized CoordinatesToken string specifying all coordinate parameters. A CoordinatesToken may be
	 *            obtained by a separate (prior) call to getCoordinatesToken().
	 * @param altId - (optional) the altId type(s) to populate in any returned RestIdentifiedObject structures.  By default, no alternate IDs are 
	 *     returned.  This can be set to one or more names or ids from the /1/id/types or the value 'ANY'.  Requesting IDs that are unneeded will harm 
	 *     performance. 
	 * @return The details on the classification.
	 * @throws RestException
	 */
	@GET
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Path(RestPaths.classification + "{" + RequestParameters.id + "}")
	public RestClassifierResult read(@PathParam(RequestParameters.id) String id, 
			@QueryParam(RequestParameters.largeResults) @DefaultValue("false") String largeResults, 
			@QueryParam(RequestParameters.coordToken) String coordToken,
			@QueryParam(RequestParameters.altId) String altId) throws RestException
	{
		SecurityUtils.validateRole(securityContext, getClass());

		RequestParameters.validateParameterNamesAgainstSupportedNames(RequestInfo.get().getParameters(), RequestParameters.id, 
				RequestParameters.largeResults, RequestParameters.COORDINATE_PARAM_NAMES, RequestParameters.altId);
		
		if (StringUtils.isBlank(id))
		{
			throw new RestException("id", "The id for the classification run is required");
		}
		
		Optional<UUID> classifyKey = UUIDUtil.getUUID(id.trim());
		if (!classifyKey.isPresent())
		{
			throw new RestException("id", id, "The id for the classification run must be a valid UUID");
		}
		
		ClassifierResult cr = ClassifierRunStorage.getClassificationResults(classifyKey.get());
		if (cr == null)
		{
			throw new RestException("id", id, "No data was located for the specified classifier run");
		}
		return new RestClassifierResult(cr, !Boolean.parseBoolean(largeResults.trim()));
	}
	
	/**
	 * Get the current information on all known classifier runs, ordered from most recently run to oldest run
	 * @param largeResults - If false, or unspecified, all parts of the result that return lists or arrays will be limited to 100.  
	 *     To include all details,set this to true.
	 * @param coordToken specifies an explicit serialized CoordinatesToken string specifying all coordinate parameters. A CoordinatesToken may be
	 *            obtained by a separate (prior) call to getCoordinatesToken().
	 * @param altId - (optional) the altId type(s) to populate in any returned RestIdentifiedObject structures.  By default, no alternate IDs are 
	 *     returned.  This can be set to one or more names or ids from the /1/id/types or the value 'ANY'.  Requesting IDs that are unneeded will harm 
	 *     performance. 
	 * @return The details on all of the classifications that have occurred.
	 * @throws RestException
	 */
	@GET
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Path(RestPaths.classifications)
	public RestClassifierResult[] readAll(@QueryParam(RequestParameters.largeResults) @DefaultValue("false") String largeResults,  
			@QueryParam(RequestParameters.coordToken) String coordToken,
			@QueryParam(RequestParameters.altId) String altId) throws RestException
	{
		SecurityUtils.validateRole(securityContext, getClass());

		RequestParameters.validateParameterNamesAgainstSupportedNames(RequestInfo.get().getParameters(), 
				RequestParameters.largeResults, RequestParameters.COORDINATE_PARAM_NAMES, RequestParameters.altId);

		List<ClassifierResult> cr = ClassifierRunStorage.getClassificationResults();
		Collections.sort(cr);
		boolean limitResults = !Boolean.parseBoolean(largeResults.trim());
		
		RestClassifierResult[] rcr = new RestClassifierResult[cr.size()];
		for(int i = 0; i < rcr.length; i++)
		{
			rcr[i] = new RestClassifierResult(cr.get(i), limitResults);
		}
		return rcr;
	}
}