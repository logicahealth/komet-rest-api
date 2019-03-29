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

package net.sagebits.tmp.isaac.rest.api1.coordinate;

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
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.sagebits.tmp.isaac.rest.api.exceptions.RestException;
import net.sagebits.tmp.isaac.rest.api1.RestPaths;
import net.sagebits.tmp.isaac.rest.api1.concept.ConceptAPIs;
import net.sagebits.tmp.isaac.rest.api1.data.RestCoordinatesToken;
import net.sagebits.tmp.isaac.rest.api1.data.RestEditToken;
import net.sagebits.tmp.isaac.rest.api1.data.RestIdentifiedObject;
import net.sagebits.tmp.isaac.rest.api1.data.coordinate.RestCoordinates;
import net.sagebits.tmp.isaac.rest.api1.data.coordinate.RestLanguageCoordinate;
import net.sagebits.tmp.isaac.rest.api1.data.coordinate.RestLogicCoordinate;
import net.sagebits.tmp.isaac.rest.api1.data.coordinate.RestManifoldCoordinate;
import net.sagebits.tmp.isaac.rest.api1.data.coordinate.RestStampCoordinate;
import net.sagebits.tmp.isaac.rest.session.RequestInfo;
import net.sagebits.tmp.isaac.rest.session.RequestInfoUtils;
import net.sagebits.tmp.isaac.rest.session.RequestParameters;
import net.sagebits.tmp.isaac.rest.tokens.EditToken;
import net.sagebits.uts.auth.data.UserRole.SystemRoleConstants;
import net.sagebits.uts.auth.rest.api1.data.RestUser;
import net.sagebits.uts.auth.rest.session.AuthRequestParameters;
import sh.isaac.MetaData;
import sh.isaac.api.Get;
import sh.isaac.api.coordinate.EditCoordinate;
import sh.isaac.utility.Frills;

/**
 * {@link CoordinateAPIs}
 * 
 * @author <a href="mailto:daniel.armbrust.list@sagebits.net">Dan Armbrust</a>
 */
@Path(RestPaths.coordinateAPIsPathComponent)
@RolesAllowed({ SystemRoleConstants.AUTOMATED, SystemRoleConstants.ADMINISTRATOR, SystemRoleConstants.SYSTEM_MANAGER, SystemRoleConstants.CONTENT_MANAGER,
	SystemRoleConstants.EDITOR, SystemRoleConstants.READ })
public class CoordinateAPIs
{
	private static Logger log = LogManager.getLogger(CoordinateAPIs.class);

	@Context
	private SecurityContext securityContext;

