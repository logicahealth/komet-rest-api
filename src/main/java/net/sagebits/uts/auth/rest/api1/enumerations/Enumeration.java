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

package net.sagebits.uts.auth.rest.api1.enumerations;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * 
 * {@link Enumeration}
 *
 * Note that this is an abstract base class. The actual returned type will be one of the
 * concrete subtype classes, such as {@link RestUserRoleType}
 *
 * @see RestUserRoleType
 * 
 * @author <a href="mailto:daniel.armbrust.list@sagebits.net">Dan Armbrust</a>
 */
@XmlSeeAlso({ RestUserRoleType.class })
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY)
@XmlRootElement
public abstract class Enumeration implements Comparable<Enumeration>
{
	/**
	 * The enum name of this enumeration type
	 */
	@XmlElement
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public String enumName;

	/**
	 * The user-friendly name of this enumeration type - if available. May be null
	 */
	@XmlElement
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public String friendlyName;

	/**
	 * The identifier of this enumeration. This would be passed back to a call that requested an enum type.
	 */
	@XmlElement
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public int enumId;

	protected Enumeration(String enumName, String friendlyName, int id)
	{
		this.enumName = enumName;
		this.friendlyName = friendlyName;
		this.enumId = id;
	}

	protected Enumeration()
	{
		// for jaxb
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int compareTo(Enumeration o)
	{
		return enumId - o.enumId;
	}

	@Override
	public String toString()
	{
		return enumName;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + enumId;
		result = prime * result + ((enumName == null) ? 0 : enumName.hashCode());
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
		Enumeration other = (Enumeration) obj;
		if (enumId != other.enumId)
			return false;
		if (enumName == null)
		{
			if (other.enumName != null)
				return false;
		}
		else if (!enumName.equals(other.enumName))
			return false;
		return true;
	}
}
