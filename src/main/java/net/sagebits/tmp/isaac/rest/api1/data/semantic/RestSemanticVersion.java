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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlTransient;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.webcohesion.enunciate.metadata.json.JsonSeeAlso;
import net.sagebits.tmp.isaac.rest.ExpandUtil;
import net.sagebits.tmp.isaac.rest.api.data.Expandable;
import net.sagebits.tmp.isaac.rest.api.data.Expandables;
import net.sagebits.tmp.isaac.rest.api.exceptions.RestException;
import net.sagebits.tmp.isaac.rest.api1.RestPaths;
import net.sagebits.tmp.isaac.rest.api1.data.RestStampedVersion;
import net.sagebits.tmp.isaac.rest.api1.semantic.SemanticAPIs;
import net.sagebits.tmp.isaac.rest.api1.semantic.SemanticAPIs.SemanticVersions;
import net.sagebits.tmp.isaac.rest.session.RequestInfo;
import net.sagebits.tmp.isaac.rest.session.RequestParameters;
import sh.isaac.api.ConceptProxy;
import sh.isaac.api.chronicle.VersionType;
import sh.isaac.api.component.concept.ConceptSpecification;
import sh.isaac.api.component.semantic.version.ComponentNidVersion;
import sh.isaac.api.component.semantic.version.DescriptionVersion;
import sh.isaac.api.component.semantic.version.DynamicVersion;
import sh.isaac.api.component.semantic.version.LogicGraphVersion;
import sh.isaac.api.component.semantic.version.LongVersion;
import sh.isaac.api.component.semantic.version.SemanticVersion;
import sh.isaac.api.component.semantic.version.StringVersion;
import sh.isaac.api.coordinate.StampCoordinate;

/**
 * 
 * {@link RestSemanticVersion}
 * 
 * Note that this is an abstract base class. The actual returned type will be one of the
 * concrete subtype classes, such as {@link RestSemanticDescriptionVersion} or {@link RestDynamicSemanticVersion}
 *
 * @see RestSemanticDescriptionVersion
 * @see RestDynamicSemanticVersion
 * @author <a href="mailto:daniel.armbrust.list@sagebits.net">Dan Armbrust</a>
 */
@XmlSeeAlso({ RestSemanticDescriptionVersion.class, RestDynamicSemanticVersion.class, RestSemanticLogicGraphVersion.class })
@JsonSeeAlso({ RestSemanticDescriptionVersion.class, RestDynamicSemanticVersion.class, RestSemanticLogicGraphVersion.class })
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE)
@XmlRootElement
public abstract class RestSemanticVersion
{
	/**
	 * The data that was not expanded as part of this call (but can be)
	 */
	@XmlElement
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public Expandables expandables;

	/**
	 * The semantic chronology for this concept. Depending on the expand parameter, may be empty.
	 */
	@XmlElement
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public RestSemanticChronology semanticChronology;

	/**
	 * The StampedVersion details for this version of this semantic.
	 */
	@XmlElement
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public RestStampedVersion semanticVersion;

	/**
	 * The nested semantics attached to this semantic. Not populated by default, include expand=nested to expand these.
	 */
	@XmlElement
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public List<RestDynamicSemanticVersion> nestedSemantics = new ArrayList<>();

	protected RestSemanticVersion()
	{
		// For jaxb
	}

	public RestSemanticVersion(SemanticVersion sv, boolean includeChronology, boolean expandNested, boolean expandReferenced,
			boolean useLatestStamp, BiFunction<RestSemanticVersion, StampCoordinate, Boolean> includeInNested) throws RestException
	{
		setup(sv, includeChronology, expandNested, expandReferenced, useLatestStamp, includeInNested);
	}

