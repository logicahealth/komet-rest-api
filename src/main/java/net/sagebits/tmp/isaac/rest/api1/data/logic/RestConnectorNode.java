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

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.webcohesion.enunciate.metadata.json.JsonSeeAlso;
import sh.isaac.api.logic.LogicNode;
import sh.isaac.model.logic.node.AbstractLogicNode;

/**
 * {@link RestConnectorNode} is the shared class for logic graph nodes
 * containing children that are linked together.  The semantic type 
 * of this node may be things like AndNode, OrNode, etc.
 * 
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
@XmlSeeAlso({RestTypedConnectorNode.class})
@JsonSeeAlso({RestTypedConnectorNode.class})
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY)
public class RestConnectorNode extends RestLogicNode
{
	/**
	 * The list of child RestLogicNode instances contained within this RestConnectorNode.
	 */
	@XmlElement
	List<RestLogicNode> children;

	protected RestConnectorNode()
	{
		// For JAXB
	}

	/**
	 * @param typedNodeWithNids
	 */
	public RestConnectorNode(AbstractLogicNode passedLogicNode)
	{
		super(passedLogicNode);
		AbstractLogicNode[] childrenOfPassedLogicNode = passedLogicNode.getChildren();
		this.children = new ArrayList<>(childrenOfPassedLogicNode.length);

		LOG.debug("Constructing " + getClassBaseName(this) + " " + this.nodeSemantic + " from " + passedLogicNode.toString() + " with {} child nodes",
				childrenOfPassedLogicNode.length);
		for (int i = 0; i < childrenOfPassedLogicNode.length; ++i)
		{
			LOG.debug(childrenOfPassedLogicNode[i].getNodeSemantic() + " node #" + ((int) i + 1) + " of " + childrenOfPassedLogicNode.length + " (node index="
					+ childrenOfPassedLogicNode[i].getNodeIndex() + "): class=" + getClassBaseName(childrenOfPassedLogicNode[i]) + ", "
					+ childrenOfPassedLogicNode[i]);
		}
		for (int i = 0; i < childrenOfPassedLogicNode.length; ++i)
		{
			LogicNode childOfPassedLogicNode = childrenOfPassedLogicNode[i];
			LOG.debug(getClassBaseName(this) + " " + this.nodeSemantic + " constructing child node from " + childOfPassedLogicNode + " with {} child nodes",
					childOfPassedLogicNode.getChildren().length);
			RestLogicNode newRestNode = RestLogicNodeFactory.create(childOfPassedLogicNode);
			LOG.debug(getClassBaseName(this) + " " + this.nodeSemantic + " ctor inserting new " + getClassBaseName(newRestNode) + " " + newRestNode.nodeSemantic
					+ " (index=" + childOfPassedLogicNode.getNodeIndex() + ") into child list at index " + i);
			children.add(newRestNode);
		}
	}
}
