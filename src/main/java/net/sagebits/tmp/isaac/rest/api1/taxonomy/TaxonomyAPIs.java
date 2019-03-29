/**
 * Copyright Notice
 *
 * This is a work of the U.S. Government and is not subject to copyright
 * protection in the United States. Foreign copyrights may apply.
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
 */
package net.sagebits.tmp.isaac.rest.api1.taxonomy;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.sagebits.tmp.isaac.rest.ExpandUtil;
import net.sagebits.tmp.isaac.rest.Util;
import net.sagebits.tmp.isaac.rest.api.exceptions.RestException;
import net.sagebits.tmp.isaac.rest.api1.RestPaths;
import net.sagebits.tmp.isaac.rest.api1.concept.ConceptAPIs;
import net.sagebits.tmp.isaac.rest.api1.data.RestIdentifiedObject;
import net.sagebits.tmp.isaac.rest.api1.data.concept.RestConceptVersion;
import net.sagebits.tmp.isaac.rest.api1.data.concept.RestConceptVersionPage;
import net.sagebits.tmp.isaac.rest.session.RequestInfo;
import net.sagebits.tmp.isaac.rest.session.RequestParameters;
import net.sagebits.uts.auth.data.UserRole.SystemRoleConstants;
import sh.isaac.MetaData;
import sh.isaac.api.Get;
import sh.isaac.api.TaxonomySnapshot;
import sh.isaac.api.chronicle.LatestVersion;
import sh.isaac.api.collections.NidSet;
import sh.isaac.api.component.concept.ConceptChronology;
import sh.isaac.api.component.concept.ConceptVersion;

/**
 * {@link TaxonomyAPIs}
 *
 * @author <a href="mailto:daniel.armbrust.list@sagebits.net">Dan Armbrust</a>
 */

@Path(RestPaths.taxonomyAPIsPathComponent)
@RolesAllowed({ SystemRoleConstants.AUTOMATED, SystemRoleConstants.ADMINISTRATOR, SystemRoleConstants.SYSTEM_MANAGER, SystemRoleConstants.CONTENT_MANAGER,
	SystemRoleConstants.EDITOR, SystemRoleConstants.READ })
public class TaxonomyAPIs
{
	private static Logger log = LogManager.getLogger(TaxonomyAPIs.class);

	public final static int MAX_PAGE_SIZE_DEFAULT = 5000;
	public final static int PAGE_NUM_DEFAULT = 1;

	@Context
	private SecurityContext securityContext;

