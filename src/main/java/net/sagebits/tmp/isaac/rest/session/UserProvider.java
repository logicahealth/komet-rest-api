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

import static sh.isaac.api.logic.LogicalExpressionBuilder.And;
import static sh.isaac.api.logic.LogicalExpressionBuilder.ConceptAssertion;
import static sh.isaac.api.logic.LogicalExpressionBuilder.NecessarySet;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import javax.inject.Singleton;
import org.glassfish.hk2.api.Rank;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.sagebits.tmp.isaac.rest.Util;
import net.sagebits.tmp.isaac.rest.api.exceptions.RestException;
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
import sh.isaac.api.logic.LogicalExpression;
import sh.isaac.api.logic.LogicalExpressionBuilder;
import sh.isaac.api.logic.LogicalExpressionBuilderService;
import sh.isaac.api.util.UuidT5Generator;
import sh.isaac.misc.security.User;
import sh.isaac.misc.security.UserService;
import sh.isaac.model.configuration.EditCoordinates;
import sh.isaac.model.configuration.LanguageCoordinates;
import sh.isaac.model.configuration.LogicCoordinates;

/**
 * 
 * {@link UserProvider}
 *
 * @author <a href="mailto:daniel.armbrust.list@sagebits.net">Dan Armbrust</a>
 */

@Service
@Rank(value = 10)
@Singleton
public class UserProvider implements UserService
{
	private static final Logger log = LoggerFactory.getLogger(UserProvider.class);

	private static final long userMaxAge = 1000l * 60l * 60l;  // Any users in our list that haven't had their roles checked in an hour, should be
																  // purged.

	private static final ConcurrentHashMap<Integer, User> USERS = new ConcurrentHashMap<>();  // Store of all currently active users

	@PostConstruct
	protected void onStart()
	{
		Get.workExecutors().getScheduledThreadPoolExecutor().scheduleAtFixedRate(() -> {
			expireOldUsers();
		}, 15, 15, TimeUnit.MINUTES);
	}

	private void expireOldUsers()
	{
		try
		{
			log.debug("Expiring out-of-date users - size before: " + USERS.size());
			Iterator<Entry<Integer, User>> x = USERS.entrySet().iterator();
			while (x.hasNext())
			{
				if ((System.currentTimeMillis() - x.next().getValue().rolesCheckedAt()) > userMaxAge)
				{
					x.remove();
				}
			}
			log.debug("Finished expiring unused tokens - size after: " + USERS.size());
		}
		catch (Exception e)
		{
			log.error("Unexpected error expiring unused tokens", e);
		}
	}

	@Override
	public void addUser(User user)
	{

		log.debug("Adding user to cache: " + user.toString());

		int authorNid;
		if (!Get.identifierService().hasUuid(user.getId()))
		{
			// This creates the user concept in our DB
			authorNid = getAuthorNid(user.getName());
		}
		else
		{
			authorNid = Get.identifierService().getNidForUuids(user.getId());
		}

		USERS.put(authorNid, user);

		return;
	}

	@Override
	public Optional<User> get(UUID userId)
	{
		Optional<User> temp = Optional.ofNullable(USERS.get(Get.identifierService().getNidForUuids(userId)));
		log.debug("User Cache " + (temp.isPresent() ? "hit" : "miss") + " for user " + userId);
		return temp;
	}

	@Override
	public Optional<User> get(int userNid)
	{
		Optional<User> temp = Optional.ofNullable(USERS.get(userNid));
		log.debug("User Cache " + (temp.isPresent() ? "hit" : "miss") + " for user " + userNid);
		return temp;
	}

	public static UUID getUuidFromUserName(String fqn)
	{
		return UuidT5Generator.get(MetaData.USER____SOLOR.getPrimordialUuid(), fqn);
	}

	/**
	 * Get the concept nid that corresponds to a given editors user name - constructing the concept if necessary.
	 * 
	 * @param ssoProvidedUserName
	 * @return the nid of the author
	 */
	public static int getAuthorNid(String ssoProvidedUserName)
	{
		UUID userUUID = getUuidFromUserName(ssoProvidedUserName);

		// If the SSO UUID already persisted
		if (Get.identifierService().hasUuid(userUUID))
		{
			// Set authorNid to value corresponding to SSO UUID
			return Get.identifierService().getNidForUuids(userUUID);
		}
		else
		{
			log.debug("Creating new concept for user '" + ssoProvidedUserName + "'");
			EditCoordinate adminEditCoordinate = EditCoordinates.getDefaultUserMetadata();
			LanguageCoordinate languageCoordinate = LanguageCoordinates.getUsEnglishLanguageFullySpecifiedNameCoordinate();
			LogicCoordinate logicCoordinate = LogicCoordinates.getStandardElProfile();

			ConceptSpecification defaultDescriptionsLanguageConceptSpec = Get.conceptSpecification(languageCoordinate.getLanguageConceptNid());
			ConceptSpecification defaultDescriptionDialectConceptSpec = Get.conceptSpecification(languageCoordinate.getDialectAssemblagePreferenceList()[0]);

			ConceptBuilderService conceptBuilderService = LookupService.getService(ConceptBuilderService.class);
			conceptBuilderService.setDefaultLanguageForDescriptions(defaultDescriptionsLanguageConceptSpec);
			conceptBuilderService.setDefaultDialectAssemblageForDescriptions(defaultDescriptionDialectConceptSpec);
			conceptBuilderService.setDefaultLogicCoordinate(logicCoordinate);

			LogicalExpressionBuilder defBuilder = LookupService.getService(LogicalExpressionBuilderService.class).getLogicalExpressionBuilder();

			NecessarySet(And(ConceptAssertion(MetaData.USER____SOLOR, defBuilder)));

			LogicalExpression parentDef = defBuilder.build();

			ConceptBuilder builder = conceptBuilderService.getDefaultConceptBuilder(ssoProvidedUserName, null, parentDef,
					MetaData.SOLOR_CONCEPT_ASSEMBLAGE____SOLOR.getAssemblageNid());

			// Set new author concept UUID to SSO UUID
			builder.setPrimordialUuid(userUUID);

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
						Get.commitService().commit(adminEditCoordinate, "creating new concept: NID=" + newCon.getNid() + ", FQN=" + ssoProvidedUserName));
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
