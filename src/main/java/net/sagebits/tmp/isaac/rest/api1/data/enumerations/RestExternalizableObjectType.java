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
import javax.xml.bind.annotation.XmlRootElement;
import sh.isaac.api.externalizable.IsaacObjectType;
import sh.isaac.api.util.NumericUtils;

/**
 * {@link RestExternalizableObjectType}
 * A class that maps ISAAC {@link IsaacObjectType} values to REST.
 * 
 * @author <a href="mailto:joel.kniaz.list@gmail.com">Joel Kniaz</a>
 */
@XmlRootElement
public class RestExternalizableObjectType extends Enumeration
{
	protected RestExternalizableObjectType()
	{
		// for jaxb
	}

	public RestExternalizableObjectType(IsaacObjectType dt)
	{
		super(dt.name(), null, dt.ordinal());
	}

	public static RestExternalizableObjectType[] getAll()
	{
		RestExternalizableObjectType[] result = new RestExternalizableObjectType[IsaacObjectType.values().length];
		for (int i = 0; i < IsaacObjectType.values().length; i++)
		{
			result[i] = new RestExternalizableObjectType(IsaacObjectType.values()[i]);
		}
		return result;
	}

	public static RestExternalizableObjectType valueOf(String str)
	{
		for (IsaacObjectType enumValue : IsaacObjectType.values())
		{
			if (enumValue.name().equals(str.trim()))
			{
				return new RestExternalizableObjectType(enumValue);
			}
			else
			{
				OptionalInt intOptional = NumericUtils.getInt(str.trim());
				if (intOptional.isPresent() && intOptional.getAsInt() == enumValue.ordinal())
				{
					return new RestExternalizableObjectType(enumValue);
				}
			}
		}
		throw new IllegalArgumentException("invalid RestExternalizableObjectType value \"" + str + "\"");
	}
}