	/**
	 * Returns a single version of a concept, with parents and children expanded to the specified extent and levels.
	 * If no version parameter is specified, returns the latest version.
	 * Pagination parameters may restrict which children are returned, but only effect the direct children of the specified concept,
	 * and are ignored (defaulted) during populating children of children and descendants.
	 * 
	 * When Parents and Children are returned, the order of the parents and children is alphabetical, HOWEVER if there are a large number
	 * of children - such that you only get back one page of children - only the returned page of children will be sorted. The sort will
	 * NOT be correct across multiple pages. Each page will be sorted independently. If the end user needs to display all children, sorted,
	 * they will have to fetch all pages, and then sort.
	 * 
	 * @param id - A UUID or nid of a concept to center this taxonomy lookup on. If not provided, the default value
	 *            is the UUID for the ISAAC_ROOT concept.
	 * @param parentHeight - How far to walk up (expand) the parent tree.  When the parentHeight is > 0, one level of parent will be expanded
	 *            for each child returned as well, if childDepth is > 0.
	 * @param countParents - true to count the number of parents above this node. May be used with or without the parentHeight parameter
	 *            - it works independently. When used in combination with the parentHeight parameter, only the last level of items returned will
	 *            return parent counts. This parameter also applies to the expanded children - if childDepth is requested, and countParents is set,
	 *            this will return a count of parents of each child, which can be used to determine if a child has multiple parents.
	 * @param childDepth - How far to walk down (expand) the tree
	 * @param countChildren - true to count the number of children below this node. May be used with or without the childDepth parameter
	 *            - it works independently. When used in combination with the childDepth parameter, only the last level of items returned will return
	 *            child counts.
	 * @param semanticMembership - when true, the semanticMembership field of the RestConceptVersion object will be populated with the set of unique
	 *            concept nids that describe semantics that this concept is referenced by. (there exists a semantic instance where the
	 *            referencedComponent is the RestConceptVersion being returned here, then the value of the assemblage is also included in the
	 *            RestConceptVersion).
	 *            This will not include the membership information for any assemblage of type logic graph or descriptions.
	 * @param terminologyType - when true, the concept nids of the terminologies that this concept is part of on any stamp is returned. This
	 *            is determined by whether or not there is version of this concept present with a module that extends from one of the children of the
	 *            {@link MetaData#MODULE____SOLOR} concepts. This is returned as a set, as a concept may exist in multiple terminologies at the same
	 *            time.
	 * @param expand - comma separated list of fields to expand. Supports 'chronology'.
	 * @param processId 
	 * @param coordToken specifies an explicit serialized CoordinatesToken string specifying all coordinate parameters. A CoordinatesToken may be
	 *            obtained by a separate (prior) call to getCoordinatesToken().
	 * @param pageNum The pagination page number >= 1 to return
	 * @param maxPageSize The maximum number of results to return per page, must be greater than 0, defaults to (MAX_PAGE_SIZE_DEFAULT==5000)
	 * @param altId - (optional) the altId type(s) to populate in any returned RestIdentifiedObject structures.  By default, no alternate IDs are 
	 *     returned.  This can be set to one or more names or ids from the /1/id/types or the value 'ANY'.  Requesting IDs that are unneeded will harm 
	 *     performance. 
	 * @return the concept version object
	 * @throws RestException
	 */
	@GET
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Path(RestPaths.versionComponent)
	public RestConceptVersion getConceptVersionTaxonomy(
			// ISAAC_Root - any variable ref here breaks the compiler and/or enunciate
			@QueryParam(RequestParameters.id) @DefaultValue(RequestParameters.ISAAC_ROOT_UUID) String id,
			@QueryParam(RequestParameters.parentHeight) @DefaultValue("0") int parentHeight,
			@QueryParam(RequestParameters.countParents) @DefaultValue("false") String countParents,
			@QueryParam(RequestParameters.childDepth) @DefaultValue("1") int childDepth,
			@QueryParam(RequestParameters.countChildren) @DefaultValue("false") String countChildren,
			@QueryParam(RequestParameters.semanticMembership) @DefaultValue("false") String semanticMembership,
			@QueryParam(RequestParameters.terminologyType) @DefaultValue("false") String terminologyType, @QueryParam(RequestParameters.expand) String expand,
			@QueryParam(RequestParameters.processId) String processId, @QueryParam(RequestParameters.coordToken) String coordToken,
			@QueryParam(RequestParameters.pageNum) @DefaultValue(PAGE_NUM_DEFAULT + "") int pageNum,
			@QueryParam(RequestParameters.maxPageSize) @DefaultValue(MAX_PAGE_SIZE_DEFAULT + "") int maxPageSize,
			@QueryParam(RequestParameters.altId) String altId) throws RestException
	{
		RequestParameters.validateParameterNamesAgainstSupportedNames(RequestInfo.get().getParameters(), RequestParameters.id, RequestParameters.parentHeight,
				RequestParameters.countParents, RequestParameters.childDepth, RequestParameters.countChildren, RequestParameters.semanticMembership,
				RequestParameters.terminologyType, RequestParameters.expand, RequestParameters.processId, RequestParameters.COORDINATE_PARAM_NAMES,
				RequestParameters.PAGINATION_PARAM_NAMES, RequestParameters.altId);

		boolean countChildrenBoolean = Boolean.parseBoolean(countChildren.trim());
		boolean countParentsBoolean = Boolean.parseBoolean(countParents.trim());
		boolean includeSemanticMembership = Boolean.parseBoolean(semanticMembership.trim());
		boolean includeTerminologyType = Boolean.parseBoolean(terminologyType.trim());

		UUID processIdUUID = Util.validateWorkflowProcess(processId);

		ConceptChronology concept = ConceptAPIs.findConceptChronology(id);
		LatestVersion<ConceptVersion> cv = concept.getLatestVersion(Util.getPreWorkflowStampCoordinate(processId, concept.getNid()));
		Util.logContradictions(log, cv);
		if (cv.isPresent())
		{
			// parent / child expansion is handled here by providing a depth, not with expandables.
			// TODO handle contradictions
			RestConceptVersion rcv = new RestConceptVersion(cv.get(), RequestInfo.get().shouldExpand(ExpandUtil.chronologyExpandable), false, false, false,
					false, RequestInfo.get().getStated(), includeSemanticMembership, includeTerminologyType, processIdUUID);

			TaxonomySnapshot tree = Get.taxonomyService().getSnapshotNoTree(RequestInfo.get().getManifoldCoordinate());

			if (parentHeight > 0)
			{
				addParents(concept.getNid(), rcv, tree, countParentsBoolean, parentHeight - 1, includeSemanticMembership, includeTerminologyType, new NidSet(),
						processIdUUID);
			}
			else if (countParentsBoolean)
			{
				countParents(concept.getNid(), rcv, tree, processIdUUID);
			}

			if (childDepth > 0)
			{
				//If parent height of more than 1 was requested, populate the direct parents for each child.
				addChildren(concept.getNid(), rcv, tree, countChildrenBoolean, parentHeight > 0, countParentsBoolean, childDepth - 1, includeSemanticMembership,
						includeTerminologyType, new NidSet(), processIdUUID, pageNum, maxPageSize);
			}
			else if (countChildrenBoolean)
			{
				countChildren(concept.getNid(), rcv, tree, processIdUUID);
			}
			rcv.sortParentsAndChildren();
			return rcv;
		}
		throw new RestException(RequestParameters.id, id, "No concept was found");
	}

