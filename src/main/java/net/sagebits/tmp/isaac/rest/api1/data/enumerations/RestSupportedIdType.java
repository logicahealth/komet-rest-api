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

import javax.xml.bind.annotation.XmlRootElement;
import sh.isaac.api.component.semantic.version.dynamic.DynamicDataType;

/**
 * {@link RestSupportedIdType}
 * A class that maps ISAAC {@link DynamicDataType} values to REST.
 * 
 * @author <a href="mailto:daniel.armbrust.list@sagebits.net">Dan Armbrust</a>
 */
@XmlRootElement
public class RestSupportedIdType extends Enumeration
{
	protected RestSupportedIdType()
	{
		// for jaxb
	}

	public RestSupportedIdType(IdType idType)
	{
		super(idType.name(), idType.getDisplayName(), idType.ordinal());
	}

	public static RestSupportedIdType[] getAll()
	{
		RestSupportedIdType[] result = new RestSupportedIdType[IdType.values().length];
		for (int i = 0; i < IdType.values().length; i++)
		{
			result[i] = new RestSupportedIdType(IdType.values()[i]);
		}
		return result;
	}
}
