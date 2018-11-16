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

package net.sagebits.tmp.isaac.rest.api.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * 
 * {@link Expandables}
 *
 * @author <a href="mailto:daniel.armbrust.list@sagebits.net">Dan Armbrust</a>
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY)
public class Expandables
{
	/**
	 * The list of data types that were not expanded on this request that could be expanded directly, or can
	 * be expanded with a second trip by calling the provided URL
	 */
	@XmlElement
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public List<Expandable> items;

	public Expandables()
	{
		// For JAXB only
	}

	public Expandables(List<Expandable> items)
	{
		this.items = items;
	}

	public Expandables(Expandable... items)
	{
		this.items = new ArrayList<>(items == null ? 0 : items.length);
		if (items != null)
		{
			this.items.addAll(Arrays.asList(items));
		}
	}

	/**
	 * @param expandable
	 */
	public void add(Expandable expandable)
	{
		if (items == null)
		{
			this.items = new ArrayList<>();
		}
		this.items.add(expandable);
	}

	/**
	 * @param expandable
	 */
	public void remove(String expandable)
	{
		for (int i = 0; items != null && i < this.items.size(); i++)
		{
			if (this.items.get(i).name.equals(expandable))
			{
				this.items.remove(i--);
			}
		}
	}

	/**
	 * @return the size
	 */
	public int size()
	{
		return items == null ? 0 : this.items.size();
	}
}