	/**
	 * @param conceptNid
	 * @param rcv 
	 * @param tree
	 * @param countLeafChildren
	 * @param populateParents
	 * @param countParents
	 * @param remainingChildDepth
	 * @param includeSemanticMembership
	 * @param includeTerminologyType
	 * @param alreadyAddedChildren
	 * @param processId
	 * @param pageNum > 0
	 * @param maxPageSize > 0
	 * @throws RestException 
	 */
	public static void addChildren(int conceptNid, RestConceptVersion rcv, TaxonomySnapshot tree, boolean countLeafChildren, boolean populateParents, 
			boolean countParents, int remainingChildDepth, boolean includeSemanticMembership, boolean includeTerminologyType, NidSet alreadyAddedChildren, 
			UUID processId, int pageNum, // PAGE_NUM_DEFAULT == 1 
			int maxPageSize) // MAX_PAGE_SIZE_DEFAULT == 5000
			throws RestException
	{
		if (pageNum < 1)
		{
			// Bad pageNum parameter value
			throw new RestException(RequestParameters.pageNum, pageNum + "", "pageNum (" + pageNum + ") should be >= 1");
		}
		if (maxPageSize < 1)
		{
			// Bad maxPageSize parameter value
			throw new RestException(RequestParameters.maxPageSize, maxPageSize + "", "maxPageSize (" + maxPageSize + ") should be >= 1");
		}

		if (alreadyAddedChildren.contains(conceptNid))
		{
			// Avoiding infinite loop
			log.warn("addChildren(" + conceptNid + ") aborted potential infinite recursion");
			return;
		}
		else
		{
			alreadyAddedChildren.add(conceptNid);
		}

		int childCount = 0;
		final int first = pageNum == 1 ? 0 : ((pageNum - 1) * maxPageSize + 1);
		final int last = pageNum * maxPageSize;
		final int[] totalChildrenNids = tree.getTaxonomyChildConceptNids(conceptNid);
		List<RestConceptVersion> children = new ArrayList<>();
		for (int childNid : totalChildrenNids)
		{
			childCount++;
			if (childCount < first)
			{
				// Ignore unrequested pages prior to requested page
				continue;
			}
			else if (childCount > last)
			{
				// Ignore unrequested pages subsequent to requested page
				break;
			}

			ConceptChronology childConcept = null;
			try
			{
				childConcept = ConceptAPIs.findConceptChronology(childNid + "");
			}
			catch (RestException e)
			{
				log.error("Failed finding concept for child concept SEQ=" + childNid + " of parent concept " + new RestIdentifiedObject(conceptNid)
						+ ". Not including child.", e);
				rcv.exceptionMessages
						.add("Error adding child concept SEQ=" + childNid + " of parent concept SEQ=" + conceptNid + ": " + e.getLocalizedMessage());
			}
			if (childConcept != null)
			{
				try
				{
					LatestVersion<ConceptVersion> cv = childConcept.getLatestVersion(Util.getPreWorkflowStampCoordinate(processId, childConcept.getNid()));
					Util.logContradictions(log, cv);
					if (cv.isPresent())
					{
						// expand chronology of child even if unrequested, otherwise, you can't identify what the child is
						// TODO handle contradictions
						RestConceptVersion childVersion = new RestConceptVersion(cv.get(), true, populateParents, countParents, false, false,
								RequestInfo.get().getStated(), includeSemanticMembership, includeTerminologyType, processId);
						children.add(childVersion);
						if (remainingChildDepth > 0)
						{
							addChildren(childConcept.getNid(), childVersion, tree, countLeafChildren, populateParents, countParents, remainingChildDepth - 1,
									includeSemanticMembership, includeTerminologyType, alreadyAddedChildren, processId, 1, MAX_PAGE_SIZE_DEFAULT);
						}
						else if (countLeafChildren)
						{
							countChildren(childConcept.getNid(), childVersion, tree, processId);
						}
					}
				}
				catch (RestException | RuntimeException e)
				{
					rcv.exceptionMessages.add("Error adding child concept " + childConcept.getPrimordialUuid() + " of parent concept SEQ=" + conceptNid
							+ ": " + e.getLocalizedMessage());
					throw e;
				}
			}
		}

		final String baseUrl = RestPaths.taxonomyAPIsPathComponent + RestPaths.versionComponent + "?" + RequestParameters.id + "=" + conceptNid;

		rcv.children = new RestConceptVersionPage(pageNum, maxPageSize, totalChildrenNids.length, true, (last - first) < totalChildrenNids.length, baseUrl,
				children.toArray(new RestConceptVersion[children.size()]));
	}

