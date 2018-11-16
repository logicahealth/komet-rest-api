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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
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
import net.sagebits.tmp.isaac.rest.api1.RestPaths;
import net.sagebits.tmp.isaac.rest.api1.data.RestIdentifiedObject;
import net.sagebits.tmp.isaac.rest.session.RequestInfo;
import net.sagebits.tmp.isaac.rest.session.RequestParameters;
import sh.isaac.MetaData;
import sh.isaac.api.chronicle.LatestVersion;
import sh.isaac.api.component.concept.ConceptChronology;
import sh.isaac.api.component.concept.ConceptVersion;
import sh.isaac.api.coordinate.LanguageCoordinate;
import sh.isaac.api.externalizable.IsaacObjectType;
import sh.isaac.api.util.AlphanumComparator;
import sh.isaac.utility.Frills;

/**
 * 
 * {@link RestConceptChronology}
 *
 * @author <a href="mailto:daniel.armbrust.list@sagebits.net">Dan Armbrust</a>
 */
@XmlRootElement
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE)
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY)
public class RestConceptChronology implements Comparable<RestConceptChronology>
{
	private transient static Logger log = LogManager.getLogger();
	/**
	 * The "best" description for this concept. This is selected based on the attributes within the session for
	 * stamp and language coordinates - or - if none present - the server default.
	 */
	@XmlElement
	String description;

	/**
	 * The data that was not expanded as part of this call (but can be)
	 */
	@XmlElement
	Expandables expandables;

	/**
	 * The identifiers for the concept
	 */
	@XmlElement
	RestIdentifiedObject identifiers;

	/**
	 * The list of concept versions. Depending on the expand parameter, may be empty, the latest only, or all versions.
	 */
	@XmlElement
	List<RestConceptVersion> versions = new ArrayList<>();

	/**
	 * The identifiers of the terminologies (concepts that represent terminologies) that this concept is part of. This is determined by whether or not
	 * there is version of this concept present with a module that extends from one of the children of the {@link MetaData#MODULE____SOLOR} concepts.
	 * 
	 * Additionally, this field will include the terminologyType of {@link MetaData#ISAAC_MODULE} if the concept is a kind of the metadata concept
	 * tree starting at {@link MetaData#METADATA____SOLOR}
	 * 
	 * Note that this field is typically not populated - and when it is populated, it is only in response to a request via the Taxonomy or Concept
	 * APIs, when the parameter 'terminologyType=true' is passed.
	 * 
	 * Note that this is calculated WITHOUT taking into account the view coordinate, or the active / inactive state of the concept in any particular
	 * terminology.
	 * 
	 * See 1/system/terminologyTypes for more details on the potential terminology concepts that will be returned.
	 */
	@XmlElement
	@JsonInclude(JsonInclude.Include.NON_NULL)
	RestIdentifiedObject[] terminologyTypes;

	protected RestConceptChronology()
	{
		// for JaxB
	}

	public RestConceptChronology(ConceptChronology cc, boolean includeAllVersions, boolean includeLatestVersion, boolean includeTerminologyType, UUID processId)
	{
		this(cc, includeAllVersions, includeLatestVersion, includeTerminologyType, processId, RequestInfo.get().getLanguageCoordinate());
	}

	public RestConceptChronology(ConceptChronology cc, boolean includeAllVersions, boolean includeLatestVersion, boolean includeTerminologyType, UUID processId,
			LanguageCoordinate descriptionLanguageCoordinate)
	{
		identifiers = new RestIdentifiedObject(cc);

		description = Util.readBestDescription(cc.getNid(), RequestInfo.get().getStampCoordinate(), descriptionLanguageCoordinate);

		if (includeTerminologyType)
		{
			HashSet<Integer> terminologyTypeNids = Frills.getTerminologyTypes(cc, RequestInfo.get().getStampCoordinate());

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

		if (includeAllVersions || includeLatestVersion)
		{
			expandables = null;
			if (includeAllVersions)
			{
				List<ConceptVersion> cvl = cc.getVersionList();
				for (ConceptVersion cv : cvl)
				{
					versions.add(new RestConceptVersion(cv, false, false, false, false, false, false, false, false, processId));
				}
			}
			else // if (includeLatestVersion)
			{
				LatestVersion<ConceptVersion> latest = cc.getLatestVersion(Util.getPreWorkflowStampCoordinate(processId, cc.getNid()));
				Util.logContradictions(log, latest);

				if (latest.isPresent())
				{
					// TODO handle contradictions
					versions.add(new RestConceptVersion(latest.get(), false, false, false, false, false, false, false, false, processId));
				}
			}
		}
		else
		{
			versions.clear();
			if (RequestInfo.get().returnExpandableLinks())
			{
				expandables = new Expandables(new Expandable(ExpandUtil.versionsAllExpandable, RestPaths.conceptVersionsAppPathComponent + cc.getNid() + "/"),
						new Expandable(ExpandUtil.versionsLatestOnlyExpandable, RestPaths.conceptVersionAppPathComponent + cc.getNid() + "/" + "?"
								+ RequestParameters.coordToken + "=" + RequestInfo.get().getCoordinatesToken().getSerialized()));
			}
			else
			{
				expandables = null;
			}
		}
	}

	/**
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(RestConceptChronology o)
	{
		return AlphanumComparator.compare(description, o.description, true);
	}

	/**
	 * @return description
	 */
	@XmlTransient
	public String getDescription()
	{
		return description;
	}

	/**
	 * @return identifiers
	 */
	@XmlTransient
	public RestIdentifiedObject getIdentifiers()
	{
		return identifiers;
	}

	/**
	 * @return the versions
	 */
	public List<RestConceptVersion> getVersions()
	{
		return Collections.unmodifiableList(versions);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString()
	{
		return "RestConceptChronology [description=" + description + ", identifiers=" + identifiers + ", versions=" + versions + "]";
	}
}
