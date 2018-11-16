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
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.webcohesion.enunciate.metadata.json.JsonSeeAlso;
import net.sagebits.tmp.isaac.rest.api.exceptions.RestException;
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
import sh.isaac.api.component.semantic.version.dynamic.DynamicData;
import sh.isaac.api.component.semantic.version.dynamic.types.DynamicArray;
import sh.isaac.api.component.semantic.version.dynamic.types.DynamicBoolean;
import sh.isaac.api.component.semantic.version.dynamic.types.DynamicByteArray;
import sh.isaac.api.component.semantic.version.dynamic.types.DynamicDouble;
import sh.isaac.api.component.semantic.version.dynamic.types.DynamicFloat;
import sh.isaac.api.component.semantic.version.dynamic.types.DynamicInteger;
import sh.isaac.api.component.semantic.version.dynamic.types.DynamicLong;
import sh.isaac.api.component.semantic.version.dynamic.types.DynamicNid;
import sh.isaac.api.component.semantic.version.dynamic.types.DynamicString;
import sh.isaac.api.component.semantic.version.dynamic.types.DynamicUUID;
import sh.isaac.model.semantic.types.DynamicArrayImpl;
import sh.isaac.model.semantic.types.DynamicBooleanImpl;
import sh.isaac.model.semantic.types.DynamicByteArrayImpl;
import sh.isaac.model.semantic.types.DynamicDoubleImpl;
import sh.isaac.model.semantic.types.DynamicFloatImpl;
import sh.isaac.model.semantic.types.DynamicIntegerImpl;
import sh.isaac.model.semantic.types.DynamicLongImpl;
import sh.isaac.model.semantic.types.DynamicNidImpl;
import sh.isaac.model.semantic.types.DynamicStringImpl;
import sh.isaac.model.semantic.types.DynamicUUIDImpl;

