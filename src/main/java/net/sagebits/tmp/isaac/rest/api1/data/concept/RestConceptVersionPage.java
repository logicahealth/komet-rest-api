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

package net.sagebits.tmp.isaac.rest.api1.data.concept;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import net.sagebits.tmp.isaac.rest.api.data.Pagination;
import net.sagebits.tmp.isaac.rest.api.exceptions.RestException;

/**
 * {@link RestConceptVersionPage}
 * 
 * This class carries back result sets in a way that allows pagination
 *
 * @author <a href="mailto:joel.kniaz.list@gmail.com">Joel Kniaz</a>
 */
@XmlRootElement
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE)
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY)
public class RestConceptVersionPage
{
	/**
	 * Pagination information
	 * 
	 * If results.length < paginationData.approximateTotal,
	 * then that is an indication that the results set has been truncated due to pagination (size of results likely corresponds to the specified
	 * maxPageSize, unless the page returned includes the last item (final tranche) of the total set).
	 * The paginationData object also contains URL suggestions for how to get prior and subsequent tranches of data.
	 * The paginationData.totalIsExact should always be set to true.
	 */
	@XmlElement
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public Pagination paginationData;

	/**
	 * The contained results (see paginationData, above)
	 */
	@XmlElement
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public RestConceptVersion[] results;

	protected RestConceptVersionPage()
	{
		// For jaxb
	}

	/**
	 * @param pageNum The pagination page number >= 1 to return
	 * @param maxPageSize The maximum number of results to return per page, must be greater than 0
	 * @param approximateTotal approximate size of full matching set of which this paginated result is a subset
	 * @param totalIsExact 
	 * @param hasMorePages 
	 * @param baseUrl url used to construct example previous and next urls
	 * @param results list of RestConceptVersion
	 * @throws RestException
	 */
	public RestConceptVersionPage(int pageNum, int maxPageSize, int approximateTotal, boolean totalIsExact, boolean hasMorePages, String baseUrl,
			RestConceptVersion[] results) throws RestException
	{
		this.results = results;
		this.paginationData = new Pagination(pageNum, maxPageSize, approximateTotal, totalIsExact, hasMorePages, baseUrl);
	}

	/**
	 * @return the results
	 */
	public RestConceptVersion[] getResults()
	{
		return results;
	}
}