	/**
	 * 
	 * This method returns a serialized CoordinatesToken string specifying all coordinate parameters
	 * It takes an explicit serialized CoordinatesToken string parameter <code>coordToken</code>
	 * specifying all coordinate parameters in addition to all of the other coordinate-specific parameters.
	 * If no additional individual coordinate-specific parameters are specified,
	 * then the passed <code>coordToken</code> CoordinatesToken will be returned.
	 * If any additional individual parameters are passed, then their values will be applied to the coordinates specified by the
	 * explicit serialized CoordinatesToken string, and the resulting modified CoordinatesToken will be returned.
	 * 
	 * @param coordToken specifies an explicit serialized CoordinatesToken string specifying all coordinate parameters. A CoordinatesToken may be
	 *            obtained by a separate (prior) call to getCoordinatesToken().
	 * @param stated specifies premise/taxonomy type of <code>STATED</code> when true and <code>INFERRED</code> when false.
	 * @param descriptionTypePrefs specifies the order preference of description types for the LanguageCoordinate. Values are description type UUIDs,
	 *            nids or the terms "fqn", "regular" and/or "definition". The default is "fqn,regular".  The system also supports deprecated
	 *            name constants - "fsn" is the same as "fqn" and "synonym" is the same as "regular".
	 * @param dialectPrefs specifies the order preference of dialects for the LanguageCoordinate. Values are description type UUIDs, int ids or the
	 *            terms "us" or "gb".
	 *            The default is "us,gb".
	 *            In general, dialect concepts are children of the concept "Dialect assemblage (SOLOR)" - f5d5c959-a9b2-57aa-b88b-3676e7147265.  Note, 
	 *            however, this is a hierarchy of less specific to more specific, for example, English Dialect -> US English Dialect -> US Nursing Dialect.
	 * 
	 * When 'recurse' is specified, the specified dialects will be used first, followed by their children.  If you specify the dialects of
	 * 'us,gb,recurse' you will get the resulting dialect list:
	 * 
	 * [US English Dialect, GB English dialect, US Nursing Dialect].  When it looks for designations to return, it evaluates this list in order.
	 * 
	 * @param language specifies language of the LanguageCoordinate. Value may be a language UUID, int id or one of the following terms: "english",
	 *            "spanish", "french", "danish", "polish", "dutch", "lithuanian", "chinese", "japanese", or "swedish". The default is "english".
	 *            If a concept is specified, it must be a direct child of the concept 'Language (SOLOR)' - f56fa231-10f9-5e7f-a86d-a1d61b5b56e3.
	 * @param modules specifies modules of the StampCoordinate. Value may be a comma delimited list of module concept UUID or int ids.
	 * @param path specifies path component of StampPosition component of the StampCoordinate. Values is path UUID, int id or the term "development"
	 *            or "master". The default is "development".
	 * @param precedence specifies precedence of the StampCoordinate. Values are either "path" or "time". The default is "path".
	 *            </p>
	 * @param allowedStates specifies allowed states of the StampCoordinate. Value may be a comma delimited list of Status enum names. The default is
	 *            "active".
	 * @param time specifies time component of StampPosition component of the StampCoordinate. Values are Long time values or "latest" (case ignored).
	 *            The default is "latest".
	 * @param logicStatedAssemblage specifies stated assemblage of the LogicCoordinate. Value may be a concept UUID string or int id.
	 * @param logicInferredAssemblage specifies inferred assemblage of the LogicCoordinate. Value may be a concept UUID string or int id.
	 * @param descriptionLogicProfile specifies description profile assemblage of the LogicCoordinate. Value may be a concept UUID string or int id.
	 * @param classifier specifies classifier assemblage of the LogicCoordinate. Value may be a concept UUID string or int id.
	 * 
	 * @return RestCoordinatesToken
	 * @throws RestException
	 */
	@GET
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Path(RestPaths.coordinatesTokenComponent)
	public RestCoordinatesToken getCoordinatesToken(@QueryParam(RequestParameters.coordToken) String coordToken,

			@QueryParam(RequestParameters.stated) String stated,

			@QueryParam(RequestParameters.descriptionTypePrefs) String descriptionTypePrefs, @QueryParam(RequestParameters.dialectPrefs) String dialectPrefs,
			@QueryParam(RequestParameters.language) String language,

			@QueryParam(RequestParameters.modules) String modules, @QueryParam(RequestParameters.path) String path,
			@QueryParam(RequestParameters.precedence) String precedence, @QueryParam(RequestParameters.allowedStates) String allowedStates,
			@QueryParam(RequestParameters.time) String time,

			@QueryParam(RequestParameters.logicStatedAssemblage) String logicStatedAssemblage,
			@QueryParam(RequestParameters.logicInferredAssemblage) String logicInferredAssemblage,
			@QueryParam(RequestParameters.descriptionLogicProfile) String descriptionLogicProfile, @QueryParam(RequestParameters.classifier) String classifier)
			throws RestException
	{
		RequestParameters.validateParameterNamesAgainstSupportedNames(RequestInfo.get().getParameters(), RequestParameters.COORDINATE_PARAM_NAMES);

		// All parameters, including defaults, are handled by the RestContainerRequestFilter

		log.debug("Returning RestCoordinatesToken...");
		return new RestCoordinatesToken(RequestInfo.get().getCoordinatesToken());
	}

