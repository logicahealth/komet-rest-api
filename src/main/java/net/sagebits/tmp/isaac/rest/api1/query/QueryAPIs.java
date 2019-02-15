/**
 * Copyright Notice
 *
 * This is a work of the U.S. Government and is not subject to copyright
 * protection in the United States. Foreign copyrights may apply.
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
 */
package net.sagebits.tmp.isaac.rest.api1.query;

import java.io.StringReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import static java.util.stream.Collectors.*;
import static java.lang.Math.min;
import java.util.stream.IntStream;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;
import javax.xml.bind.JAXBException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.sagebits.tmp.isaac.rest.api.exceptions.RestException;
import net.sagebits.tmp.isaac.rest.api1.RestPaths;
import net.sagebits.tmp.isaac.rest.api1.data.query.RestQueryResult;
import net.sagebits.tmp.isaac.rest.api1.data.query.RestQueryResultPage;
import net.sagebits.tmp.isaac.rest.api1.data.query.RestQueryResults;
import net.sagebits.tmp.isaac.rest.session.RequestInfo;
import net.sagebits.tmp.isaac.rest.session.RequestInfoUtils;
import net.sagebits.tmp.isaac.rest.session.RequestParameters;
import net.sagebits.tmp.isaac.rest.session.SecurityUtils;
import sh.isaac.api.Get;
import sh.isaac.api.query.Query;
import sh.isaac.misc.security.SystemRoleConstants;
import sh.isaac.model.xml.QueryFromXmlProvider;

/**
 * {@link QueryAPIs}
 *
 * @author <a href="mailto:joel.kniaz.list@gmail.com">Joel Kniaz</a>
 */
@Path(RestPaths.queryAPIsPathComponent)
@RolesAllowed({ SystemRoleConstants.AUTOMATED, SystemRoleConstants.SUPER_USER, SystemRoleConstants.ADMINISTRATOR, SystemRoleConstants.READ_ONLY,
	SystemRoleConstants.EDITOR, SystemRoleConstants.REVIEWER, SystemRoleConstants.APPROVER, SystemRoleConstants.DEPLOYMENT_MANAGER })
public class QueryAPIs
{
	private static Logger log = LogManager.getLogger();

	@Context
	private SecurityContext securityContext;

