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
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import net.sagebits.tmp.isaac.rest.ExpandUtil;
import net.sagebits.tmp.isaac.rest.Util;
import net.sagebits.tmp.isaac.rest.api.data.Expandable;
import net.sagebits.tmp.isaac.rest.api.data.Expandables;
import net.sagebits.tmp.isaac.rest.api.exceptions.RestException;
import net.sagebits.tmp.isaac.rest.api1.comment.CommentAPIs;
import net.sagebits.tmp.isaac.rest.api1.data.RestIdentifiedObject;
import net.sagebits.tmp.isaac.rest.api1.data.RestStampedVersion;
import net.sagebits.tmp.isaac.rest.api1.data.comment.RestCommentVersion;
import net.sagebits.tmp.isaac.rest.api1.data.semantic.RestDynamicSemanticColumnInfo;
import net.sagebits.tmp.isaac.rest.api1.data.semantic.RestDynamicSemanticData;
import net.sagebits.tmp.isaac.rest.api1.mapping.MappingAPIs;
import net.sagebits.tmp.isaac.rest.session.RequestInfo;
import sh.isaac.MetaData;
import sh.isaac.api.Get;
import sh.isaac.api.Status;
import sh.isaac.api.chronicle.LatestVersion;
import sh.isaac.api.chronicle.VersionType;
import sh.isaac.api.component.concept.ConceptVersion;
import sh.isaac.api.component.semantic.SemanticChronology;
import sh.isaac.api.component.semantic.version.DescriptionVersion;
import sh.isaac.api.component.semantic.version.DynamicVersion;
import sh.isaac.api.component.semantic.version.dynamic.DynamicData;
import sh.isaac.api.component.semantic.version.dynamic.types.DynamicNid;
import sh.isaac.api.component.semantic.version.dynamic.types.DynamicString;
import sh.isaac.api.constants.DynamicConstants;
import sh.isaac.api.coordinate.StampCoordinate;
import sh.isaac.api.externalizable.IsaacObjectType;
import sh.isaac.mapping.constants.IsaacMappingConstants;
import sh.isaac.utility.Frills;

/**
 * 
 * {@link RestMappingSetVersion}
 *
 * @author <a href="mailto:daniel.armbrust.list@sagebits.net">Dan Armbrust</a>
 */
@XmlRootElement
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE)
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY)
public class RestMappingSetVersion extends RestMappingSetVersionBase implements Comparable<RestMappingSetVersion>
{
	private transient static Logger log = LogManager.getLogger(RestMappingSetVersion.class);

	// TODO populate expandables
	/**
	 * The data that was not expanded as part of this call (but can be)
	 */
	@XmlElement
	public Expandables expandables;

	/**
	 * The identifier data of the concept that represents this mapping set
	 */
	@XmlElement
	public RestIdentifiedObject identifiers;

	/**
	 * The identifiers of the terminologies (concepts that represent terminologies) that this mapset concept is part of. This is determined by whether
	 * or not there is
	 * version of this concept present with a module that extends from one of the children of the {@link MetaData#MODULE} concepts.
	 * 
	 * Note that this is calculated WITH taking into account the view coordinate, including the active / inactive state of the concept in any
	 * particular terminology.
	 * This means that if a concept is present in both Snomed CT and the US Extension modules, but your view coordinate excludes the US Extension,
	 * this will not
	 * include the US Extension module.
	 * 
	 * See 1/system/terminologyTypes for more details on the potential terminology concepts that will be returned.
	 */
	@XmlElement
	@JsonInclude(JsonInclude.Include.NON_NULL)
	RestIdentifiedObject[] terminologyTypes;

	/**
	 * The StampedVersion details for this map set definition
	 */
	@XmlElement
	public RestStampedVersion mappingSetStamp;

	/**
	 * The (optional) extended fields which carry additional information about this map set definition.
	 */
	@XmlElement
	public List<RestMappingSetExtensionValue> mapSetExtendedFields = new ArrayList<>();

