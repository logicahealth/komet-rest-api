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

import java.util.UUID;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import net.sagebits.tmp.isaac.rest.ExpandUtil;
import net.sagebits.tmp.isaac.rest.api.data.Expandable;
import net.sagebits.tmp.isaac.rest.api.data.Expandables;
import net.sagebits.tmp.isaac.rest.api1.RestPaths;
import net.sagebits.tmp.isaac.rest.api1.data.RestIdentifiedObject;
import net.sagebits.tmp.isaac.rest.api1.data.RestStampedVersion;
import net.sagebits.tmp.isaac.rest.api1.data.concept.RestConceptChronology;
import net.sagebits.tmp.isaac.rest.session.RequestInfo;
import sh.isaac.misc.associations.AssociationType;

/**
 * {@link RestAssociationTypeVersion}
 * Carries the definition of an Association in the system.
 *
 * @author <a href="mailto:daniel.armbrust.list@sagebits.net">Dan Armbrust</a>
 */
@XmlRootElement
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE)
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, defaultImpl = RestAssociationTypeVersion.class)
public class RestAssociationTypeVersion extends RestAssociationTypeVersionCreate
{
	/**
	 * The data that was not expanded as part of this call (but can be)
	 */
	@XmlElement
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public Expandables expandables;

	/**
	 * The identifiers of the concept that represents the association definition
	 */
	@XmlElement
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public RestIdentifiedObject identifiers;

	/**
	 * The StampedVersion details for this association type
	 */
	@XmlElement
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public RestStampedVersion associationItemStamp;

	/**
	 * The Concept Chronology of the concept represented by associationConceptNid. Typically blank, unless requested via the expand parameter
	 * 'referencedConcept' If 'referencedConcept' is passed, you can also pass 'versionsAll' or 'versionsLatestOnly'
	 */
	@XmlElement
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public RestConceptChronology associationConcept;

	protected RestAssociationTypeVersion()
	{
		// for jaxb
	}

	/**
	 * @param read
	 */
	public RestAssociationTypeVersion(AssociationType read)
	{
		associationName = read.getAssociationName();
		associationInverseName = read.getAssociationInverseName().orElse(null);
		description = read.getDescription();
		identifiers = new RestIdentifiedObject(read.getAssociationTypeConcept());

		if (RequestInfo.get().shouldExpand(ExpandUtil.referencedConcept))
		{
			associationConcept = new RestConceptChronology(read.getAssociationTypeConcept(), false,
					RequestInfo.get().shouldExpand(ExpandUtil.versionsLatestOnlyExpandable), true);
		}
		else
		{
			associationConcept = null;
			if (RequestInfo.get().returnExpandableLinks())
			{
				expandables = new Expandables();
				expandables.add(new Expandable(ExpandUtil.referencedConcept,
						RestPaths.conceptChronologyAppPathComponent + read.getAssociationTypeConcept().getNid()));
			}
		}
	}
}
