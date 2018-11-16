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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;
import net.sagebits.tmp.isaac.rest.Util;
import net.sagebits.tmp.isaac.rest.api.exceptions.RestException;
import net.sagebits.tmp.isaac.rest.api1.data.mapping.RestMappingSetDisplayField;
import sh.isaac.MetaData;
import sh.isaac.api.Get;
import sh.isaac.api.LookupService;
import sh.isaac.api.chronicle.LatestVersion;
import sh.isaac.api.component.concept.ConceptChronology;
import sh.isaac.api.component.semantic.SemanticChronology;
import sh.isaac.api.component.semantic.version.SemanticVersion;
import sh.isaac.api.constants.DynamicConstants;
import sh.isaac.api.coordinate.StampCoordinate;
import sh.isaac.api.identity.IdentifiedObject;
import sh.isaac.api.util.AlphanumComparator;
import sh.isaac.api.util.NumericUtils;
import sh.isaac.api.util.UUIDUtil;
import sh.isaac.mapping.constants.IsaacMappingConstants;
import sh.isaac.model.configuration.StampCoordinates;

/**
 * 
 * {@link MapSetDisplayFieldsService}
 * 
 * Return available immutable map set display fields for use in ordering and displaying map set data
 *
 * @author <a href="mailto:joel.kniaz.list@gmail.com">Joel Kniaz</a>
 */
@RunLevel(LookupService.SL_L6_ISAAC_DEPENDENTS_RUNLEVEL)
@Service
public class MapSetDisplayFieldsService
{
	private static Logger log = LogManager.getLogger(MapSetDisplayFieldsService.class);

	private Map<UUID, Field> fields_;

	private static class Field
	{

		private final IdentifiedObject concept;

		private Field(IdentifiedObject conceptIdentifier)
		{
			super();
			this.concept = conceptIdentifier;
		}

		/**
		 * @return the identifiers of the concept that backs this field type
		 */
		public IdentifiedObject getBackingConcept()
		{
			return concept;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public String toString()
		{
			return "Field [concept=" + concept + "]";
		}
	}

	MapSetDisplayFieldsService()
	{
		// For HK2
	}

	private Map<UUID, Field> getFields()
	{
		synchronized (fields_)
		{
			if (fields_.size() == 0)
			{
				// Non-concept fields
				add(IsaacMappingConstants.get().MAPPING_CODE_DESCRIPTION);

				for (ConceptChronology cc : getAnnotationConcepts(StampCoordinates.getDevelopmentLatest()))
				{
					add(cc);
				}

				for (ConceptChronology idConcept : getIdentifierAnnotatedConcepts(StampCoordinates.getDevelopmentLatest()))
				{
					add(idConcept);
				}
			}

			return fields_;
		}
	}

	public void invalidateCache()
	{
		getFields().clear();
	}

	public RestMappingSetDisplayField[] getAllFields() throws RestException
	{
		ArrayList<RestMappingSetDisplayField> result = new ArrayList<RestMappingSetDisplayField>(getFields().size());
		for (Field f : getFields().values())
		{
			try
			{
				result.add(new RestMappingSetDisplayField(f.getBackingConcept(), null));
			}
			catch (Exception e)
			{
				String msg = "Failed constructing RestMappingSetDisplayField from field UUID=" + f.concept.getPrimordialUuid() + ", DESC="
						+ Get.conceptDescriptionText(f.concept.getNid()) + " BACKING=" + Get.conceptDescriptionText(f.getBackingConcept().getNid());
				log.error(msg, e);
				throw new RestException(msg);
			}
		}

		Collections.sort(result, new Comparator<RestMappingSetDisplayField>()
		{
			@Override
			public int compare(RestMappingSetDisplayField o1, RestMappingSetDisplayField o2)
			{
				return AlphanumComparator.getCachedInstance(true).compare(o1.description, o2.description);
			}
		});

		return result.toArray(new RestMappingSetDisplayField[result.size()]);
	}

	public Set<UUID> getAllGlobalFieldIds()
	{
		return Collections.unmodifiableSet(getFields().keySet());
	}

	/**
	 * Takes in a nid or uuid representing a concept, which represents a supported map set display field.
	 * returns the IdentifiedObject of the concept, after validating it is a valid field.
	 * If you pass a UUID in string form into this method, the most likely result is getting the same UUID back within an IdentifiedObject
	 * (the only other possibility, is getting back null, if the passed UUID isn't a valid display field)
	 * 
	 * @param conceptId
	 * @return an IdentifiedObject for the specified concept
	 */
	public IdentifiedObject getFieldConceptIdentifierByFieldConceptId(String conceptId)
	{

		String temp = conceptId.trim();
		Optional<UUID> conceptUUID = UUIDUtil.getUUID(temp);
		if (conceptUUID.isPresent())
		{
			return getFields().get(conceptUUID.get()).concept;
		}

		OptionalInt intId = NumericUtils.getInt(temp);
		if (intId.isPresent())
		{
			conceptUUID = Optional.of(Get.identifierService().getUuidPrimordialForNid(intId.getAsInt()));
			if (conceptUUID.isPresent())
			{
				return getFields().get(conceptUUID.get()).concept;
			}
		}

		return null;
	}

