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

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import sh.isaac.api.coordinate.ManifoldCoordinate;
import sh.isaac.api.coordinate.PremiseType;

/**
 * 
 * {@link RestManifoldCoordinate}
 *
 * @author <a href="mailto:joel.kniaz.list@gmail.com">Joel Kniaz</a>
 *
 */
@XmlRootElement
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY)
public class RestManifoldCoordinate
{
	/**
	 * Boolean indicating whether or not RestManifoldCoordinate is of STATED PremiseType.
	 * If TRUE then RestManifoldCoordinate is of PremiseType STATED.
	 * If FALSE then RestManifoldCoordinate is of PremiseType INFERRED.
	 */
	@XmlElement
	public boolean stated;

	/**
	 * RestStampCoordinate component of RestManifoldCoordinate
	 */
	@XmlElement
	public RestStampCoordinate stampCoordinate;

	/**
	 * RestLanguageCoordinate component of RestManifoldCoordinate
	 */
	@XmlElement
	public RestLanguageCoordinate languageCoordinate;

	/**
	 * RestLogicCoordinate component of RestManifoldCoordinate
	 */
	@XmlElement
	public RestLogicCoordinate logicCoordinate;

	/**
	 * @param ochreTaxonomyCoordinate OCHRE TaxonomyCoordinate
	 * 
	 *            Constructs a RestManifoldCoordinate from an OCHRE TaxonomyCoordinate
	 */
	public RestManifoldCoordinate(ManifoldCoordinate ochreTaxonomyCoordinate)
	{
		stated = ochreTaxonomyCoordinate.getTaxonomyPremiseType() == PremiseType.STATED;
		stampCoordinate = new RestStampCoordinate(ochreTaxonomyCoordinate.getStampCoordinate());
		languageCoordinate = new RestLanguageCoordinate(ochreTaxonomyCoordinate.getLanguageCoordinate());
		logicCoordinate = new RestLogicCoordinate(ochreTaxonomyCoordinate.getLogicCoordinate());
	}

	protected RestManifoldCoordinate()
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
		result = prime * result + ((languageCoordinate == null) ? 0 : languageCoordinate.hashCode());
		result = prime * result + ((logicCoordinate == null) ? 0 : logicCoordinate.hashCode());
		result = prime * result + ((stampCoordinate == null) ? 0 : stampCoordinate.hashCode());
		result = prime * result + (stated ? 1231 : 1237);
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
		RestManifoldCoordinate other = (RestManifoldCoordinate) obj;
		if (languageCoordinate == null)
		{
			if (other.languageCoordinate != null)
				return false;
		}
		else if (!languageCoordinate.equals(other.languageCoordinate))
			return false;
		if (logicCoordinate == null)
		{
			if (other.logicCoordinate != null)
				return false;
		}
		else if (!logicCoordinate.equals(other.logicCoordinate))
			return false;
		if (stampCoordinate == null)
		{
			if (other.stampCoordinate != null)
				return false;
		}
		else if (!stampCoordinate.equals(other.stampCoordinate))
			return false;
		if (stated != other.stated)
			return false;
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString()
	{
		return "RestManifoldCoordinate [stated=" + stated + ", stampCoordinate=" + stampCoordinate + ", languageCoordinate=" + languageCoordinate
				+ ", logicCoordinate=" + logicCoordinate + "]";
	}
}