/**
 * 
 * {@link RestDynamicSemanticData}
 *
 * @author <a href="mailto:daniel.armbrust.list@sagebits.net">Dan Armbrust</a>
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE)
@XmlSeeAlso({ RestDynamicSemanticArray.class, RestDynamicSemanticBoolean.class, RestDynamicSemanticByteArray.class, RestDynamicSemanticDouble.class,
		RestDynamicSemanticFloat.class, RestDynamicSemanticInteger.class, RestDynamicSemanticLong.class, RestDynamicSemanticString.class,
		RestDynamicSemanticIdentifiedData.class, RestDynamicSemanticArray[].class, RestDynamicSemanticBoolean[].class, RestDynamicSemanticByteArray[].class,
		RestDynamicSemanticDouble[].class, RestDynamicSemanticFloat[].class, RestDynamicSemanticLong[].class, RestDynamicSemanticString[].class,
		RestDynamicSemanticData[].class, RestDynamicSemanticIdentifiedData[].class })
@JsonSeeAlso({ RestDynamicSemanticArray.class, RestDynamicSemanticBoolean.class, RestDynamicSemanticByteArray.class, RestDynamicSemanticDouble.class,
		RestDynamicSemanticFloat.class, RestDynamicSemanticInteger.class, RestDynamicSemanticLong.class, RestDynamicSemanticString.class,
		RestDynamicSemanticIdentifiedData.class, RestDynamicSemanticArray[].class, RestDynamicSemanticBoolean[].class, RestDynamicSemanticByteArray[].class,
		RestDynamicSemanticDouble[].class, RestDynamicSemanticFloat[].class, RestDynamicSemanticLong[].class, RestDynamicSemanticString[].class,
		RestDynamicSemanticData[].class, RestDynamicSemanticIdentifiedData[].class })
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY)
@XmlRootElement
public abstract class RestDynamicSemanticData
{
	/**
	 * The 0 indexed column number for this data. Will not be populated for nested RestDynamicSemanticData objects where the 'data' field
	 * is of type RestDynamicSemanticArray. This field MUST be provided during during a semantic create or update, and it takes priority over
	 * the ordering of fields in an array of columns. Also may be irrelevant in cases where setting DefaultData, or ValidatorData.
	 */
	@XmlElement
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public Integer columnNumber;

	/**
	 * The data for a column within a RestDynamicSemanticVersion instance. The data type of this field depends on the type of class that extends
	 * this abstract class. The mapping of types is: <ClassType> - <Java Data Type>
	 * 
	 * - RestDynamicSemanticBoolean - boolean
	 * - RestDynamicSemanticByteArray - byte[]
	 * - RestDynamicSemanticDouble - double
	 * - RestDynamicSemanticFloat - float
	 * - RestDynamicSemanticInteger - int
	 * - RestDynamicLong - long
	 * - RestDynamicSemanticString - string
	 * - RestDynamicSemanticNid - int
	 * - RestDynamicSemanticUUID - UUID
	 * - RestDynamicSemanticArray - An array of one of the above types
	 * 
	 * The data type as returned via the REST interface will be typed however the JSON or XML serializer handles the java types.
	 * 
	 * When using this class in a create or update call, a special annotation must be included to create the proper type of
	 * {@link RestDynamicSemanticData}
	 * because {@link RestDynamicSemanticData} is an abstract type.
	 * 
	 * For the server to deserialize the type properly, a field must be included of the form "@class":
	 * "net.sagebits.tmp.isaac.rest.api1.data.semantic.dataTypes.CLASSTYPE"
	 * 
	 * where CLASSTYPE is one of:
	 * - RestDynamicSemanticBoolean
	 * - RestDynamicSemanticByteArray
	 * - RestDynamicSemanticDouble
	 * - RestDynamicSemanticFloat
	 * - RestDynamicSemanticInteger,
	 * - RestDynamicLong,
	 * - RestDynamicSemanticString,
	 * - RestDynamicSemanticNid
	 * - RestDynamicSemanticUUID
	 * - RestDynamicSemanticArray
	 * 
	 * Example JSON that provides two columns of differing types:
	 * 
	 * ...
	 * "restDynamicDataArrayField": [{
	 * "@class": "net.sagebits.tmp.isaac.rest.api1.data.semantic.dataTypes.RestDynamicSemanticString",
	 * "data": "test"
	 * }, {
	 * "@class": "net.sagebits.tmp.isaac.rest.api1.data.semantic.dataTypes.RestDynamicLong",
	 * "data": 5
	 * }]
	 * }
	 * 
	 */
	@XmlElement
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public Object data;

	protected RestDynamicSemanticData(Integer columnNumber, Object data)
	{
		this.columnNumber = columnNumber;
		this.data = data;
	}

	protected RestDynamicSemanticData()
	{
		// for jaxb
	}

	public static RestDynamicSemanticData translate(Integer columnNumber, DynamicData data)
	{
		if (data == null)
		{
			return null;
		}
		switch (data.getDynamicDataType())
		{
			case ARRAY:
				List<RestDynamicSemanticData> nested = new ArrayList<>();
				for (DynamicData nestedDataItem : ((DynamicArray<?>) data).getDataArray())
				{
					nested.add(translate(columnNumber, nestedDataItem));
				}
				return new RestDynamicSemanticArray(columnNumber, nested.toArray(new RestDynamicSemanticData[nested.size()]));
			case BOOLEAN:
				return new RestDynamicSemanticBoolean(columnNumber, ((DynamicBoolean) data).getDataBoolean());
			case BYTEARRAY:
				return new RestDynamicSemanticByteArray(columnNumber, ((DynamicByteArray) data).getDataByteArray());
			case DOUBLE:
				return new RestDynamicSemanticDouble(columnNumber, ((DynamicDouble) data).getDataDouble());
			case FLOAT:
				return new RestDynamicSemanticFloat(columnNumber, ((DynamicFloat) data).getDataFloat());
			case INTEGER:
				return new RestDynamicSemanticInteger(columnNumber, ((DynamicInteger) data).getDataInteger());
			case LONG:
				return new RestDynamicSemanticLong(columnNumber, ((DynamicLong) data).getDataLong());
			case NID:
				return new RestDynamicSemanticNid(columnNumber, ((DynamicNid) data).getDataNid());
			case STRING:
				return new RestDynamicSemanticString(columnNumber, ((DynamicString) data).getDataString());
			case UUID:
				return new RestDynamicSemanticUUID(columnNumber, ((DynamicUUID) data).getDataUUID());
			case POLYMORPHIC:
			case UNKNOWN:
			default :
				throw new RuntimeException("Programmer error");
		}
	}

	/**
	 * This implementation sorts by the column number
	 * 
	 * @param values
	 * @return the sorted data
	 * @throws RestException
	 */
	public static DynamicData[] translate(RestDynamicSemanticData[] values) throws RestException
	{
		return translate(values, false);
	}

	/**
	 * 
	 * This implementation sorts by the column number, and, if nullPadGaps is true, fills gaps (with nulls) as appropriate,
	 * as determined by the passed in column numbers
	 * 
	 * @param values
	 * @param nullPadGaps
	 * @return the data
	 * @throws RestException
	 */
	public static DynamicData[] translate(RestDynamicSemanticData[] values, boolean nullPadGaps) throws RestException
	{
		if (values == null)
		{
			return null;
		}

		sort(values);

		List<DynamicData> result = new ArrayList<DynamicData>();

		for (RestDynamicSemanticData rdsd : values)
		{
			if (nullPadGaps)
			{
				while (result.size() < rdsd.columnNumber.intValue())
				{
					result.add(null);  // fill a gap
				}
				if (result.size() == rdsd.columnNumber.intValue() || rdsd.columnNumber.intValue() < 0)
				{
					result.add(RestDynamicSemanticData.translate(rdsd));
				}
				else
				{
					throw new RuntimeException("Dan needs more sleep");
				}
			}
			else
			{
				result.add(RestDynamicSemanticData.translate(rdsd));
			}
		}
		return result.toArray(new DynamicData[result.size()]);
	}

	public static void sort(RestDynamicSemanticData[] values) throws RestException
	{
		if (values == null)
		{
			return;
		}

		int nextColNum = 0;
		for (int i = 0; i < values.length; i++)
		{
			if (values[i] == null)
			{
				// Put in an arbitrary filler, so we don't null pointer below. Assume the null was in the correct position.
				values[i] = new RestDynamicSemanticString(nextColNum++, null);
			}
			else
			{
				if (values[i].columnNumber == null)
				{
					throw new RestException("The field 'columnNumber' must be populated in the RestDynamicSemanticData");
				}
				nextColNum = values[i].columnNumber + 1;
			}
		}

		// Sort the values by column number
		Arrays.sort(values, new Comparator<RestDynamicSemanticData>()
		{
			@Override
			public int compare(RestDynamicSemanticData o1, RestDynamicSemanticData o2)
			{
				if (o1.columnNumber == o2.columnNumber && o1.columnNumber >= 0)
				{
					throw new RuntimeException("The field 'columnNumber' contained a duplicate");
				}
				return o1.columnNumber.compareTo(o2.columnNumber);
			}
		});
	}

	/**
	 * If you are translating an array of data, you should use the {@link #translate(RestDynamicSemanticData[])} method instead, as that handles
	 * honoring column number and gaps properly.
	 * 
	 * @param data
	 * @return the translated data
	 */
	public static DynamicData translate(RestDynamicSemanticData data)
	{
		if (data == null)
		{
			return null;
		}
		else if (data.data == null)
		{
			return null;
		}
		else if (data instanceof RestDynamicSemanticArray)
		{
			List<DynamicData> nested = new ArrayList<>();
			for (RestDynamicSemanticData nestedDataItem : ((RestDynamicSemanticArray) data).getDataArray())
			{
				nested.add(translate(nestedDataItem));
			}
			return new DynamicArrayImpl<>(nested.toArray(new DynamicData[nested.size()]));
		}
		else if (data instanceof RestDynamicSemanticBoolean)
		{
			return new DynamicBooleanImpl(((RestDynamicSemanticBoolean) data).getBoolean());
		}
		else if (data instanceof RestDynamicSemanticByteArray)
		{
			return new DynamicByteArrayImpl(((RestDynamicSemanticByteArray) data).getByteArray());
		}
		else if (data instanceof RestDynamicSemanticDouble)
		{
			return new DynamicDoubleImpl(((RestDynamicSemanticDouble) data).getDouble());
		}
		else if (data instanceof RestDynamicSemanticFloat)
		{
			return new DynamicFloatImpl(((RestDynamicSemanticFloat) data).getFloat());
		}
		else if (data instanceof RestDynamicSemanticInteger)
		{
			return new DynamicIntegerImpl(((RestDynamicSemanticInteger) data).getInteger());
		}
		else if (data instanceof RestDynamicSemanticLong)
		{
			return new DynamicLongImpl(((RestDynamicSemanticLong) data).getLong());
		}
		else if (data instanceof RestDynamicSemanticNid)
		{
			return new DynamicNidImpl(((RestDynamicSemanticNid) data).getNid());
		}
		else if (data instanceof RestDynamicSemanticString)
		{
			return new DynamicStringImpl(((RestDynamicSemanticString) data).getString());
		}
		else if (data instanceof RestDynamicSemanticUUID)
		{
			return new DynamicUUIDImpl(((RestDynamicSemanticUUID) data).getUUID());
		}
		else
		{
			throw new RuntimeException("Programmer error");
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((columnNumber == null) ? 0 : columnNumber.hashCode());
		result = prime * result + ((data == null) ? 0 : data.hashCode());
		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RestDynamicSemanticData other = (RestDynamicSemanticData) obj;
		if (columnNumber == null)
		{
			if (other.columnNumber != null)
				return false;
		}
		else if (!columnNumber.equals(other.columnNumber))
			return false;
		if (data == null)
		{
			if (other.data != null)
				return false;
		}
		else if (!data.equals(other.data))
			return false;
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString()
	{
		return "RestDynamicSemanticData [columnNumber=" + columnNumber + ", data=" + data + "]";
	}
}
