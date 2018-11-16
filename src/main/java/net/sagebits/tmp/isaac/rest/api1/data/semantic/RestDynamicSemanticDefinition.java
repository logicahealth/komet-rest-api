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
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import net.sagebits.tmp.isaac.rest.Util;
import net.sagebits.tmp.isaac.rest.api1.data.RestIdentifiedObject;
import net.sagebits.tmp.isaac.rest.api1.data.enumerations.RestObjectChronologyType;
import net.sagebits.tmp.isaac.rest.api1.data.enumerations.RestSemanticType;
import sh.isaac.api.component.semantic.version.dynamic.DynamicColumnInfo;
import sh.isaac.api.component.semantic.version.dynamic.DynamicUsageDescription;
import sh.isaac.api.externalizable.IsaacObjectType;

/**
 * 
 * {@link RestDynamicSemanticDefinition}
 *
 * @author <a href="mailto:daniel.armbrust.list@sagebits.net">Dan Armbrust</a>
 */
@XmlRootElement
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY)
public class RestDynamicSemanticDefinition
{
	protected RestDynamicSemanticDefinition()
	{
		// for jaxb
	}

	public RestDynamicSemanticDefinition(DynamicUsageDescription dsud)
	{
		this.assemblageConceptId = new RestIdentifiedObject(dsud.getDynamicUsageDescriptorNid(), IsaacObjectType.CONCEPT);
		this.assemblageConceptDescription = Util.readBestDescription(this.assemblageConceptId.nid);
		this.semanticUsageDescription = dsud.getDynamicUsageDescription();
		this.referencedComponentTypeRestriction = dsud.getReferencedComponentTypeRestriction() == null ? null
				: new RestObjectChronologyType(dsud.getReferencedComponentTypeRestriction());
		this.referencedComponentTypeSubRestriction = dsud.getReferencedComponentTypeSubRestriction() == null ? null
				: new RestSemanticType(dsud.getReferencedComponentTypeSubRestriction());
		this.columnInfo = new RestDynamicSemanticColumnInfo[dsud.getColumnInfo().length];
		int i = 0;
		for (DynamicColumnInfo dsci : dsud.getColumnInfo())
		{
			this.columnInfo[i++] = new RestDynamicSemanticColumnInfo(dsci);
		}
	}

	/**
	 * The concept nio of the concept that is used as an assemblage. The rest of the descriptive details of the
	 * semantic assemblage (returned in this object) are read from this concept.
	 */
	@XmlElement
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public RestIdentifiedObject assemblageConceptId;

	/**
	 * The "best" description for the concept identified by the assemblageConceptId. This is selected based on the attributes within the session for
	 * stamp and language coordinates - or - if none present - the server default.
	 */
	@XmlElement
	String assemblageConceptDescription;

	/**
	 * the user-friendly description of the overall purpose of this semantic
	 */
	@XmlElement
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public String semanticUsageDescription;

	/**
	 * The ordered column information which will correspond with the data returned by the dataColumns field of a {@link RestDynamicSemanticVersion}.
	 * These arrays will be the same size, and in the same order.
	 * 
	 *  the column information that describes the data that may be returned as part of a semantic instance.
	 */
	@XmlElement
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public RestDynamicSemanticColumnInfo[] columnInfo;

	/**
	 * Return the {@link RestObjectChronologyType} of the restriction on referenced components for this semantic (if any - may return null)
	 * 
	 * If there is a restriction, the nid set for the referenced component in an instance of this semantic must be of the type listed here.
	 * 
	 * See rest/1/enumeration/restObjectChronologyType for a list of potential object types returned.
	 * 
	 */
	@XmlElement
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public RestObjectChronologyType referencedComponentTypeRestriction;

	/**
	 * Return the {@link RestSemanticType} of the sub restriction on referenced components for this DynamicSemantic (if any - may return null)
	 * 
	 * If there is a restriction, the nid set for the referenced component in an instance of this semantic must be of the type listed here.
	 * 
	 * This is only applicable when {@link #referencedComponentTypeRestriction} returns {@link IsaacObjectType#SEMANTIC}
	 * 
	 * See rest/1/enumeration/restVersionType for a list of potential object types returned.
	 */
	@XmlElement
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public RestSemanticType referencedComponentTypeSubRestriction;
}
