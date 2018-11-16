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

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import sh.isaac.model.logic.node.external.RoleNodeAllWithUuids;
import sh.isaac.model.logic.node.external.RoleNodeSomeWithUuids;
import sh.isaac.model.logic.node.internal.RoleNodeAllWithNids;
import sh.isaac.model.logic.node.internal.RoleNodeSomeWithNids;

/**
 * 
 * {@link RestRoleNode}
 *
 * @author <a href="mailto:joel.kniaz.list@gmail.com">Joel Kniaz</a>
 *
 *         RestRoleNode is a RestTypedConnectorNode corresponding to either RoleNodeSome or RoleNodeAll.
 * 
 *         RestFeatureNode must have exactly 1 child node.
 * 
 *         RestRoleNode for RoleNodeSome has RestNodeSemanticType == NodeSemantic.ROLE_SOME
 *         RestRoleNode for RoleNodeAll has RestNodeSemanticType == NodeSemantic.ROLE_ALL
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY)
public class RestRoleNode extends RestTypedConnectorNode
{
	protected RestRoleNode()
	{
		// FOR JAXB
	}

	/**
	 * @param roleNodeSomeWithNids
	 */
	public RestRoleNode(RoleNodeSomeWithNids roleNodeSomeWithNids)
	{
		super(roleNodeSomeWithNids);
	}

	/**
	 * @param roleNodeSomeWithUuids
	 */
	public RestRoleNode(RoleNodeSomeWithUuids roleNodeSomeWithUuids)
	{
		super(roleNodeSomeWithUuids);
	}

	/**
	 * @param roleNodeAllWithNids
	 */
	public RestRoleNode(RoleNodeAllWithNids roleNodeAllWithNids)
	{
		super(roleNodeAllWithNids);
	}

	/**
	 * @param roleNodeAllWithUuids
	 */
	public RestRoleNode(RoleNodeAllWithUuids roleNodeAllWithUuids)
	{
		super(roleNodeAllWithUuids);
	}
}