	synchronized private void add(IdentifiedObject object)
	{
		Field field = new Field(object);
		fields_.put(field.getBackingConcept().getPrimordialUuid(), field);
	}

	private static Set<ConceptChronology> getAnnotationConcepts(StampCoordinate sc)
	{
		Map<Integer, Set<Integer>> extensionDefinitionsByAssemblageNid = new HashMap<>();
		Set<ConceptChronology> annotationConcepts = new HashSet<>();

		Stream<SemanticChronology> extensionDefinitionChronologyStream = Get.assemblageService()
				.getSemanticChronologyStream(DynamicConstants.get().DYNAMIC_EXTENSION_DEFINITION.getNid());
		extensionDefinitionChronologyStream.forEach(extensionDefinitionChronology -> {
			LatestVersion<SemanticVersion> extensionDefinitionLatestOptional = extensionDefinitionChronology.getLatestVersion(sc);
			Util.logContradictions(log, extensionDefinitionLatestOptional);
			if (extensionDefinitionLatestOptional.isPresent())
			{
				// TODO handle contradictions
				SemanticVersion extensionDefinition = extensionDefinitionLatestOptional.get();
				Set<Integer> extensionDefinitionsForAssemblage = extensionDefinitionsByAssemblageNid.get(extensionDefinition.getReferencedComponentNid());
				if (extensionDefinitionsForAssemblage == null)
				{
					extensionDefinitionsForAssemblage = new HashSet<>();
					extensionDefinitionsByAssemblageNid.put(extensionDefinition.getReferencedComponentNid(), extensionDefinitionsForAssemblage);
				}
				extensionDefinitionsForAssemblage.add(extensionDefinition.getNid());
			}
		});

		for (Map.Entry<Integer, Set<Integer>> entry : extensionDefinitionsByAssemblageNid.entrySet())
		{
			ConceptChronology assemblageConcept = Get.conceptService().getConceptChronology(entry.getKey());
			if (entry.getValue().size() == 1)
			{
				log.debug("Registering annotation concept as map item display field: " + getUuidsWithDescriptions(assemblageConcept.getNid()));
				annotationConcepts.add(assemblageConcept);
			}
			else
			{
				log.debug("NOT registering annotation concept with " + entry.getValue().size() + " extension definitions as map item display field: "
						+ getUuidsWithDescriptions(assemblageConcept.getNid()));
			}
		}

		return annotationConcepts;
	}

	private static Set<ConceptChronology> getIdentifierAnnotatedConcepts(StampCoordinate sc)
	{
		Set<ConceptChronology> identifierAnnotatedConcepts = new HashSet<>();

		Stream<SemanticChronology> identifierAnnotationSemanticChronologyStream = Get.assemblageService()
				.getSemanticChronologyStream(MetaData.IDENTIFIER_SOURCE____SOLOR.getNid());
		identifierAnnotationSemanticChronologyStream.sequential().forEach(identifierAnnotationSemanticChronology -> {
			LatestVersion<SemanticVersion> identifierAnnotationSemanticLatestOptional = identifierAnnotationSemanticChronology.getLatestVersion(sc);
			Util.logContradictions(log, identifierAnnotationSemanticLatestOptional);
			if (identifierAnnotationSemanticLatestOptional.isPresent())
			{
				// TODO handle contradictions
				SemanticVersion identifierAnnotationSemantic = identifierAnnotationSemanticLatestOptional.get();
				identifierAnnotatedConcepts.add(Get.conceptService().getConceptChronology(identifierAnnotationSemantic.getReferencedComponentNid()));
			}
		});

		return identifierAnnotatedConcepts;
	}

	@PostConstruct
	public void construct()
	{
		fields_ = new HashMap<>();
	}

	@PreDestroy
	public void destroy()
	{
		fields_.clear();
	}

	private static Map<Object, String> getUuidsWithDescriptions(Integer... ids)
	{
		Map<Object, String> descriptionsByUuid = new HashMap<>();

		if (ids != null)
		{
			for (int id : ids)
			{
				ConceptChronology concept = Get.conceptService().getConceptChronology(id);

				descriptionsByUuid.put(concept.getPrimordialUuid(), Get.conceptDescriptionText(id));
			}
		}

		return descriptionsByUuid;
	}
}
