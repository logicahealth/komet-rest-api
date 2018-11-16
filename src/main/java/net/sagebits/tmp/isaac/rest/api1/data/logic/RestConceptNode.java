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

package net.sagebits.tmp.isaac.rest.api1.data.logic;

import java.util.Optional;
import javax.xml.bind.annotation.XmlElement;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import net.sagebits.tmp.isaac.rest.ExpandUtil;
import net.sagebits.tmp.isaac.rest.api1.data.RestIdentifiedObject;
import net.sagebits.tmp.isaac.rest.api1.data.concept.RestConceptVersion;
import net.sagebits.tmp.isaac.rest.session.RequestInfo;
import sh.isaac.api.Get;
import sh.isaac.api.chronicle.LatestVersion;
import sh.isaac.api.component.concept.ConceptChronology;
import sh.isaac.api.component.concept.ConceptVersion;
import sh.isaac.api.component.semantic.SemanticChronology;
import sh.isaac.api.component.semantic.version.LogicGraphVersion;
import sh.isaac.api.externalizable.IsaacObjectType;
import sh.isaac.model.logic.node.external.ConceptNodeWithUuids;
import sh.isaac.model.logic.node.internal.ConceptNodeWithNids;
import sh.isaac.utility.Frills;

/**
 * 
 * {@link RestConceptNode}
 *
 * @author <a href="mailto:joel.kniaz.list@gmail.com">Joel Kniaz</a>
 *
 *         A REST logic graph node containing (referencing) a concept by nid and its text description.
 *         RestConceptNode has RestNodeSemanticType. == NodeSemantic.CONCEPT and should never have any child nodes.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY)
public class RestConceptNode extends RestLogicNode
{
	private static Logger LOG = LogManager.getLogger();

	/**
	 * The int nid of the concept referred to by this REST logic graph node
	 */
	@XmlElement
	RestIdentifiedObject concept;

	/**
	 * Optionally-expandable RestConceptVersion corresponding to RestConceptNode concept
	 */
	@XmlElement
	RestConceptVersion conceptVersion;

	/**
	 * A boolean indicating whether the concept referred to by this RestConceptVersion is defined rather than primitive
	 */
	@XmlElement
	boolean isConceptDefined;

	/**
	 * The String text description of the concept referred to by this REST logic graph node. It is included as a convenience, as it may be retrieved
	 * based on the concept nid.
	 */
	@XmlElement
	String conceptDescription;

	protected RestConceptNode()
	{
		// For JAXB
	}

	/**
	 * @param conceptNodeWithUuids
	 */
	public RestConceptNode(ConceptNodeWithUuids conceptNodeWithUuids)
	{
		super(conceptNodeWithUuids);
		finishSetup(Get.identifierService().getNidForUuids(conceptNodeWithUuids.getConceptUuid()));
	}
	
	/**
	 * @param conceptNodeWithNids
	 */
	public RestConceptNode(ConceptNodeWithNids conceptNodeWithNids)
	{
		super(conceptNodeWithNids);
		finishSetup(conceptNodeWithNids.getConceptNid());
	}

	private void finishSetup(int conceptNid)
	{
		this.concept = new RestIdentifiedObject(conceptNid, IsaacObjectType.CONCEPT);
		conceptDescription = Get.conceptService().getSnapshot(RequestInfo.get().getManifoldCoordinate()).conceptDescriptionText(conceptNid);
		try
		{
			// TODO Joel - what does this mean? You left: Fine tune this when data problems resolved
			Optional<SemanticChronology> lgcOptional = Frills.getLogicGraphChronology(conceptNid, RequestInfo.get().getStated(),
					RequestInfo.get().getStampCoordinate(), RequestInfo.get().getLanguageCoordinate(), RequestInfo.get().getLogicCoordinate());
			LatestVersion<LogicGraphVersion> lgs = Frills.getLogicGraphVersion(lgcOptional.get(), RequestInfo.get().getStampCoordinate());
			isConceptDefined = Frills.isConceptFullyDefined(lgs.get());
		}
		catch (Exception e)
		{
			LOG.warn("Problem getting isConceptDefined value (defaulting to false) for ConceptNode with {}", this.concept.toString());
			isConceptDefined = false;
		}

		if (RequestInfo.get().shouldExpand(ExpandUtil.versionExpandable))
		{
			ConceptChronology cc = Get.conceptService().getConceptChronology(conceptNid);
			LatestVersion<ConceptVersion> olcv = cc.getLatestVersion(RequestInfo.get().getStampCoordinate());
			// TODO handle contradictions
			// TODO handle processId?
			conceptVersion = new RestConceptVersion(olcv.get(), true, null);
		}
		else
		{
			conceptVersion = null;
		}
	}
}