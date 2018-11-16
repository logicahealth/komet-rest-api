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
import net.sagebits.tmp.isaac.rest.api1.data.RestIdentifiedObject;
import net.sagebits.tmp.isaac.rest.api1.data.enumerations.RestDynamicSemanticDataType;
import net.sagebits.tmp.isaac.rest.api1.data.enumerations.RestDynamicSemanticValidatorType;
import sh.isaac.api.component.semantic.version.dynamic.DynamicColumnInfo;

/**
 * 
 * {@link RestDynamicSemanticColumnInfo}
 *
 * @author <a href="mailto:daniel.armbrust.list@sagebits.net">Dan Armbrust</a>
 */
@XmlRootElement
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY)
public class RestDynamicSemanticColumnInfo
{
	/**
	 * The concept nid of the concept that represents the column within the dynamic semantic.
	 */
	@XmlElement
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public RestIdentifiedObject columnLabelConcept;

	/**
	 * The user-friendly name to display for this column.
	 */
	@XmlElement
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public String columnName;

	/**
	 * The user friendly description for this column. Suitable for things like tooltip descriptions.
	 */
	@XmlElement
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public String columnDescription;

	/**
	 * The 0 indexed order of this column within the dynamic semantic.
	 */
	@XmlElement
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public int columnOrder;

	/**
	 * The type of data that will be found in this column. String, Integer, etc. See
	 * rest/1/enumeration/restDynamicDataType for a list of all of the possible data types.
	 */
	@XmlElement
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public RestDynamicSemanticDataType columnDataType;

	/**
	 * The default value to use for this column when creating a new semantic (if no user value is specified).
	 * This field is optional and may be null.
	 */
	@XmlElement
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public RestDynamicSemanticData columnDefaultData;

	/**
	 * Does the user have to provide a value for this column in order to create an instance of this semantic.
	 */
	@XmlElement
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public boolean columnRequired;

	/**
	 * The validators types that are attached to this semantic (if any). Interval, <, etc. See
	 * rest/1/enumeration/restDynamicValidatorType for a list of all possible validator types.
	 */
	@XmlElement
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public RestDynamicSemanticValidatorType[] columnValidatorTypes;

	/**
	 * The data required to execute the validator type specified in columnValidatorTypes. The format and type of this field
	 * will depend on the columnValidatorTypes field. The positions within this array will match with the columnValidatorTypes
	 * array.
	 */
	@XmlElement
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public RestDynamicSemanticData[] columnValidatorData;

	protected RestDynamicSemanticColumnInfo()
	{
		// for jaxb
	}

	public RestDynamicSemanticColumnInfo(DynamicColumnInfo dsci)
	{
		// TODO Dynamic Semantic Column Info is not computed with the passed view coordinate, therefore, the descriptions are not correct.
		// Need to fix...
		this.columnLabelConcept = new RestIdentifiedObject(dsci.getColumnDescriptionConcept());
		this.columnDataType = new RestDynamicSemanticDataType(dsci.getColumnDataType());
		this.columnDefaultData = dsci.getDefaultColumnValue() == null ? null : RestDynamicSemanticData.translate(dsci.getColumnOrder(), dsci.getDefaultColumnValue());
		this.columnDescription = dsci.getColumnDescription();
		this.columnName = dsci.getColumnName();
		this.columnOrder = dsci.getColumnOrder();
		this.columnRequired = dsci.isColumnRequired();
		this.columnValidatorData = dsci.getValidatorData() == null ? null : new RestDynamicSemanticData[dsci.getValidatorData().length];
		if (this.columnValidatorData != null)
		{
			for (int i = 0; i < dsci.getValidatorData().length; i++)
			{
				// NOTE this dsci.getColumnOrder() column number value is meaningless and useless here
				// and could just as well be set to anything
				this.columnValidatorData[i] = RestDynamicSemanticData.translate(dsci.getColumnOrder(), dsci.getValidatorData()[i]);
			}
		}
		this.columnValidatorTypes = dsci.getValidator() == null ? null : new RestDynamicSemanticValidatorType[dsci.getValidator().length];
		if (this.columnValidatorTypes != null)
		{
			for (int i = 0; i < dsci.getValidatorData().length; i++)
			{
				this.columnValidatorTypes[i] = new RestDynamicSemanticValidatorType(dsci.getValidator()[i]);
			}
		}
	}
}
