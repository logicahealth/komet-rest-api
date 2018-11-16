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

package net.sagebits.tmp.isaac.rest.api1.data.logic;

import java.time.Instant;
import javax.xml.bind.annotation.XmlElement;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import sh.isaac.model.logic.node.LiteralNodeInstant;

/**
 * 
 * {@link RestLiteralNodeInstant}
 *
 * @author <a href="mailto:joel.kniaz.list@gmail.com">Joel Kniaz</a>
 *
 *         RestLiteralNodeInstant is a logic node containing only an Instant literal value
 * 
 *         Each RestLiteralNodeInstant instance has a RestNodeSemanticType/NodeSemantic == NodeSemantic.LITERAL_INSTANT
 * 
 *         A RestLiteralNodeInstant may not have any child logic nodes.
 * 
 *         For serialization purposes, we store / send the second and nanos in this Rest class.
 *         See {@link Instant} for details on these fields.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY)
public class RestLiteralNodeInstant extends RestLogicNode
{
	/**
	 * The epochSecond of the {@link Instant}
	 * 
	 * Gets the number of seconds from the Java epoch of 1970-01-01T00:00:00Z.
	 * <p>
	 * The epoch second count is a simple incrementing count of seconds where
	 * second 0 is 1970-01-01T00:00:00Z.
	 * The nanosecond part of the day is returned by {@code nanos}.
	 *
	 * returns the seconds from the epoch of 1970-01-01T00:00:00Z
	 */
	@XmlElement
	long epochSecond;

	/**
	 * The nanos of the The epochSecond of the {@link Instant}
	 * 
	 * Gets the number of nanoseconds, later along the time-line, from the start
	 * of the second.
	 * <p>
	 * The nanosecond-of-second value measures the total number of nanoseconds from
	 * the second returned by {@code epochSecond}.
	 *
	 * returns the nanoseconds within the second, always positive, never exceeds 999,999,999
	 */
	@XmlElement
	int nanos;

	protected RestLiteralNodeInstant()
	{
		// For JAXB
	}

	/**
	 * @param literalNodeInstant
	 */
	public RestLiteralNodeInstant(LiteralNodeInstant literalNodeInstant)
	{
		super(literalNodeInstant);
		epochSecond = literalNodeInstant.getLiteralValue().getEpochSecond();
		nanos = literalNodeInstant.getLiteralValue().getNano();
	}
}