	private static String getRequestPathForExpandable(SemanticVersion sv)
	{
		switch (sv.getChronology().getVersionType())
		{
			case LOGIC_GRAPH:
				return RestPaths.logicGraphVersionAppPathComponent + sv.getNid();
			case MEMBER:
			case COMPONENT_NID:
			case LONG:
			case STRING:
			case DYNAMIC:
			case DESCRIPTION:
			case UNKNOWN:
			default :
				return RestPaths.semanticVersionAppPathComponent + sv.getNid();
		}
	}

	/**
	 * @param sv
	 * @param includeChronology - true, to populate the semantiChronology aspect of this version
	 * @param expandNested - true, to return nested semantics.  False to skip.
	 * @param expandReferenced - if true, populate a description for the referencedComponent in the included chronology (if includeChronology is true)
	 * @param useLatestStampForExpansion - true, to use the latest stamp from the request info when populating expansions.  False, to use a stamp
	 *     which is constructed from the fields of the provided sv (this is normally what should be used when populating a list of all versions, so 
	 *     that the expanded items match the point in time from the version being populated
	 * @param includeInNested - an optional function that can be used to filter the nested semantics to include
	 * @return the stamp coording used for the setup
	 * @throws RestException
	 */
	protected void setup(SemanticVersion sv, boolean includeChronology, boolean expandNested, boolean expandReferenced,
			boolean useLatestStampForExpansion, BiFunction<RestSemanticVersion, StampCoordinate, Boolean> includeInNested) throws RestException
	{
		semanticVersion = new RestStampedVersion(sv);
		expandables = new Expandables();
		if (includeChronology)
		{
			semanticChronology = new RestSemanticChronology(sv.getChronology(), false, false, false, expandReferenced);
		}
		else
		{
			semanticChronology = null;
			if (RequestInfo.get().returnExpandableLinks())
			{
				expandables.add(new Expandable(ExpandUtil.chronologyExpandable, RestPaths.semanticChronologyAppPathComponent + sv.getChronology().getNid()));
			}
		}

		if (!expandReferenced && RequestInfo.get().returnExpandableLinks())
		{
			// No details on this one to follow, there is no clear URL that would fetch all of the details that this convenience adds
			expandables.add(new Expandable(ExpandUtil.referencedDetails, ""));
		}

		if (expandNested)
		{
			nestedSemantics.clear();
			
			StampCoordinate stampToUse = computeVersionStamp(sv, useLatestStampForExpansion);
			
			// Always include the chronology for nested semantics... otherwise, the user would always have to make a return trip to find out what the
			// nested thing is
			SemanticVersions temp = SemanticAPIs.get(sv.getNid() + "", null, 1, Integer.MAX_VALUE, true, false, stampToUse);
			for (SemanticVersion nestedSv : temp.getValues())
			{
				RestSemanticVersion rsv = RestSemanticVersion.buildRestSemanticVersion(nestedSv, true, true, false, false);
				if (includeInNested == null || includeInNested.apply(rsv, stampToUse))
				{
					// This cast is expected to be safe - we should never nest a DescriptionSemantic under another type of Semantic.
					// In the case where we do have descriptions, the includeInNested function should handle it.
					// Everything else is being treated as a DynamicSemantic
					nestedSemantics.add((RestDynamicSemanticVersion) rsv);
				}
			}
		}
		else
		{
			nestedSemantics.clear();
			if (RequestInfo.get().returnExpandableLinks() && sv.getChronology().getVersionType() != VersionType.LOGIC_GRAPH)
			{
				expandables.add(new Expandable(ExpandUtil.nestedSemanticsExpandable, getRequestPathForExpandable(sv) + "?" + RequestParameters.expand + "="
						+ ExpandUtil.nestedSemanticsExpandable + (includeChronology ? "," + ExpandUtil.chronologyExpandable : "")));
			}
		}

		if (expandables.size() == 0)
		{
			expandables = null;
		}
	}

