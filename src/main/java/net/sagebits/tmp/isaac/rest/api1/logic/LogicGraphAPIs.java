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
package net.sagebits.tmp.isaac.rest.api1.logic;

import java.util.Optional;
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
import net.sagebits.tmp.isaac.rest.ExpandUtil;
import net.sagebits.tmp.isaac.rest.Util;
import net.sagebits.tmp.isaac.rest.api.exceptions.RestException;
import net.sagebits.tmp.isaac.rest.api1.RestPaths;
import net.sagebits.tmp.isaac.rest.api1.data.semantic.RestSemanticChronology;
import net.sagebits.tmp.isaac.rest.api1.data.semantic.RestSemanticLogicGraphVersion;
import net.sagebits.tmp.isaac.rest.session.RequestInfo;
import net.sagebits.tmp.isaac.rest.session.RequestInfoUtils;
import net.sagebits.tmp.isaac.rest.session.RequestParameters;
import net.sagebits.tmp.isaac.rest.session.SecurityUtils;
import sh.isaac.api.chronicle.LatestVersion;
import sh.isaac.api.component.semantic.SemanticChronology;
import sh.isaac.api.component.semantic.version.LogicGraphVersion;
import sh.isaac.api.coordinate.LanguageCoordinate;
import sh.isaac.api.coordinate.LogicCoordinate;
import sh.isaac.api.coordinate.StampCoordinate;
import sh.isaac.misc.security.SystemRoleConstants;
import sh.isaac.utility.Frills;

/**
 * {@link LogicGraphAPIs}
 *
 * @author <a href="mailto:joel.kniaz.list@gmail.com">Joel Kniaz</a>
 */

@Path(RestPaths.logicGraphAPIsPathComponent)
@RolesAllowed({ SystemRoleConstants.AUTOMATED, SystemRoleConstants.SUPER_USER, SystemRoleConstants.ADMINISTRATOR, SystemRoleConstants.READ_ONLY,
		SystemRoleConstants.EDITOR, SystemRoleConstants.REVIEWER, SystemRoleConstants.APPROVER, SystemRoleConstants.DEPLOYMENT_MANAGER })
public class LogicGraphAPIs
{
	@Context
	private SecurityContext securityContext;

	private static Logger log = LogManager.getLogger();

	/**
	 * Returns a single version of a logic graph.
	 * If no version parameter is specified, returns the latest version.
	 * 
	 * @param id - A UUID or nid identifying the concept at the root of the logic graph.
	 * @param expand - comma separated list of fields to expand. Supports 
	 *     'chronology' - to include the chronology of the semantic that carries the logic graph, 
	 *     'version'  - to include RestConceptVersion objects inside of RestConceptNode and RestTypedConnectorNode types.
	 *     'countParents' - may only be specified in combination with 'version' - will cause the expanded version to also have the parent count populated.
	 *     
	 * @param processId if set, specifies that retrieved components should be checked against the specified active
	 *            workflow process, and if existing in the process, only the version of the corresponding object prior to the version referenced
	 *            in the workflow process should be returned or referenced. If no version existed prior to creation of the workflow process,
	 *            then either no object will be returned or an exception will be thrown, depending on context.
	 * @param coordToken specifies an explicit serialized CoordinatesToken string specifying all coordinate parameters. A CoordinatesToken may be
	 *            obtained by a separate (prior) call to getCoordinatesToken().
	 * @return the logic graph version object
	 * @throws RestException
	 */
	@GET
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Path(RestPaths.versionComponent + "{" + RequestParameters.id + "}")
	public RestSemanticLogicGraphVersion getLogicGraphVersion(@PathParam(RequestParameters.id) String id, @QueryParam(RequestParameters.expand) String expand,
			@QueryParam(RequestParameters.processId) String processId, @QueryParam(RequestParameters.coordToken) String coordToken) throws RestException
	{
		SecurityUtils.validateRole(securityContext, getClass());

		RequestParameters.validateParameterNamesAgainstSupportedNames(RequestInfo.get().getParameters(), RequestParameters.id, RequestParameters.expand,
				RequestParameters.processId, RequestParameters.COORDINATE_PARAM_NAMES);

		// TODO bug - the methods below findLogicGraphChronology are relying on some default logic graph coordiantes... Also seems to be a lot
		// of optional to not optional to optional stuff going on below this call... look at cleaning up.
		// See impl in RestConceptVersion constructor
		SemanticChronology logicGraphSemanticChronology = findLogicGraphChronology(id, RequestInfo.get().getStated(), RequestInfo.get().getStampCoordinate(),
				RequestInfo.get().getLanguageCoordinate(), RequestInfo.get().getLogicCoordinate());

		UUID processIdUUID = Util.validateWorkflowProcess(processId);

		LatestVersion<LogicGraphVersion> lgs = logicGraphSemanticChronology
				.getLatestVersion(Util.getPreWorkflowStampCoordinate(processIdUUID, logicGraphSemanticChronology.getNid()));
		if (lgs.isPresent())
		{
			// TODO handle contradictions
			Util.logContradictions(log, lgs);
			return new RestSemanticLogicGraphVersion(lgs.get(), RequestInfo.get().shouldExpand(ExpandUtil.chronologyExpandable), processIdUUID);
		}
		throw new RestException(RequestParameters.id, id, "No concept was found");
	}

