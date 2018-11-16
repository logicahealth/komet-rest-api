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

package net.sagebits.tmp.isaac.rest.api1.data.mapping;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import net.sagebits.tmp.isaac.rest.Util;
import net.sagebits.tmp.isaac.rest.api1.data.RestIdentifiedObject;
import net.sagebits.tmp.isaac.rest.api1.data.semantic.RestDynamicSemanticData;
import sh.isaac.api.externalizable.IsaacObjectType;

/**
 * 
 * {@link RestMappingSetExtensionValue}
 * 
 *
 * @author <a href="mailto:daniel.armbrust.list@sagebits.net">Dan Armbrust</a>
 */
@XmlRootElement
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY)
public class RestMappingSetExtensionValue extends RestMappingSetExtensionValueBase
{
	/**
	 * The selected description of the extensionNameConcept that describes the purpose of this extended field on a map set definition.
	 * This is provided as a convenience on read.
	 */
	@XmlElement
	public String extensionNameConceptDescription;

	/**
	 * The identifiers of the concept that describes the purpose of this extended field on a map set definition. The descriptions from this concept
	 * will be used as the label of the extension.
	 */
	@XmlElement
	public RestIdentifiedObject extensionNameConceptIdentifiers;

	public RestMappingSetExtensionValue()
	{
		// for Jaxb
		super();
	}

	public RestMappingSetExtensionValue(int extensionNameConcept, RestDynamicSemanticData extensionValue, boolean active)
	{
		this.extensionNameConceptIdentifiers = new RestIdentifiedObject(extensionNameConcept, IsaacObjectType.CONCEPT);
		this.extensionValue = extensionValue;
		this.active = active;
		this.extensionNameConceptDescription = Util.readBestDescription(extensionNameConcept);
	}
}
