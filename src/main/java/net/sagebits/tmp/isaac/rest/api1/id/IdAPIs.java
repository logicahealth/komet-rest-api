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

package net.sagebits.tmp.isaac.rest.api1.id;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
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
import net.sagebits.tmp.isaac.rest.Util;
import net.sagebits.tmp.isaac.rest.api.exceptions.RestException;
import net.sagebits.tmp.isaac.rest.api1.RestPaths;
import net.sagebits.tmp.isaac.rest.api1.data.RestId;
import net.sagebits.tmp.isaac.rest.api1.data.concept.RestConceptChronology;
import net.sagebits.tmp.isaac.rest.api1.data.enumerations.IdType;
import net.sagebits.tmp.isaac.rest.api1.data.enumerations.RestSupportedIdType;
import net.sagebits.tmp.isaac.rest.session.RequestInfo;
import net.sagebits.tmp.isaac.rest.session.RequestParameters;
import net.sagebits.uts.auth.data.UserRole.SystemRoleConstants;
import sh.isaac.MetaData;
import sh.isaac.api.ConceptProxy;
import sh.isaac.api.Get;
import sh.isaac.api.bootstrap.TermAux;
import sh.isaac.api.chronicle.Chronology;
import sh.isaac.api.chronicle.LatestVersion;
import sh.isaac.api.component.concept.ConceptChronology;
import sh.isaac.api.component.semantic.SemanticChronology;
import sh.isaac.api.component.semantic.version.SemanticVersion;
import sh.isaac.api.coordinate.LanguageCoordinate;
import sh.isaac.api.coordinate.PremiseType;
import sh.isaac.api.util.NumericUtils;
import sh.isaac.api.util.UUIDUtil;
import sh.isaac.model.coordinate.LanguageCoordinateImpl;
import sh.isaac.utility.Frills;

/**
 * {@link IdAPIs}
 *
 * @author <a href="mailto:daniel.armbrust.list@sagebits.net">Dan Armbrust</a>
 */
@Path(RestPaths.idAPIsPathComponent)
@RolesAllowed({ SystemRoleConstants.AUTOMATED, SystemRoleConstants.ADMINISTRATOR, SystemRoleConstants.SYSTEM_MANAGER, SystemRoleConstants.CONTENT_MANAGER,
	SystemRoleConstants.EDITOR, SystemRoleConstants.READ })
public class IdAPIs
{
	private static Logger log = LogManager.getLogger();

	@Context
	private SecurityContext securityContext;

