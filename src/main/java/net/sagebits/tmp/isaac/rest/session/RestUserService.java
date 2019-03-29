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

package net.sagebits.tmp.isaac.rest.session;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jvnet.hk2.annotations.Contract;
import net.sagebits.tmp.isaac.rest.Util;
import net.sagebits.tmp.isaac.rest.api.exceptions.RestException;
import net.sagebits.tmp.isaac.rest.tokens.EditToken;
import net.sagebits.uts.auth.rest.api1.data.RestUser;
import sh.isaac.MetaData;
import sh.isaac.api.Get;
import sh.isaac.api.LookupService;
import sh.isaac.api.chronicle.Chronology;
import sh.isaac.api.commit.ChangeCheckerMode;
import sh.isaac.api.commit.CommitRecord;
import sh.isaac.api.component.concept.ConceptBuilder;
import sh.isaac.api.component.concept.ConceptBuilderService;
import sh.isaac.api.component.concept.ConceptChronology;
import sh.isaac.api.component.concept.ConceptSpecification;
import sh.isaac.api.coordinate.EditCoordinate;
import sh.isaac.api.coordinate.LanguageCoordinate;
import sh.isaac.api.coordinate.LogicCoordinate;
import sh.isaac.model.configuration.EditCoordinates;
import sh.isaac.model.configuration.LanguageCoordinates;
import sh.isaac.model.configuration.LogicCoordinates;
import sh.isaac.model.logic.LogicalExpressionImpl;
import sh.isaac.model.logic.node.NecessarySetNode;
import sh.isaac.model.logic.node.external.ConceptNodeWithUuids;

/**
 * {@link RestUserService}
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
@Contract
public interface RestUserService
{
	static Logger log = LogManager.getLogger(RestUserService.class);
	
	/**
	 * Determine the user information, if any, provided via requestParameters.
	 * 
	 * This may be resolved against a remote auth-api implementation, or against a local datastore, depending on the operating mode.
	 * 
	 * @param requestParameters - all parameters passed in with the request, this service will filter for the ones it cares about.
	 * @param editToken - optional - if provided, user will be loaded from the edit token, or sanity checked against it.
	 * @return the user and roles available to the user
	 * @throws RestException  if the provided parameters are invalid
	 */
	public Optional<RestUser> getUser(Map<String, List<String>> requestParameters, EditToken editToken) throws RestException;
	
	/**
	 * Get the concept nid that corresponds to a given editors user name - constructing the concept if necessary.
	 * 
	 * @param user
	 * @return the nid of the author
	 */
	public default int getAuthorNid(RestUser user)
	{
		// If the SSO UUID already persisted
		if (Get.identifierService().hasUuid(user.userId))
		{
			// Set authorNid to value corresponding to SSO UUID
			return Get.identifierService().getNidForUuids(user.userId);
		}
		else
		{
			log.debug("Creating new concept for user '" + user.userId + "'");
			EditCoordinate adminEditCoordinate = EditCoordinates.getDefaultUserMetadata();
			LanguageCoordinate languageCoordinate = LanguageCoordinates.getUsEnglishLanguageFullySpecifiedNameCoordinate();
			LogicCoordinate logicCoordinate = LogicCoordinates.getStandardElProfile();

			ConceptSpecification defaultDescriptionsLanguageConceptSpec = Get.conceptSpecification(languageCoordinate.getLanguageConceptNid());
			ConceptSpecification defaultDescriptionDialectConceptSpec = Get.conceptSpecification(languageCoordinate.getDialectAssemblagePreferenceList()[0]);

			ConceptBuilderService conceptBuilderService = LookupService.getService(ConceptBuilderService.class);
			conceptBuilderService.setDefaultLanguageForDescriptions(defaultDescriptionsLanguageConceptSpec);
			conceptBuilderService.setDefaultDialectAssemblageForDescriptions(defaultDescriptionDialectConceptSpec);
			conceptBuilderService.setDefaultLogicCoordinate(logicCoordinate);

			LogicalExpressionImpl parentDef = new LogicalExpressionImpl();
			NecessarySetNode nsn = parentDef.NecessarySet(parentDef.And(new ConceptNodeWithUuids(parentDef.Concept(MetaData.USER____SOLOR))));
			parentDef.getRoot().addChildren(nsn);

			ConceptBuilder builder = conceptBuilderService.getDefaultConceptBuilder(user.userName, null, parentDef,
					MetaData.SOLOR_CONCEPT_ASSEMBLAGE____SOLOR.getAssemblageNid());

			// Set new author concept UUID to SSO UUID
			builder.setPrimordialUuid(user.userId);

			if (languageCoordinate.getDialectAssemblagePreferenceList() != null && languageCoordinate.getDialectAssemblagePreferenceList().length > 0)
			{
				for (int i : languageCoordinate.getDialectAssemblagePreferenceList())
				{
					builder.getFullySpecifiedDescriptionBuilder().addPreferredInDialectAssemblage(Get.conceptSpecification(i));
				}
			}

			List<Chronology> createdObjects = new ArrayList<>();
			ConceptChronology newCon = builder.build(adminEditCoordinate, ChangeCheckerMode.ACTIVE, createdObjects).getNoThrow();

			try
			{
				CommitRecord commitRecord = Util.commitCheck(
						Get.commitService().commit(adminEditCoordinate, "creating new concept: NID=" + newCon.getNid() + ", FQN=" + user.userName));
				log.debug("commit {}", commitRecord);
				return newCon.getNid();
			}
			catch (RestException e)
			{
				throw new RuntimeException("Creation of user concept failed. Caught " + e.getClass().getSimpleName() + " " + e.getLocalizedMessage());
			}
		}
	}
}