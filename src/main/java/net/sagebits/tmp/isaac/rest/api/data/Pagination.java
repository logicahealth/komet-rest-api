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

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import net.sagebits.tmp.isaac.rest.api.exceptions.RestException;
import net.sagebits.tmp.isaac.rest.session.RequestParameters;

/**
 * {@link Pagination}
 * 
 * Carries data for paginating result sets and calculates and creates example previous and next page URLs
 *
 * @author <a href="mailto:joel.kniaz.list@gmail.com">Joel Kniaz</a>
 */
@XmlRootElement
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY)
public class Pagination
{
	/**
	 * Link to retrieve previous result set page - not provided if no previous page exists.
	 */
	@XmlElement
	public String previousUrl;

	/**
	 * Link to retrieve next result set page
	 */
	@XmlElement
	public String nextUrl;

	/**
	 * The page (of maximum size pageSize) number from beginning of dataset starting at 1
	 */
	@XmlElement
	public int pageNum;

	/**
	 * Estimated size of set of all matching values of which the current page is a subset. Value is negative if and only if unknown. May be affected
	 * by filtering.
	 */
	@XmlElement
	public int approximateTotal;

	/**
	 * True if the approximateTotal value is an exact count of results, false if the value is an estimate.
	 */
	@XmlElement
	public boolean totalIsExact;

	protected Pagination()
	{
		//For jaxb
	}

	/**
	 * @param pageNum page number index > 0
	 * @param maxPageSize The maximum number of results to return per page, must be greater than 0
	 * @param approximateTotal total size of set of which this page is a subset. May be affected by filtering.
	 * @param totalIsExact 
	 * @param hasMorePages 
	 * @param baseUrl base URL used to construct and return example previous and next URLs
	 * @throws RestException
	 */
	public Pagination(int pageNum, int maxPageSize, int approximateTotal, boolean totalIsExact, boolean hasMorePages, String baseUrl) throws RestException
	{
		PaginationUtils.validateParameters(pageNum, maxPageSize);

		this.pageNum = pageNum;
		this.approximateTotal = approximateTotal < 0 ? -1 : approximateTotal;
		this.totalIsExact = totalIsExact;

		boolean baseUrlHasParams = baseUrl.contains("?");

		if (this.pageNum == 1)
		{
			// At beginning
			previousUrl = null;
		}
		else
		{
			this.previousUrl = baseUrl + (baseUrlHasParams ? "&" : "?") + RequestParameters.pageNum + "=" + (this.pageNum - 1) + "&"
					+ RequestParameters.maxPageSize + "=" + maxPageSize;
		}

		if (hasMorePages)
		{
			this.nextUrl = baseUrl + (baseUrlHasParams ? "&" : "?") + RequestParameters.pageNum + "=" + (this.pageNum + 1) + "&" + RequestParameters.maxPageSize
					+ "=" + maxPageSize;
		}
		else
		{
			nextUrl = null;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString()
	{
		return "Pagination [previousUrl=" + previousUrl + ", nextUrl=" + nextUrl + ", pageNum=" + pageNum + ", approximateTotal=" + approximateTotal
				+ ", totalIsExact=" + totalIsExact + "]";
	}
}