	/**
	 * Returns the chronology of a logic graph.
	 * 
	 * @param id - A UUID or nid, of a concept identifying the concept at the root of the logic graph
	 * @param expand - comma separated list of fields to expand. Supports 'versionsAll', 'versionsLatestOnly', 'logicNodeUuids' and/or 'version'
	 *            If latest only is specified in combination with versionsAll, it is ignored (all versions are returned)
	 * @param processId if set, specifies that retrieved components should be checked against the specified active
	 *            workflow process, and if existing in the process, only the version of the corresponding object prior to the version referenced
	 *            in the workflow process should be returned or referenced. If no version existed prior to creation of the workflow process,
	 *            then either no object will be returned or an exception will be thrown, depending on context.
	 * @param coordToken specifies an explicit serialized CoordinatesToken string specifying all coordinate parameters. A CoordinatesToken may be
	 *            obtained by a separate (prior) call to getCoordinatesToken().
	 * 
	 * @return the concept chronology object
	 * @throws RestException
	 */
	@GET
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Path(RestPaths.chronologyComponent + "{" + RequestParameters.id + "}")
	public RestSemanticChronology getLogicGraphChronology(@PathParam(RequestParameters.id) String id, @QueryParam(RequestParameters.expand) String expand,
			@QueryParam(RequestParameters.processId) String processId, @QueryParam(RequestParameters.coordToken) String coordToken) throws RestException
	{
		SecurityUtils.validateRole(securityContext, getClass());

		RequestParameters.validateParameterNamesAgainstSupportedNames(RequestInfo.get().getParameters(), RequestParameters.id, RequestParameters.expand,
				RequestParameters.processId, RequestParameters.COORDINATE_PARAM_NAMES);

		SemanticChronology logicGraphSemanticChronology = findLogicGraphChronology(id, RequestInfo.get().getStated(), RequestInfo.get().getStampCoordinate(),
				RequestInfo.get().getLanguageCoordinate(), RequestInfo.get().getLogicCoordinate());

		return new RestSemanticChronology(logicGraphSemanticChronology, RequestInfo.get().shouldExpand(ExpandUtil.versionsAllExpandable),
				RequestInfo.get().shouldExpand(ExpandUtil.versionsLatestOnlyExpandable), false,
				// LogicGraphVersion should not support nestedSemanticsExpandable
				false, Util.validateWorkflowProcess(processId));
	}

	/**
	 * @param id - A UUID or nid of a concept identifying the concept at the root of the logic graph
	 * @param stated - A boolean specifying whether to use the stated definition of the logic graph
	 * @return - A LogicGraphVersion SemanticChronology corresponding to the concept identified by the passed id
	 * @throws RestException
	 * 
	 *             Returns the either the stated or inferred logic graph semantic chronology corresponding to the passed id
	 * 
	 *             If the passed String id is an integer, it will be interpreted as the id of the referenced concept
	 * 
	 *             If the passed String id is a UUID, it will be interpreted as the id of either the LogicGraphVersion or the referenced concept
	 */
	private static SemanticChronology findLogicGraphChronology(String id, boolean stated, StampCoordinate stampCoordinate,
			LanguageCoordinate languageCoordinate, LogicCoordinate logicCoordinate) throws RestException
	{
		// id interpreted as the id of the referenced concept
		Optional<SemanticChronology> defChronologyOptional = Frills.getLogicGraphChronology(
				RequestInfoUtils.getConceptNidFromParameter(RequestParameters.id, id), stated, stampCoordinate, languageCoordinate, logicCoordinate);
		if (defChronologyOptional.isPresent())
		{
			return defChronologyOptional.get();
		}
		else
		{
			throw new RestException(RequestParameters.id, id, "No LogicGraph chronology is available for the concept with the specified id");
		}
	}
}