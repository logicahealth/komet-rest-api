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
import net.sagebits.tmp.isaac.rest.api1.data.RestIdentifiedObject;
import sh.isaac.api.coordinate.LogicCoordinate;
import sh.isaac.api.externalizable.IsaacObjectType;

/**
 * 
 * {@link RestLogicCoordinate}
 *
 * @author <a href="mailto:joel.kniaz.list@gmail.com">Joel Kniaz</a>
 *
 */
@XmlRootElement
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY)
public class RestLogicCoordinate
{
	/**
	 * Identifiers of the STATED assemblage concept.
	 */
	@XmlElement
	public RestIdentifiedObject statedAssemblage;

	/**
	 * Identifiers of the INFERRED assemblage concept.
	 */
	@XmlElement
	public RestIdentifiedObject inferredAssemblage;

	/**
	 * Identifiers of the description profile assemblage concept.
	 */
	@XmlElement
	public RestIdentifiedObject descriptionLogicProfile;

	/**
	 * Identifiers of the classifier assemblage concept.
	 */
	@XmlElement
	public RestIdentifiedObject classifier;

	/**
	 * @param ochreLogicCoordinate OCHRE LogicCoordinate
	 * 
	 *            Constructs RestLogicCoordinate from OCHRE LogicCoordinate
	 */
	public RestLogicCoordinate(LogicCoordinate ochreLogicCoordinate)
	{
		statedAssemblage = new RestIdentifiedObject(ochreLogicCoordinate.getStatedAssemblageNid(), IsaacObjectType.CONCEPT);
		inferredAssemblage = new RestIdentifiedObject(ochreLogicCoordinate.getInferredAssemblageNid(), IsaacObjectType.CONCEPT);
		descriptionLogicProfile = new RestIdentifiedObject(ochreLogicCoordinate.getDescriptionLogicProfileNid(), IsaacObjectType.CONCEPT);
		classifier = new RestIdentifiedObject(ochreLogicCoordinate.getClassifierNid(), IsaacObjectType.CONCEPT);
	}

	protected RestLogicCoordinate()
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
		result = prime * result + (classifier == null ? 0 : classifier.hashCode());
		result = prime * result + (descriptionLogicProfile == null ? 0 : descriptionLogicProfile.hashCode());
		result = prime * result + (inferredAssemblage == null ? 0 : inferredAssemblage.hashCode());
		result = prime * result + (statedAssemblage == null ? 0 : statedAssemblage.hashCode());
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
		RestLogicCoordinate other = (RestLogicCoordinate) obj;
		if (classifier != other.classifier)
		{
			if (classifier == null && other.classifier != null)
				return false;
			if (classifier != null && other.classifier == null)
				return false;
			if (!classifier.equals(other.classifier))
				return false;
		}
		if (descriptionLogicProfile != other.descriptionLogicProfile)
		{
			if (descriptionLogicProfile == null && other.descriptionLogicProfile != null)
				return false;
			if (descriptionLogicProfile != null && other.descriptionLogicProfile == null)
				return false;
			if (!descriptionLogicProfile.equals(other.descriptionLogicProfile))
				return false;
		}
		if (inferredAssemblage != other.inferredAssemblage)
		{
			if (inferredAssemblage == null && other.inferredAssemblage != null)
				return false;
			if (inferredAssemblage != null && other.inferredAssemblage == null)
				return false;
			if (!inferredAssemblage.equals(other.inferredAssemblage))
				return false;
		}
		if (statedAssemblage != other.statedAssemblage)
		{
			if (statedAssemblage == null && other.statedAssemblage != null)
				return false;
			if (statedAssemblage != null && other.statedAssemblage == null)
				return false;
			if (!statedAssemblage.equals(other.statedAssemblage))
				return false;
		}
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString()
	{
		return "RestLogicCoordinate [statedAssemblage=" + statedAssemblage + ", inferredAssemblage=" + inferredAssemblage + ", descriptionLogicProfile="
				+ descriptionLogicProfile + ", classifier=" + classifier + "]";
	}
}
