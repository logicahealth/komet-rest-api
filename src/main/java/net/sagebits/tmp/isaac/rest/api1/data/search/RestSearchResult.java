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

package net.sagebits.tmp.isaac.rest.api1.data.search;

import java.util.Optional;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import net.sagebits.tmp.isaac.rest.ExpandUtil;
import net.sagebits.tmp.isaac.rest.api.data.Expandable;
import net.sagebits.tmp.isaac.rest.api.data.Expandables;
import net.sagebits.tmp.isaac.rest.api1.RestPaths;
import net.sagebits.tmp.isaac.rest.api1.data.RestIdentifiedObject;
import net.sagebits.tmp.isaac.rest.api1.data.concept.RestConceptChronology;
import net.sagebits.tmp.isaac.rest.api1.data.enumerations.IdType;
import net.sagebits.tmp.isaac.rest.session.RequestInfo;
import net.sagebits.tmp.isaac.rest.session.RequestParameters;
import sh.isaac.api.Get;
import sh.isaac.api.Status;
import sh.isaac.api.chronicle.Chronology;
import sh.isaac.utility.Frills;

/**
 * 
 * {@link RestSearchResult}
 *
 * @author <a href="mailto:daniel.armbrust.list@sagebits.net">Dan Armbrust</a>
 */
@XmlRootElement
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE)
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY)
public class RestSearchResult
{
	private static Logger log = LogManager.getLogger();

	/**
	 * The internal identifier of the semantic that matched the query
	 */
	@XmlElement
	Integer matchNid;

	/**
	 * The text of the description that matched the query (may be blank, if the description is not available/active on the path used to populate this)
	 */

	@XmlElement
	String matchText;

	/**
	 * The Lucene Score for this result. This value is only useful for ranking search results relative to other search results within the SAME QUERY
	 * execution. It may not be used to rank one query against another.
	 */
	@XmlElement
	float score;

	/**
	 * Returns true if the semantic that matched the query is active (with the specified of default stamp,
	 * false if inactive.
	 */
	@XmlElement
	boolean active;

	/**
	 * The data that was not expanded as part of this call (but can be)
	 */
	@XmlElement
	Expandables expandables;

	/**
	 * The (optionally) populated identifiers (UUIDs or nid) of the semantic that matched the query. Must pass expand='uuid' to have
	 * this populated. When populated, the nid here is identical to matchNid - but this is significantly more expensive to populate than matchNid
	 * (because that comes directly from the search index) - hence, this field is only optionally populated.
	 */
	@XmlElement
	RestIdentifiedObject identifiers;

	/**
	 * The (optionally) populated concept that is referenced (indirectly) by the semantic that matched the query. This is calculated by
	 * looking up the semantic of the matchNid, and then getting the referenced component of that semantic. If the referenced component
	 * is a concept, that is the concept that is returned. If the referenced component is a semantic, then the process is repeated (following
	 * the referenced component reference of the semantic) - continuing until a concept is found. If an (unusual) case occurs where the
	 * semantic chain doesn't lead to a concept, this will not be populated. This is populated by passing the expand parameter 'referencedConcept'.
	 * If this is passed, you may also (optionally) pass the parameters 'versionsLatestOnly' or 'versionsAll'
	 */
	@XmlElement
	RestConceptChronology referencedConcept;

	protected RestSearchResult()
	{
		// for Jaxb
	}

	/**
	 * 
	 * @param matchNid
	 * @param matchText
	 * @param score
	 * @param state
	 * @param conceptNid - optional
	 */
	public RestSearchResult(int matchNid, String matchText, float score, Status state, Integer conceptNid)
	{
		this.matchNid = matchNid;
		this.matchText = matchText;
		this.score = score;
		this.active = (state == Status.ACTIVE);

		expandables = new Expandables();
		if (RequestInfo.get().shouldExpand(ExpandUtil.uuid))
		{
			Optional<? extends Chronology> object = Get.identifiedObjectService().getChronology(matchNid);
			if (object.isPresent())
			{
				identifiers = new RestIdentifiedObject(object.get());
			}
			else
			{
				log.warn("Couldn't identify UUID for matchNid " + matchNid);
			}
		}
		else
		{
			identifiers = null;
			if (RequestInfo.get().returnExpandableLinks())
			{
				expandables.add(new Expandable(ExpandUtil.uuid, RestPaths.idAppPathComponent + RestPaths.idTranslateComponent + matchNid + "?inputType="
						+ IdType.NID.getDisplayName() + "&outputType=" + IdType.UUID.getDisplayName()));
			}
		}

		if (RequestInfo.get().shouldExpand(ExpandUtil.referencedConcept))
		{
			if (conceptNid == null)
			{
				conceptNid = Frills.getNearestConcept(matchNid).orElse(0);
			}
			if (conceptNid != 0)
			{
				referencedConcept = new RestConceptChronology(Get.conceptService().getConceptChronology(conceptNid),
						RequestInfo.get().shouldExpand(ExpandUtil.versionsAllExpandable),
						RequestInfo.get().shouldExpand(ExpandUtil.versionsLatestOnlyExpandable), true, null); // TODO possibly handle processId
				if (RequestInfo.get().returnExpandableLinks())
				{
					if (!RequestInfo.get().shouldExpand(ExpandUtil.versionsAllExpandable))
					{
						expandables.add(new Expandable(ExpandUtil.versionsAllExpandable,
								RestPaths.conceptVersionAppPathComponent + conceptNid + "?expand=" + ExpandUtil.versionsAllExpandable));
					}
					if (!RequestInfo.get().shouldExpand(ExpandUtil.versionsLatestOnlyExpandable))
					{
						expandables.add(new Expandable(ExpandUtil.versionsLatestOnlyExpandable,
								RestPaths.conceptVersionAppPathComponent + conceptNid + "?expand=" + ExpandUtil.versionsLatestOnlyExpandable + "&"
										+ RequestParameters.coordToken + "=" + RequestInfo.get().getCoordinatesToken().getSerialized()));
					}
				}
			}
		}
		else
		{
			referencedConcept = null;
			if (RequestInfo.get().returnExpandableLinks())
			{
				// This is expensive to calculate, not going to support it as a convenience at this time.
				expandables.add(new Expandable(ExpandUtil.referencedConcept, ""));
				// two other variations (that depend on this)
				expandables.add(new Expandable(ExpandUtil.versionsLatestOnlyExpandable, ""));
				expandables.add(new Expandable(ExpandUtil.versionsAllExpandable, ""));
			}
		}

		if (expandables.size() == 0)
		{
			expandables = null;
		}
	}

	/**
	 * @return the matchNid
	 */
	@XmlTransient
	public Integer getMatchNid()
	{
		return matchNid;
	}

	/**
	 * @return the matchText
	 */
	@XmlTransient
	public String getMatchText()
	{
		return matchText;
	}

	/**
	 * @return the score
	 */
	@XmlTransient
	public float getScore()
	{
		return score;
	}

	/**
	 * @return the active
	 */
	@XmlTransient
	public boolean isActive()
	{
		return active;
	}

	/**
	 * @return the identifiers
	 */
	@XmlTransient
	public RestIdentifiedObject getIdentifiers()
	{
		return identifiers;
	}

	/**
	 * @return the referencedConcept
	 */
	@XmlTransient
	public RestConceptChronology getReferencedConcept()
	{
		return referencedConcept;
	}
}