	/**
	 * The fields that are declared for each map item instance that is created using this map set definition.
	 */
	@XmlElement
	public List<RestDynamicSemanticColumnInfo> mapItemFieldsDefinition = new ArrayList<>();

	/**
	 * Specifies display fields that should populate each item and respective order
	 */
	@XmlElement
	public List<RestMappingSetDisplayField> displayFields = new ArrayList<>();

	/**
	 * The (optionally) populated comments attached to this map set. This field is only populated when requested via an 'expand' parameter.
	 */
	@XmlElement
	public List<RestCommentVersion> comments;

	// TODO 2 Dan needs an option to return other semantics here...

	protected RestMappingSetVersion()
	{
		// for Jaxb
		super();
	}

	/**
	 * This code expects to read a semantic of type {@link IsaacMappingConstants#DYNAMIC_SEMANTIC_MAPPING_SEMANTIC_TYPE}
	 * @param mappingConcept 
	 * 
	 * @param semantic
	 * @param stampCoord
	 * @param includeComments 
	 * @param processId 
	 */
	@SuppressWarnings("rawtypes")
	public RestMappingSetVersion(ConceptVersion mappingConcept, DynamicVersion<?> semantic, StampCoordinate stampCoord, boolean includeComments, UUID processId)
	{
		super();

		final StampCoordinate myStampCoord = stampCoord.makeCoordinateAnalog(Status.ACTIVE, Status.INACTIVE);
		active = mappingConcept.getStatus() == Status.ACTIVE;
		identifiers = new RestIdentifiedObject(mappingConcept.getChronology());
		// TODO whenever we make an edit to any component of the map set, we will also need to commit the concept, so that this stamp
		// always updates with any other stamp that is updated
		mappingSetStamp = new RestStampedVersion(mappingConcept);

		if (semantic.getData().length > 0 && semantic.getData()[0] != null)
		{
			purpose = ((DynamicString) semantic.getData()[0]).getDataString();
		}

		// read the extended field definition information
		mapItemFieldsDefinition.addAll(MappingAPIs.getItemFieldDefinitions(mappingConcept.getNid()));

		// Read the extended fields off of the map set concept.
		// Strings
		Get.assemblageService().getSemanticChronologyStreamForComponentFromAssemblage(mappingConcept.getNid(),
				IsaacMappingConstants.get().DYNAMIC_SEMANTIC_MAPPING_STRING_EXTENSION.getNid()).forEach(stringExtensionSemantic -> {
					SemanticChronology rawSemanticChronology = (SemanticChronology) stringExtensionSemantic;
					LatestVersion<DynamicVersion> ds = rawSemanticChronology.getLatestVersion(myStampCoord);
					Util.logContradictions(log, ds);
					// TODO handle contradictions
					if (ds.isPresent())
					{
						int nameNid = ((DynamicNid) ds.get().getData(0)).getDataNid();
						DynamicData value = null;
						if (ds.get().getData().length > 1)
						{
							value = ds.get().getData(1);
						}
						mapSetExtendedFields
								.add(new RestMappingSetExtensionValue(nameNid, RestDynamicSemanticData.translate(1, value), ds.get().getStatus() == Status.ACTIVE));
					}
				});

		// Nids
		Get.assemblageService().getSemanticChronologyStreamForComponentFromAssemblage(mappingConcept.getNid(),
				IsaacMappingConstants.get().DYNAMIC_SEMANTIC_MAPPING_NID_EXTENSION.getNid()).forEach(nidExtensionSemantic -> {
					SemanticChronology rawSemanticChronology = (SemanticChronology) nidExtensionSemantic;
					LatestVersion<DynamicVersion> latest = rawSemanticChronology.getLatestVersion(myStampCoord);
					Util.logContradictions(log, latest);
					// TODO handle contradictions
					if (latest.isPresent())
					{
						DynamicVersion<?> ds = latest.get();
						int nameNid = ((DynamicNid) ds.getData(0)).getDataNid();
						DynamicData value = null;
						if (ds.getData().length > 1)
						{
							value = ds.getData(1);
						}
						// column number makes no sense here.
						mapSetExtendedFields
								.add(new RestMappingSetExtensionValue(nameNid, RestDynamicSemanticData.translate(-1, value), ds.getStatus() == Status.ACTIVE));
					}
				});

		// Read the the description values
		Get.assemblageService().getSemanticChronologyStreamForComponent(mappingConcept.getNid()).filter(s -> s.getVersionType() == VersionType.DESCRIPTION)
				.forEach(descriptionC -> {
					if (name != null && description != null && inverseName != null)
					{
						// noop... sigh... can't short-circuit in a forEach....
					}
					else
					{
						LatestVersion<DescriptionVersion> dv = descriptionC.getLatestVersion(myStampCoord);
						Util.logContradictions(log, dv);
						// TODO handle contradictions
						if (dv.isPresent())
						{
							if (dv.get().getDescriptionTypeConceptNid() == MetaData.REGULAR_NAME_DESCRIPTION_TYPE____SOLOR.getNid())
							{
								if (Frills.isDescriptionPreferred(dv.get().getNid(), myStampCoord))
								{
									name = dv.get().getText();
								}
								else
								// see if it is the inverse name
								{
									if (Get.assemblageService().getSemanticChronologyStreamForComponentFromAssemblage(dv.get().getNid(),
											DynamicConstants.get().DYNAMIC_ASSOCIATION_INVERSE_NAME.getNid()).anyMatch(semanticC -> {
												return dv.get().getChronology().isLatestVersionActive(myStampCoord);
											}))
									{
										inverseName = dv.get().getText();
									}
								}
							}
							else if (dv.get().getDescriptionTypeConceptNid() == MetaData.DEFINITION_DESCRIPTION_TYPE____SOLOR.getNid())
							{
								if (Frills.isDescriptionPreferred(dv.get().getNid(), myStampCoord))
								{
									description = dv.get().getText();
								}
							}
						}
					}
				});

		displayFields.addAll(MappingAPIs.getMappingSetDisplayFieldsFromMappingSet(mappingConcept.getNid(), stampCoord));

		// figure out the terminology info
		HashSet<Integer> terminologyTypeNids = Frills.getTerminologyTypes(mappingConcept.getChronology(), RequestInfo.get().getStampCoordinate());

		terminologyTypes = new RestIdentifiedObject[terminologyTypeNids.size()];
		int i = 0;
		for (int nid : terminologyTypeNids)
		{
			terminologyTypes[i++] = new RestIdentifiedObject(nid, IsaacObjectType.CONCEPT);
		}

		if (includeComments)
		{
			try
			{
				comments = CommentAPIs.readComments(mappingConcept.getNid() + "", processId, myStampCoord);
			}
			catch (RestException e)
			{
				LogManager.getLogger().error("Unexpected", e);
				throw new RuntimeException(e);
			}
		}
		else
		{
			expandables = new Expandables();
			if (RequestInfo.get().returnExpandableLinks())
			{
				// TODO fix this expandable link
				expandables.add(new Expandable(ExpandUtil.comments, ""));
			}
		}
	}

	/**
	 * Sorts by name
	 */
	@Override
	public int compareTo(RestMappingSetVersion o)
	{
		return name.compareTo(o.name);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString()
	{
		return "RestMappingSetVersion [expandables=" + expandables + ", identifiers=" + identifiers + ", mappingSetStamp=" + mappingSetStamp
				+ ", mapSetExtendedFields=" + mapSetExtendedFields + ", mapItemFieldsDefinition=" + mapItemFieldsDefinition + ", mapSetFields=" + displayFields
				+ ", comments=" + comments + ", name=" + name + ", inverseName=" + inverseName + ", description=" + description + ", purpose=" + purpose
				+ ", active=" + active + "]";
	}
}
