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

package net.sagebits.tmp.isaac.rest.api1.data.semantic;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import net.sagebits.tmp.isaac.rest.api.exceptions.RestException;
import net.sagebits.tmp.isaac.rest.api1.data.RestIdentifiedObject;
import net.sagebits.tmp.isaac.rest.session.RequestInfo;
import sh.isaac.MetaData;
import sh.isaac.api.Get;
import sh.isaac.api.Status;
import sh.isaac.api.collections.NidSet;
import sh.isaac.api.component.semantic.version.DescriptionVersion;
import sh.isaac.api.constants.DynamicConstants;
import sh.isaac.api.coordinate.StampPrecedence;
import sh.isaac.api.externalizable.IsaacObjectType;
import sh.isaac.model.coordinate.ManifoldCoordinateImpl;
import sh.isaac.model.coordinate.StampCoordinateImpl;
import sh.isaac.model.coordinate.StampPositionImpl;
import sh.isaac.utility.Frills;

/**
 * 
 * {@link RestSemanticDescriptionVersion}
 *
 * @author <a href="mailto:daniel.armbrust.list@sagebits.net">Dan Armbrust</a>
 */
@XmlRootElement
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE)
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY)
public class RestSemanticDescriptionVersion extends RestSemanticVersion
{
	/**
	 * The concept that represents the case significance flag on the description .
	 * This should be description case sensitive, description not case sensitive or description initial character sensitive
	 */
	@XmlElement
	public RestIdentifiedObject caseSignificanceConcept;

	/**
	 * The concept that represents the language of the description (note, this is NOT the dialect)
	 */
	@XmlElement
	public RestIdentifiedObject languageConcept;

	/**
	 * The text of the description
	 */
	@XmlElement
	public String text;

	/**
	 * The concept that represents the type of the description.
	 * This should be FQN, Regular Name, or Definition.
	 */
	@XmlElement
	public RestIdentifiedObject descriptionTypeConcept;

	/**
	 * The optional concept that represents the extended type of the description.
	 * This should be a {@link MetaData#EXTENDED_DESCRIPTION_TYPE____SOLOR}.
	 */
	@XmlElement
	public RestIdentifiedObject descriptionExtendedTypeConcept;

	/**
	 * The dialects attached to this semantic. Not populated by default, include expand=nestedSemantics to expand this.
	 */
	@XmlElement
	public List<RestDynamicSemanticVersion> dialects = new ArrayList<>();

	protected RestSemanticDescriptionVersion()
	{
		// for Jaxb
	}

	public RestSemanticDescriptionVersion(DescriptionVersion dsv, boolean includeChronology, boolean expandNested, boolean expandReferenced, UUID processId)
			throws RestException
	{
		super();
		setup(dsv, includeChronology, expandNested, expandReferenced, (restSemanticVersion -> {
			// If the assemblage is a dialect, put it in our list.
			if (Get.taxonomyService().getSnapshotNoTree(new ManifoldCoordinateImpl(
					new StampCoordinateImpl(StampPrecedence.TIME, 
								new StampPositionImpl(Long.MAX_VALUE, RequestInfo.get().getStampCoordinate().getStampPosition().getStampPathSpecification().getNid()), 
							NidSet.EMPTY, Status.ACTIVE_ONLY_SET), 
						Get.configurationService().getGlobalDatastoreConfiguration().getDefaultLanguageCoordinate()))
					.isKindOf(restSemanticVersion.semanticChronology.assemblage.nid, MetaData.DIALECT_ASSEMBLAGE____SOLOR.getNid()))
			{
				dialects.add((RestDynamicSemanticVersion) restSemanticVersion);
				return false;
			}
			// if the assemblage is extendedDescriptionType, skip - we handle below
			if (restSemanticVersion.semanticChronology.assemblage.nid == DynamicConstants.get().DYNAMIC_EXTENDED_DESCRIPTION_TYPE.getNid())
			{
				return false;
			}
			return true;
		}), processId);
		caseSignificanceConcept = new RestIdentifiedObject(dsv.getCaseSignificanceConceptNid(), IsaacObjectType.CONCEPT);
		languageConcept = new RestIdentifiedObject(dsv.getLanguageConceptNid(), IsaacObjectType.CONCEPT);
		text = dsv.getText();
		descriptionTypeConcept = new RestIdentifiedObject(dsv.getDescriptionTypeConceptNid(), IsaacObjectType.CONCEPT);

		// populate descriptionExtendedTypeConceptNid
		Optional<UUID> descriptionExtendedTypeOptional = Frills.getDescriptionExtendedTypeConcept(RequestInfo.get().getStampCoordinate(), dsv.getNid(), false);
		if (descriptionExtendedTypeOptional.isPresent())
		{
			descriptionExtendedTypeConcept = new RestIdentifiedObject(descriptionExtendedTypeOptional.get());
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString()
	{
		return "RestSemanticDescriptionVersion [" + "caseSignificanceConceptNid=" + caseSignificanceConcept + ", languageConceptNid=" + languageConcept
				+ ", text=" + text + ", descriptionTypeConceptNid=" + descriptionTypeConcept + ", descriptionExtendedTypeConceptNid="
				+ descriptionExtendedTypeConcept + ", dialects=" + dialects + ", expandables=" + expandables + ", semanticChronology=" + semanticChronology
				+ ", semanticVersion=" + semanticVersion + ", nestedSemantics=" + nestedSemantics + "]";
	}
}
