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

package net.sagebits.tmp.isaac.rest.api1.data.enumerations;

import java.util.Optional;
import org.apache.commons.lang3.StringUtils;

/**
 * 
 * {@link MapSetItemComponent}
 *
 * @author <a href="mailto:joel.kniaz.list@gmail.com">Joel Kniaz</a>
 *
 */
public enum MapSetItemComponent
{
	SOURCE, TARGET, EQUIVALENCE_TYPE, // Attached semantics: i.e. VUID, CODE, SCT_ID, LOINC_NUM, RXCUI, FQN, PT, UUID
	ITEM_EXTENDED;// mapItemExtendedFields column numbers: i.e. 1, 2, 3

	public static Optional<MapSetItemComponent> parse(String input)
	{
		if (StringUtils.isNotBlank(input))
		{
			String trimmed = input.trim();
			for (MapSetItemComponent msit : MapSetItemComponent.values())
			{
				if (("" + msit.ordinal()).equals(trimmed) || msit.name().equalsIgnoreCase(trimmed))
				{
					return Optional.of(msit);
				}
			}
		}
		return Optional.empty();
	}
}
