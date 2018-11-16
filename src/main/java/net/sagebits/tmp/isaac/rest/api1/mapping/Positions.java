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
package net.sagebits.tmp.isaac.rest.api1.mapping;

import net.sagebits.tmp.isaac.rest.api.exceptions.RestException;
import sh.isaac.api.component.semantic.version.dynamic.DynamicUsageDescription;
import sh.isaac.api.constants.DynamicConstants;
import sh.isaac.mapping.constants.IsaacMappingConstants;
import sh.isaac.model.semantic.DynamicUsageDescriptionImpl;

public class Positions
{
	public int targetPos;
	public int qualfierPos;

	private Positions(int targetPos, int qualifierPos)
	{
		this.targetPos = targetPos;
		this.qualfierPos = qualifierPos;
	}

	/**
	 * @param semanticAssemblageConceptNid
	 * @return the positions of this concept
	 * @throws RestException
	 */
	public static Positions getPositions(int semanticAssemblageConceptNid) throws RestException
	{
		return getPositions(DynamicUsageDescriptionImpl.read(semanticAssemblageConceptNid));
	}

	public static Positions getPositions(DynamicUsageDescription dsud) throws RestException
	{
		int targetPos = -1;
		int qualifierPos = -1;

		for (int i = 0; i < dsud.getColumnInfo().length; i++)
		{
			if (dsud.getColumnInfo()[i].getColumnDescriptionConcept()
					.equals(DynamicConstants.get().DYNAMIC_COLUMN_ASSOCIATION_TARGET_COMPONENT.getPrimordialUuid()))
			{
				targetPos = i;
			}
			else if (dsud.getColumnInfo()[i].getColumnDescriptionConcept()
					.equals(IsaacMappingConstants.get().DYNAMIC_COLUMN_MAPPING_EQUIVALENCE_TYPE.getPrimordialUuid()))
			{
				qualifierPos = i;
			}
			if (targetPos >= 0 && qualifierPos >= 0)
			{
				break;
			}
		}
		if (targetPos < 0 || qualifierPos < 0)
		{
			throw new RestException("The specified semantic doesn't appear to be configured correctly as a mapset");
		}
		return new Positions(targetPos, qualifierPos);
	}
}