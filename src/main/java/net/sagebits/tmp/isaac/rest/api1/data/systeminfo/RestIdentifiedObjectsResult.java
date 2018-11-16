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

package net.sagebits.tmp.isaac.rest.api1.data.systeminfo;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import net.sagebits.tmp.isaac.rest.api1.data.concept.RestConceptChronology;
import net.sagebits.tmp.isaac.rest.api1.data.semantic.RestSemanticChronology;

/**
 * {@link RestIdentifiedObjectsResult}
 * 
 * This is used by the API that takes in an arbitrary identifier, and returns the Chronology details about the object.
 * 
 *
 * @author <a href="mailto:joel.kniaz.list@gmail.com">Joel Kniaz</a>
 */
@XmlRootElement
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE)
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY)
public class RestIdentifiedObjectsResult
{
	/**
	 * Zero or one concept chronology
	 */
	@XmlElement
	RestConceptChronology concept;

	/**
	 * Zero or one semantic chronology
	 */
	@XmlElement
	RestSemanticChronology semantic;

	public RestIdentifiedObjectsResult()
	{
		// For jaxb
	}

	/**
	 * @param concept RestConceptChronology
	 * @param semantic RestSemanticChronology
	 */
	public RestIdentifiedObjectsResult(RestConceptChronology concept, RestSemanticChronology semantic)
	{
		super();
		this.concept = concept;
		this.semantic = semantic;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString()
	{
		return "RestIdentifiedObjectsResult [concept=" + (concept != null ? concept.getDescription() : null) + ", semantic="
				+ (semantic != null ? semantic.identifiers.uuids : null) + "]";
	}

	/**
	 * @return the concept
	 */
	@XmlTransient
	public RestConceptChronology getConcept()
	{
		return concept;
	}

	/**
	 * @return the semantic
	 */
	@XmlTransient
	public RestSemanticChronology getSemanticChronology()
	{
		return semantic;
	}
}