	public static void countParents(int conceptNid, RestConceptVersion rcv, TaxonomySnapshot tree, UUID processId)
	{
		int count = 0;
		for (int parentNid : tree.getTaxonomyParentConceptNids(conceptNid))
		{
			ConceptChronology parentConcept = null;
			try
			{
				parentConcept = ConceptAPIs.findConceptChronology(parentNid + "");
			}
			catch (RestException e)
			{
				log.error("Unexpected error reading parent concept " + parentNid + " of child concept " + new RestIdentifiedObject(conceptNid)
						+ ". Will not be included in count!", e);
				rcv.exceptionMessages
						.add("Error counting parent concept SEQ=" + parentNid + " of child concept SEQ=" + conceptNid + ": " + e.getLocalizedMessage());
			}

			if (parentConcept != null)
			{
				try
				{
					LatestVersion<ConceptVersion> cv = parentConcept.getLatestVersion(Util.getPreWorkflowStampCoordinate(processId, parentConcept.getNid()));
					Util.logContradictions(log, cv);
					if (cv.isPresent())
					{
						count++;
					}
				}
				catch (Exception e)
				{
					log.error("Unexpected error reading latest version of parent concept " + new RestIdentifiedObject(parentNid) + " of child concept "
							+ new RestIdentifiedObject(conceptNid) + ". Will not be included in count!", e);
					rcv.exceptionMessages.add("Error counting latest version of parent concept SEQ=" + parentNid + " of child concept SEQ=" + conceptNid
							+ ": " + e.getLocalizedMessage());
				}
			}
		}
		rcv.setParentCount(count);
	}

