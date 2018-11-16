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

import java.util.Arrays;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import net.sagebits.tmp.isaac.rest.api1.data.semantic.dataTypes.RestDynamicSemanticString;
import net.sagebits.tmp.isaac.rest.api1.semantic.SemanticAPIs;

/**
 * {@link RestDynamicSemanticBase}
 *
 * This stub class is used for callers to edit {@link RestSemanticVersion} objects. It only contains the fields that may be edited after creation.
 * 
 * The API never returns this class.
 * 
 * @author <a href="mailto:daniel.armbrust.list@sagebits.net">Dan Armbrust</a>
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE)
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, defaultImpl = RestDynamicSemanticBase.class)
public class RestDynamicSemanticBase
{
	public RestDynamicSemanticBase()
	{
		// for jaxb
	}

	/**
	 * @param columnData
	 * @param active
	 */
	public RestDynamicSemanticBase(RestDynamicSemanticData[] columnData, Boolean active)
	{
		super();
		this.columnData = columnData;
		this.active = active;
	}

	/**
	 * The data to attach with this semantic instance (if any). This may be null, empty, or a length up to the defined length of the semantic
	 * definition.
	 * 
	 * The supplied data must match the definition of the semantic - which can be read via {@link SemanticAPIs#getSemanticDefinition(String, String)}
	 * (1/semantic/semanticDefinition/{assemblageId})
	 * 
	 * RestDynamicSemanticData is an abstract type. The data passed here, must be of a concrete type. For the server to deserialize the type properly,
	 * a field must be included of the form "@class": "net.sagebits.tmp.isaac.rest.api1.data.semantic.dataTypes.CLASSTYPE"
	 * 
	 * where CLASSTYPE is one of:
	 * - RestDynamicSemanticArray
	 * - RestDynamicSemanticBoolean
	 * - RestDynamicSemanticByteArray
	 * - RestDynamicSemanticDouble
	 * - RestDynamicSemanticFloat
	 * - RestDynamicSemanticInteger,
	 * - RestDynamicLong,
	 * - RestDynamicSemanticString,
	 * - RestDynamicSemanticNid
	 * - RestDynamicSemanticUUID
	 * 
	 * The class type strings are also available in the /rest/1/system/enumeration/restDynamicDataType call, which returns all of the available data
	 * types, names, ids, and class type information.
	 * 
	 * Example JSON that provides two columns of differing types:
	 * 
	 * {
	 *   "assemblageConcept": "-2147483449",
	 *   "referencedComponent": "-2147483557",
	 *   "columnData": [{
	 *     "@class": "net.sagebits.tmp.isaac.rest.api1.data.semantic.dataTypes.RestDynamicSemanticString",
		    "data": "test"
		    "columnNumber": "0"
	 *   }, {
	 *     "@class": "net.sagebits.tmp.isaac.rest.api1.data.semantic.dataTypes.RestDynamicLong",
	 *     "data": 5
	 *     "columnNumber": "1"
	 *   }]
	 * }
	 * 
	 * If the semantic definition contains a column that is optional, and the optional column is at the end of the set of fields, you may omit the
	 * field from the array entirely. If the optional field is in the middle of the columnData list, you may either pass a concrete type with a null data
	 * field, such as a {@link RestDynamicSemanticString} - or, you may simply pass null in the array. However, the column number will have to be assumed
	 * from the position of the null within the array, as it arrives, in this case.
	 * 
	 * Column numbers are required whenever a non-null element is passed in the array, and the column number has precedence over the array position
	 * order when it comes to aligning the data for storage.
	 */
	@XmlElement
	@JsonInclude
	public RestDynamicSemanticData[] columnData;

	/**
	 * True to indicate the semantic should be set as active, false for inactive.
	 * This field is optional, if not provided, it will be assumed to be active.
	 */
	@XmlElement
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public Boolean active;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString()
	{
		return "RestDynamicSemanticBase [columnData=" + Arrays.toString(columnData) + ", active=" + active + "]";
	}
}
