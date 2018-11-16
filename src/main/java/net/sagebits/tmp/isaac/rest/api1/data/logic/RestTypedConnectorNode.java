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
import javax.xml.bind.annotation.XmlSeeAlso;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.webcohesion.enunciate.metadata.json.JsonSeeAlso;
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
import sh.isaac.model.logic.node.external.TypedNodeWithUuids;
import sh.isaac.model.logic.node.internal.TypedNodeWithNids;
import sh.isaac.utility.Frills;

/**
 * 
 * {@link RestTypedConnectorNode}
 *
 * RestTypedConnectorNode is the abstract base class for logic graph nodes
 * containing a connector type specified by connectorTypeConceptNid
 * and described by connectorTypeConceptDescription
 * 
 * RestTypedConnectorNode derived classes must have exactly 1 child node.
 * 
 * @see RestFeatureNode
 * @see RestRoleNode
 * 
 * @author <a href="mailto:joel.kniaz.list@gmail.com">Joel Kniaz</a>
 * 
 */
@XmlSeeAlso({ RestFeatureNode.class, RestRoleNode.class })
@JsonSeeAlso({ RestFeatureNode.class, RestRoleNode.class })
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY)
public abstract class RestTypedConnectorNode extends RestConnectorNode
{
	// private static final Logger LOG = LoggerFactory.getLogger(RestTypedConnectorNode.class);

	/**
	 * connectorTypeConcept the concept that tells you the type of the Node
	 */
	@XmlElement
	RestIdentifiedObject connectorTypeConcept;
	
	/**
	 * A boolean indicating whether the concept referred to by this connectorTypeConcept is defined rather than primitive
	 */
	@XmlElement
	boolean isConceptDefined;

	/**
	 * Optionally-populated RestConceptVersion for the connectorTypeConcept - pass expand='version'
	 */
	@XmlElement
	RestConceptVersion connectorTypeConceptVersion;

	protected RestTypedConnectorNode()
	{
		// For JAXB
	}

	/**
	 * @param typedNodeWithNids
	 */
	public RestTypedConnectorNode(TypedNodeWithNids typedNodeWithNids)
	{
		super(typedNodeWithNids);
		connectorTypeConcept = new RestIdentifiedObject(typedNodeWithNids.getTypeConceptNid(), IsaacObjectType.CONCEPT);
		finishSetup();
	}
	
	/**
	 * @param typedNodeWithUuids
	 */
	public RestTypedConnectorNode(TypedNodeWithUuids typedNodeWithUuids)
	{
		super(typedNodeWithUuids);
		connectorTypeConcept = new RestIdentifiedObject(Get.identifierService().getNidForUuids(typedNodeWithUuids.getTypeConceptUuid()),
				IsaacObjectType.CONCEPT);
		finishSetup();
	}
	
	private void finishSetup()
	{
		if (RequestInfo.get().shouldExpand(ExpandUtil.versionExpandable))
		{
			ConceptChronology cc = Get.conceptService().getConceptChronology(connectorTypeConcept.nid);
			LatestVersion<ConceptVersion> olcv = cc.getLatestVersion(RequestInfo.get().getStampCoordinate());
			// TODO handle contradictions
			connectorTypeConceptVersion = new RestConceptVersion(olcv.get(), true, null);
		}
		else
		{
			connectorTypeConceptVersion = null;
		}
		try
		{
			Optional<SemanticChronology> lgcOptional = Frills.getLogicGraphChronology(connectorTypeConcept.nid, RequestInfo.get().getStated(),
					RequestInfo.get().getStampCoordinate(), RequestInfo.get().getLanguageCoordinate(), RequestInfo.get().getLogicCoordinate());
			LatestVersion<LogicGraphVersion> lgs = Frills.getLogicGraphVersion(lgcOptional.get(), RequestInfo.get().getStampCoordinate());
			isConceptDefined = Frills.isConceptFullyDefined(lgs.get());
		}
		catch (Exception e)
		{
			LOG.warn("Problem getting isConceptDefined value (defaulting to false) for ConceptNode with {}", connectorTypeConcept.toString());
			isConceptDefined = false;
		}
	}
}
