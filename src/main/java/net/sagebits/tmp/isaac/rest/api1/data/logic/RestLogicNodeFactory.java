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

import sh.isaac.api.coordinate.ManifoldCoordinate;
import sh.isaac.api.logic.LogicNode;
import sh.isaac.model.logic.node.AndNode;
import sh.isaac.model.logic.node.DisjointWithNode;
import sh.isaac.model.logic.node.LiteralNodeBoolean;
import sh.isaac.model.logic.node.LiteralNodeDouble;
import sh.isaac.model.logic.node.LiteralNodeInstant;
import sh.isaac.model.logic.node.LiteralNodeInteger;
import sh.isaac.model.logic.node.LiteralNodeString;
import sh.isaac.model.logic.node.NecessarySetNode;
import sh.isaac.model.logic.node.OrNode;
import sh.isaac.model.logic.node.RootNode;
import sh.isaac.model.logic.node.SufficientSetNode;
import sh.isaac.model.logic.node.external.ConceptNodeWithUuids;
import sh.isaac.model.logic.node.external.FeatureNodeWithUuids;
import sh.isaac.model.logic.node.external.RoleNodeAllWithUuids;
import sh.isaac.model.logic.node.external.RoleNodeSomeWithUuids;
import sh.isaac.model.logic.node.internal.ConceptNodeWithNids;
import sh.isaac.model.logic.node.internal.FeatureNodeWithNids;
import sh.isaac.model.logic.node.internal.RoleNodeAllWithNids;
import sh.isaac.model.logic.node.internal.RoleNodeSomeWithNids;

/**
 * 
 * {@link RestLogicNodeFactory}
 *
 * @author <a href="mailto:joel.kniaz.list@gmail.com">Joel Kniaz</a>
 *
 *         Static factory that constructs an appropriate RestLogicNode according to the type of passed LogicNode.
 *         The RestLogicNode constructor is recursive, so each node returned will be a fully populated
 *         logic graph tree or tree fragment.
 */
public final class RestLogicNodeFactory
{
	private RestLogicNodeFactory()
	{
	}

	public static RestLogicNode create(LogicNode logicNode, ManifoldCoordinate coordForRead)
	{
		//TODO we don't yet support any of the SubstitutionNode hierarchy
		//TODO nor do we support TemplateNode
		if (logicNode instanceof AndNode)
			return new RestConnectorNode((AndNode) logicNode, coordForRead);
		else if (logicNode instanceof ConceptNodeWithNids)
			return new RestConceptNode((ConceptNodeWithNids) logicNode, coordForRead);
		else if (logicNode instanceof ConceptNodeWithUuids)
			return new RestConceptNode((ConceptNodeWithUuids) logicNode, coordForRead);
		else if (logicNode instanceof DisjointWithNode)
			return new RestConnectorNode((DisjointWithNode) logicNode, coordForRead);
		else if (logicNode instanceof FeatureNodeWithNids)
			return new RestFeatureNode((FeatureNodeWithNids) logicNode, coordForRead);
		else if (logicNode instanceof FeatureNodeWithUuids)
			return new RestFeatureNode((FeatureNodeWithUuids) logicNode, coordForRead);
		else if (logicNode instanceof LiteralNodeBoolean)
			return new RestLiteralNodeBoolean((LiteralNodeBoolean) logicNode);
		else if (logicNode instanceof LiteralNodeDouble)
			return new RestLiteralNodeDouble((LiteralNodeDouble) logicNode);
		else if (logicNode instanceof LiteralNodeInstant)
			return new RestLiteralNodeInstant((LiteralNodeInstant) logicNode);
		else if (logicNode instanceof LiteralNodeInteger)
			return new RestLiteralNodeInteger((LiteralNodeInteger) logicNode);
		else if (logicNode instanceof LiteralNodeString)
			return new RestLiteralNodeString((LiteralNodeString) logicNode);
		else if (logicNode instanceof NecessarySetNode)
			return new RestConnectorNode((NecessarySetNode) logicNode, coordForRead);
		else if (logicNode instanceof OrNode)
			return new RestConnectorNode((OrNode) logicNode, coordForRead);
		else if (logicNode instanceof RoleNodeAllWithNids)
			return new RestRoleNode((RoleNodeAllWithNids) logicNode, coordForRead);
		else if (logicNode instanceof RoleNodeAllWithUuids)
			return new RestRoleNode((RoleNodeAllWithUuids) logicNode, coordForRead);
		else if (logicNode instanceof RoleNodeSomeWithNids)
			return new RestRoleNode((RoleNodeSomeWithNids) logicNode, coordForRead);
		else if (logicNode instanceof RoleNodeSomeWithUuids)
			return new RestRoleNode((RoleNodeSomeWithUuids) logicNode, coordForRead);
		else if (logicNode instanceof RootNode)
			return new RestConnectorNode((RootNode) logicNode, coordForRead);
		else if (logicNode instanceof SufficientSetNode)
			return new RestConnectorNode((SufficientSetNode) logicNode, coordForRead);
		else
			throw new IllegalArgumentException("create() Failed: Unsupported LogicNode " + logicNode.getClass().getName() + " " + logicNode);
	}
}