	/**
	 * Translate an ID from one type to another.
	 * 
	 * @param id The id to translate
	 * @param inputType - optional - must be one of the types from the supportedTypes call. You can pass the name or enumId of the
	 *            returned RestIdType object. This will be something like [uuid, nid, sctid, vuid] but may also be a nid (from the enumid)
	 *            If inputType is not specified, selects the type as follows. <br>
	 *            - UUIDs - if it is a correctly formatted UUID.<br>
	 *            - If negative - a nid. All other values are ambiguous, and the type must be input. An error will be thrown.
	 * @param outputType - optional - should be one of the types from the supportedTypes call. You can pass the name or enumId of the
	 *            returned RestIdType object. Currently includes [uuid, nid, sctid, vuid] but may also be a nid (from the enumid)
	 *            Defaults to uuid.
	 * @param coordToken specifies an explicit serialized CoordinatesToken string specifying all coordinate parameters. A CoordinatesToken may be
	 *            obtained by a separate (prior) call to getCoordinatesToken().
	 * 
	 * @return the converted ID, if possible. Otherwise, a RestException, if no translation is possible. Note that for some id types,
	 *         the translation may depend on the STAMP!
	 * @throws RestException
	 */
	@GET
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Path(RestPaths.idTranslateComponent + "{" + RequestParameters.id + "}")
	public RestId translateId(@PathParam(RequestParameters.id) String id, @QueryParam(RequestParameters.inputType) String inputType,
			@QueryParam(RequestParameters.outputType) @DefaultValue("uuid") String outputType, @QueryParam(RequestParameters.coordToken) String coordToken)
			throws RestException
	{
		RequestParameters.validateParameterNamesAgainstSupportedNames(RequestInfo.get().getParameters(), RequestParameters.id, RequestParameters.inputType,
				RequestParameters.outputType, RequestParameters.COORDINATE_PARAM_NAMES);

		Optional<RestSupportedIdType> inputTypeFormat = RestSupportedIdType.parse(inputType);
		
		Optional<? extends Chronology> object = Optional.empty();
		
		if (inputTypeFormat.isPresent())
		{
			if (inputTypeFormat.get().enumId == IdType.NID.getId())
			{
				if (NumericUtils.isNID(id))
				{
					object = Get.identifiedObjectService().getChronology(NumericUtils.getNID(id).getAsInt());
				}
				else
				{
					throw new RestException("id", id, "The input id was not a nid");
				}
			}
			else if (inputTypeFormat.get().enumId == IdType.UUID.getId())
			{
				Optional<UUID> uuid = UUIDUtil.getUUID(id);
				if (uuid.isPresent())
				{
					if (Get.identifierService().hasUuid(uuid.get()))
					{
						object = Get.identifiedObjectService().getChronology(Get.identifierService().getNidForUuids(uuid.get()));
					}
				}
				else
				{
					throw new RestException("id", id, "The input id was not a uuid");
				}
			}
			else
			{
				//It should be a nid
				List<Integer> nids = Frills.getNidForAltId(inputTypeFormat.get().enumId, id);
				if (nids.size() > 0)
				{
					object = Get.identifiedObjectService().getChronology(nids.get(0));
				}
			}
		}
		else
		{
			// If not specified, check for uuid or nid
			Optional<UUID> uuid = UUIDUtil.getUUID(id);
			if (uuid.isPresent())
			{
				if (Get.identifierService().hasUuid(uuid.get()))
				{
					object = Get.identifiedObjectService().getChronology(Get.identifierService().getNidForUuids(uuid.get()));
				}
			}
			else if (NumericUtils.isNID(id))
			{
				object = Get.identifiedObjectService().getChronology(NumericUtils.getNID(id).getAsInt());
			}
			if (!object.isPresent() && !StringUtils.isBlank(id))
			{
				throw new RestException("inputType", "must be provided in cases where the 'id' value is ambiguous");
			}
		}

		if (object.isPresent())
		{
			RestSupportedIdType outputTypeFormat = RestSupportedIdType.parse(outputType).orElse(new RestSupportedIdType(IdType.UUID));
			if (outputTypeFormat.enumId == IdType.NID.getId())
			{
				return new RestId(outputTypeFormat, object.get().getNid() + "");
			}
			else if (outputTypeFormat.enumId == IdType.UUID.getId())
			{
				return new RestId(outputTypeFormat, object.get().getPrimordialUuid().toString());
			}
			else
			{
				//must be a nid
				Optional<String> s = Frills.getAltId(outputTypeFormat.enumId, object.get().getNid(), RequestInfo.get().getStampCoordinate());
				if (s.isPresent())
				{
					return new RestId(outputTypeFormat, s.get());
				}
				else
				{
					throw new RestException("No id of type " + outputTypeFormat.enumName + " was found on the specified component '" 
							+ object.get().getPrimordialUuid() + "'");
				}
			}
		}
		else
		{
			throw new RestException(RequestParameters.id, id, "Unable to locate an object with the given id");
		}
	}

	/**
	 * Enumerate the valid types for the system. These values can be cached for the life of the connection, though 
	 * if you create a new semantic id type, you would need to call this again to get the new list.
	 * 
	 * This returns 'special' types, like UUID and NID, and all semantic based types which would be returned via the
	 * /ids call.
	 * 
	 * @return the supported types
	 * @throws RestException 
	 */
	@GET
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Path(RestPaths.idTypesComponent)
	public RestSupportedIdType[] getSupportedTypes() throws RestException
	{
		RequestParameters.validateParameterNamesAgainstSupportedNames(RequestInfo.get().getParameters(), RequestParameters.COORDINATE_PARAM_NAMES);

		return RestSupportedIdType.getAll();
	}

