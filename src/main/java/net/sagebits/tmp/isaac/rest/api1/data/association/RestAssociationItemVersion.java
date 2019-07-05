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
package net.sagebits.tmp.isaac.rest.api1.data.association;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import net.sagebits.tmp.isaac.rest.ExpandUtil;
import net.sagebits.tmp.isaac.rest.api.data.Expandable;
import net.sagebits.tmp.isaac.rest.api.data.Expandables;
import net.sagebits.tmp.isaac.rest.api.exceptions.RestException;
import net.sagebits.tmp.isaac.rest.api1.RestPaths;
import net.sagebits.tmp.isaac.rest.api1.data.RestIdentifiedObject;
import net.sagebits.tmp.isaac.rest.api1.data.RestStampedVersion;
import net.sagebits.tmp.isaac.rest.api1.data.concept.RestConceptChronology;
import net.sagebits.tmp.isaac.rest.api1.data.semantic.RestSemanticChronology;
import net.sagebits.tmp.isaac.rest.api1.data.semantic.RestSemanticVersion;
import net.sagebits.tmp.isaac.rest.api1.semantic.SemanticAPIs;
import net.sagebits.tmp.isaac.rest.session.RequestInfo;
import sh.isaac.api.Get;
import sh.isaac.api.externalizable.IsaacObjectType;
import sh.isaac.misc.associations.AssociationInstance;

/**
 * {@link RestAssociationItemVersion}
 * Represents an association between two components - essentially, a triplet of Source -> Type -> Target
 *
 * @author <a href="mailto:daniel.armbrust.list@sagebits.net">Dan Armbrust</a>
 */

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE)
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, defaultImpl = RestAssociationItemVersion.class)
public class RestAssociationItemVersion
{
	private static Logger log = LogManager.getLogger();
	/**
	 * The data that was not expanded as part of this call (but can be)
	 */
	@XmlElement
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public Expandables expandables;

	/**
	 * The target item in the association. Typically this is a concept, but it may also be a semantic. Note that
	 * this may be null, in the case where the association intends to represent that no target is available for a particular
	 * association type and source component.
	 */
	@XmlElement
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public RestIdentifiedObject targetId;

	/**
	 * The concept identifiers of the association type
	 */
	@XmlElement
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public RestIdentifiedObject associationType;

	/**
	 * The identifiers of the source item in the association. Typically this is a concept, but it may also be a semantic.
	 */
	@XmlElement
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public RestIdentifiedObject sourceId;

	/**
	 * The semantic identifiers of the semantic that represents this association
	 */
	@XmlElement
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public RestIdentifiedObject identifiers;

	/**
	 * The StampedVersion details for this association entry
	 */
	@XmlElement
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public RestStampedVersion associationItemStamp;

	/**
	 * The Concept Chronology of the concept represented by sourceNid - if the the sourceNid represents a concept. Blank, unless requested via the
	 * expand parameter
	 * 'source' and the nid represents a concept. If 'source' is passed, you can also pass 'versionsAll' or 'versionsLatestOnly'
	 */
	@XmlElement
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public RestConceptChronology sourceConcept;

	/**
	 * The Semantic Chronology of the semantic represented by sourceNid - if the sourceNid represents a semantic. Blank, unless requested via the expand
	 * parameter
	 * 'source' and the nid represents a semantic. If 'source' is passed, you can also pass 'versionsAll', 'versionsLatestOnly', 'nestedSemantics',
	 * 'referencedDetails'
	 */
	@XmlElement
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public RestSemanticChronology sourceSemantic;

	/**
	 * The Concept Chronology of the concept represented by sourceNid. Typically blank, unless requested via the expand parameter
	 * 'target' If 'target' is passed, you can also pass 'versionsAll' or 'versionsLatestOnly'
	 */
	@XmlElement
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public RestConceptChronology targetConcept;

	/**
	 * The Semantic Chronology of the semantic represented by targetNid - if the targetNid represents a semantic. Blank, unless requested via the expand
	 * parameter
	 * 'target' and the nid represents a semantic. If 'target' is passed, you can also pass 'versionsAll', 'versionsLatestOnly', 'nestedSemantics',
	 * 'referencedDetails'
	 */
	@XmlElement
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public RestSemanticChronology targetSemantic;

	/**
	 * The nested semantics (if any) attached to this association. Not populated by default, include expand=nestedSemantics to expand these. When
	 * 'nestedSemantics' is passed,
	 * you can also pass 'referencedDetails' and 'chronology'
	 */
	@XmlElement
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public RestSemanticVersion[] nestedSemantics;

	protected RestAssociationItemVersion()
	{
		// for jaxb
	}

