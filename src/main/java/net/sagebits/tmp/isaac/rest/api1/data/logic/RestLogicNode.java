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

import java.util.UUID;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.webcohesion.enunciate.metadata.json.JsonSeeAlso;
import net.sagebits.tmp.isaac.rest.api1.data.enumerations.RestNodeSemanticType;
import sh.isaac.model.logic.node.AbstractLogicNode;

/**
 * 
 * {@link RestLogicNode}
 *
 * The abstract base class of all REST logic graph tree structure nodes.
 * Each node represents a part of the logic graph grammar and has, at least,
 * its own UUID, a RestNodeSemanticType enumerated type and a list of child RestNodeSemanticType nodes.
 * The allowed number of child RestNodeSemanticType nodes and any additional data
 * depend on the RestNodeSemanticType enumerated type.
 * 
 * @see RestConceptNode
 * @see RestUntypedConnectorNode
 * @see RestTypedConnectorNode
 * @see RestLiteralNodeBoolean
 * @see RestLiteralNodeInteger
 * @see RestLiteralNodeDouble
 * @see RestLiteralNodeString
 * @see RestLiteralNodeInstant
 * 
 * @author <a href="mailto:joel.kniaz.list@gmail.com">Joel Kniaz</a>
 * 
 */
@XmlSeeAlso({
	RestConceptNode.class,
	RestConnectorNode.class,
	RestLiteralNodeBoolean.class,
	RestLiteralNodeInteger.class,
	RestLiteralNodeDouble.class,
	RestLiteralNodeString.class,
	RestLiteralNodeInstant.class})
@JsonSeeAlso({
	RestConceptNode.class,
	RestConnectorNode.class,
	RestLiteralNodeBoolean.class,
	RestLiteralNodeInteger.class,
	RestLiteralNodeDouble.class,
	RestLiteralNodeString.class,
	RestLiteralNodeInstant.class})
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY)
@XmlRootElement
public abstract class RestLogicNode
{
	protected transient Logger LOG = LogManager.getLogger();

	/**
	 * The RestNodeSemanticType type of this node corresponding to the NodeSemantic enum
	 */
	@XmlElement
	RestNodeSemanticType nodeSemantic;

	/**
	 * The UUID of the logic node itself (not of any referenced or associated component or concept)
	 */
	@XmlElement
	UUID nodeUuid;

	protected RestLogicNode()
	{
		// For jaxb
	}

	protected static String getClassBaseName(Object obj)
	{
		return obj.getClass().getSimpleName().replaceAll(".*\\.", "");
	}

	/**
	 * @param passedLogicNode constructor takes an AbstractLogicNode representing the
	 *            root of a logic graph tree or tree fragment and recursively creates and populates an equivalent RestLogicNode
	 */
	public RestLogicNode(AbstractLogicNode passedLogicNode)
	{
		this.nodeUuid = passedLogicNode.getNodeUuid();
		this.nodeSemantic = new RestNodeSemanticType(passedLogicNode.getNodeSemantic());
	}
}
