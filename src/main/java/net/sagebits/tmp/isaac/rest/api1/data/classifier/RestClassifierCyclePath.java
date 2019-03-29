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
 * {@link RestClassifierCyclePath}
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a> 
 */

@XmlRootElement
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE)
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY)
public class RestClassifierCyclePath
{	
	/**
	 * The isA path of concepts that forms the cycle from the conceptWithCycle back to itself
	 */
	@XmlElement
	private RestIdentifiedObject[] cyclePath;
	
	
	protected RestClassifierCyclePath()
	{
		//For jaxb
	}

	/**
	 * @param cyclePathNids 
	 */
	public RestClassifierCyclePath(int[] cyclePathNids)
	{
		this.cyclePath = new RestIdentifiedObject[cyclePathNids.length];
		
		for (int i = 0; i < cyclePathNids.length; i++)
		{
			this.cyclePath[i] = new RestIdentifiedObject(cyclePathNids[i]);
		}
	}
}