	/**
	 * 
	 * This method returns an object comprising all coordinate parameters.
	 * It takes an explicit serialized CoordinatesToken string parameter <code>coordToken</code>
	 * specifying all coordinate parameters in addition to all of the other coordinate-specific parameters.
	 * If no additional individual coordinate-specific parameters are specified,
	 * then the coordinates corresponding to the passed <code>coordToken</code> CoordinatesToken will be returned.
	 * If any additional individual parameters are passed, then their values will be applied to the coordinates specified by the
	 * explicit serialized CoordinatesToken string, and the resulting coordinates will be returned.
	 * 
	 * @param coordToken specifies an explicit serialized CoordinatesToken string specifying all coordinate parameters. A CoordinatesToken may be
	 *            obtained by a separate (prior) call to getCoordinatesToken().
	 * 
	 * @return RestCoordinates Object containing all coordinates.
	 *         Note that <code>RestManifoldCoordinate</code> contains <code>RestStampCoordinate</code>, <code>RestLanguageCoordinate</code> and
	 *         <code>RestLogicCoordinate</code>.
	 * @throws RestException
	 */
	@GET
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Path(RestPaths.coordinatesComponent)
	public RestCoordinates getCoordinates(@QueryParam(RequestParameters.coordToken) String coordToken) throws RestException
	{
		RequestParameters.validateParameterNamesAgainstSupportedNames(RequestInfo.get().getParameters(), RequestParameters.COORDINATE_PARAM_NAMES);

		RestManifoldCoordinate taxonomyCoordinate = getTaxonomyCoordinate(coordToken);
		RestCoordinates coordinates = new RestCoordinates(taxonomyCoordinate, taxonomyCoordinate.stampCoordinate, taxonomyCoordinate.languageCoordinate,
				taxonomyCoordinate.logicCoordinate);

		log.debug("Returning REST Coordinates...");

		return coordinates;
	}

	/**
	 * 
	 * This method returns <code>RestManifoldCoordinate</code>.
	 * It takes an explicit serialized CoordinatesToken string parameter <code>coordToken</code>
	 * specifying all coordinate parameters in addition to all of the other coordinate-specific parameters.
	 * If no additional individual coordinate-specific parameters are specified,
	 * then the coordinate corresponding to the passed <code>coordToken</code> CoordinatesToken will be returned.
	 * If any additional individual parameters are passed, then their values will be applied to the coordinate specified by the
	 * explicit serialized CoordinatesToken string, and the resulting coordinate will be returned.
	 * 
	 * @param coordToken specifies an explicit serialized CoordinatesToken string specifying all coordinate parameters. A CoordinatesToken may be
	 *            obtained by a separate (prior) call to getCoordinatesToken().
	 * 
	 * @return RestManifoldCoordinate
	 *         Note that <code>RestManifoldCoordinate</code> contains <code>RestStampCoordinate</code>, <code>RestLanguageCoordinate</code> and
	 *         <code>RestLogicCoordinate</code>.
	 * 
	 * @throws RestException
	 */
	@GET
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Path(RestPaths.taxonomyCoordinatePathComponent)
	public RestManifoldCoordinate getTaxonomyCoordinate(@QueryParam(RequestParameters.coordToken) String coordToken) throws RestException
	{
		RequestParameters.validateParameterNamesAgainstSupportedNames(RequestInfo.get().getParameters(), RequestParameters.COORDINATE_PARAM_NAMES);

		return new RestManifoldCoordinate(RequestInfo.get().getManifoldCoordinate());
	}

	/**
	 * 
	 * This method returns <code>RestStampCoordinate</code>.
	 * It takes an explicit serialized CoordinatesToken string parameter <code>coordToken</code>
	 * specifying all coordinate parameters in addition to all of the other coordinate-specific parameters.
	 * If no additional individual coordinate-specific parameters are specified,
	 * then the coordinate corresponding to the passed <code>coordToken</code> CoordinatesToken will be returned.
	 * If any additional individual parameters are passed, then their values will be applied to the coordinate specified by the
	 * explicit serialized CoordinatesToken string, and the resulting coordinate will be returned.
	 * 
	 * @param coordToken specifies an explicit serialized CoordinatesToken string specifying all coordinate parameters. A CoordinatesToken may be
	 *            obtained by a separate (prior) call to getCoordinatesToken().
	 * 
	 * @return RestStampCoordinate
	 * 
	 * @throws RestException
	 */
	@GET
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Path(RestPaths.stampCoordinatePathComponent)
	public RestStampCoordinate getStampCoordinate(@QueryParam(RequestParameters.coordToken) String coordToken) throws RestException
	{
		RequestParameters.validateParameterNamesAgainstSupportedNames(RequestInfo.get().getParameters(), RequestParameters.COORDINATE_PARAM_NAMES);

		return new RestStampCoordinate(RequestInfo.get().getStampCoordinate());
	}

