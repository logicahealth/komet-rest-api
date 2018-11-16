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

package net.sagebits.tmp.isaac.rest.api1.data.mapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import net.sagebits.tmp.isaac.rest.ExpandUtil;
import net.sagebits.tmp.isaac.rest.api.data.Expandable;
import net.sagebits.tmp.isaac.rest.api.data.Expandables;
import net.sagebits.tmp.isaac.rest.api.exceptions.RestException;
import net.sagebits.tmp.isaac.rest.api1.comment.CommentAPIs;
import net.sagebits.tmp.isaac.rest.api1.data.RestIdentifiedObject;
import net.sagebits.tmp.isaac.rest.api1.data.RestStampedVersion;
import net.sagebits.tmp.isaac.rest.api1.data.comment.RestCommentVersion;
import net.sagebits.tmp.isaac.rest.api1.data.enumerations.MapSetItemComponent;
import net.sagebits.tmp.isaac.rest.api1.data.semantic.RestDynamicSemanticData;
import net.sagebits.tmp.isaac.rest.api1.mapping.MappingAPIs;
import net.sagebits.tmp.isaac.rest.session.MapSetDisplayFieldsService;
import net.sagebits.tmp.isaac.rest.session.RequestInfo;
import sh.isaac.api.Get;
import sh.isaac.api.LookupService;
import sh.isaac.api.component.semantic.version.DynamicVersion;
import sh.isaac.api.component.semantic.version.dynamic.DynamicData;
import sh.isaac.api.component.semantic.version.dynamic.types.DynamicUUID;
import sh.isaac.api.coordinate.StampCoordinate;
import sh.isaac.api.externalizable.IsaacObjectType;
import sh.isaac.api.identity.IdentifiedObject;
import sh.isaac.mapping.constants.IsaacMappingConstants;
import sh.isaac.utility.Frills;

/**
 * 
 * {@link RestMappingItemVersion}
 *
 * @author <a href="mailto:daniel.armbrust.list@sagebits.net">Dan Armbrust</a>
 */
@XmlRootElement
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE)
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY)
public class RestMappingItemVersion extends RestMappingItemVersionBase
{
	private static Logger log = LogManager.getLogger(RestMappingItemVersion.class);

	/**
	 * The data that was not expanded as part of this call (but can be)
	 */
	@XmlElement
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public Expandables expandables;

	/**
	 * The concept that identifies the map set that this entry belongs to.
	 */
	@XmlElement
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public RestIdentifiedObject mapSetConcept;

	/**
	 * The source concept mapped by this map item.
	 */
	@XmlElement
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public RestIdentifiedObject sourceConcept;

	/**
	 * The (optional) target concept being mapped by this map item. This field is optional, and may be blank, if no target mapping
	 * is available.
	 */
	@XmlElement
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public RestIdentifiedObject targetConcept;

	/**
	 * An (optional) concept used to qualify this mapping entry.
	 */
	@XmlElement
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public RestIdentifiedObject qualifierConcept; // TODO rename this to equivalenceTypeConcept

	/**
	 * The identifier data for the semantic that represents this mapping item
	 */
	@XmlElement
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public RestIdentifiedObject identifiers;

	/**
	 * The StampedVersion details for this mapping entry
	 */
	@XmlElement
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public RestStampedVersion mappingItemStamp;

	/**
	 * The (optionally) populated comments attached to this map set. This field is only populated when requested via an 'expand' parameter.
	 */
	@XmlElement
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public List<RestCommentVersion> comments;

	/**
	 * If any computed columns were specified to be included in the definition of this mapset,
	 * they are returned here.
	 */
	@XmlElement
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public List<RestMappingItemComputedDisplayField> computedDisplayFields;

	protected RestMappingItemVersion()
	{
		// for Jaxb
		super();
	}