	/**
	 * @param sv The version of the item being constructed
	 * @param includeChronology - true, to also populate the chonology fields of the version being constructed
	 * @param expandNested - true, to populate nested items like nested semantics
	 * @param expandReferenced - true, to populate if true, populate a description for the referencedComponent
	 * @param useLatestStampForExpansions - true, to use the latest stamp from the request info when populating expansions.  False, to use a stamp
	 *     which is constructed from the fields of the provided sv (this is normally what should be used when populating a list of all versions, so 
	 *     that the expanded items match the point in time from the version being populated)
	 * @return
	 * @throws RestException
	 */
	public static RestSemanticVersion buildRestSemanticVersion(SemanticVersion sv, boolean includeChronology, boolean expandNested, boolean expandReferenced, 
			boolean useLatestStampForExpansions) throws RestException
	{
		switch (sv.getChronology().getVersionType())
		{
			case COMPONENT_NID:
				return new RestDynamicSemanticVersion((ComponentNidVersion) sv, includeChronology, expandNested, expandReferenced, useLatestStampForExpansions);
			case DESCRIPTION:
				return new RestSemanticDescriptionVersion((DescriptionVersion) sv, includeChronology, expandNested, expandReferenced, useLatestStampForExpansions);
			case DYNAMIC:
				return new RestDynamicSemanticVersion((DynamicVersion<?>) sv, includeChronology, expandNested, expandReferenced, useLatestStampForExpansions);
			case LONG:
				return new RestDynamicSemanticVersion((LongVersion) sv, includeChronology, expandNested, expandReferenced, useLatestStampForExpansions);
			case MEMBER:
				return new RestDynamicSemanticVersion((SemanticVersion) sv, includeChronology, expandNested, expandReferenced, useLatestStampForExpansions);
			case STRING:
				return new RestDynamicSemanticVersion((StringVersion) sv, includeChronology, expandNested, expandReferenced, useLatestStampForExpansions);
			case LOGIC_GRAPH:
				return new RestSemanticLogicGraphVersion((LogicGraphVersion) sv, includeChronology, expandReferenced, useLatestStampForExpansions);
			case UNKNOWN:
			default :
				throw new RestException("Semantic Type " + sv.getChronology().getVersionType() + " not currently supported");

		}
	}

	/**
	 * @return the expandables
	 */
	@XmlTransient
	public Expandables getExpandables()
	{
		return expandables;
	}

	/**
	 * @return the semanticChronology
	 */
	@XmlTransient
	public RestSemanticChronology getSemanticChronology()
	{
		return semanticChronology;
	}

	/**
	 * @return the semanticVersion
	 */
	@XmlTransient
	public RestStampedVersion getSemanticVersion()
	{
		return semanticVersion;
	}

	/**
	 * @return the nestedSemantics
	 */
	@XmlTransient
	public List<RestDynamicSemanticVersion> getNestedSemantics()
	{
		return Collections.unmodifiableList(nestedSemantics);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString()
	{
		return "RestSemanticVersion [expandables=" + expandables + ", semanticChronology=" + semanticChronology + ", semanticVersion=" + semanticVersion
				+ ", nestedSemantics=" + nestedSemantics + "]";
	}
	
	protected StampCoordinate computeVersionStamp(SemanticVersion sv, boolean useLatestStampForVersion)
	{
		if (useLatestStampForVersion)
		{
			return RequestInfo.get().getStampCoordinate();
		}
		else
		{
			StampCoordinate stampToUse = RequestInfo.get().getStampCoordinate().makeCoordinateAnalog(sv.getTime());
			//we don't want our nested stuff to be newer than the version being returned.  Also, adjust the module order prefs, so the module
			//for the stamp being processed is first (but don't exclude existing modules)
			ArrayList<ConceptSpecification> orderPrefs = new ArrayList<>();
			orderPrefs.add(new ConceptProxy(sv.getModuleNid()));
			orderPrefs.addAll(stampToUse.getModulePreferenceOrderForVersions());
			return stampToUse.makeModulePreferenceOrderAnalog(orderPrefs);
		}
	}
}