	/**
	 * 
	 * This method returns <code>RestLanguageCoordinate</code>.
	 * It takes an explicit serialized CoordinatesToken string parameter <code>coordToken</code>
	 * specifying all coordinate parameters in addition to all of the other coordinate-specific parameters.
	 * If no additional individual coordinate-specific parameters are specified,
	 * then the coordinate corresponding to the passed <code>coordToken</code> CoordinatesToken will be returned.
	 * If any additional individual parameters are passed, then their values will be applied to the coordinate specified by the
	 * explicit serialized CoordinatesToken string, and the resulting coordinate will be returned.
	 * 
	 * @param coordToken specifies an explicit serialized CoordinatesToken string specifying all coordinate parameters. A CoordinatesToken may be
	 *            obtained by a separate (prior) call to getCoordinatesToken().
	 * 
	 * @return RestLanguageCoordinate
	 * 
	 * @throws RestException
	 */
	@GET
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Path(RestPaths.languageCoordinatePathComponent)
	public RestLanguageCoordinate getLanguageCoordinate(@QueryParam(RequestParameters.coordToken) String coordToken) throws RestException
	{
		RequestParameters.validateParameterNamesAgainstSupportedNames(RequestInfo.get().getParameters(), RequestParameters.COORDINATE_PARAM_NAMES);

		return new RestLanguageCoordinate(RequestInfo.get().getLanguageCoordinate());
	}

	/**
	 * 
	 * This method returns <code>RestLogicCoordinate</code>.
	 * It takes an explicit serialized CoordinatesToken string parameter <code>coordToken</code>
	 * specifying all coordinate parameters in addition to all of the other coordinate-specific parameters.
	 * If no additional individual coordinate-specific parameters are specified,
	 * then the coordinate corresponding to the passed <code>coordToken</code> CoordinatesToken will be returned.
	 * If any additional individual parameters are passed, then their values will be applied to the coordinate specified by the
	 * explicit serialized CoordinatesToken string, and the resulting coordinate will be returned.
	 * 
	 * @param coordToken specifies an explicit serialized CoordinatesToken string specifying all coordinate parameters. A CoordinatesToken may be
	 *            obtained by a separate (prior) call to getCoordinatesToken().
	 * 
	 * @return RestLogicCoordinate
	 * 
	 * @throws RestException
	 */
	@GET
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Path(RestPaths.logicCoordinatePathComponent)
	public RestLogicCoordinate getLogicCoordinate(@QueryParam(RequestParameters.coordToken) String coordToken) throws RestException
	{
		RequestParameters.validateParameterNamesAgainstSupportedNames(RequestInfo.get().getParameters(), RequestParameters.COORDINATE_PARAM_NAMES);

		return new RestLogicCoordinate(RequestInfo.get().getLogicCoordinate());
	}

