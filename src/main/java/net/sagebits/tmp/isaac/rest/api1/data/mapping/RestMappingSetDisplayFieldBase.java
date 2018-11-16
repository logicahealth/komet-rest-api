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
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import net.sagebits.tmp.isaac.rest.api1.data.enumerations.MapSetItemComponent;
import net.sagebits.tmp.isaac.rest.api1.data.enumerations.RestMapSetItemComponentType;
import sh.isaac.api.identity.IdentifiedObject;

/**
 * 
 * {@link RestMappingSetDisplayFieldBase}
 * 
 * The base class used to return attributes of a mapping set display field. This class
 * is never returned by itself - you will always be given a concrete subclass.
 *
 * @author <a href="mailto:joel.kniaz.list@gmail.com">Joel Kniaz</a>
 */
@XmlRootElement
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, defaultImpl = RestMappingSetDisplayFieldBase.class)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE)
public class RestMappingSetDisplayFieldBase
{
	/**
	 * The unique ID that identifies this display field. Depending on the value of componenetType, this may be one of three distinct types:
	 * 
	 * 1) when componentType below is set to ITEM_EXTENDED, this should be the integer column number that represents the columnPosition
	 * of the extended field (extensionValue.columnNumber)
	 * 2) when fieldComponentType below is set to a value such as SOURCE or TARGET - This required value must be an ID pulled from
	 * 1/mapping/fields[.id]. - and will be returned as a UUID.
	 * the ID returned should be utilized to link the column position of this display field in the RestMappingSetVersion.displayFields list
	 * with the ID found in RestMappingItemComputedDisplayField, when placing items on screen.
	 */
	@XmlElement
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public String id;

	/**
	 * An optional value that describes the type of this display field - which will come from the available values at /1/mapping/fieldComponentTypes
	 * 
	 * Example values of for this field are SOURCE, ITEM_EXTENDED, etc
	 * 
	 * This is only populated in the context of a mapset that already has a field order assigned. When this class is returned in a way that it only
	 * expresses the possible display fields, this will be null.
	 */
	@XmlElement
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public RestMapSetItemComponentType componentType;

	RestMappingSetDisplayFieldBase()
	{
		// for Jaxb
		super();
	}

	public RestMappingSetDisplayFieldBase(int extendedColumnIdentifier, MapSetItemComponent componentType)
	{
		// for Jaxb
		super();

		if (componentType != MapSetItemComponent.ITEM_EXTENDED)
		{
			throw new RuntimeException("oops");
		}
		this.componentType = new RestMapSetItemComponentType(componentType);
		if (extendedColumnIdentifier < 0)
		{
			throw new RuntimeException(
					"Invalid (negative) map item extended field column " + extendedColumnIdentifier + " for RestMapSetItemComponentType " + componentType);
		}
		this.id = extendedColumnIdentifier + "";

	}

	/**
	 * 
	 * @param id
	 * @param componentType should only be null when returning field capabilities - should always be populated in the context of a mapset or mapitem
	 */
	public RestMappingSetDisplayFieldBase(IdentifiedObject id, MapSetItemComponent componentType)
	{
		super();

		// Null, in cases of communicating capabilities. should be populated when describing a mapset.
		if (componentType != null)
		{
			this.componentType = new RestMapSetItemComponentType(componentType);
			if (componentType == MapSetItemComponent.ITEM_EXTENDED)
			{
				throw new RuntimeException("oops");
			}
		}
		this.id = id.getPrimordialUuid().toString();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString()
	{
		return "RestMappingSetDisplayFieldBase [name=" + id + ", component=" + componentType + "]";
	}
}
