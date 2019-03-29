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
 */
package net.sagebits.tmp.isaac.rest.api1.data.classifier;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import net.sagebits.tmp.isaac.rest.api1.data.RestIdentifiedObject;

/**
 * {@link RestClassifierCycle}
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a> 
 */

@XmlRootElement
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE)
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY)
public class RestClassifierCycle
{	
	/**
	 * A concept that has a cycle in the taxonomy
	 */
	@XmlElement
	private RestIdentifiedObject conceptWithCycle;
	
	/**
	 * The isA path(s) of concepts that forms the cycle(s) from the conceptWithCycle back to itself
	 */
	@XmlElement
	private RestClassifierCyclePath[] cyclePaths;
	
	
	protected RestClassifierCycle()
	{
		//For jaxb
	}

	/**
	 * @param cc
	 */
	public RestClassifierCycle(ClassifierCycle cc)
	{
		conceptWithCycle = new RestIdentifiedObject(cc.conceptWithCycle);
		cyclePaths = new RestClassifierCyclePath[cc.cyclePaths.size()];
		
		for (int i = 0; i < cc.cyclePaths.size(); i++)
		{
			cyclePaths[i] = new RestClassifierCyclePath(cc.cyclePaths.get(i));
		}
	}
}