	/**
	 * @param read
	 * @throws RestException
	 */
	public RestAssociationItemVersion(AssociationInstance read) throws RestException
	{
		associationType = new RestIdentifiedObject(read.getAssociationTypeNid(), IsaacObjectType.CONCEPT);
		identifiers = new RestIdentifiedObject(read.getData().getChronology());
		associationItemStamp = new RestStampedVersion(read.getData());
		sourceId = new RestIdentifiedObject(read.getSourceComponent());
		targetId = read.getTargetComponent().isPresent() ? new RestIdentifiedObject(read.getTargetComponent().get()) : null;

		sourceConcept = null;
		sourceSemantic = null;
		targetConcept = null;
		targetSemantic = null;
		nestedSemantics = null;

		if (RequestInfo.get().shouldExpand(ExpandUtil.source) || RequestInfo.get().shouldExpand(ExpandUtil.target)
				|| RequestInfo.get().shouldExpand(ExpandUtil.nestedSemanticsExpandable))
		{
			expandables = new Expandables();
			if (RequestInfo.get().shouldExpand(ExpandUtil.source))
			{
				if (sourceId.type.enumId == IsaacObjectType.CONCEPT.ordinal())
				{
					sourceConcept = new RestConceptChronology(Get.conceptService().getConceptChronology(sourceId.nid),
							false,
							RequestInfo.get().shouldExpand(ExpandUtil.versionsLatestOnlyExpandable), true);
				}
				else if (sourceId.type.enumId == IsaacObjectType.SEMANTIC.ordinal())
				{
					sourceSemantic = new RestSemanticChronology(Get.assemblageService().getSemanticChronology(sourceId.nid),
							false,
							RequestInfo.get().shouldExpand(ExpandUtil.versionsLatestOnlyExpandable),
							RequestInfo.get().shouldExpand(ExpandUtil.nestedSemanticsExpandable), RequestInfo.get().shouldExpand(ExpandUtil.referencedDetails));
				}
				else
				{
					log.error("Unexpected object type for source nid: " + sourceId.nid);
				}
			}

			if (RequestInfo.get().shouldExpand(ExpandUtil.target) && targetId != null)
			{
				if (targetId.type.enumId == IsaacObjectType.CONCEPT.ordinal())
				{
					targetConcept = new RestConceptChronology(Get.conceptService().getConceptChronology(targetId.nid),
							false,
							RequestInfo.get().shouldExpand(ExpandUtil.versionsLatestOnlyExpandable), true);
				}
				else if (targetId.type.enumId == IsaacObjectType.SEMANTIC.ordinal())
				{
					targetSemantic = new RestSemanticChronology(Get.assemblageService().getSemanticChronology(targetId.nid),
							false,
							RequestInfo.get().shouldExpand(ExpandUtil.versionsLatestOnlyExpandable),
							RequestInfo.get().shouldExpand(ExpandUtil.nestedSemanticsExpandable), RequestInfo.get().shouldExpand(ExpandUtil.referencedDetails));
				}
				else
				{
					log.error("Unexpected object type for target nid: " + targetId.nid);
				}
			}

			if (RequestInfo.get().shouldExpand(ExpandUtil.nestedSemanticsExpandable))
			{
				nestedSemantics = SemanticAPIs.get(identifiers.getFirst().toString(), null, null,
						RequestInfo.get().shouldExpand(ExpandUtil.chronologyExpandable), true, RequestInfo.get().shouldExpand(ExpandUtil.referencedDetails),
						false, true, true, true);
			}
			if (expandables.size() == 0)
			{
				expandables = null;
			}
		}
		buildMissingExpandables();
	}

	private void buildMissingExpandables()
	{
		if (RequestInfo.get().returnExpandableLinks())
		{
			if (expandables == null)
			{
				expandables = new Expandables();
			}
			if (!RequestInfo.get().shouldExpand(ExpandUtil.source))
			{
				if (sourceId.type.enumId == IsaacObjectType.CONCEPT.ordinal())
				{
					expandables.add(new Expandable(ExpandUtil.source, RestPaths.conceptChronologyAppPathComponent + sourceId.nid));
				}
				else if (sourceId.type.enumId == IsaacObjectType.SEMANTIC.ordinal())
				{
					expandables.add(new Expandable(ExpandUtil.source, RestPaths.semanticChronologyAppPathComponent + sourceId.nid));
				}
			}
			if (!RequestInfo.get().shouldExpand(ExpandUtil.target) && targetId != null)
			{
				if (targetId.type.enumId == IsaacObjectType.CONCEPT.ordinal())
				{
					expandables.add(new Expandable(ExpandUtil.target, RestPaths.conceptChronologyAppPathComponent + targetId.nid));
				}
				else if (targetId.type.enumId == IsaacObjectType.SEMANTIC.ordinal())
				{
					expandables.add(new Expandable(ExpandUtil.target, RestPaths.semanticChronologyAppPathComponent + targetId.nid));
				}
			}
			if (!RequestInfo.get().shouldExpand(ExpandUtil.nestedSemanticsExpandable))
			{
				expandables.add(new Expandable(ExpandUtil.nestedSemanticsExpandable,
						RestPaths.semanticAPIsPathComponent + RestPaths.forReferencedComponentComponent + identifiers.getFirst().toString()));
			}
		}
	}
}
