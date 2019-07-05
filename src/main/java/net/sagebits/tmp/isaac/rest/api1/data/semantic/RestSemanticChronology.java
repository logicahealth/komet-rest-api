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
import java.util.Comparator;
import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import net.sagebits.tmp.isaac.rest.ExpandUtil;
import net.sagebits.tmp.isaac.rest.Util;
import net.sagebits.tmp.isaac.rest.api.data.Expandable;
import net.sagebits.tmp.isaac.rest.api.data.Expandables;
import net.sagebits.tmp.isaac.rest.api.exceptions.RestException;
import net.sagebits.tmp.isaac.rest.api1.RestPaths;
import net.sagebits.tmp.isaac.rest.api1.data.RestIdentifiedObject;
import net.sagebits.tmp.isaac.rest.session.RequestInfo;
import net.sagebits.tmp.isaac.rest.session.RequestParameters;
import sh.isaac.api.Get;
import sh.isaac.api.chronicle.LatestVersion;
import sh.isaac.api.chronicle.VersionType;
import sh.isaac.api.component.semantic.SemanticChronology;
import sh.isaac.api.component.semantic.version.DescriptionVersion;
import sh.isaac.api.component.semantic.version.SemanticVersion;
import sh.isaac.api.externalizable.IsaacObjectType;

/**
 * 
 * {@link RestSemanticChronology}
 *
 * @author <a href="mailto:daniel.armbrust.list@sagebits.net">Dan Armbrust</a>
 */
@XmlRootElement
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE)
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY)
public class RestSemanticChronology
{
	private static transient Logger log = LogManager.getLogger();
	/**
	 * The data that was not expanded as part of this call (but can be)
	 */
	@XmlElement
	public Expandables expandables;

	/**
	 * The concept nid identifier of the concept that represents the type of this semantic
	 */
	@XmlElement
	public RestIdentifiedObject assemblage;

	/**
	 * The identifier of the object that is referenced by this semantic instance. This could represent a concept or a semantic.
	 */
	@XmlElement
	public RestIdentifiedObject referencedComponent;

	/**
	 * If the referencedComponentNid represents a concept, then this carries the "best" description for that concept. This is selected based on the
	 * attributes within the session for stamp and language coordinates - or - if none present - the server default. This is not populated if the
	 * referencedComponentNid is a semantic type.
	 * Only populated when the expand parameter 'referencedDetails' is passed.
	 */
	@XmlElement
	public String referencedComponentNidDescription;

	/**
	 * The permanent identifiers attached to this semantic instance
	 */
	@XmlElement
	public RestIdentifiedObject identifiers;

	/**
	 * The list of semantic versions. Depending on the expand parameter, may be empty, the latest only, or all versions.
	 */
	@XmlElement
	public List<RestSemanticVersion> versions;

	protected RestSemanticChronology()
	{
		// For Jaxb
	}

	/**
	 * @param sc
	 * @param includeAllVersions
	 * @param includeLatestVersion
	 * @param includeNested
	 * @param populateReferencedDetails - if true, populate a description for the referencedComponent
	 * @param stampForReferencedDetailsAndLatestVersion - optional - defaults to stamp in RequestInfo, if not provided
	 * @throws RestException
	 */
	public RestSemanticChronology(SemanticChronology sc, boolean includeAllVersions, boolean includeLatestVersion, boolean includeNested,
			boolean populateReferencedDetails) throws RestException
	{
		identifiers = new RestIdentifiedObject(sc);
		assemblage = new RestIdentifiedObject(sc.getAssemblageNid(), IsaacObjectType.CONCEPT);
		referencedComponent = new RestIdentifiedObject(sc.getReferencedComponentNid());
		if (populateReferencedDetails)
		{
			if (referencedComponent.type.enumId == IsaacObjectType.CONCEPT.ordinal())
			{
				referencedComponentNidDescription = Util.readBestDescription(referencedComponent.nid);
			}
			else if (referencedComponent.type.enumId == IsaacObjectType.SEMANTIC.ordinal())
			{
				SemanticChronology referencedComponentSemantic = Get.assemblageService().getSemanticChronology(referencedComponent.nid);
				if (VersionType.DESCRIPTION == referencedComponentSemantic.getVersionType())
				{
					LatestVersion<DescriptionVersion> ds = referencedComponentSemantic
							.getLatestVersion(RequestInfo.get().getStampCoordinate());
					Util.logContradictions(log, ds);
					if (ds.isPresent())
					{
						// TODO handle contradictions
						referencedComponentNidDescription = ds.get().getText();
					}
				}

				if (referencedComponentNidDescription == null)
				{
					referencedComponentNidDescription = "[" + referencedComponentSemantic.getVersionType().name() + "]";
				}
			}
		}

		if (includeAllVersions || includeLatestVersion)
		{
			expandables = null;
			versions = new ArrayList<>();
			if (includeAllVersions)
			{
				List<SemanticVersion> versionList = sc.getVersionList();
				for (SemanticVersion sv : versionList)
				{
					versions.add(RestSemanticVersion.buildRestSemanticVersion(sv, false, includeNested, false, false));
				}
				//newest to oldest
				versions.sort(new Comparator<RestSemanticVersion>()
				{
					@Override
					public int compare(RestSemanticVersion o1, RestSemanticVersion o2)
					{
						return -1 * Long.compare(o1.semanticVersion.time, o2.semanticVersion.time);
					}
				});
			}
			else if (includeLatestVersion)
			{
				LatestVersion<SemanticVersion> latest = sc.getLatestVersion(RequestInfo.get().getStampCoordinate());
				if (latest.isPresent())
				{
					versions.add(RestSemanticVersion.buildRestSemanticVersion(latest.get(), false, includeNested, false, true));
				}
			}
		}
		else
		{
			versions = null;
			if (RequestInfo.get().returnExpandableLinks())
			{
				expandables = new Expandables(
						new Expandable(ExpandUtil.versionsAllExpandable,
								RestPaths.semanticVersionsAppPathComponent + sc.getNid() + "?" + RequestParameters.expand + "="
										+ ExpandUtil.versionsAllExpandable),
						new Expandable(ExpandUtil.versionsLatestOnlyExpandable,
								RestPaths.semanticVersionAppPathComponent + sc.getNid() + "?" + RequestParameters.expand + "="
										+ ExpandUtil.versionsLatestOnlyExpandable + "&" + RequestParameters.coordToken + "="
										+ RequestInfo.get().getCoordinatesToken().getSerialized()));
			}
			else
			{
				expandables = null;
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString()
	{
		return "RestSemanticChronology [assemblageNid=" + assemblage + ", referencedComponentNid=" + referencedComponent
				+ ", referencedComponentNidDescription=" + referencedComponentNidDescription + ", identifiers=" + identifiers + ", versions=" + versions + "]";
	}
}