	/**
	 * 
	 * This method returns a <code>RestEditToken</code>, which is an encrypted String that is used internally
	 * to authenticate and convey user and session information between KOMET and PRISME. Information conveyed includes
	 * user, module, and path concepts as well as, optionally, a workflow process. Each EditToken expires after a set amount of time
	 * and is otherwise usable.
	 * 
	 * Each write token can only be used for one write operation - after that, is is unusable for further write operations (but may be used 
	 * to get a fresh edit token)
	 * 
	 * An expired token may be renewed by passing it as an editToken parameter to another getEditToken() call.
	 * 
	 * All write operations return a <code>RestWriteResponse</code> object containing a renewed, and therefore readily usable EditToken.
	 * 
	 * If an editToken is passed into this method in combination with other edit token parameters, the editToken parameters take priority 
	 * in the newly returned token.
	 * 
	 * This method requires at least EDITOR permissions
	 * 
	 * @param ssoToken - optional - a previously issued token that represents an authenticated user.
	 * @param userName - optional - a user name to authenticate against local auth - must be used with password
	 * @param email  - optional - an e-mail address (instead of a username) to authenticate against local auth - must be used with password
	 * @param password  - optional - the password to authenticate against local in combination with  username or email.
	 * @param editToken - optional previously-retrieved editToken string encoding user, module, path concept ids and optional workflow process id. 
	 *     If sent in combination with an ssoToken, or other user identifying information, the user details must align.
	 * @param editModule - optional module concept id
	 * @param editPath - optional path concept id
	 * @param processId 
	 *
	 * @return RestEditToken
	 * @throws RestException
	 */
	@GET
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Path(RestPaths.editTokenComponent)
	@RolesAllowed({ SystemRoleConstants.AUTOMATED, SystemRoleConstants.ADMINISTRATOR, SystemRoleConstants.SYSTEM_MANAGER, SystemRoleConstants.CONTENT_MANAGER,
		SystemRoleConstants.EDITOR})
	public RestEditToken getEditToken(@QueryParam(AuthRequestParameters.ssoToken) String ssoToken, // Applied in RestContainerRequestFilter
			@QueryParam(AuthRequestParameters.email) String email, // Applied in RestContainerRequestFilter
			@QueryParam(AuthRequestParameters.userName) String userName, // Applied in RestContainerRequestFilter
			@QueryParam(AuthRequestParameters.password) String password, // Applied in RestContainerRequestFilter
			@QueryParam(RequestParameters.editToken) String editToken, // Applied in RestContainerRequestFilter
			@QueryParam(RequestParameters.editModule) String editModule, // Applied in RestContainerRequestFilter
			@QueryParam(RequestParameters.editPath) String editPath, // Applied in RestContainerRequestFilter
			@QueryParam(RequestParameters.processId) String processId // Applied in RestContainerRequestFilter
	) throws RestException
	{
		RequestParameters.validateParameterNamesAgainstSupportedNames(RequestInfo.get().getParameters(), 
				AuthRequestParameters.ssoToken,
				AuthRequestParameters.email,
				AuthRequestParameters.userName,
				AuthRequestParameters.password, 
				RequestParameters.EDIT_TOKEN_PARAM_NAMES);
		
		//For an encoded editToken, or SSO / username credentials, all work is done in the RestContainerRequestFilter.
		//If we get this far, the user information has been passed and validated by the filter.
		
		//They didn't pass an editToken, need to construct the entire thing.
		if (StringUtils.isBlank(editToken))
		{
			Integer module = null;
			Integer path = null;
			UUID workflowProcessId = null;
			
			EditCoordinate defaultEditCoordinate = RequestInfo.getDefaultEditCoordinate();
			
			if (StringUtils.isNotBlank(processId))
			{
				workflowProcessId = RequestInfoUtils.parseUuidParameter(RequestParameters.processId, processId);
			}
			if (StringUtils.isNotBlank(editModule))
			{
				module = RequestInfoUtils.getConceptNidFromParameter(RequestParameters.editModule, editModule);
			}
			if (StringUtils.isNotBlank(editPath))
			{
				path = RequestInfoUtils.getConceptNidFromParameter(RequestParameters.editPath, editPath);
			}

			Optional<RestUser> user = RequestInfo.get().getUser();
			// Must have a real user
			if (!user.isPresent())
			{
				throw new RestException("Edit token cannot be constructed without user information!");
			}
			
			return new RestEditToken(new EditToken(Get.identifierService().getNidForUuids(user.get().userId),
					module != null ? module : defaultEditCoordinate.getModuleNid(), path != null ? path : defaultEditCoordinate.getPathNid(),
					workflowProcessId));
		}
		else
		{
			// All work is done in RequestInfo.get().getEditToken(), initially invoked by RestContainerRequestFilter
			return new RestEditToken(RequestInfo.get().getEditToken().renewToken());
		}
	}
	

	/**
	 * Get the module identifier that should be used (by default) for edits on a particular terminology.  
	 * You can get the allowed input parameters from 1/system/terminologyTypes 
	 * 
	 * @param id - a nid or UUID of a concept that represents a terminology in the system. This should be a direct child of
	 *            {@link MetaData#MODULE____SOLOR}
	 * @return The id of the concept that represents the default 'edit' module for this content.  This is the module where most
	 *            in-progress work is put, until the work goes through a release process. 
	 * @throws RestException
	 */
	@GET
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Path(RestPaths.editModule + "{" + RequestParameters.id + "}")
	public RestIdentifiedObject getEditModule(@PathParam(RequestParameters.id) String id) throws RestException
	{
		RequestParameters.validateParameterNamesAgainstSupportedNames(RequestInfo.get().getParameters(), RequestParameters.id, 
				RequestParameters.COORDINATE_PARAM_NAMES);
		
		//This may actually create a concept, but internally, the concept is recorded as a system owned concept, not one created by the user.
		return new RestIdentifiedObject(Frills.createAndGetDefaultEditModule(ConceptAPIs.findConceptChronology(id).getNid()));
	}
}
