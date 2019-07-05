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
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import net.sagebits.tmp.isaac.rest.ExpandUtil;
import net.sagebits.tmp.isaac.rest.api1.data.RestIdentifiedObject;
import net.sagebits.tmp.isaac.rest.api1.data.concept.RestConceptVersion;
import net.sagebits.tmp.isaac.rest.api1.data.enumerations.RestConcreteDomainOperatorsType;
import net.sagebits.tmp.isaac.rest.session.RequestInfo;
import sh.isaac.api.Get;
import sh.isaac.api.chronicle.LatestVersion;
import sh.isaac.api.component.concept.ConceptChronology;
import sh.isaac.api.component.concept.ConceptVersion;
import sh.isaac.api.coordinate.ManifoldCoordinate;
import sh.isaac.api.logic.NodeSemantic;
import sh.isaac.model.logic.ConcreteDomainOperators;
import sh.isaac.model.logic.node.external.FeatureNodeWithUuids;
import sh.isaac.model.logic.node.internal.FeatureNodeWithNids;

/**
 * 
 * {@link RestFeatureNode}
 *
 * @author <a href="mailto:joel.kniaz.list@gmail.com">Joel Kniaz</a>
 *
 *         The RestFeatureNode contains a RestConcreteDomainOperatorsType operator type,
 *         must have exactly 1 child node, which is one of the literal types like {@link NodeSemantic#LITERAL_BOOLEAN}
 * 
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY)
public class RestFeatureNode extends RestTypedConnectorNode
{

	/**
	 * RestFeatureNode contains a RestConcreteDomainOperatorsType/ConcreteDomainOperators instance,
	 * which is an enumeration specifying a type of comparison
	 * 
	 * RestFeatureNode must have exactly 1 child node.
	 * 
	 * Available RestConcreteDomainOperatorsType/ConcreteDomainOperator values include
	 * EQUALS,
	 * LESS_THAN,
	 * LESS_THAN_EQUALS,
	 * GREATER_THAN,
	 * GREATER_THAN_EQUALS
	 */
	@XmlElement
	RestConcreteDomainOperatorsType operator;
	
	/**
	 * connectorTypeConcept the concept that tells you the unit type of a measure
	 */
	@XmlElement
	RestIdentifiedObject measureSemanticConcept;

	/**
	 * Optionally-populated RestConceptVersion for the measureSemanticConcept - pass expand=version
	 */
	@XmlElement
	RestConceptVersion measureSemanticConceptVersion;

	protected RestFeatureNode()
	{
		// For JAXB
	}

	/**
	 * @param featureNodeWithNids
	 * @param coordForRead 
	 */
	public RestFeatureNode(FeatureNodeWithNids featureNodeWithNids, ManifoldCoordinate coordForRead)
	{
		super(featureNodeWithNids, coordForRead);
		setup(featureNodeWithNids.getOperator(), Get.concept(featureNodeWithNids.getMeasureSemanticNid()), coordForRead);
	}

	/**
	 * @param featureNodeWithUuids
	 * @param coordForRead 
	 */
	public RestFeatureNode(FeatureNodeWithUuids featureNodeWithUuids, ManifoldCoordinate coordForRead)
	{
		super(featureNodeWithUuids, coordForRead);
		setup(featureNodeWithUuids.getOperator(), Get.concept(featureNodeWithUuids.getMeasureSemanticUuid()), coordForRead);
	}
	
	private void setup(ConcreteDomainOperators cdo, ConceptChronology msc, ManifoldCoordinate coordForRead)
	{
		operator = new RestConcreteDomainOperatorsType(cdo);
		measureSemanticConcept = new RestIdentifiedObject(msc);

		if (RequestInfo.get().shouldExpand(ExpandUtil.versionExpandable))
		{
			LatestVersion<ConceptVersion> olcv = msc.getLatestVersion(coordForRead.getStampCoordinate());
			// TODO handle contradictions
			measureSemanticConceptVersion = new RestConceptVersion(olcv.get(), true, RequestInfo.get().shouldExpand(ExpandUtil.includeParents),
					RequestInfo.get().shouldExpand(ExpandUtil.countParents),
					false, false, RequestInfo.get().getStated(), false, RequestInfo.get().shouldExpand(ExpandUtil.terminologyType), false);
		}
		else
		{
			measureSemanticConceptVersion = null;
		}
	}
}