	/**
	 * Executes queries within the ISAAC terminology hierarchy
	 * and returns the JAXB-serialized characteristics (specified within the passed FLWOR query XML) of the components
	 * that match the criteria specified within the passed FLWOR query XML
	 * 
	 * @param pageNum The pagination page number >= 1 to return
	 * @param maxPageSize The maximum number of results to return per page, must be greater than 0
	 * @param flworQueryXml the FLWOR query XML as type {@link sh.isaac.api.query.Query}
	 * <br>Example:
	 * <pre>
	 * &lt;Query&gt;
	 *     &lt;For&gt;
	 *         &lt;Concept fqn="Paths (SOLOR)" uuids="fd9d47b7-c0a4-3eea-b3ab-2b5a3f9e888f"/&gt;
	 *     &lt;/For&gt;
	 *     &lt;Let&gt;
	 *         &lt;map&gt;
	 *             &lt;entry&gt;
	 *                 &lt;key name="STAMP 1" uuid="e836892c-c408-422c-a51f-e96adfe01752"/&gt;
	 *                 &lt;value xsi:type="stampCoordinateImpl" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"&gt;
	 *                     &lt;stampCoordinateUuid&gt;cb961834-8389-3c89-ae66-aaad4743b13b&lt;/stampCoordinateUuid&gt;
	 *                     &lt;stampPosition&gt;
	 *                         &lt;time&gt;Latest&lt;/time&gt;
	 *                         &lt;path fqn="Development path (SOLOR)" uuids="1f200ca6-960e-11e5-8994-feff819cdc9f"/&gt;
	 *                     &lt;/stampPosition&gt;
	 *                     &lt;allowedStatus&gt;
	 *                         &lt;StatusList&gt;ACTIVE&lt;/StatusList&gt;
	 *                     &lt;/allowedStatus&gt;
	 *                     &lt;stampPrecedence&gt;PATH&lt;/stampPrecedence&gt;
	 *                     &lt;modules/&gt;
	 *                     &lt;modulePreferenceOrder/&gt;
	 *                 &lt;/value&gt;
	 *             &lt;/entry&gt;
	 *             &lt;entry&gt;
	 *                 &lt;key name="[US, UK] English" uuid="b6b4287f-eae4-425d-894f-5191f92155a8"/&gt;
	 *                 &lt;value xsi:type="languageCoordinateImpl" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"&gt;
	 *                     &lt;languageCoordinateUuid&gt;04c3344d-9ade-3b38-93b7-48111a900cb8&lt;/languageCoordinateUuid&gt;
	 *                     &lt;language fqn="English language (SOLOR)" uuids="06d905ea-c647-3af9-bfe5-2514e135b558"/&gt;
	 *                     &lt;dialectPreference&gt;
	 *                         &lt;Concept fqn="US English dialect (SOLOR)" uuids="bca0a686-3516-3daf-8fcf-fe396d13cfad"/&gt;
	 *                         &lt;Concept fqn="GB English dialect (SOLOR)" uuids="eb9a5e42-3cba-356d-b623-3ed472e20b30"/&gt;
	 *                     &lt;/dialectPreference&gt;
	 *                     &lt;typePreference&gt;
	 *                         &lt;Concept fqn="Fully qualified name description type (SOLOR)" uuids="00791270-77c9-32b6-b34f-d932569bd2bf 5e1fe940-8faf-11db-b606-0800200c9a66"/&gt;
	 *                         &lt;Concept fqn="Regular name description type (SOLOR)" uuids="8bfba944-3965-3946-9bcb-1e80a5da63a2 d6fad981-7df6-3388-94d8-238cc0465a79"/&gt;
	 *                     &lt;/typePreference&gt;
	 *                     &lt;modulePreference&gt;
	 *                         &lt;Concept fqn="SNOMED CT« core modules (SOLOR)" uuids="1b4f1ba5-b725-390f-8c3b-33ec7096bdca"/&gt;
	 *                         &lt;Concept fqn="SOLOR overlay module (SOLOR)" uuids="9ecc154c-e490-5cf8-805d-d2865d62aef3"/&gt;
	 *                     &lt;/modulePreference&gt;
	 *                 &lt;/value&gt;
	 *             &lt;/entry&gt;
	 *         &lt;/map&gt;
	 *     &lt;/Let&gt;
	 *     &lt;Where xsi:type="or" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"&gt;
	 *         &lt;assemblageForIteration fqn="Uninitialized component (SOLOR)" uuids="55f74246-0a25-57ac-9473-a788d08fb656"/&gt;
	 *         &lt;child-clauses&gt;
	 *             &lt;clause xsi:type="componentIsActive"&gt;
	 *                 &lt;assemblageForIteration fqn="Paths (SOLOR)" uuids="fd9d47b7-c0a4-3eea-b3ab-2b5a3f9e888f"/&gt;
	 *                 &lt;stampCoordinateKey name="STAMP 1" uuid="e836892c-c408-422c-a51f-e96adfe01752"/&gt;
	 *             &lt;/clause&gt;ulePreferenceOrder/&gt;
	 *         &lt;/child-clauses&gt;
	 *     &lt;/Where&gt;ntry&gt;
	 *     &lt;Order/&gt;try&gt;
	 *     &lt;Return&gt;t;key name="[US, UK] English" uuid="b6b4287f-eae4-425d-894f-5191f92155a8"/&gt;
	 *         &lt;AttributeSpecification columnName="Paths assemblage:Primordial UUID" propertyIndex="0"&gt;a-instance"&gt;
	 *             &lt;assemblage fqn="Paths assemblage (SOLOR)" uuids="fd9d47b7-c0a4-3eea-b3ab-2b5a3f9e888f"/&gt;teUuid&gt;
	 *             &lt;attributeFunction functionName="Primordial uuid"/&gt;="06d905ea-c647-3af9-bfe5-2514e135b558"/&gt;
	 *             &lt;propertySpecification fqn="Primordial UUID for chronicle (SOLOR)" uuids="e0fcafbc-7191-5cdc-b14a-19d4d97f71bd"/&gt;
	 *             &lt;stampCoordinateKey name="STAMP 1" uuid="e836892c-c408-422c-a51f-e96adfe01752"/&gt;f-fe396d13cfad"/&gt;
	 *         &lt;/AttributeSpecification&gt;="GB English dialect (SOLOR)" uuids="eb9a5e42-3cba-356d-b623-3ed472e20b30"/&gt;
	 *         &lt;AttributeSpecification columnName="Paths assemblage:Status for version" propertyIndex="2"&gt;
	 *             &lt;assemblage fqn="Paths assemblage (SOLOR)" uuids="fd9d47b7-c0a4-3eea-b3ab-2b5a3f9e888f"/&gt;
	 *             &lt;attributeFunction functionName="Identity"/&gt;
	 *             &lt;propertySpecification fqn="Status for version (SOLOR)" uuids="0608e233-d79d-5076-985b-9b1ea4e14b4c"/&gt;
	 *             &lt;stampCoordinateKey name="STAMP 1" uuid="e836892c-c408-422c-a51f-e96adfe01752"/&gt;
	 *         &lt;/AttributeSpecification&gt;
	 *     &lt;/Return&gt;
	 * &lt;/Query&gt;
	 * </pre>
	 * @return JAXB XML or JSON that contains the result of the query
	 * <br>Example:
	 * <pre>
	 * &lt;restQueryResultPage&gt;
	 *     &lt;paginationData&gt;
	 *         &lt;pageNum&gt;1&lt;/pageNum&gt;
	 *         &lt;approximateTotal&gt;2&lt;/approximateTotal&gt;
	 *         &lt;totalIsExact&gt;false&lt;/totalIsExact&gt;
	 *     &lt;/paginationData&gt;
	 *     &lt;results&gt;
	 *         &lt;result&gt;79a92f9e-cd93-5537-984c-c9aa4532e59d&lt;/result&gt;
	 *         &lt;result&gt;Active&lt;/result&gt;
	 *     &lt;/results&gt;
	 *     &lt;results&gt;
	 *         &lt;result&gt;f02874c5-186b-53c4-9054-f819975a9814&lt;/result&gt;
	 *         &lt;result&gt;Active&lt;/result&gt;
	 *     &lt;/results&gt;
	 * &lt;/restQueryResultPage&gt;
	 * </pre>
	 * @throws {@link net.sagebits.tmp.isaac.rest.api.exceptions.RestException}
	 */
	@POST
	@Path(RestPaths.flworComponent)
	@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
	@Consumes({MediaType.APPLICATION_XML})
	public RestQueryResultPage flworQuery(
			@QueryParam(RequestParameters.pageNum) @DefaultValue(RequestParameters.pageNumDefault) int pageNum,
			@QueryParam(RequestParameters.maxPageSize) @DefaultValue(RequestParameters.maxPageSizeDefault) int maxPageSize,
			String flworQueryXml) throws RestException {

		SecurityUtils.validateRole(securityContext, getClass());

		// Each API method should validate that passed query parameters are appropriate for this request
		RequestParameters.validateParameterNamesAgainstSupportedNames(
				RequestInfo.get().getParameters(),
				RequestParameters.PAGINATION_PARAM_NAMES);

		if (pageNum < 1) {
			throw new RestException(RequestParameters.pageNum, pageNum + "", "Parameter " + RequestParameters.pageNum + " value must be > 0");
		}

		if (maxPageSize < 1) {
			throw new RestException(RequestParameters.maxPageSize, maxPageSize + "", "Parameter " + RequestParameters.maxPageSize + " value must be > 0");
		}

		log.debug("Executing FLWOR query (pageNum=" + pageNum + ", maxPageSize=" + maxPageSize + "):\n" + flworQueryXml);

		Query queryFromXml = null;
		try {
			QueryFromXmlProvider queryParserService = Get.service(QueryFromXmlProvider.class);
			queryFromXml = queryParserService.fromXml(new StringReader(flworQueryXml));
		} catch (JAXBException e) {
			log.warn("Bad query XML: " + e.getLocalizedMessage() + "\n" + flworQueryXml, e);
			throw new RestException("Bad query XML: ", e.getLocalizedMessage());
		} catch (Exception e) {
			log.error("Unexpected error parsing FLWOR query XML:\n" + flworQueryXml, e);
			throw new RuntimeException("Unexpected error parsing FLWOR query XML", e);
		}

		// ISAAC FLWOR query executeQuery() returns rows as arrays of column arrays
		@SuppressWarnings("unchecked")
		List<List<String>> queryResultFromIsaacAsRows = Collections.EMPTY_LIST;
		try {
			queryResultFromIsaacAsRows = queryFromXml.executeQuery();
			log.info("Retrieved " + queryResultFromIsaacAsRows.size() + " unpaginated rows from FLWOR query:\n" + flworQueryXml);

		} catch (Exception e) {
			log.error(e.getClass().getSimpleName() + " executing FLWOR query with XML:\n" + flworQueryXml, e);
			throw new RuntimeException(e.getLocalizedMessage() + " executing FLWOR query " + flworQueryXml, e);
		}

		@SuppressWarnings("unchecked")
		List<List<String>> paginatedResult = Collections.EMPTY_LIST;
		final Map<Integer, List<List<String>>> paginatedResults = paginate(queryResultFromIsaacAsRows, maxPageSize);
		log.trace("Retrieved " + paginatedResults.size() + " paginated pages (pageNum=\"" + pageNum + "\", maxPageSize=\"" + maxPageSize + "\"):\n" + paginatedResults);
		if (paginatedResults.get(pageNum) != null) {
			paginatedResult = paginatedResults.get(pageNum);
		}

		log.trace("Retrieved " + paginatedResult.size() + " paginated rows (pageNum=\"" + pageNum + "\", maxPageSize=\"" + maxPageSize + "\") FLWOR query:\n" + flworQueryXml);

		final RestQueryResults results = getRestQueryResultsFromFlworQueryResult(paginatedResult);

		final String restPath = RestPaths.queryAppPathComponent + RestPaths.flworComponent;

		final RestQueryResultPage resultPage = getRestQueryResultsFromFlworQueryResults(results, pageNum, maxPageSize, restPath);

		return resultPage;
	}

