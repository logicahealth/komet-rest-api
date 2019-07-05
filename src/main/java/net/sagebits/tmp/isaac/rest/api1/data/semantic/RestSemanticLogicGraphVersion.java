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

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import net.sagebits.tmp.isaac.rest.api.exceptions.RestException;
import net.sagebits.tmp.isaac.rest.api1.data.RestIdentifiedObject;
import net.sagebits.tmp.isaac.rest.api1.data.logic.RestLogicNode;
import net.sagebits.tmp.isaac.rest.api1.data.logic.RestLogicNodeFactory;
import net.sagebits.tmp.isaac.rest.session.RequestInfo;
import sh.isaac.api.Get;
import sh.isaac.api.component.semantic.version.LogicGraphVersion;
import sh.isaac.api.coordinate.ManifoldCoordinate;
import sh.isaac.api.logic.LogicalExpression;
import sh.isaac.model.coordinate.ManifoldCoordinateImpl;
import sh.isaac.utility.Frills;

/**
 * 
 * {@link RestSemanticLogicGraphVersion}
 *
 * @author <a href="mailto:joel.kniaz.list@gmail.com">Joel Kniaz</a>
 */
@XmlRootElement
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY)
public class RestSemanticLogicGraphVersion extends RestSemanticVersion
{
	private static Logger LOG = LogManager.getLogger();

	/**
	 * The String text of the description of the associated concept
	 */
	@XmlElement
	public String referencedConceptDescription;

	/**
	 * A boolean indicating whether the concept referred to by this
	 * RestSemanticLogicGraphVersion is defined rather than primitive
	 */
	@XmlElement
	public boolean isReferencedConceptDefined;

	/**
	 * The root node of the logical expression tree associated with the concept
	 */
	@XmlElement
	public RestLogicNode rootLogicNode;

	protected RestSemanticLogicGraphVersion()
	{
		// for Jaxb
	}

	/**
	 * @param lgs - A LogicGraphVersion
	 * @param includeChronology - - A boolean value indicating whether or not the RestSemanticLogicGraphVersion should include a populated chronology
	 * @param expandReferenced - 
	 * @param useLatestStampForExpansions true, to use the latest stamp from the request info when populating expansions.  False, to use a stamp
	 *     which is constructed from the fields of the provided sv (this is normally what should be used when populating a list of all versions, so 
	 *     that the expanded items match the point in time from the version being populated
	 * @throws RestException
	 */
	public RestSemanticLogicGraphVersion(LogicGraphVersion lgs, boolean includeChronology, boolean expandReferenced, boolean useLatestStampForExpansions) 
			throws RestException
	{
		super();
		setup(lgs, includeChronology, false, expandReferenced, useLatestStampForExpansions, null);

		referencedConceptDescription = Get.conceptService().getSnapshot(RequestInfo.get().getManifoldCoordinate())
				.conceptDescriptionText(lgs.getReferencedComponentNid());
		LOG.debug("Constructing REST logic graph for {} from LogicalExpression\n{}", new RestIdentifiedObject(lgs.getReferencedComponentNid()).toString(),
				lgs.getLogicalExpression().toString());
		
		ManifoldCoordinate coordForRead = RequestInfo.get().getManifoldCoordinate();
		if (!useLatestStampForExpansions)
		{
			coordForRead = new ManifoldCoordinateImpl(computeVersionStamp(lgs, useLatestStampForExpansions), coordForRead.getLanguageCoordinate());
		}
		
		rootLogicNode = constructRootRestLogicNodeFromLogicGraphVersion(lgs, coordForRead);
		try
		{
			isReferencedConceptDefined = Frills.isConceptFullyDefined(lgs);
		}
		catch (Exception e)
		{
			LOG.warn("Problem getting isConceptDefined value (defaulting to false) for LogicGraphVersion referencing {}",
					new RestIdentifiedObject(lgs.getReferencedComponentNid()).toString());
			isReferencedConceptDefined = false;
		}
	}

	/**
	 * @param lgs
	 *            - A LogicGraphVersion
	 * @return - A RestUntypedConnectorNode with NodeSemantic of DEFINITION_ROOT
	 * 
	 *         Constructs a RestUntypedConnectorNode with NodeSemantic of
	 *         DEFINITION_ROOT which is the root of the logic graph tree
	 */
	private static RestLogicNode constructRootRestLogicNodeFromLogicGraphVersion(LogicGraphVersion lgs, ManifoldCoordinate coordForRead)
	{
		LogicalExpression le = lgs.getLogicalExpression();

		LOG.debug("Processing LogicalExpression for concept {}",
				Get.conceptService().getSnapshot(RequestInfo.get().getManifoldCoordinate()).conceptDescriptionText(le.getConceptBeingDefinedNid()));
		LOG.debug(le.toString());
		LOG.debug("Root is a {}", le.getRoot().getNodeSemantic().name());

		if (le.getNodeCount() > 0)
		{
			LOG.debug("Passed LogicalExpression with {} > 0 nodes", le.getNodeCount());
			for (int i = 0; i < le.getNodeCount(); ++i)
			{
				LOG.debug("{} node #{} of {}: class={}, {}", le.getNode(i).getNodeSemantic(), ((int) i + 1), le.getNodeCount(),
						le.getNode(i).getClass().getName(), le.getNode(i));
			}

			return RestLogicNodeFactory.create(le.getRoot(), coordForRead);
		}
		else
		{ // (le.getNodeCount() <= 0) {
			LOG.warn("Passed LogicalExpression with no children");
			throw new RuntimeException("No children found in LogicalExpression for "
					+ Get.conceptService().getSnapshot(RequestInfo.get().getManifoldCoordinate()).conceptDescriptionText(le.getConceptBeingDefinedNid()) + ": " + lgs);
		}
	}
}
