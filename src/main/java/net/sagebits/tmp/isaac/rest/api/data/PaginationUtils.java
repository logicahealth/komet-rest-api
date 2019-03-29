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

import java.util.List;
import net.sagebits.tmp.isaac.rest.api.exceptions.RestException;
import net.sagebits.tmp.isaac.rest.session.RequestParameters;

/**
 * 
 * {@link PaginationUtils}
 *
 * @author <a href="mailto:joel.kniaz.list@gmail.com">Joel Kniaz</a>
 *
 */
public class PaginationUtils
{
	private PaginationUtils()
	{
	}

	public static void validateParameters(int pageNum, int maxPageSize) throws RestException
	{
		if (pageNum < 1)
		{
			throw new RestException(RequestParameters.pageNum, Integer.toString(pageNum), "invalid parameter value. Must be int >= 1.");
		}
		if (maxPageSize < 1)
		{
			throw new RestException(RequestParameters.maxPageSize, Integer.toString(maxPageSize), "invalid parameter value. Must be int >= 0.");
		}
	}

	/**
	 * @param fullSet
	 * @param pageNum
	 * @param maxPageSize The maximum number of results to return per page, must be greater than 0
	 * @return sublist of passed list according to pageNum and maxPageSize parameters
	 * @throws RestException
	 */
	public static <T> List<T> getResults(List<T> fullSet, int pageNum, int maxPageSize) throws RestException
	{
		PaginationUtils.validateParameters(pageNum, maxPageSize);

		int lowerBound = (pageNum - 1) * maxPageSize;
		int upperBound = pageNum * maxPageSize;

		if (lowerBound >= fullSet.size())
		{
			lowerBound = 0;
			upperBound = 0;
		}
		else if (upperBound > fullSet.size())
		{
			upperBound = fullSet.size();
		}

		return fullSet.subList(lowerBound, upperBound);
	}
}
