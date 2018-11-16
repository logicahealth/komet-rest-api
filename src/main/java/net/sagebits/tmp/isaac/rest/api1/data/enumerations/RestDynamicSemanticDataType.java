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

package net.sagebits.tmp.isaac.rest.api1.data.enumerations;

import java.util.OptionalInt;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import net.sagebits.tmp.isaac.rest.api1.data.semantic.dataTypes.RestDynamicSemanticArray;
import net.sagebits.tmp.isaac.rest.api1.data.semantic.dataTypes.RestDynamicSemanticBoolean;
import net.sagebits.tmp.isaac.rest.api1.data.semantic.dataTypes.RestDynamicSemanticByteArray;
import net.sagebits.tmp.isaac.rest.api1.data.semantic.dataTypes.RestDynamicSemanticDouble;
import net.sagebits.tmp.isaac.rest.api1.data.semantic.dataTypes.RestDynamicSemanticFloat;
import net.sagebits.tmp.isaac.rest.api1.data.semantic.dataTypes.RestDynamicSemanticInteger;
import net.sagebits.tmp.isaac.rest.api1.data.semantic.dataTypes.RestDynamicSemanticLong;
import net.sagebits.tmp.isaac.rest.api1.data.semantic.dataTypes.RestDynamicSemanticNid;
import net.sagebits.tmp.isaac.rest.api1.data.semantic.dataTypes.RestDynamicSemanticString;
import net.sagebits.tmp.isaac.rest.api1.data.semantic.dataTypes.RestDynamicSemanticUUID;
import sh.isaac.api.component.semantic.version.dynamic.DynamicDataType;
import sh.isaac.api.util.NumericUtils;

/**
 * {@link RestDynamicSemanticDataType}
 * A class that maps ISAAC {@link DynamicDataType} values to REST.
 * 
 * @author <a href="mailto:daniel.armbrust.list@sagebits.net">Dan Armbrust</a>
 */

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY)
@XmlRootElement
public class RestDynamicSemanticDataType extends Enumeration
{

	/**
	 * The full value of the "@class" annotation that needs to be passed back in when constructing a RestDynamicSemanticData like
	 * RestDynamicSemanticDouble.
	 */
	@XmlElement
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public String classType;

	protected RestDynamicSemanticDataType()
	{
		// for jaxb
	}

	public RestDynamicSemanticDataType(DynamicDataType dt)
	{
		super(dt.name(), dt.getDisplayName(), dt.ordinal());
		switch (dt)
		{
			case ARRAY:
				classType = RestDynamicSemanticArray.class.getName();
				break;
			case BOOLEAN:
				classType = RestDynamicSemanticBoolean.class.getName();
				break;
			case BYTEARRAY:
				classType = RestDynamicSemanticByteArray.class.getName();
				break;
			case DOUBLE:
				classType = RestDynamicSemanticDouble.class.getName();
				break;
			case FLOAT:
				classType = RestDynamicSemanticFloat.class.getName();
				break;
			case INTEGER:
				classType = RestDynamicSemanticInteger.class.getName();
				break;
			case LONG:
				classType = RestDynamicSemanticLong.class.getName();
				break;
			case NID:
				classType = RestDynamicSemanticNid.class.getName();
				break;
			case STRING:
				classType = RestDynamicSemanticString.class.getName();
				break;
			case UUID:
				classType = RestDynamicSemanticUUID.class.getName();
				break;
			case UNKNOWN:
			case POLYMORPHIC:
			default :
				break;
		}
	}

	public static RestDynamicSemanticDataType[] getAll()
	{
		RestDynamicSemanticDataType[] result = new RestDynamicSemanticDataType[DynamicDataType.values().length];
		for (int i = 0; i < DynamicDataType.values().length; i++)
		{
			result[i] = new RestDynamicSemanticDataType(DynamicDataType.values()[i]);
		}
		return result;
	}

	public DynamicDataType translate()
	{
		return DynamicDataType.values()[this.enumId];
	}

	public static RestDynamicSemanticDataType valueOf(String str)
	{
		for (DynamicDataType enumValue : DynamicDataType.values())
		{
			if (enumValue.name().equals(str.trim()))
			{
				return new RestDynamicSemanticDataType(enumValue);
			}
			else
			{
				OptionalInt intOptional = NumericUtils.getInt(str.trim());
				if (intOptional.isPresent() && intOptional.getAsInt() == enumValue.ordinal())
				{
					return new RestDynamicSemanticDataType(enumValue);
				}
			}
		}
		throw new IllegalArgumentException("invalid RestDynamicSemanticDataType value \"" + str + "\"");
	}
}