	public static void countChildren(int conceptNid, RestConceptVersion rcv, TaxonomySnapshot tree, UUID processId)
	{
		int count = 0;
		for (int childNid : tree.getTaxonomyChildConceptNids(conceptNid))
		{
			ConceptChronology childConcept = null;
			try
			{
				childConcept = ConceptAPIs.findConceptChronology(childNid + "");
			}
			catch (Exception e)
			{
				log.error("Failed finding concept for child concept SEQ=" + childNid + " of parent concept " + new RestIdentifiedObject(conceptNid)
						+ ". Not including child in count.", e);
				rcv.exceptionMessages
						.add("Error counting child concept SEQ=" + childNid + " of parent concept SEQ=" + conceptNid + ": " + e.getLocalizedMessage());
			}

			if (childConcept != null)
			{
				try
				{
					LatestVersion<ConceptVersion> cv = childConcept.getLatestVersion(Util.getPreWorkflowStampCoordinate(processId, childConcept.getNid()));
					Util.logContradictions(log, cv);
					if (cv.isPresent())
					{
						count++;
					}
				}
				catch (Exception e)
				{
					log.error("Failed finding latest version of child concept " + new RestIdentifiedObject(childNid) + " of parent concept "
							+ new RestIdentifiedObject(conceptNid) + ". Not including child in count.", e);
					rcv.exceptionMessages.add("Error counting latest version of child concept SEQ=" + childNid + " of parent concept SEQ=" + conceptNid
							+ ": " + e.getLocalizedMessage());
				}
			}
		}
		rcv.setChildCount(count);
	}

	public static void addParents(int conceptNid, RestConceptVersion rcv, TaxonomySnapshot tree, boolean countLeafParents, int remainingParentDepth,
			boolean includeSemanticMembership, boolean includeTerminologyType, NidSet handledConcepts, UUID processId)
	{
		if (handledConcepts.contains(conceptNid))
		{
			// Avoiding infinite loop
			String msg = "addParents(" + conceptNid + ") aborted potential infinite recursion";
			log.warn(msg);
			return;
		}
		else if (tree.getTaxonomyParentConceptNids(conceptNid).length == 0)
		{
			// If no parents, just add self
			handledConcepts.add(conceptNid);
		}
		else
		{
			NidSet passedHandledConcepts = new NidSet();
			passedHandledConcepts.addAll(handledConcepts.stream());

			for (int parentNid : tree.getTaxonomyParentConceptNids(conceptNid))
			{
				// create a new perParentHandledConcepts for each parent
				NidSet perParentHandledConcepts = new NidSet();
				perParentHandledConcepts.add(conceptNid);
				perParentHandledConcepts.addAll(passedHandledConcepts.stream());

				ConceptChronology parentConceptChronlogy = null;
				try
				{
					parentConceptChronlogy = ConceptAPIs.findConceptChronology(parentNid + "");
				}
				catch (Exception e)
				{
					log.error("Unexpected error reading parent concept " + parentNid + " of child concept " + new RestIdentifiedObject(conceptNid)
							+ ". Will not be included in result!", e);
					rcv.exceptionMessages
							.add("Error reading parent concept SEQ=" + parentNid + " of child concept SEQ=" + conceptNid + ": " + e.getLocalizedMessage());
				}

				// if error is caught above parentConceptChronlogy will be null and not usable in the block below
				if (parentConceptChronlogy != null)
				{
					try
					{
						LatestVersion<ConceptVersion> cv = parentConceptChronlogy
								.getLatestVersion(Util.getPreWorkflowStampCoordinate(processId, parentConceptChronlogy.getNid()));
						Util.logContradictions(log, cv);

						if (cv.isPresent())
						{
							// expand chronology of the parent even if unrequested, otherwise, you can't identify what the child is
							// TODO handle contradictions
							RestConceptVersion parentVersion = new RestConceptVersion(cv.get(), true, false, false, false, false, RequestInfo.get().getStated(),
									includeSemanticMembership, includeTerminologyType, processId);
							rcv.addParent(parentVersion);
							if (remainingParentDepth > 0)
							{
								addParents(parentConceptChronlogy.getNid(), parentVersion, tree, countLeafParents, remainingParentDepth - 1,
										includeSemanticMembership, includeTerminologyType, perParentHandledConcepts, processId);
							}
							else if (countLeafParents)
							{
								countParents(parentConceptChronlogy.getNid(), parentVersion, tree, processId);
							}
						}
					}
					catch (Exception e)
					{
						log.error("Unexpected error processing parent concept " + new RestIdentifiedObject(parentNid) + " of child concept "
								+ new RestIdentifiedObject(conceptNid) + ". May not be included in result!", e);
						rcv.exceptionMessages.add(
								"Error reading parent concept SEQ=" + parentNid + " of child concept SEQ=" + conceptNid + ": " + e.getLocalizedMessage());
					}
				}
				// Add perParentHandledConcepts concepts back to handledConcepts
				handledConcepts.addAll(perParentHandledConcepts.stream());
			}
		}
	}
}
