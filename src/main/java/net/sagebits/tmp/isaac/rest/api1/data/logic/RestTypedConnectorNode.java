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
import sh.isaac.api.coordinate.ManifoldCoordinate;
import sh.isaac.api.externalizable.IsaacObjectType;
import sh.isaac.model.logic.node.external.TypedNodeWithUuids;
import sh.isaac.model.logic.node.internal.TypedNodeWithNids;

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
	 * Optionally-populated RestConceptVersion for the connectorTypeConcept - pass expand='version'
	 * 
	 * To also include parent counts, specify expand=version,countParents
	 * 
	 * This will also include the primitive vs defined status for the concept - you must request an expand of 'version' to get
	 * the primitive vs defined information.
	 */
	@XmlElement
	RestConceptVersion connectorTypeConceptVersion;
	
	/**
	 * The String text description of the concept referred to by the connectorTypeConcept
	 */
	@XmlElement
	String connectorTypeDescription;

	protected RestTypedConnectorNode()
	{
		// For JAXB
	}

	/**
	 * @param typedNodeWithNids
	 * @param coordForRead 
	 */
	public RestTypedConnectorNode(TypedNodeWithNids typedNodeWithNids, ManifoldCoordinate coordForRead)
	{
		super(typedNodeWithNids, coordForRead);
		connectorTypeConcept = new RestIdentifiedObject(typedNodeWithNids.getTypeConceptNid(), IsaacObjectType.CONCEPT);
		finishSetup(coordForRead);
	}
	
	/**
	 * @param typedNodeWithUuids
	 * @param coordForRead 
	 */
	public RestTypedConnectorNode(TypedNodeWithUuids typedNodeWithUuids, ManifoldCoordinate coordForRead)
	{
		super(typedNodeWithUuids, coordForRead);
		connectorTypeConcept = new RestIdentifiedObject(Get.identifierService().getNidForUuids(typedNodeWithUuids.getTypeConceptUuid()),
				IsaacObjectType.CONCEPT);
		finishSetup(coordForRead);
	}
	
	private void finishSetup(ManifoldCoordinate coordForRead)
	{
		connectorTypeDescription = Get.conceptService().getSnapshot(coordForRead).conceptDescriptionText(connectorTypeConcept.nid);
		if (RequestInfo.get().shouldExpand(ExpandUtil.versionExpandable))
		{
			ConceptChronology cc = Get.conceptService().getConceptChronology(connectorTypeConcept.nid);
			LatestVersion<ConceptVersion> olcv = cc.getLatestVersion(coordForRead.getStampCoordinate());
			// TODO handle contradictions
			connectorTypeConceptVersion = new RestConceptVersion(olcv.get(), true, RequestInfo.get().shouldExpand(ExpandUtil.includeParents),
					RequestInfo.get().shouldExpand(ExpandUtil.countParents),
					false, false, RequestInfo.get().getStated(), false, RequestInfo.get().shouldExpand(ExpandUtil.terminologyType), false);
		}
		else
		{
			connectorTypeConceptVersion = null;
		}
	}
}
