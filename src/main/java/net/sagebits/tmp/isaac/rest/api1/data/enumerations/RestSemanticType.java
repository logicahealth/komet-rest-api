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
import sh.isaac.api.chronicle.VersionType;

/**
 * 
 * {@link RestSemanticType}
 *
 * A class that maps ISAAC {@link VersionType} values to REST
 * 
 * @author <a href="mailto:daniel.armbrust.list@sagebits.net">Dan Armbrust</a>
 */
@XmlRootElement
public class RestSemanticType extends Enumeration
{
	protected RestSemanticType()
	{
		// for jaxb
	}

	public RestSemanticType(VersionType st)
	{
		super(st.name(), st.toString(), st.ordinal());
	}

	public static RestSemanticType[] getAll()
	{
		RestSemanticType[] result = new RestSemanticType[VersionType.values().length];
		for (int i = 0; i < VersionType.values().length; i++)
		{
			result[i] = new RestSemanticType(VersionType.values()[i]);
		}
		return result;
	}
}
