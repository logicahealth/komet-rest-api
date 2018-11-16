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
import net.sagebits.tmp.isaac.rest.HashCodeUtils;
import net.sagebits.tmp.isaac.rest.api1.data.RestIdentifiedObject;
import sh.isaac.api.coordinate.LanguageCoordinate;
import sh.isaac.api.externalizable.IsaacObjectType;

/**
 * 
 * {@link RestLanguageCoordinate}
 *
 * @author <a href="mailto:joel.kniaz.list@gmail.com">Joel Kniaz</a>
 *
 */
@XmlRootElement
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY)
public class RestLanguageCoordinate
{
	/**
	 * Identifier of the language concept associated with the language coordinate.
	 * The language will be something like
	 * english, spanish, french, danish, polish, dutch,
	 * lithuanian, chinese, japanese, or swedish.
	 */
	@XmlElement
	public RestIdentifiedObject language;

	/**
	 * Ordered list of dialect assemblage concept identifiers. Order determines preference.
	 * A dialect assemblage will be something like US (US Dialect) or GB (Great Britain Dialect).
	 */
	@XmlElement
	public RestIdentifiedObject[] dialectAssemblagePreferences;

	/**
	 * Ordered list of description type concept identifiers. Order determines preference.
	 * A description type will be something like FQN (Fully Qualified Name), Regular (Regular Name) or Definition.
	 */
	@XmlElement
	public RestIdentifiedObject[] descriptionTypePreferences;
	
	
	/**
	 * An optional language coordinate, which is the next used language coordinate if no descriptions are found
	 * which match this language coordinate.
	 */
	@XmlElement
	public RestLanguageCoordinate nextPriorityLanguageCoordinate;
	
	/**
	 * Used to adjudicate which component to return when more than one component is available. For example, 
	 * if two modules have different preferred names for the component, which one do you prefer to return?
	 * 
	 * If this list is null or empty, the returned preferred name in the multiple case is unspecified.
	 */
	@XmlElement
	public RestIdentifiedObject[] modulePreferences;

	/**
	 * @param ochreLanguageCoordinate OCHRE LanguageCoordinate
	 * 
	 *            Constructs RestLanguageCoordinate from OCHRE LanguageCoordinate
	 */
	public RestLanguageCoordinate(LanguageCoordinate ochreLanguageCoordinate)
	{
		language = new RestIdentifiedObject(ochreLanguageCoordinate.getLanguageConceptNid(), IsaacObjectType.CONCEPT);
		dialectAssemblagePreferences = new RestIdentifiedObject[ochreLanguageCoordinate.getDialectAssemblagePreferenceList() != null
				? ochreLanguageCoordinate.getDialectAssemblagePreferenceList().length
				: 0];
		int index = 0;
		for (int seq : ochreLanguageCoordinate.getDialectAssemblagePreferenceList())
		{
			dialectAssemblagePreferences[index++] = new RestIdentifiedObject(seq, IsaacObjectType.CONCEPT);
		}
		descriptionTypePreferences = new RestIdentifiedObject[ochreLanguageCoordinate.getDescriptionTypePreferenceList() != null
				? ochreLanguageCoordinate.getDescriptionTypePreferenceList().length
				: 0];
		index = 0;
		if (descriptionTypePreferences.length > 0)
		{
			for (int seq : ochreLanguageCoordinate.getDescriptionTypePreferenceList())
			{
				descriptionTypePreferences[index++] = new RestIdentifiedObject(seq, IsaacObjectType.CONCEPT);
			}
		}
		
		if (ochreLanguageCoordinate.getNextProrityLanguageCoordinate().isPresent())
		{
			nextPriorityLanguageCoordinate = new RestLanguageCoordinate(ochreLanguageCoordinate.getNextProrityLanguageCoordinate().get());
		}
		
		modulePreferences = new RestIdentifiedObject[ochreLanguageCoordinate.getModulePreferenceListForLanguage() == null ? 0 
				: ochreLanguageCoordinate.getModulePreferenceListForLanguage().length];
		index = 0;
		if (modulePreferences.length > 0)
		{
			for (int nid : ochreLanguageCoordinate.getModulePreferenceListForLanguage())
			{
				modulePreferences[index++] = new RestIdentifiedObject(nid);
			}
		}
		
	}

	protected RestLanguageCoordinate()
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
		result = prime * result + HashCodeUtils.getOrderedUniqueValues(descriptionTypePreferences).hashCode();
		result = prime * result + HashCodeUtils.getOrderedUniqueValues(dialectAssemblagePreferences).hashCode();
		result = prime * result + (language == null ? 0 : language.hashCode());
		result = prime * result + (nextPriorityLanguageCoordinate == null ? 0 : nextPriorityLanguageCoordinate.hashCode());
		result = prime * result + HashCodeUtils.getOrderedUniqueValues(modulePreferences).hashCode();
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
		RestLanguageCoordinate other = (RestLanguageCoordinate) obj;
		if (!HashCodeUtils.getOrderedUniqueValues(descriptionTypePreferences).equals(HashCodeUtils.getOrderedUniqueValues(other.descriptionTypePreferences)))
			return false;
		if (!HashCodeUtils.getOrderedUniqueValues(dialectAssemblagePreferences)
				.equals(HashCodeUtils.getOrderedUniqueValues(other.dialectAssemblagePreferences)))
			return false;
		if (!HashCodeUtils.equals(language, other.language))
		{
			return false;
		}
		if (!HashCodeUtils.equals(nextPriorityLanguageCoordinate, other.nextPriorityLanguageCoordinate))
		{
			return false;
		}
		if (!HashCodeUtils.getOrderedUniqueValues(modulePreferences).equals(HashCodeUtils.getOrderedUniqueValues(other.modulePreferences)))
		{
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
		return "RestLanguageCoordinate [language=" + language + ", dialectAssemblagePreferences=" + dialectAssemblagePreferences
				+ ", descriptionTypePreferences=" + descriptionTypePreferences + " nextPriorityLanguageCode=" 
				+ nextPriorityLanguageCoordinate == null ? "null" : nextPriorityLanguageCoordinate.toString() + "]";
	}
}