	/**
	 * Enumerate the supported ID static string semantic concepts for the system.
	 * Each of these ID concepts is annotated with a membership semantic associated with the "identifier source" assemblage concept.
	 * In addition to {@code expand} parameter(s), accepts coordinate token and/or parameter(s).
	 * 
	 * NOTE: For the convenient use of ids as labels, the descriptionTypePrefs is unconditionally overridden to the value of
	 * "REGULAR,FQN"
	 * 
	 * This returns a subset of idTypes contained in the /types call - only the ones that are stored via a semantic.
	 * 
	 * @return RestConceptChronology[] - Array of {@link RestConceptChronology} representing identifier static string semantic concepts
	 * @throws RestException
	 */
	@GET
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Path(RestPaths.idsComponent)
	public RestConceptChronology[] getSupportedIdConcepts() throws RestException
	{
		RequestParameters.validateParameterNamesAgainstSupportedNames(RequestInfo.get().getParameters(), RequestParameters.COORDINATE_PARAM_NAMES);
		
		Set<ConceptChronology> identifierAnnotatedConcepts = new HashSet<>();

		Stream<SemanticChronology> identifierAnnotationSemanticChronologyStream = Get.assemblageService()
				.getSemanticChronologyStream(MetaData.IDENTIFIER_SOURCE____SOLOR.getNid());
		identifierAnnotationSemanticChronologyStream.sequential().forEach(identifierAnnotationSemanticChronology -> {
			LatestVersion<SemanticVersion> identifierAnnotationSemanticLatestOptional = identifierAnnotationSemanticChronology
					.getLatestVersion(RequestInfo.get().getStampCoordinate());
			if (identifierAnnotationSemanticLatestOptional.isPresent())
			{
				Util.logContradictions(log, identifierAnnotationSemanticLatestOptional);
				SemanticVersion identifierAnnotationSemantic = identifierAnnotationSemanticLatestOptional.get();
				identifierAnnotatedConcepts.add(Get.conceptService().getConceptChronology(identifierAnnotationSemantic.getReferencedComponentNid()));
			}
		});

		RestConceptChronology[] arrayToReturn = new RestConceptChronology[identifierAnnotatedConcepts.size()];

		// Create an analog of the LanguageCoordinate differing only by descriptionTypePrefs value
		final LanguageCoordinate languageCoordinateToUse = new LanguageCoordinateImpl(RequestInfo.get().getLanguageCoordinate().getLanguageConceptNid(),
				RequestInfo.get().getLanguageCoordinate().getDialectAssemblagePreferenceList(),
				new ConceptProxy[] { TermAux.REGULAR_NAME_DESCRIPTION_TYPE, TermAux.FULLY_QUALIFIED_NAME_DESCRIPTION_TYPE });

		int i = 0;
		for (ConceptChronology idConcept : identifierAnnotatedConcepts)
		{
			arrayToReturn[i++] = new RestConceptChronology(idConcept, false, true, false, languageCoordinateToUse, 
					RequestInfo.get().getManifoldCoordinate().getTaxonomyPremiseType() == PremiseType.STATED);
		}

		// Sort results by description for display
		Arrays.sort(arrayToReturn, REST_CONCEPT_DESCRIPTION_COMPARATOR);

		return arrayToReturn;
	}
	private final static Comparator<RestConceptChronology> REST_CONCEPT_DESCRIPTION_COMPARATOR = new Comparator<RestConceptChronology>()
	{
		public int compare(RestConceptChronology concept1, RestConceptChronology concept2)
		{

			String concept1Description = concept1.getDescription().toUpperCase(Locale.ENGLISH);
			String concept2Description = concept2.getDescription().toUpperCase(Locale.ENGLISH);

			// ascending order
			return concept1Description.compareTo(concept2Description);

			// descending order
			// return concept2Description.compareTo(concept1Description);
		}
	};
}
