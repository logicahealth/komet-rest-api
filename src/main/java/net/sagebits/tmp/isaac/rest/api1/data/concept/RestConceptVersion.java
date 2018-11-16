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

package net.sagebits.tmp.isaac.rest.api1.data.concept;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import net.sagebits.tmp.isaac.rest.ExpandUtil;
import net.sagebits.tmp.isaac.rest.Util;
import net.sagebits.tmp.isaac.rest.api.data.Expandable;
import net.sagebits.tmp.isaac.rest.api.data.Expandables;
import net.sagebits.tmp.isaac.rest.api.exceptions.RestException;
import net.sagebits.tmp.isaac.rest.api1.RestPaths;
import net.sagebits.tmp.isaac.rest.api1.data.RestIdentifiedObject;
import net.sagebits.tmp.isaac.rest.api1.data.RestStampedVersion;
import net.sagebits.tmp.isaac.rest.api1.taxonomy.TaxonomyAPIs;
import net.sagebits.tmp.isaac.rest.session.RequestInfo;
import sh.isaac.MetaData;
import sh.isaac.api.Get;
import sh.isaac.api.TaxonomySnapshot;
import sh.isaac.api.chronicle.LatestVersion;
import sh.isaac.api.chronicle.VersionType;
import sh.isaac.api.collections.NidSet;
import sh.isaac.api.component.concept.ConceptVersion;
import sh.isaac.api.component.semantic.SemanticChronology;
import sh.isaac.api.component.semantic.version.LogicGraphVersion;
import sh.isaac.api.externalizable.IsaacObjectType;
import sh.isaac.utility.Frills;

/**
 * 
 * {@link RestConceptVersion}
 *
 * @author <a href="mailto:daniel.armbrust.list@sagebits.net">Dan Armbrust</a>
 */
@XmlRootElement
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE)
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY)
public class RestConceptVersion implements Comparable<RestConceptVersion>
{
	private transient static Logger log = LogManager.getLogger();

	/**
	 * The data that was not expanded as part of this call (but can be)
	 */
	@XmlElement
	@JsonInclude(JsonInclude.Include.NON_NULL)
	Expandables expandables;

	/**
	 * The concept chronology for this concept. Depending on the expand parameter, may be empty.
	 */
	@XmlElement
	@JsonInclude(JsonInclude.Include.NON_NULL)
	RestConceptChronology conChronology;

	/**
	 * The StampedVersion details for this version of this concept.
	 */
	@XmlElement
	@JsonInclude(JsonInclude.Include.NON_NULL)
	RestStampedVersion conVersion;

	/**
	 * A boolean indicating whether the concept is fully-defined or primitive. true for fully-defined, false for primitive
	 * This value is not populated / returned if the concept does not contain a logic graph from which to derive the information.
	 */
	@XmlElement
	@JsonInclude(JsonInclude.Include.NON_NULL)
	Boolean isConceptDefined;

	/**
	 * The parent concepts(s) of the concept at this point in time ('is a' relationships). Depending on the expand parameter, this may not be
	 * returned.
	 */
	@XmlElement
	@JsonInclude(JsonInclude.Include.NON_NULL)
	List<RestConceptVersion> parents = new ArrayList<>();

	/**
	 * The child concepts(s) of the concept at this point in time ('is a' relationships). Depending on the expand parameter, this may not be returned.
	 * They are stored in the results member of the RestConceptVersionPage object.
	 * If children.results.length < children.paginationData.approximateTotal, then that is an indication that the results set has been truncated due
	 * to pagination (size of children.results likely corresponds to the
	 * specified maxPageSize, unless the page returned includes the last item (final tranche) of the total set).
	 * The children.paginationData object also contains URL suggestions for how to get prior and subsequent tranches of data.
	 * The children.paginationData.totalIsExact should always be set to true.
	 */
	@XmlElement
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public RestConceptVersionPage children;

	/**
	 * The number of child concept(s) of the concept at this point in time ('is a' relationships). Depending on the expand parameter, this may not be
	 * returned.
	 * This will not be returned if the children field is populated.
	 */
	@XmlElement
	@JsonInclude(JsonInclude.Include.NON_NULL)
	Integer childCount;

	/**
	 * The number of parent concept(s) of the concept at this point in time ('is a' relationships). Depending on the expand parameter, this may not be
	 * returned.
	 * This will not be returned if the parents field is populated.
	 */
	@XmlElement
	@JsonInclude(JsonInclude.Include.NON_NULL)
	Integer parentCount;

	/**
	 * The identifiers of the semantic assemblage concepts that this concept is a member of (there exists a semantic instance where the
	 * referencedComponent is this concept, and the assemblage is the value returned). Note that this field is typically not populated - and when it
	 * is populated, it is only in response to a request via the Taxonomy or Concept APIs, when the parameter 'semanticMembership=true' is passed.
	 * See more details on {@link TaxonomyAPIs#getConceptVersionTaxonomy(String, String, int, String, int, String, String, String)}
	 */
	@XmlElement
	@JsonInclude(JsonInclude.Include.NON_NULL)
	RestIdentifiedObject[] semanticMembership;

