/**
 * Copyright Notice
 *
 * This is a work of the U.S. Government and is not subject to copyright
 * protection in the United PremiseTypes. Foreign copyrights may apply.
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
 */

package net.sagebits.tmp.isaac.rest.api1.data.enumerations;

import javax.xml.bind.annotation.XmlRootElement;
import sh.isaac.api.coordinate.PremiseType;

/**
 * 
 * {@link RestDescriptionStyle}
 * 
 * {@link DescriptionStyle#NATIVE} The terminology only uses Snomed description types - FQN, regular name, definition
 * {@link DescriptionStyle#EXTERNAL} The terminology uses its own description types.
 * {@link DescriptionStyle#EXTENDED} The terminology is mapped to native snomed description types, but description types from the source terminology 
 * are added as extended types.
 * 
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
@XmlRootElement
public class RestDescriptionStyle extends Enumeration
{
	protected RestDescriptionStyle()
	{
		// for jaxb
	}

	public RestDescriptionStyle(DescriptionStyle ds)
	{
		super(ds.name(), null, ds.ordinal());
	}

	public static RestDescriptionStyle[] getAll()
	{
		RestDescriptionStyle[] result = new RestDescriptionStyle[PremiseType.values().length];
		for (int i = 0; i < PremiseType.values().length; i++)
		{
			result[i] = new RestDescriptionStyle(DescriptionStyle.values()[i]);
		}
		return result;
	}
}