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
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import net.sagebits.tmp.isaac.rest.api1.data.semantic.RestDynamicSemanticData;
import net.sagebits.tmp.isaac.rest.api1.data.semantic.dataTypes.RestDynamicSemanticNid;
import net.sagebits.tmp.isaac.rest.api1.data.semantic.dataTypes.RestDynamicSemanticString;
import net.sagebits.tmp.isaac.rest.api1.data.semantic.dataTypes.RestDynamicSemanticUUID;

/**
 * 
 * {@link RestMappingSetExtensionValueBase}
 * 
 * This stub class is used for callers as part of creating or editing {@link RestMappingSetExtensionValue} objects. It only contains the fields they
 * may be edited
 * after creation.
 *
 * @author <a href="mailto:daniel.armbrust.list@sagebits.net">Dan Armbrust</a>
 */
@XmlRootElement
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, defaultImpl = RestMappingSetExtensionValueBase.class)
public class RestMappingSetExtensionValueBase
{
	/**
	 * The value to store for this mapping set extension. By the API, this could support any implementing type of RestDynamicSemanticData - but
	 * in practice, the only currently supported types are:
	 * {@link RestDynamicSemanticString} (for arbitrary values) and {@link RestDynamicSemanticNid}, {@link RestDynamicSemanticUUID} for storing a reference
	 * to another concept or semantic. Internally, the UUID type will be mapped to nid - and a read operation will only return
	 * {@link RestDynamicSemanticString} or {@link RestDynamicSemanticNid}
	 */
	@XmlElement
	public RestDynamicSemanticData extensionValue;

	/**
	 * The state of the extensionValue. On read, it will always be returned - true for active, false for inactive.
	 * On create or update, true to indicate the extension value should be set as active, false for inactive.
	 * This field is optional on create or update, if not provided, it will be assumed to be active.
	 */
	@XmlElement
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public Boolean active;

	public RestMappingSetExtensionValueBase()
	{
		// for Jaxb
		super();
	}

	public RestMappingSetExtensionValueBase(RestDynamicSemanticData extensionValue, boolean active)
	{
		// for Jaxb
		super();
		this.extensionValue = extensionValue;
		this.active = active;
	}
}