	/**
	 * The identifiers of the terminologies (concepts that represent terminologies) that this concept is part of. This is determined by whether or not
	 * there is version of this concept present with a module that extends from one of the children of the {@link MetaData#MODULE} concepts.
	 * 
	 * Additionally, this field will include the terminologyType of {@link MetaData#ISAAC_MODULE} if the concept was ever a kind of the metadata
	 * concept tree starting at {@link MetaData#METADATA____SOLOR}
	 * 
	 * Note that this field is typically not populated - and when it is populated, it is only in response to a request via the Taxonomy or Concept
	 * APIs, when the parameter 'terminologyType=true' is passed.
	 * 
	 * Note that this is calculated WITH taking into account the view coordinate, including the active / inactive state of the concept in any
	 * particular terminology. This means that if a concept is present in both Snomed CT and the US Extension modules, but your view coordinate
	 * excludes the US Extension, this will not include the US Extension module.
	 * 
	 * For behavior that ignores stamp, request the same value on the ConceptChronology, instead.
	 * 
	 * See 1/system/terminologyTypes for more details on the potential terminology concepts that will be returned.
	 */
	@XmlElement
	@JsonInclude(JsonInclude.Include.NON_NULL)
	RestIdentifiedObject[] terminologyTypes;

	/**
	 * Exception messages encountered while populating data, including parents and children
	 */
	@XmlElement
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public List<String> exceptionMessages = new ArrayList<>();

	protected RestConceptVersion()
	{
		// for Jaxb
	}

	public RestConceptVersion(ConceptVersion cv, boolean includeChronology, UUID processId)
	{
		this(cv, includeChronology, false, false, false, false, false, false, false, processId);
	}

	public RestConceptVersion(ConceptVersion cv, boolean includeChronology, boolean includeParents, boolean countParents, boolean includeChildren,
			boolean countChildren, boolean stated, boolean includeSemanticMembership, boolean includeTerminologyType, final UUID processId)
	{
		conVersion = new RestStampedVersion(cv);

		Optional<SemanticChronology> semantic = Get.assemblageService()
				.getSemanticChronologyStreamForComponentFromAssemblage(cv.getNid(),
						(RequestInfo.get().getStated() ? RequestInfo.get().getLogicCoordinate().getStatedAssemblageNid()
								: RequestInfo.get().getLogicCoordinate().getInferredAssemblageNid()))
				.findAny();

		if (semantic.isPresent())
		{
			LatestVersion<LogicGraphVersion> sv = semantic.get().getLatestVersion(Util.getPreWorkflowStampCoordinate(processId, semantic.get().getNid()));
			Util.logContradictions(log, sv);
			if (sv.isPresent())
			{
				// TODO handle contradictions
				isConceptDefined = Frills.isConceptFullyDefined(sv.get());
			}
		}

		if (includeSemanticMembership)
		{
			HashSet<Integer> semanticMembershipNids = new HashSet<>();
			Consumer<SemanticChronology> consumer = new Consumer<SemanticChronology>()
			{
				@Override
				public void accept(SemanticChronology sc)
				{
					try
					{
						if (!semanticMembershipNids.contains(sc.getAssemblageNid()) && sc.getVersionType() != VersionType.LOGIC_GRAPH
								&& sc.getVersionType() != VersionType.DESCRIPTION
								&& sc.getLatestVersion(Util.getPreWorkflowStampCoordinate(processId, sc.getNid())).isPresent())
						{
							semanticMembershipNids.add(sc.getAssemblageNid());
						}
					}
					catch (RuntimeException e)
					{
						exceptionMessages.add("Error checking semantic membership " + sc.getPrimordialUuid() + ": " + e.getLocalizedMessage());
						throw e;
					}
				}
			};

			Stream<SemanticChronology> semantics = Get.assemblageService().getSemanticChronologyStreamForComponent(cv.getNid());
			semantics.forEach(consumer);

			semanticMembership = new RestIdentifiedObject[semanticMembershipNids.size()];
			int i = 0;
			for (int nid : semanticMembershipNids)
			{
				try
				{
					semanticMembership[i++] = new RestIdentifiedObject(nid, IsaacObjectType.CONCEPT);
				}
				catch (RuntimeException e)
				{
					exceptionMessages.add("Error creating identified object for concept SEQ=" + nid + ": " + e.getLocalizedMessage());
					throw e;
				}
			}
		}
		else
		{
			semanticMembership = null;
		}

		if (includeTerminologyType)
		{
			HashSet<Integer> terminologyTypeNids = null;
			try
			{
				terminologyTypeNids = Frills.getTerminologyTypes(cv.getChronology(), RequestInfo.get().getStampCoordinate());
			}
			catch (RuntimeException e)
			{
				exceptionMessages.add("Error getting terminology types for concept " + cv.getChronology().getPrimordialUuid() + ": " + e.getLocalizedMessage());
				throw e;
			}

			terminologyTypes = new RestIdentifiedObject[terminologyTypeNids.size()];
			int i = 0;
			for (int nid : terminologyTypeNids)
			{
				terminologyTypes[i++] = new RestIdentifiedObject(nid, IsaacObjectType.CONCEPT);
			}
		}
		else
		{
			terminologyTypes = null;
		}

		if (includeChronology || includeParents || includeChildren || countChildren || countParents)
		{
			expandables = new Expandables();
			if (includeChronology)
			{
				conChronology = new RestConceptChronology(cv.getChronology(), false, false, false, processId);
			}
			else
			{
				conChronology = null;
				if (RequestInfo.get().returnExpandableLinks())
				{
					expandables.add(new Expandable(ExpandUtil.chronologyExpandable, RestPaths.conceptChronologyAppPathComponent + cv.getChronology().getNid()));
				}
			}
			TaxonomySnapshot tree = null;
			if (includeParents || includeChildren || countChildren || countParents)
			{
				try
				{

					tree = Get.taxonomyService().getSnapshotNoTree(RequestInfo.get().getManifoldCoordinate(stated));
				}
				catch (RuntimeException e)
				{
					exceptionMessages.add("Error getting " + (stated ? "stated" : "inferred") + " taxonomy tree: " + e.getLocalizedMessage());
					throw e;
				}
			}

			if (includeParents)
			{
				TaxonomyAPIs.addParents(cv.getChronology().getNid(), this, tree, countParents, 0, includeSemanticMembership, includeTerminologyType,
						new NidSet(), processId);
			}
			else if (countParents)
			{
				TaxonomyAPIs.countParents(cv.getChronology().getNid(), this, tree, processId);
			}

			if (includeChildren)
			{
				try
				{
					TaxonomyAPIs.addChildren(cv.getChronology().getNid(), this, tree, countChildren, countParents, 0, includeSemanticMembership,
							includeTerminologyType, new NidSet(), processId, TaxonomyAPIs.PAGE_NUM_DEFAULT, TaxonomyAPIs.MAX_PAGE_SIZE_DEFAULT);
				}
				catch (RestException e)
				{
					// RestException thrown due to Pagination Shouldn't happen with default values
					throw new RuntimeException(e);
				}
			}
			else if (countChildren)
			{
				TaxonomyAPIs.countChildren(cv.getChronology().getNid(), this, tree, processId);
			}

			if (includeParents || includeChildren)
			{
				sortParentsAndChildren();
			}

			if (expandables.size() == 0)
			{
				expandables = null;
			}
		}
		else
		{
			if (RequestInfo.get().returnExpandableLinks())
			{
				expandables = new Expandables(
						new Expandable(ExpandUtil.chronologyExpandable, RestPaths.conceptChronologyAppPathComponent + cv.getChronology().getNid()));
			}
			else
			{
				expandables = null;
			}
			conChronology = null;
			parents.clear();
			children = null;
		}
	}