	private static Map<Integer, List<List<String>>> paginate(List<List<String>> list, int pageSize) {
		return IntStream.iterate(0, i -> i + pageSize)
				.limit((list.size() + pageSize - 1) / pageSize)
				.boxed()
				.collect(toMap(i -> i / pageSize + 1,
						i -> list.subList(i, min(i + pageSize, list.size()))));
	}

	private static RestQueryResults getRestQueryResultsFromFlworQueryResult(List<List<String>> queryResultFromIsaacAsRowsOfStrings) {
		List<RestQueryResult> resultRows = new LinkedList<>();

		for (List<String> aRowOfStringsFromIsaacQueryResult : queryResultFromIsaacAsRowsOfStrings) {
			RestQueryResult aRowOfStrings = new RestQueryResult(aRowOfStringsFromIsaacQueryResult.toArray(new String[aRowOfStringsFromIsaacQueryResult.size()])); // a row of strings

			resultRows.add(aRowOfStrings);
		}
		RestQueryResults restQueryResultsToReturn = new RestQueryResults(resultRows.toArray(new RestQueryResult[resultRows.size()]));

		log.trace("Returning result of FLWOR query: " + restQueryResultsToReturn);

		return restQueryResultsToReturn;
	}

	private RestQueryResultPage getRestQueryResultsFromFlworQueryResults(RestQueryResults restQueryResults, int pageNum, int maxPageSize,
			String restPath) throws RestException
	{
		return new RestQueryResultPage(pageNum, maxPageSize, restQueryResults.getRows().length, false, restQueryResults.getRows().length == maxPageSize, restPath,
				Arrays.asList(restQueryResults.getRows()));
	}
}