	public RestMappingItemVersion(DynamicVersion<?> semantic, int targetColPosition, int equivalenceTypeColPosition, boolean expandDescriptions,
			boolean expandComments, UUID processId, List<RestMappingSetDisplayField> displayFieldsFromMapSet)
	{
		final StampCoordinate stampCoordinate = RequestInfo.get().getStampCoordinate();
		identifiers = new RestIdentifiedObject(semantic.getChronology());
		mappingItemStamp = new RestStampedVersion(semantic);
		mapSetConcept = new RestIdentifiedObject(semantic.getAssemblageNid(), IsaacObjectType.CONCEPT);
		if (Get.identifierService().getObjectTypeForComponent(semantic.getReferencedComponentNid()) != IsaacObjectType.CONCEPT)
		{
			throw new RuntimeException("Source of the map is not a concept");
		}
		else
		{
			sourceConcept = new RestIdentifiedObject(semantic.getReferencedComponentNid(), IsaacObjectType.CONCEPT);
		}

		DynamicData[] data = semantic.getData();
		int offset = 0;

		if (data != null)
		{
			mapItemExtendedFields = new ArrayList<>();
			for (int i = 0; i < data.length; i++)
			{
				if (i == targetColPosition)
				{
					targetConcept = ((data[i] != null) ? new RestIdentifiedObject(((DynamicUUID) data[i]).getDataUUID()) : null);
					offset++;
				}
				else if (i == equivalenceTypeColPosition)
				{
					qualifierConcept = ((data[i] != null) ? new RestIdentifiedObject(((DynamicUUID) data[i]).getDataUUID()) : null);
					offset++;
				}
				else
				{
					RestDynamicSemanticData rdsd = RestDynamicSemanticData.translate(i, data[i]);
					if (rdsd != null)
					{
						rdsd.columnNumber = i - offset;  // renumber, to match with the numbers we are removing.
					}
					mapItemExtendedFields.add(rdsd);
				}
			}
		}

		if (displayFieldsFromMapSet == null || displayFieldsFromMapSet.size() == 0)
		{
			displayFieldsFromMapSet = new ArrayList<>();
			displayFieldsFromMapSet.addAll(MappingAPIs.getMappingSetDisplayFieldsFromMappingSet(mapSetConcept.nid, stampCoordinate));
		}
		if (displayFieldsFromMapSet != null && displayFieldsFromMapSet.size() > 0)
		{
			for (RestMappingSetDisplayField fieldFromMapSet : displayFieldsFromMapSet)
			{
				if (computedDisplayFields == null)
				{
					computedDisplayFields = new ArrayList<>();
				}
				// Only need to return these for computed fields...
				if (fieldFromMapSet.componentType.enumId != MapSetItemComponent.ITEM_EXTENDED.ordinal())
				{
					computedDisplayFields.add(constructDisplayField(mapSetConcept.nid, sourceConcept.nid, targetConcept, qualifierConcept, fieldFromMapSet.id,
							MapSetItemComponent.valueOf(fieldFromMapSet.componentType.enumName)));
				}
			}
		}

		expandables = new Expandables();
		if (expandComments)
		{
			try
			{
				comments = CommentAPIs.readComments(semantic.getNid() + "", processId, stampCoordinate);
			}
			catch (RestException e)
			{
				LogManager.getLogger().error("Unexpected", e);
				throw new RuntimeException(e);
			}
		}
		else
		{
			if (RequestInfo.get().returnExpandableLinks())
			{
				// TODO fix this expandable link
				expandables.add(new Expandable(ExpandUtil.comments, ""));
			}
		}
		if (expandables.size() == 0)
		{
			expandables = null;
		}
	}

	private static RestMappingItemComputedDisplayField constructDisplayField(int mapSetNid, int sourceConceptNid, RestIdentifiedObject targetConcept,
			RestIdentifiedObject equivalenceTypeConcept, String fieldId, MapSetItemComponent componentType)
	{
		Integer componentNid = null;
		String value = null;
		switch (componentType)
		{
			case SOURCE:
				componentNid = sourceConceptNid;
				break;
			case TARGET:
				componentNid = targetConcept == null ? null : targetConcept.nid;
				break;
			case EQUIVALENCE_TYPE:
				componentNid = equivalenceTypeConcept == null ? null : equivalenceTypeConcept.nid;
				break;
			default :
				String msg = "Invalid/unsupported MapSetItemComponent value \"" + componentType + "\".  Should be one of " + MapSetItemComponent.values();
				log.error(msg);
				throw new RuntimeException(msg);
		}

		/*
		 * Fields must correspond to entries returned by MapSetDisplayFieldsService.getAllFields()
		 */

		IdentifiedObject fieldType = LookupService.getService(MapSetDisplayFieldsService.class).getFieldConceptIdentifierByFieldConceptId(fieldId);
		if (fieldType == null)
		{
			throw new RuntimeException("Unsupported/unexpected map set field \"" + fieldId + "\"");
		}

		if (componentNid == null)
		{
			value = null;
		}
		else
		{
			if (fieldType.getPrimordialUuid().equals(IsaacMappingConstants.get().MAPPING_CODE_DESCRIPTION.getPrimordialUuid()))
			{
				Optional<String> descLatestVersion = Frills.getDescription(componentNid, RequestInfo.get().getStampCoordinate(),
						RequestInfo.get().getLanguageCoordinate());
				// TODO handle missing values and contradictions
				value = descLatestVersion.isPresent() ? descLatestVersion.get() : null;

			}
			else  // represents a single-column semantic field. Read the semantic data
			{
				Optional<String> valueOptional = Frills.getAnnotationStringValue(componentNid, fieldType.getNid(), RequestInfo.get().getStampCoordinate());
				// TODO handle missing values and contradictions
				value = valueOptional.isPresent() ? valueOptional.get() : null;
			}
		}

		try
		{
			return new RestMappingItemComputedDisplayField(fieldType, componentType, value);
		}
		catch (RestException e)
		{
			log.error(e);
			throw new RuntimeException(e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString()
	{
		return "RestMappingItemVersion [expandables=" + expandables + ", identifiers=" + identifiers + ", mappingItemStamp=" + mappingItemStamp
				+ ", mapSetConcept=" + mapSetConcept + ", sourceConcept=" + sourceConcept + ", targetConcept=" + targetConcept + ", equivalenceTypeConcept="
				+ qualifierConcept + ", mapItemExtendedFields=" + mapItemExtendedFields + "]";
	}
}