	public void addParent(RestConceptVersion parent)
	{
		this.parents.add(parent);
	}

	public void setChildCount(int count)
	{
		this.childCount = count;
	}

	public void setParentCount(int count)
	{
		this.parentCount = count;
	}

	public void sortParentsAndChildren()
	{
		if (parents.size() > 0)
		{
			Collections.sort(parents);
			for (RestConceptVersion rcv : parents)
			{
				rcv.sortParentsAndChildren();
			}
		}
		if (children != null && children.results.length > 1)
		{
			Arrays.sort(children.results);
			for (RestConceptVersion rcv : children.results)
			{
				rcv.sortParentsAndChildren();
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int compareTo(RestConceptVersion o)
	{
		if (this.conChronology != null && o.conChronology != null)
		{
			return this.conChronology.compareTo(o.conChronology);
		}
		return 0;  // not really anything worth sorting on, if no chronology.
	}

	/**
	 * @return conVersion
	 */
	@XmlTransient
	public RestStampedVersion getConVersion()
	{
		return conVersion;
	}

	/**
	 * @return parents
	 */
	@XmlTransient
	public List<RestConceptVersion> getParents()
	{
		return Collections.unmodifiableList(parents);
	}

	/**
	 * @return conChronology
	 */
	@XmlTransient
	public RestConceptChronology getConChronology()
	{
		return conChronology;
	}

	/**
	 * This is an internal method, not part of the over the wire information.
	 * 
	 * @return number of actual children, if present, otherwise, the value of the child count variable
	 */
	@XmlTransient
	public int getChildCount()
	{
		return (children == null || children.results == null || children.results.length == 0 ? (childCount == null ? 0 : childCount) : children.results.length);
	}

	/**
	 * @return the children
	 */
	@XmlTransient
	public RestConceptVersionPage getChildren()
	{
		return children;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString()
	{
		return "RestConceptVersion [conChronology=" + conChronology + ", conVersion=" + conVersion + ", isConceptDefined=" + isConceptDefined + ", parents="
				+ parents + ", children=" + children + ", childCount=" + childCount + ", parentCount=" + parentCount + ", semanticMembership="
				+ Arrays.toString(semanticMembership) + ", terminologyTypes=" + Arrays.toString(terminologyTypes) + "]";
	}
}
