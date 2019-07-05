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
import net.sagebits.uts.auth.data.UserRole.SystemRoleConstants;
import sh.isaac.MetaData;
import sh.isaac.api.chronicle.LatestVersion;
import sh.isaac.api.component.semantic.SemanticChronology;
import sh.isaac.api.component.semantic.version.LogicGraphVersion;
import sh.isaac.api.coordinate.LanguageCoordinate;
import sh.isaac.api.coordinate.LogicCoordinate;
import sh.isaac.api.coordinate.StampCoordinate;
import sh.isaac.utility.Frills;

/**
 * {@link LogicGraphAPIs}
 *
 * @author <a href="mailto:joel.kniaz.list@gmail.com">Joel Kniaz</a>
 */

@Path(RestPaths.logicGraphAPIsPathComponent)
@RolesAllowed({ SystemRoleConstants.AUTOMATED, SystemRoleConstants.ADMINISTRATOR, SystemRoleConstants.SYSTEM_MANAGER, SystemRoleConstants.CONTENT_MANAGER,
	SystemRoleConstants.EDITOR, SystemRoleConstants.READ })
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
	 *     <br> 'chronology' - to include the chronology of the semantic that carries the logic graph, 
	 *     <br> 'version'  - to include RestConceptVersion objects inside of RestConceptNode and RestTypedConnectorNode types.
	 *     <br> 'countParents' - may only be specified in combination with 'version' - will cause the expanded version to also have the parent count populated.
	 *     <br> 'includeParents' - may only be specified in combination with 'version' - will cause the expanded version to also have the first-level parent list 
	 *          populated.
	 *     <br> 'terminologyType' - may only be specified in combination with 'version'.   when specified, the concept nids of the terminologies that this concept 
	 *          is part of on any stamp is returned. This is determined by whether or not there is version of this concept present with a module that extends from 
	 *          one of the children of the {@link MetaData#MODULE____SOLOR} concepts. This is returned as a set, as a concept may exist in multiple terminologies 
	 *          at the same time.
	 * @param coordToken specifies an explicit serialized CoordinatesToken string specifying all coordinate parameters. A CoordinatesToken may be
	 *            obtained by a separate (prior) call to getCoordinatesToken().
	 * @param altId - (optional) the altId type(s) to populate in any returned RestIdentifiedObject structures.  By default, no alternate IDs are 
	 *     returned.  This can be set to one or more names or ids from the /1/id/types or the value 'ANY'.  Requesting IDs that are unneeded will harm 
	 *     performance. 
	 * @return the logic graph version object
	 * @throws RestException
	 */
	@GET
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Path(RestPaths.versionComponent + "{" + RequestParameters.id + "}")
	public RestSemanticLogicGraphVersion getLogicGraphVersion(@PathParam(RequestParameters.id) String id, @QueryParam(RequestParameters.expand) String expand,
			@QueryParam(RequestParameters.coordToken) String coordToken,
			@QueryParam(RequestParameters.altId) String altId) throws RestException
	{
		RequestParameters.validateParameterNamesAgainstSupportedNames(RequestInfo.get().getParameters(), RequestParameters.id, RequestParameters.expand,
				RequestParameters.COORDINATE_PARAM_NAMES, RequestParameters.altId);
		
		RequestInfo.get().validateMethodExpansions(ExpandUtil.chronologyExpandable, ExpandUtil.versionExpandable, ExpandUtil.countParents, ExpandUtil.includeParents, 
				ExpandUtil.terminologyType);

		SemanticChronology logicGraphSemanticChronology = findLogicGraphChronology(id, RequestInfo.get().getStated(), RequestInfo.get().getStampCoordinate(),
				RequestInfo.get().getLanguageCoordinate(), RequestInfo.get().getLogicCoordinate());

		LatestVersion<LogicGraphVersion> lgs = logicGraphSemanticChronology.getLatestVersion(RequestInfo.get().getStampCoordinate());
		if (lgs.isPresent())
		{
			// TODO handle contradictions
			Util.logContradictions(log, lgs);
			return new RestSemanticLogicGraphVersion(lgs.get(), RequestInfo.get().shouldExpand(ExpandUtil.chronologyExpandable), false, true);
		}
		throw new RestException(RequestParameters.id, id, "No concept was found");
	}

	/**
	 * Returns the chronology of a logic graph.
	 * 
	 * @param id - A UUID or nid, of a concept identifying the concept at the root of the logic graph
	 * @param expand - comma separated list of fields to expand. Supports 
	 *     <br> 'versionsAll' - when supplied, all versions of the logic graph will be returned attached to the chronology.  Note that, this only includes 
	 *         all versions for the top level logic graph chronology.  If 'version' is also specified, the version of the referenced component returned for 
	 *         each version of the logic graph will be the most appropriate version for the top level logic graph.  In other words, the STAMP of the logic graph 
	 *         version being returned is used to calculate the appropriate stamp for the referenced component versions, when they are looked up.
	 *         Sorted newest to oldest.
	 *     <br> 'versionsLatestOnly' - ignored if specified in combination with versionsAll
	 *     <br> 'version'  - to include RestConceptVersion objects for referenced concepts, such as RestConceptNode and RestTypedConnectorNode types in the graph.
	 *       Only applicable when versionsAll or versionsLatestOnly is also specified.
	 *     <br> 'countParents' - may only be specified in combination with 'version' - will cause the expanded version to also have the parent count populated.
	 *         Only applicable when versionsAll or versionsLatestOnly is also specified.
	 *     <br> 'includeParents' - may only be specified in combination with 'version' - will cause the expanded version to also have the first-level parent list 
	 *          populated.  Only applicable when versionsAll or versionsLatestOnly is also specified.
	 *     <br> 'terminologyType' - may only be specified in combination with 'version'.   when specified, the concept nids of the terminologies that this concept 
	 *          is part of on any stamp is returned. This is determined by whether or not there is version of this concept present with a module that extends from 
	 *          one of the children of the {@link MetaData#MODULE____SOLOR} concepts. This is returned as a set, as a concept may exist in multiple terminologies 
	 *          at the same time.  Only applicable when versionsAll or versionsLatestOnly is also specified.
	 * @param coordToken specifies an explicit serialized CoordinatesToken string specifying all coordinate parameters. A CoordinatesToken may be
	 *            obtained by a separate (prior) call to getCoordinatesToken().
	 * @param altId - (optional) the altId type(s) to populate in any returned RestIdentifiedObject structures.  By default, no alternate IDs are 
	 *     returned.  This can be set to one or more names or ids from the /1/id/types or the value 'ANY'.  Requesting IDs that are unneeded will harm 
	 *     performance. 
	 * 
	 * @return the concept chronology object
	 * @throws RestException
	 */
	@GET
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Path(RestPaths.chronologyComponent + "{" + RequestParameters.id + "}")
	public RestSemanticChronology getLogicGraphChronology(@PathParam(RequestParameters.id) String id, @QueryParam(RequestParameters.expand) String expand,
			@QueryParam(RequestParameters.coordToken) String coordToken, @QueryParam(RequestParameters.altId) String altId) throws RestException
	{
		RequestParameters.validateParameterNamesAgainstSupportedNames(RequestInfo.get().getParameters(), RequestParameters.id, RequestParameters.expand,
				RequestParameters.COORDINATE_PARAM_NAMES, RequestParameters.altId);
		
		RequestInfo.get().validateMethodExpansions(ExpandUtil.versionsAllExpandable, ExpandUtil.versionsLatestOnlyExpandable, ExpandUtil.versionExpandable, 
				ExpandUtil.countParents, ExpandUtil.includeParents, ExpandUtil.terminologyType);

		SemanticChronology logicGraphSemanticChronology = findLogicGraphChronology(id, RequestInfo.get().getStated(), RequestInfo.get().getStampCoordinate(),
				RequestInfo.get().getLanguageCoordinate(), RequestInfo.get().getLogicCoordinate());

		return new RestSemanticChronology(logicGraphSemanticChronology, RequestInfo.get().shouldExpand(ExpandUtil.versionsAllExpandable),
				RequestInfo.get().shouldExpand(ExpandUtil.versionsLatestOnlyExpandable), false,
				// LogicGraphVersion should not support nestedSemanticsExpandable
				false);
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