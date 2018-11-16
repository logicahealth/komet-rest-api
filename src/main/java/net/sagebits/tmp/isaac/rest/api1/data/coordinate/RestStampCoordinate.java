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

package net.sagebits.tmp.isaac.rest.api1.data.coordinate;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import net.sagebits.tmp.isaac.rest.HashCodeUtils;
import net.sagebits.tmp.isaac.rest.api1.data.RestIdentifiedObject;
import net.sagebits.tmp.isaac.rest.api1.data.enumerations.RestStampPrecedenceType;
import net.sagebits.tmp.isaac.rest.api1.data.enumerations.RestStateType;
import sh.isaac.api.Get;
import sh.isaac.api.coordinate.StampCoordinate;

/**
 * 
 * {@link RestStampCoordinate}
 *
 * @author <a href="mailto:joel.kniaz.list@gmail.com">Joel Kniaz</a>
 *
 */
@XmlRootElement
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY)
public class RestStampCoordinate
{
	/**
	 * Long epoch time component of the RestStampCoordinate.
	 * Corresponds to the time component of the StampPosition component of the OCHRE StampCoordinate.
	 * A value of 9223372036854775807 (Long.MAX_TIME) indicates that the StampCoordinate specifies LATEST
	 */
	@XmlElement
	public long time;

	/**
	 * Nid of the path concept.
	 * Corresponds to the path component of the StampPosition component of the OCHRE StampCoordinate
	 */
	@XmlElement
	public int path;

	/**
	 * RestStampPrecedenceType Enumeration specifying precedence. Values are either EDIT_PATH or TIME.
	 */
	@XmlElement
	public RestStampPrecedenceType precedence;

	/**
	 * Set of module concept identifiers.
	 */
	@XmlElement
	public RestIdentifiedObject[] modules;

	/**
	 * Set of RestStateType Enumeration values determining allowed RestStateType values.
	 * Values include INACTIVE, ACTIVE, PRIMORDIAL and CANCELLED.
	 */
	@XmlElement
	public Set<RestStateType> allowedStates = new HashSet<>();

	/**
	 * @param ochreStampCoordinate OCHRE StampCoordinate
	 * 
	 *            Constructs a RestStampCoordinate from an OCHRE StampCoordinate
	 */
	public RestStampCoordinate(StampCoordinate ochreStampCoordinate)
	{
		time = ochreStampCoordinate.getStampPosition().getTime();
		path = ochreStampCoordinate.getStampPosition().getStampPathSpecification().getNid();
		precedence = new RestStampPrecedenceType(ochreStampCoordinate.getStampPrecedence());

		Set<Integer> moduleNids = new HashSet<>();
		ochreStampCoordinate.getModuleNids().stream().forEach((seq) -> moduleNids.add(seq));
		modules = new RestIdentifiedObject[moduleNids.size()];
		int index = 0;
		for (Iterator<Integer> iter = moduleNids.iterator(); iter.hasNext();)
		{
			modules[index++] = new RestIdentifiedObject(Get.conceptService().getConceptChronology(iter.next()));
		}

		ochreStampCoordinate.getAllowedStates().stream().forEach((state) -> allowedStates.add(new RestStateType(state)));
	}

	protected RestStampCoordinate()
	{
		// For JAXB
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + HashCodeUtils.getUniqueValues(allowedStates).hashCode();
		result = prime * result + HashCodeUtils.getUniqueValues(modules).hashCode();
		result = prime * result + path;
		result = prime * result + ((precedence == null) ? 0 : precedence.hashCode());
		result = prime * result + (int) (time ^ (time >>> 32));
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
		RestStampCoordinate other = (RestStampCoordinate) obj;
		if (!HashCodeUtils.getUniqueValues(allowedStates).equals(HashCodeUtils.getUniqueValues(other.allowedStates)))
			return false;
		if (!HashCodeUtils.getUniqueValues(modules).equals(HashCodeUtils.getUniqueValues(other.modules)))
			return false;
		if (path != other.path)
			return false;
		if (!HashCodeUtils.equals(precedence, other.precedence))
		{
			return false;
		}
		if (time != other.time)
			return false;
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString()
	{
		return "RestStampCoordinate [time=" + (time == Long.MAX_VALUE ? "LATEST" : time) + ", path=" + path + ", precedence=" + precedence + ", modules="
				+ modules + ", allowedStates=" + allowedStates + "]";
	}
}
