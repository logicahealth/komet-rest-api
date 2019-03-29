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
package net.sagebits.tmp.isaac.rest.testng;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.glassfish.grizzly.http.util.Header;
import org.glassfish.grizzly.utils.Charsets;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.sagebits.tmp.isaac.rest.ExpandUtil;
import net.sagebits.tmp.isaac.rest.api.data.PaginationUtils;
import net.sagebits.tmp.isaac.rest.api.exceptions.RestException;
import net.sagebits.tmp.isaac.rest.api1.RestPaths;
import net.sagebits.tmp.isaac.rest.api1.data.RestCoordinatesToken;
import net.sagebits.tmp.isaac.rest.api1.data.RestEditToken;
import net.sagebits.tmp.isaac.rest.api1.data.RestIdentifiedObjects;
import net.sagebits.tmp.isaac.rest.api1.data.RestSystemInfo;
import net.sagebits.tmp.isaac.rest.api1.data.concept.RestConceptChronology;
import net.sagebits.tmp.isaac.rest.api1.data.coordinate.RestLanguageCoordinate;
import net.sagebits.tmp.isaac.rest.api1.data.coordinate.RestLogicCoordinate;
import net.sagebits.tmp.isaac.rest.api1.data.coordinate.RestManifoldCoordinate;
import net.sagebits.tmp.isaac.rest.api1.data.enumerations.DescriptionStyle;
import net.sagebits.tmp.isaac.rest.api1.data.enumerations.IdType;
import net.sagebits.tmp.isaac.rest.api1.data.enumerations.RestDescriptionStyle;
import net.sagebits.tmp.isaac.rest.api1.data.enumerations.RestObjectChronologyType;
import net.sagebits.tmp.isaac.rest.api1.data.query.RestQueryResult;
import net.sagebits.tmp.isaac.rest.api1.data.query.RestQueryResultPage;
import net.sagebits.tmp.isaac.rest.api1.data.search.RestSearchResult;
import net.sagebits.tmp.isaac.rest.api1.data.search.RestSearchResultPage;
import net.sagebits.tmp.isaac.rest.api1.data.semantic.RestDynamicSemanticData;
import net.sagebits.tmp.isaac.rest.api1.data.semantic.RestDynamicSemanticVersion;
import net.sagebits.tmp.isaac.rest.api1.data.semantic.RestSemanticDescriptionVersion;
import net.sagebits.tmp.isaac.rest.api1.data.semantic.RestSemanticLogicGraphVersion;
import net.sagebits.tmp.isaac.rest.api1.data.semantic.RestSemanticVersionPage;
import net.sagebits.tmp.isaac.rest.api1.data.semantic.dataTypes.RestDynamicSemanticNid;
import net.sagebits.tmp.isaac.rest.api1.data.systeminfo.RestIdentifiedObjectsResult;
import net.sagebits.tmp.isaac.rest.session.RequestParameters;
import net.sagebits.tmp.isaac.rest.tokens.CoordinatesToken;
import net.sagebits.tmp.isaac.rest.tokens.CoordinatesTokens;
import net.sagebits.tmp.isaac.rest.tokens.EditToken;
import sh.isaac.MetaData;
import sh.isaac.api.Get;
import sh.isaac.api.component.semantic.SemanticChronology;
import sh.isaac.api.component.semantic.version.DescriptionVersion;
import sh.isaac.api.constants.DynamicConstants;
import sh.isaac.api.coordinate.ManifoldCoordinate;
import sh.isaac.api.coordinate.PremiseType;
import sh.isaac.api.coordinate.StampPrecedence;
import sh.isaac.api.externalizable.IsaacObjectType;
import sh.isaac.api.logic.NodeSemantic;
import sh.isaac.model.configuration.LanguageCoordinates;
import sh.isaac.model.configuration.LogicCoordinates;
import sh.isaac.model.configuration.ManifoldCoordinates;
import sh.isaac.model.configuration.StampCoordinates;

/**
 * {@link ReadOnlyRestTest}
 * Testing framework for doing full cycle testing - this launches the REST server in a grizzly container, and makes REST requests via a loop
 * back call.
 *
 * @author <a href="mailto:daniel.armbrust.list@sagebits.net">Dan Armbrust</a>
 */
@Test(suiteName="testSuite", dependsOnGroups="first", groups="second")
public class ReadOnlyRestTest extends BaseTestCode
{
	@Test
	public void testMe()
	{
		Response response = target(conceptVersionRequestPath + DynamicConstants.get().DYNAMIC_EXTENSION_DEFINITION.getPrimordialUuid()).request().get();
		checkFail(response);
	}

	@Test
	public void testEditToken()
	{
		Response getEditTokenResponse = target(editTokenRequestPath.replaceFirst(RestPaths.appPathComponent, ""))
				.queryParam(RequestParameters.ssoToken, TEST_SSO_TOKEN).request().header(Header.Accept.toString(), MediaType.APPLICATION_XML).get();
		String getEditTokenResponseResult = checkFail(getEditTokenResponse).readEntity(String.class);
		RestEditToken restEditTokenObject = XMLUtils.unmarshalObject(RestEditToken.class, getEditTokenResponseResult);

		EditToken retrievedEditToken = null;
		try
		{
			retrievedEditToken = EditToken.read(restEditTokenObject.token);
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}

		Assert.assertNull(retrievedEditToken.getActiveWorkflowProcessId());

		// Test EditToken serialization/deserialization
		String retrievedEditTokenString = retrievedEditToken.getSerialized();
		EditToken newEditToken = null;
		try
		{
			newEditToken = EditToken.read(retrievedEditTokenString);
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}

		Assert.assertNotNull(newEditToken,
				"Failed creating EditToken from serialized EditToken: token=" + retrievedEditToken + ", string=" + retrievedEditTokenString);
		Assert.assertEquals(newEditToken.getAuthorNid(), retrievedEditToken.getAuthorNid());
		Assert.assertEquals(newEditToken.getModuleNid(), retrievedEditToken.getModuleNid());
		Assert.assertEquals(newEditToken.getPathNid(), retrievedEditToken.getPathNid());
		Assert.assertEquals(newEditToken.getActiveWorkflowProcessId(), retrievedEditToken.getActiveWorkflowProcessId());
	}

	/**
	 * This test validates that the XML serializers, semantic by-assemblage API and pagination are working correctly
	 */
	@Test
	public void testPaginatedSemanticsByAssemblage()
	{
		String xpathExpr = "/restSemanticVersionPage/results/semanticChronology/identifiers";

		// Test to confirm that requested maxPageSize of results returned
		for (int pageSize : new int[] { 1, 5, 10 })
		{
			Response response = target(semanticByAssemblageRequestPath + DynamicConstants.get().DYNAMIC_EXTENSION_DEFINITION.getPrimordialUuid())
					.queryParam(RequestParameters.expand, "chronology").queryParam(RequestParameters.maxPageSize, pageSize).request()
					.header(Header.Accept.toString(), MediaType.APPLICATION_XML).get();

			String resultXmlString = checkFail(response).readEntity(String.class);

			NodeList nodeList = XMLUtils.getNodeList(resultXmlString, xpathExpr);

			Assert.assertTrue(nodeList != null && nodeList.getLength() == pageSize);
		}

		// Test to confirm that pageNum works
		{
			// Get first page of 10 results
			Response response = target(semanticByAssemblageRequestPath + DynamicConstants.get().DYNAMIC_EXTENSION_DEFINITION.getPrimordialUuid())
					.queryParam(RequestParameters.expand, "chronology").queryParam(RequestParameters.maxPageSize, 10).request()
					.header(Header.Accept.toString(), MediaType.APPLICATION_XML).get();
			String resultXmlString = checkFail(response).readEntity(String.class);
			NodeList nodeList = XMLUtils.getNodeList(resultXmlString, xpathExpr);
			String idOfTenthResultOfFirstTenResultPage = nodeList.item(9).getTextContent();

			// Get 10th page of 1 result
			response = target(semanticByAssemblageRequestPath + DynamicConstants.get().DYNAMIC_EXTENSION_DEFINITION.getPrimordialUuid())
					.queryParam(RequestParameters.expand, "chronology").queryParam(RequestParameters.pageNum, 10).queryParam(RequestParameters.maxPageSize, 1)
					.request().header(Header.Accept.toString(), MediaType.APPLICATION_XML).get();

			resultXmlString = checkFail(response).readEntity(String.class);
			nodeList = XMLUtils.getNodeList(resultXmlString, xpathExpr);
			String idOfOnlyResultOfTenthResultPage = nodeList.item(0).getTextContent();

			Assert.assertTrue(idOfTenthResultOfFirstTenResultPage.equals(idOfOnlyResultOfTenthResultPage));
		}
	}

	/**
	 * This test validates that the XML serializers, description search API and pagination are working correctly
	 */
	@Test
	public void testPaginatedSearchResults()
	{
		String xpathExpr = "/restSearchResultPage/results/matchNid";

		// Test to confirm that requested maxPageSize of results returned
		for (int pageSize : new int[] { 2, 3, 8 })
		{
			String resultXmlString = checkFail(
					target(descriptionSearchRequestPath).queryParam(RequestParameters.query, "dynamic*").queryParam(RequestParameters.expand, "uuid")
							.queryParam(RequestParameters.maxPageSize, pageSize).request().header(Header.Accept.toString(), MediaType.APPLICATION_XML).get())
									.readEntity(String.class);

			NodeList nodeList = XMLUtils.getNodeList(resultXmlString, xpathExpr);

			Assert.assertTrue(nodeList != null && nodeList.getLength() == pageSize);
		}

		// Test to confirm that pageNum works
		{
			// Get first page of 7 results
			String resultXmlString = checkFail(
					target(descriptionSearchRequestPath).queryParam(RequestParameters.query, "dynamic*").queryParam(RequestParameters.expand, "uuid")
							.queryParam(RequestParameters.maxPageSize, 7).request().header(Header.Accept.toString(), MediaType.APPLICATION_XML).get())
									.readEntity(String.class);
			NodeList nodeList = XMLUtils.getNodeList(resultXmlString, xpathExpr);
			String idOf7thResultOfFirst7ResultPage = nodeList.item(6).getTextContent();

			// Get 7th page of 1 result
			resultXmlString = checkFail(target(descriptionSearchRequestPath).queryParam(RequestParameters.query, "dynamic*")
					.queryParam(RequestParameters.expand, "uuid").queryParam(RequestParameters.pageNum, 7).queryParam(RequestParameters.maxPageSize, 1)
					.request().header(Header.Accept.toString(), MediaType.APPLICATION_XML).get()).readEntity(String.class);
			nodeList = XMLUtils.getNodeList(resultXmlString, xpathExpr);
			Assert.assertTrue(nodeList.getLength() > 0, "no nodes fround in " + resultXmlString);
			String idOfOnlyResultOf7thResultPage = nodeList.item(0).getTextContent();

			Assert.assertEquals(idOf7thResultOfFirst7ResultPage, idOfOnlyResultOf7thResultPage);
		}
	}

	/**
	 * This test validates that both the JSON and XML serializers are working correctly with returns that contain
	 * nested array data, and various implementation types of the dynamic semantic types.
	 */
	@Test
	public void testArraySemanticReturn()
	{
		Response response = target(semanticByAssemblageRequestPath + DynamicConstants.get().DYNAMIC_EXTENSION_DEFINITION.getPrimordialUuid()).request()
				.header(Header.Accept.toString(), MediaType.APPLICATION_XML).get();

		checkFail(response);

		response = target(semanticByAssemblageRequestPath + DynamicConstants.get().DYNAMIC_EXTENSION_DEFINITION.getPrimordialUuid()).request()
				.header(Header.Accept.toString(), MediaType.APPLICATION_JSON).get();

		checkFail(response);
	}

	@Test
	public void testReferencedDetailsExpansion()
	{
		Response response = target(semanticByReferencedComponentRequestPath + DynamicConstants.get().DYNAMIC_EXTENSION_DEFINITION.getPrimordialUuid()).request()
				.header(Header.Accept.toString(), MediaType.APPLICATION_XML).get();

		String result = checkFail(response).readEntity(String.class);

		Assert.assertFalse(result.contains("<conceptDescription>preferred (SOLOR)</conceptDescription>"));
		Assert.assertFalse(result.contains("</referencedComponentNidObjectType>"));
		Assert.assertFalse(
				result.contains("<referencedComponentNidDescription>dynamic semantic extension definition (SOLOR)</referencedComponentNidDescription>"));

		response = target(semanticByReferencedComponentRequestPath + DynamicConstants.get().DYNAMIC_EXTENSION_DEFINITION.getPrimordialUuid())
				.queryParam("expand", "chronology,referencedDetails,nestedSemantics").queryParam("includeDescriptions", "true").request()
				.header(Header.Accept.toString(), MediaType.APPLICATION_XML).get();

		result = checkFail(response).readEntity(String.class);

		Assert.assertTrue(result.contains("<description>Regular name description type (SOLOR)</description>"), "looked in: " + result);
		Assert.assertTrue(
				result.contains("<referencedComponentNidDescription>Dynamic extension definition (SOLOR)</referencedComponentNidDescription>"),
				"looked in: " + result);
	}

	/**
	 * This test validates idTranslateComponent
	 * 
	 * This test validates that both the JSON and XML serializers are working correctly with returns that contain data.
	 */
	@Test
	public void testIdReturn()
	{
		final String url = RestPaths.idAPIsPathComponent + RestPaths.idTranslateComponent
				+ DynamicConstants.get().DYNAMIC_EXTENSION_DEFINITION.getPrimordialUuid().toString();

		Response response = target(url).request().header(Header.Accept.toString(), MediaType.APPLICATION_XML).get();

		checkFail(response);

		response = target(url).request().header(Header.Accept.toString(), MediaType.APPLICATION_JSON).get();

		checkFail(response);

		Assert.assertEquals(getIntegerIdForUuid(DynamicConstants.get().DYNAMIC_EXTENSION_DEFINITION.getPrimordialUuid(), IdType.NID.getDisplayName()),
				DynamicConstants.get().DYNAMIC_EXTENSION_DEFINITION.getNid());

		final String idsUrl = RestPaths.idAPIsPathComponent + RestPaths.idsComponent;
		Response idsResponse = target(idsUrl).request().header(Header.Accept.toString(), MediaType.APPLICATION_XML).get();
		String idsXml = checkFail(idsResponse).readEntity(String.class);
		RestConceptChronology[] idConcepts = XMLUtils.unmarshalObjectArray(RestConceptChronology.class, idsXml);
		Assert.assertNotNull(idConcepts);
		Assert.assertTrue(idConcepts.length >= 4);

		boolean foundCodeConcept = false;
		boolean foundSctIdConcept = false;
		boolean foundVuidConcept = false;
		boolean foundGeneratedUuidConcept = false;
		for (int i = 0; i < idConcepts.length; ++i)
		{
			int retrievedIdConceptNid = idConcepts[i].getIdentifiers().nid;
			if (retrievedIdConceptNid == MetaData.CODE____SOLOR.getNid())
			{
				foundCodeConcept = true;
			}
			else if (retrievedIdConceptNid == MetaData.SCTID____SOLOR.getNid())
			{
				foundSctIdConcept = true;
			}
			else if (retrievedIdConceptNid == MetaData.VUID____SOLOR.getNid())
			{
				foundVuidConcept = true;
			}
			else if (retrievedIdConceptNid == MetaData.UUID____SOLOR.getNid())
			{
				foundGeneratedUuidConcept = true;
			}
		}
		Assert.assertTrue(foundCodeConcept);
		Assert.assertTrue(foundSctIdConcept);
		Assert.assertTrue(foundVuidConcept);
		Assert.assertTrue(foundGeneratedUuidConcept);
	}

	/**
	 * This test validates that both the JSON and XML serializers are working correctly with returns that contain
	 * concept data.
	 */
	@Test
	public void testConceptReturn()
	{
		final String url = conceptVersionRequestPath + DynamicConstants.get().DYNAMIC_EXTENSION_DEFINITION.getPrimordialUuid().toString();

		Response response = target(url).request().header(Header.Accept.toString(), MediaType.APPLICATION_XML).get();

		checkFail(response);

		response = target(url).request().header(Header.Accept.toString(), MediaType.APPLICATION_JSON).get();

		checkFail(response);
	}

	/**
	 * This test validates that both the JSON and XML serializers are working correctly with returns that contain
	 * LogicGraph data.
	 */
	@Test
	public void testLogicGraphReturn()
	{
		final String url = RestPaths.logicGraphAPIsPathComponent + RestPaths.versionComponent
				+ DynamicConstants.get().DYNAMIC_EXTENSION_DEFINITION.getPrimordialUuid().toString();

		Response response = target(url).request().header(Header.Accept.toString(), MediaType.APPLICATION_XML).get();

		checkFail(response);

		response = target(url).request().header(Header.Accept.toString(), MediaType.APPLICATION_JSON).get();

		checkFail(response);
	}

	/**
	 * This test validates that the JSON serializer is working correctly with returns that contain
	 * LogicGraph data and validates that the returned Response JSON contains a valid RestSemanticLogicGraphVersion
	 */
	@Test
	public void testRestSemanticLogicGraphVersionReturn()
	{
		final String url = RestPaths.logicGraphAPIsPathComponent + RestPaths.versionComponent
				+ DynamicConstants.get().DYNAMIC_EXTENSION_DEFINITION.getPrimordialUuid().toString();
		Response returnedResponse = target(url)
				// .queryParam(RequestParameters.expand,"version")
				.request().get();

		String output = returnedResponse.readEntity(String.class);

		ObjectMapper mapper = new ObjectMapper();

		JsonNode rootNode = null;
		try
		{
			// System.out.println("testRestSemanticLogicGraphVersionReturn() parsing json " + output);

			rootNode = mapper.readValue(output, JsonNode.class);

			// System.out.println("testRestSemanticLogicGraphVersionReturn() parsed json as " + rootNode.getNodeType() + "\n" + rootNode);
		}
		catch (IOException e)
		{
			Assert.fail("testRestSemanticLogicGraphVersionReturn() FAILED parsing json.  Caught " + e.getClass().getName() + " " + e.getLocalizedMessage());
		}

		JsonNode rootType = rootNode.get("@class");

		if (rootType.isMissingNode())
		{
			Assert.fail("testRestSemanticLogicGraphVersionReturn() parsed json object missing @class member");
		}
		else if (!rootType.asText().equals(RestSemanticLogicGraphVersion.class.getName()))
		{
			Assert.fail("testRestSemanticLogicGraphVersionReturn() parsed json of unexpected object type " + rootType.asText());
		}
		else
		{
			// System.out.println("testRestSemanticLogicGraphVersionReturn() parsed " + rootType.asText() + " object");
		}

		final String referencedConceptDescriptionFieldName = "referencedConceptDescription";
		final String referencedConceptDescriptionExpectedValue = "Dynamic extension definition (SOLOR)";
		if (!rootNode.has(referencedConceptDescriptionFieldName))
		{
			Assert.fail("testRestSemanticLogicGraphVersionReturn() parsed RestSemanticLogicGraphVersion with no referencedConceptDescription");
		}
		if (rootNode.get(referencedConceptDescriptionFieldName) == null)
		{
			Assert.fail("testRestSemanticLogicGraphVersionReturn() parsed RestSemanticLogicGraphVersion with null referencedConceptDescription");
		}
		JsonNode referencedConceptDescriptionNode = rootNode.get(referencedConceptDescriptionFieldName);
		if (!referencedConceptDescriptionNode.asText().equals(referencedConceptDescriptionExpectedValue))
		{
			Assert.fail("testRestSemanticLogicGraphVersionReturn() parsed RestSemanticLogicGraphVersion with unexpected referencedConceptDescription=\""
					+ referencedConceptDescriptionNode.asText() + "\"");
		}
		// System.out.println("testRestSemanticLogicGraphVersionReturn() parsed RestSemanticLogicGraphVersion with referencedConceptDescription of type
		// " + referencedConceptDescriptionNode.getNodeType());

		final String rootLogicNodeFieldName = "rootLogicNode";
		final String nodeSemanticNodeFieldName = "nodeSemantic";
		if (rootNode.with(rootLogicNodeFieldName).with(nodeSemanticNodeFieldName).get("enumName") == null
				|| !rootNode.with(rootLogicNodeFieldName).with(nodeSemanticNodeFieldName).get("enumName").asText().equals(NodeSemantic.DEFINITION_ROOT.name()))
		{
			Assert.fail("testRestSemanticLogicGraphVersionReturn() parsed RestSemanticLogicGraphVersion with missing or invalid " + rootLogicNodeFieldName
					+ ": \"" + rootNode.with(rootLogicNodeFieldName).with(nodeSemanticNodeFieldName).get("enumName") + "\"!=\""
					+ NodeSemantic.DEFINITION_ROOT.name() + "\"");
		}
	}

	/**
	 * This test validates that both the JSON and XML serializers are working correctly with returns that contain
	 * taxonomy data.
	 */
	@Test
	public void testTaxonomyReturn()
	{

		Response response = target(taxonomyRequestPath).request().header(Header.Accept.toString(), MediaType.APPLICATION_XML).get();
		// System.out.println(target(taxonomyRequestUrl).request().get().toString());

		checkFail(response);

		response = target(taxonomyRequestPath).request().header(Header.Accept.toString(), MediaType.APPLICATION_JSON).get();

		checkFail(response);
	}

	@Test
	public void testSearchAssemblageRestriction1()
	{
		// Check with UUID

		String result = checkFail(
				target(semanticSearchRequestPath).queryParam(RequestParameters.treatAsString, "false").queryParam(RequestParameters.query, "3")
						.queryParam(RequestParameters.semanticAssemblageId, DynamicConstants.get().DYNAMIC_EXTENSION_DEFINITION.getPrimordialUuid().toString())
						.queryParam(RequestParameters.semanticAssemblageId, MetaData.LOINC_MODULES____SOLOR.getNid() + "").request()
						.header(Header.Accept.toString(), MediaType.APPLICATION_XML).get()).readEntity(String.class);

		Assert.assertTrue(result.contains(DynamicConstants.get().DYNAMIC_COLUMN_DEFAULT_VALUE.getPrimordialUuid().toString()));

		// Check with nid
		result = checkFail(target(semanticSearchRequestPath).queryParam(RequestParameters.treatAsString, "false").queryParam(RequestParameters.query, "3")
				.queryParam(RequestParameters.semanticAssemblageId, DynamicConstants.get().DYNAMIC_EXTENSION_DEFINITION.getNid() + "")
				.queryParam(RequestParameters.semanticAssemblageId, MetaData.LOINC_MODULES____SOLOR.getNid() + "").request()
				.header(Header.Accept.toString(), MediaType.APPLICATION_XML).get()).readEntity(String.class);

		Assert.assertTrue(result.contains(DynamicConstants.get().DYNAMIC_COLUMN_DEFAULT_VALUE.getPrimordialUuid().toString()));

		// Check with nid
		result = checkFail(target(semanticSearchRequestPath).queryParam(RequestParameters.treatAsString, "false").queryParam(RequestParameters.query, "3")
				.queryParam(RequestParameters.semanticAssemblageId, DynamicConstants.get().DYNAMIC_EXTENSION_DEFINITION.getNid() + "")
				.queryParam(RequestParameters.semanticAssemblageId, MetaData.LOINC_MODULES____SOLOR.getNid() + "").request()
				.header(Header.Accept.toString(), MediaType.APPLICATION_XML).get()).readEntity(String.class);

		Assert.assertTrue(result.contains(DynamicConstants.get().DYNAMIC_COLUMN_DEFAULT_VALUE.getPrimordialUuid().toString()));

		// sanity check search
		result = checkFail(target(semanticSearchRequestPath).queryParam(RequestParameters.treatAsString, "false").queryParam(RequestParameters.query, "55")
				.queryParam(RequestParameters.semanticAssemblageId, DynamicConstants.get().DYNAMIC_EXTENSION_DEFINITION.getNid() + "")
				.queryParam(RequestParameters.semanticAssemblageId, MetaData.LOINC_MODULES____SOLOR.getNid() + "").request()
				.header(Header.Accept.toString(), MediaType.APPLICATION_XML).get()).readEntity(String.class);

		Assert.assertFalse(result.contains(DynamicConstants.get().DYNAMIC_COLUMN_DEFAULT_VALUE.getPrimordialUuid().toString()));
	}

	@Test
	public void testSearchExpandsUUID()
	{
		// Dan notes, we no longer generate metadata with any random uuids...
		// Make sure it contains a (type 5) UUID with this pattern...
		// <uuids>12604572-254c-59d2-8d9f-39d485af0fa0</uuids>
		final Pattern pXml = Pattern.compile(".*uuids.{9}-.{4}-5.{3}-.{4}-.{14}uuids.*", Pattern.DOTALL);

		// Test expand uuid on/off for each search type

		String result = checkFail(
				target(semanticSearchRequestPath).queryParam(RequestParameters.treatAsString, "false").queryParam(RequestParameters.query, "1")
						.queryParam(RequestParameters.expand, "uuid").request().header(Header.Accept.toString(), MediaType.APPLICATION_XML).get())
								.readEntity(String.class);
		Assert.assertTrue(pXml.matcher(result).matches());

		result = checkFail(target(semanticSearchRequestPath).queryParam(RequestParameters.treatAsString, "false").queryParam(RequestParameters.query, "1")
				.request().header(Header.Accept.toString(), MediaType.APPLICATION_XML).get()).readEntity(String.class);
		Assert.assertFalse(pXml.matcher(result).matches());

		result = checkFail(target(descriptionSearchRequestPath).queryParam(RequestParameters.query, "dynamic*").queryParam(RequestParameters.expand, "uuid")
				.request().header(Header.Accept.toString(), MediaType.APPLICATION_XML).get()).readEntity(String.class);
		Assert.assertTrue(pXml.matcher(result).matches());

		result = checkFail(target(descriptionSearchRequestPath).queryParam(RequestParameters.query, "dynamic*").request()
				.header(Header.Accept.toString(), MediaType.APPLICATION_XML).get()).readEntity(String.class);
		Assert.assertFalse(pXml.matcher(result).matches());

		result = checkFail(target(prefixSearchRequestPath).queryParam(RequestParameters.query, "dynamic").queryParam(RequestParameters.expand, "uuid").request()
				.header(Header.Accept.toString(), MediaType.APPLICATION_XML).get()).readEntity(String.class);
		Assert.assertTrue(pXml.matcher(result).matches());

		result = checkFail(target(prefixSearchRequestPath).queryParam(RequestParameters.query, "dynamic").request()
				.header(Header.Accept.toString(), MediaType.APPLICATION_XML).get()).readEntity(String.class);
		Assert.assertFalse(pXml.matcher(result).matches());

		result = checkFail(target(byRefSearchRequestPath).queryParam(RequestParameters.nid, MetaData.SOLOR_CONCEPT____SOLOR.getNid())
				.queryParam(RequestParameters.expand, "uuid").request().header(Header.Accept.toString(), MediaType.APPLICATION_XML).get())
						.readEntity(String.class);
		Assert.assertTrue(pXml.matcher(result).matches());

		result = checkFail(target(byRefSearchRequestPath).queryParam(RequestParameters.nid, MetaData.SOLOR_CONCEPT____SOLOR.getNid()).request()
				.header(Header.Accept.toString(), MediaType.APPLICATION_XML).get()).readEntity(String.class);
		Assert.assertFalse(pXml.matcher(result).matches());

		// Spot check for JSON return support:
		// Make sure it contains a (type 5) UUID with this pattern...
		// "uuids" : [ "bcf22234-a736-5f6b-9ce3-d016594ca5cd" ]
		final Pattern pJson = Pattern.compile(".*uuids.{15}-.{4}-5.{3}-.{4}-.{12}.*", Pattern.DOTALL);
		result = checkFail(target(prefixSearchRequestPath).queryParam(RequestParameters.query, "dynamic").queryParam(RequestParameters.expand, "uuid").request()
				.header(Header.Accept.toString(), MediaType.APPLICATION_JSON).get()).readEntity(String.class);
		Assert.assertTrue(pJson.matcher(result).matches());

		result = checkFail(target(prefixSearchRequestPath).queryParam(RequestParameters.query, "dynamic").request()
				.header(Header.Accept.toString(), MediaType.APPLICATION_JSON).get()).readEntity(String.class);
		Assert.assertFalse(pJson.matcher(result).matches());
	}

	@Test
	public void testSearchExpandsRefConcept()
	{
		// Test expand uuid on/off for each search type

		String result = checkFail(
				target(semanticSearchRequestPath).queryParam(RequestParameters.query, DynamicConstants.get().DYNAMIC_COLUMN_NAME.getPrimordialUuid().toString())
						.queryParam("expand", ExpandUtil.referencedConcept).request().header(Header.Accept.toString(), MediaType.APPLICATION_XML).get())
								.readEntity(String.class);
		Assert.assertTrue(result.contains(DynamicConstants.get().DYNAMIC_EXTENSION_DEFINITION.getPrimordialUuid().toString()));

		result = checkFail(
				target(semanticSearchRequestPath).queryParam(RequestParameters.query, DynamicConstants.get().DYNAMIC_COLUMN_NAME.getPrimordialUuid().toString())
						.request().header(Header.Accept.toString(), MediaType.APPLICATION_XML).get()).readEntity(String.class);
		Assert.assertFalse(result.contains(DynamicConstants.get().DYNAMIC_EXTENSION_DEFINITION.getPrimordialUuid().toString()));

		result = checkFail(target(descriptionSearchRequestPath).queryParam(RequestParameters.query, "dynamic semantic Asse*")
				.queryParam(RequestParameters.expand, ExpandUtil.referencedConcept).request().header(Header.Accept.toString(), MediaType.APPLICATION_XML).get())
						.readEntity(String.class);
		Assert.assertTrue(result.contains(DynamicConstants.get().DYNAMIC_ASSEMBLAGES.getPrimordialUuid().toString()));

		result = checkFail(target(descriptionSearchRequestPath).queryParam(RequestParameters.query, "dynamic Asse*").request()
				.header(Header.Accept.toString(), MediaType.APPLICATION_XML).get()).readEntity(String.class);
		Assert.assertFalse(result.contains(DynamicConstants.get().DYNAMIC_ASSEMBLAGES.getPrimordialUuid().toString()));

		result = checkFail(target(prefixSearchRequestPath).queryParam(RequestParameters.query, "dynamic Asse")
				.queryParam(RequestParameters.expand, ExpandUtil.referencedConcept).request().header(Header.Accept.toString(), MediaType.APPLICATION_XML).get())
						.readEntity(String.class);
		Assert.assertTrue(result.contains(DynamicConstants.get().DYNAMIC_ASSEMBLAGES.getPrimordialUuid().toString()), "looked in " + result);

		result = checkFail(target(prefixSearchRequestPath).queryParam(RequestParameters.query, "dynamic Asse").request()
				.header(Header.Accept.toString(), MediaType.APPLICATION_XML).get()).readEntity(String.class);
		Assert.assertFalse(result.contains(DynamicConstants.get().DYNAMIC_ASSEMBLAGES.getPrimordialUuid().toString()), "looked in " + result);

		result = checkFail(target(byRefSearchRequestPath).queryParam(RequestParameters.nid, MetaData.METADATA____SOLOR.getNid())
				.queryParam(RequestParameters.maxPageSize, "100").queryParam(RequestParameters.expand, "uuid," + ExpandUtil.referencedConcept).request()
				.header(Header.Accept.toString(), MediaType.APPLICATION_JSON).get()).readEntity(String.class);
		Assert.assertTrue(result.contains(MetaData.METADATA_MODULES____SOLOR.getPrimordialUuid().toString()), "looked in " + result);

		result = checkFail(target(byRefSearchRequestPath).queryParam(RequestParameters.nid, MetaData.METADATA____SOLOR.getNid())
				.queryParam(RequestParameters.maxPageSize, "100").queryParam(RequestParameters.expand, "uuid").request()
				.header(Header.Accept.toString(), MediaType.APPLICATION_JSON).get()).readEntity(String.class);
		Assert.assertFalse(result.contains(MetaData.METADATA_MODULES____SOLOR.getPrimordialUuid().toString()), "looked in " + result);
	}

	@Test
	public void testSearchExpandsRefConceptVersion()
	{
		// Test expand uuid on/off for each search type

		String result = checkFail(target(descriptionSearchRequestPath).queryParam(RequestParameters.query, "dynamic semantic Asse*")
				.queryParam(RequestParameters.expand, ExpandUtil.referencedConcept + "," + ExpandUtil.versionsLatestOnlyExpandable).request()
				.header(Header.Accept.toString(), MediaType.APPLICATION_XML).get()).readEntity(String.class);

		RestSearchResultPage resultsObject = XMLUtils.unmarshalObject(RestSearchResultPage.class, result);
		boolean foundActiveResult = false;
		for (RestSearchResult resultObject : resultsObject.getResults())
		{
			if (resultObject.isActive())
			{
				foundActiveResult = true;
				break;
			}
		}
		Assert.assertTrue(foundActiveResult);

		result = checkFail(target(descriptionSearchRequestPath).queryParam(RequestParameters.query, "dynamic semantic Asse*").request()
				.header(Header.Accept.toString(), MediaType.APPLICATION_XML).get()).readEntity(String.class);
		resultsObject = XMLUtils.unmarshalObject(RestSearchResultPage.class, result);
		foundActiveResult = false;
		for (RestSearchResult resultObject : resultsObject.getResults())
		{
			if (resultObject.isActive())
			{
				foundActiveResult = true;
				break;
			}
		}
		Assert.assertTrue(foundActiveResult);
	}

	@Test
	public void testSearchRecursiveRefComponentLookup()
	{
		String result = checkFail(target(semanticSearchRequestPath)
				.queryParam(RequestParameters.query, MetaData.PREFERRED____SOLOR.getNid() + "")
				.queryParam(RequestParameters.treatAsString, "true")
				.queryParam(RequestParameters.maxPageSize, 2000 )
				.queryParam(RequestParameters.expand, ExpandUtil.referencedConcept)
				.request().header(Header.Accept.toString(), MediaType.APPLICATION_XML).get()).readEntity(String.class);
		Assert.assertTrue(result.contains(DynamicConstants.get().DYNAMIC_EXTENSION_DEFINITION.getPrimordialUuid().toString()), "looked in " + result);

		result = checkFail(target(semanticSearchRequestPath)
				.queryParam(RequestParameters.query, MetaData.PREFERRED____SOLOR.getNid() + "")
				.queryParam(RequestParameters.treatAsString, "true")
				.queryParam(RequestParameters.maxPageSize, 2000)
				.request().header(Header.Accept.toString(), MediaType.APPLICATION_XML).get()).readEntity(String.class);
		Assert.assertFalse(result.contains(DynamicConstants.get().DYNAMIC_EXTENSION_DEFINITION.getPrimordialUuid().toString()));
	}

	@Test
	public void testDescriptionsFetch()
	{
		String result = checkFail(target(conceptDescriptionsRequestPath + MetaData.USER____SOLOR.getNid()).request()
				.header(Header.Accept.toString(), MediaType.APPLICATION_XML).get()).readEntity(String.class);

		// TODO change this to use objects instead of regular expression matching
		String[] temp = result.split("<restSemanticDescriptionVersion>");
		// [0] is header junk
		// [1] is the first dialect
		// [2] is the second dialect

		Assert.assertTrue(temp.length == 3);
		for (int i = 1; i < 3; i++)
		{
			String[] temp2 = temp[i].split("<dialects>");
			String preDialect = temp2[0];
			String dialect = temp2[1];
			Assert.assertEquals(temp2.length, 2);

			// Validate that the important bit of the description semantic are put together properly
			Assert.assertTrue(preDialect.matches("(?s).*<assemblage>.*<nid>.*" + MetaData.ENGLISH_LANGUAGE____SOLOR.getNid() + ".*</assemblage>.*"),
					"Wrong language");
			Assert.assertTrue(preDialect.matches("(?s).*<referencedComponent>.*<nid>.*" + MetaData.USER____SOLOR.getNid() + ".*</referencedComponent>.*"),
					"Wrong concept");
			Assert.assertTrue(preDialect.matches("(?s).*<caseSignificanceConcept>.*<nid>.*" + MetaData.DESCRIPTION_NOT_CASE_SENSITIVE____SOLOR.getNid()
					+ ".*</caseSignificanceConcept>.*"), "Wrong case sentivity");
			Assert.assertTrue(
					preDialect.matches("(?s).*<languageConcept>.*<nid>.*" + MetaData.ENGLISH_LANGUAGE____SOLOR.getNid() + ".*</languageConcept>.*"),
					"Wrong language");
			Assert.assertTrue((preDialect.contains("<text>User</text>") || preDialect.contains("<text>User (SOLOR)</text>")), "Wrong text " + preDialect);
			Assert.assertTrue((preDialect
					.matches("(?s).*<descriptionTypeConcept>.*<nid>.*" + MetaData.REGULAR_NAME_DESCRIPTION_TYPE____SOLOR.getNid() + ".*</descriptionTypeConcept>.*")
					|| preDialect.matches("(?s).*<descriptionTypeConcept>.*<nid>.*" + MetaData.FULLY_QUALIFIED_NAME_DESCRIPTION_TYPE____SOLOR.getNid()
							+ ".*</descriptionTypeConcept>.*")),
					"Wrong description type");

			// validate that the dialect bits are put together properly
			Assert.assertTrue(dialect.matches("(?s).*<assemblage>.*<nid>.*" + MetaData.US_ENGLISH_DIALECT____SOLOR.getNid() + ".*</assemblage>.*"),
					"Wrong dialect");
			Assert.assertTrue(
					dialect.contains(
							"<data xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xsi:type=\"xs:int\">" + MetaData.PREFERRED____SOLOR.getNid() + "</data>"),
					"Wrong value");
		}
	}

	@Test
	public void testDescriptionObjectsFetch()
	{
		RestSemanticDescriptionVersion[] descriptions = getDescriptionsForConcept(MetaData.USER____SOLOR.getNid());

		Assert.assertTrue(descriptions.length == 2);
		for (RestSemanticDescriptionVersion description : descriptions)
		{
			Assert.assertTrue(description.dialects.size() > 0);

			Assert.assertEquals(description.getSemanticChronology().referencedComponent.nid.intValue(), MetaData.USER____SOLOR.getNid(), "Wrong concept");

			Assert.assertEquals(description.caseSignificanceConcept.nid.intValue(), MetaData.DESCRIPTION_NOT_CASE_SENSITIVE____SOLOR.getNid(),
					"Wrong case sentivity");

			Assert.assertEquals(description.languageConcept.nid.intValue(), MetaData.ENGLISH_LANGUAGE____SOLOR.getNid(), "Wrong language");

			Assert.assertTrue(description.text.equals("User") || description.text.equals("User (SOLOR)"), "Wrong text " + description.text);

			Assert.assertTrue(description.descriptionTypeConcept.nid == MetaData.REGULAR_NAME_DESCRIPTION_TYPE____SOLOR.getNid()
					|| description.descriptionTypeConcept.nid == MetaData.FULLY_QUALIFIED_NAME_DESCRIPTION_TYPE____SOLOR.getNid(), "Wrong description type");

			// validate that the dialect bits are put together properly
			Assert.assertEquals(description.getSemanticChronology().assemblage.nid.intValue(), MetaData.ENGLISH_LANGUAGE____SOLOR.getNid(), "Wrong language");

			boolean foundPreferredDialect = false;
			boolean foundUsEnglishDialect = false;
			for (RestDynamicSemanticVersion dialect : description.dialects)
			{
				if (dialect.getSemanticChronology().assemblage.nid == MetaData.US_ENGLISH_DIALECT____SOLOR.getNid())
				{
					foundUsEnglishDialect = true;
				}
				for (RestDynamicSemanticData data : dialect.getDataColumns())
				{
					if (data instanceof RestDynamicSemanticNid)
					{
						if (((RestDynamicSemanticNid) data).getNid() == MetaData.PREFERRED____SOLOR.getNid())
						{
							foundPreferredDialect = true;
						}
					}
				}
			}
			Assert.assertTrue(foundPreferredDialect, "Preferred dialect not found");
			Assert.assertTrue(foundUsEnglishDialect, "US English dialect not found");
		}
	}

	@Test
	public void testCoordinateTokenRoundTrip() throws Exception
	{
		ManifoldCoordinate taxonomyCoordinate = ManifoldCoordinates.getStatedManifoldCoordinate(StampCoordinates.getDevelopmentLatest(),
				LanguageCoordinates.getUsEnglishLanguagePreferredTermCoordinate(), LogicCoordinates.getStandardElProfile());
		CoordinatesToken t = CoordinatesTokens.getOrCreate(taxonomyCoordinate.getStampCoordinate(), taxonomyCoordinate.getLanguageCoordinate(),
				taxonomyCoordinate.getLogicCoordinate(), taxonomyCoordinate.getTaxonomyPremiseType());

		String token = t.getSerialized();

		CoordinatesToken read = CoordinatesTokens.getOrCreate(token);
		Assert.assertTrue(token.equals(read.getSerialized()));
	}

	@Test
	public void testCoordinatesToken()
	{
		/*
		 * These tests display the following values on fail
		 * Each test should initialize or set-to-null each of the following values
		 */
		Map<String, Object> parameters = new HashMap<>();
		WebTarget target = null;
		String result = null;
		RestCoordinatesToken retrievedToken = null;
		CoordinatesToken defaultTokenObject = null;
		RestCoordinatesToken defaultToken = null;
		try
		{
			defaultToken = new RestCoordinatesToken(CoordinatesTokens.getDefaultCoordinatesToken());
			defaultTokenObject = CoordinatesTokens.getOrCreate(defaultToken.token);

			// Test no parameters against default token
			parameters.clear();
			result = checkFail((target = target(coordinatesTokenRequestPath)).request().header(Header.Accept.toString(), MediaType.APPLICATION_XML).get())
					.readEntity(String.class);
			retrievedToken = XMLUtils.unmarshalObject(RestCoordinatesToken.class, result);
			Assert.assertTrue(retrievedToken.equals(defaultToken));

			// Test default token passed as argument against default token
			result = checkFail((target = target(coordinatesTokenRequestPath, parameters = buildParams(param(RequestParameters.coordToken, defaultToken.token))))
					.request().header(Header.Accept.toString(), MediaType.APPLICATION_XML).get()).readEntity(String.class);
			retrievedToken = XMLUtils.unmarshalObject(RestCoordinatesToken.class, result);
			Assert.assertTrue(retrievedToken.equals(defaultToken));

			// Test default token passed as argument against default token
			result = checkFail((target = target(coordinatesTokenRequestPath, parameters = buildParams(param(RequestParameters.coordToken, defaultToken.token))))
					.request().header(Header.Accept.toString(), MediaType.APPLICATION_XML).get()).readEntity(String.class);
			retrievedToken = XMLUtils.unmarshalObject(RestCoordinatesToken.class, result);
			Assert.assertTrue(retrievedToken.equals(defaultToken));

			// Compare retrieved coordinates with default generated by default token
			parameters.clear();
			result = checkFail((target = target(taxonomyCoordinateRequestPath)).request().header(Header.Accept.toString(), MediaType.APPLICATION_XML).get())
					.readEntity(String.class);
			RestManifoldCoordinate retrievedTaxonomyCoordinate = XMLUtils.unmarshalObject(RestManifoldCoordinate.class, result);
			RestManifoldCoordinate defaultTaxonomyCoordinate = new RestManifoldCoordinate(defaultTokenObject.getManifoldCoordinate());

			Assert.assertEquals(retrievedTaxonomyCoordinate, defaultTaxonomyCoordinate);

			// Test passing a custom parameter
			// First ensure that the parameters we pass are not already in the default
			Assert.assertNotEquals(defaultTokenObject.getStampCoordinate().getStampPrecedence(), StampPrecedence.TIME);
			Assert.assertNotEquals(defaultTokenObject.getManifoldCoordinate().getTaxonomyPremiseType(), PremiseType.INFERRED);


			result = checkFail((target = target(coordinatesTokenRequestPath,
					parameters = buildParams(param(RequestParameters.precedence, StampPrecedence.TIME.name()), param(RequestParameters.stated, "false"))))
							.request().header(Header.Accept.toString(), MediaType.APPLICATION_XML).get()).readEntity(String.class);
			retrievedToken = XMLUtils.unmarshalObject(RestCoordinatesToken.class, result);
			Assert.assertTrue(CoordinatesTokens.getOrCreate(retrievedToken.token).getStampPrecedence() == StampPrecedence.TIME);
			Assert.assertTrue(CoordinatesTokens.getOrCreate(retrievedToken.token).getTaxonomyType() == PremiseType.INFERRED);

			// Test using a customized token with getStampPrecedence() == StampPrecedence.TIME
			// and getTaxonomyType() == PremiseType.INFERRED
			result = checkFail(
					(target = target(taxonomyCoordinateRequestPath, parameters = buildParams(param(RequestParameters.coordToken, retrievedToken.token))))
							.request().header(Header.Accept.toString(), MediaType.APPLICATION_XML).get()).readEntity(String.class);
			retrievedTaxonomyCoordinate = XMLUtils.unmarshalObject(RestManifoldCoordinate.class, result);
			Assert.assertTrue(retrievedTaxonomyCoordinate.stampCoordinate.precedence.enumId == StampPrecedence.TIME.ordinal());
			Assert.assertTrue(retrievedTaxonomyCoordinate.stated == false);
		}
		catch (Throwable error)
		{
			System.out.println("Failing target: " + target);
			System.out.println("Failing parameters: " + parameters);
			System.out.println("Failing result XML: " + result);
			System.out.println("Failing retrievedToken: " + retrievedToken);
			System.out.println("Failing defaultToken: " + defaultToken);
			System.out.println("Failing defaultTokenObject: " + defaultTokenObject);
			throw new Error(error);
		}
	}
	
	@Test
	public void testGetCoordinatesDialects()
	{
		Map<String, Object> parameters = new HashMap<>();
		WebTarget target = null;
		String requestUrl = null;
		try
		{
			// Get token with specified non-default descriptionTypePrefs (REGULAR_NAME_DESCRIPTION_TYPE____SOLOR,FQN)
			// then test token passed as argument along with RequestParameters.stated parameter
			//with recursion on...
			String result = checkFail((target = target(requestUrl = coordinatesTokenRequestPath,
					parameters = buildParams(param(RequestParameters.dialectPrefs, "us,recurse")))).request()
							.header(Header.Accept.toString(), MediaType.APPLICATION_XML).get()).readEntity(String.class);
			RestCoordinatesToken coordToken = XMLUtils.unmarshalObject(RestCoordinatesToken.class, result);
	
			//validate the dialects returns in the token
			result = checkFail((target = target(requestUrl = languageCoordinateRequestPath,
					parameters = buildParams(param(RequestParameters.coordToken, coordToken.token)))).request()
							.header(Header.Accept.toString(), MediaType.APPLICATION_XML).get()).readEntity(String.class);
			RestLanguageCoordinate retrievedLanguageCoordinate = XMLUtils.unmarshalObject(RestLanguageCoordinate.class, result);
			Assert.assertTrue(retrievedLanguageCoordinate.dialectAssemblagePreferences.length == 2);
			Assert.assertTrue(retrievedLanguageCoordinate.dialectAssemblagePreferences[0].nid == MetaData.US_ENGLISH_DIALECT____SOLOR.getNid());
			Assert.assertTrue(retrievedLanguageCoordinate.dialectAssemblagePreferences[1].nid == MetaData.US_NURSING_DIALECT____SOLOR.getNid());
			
			//recursion off
			result = checkFail((target = target(requestUrl = coordinatesTokenRequestPath,
					parameters = buildParams(param(RequestParameters.dialectPrefs, "us")))).request()
							.header(Header.Accept.toString(), MediaType.APPLICATION_XML).get()).readEntity(String.class);
			coordToken = XMLUtils.unmarshalObject(RestCoordinatesToken.class, result);
	
			//validate the dialects returns in the token
			result = checkFail((target = target(requestUrl = languageCoordinateRequestPath,
					parameters = buildParams(param(RequestParameters.coordToken, coordToken.token)))).request()
							.header(Header.Accept.toString(), MediaType.APPLICATION_XML).get()).readEntity(String.class);
			retrievedLanguageCoordinate = XMLUtils.unmarshalObject(RestLanguageCoordinate.class, result);
			Assert.assertTrue(retrievedLanguageCoordinate.dialectAssemblagePreferences.length == 1);
			Assert.assertTrue(retrievedLanguageCoordinate.dialectAssemblagePreferences[0].nid == MetaData.US_ENGLISH_DIALECT____SOLOR.getNid());
			
			//by UUID with recursion
			result = checkFail((target = target(requestUrl = coordinatesTokenRequestPath,
					parameters = buildParams(param(RequestParameters.dialectPrefs, 
							"recurse," + MetaData.KOREAN_DIALECT____SOLOR.getPrimordialUuid().toString())))).request()
							.header(Header.Accept.toString(), MediaType.APPLICATION_XML).get()).readEntity(String.class);
			coordToken = XMLUtils.unmarshalObject(RestCoordinatesToken.class, result);
	
			//validate the dialects returns in the token
			result = checkFail((target = target(requestUrl = languageCoordinateRequestPath,
					parameters = buildParams(param(RequestParameters.coordToken, coordToken.token)))).request()
							.header(Header.Accept.toString(), MediaType.APPLICATION_XML).get()).readEntity(String.class);
			retrievedLanguageCoordinate = XMLUtils.unmarshalObject(RestLanguageCoordinate.class, result);
			Assert.assertEquals(retrievedLanguageCoordinate.dialectAssemblagePreferences.length, 2);
			Assert.assertTrue(retrievedLanguageCoordinate.dialectAssemblagePreferences[0].nid == MetaData.KOREAN_DIALECT____SOLOR.getNid());
			Assert.assertTrue(retrievedLanguageCoordinate.dialectAssemblagePreferences[1].nid == MetaData.STANDARD_KOREAN_DIALECT____SOLOR.getNid());
		}
		catch (Throwable error)
		{
			System.out.println("Failing request target: " + target);
			System.out.println("Failing request URL: " + requestUrl);
			System.out.println("Failing request parameters: " + parameters);
			throw error;
		}
	}

	@Test
	public void testGetCoordinates()
	{
		/*
		 * These tests display the following values on fail
		 * Each test should initialize or set-to-null each of the following values
		 */
		Map<String, Object> parameters = new HashMap<>();
		WebTarget target = null;
		String xpath = null;
		Node node = null;
		NodeList nodeList = null;
		String result = null;
		String requestUrl = null;
		try
		{
			// RestManifoldCoordinate
			boolean taxonomyCoordinateStated;
			result = checkFail((target = target(requestUrl = taxonomyCoordinateRequestPath, parameters = buildParams(param(RequestParameters.stated, "false"))))
					.request().header(Header.Accept.toString(), MediaType.APPLICATION_XML).get()).readEntity(String.class);
			xpath = "/restManifoldCoordinate/stated";
			node = XMLUtils.getNodeFromXml(result, xpath);
			Assert.assertTrue(node != null, "No nodes parsed from " + result);
			taxonomyCoordinateStated = Boolean.valueOf(node.getTextContent());
			Assert.assertTrue(taxonomyCoordinateStated == false);

			result = checkFail((target = target(requestUrl = taxonomyCoordinateRequestPath, parameters = buildParams(param(RequestParameters.stated, "true"))))
					.request().header(Header.Accept.toString(), MediaType.APPLICATION_XML).get()).readEntity(String.class);
			node = XMLUtils.getNodeFromXml(result, xpath);
			taxonomyCoordinateStated = Boolean.valueOf(node.getTextContent());
			Assert.assertTrue(taxonomyCoordinateStated == true);

			// RestStampCoordinate
			result = checkFail((target = target(requestUrl = stampCoordinateRequestPath,
					parameters = buildParams(param(RequestParameters.time, 123456789), param(RequestParameters.precedence, StampPrecedence.TIME),
							param(RequestParameters.modules,
									MetaData.LOINC_MODULES____SOLOR.getNid() + "," + MetaData.CORE_METADATA_MODULE____SOLOR.getNid() + ","
											+ MetaData.SNOMED_CT_CORE_MODULES____SOLOR.getNid() + "," + MetaData.KOMET_MODULE____SOLOR.getNid()
											+ "," + MetaData.SOLOR_MODULE____SOLOR.getNid()),
							param(RequestParameters.allowedStates,
									sh.isaac.api.Status.INACTIVE.getAbbreviation() + "," + sh.isaac.api.Status.PRIMORDIAL.getAbbreviation())))).request()
											.header(Header.Accept.toString(), MediaType.APPLICATION_XML).get()).readEntity(String.class);
			xpath = "/restStampCoordinate/time";
			node = XMLUtils.getNodeFromXml(result, xpath);
			long stampCoordinateTime = Long.parseLong(node.getTextContent());
			Assert.assertTrue(stampCoordinateTime == 123456789);
			xpath = "/restStampCoordinate/modules/nid";
			List<Integer> stampCoordinateModules = new ArrayList<>();
			node = null;
			nodeList = XMLUtils.getNodeSetFromXml(result, xpath);
			for (int i = 0; i < nodeList.getLength(); ++i)
			{
				stampCoordinateModules.add(Integer.valueOf(nodeList.item(i).getTextContent()));
			}
			Assert.assertEquals(stampCoordinateModules.size(), 11);
			Assert.assertTrue(stampCoordinateModules.contains(MetaData.LOINC_MODULES____SOLOR.getNid()));
			Assert.assertTrue(stampCoordinateModules.contains(MetaData.CORE_METADATA_MODULE____SOLOR.getNid()));
			Assert.assertTrue(stampCoordinateModules.contains(MetaData.SNOMED_CT_CORE_MODULES____SOLOR.getNid()));
			//There are a bunch of other new modules under solor modules now.

			xpath = "/restStampCoordinate/allowedStates/enumId";
			List<Integer> allowedStates = new ArrayList<>();
			node = null;
			nodeList = XMLUtils.getNodeSetFromXml(result, xpath);
			for (int i = 0; i < nodeList.getLength(); ++i)
			{
				allowedStates.add(Integer.valueOf(nodeList.item(i).getTextContent()));
			}
			Assert.assertTrue(allowedStates.size() == 2);
			Assert.assertTrue(allowedStates.contains(0));
			Assert.assertTrue(allowedStates.contains(2));

			// LanguageCoordinate
			// language
			result = checkFail((target = target(requestUrl = languageCoordinateRequestPath,
					parameters = buildParams(param(RequestParameters.language, MetaData.ENGLISH_LANGUAGE____SOLOR.getNid())))).request()
							.header(Header.Accept.toString(), MediaType.APPLICATION_XML).get()).readEntity(String.class);
			RestLanguageCoordinate retrievedLanguageCoordinate = XMLUtils.unmarshalObject(RestLanguageCoordinate.class, result);
			Assert.assertTrue(retrievedLanguageCoordinate.language.nid == MetaData.ENGLISH_LANGUAGE____SOLOR.getNid());

			// descriptionTypePrefs
			result = checkFail((target = target(requestUrl = languageCoordinateRequestPath,
					parameters = buildParams(param(RequestParameters.descriptionTypePrefs, "fqn,regular")))).request()
							.header(Header.Accept.toString(), MediaType.APPLICATION_XML).get()).readEntity(String.class);
			retrievedLanguageCoordinate = XMLUtils.unmarshalObject(RestLanguageCoordinate.class, result);
			Assert.assertTrue(retrievedLanguageCoordinate.descriptionTypePreferences.length == 2);
			Assert.assertTrue(retrievedLanguageCoordinate.descriptionTypePreferences[0].nid == MetaData.FULLY_QUALIFIED_NAME_DESCRIPTION_TYPE____SOLOR.getNid());
			Assert.assertTrue(retrievedLanguageCoordinate.descriptionTypePreferences[1].nid == MetaData.REGULAR_NAME_DESCRIPTION_TYPE____SOLOR.getNid());

			// descriptionTypePrefs (reversed)
			result = checkFail((target = target(requestUrl = languageCoordinateRequestPath,
					parameters = buildParams(param(RequestParameters.descriptionTypePrefs, "regular,fqn")))).request()
							.header(Header.Accept.toString(), MediaType.APPLICATION_XML).get()).readEntity(String.class);
			retrievedLanguageCoordinate = XMLUtils.unmarshalObject(RestLanguageCoordinate.class, result);
			Assert.assertTrue(retrievedLanguageCoordinate.descriptionTypePreferences.length == 2);
			Assert.assertTrue(retrievedLanguageCoordinate.descriptionTypePreferences[0].nid == MetaData.REGULAR_NAME_DESCRIPTION_TYPE____SOLOR.getNid());
			Assert.assertTrue(retrievedLanguageCoordinate.descriptionTypePreferences[1].nid == MetaData.FULLY_QUALIFIED_NAME_DESCRIPTION_TYPE____SOLOR.getNid());

			// Get token with specified non-default descriptionTypePrefs (REGULAR_NAME_DESCRIPTION_TYPE____SOLOR,FQN)
			// then test token passed as argument along with RequestParameters.stated parameter
			result = checkFail((target = target(requestUrl = coordinatesTokenRequestPath,
					parameters = buildParams(param(RequestParameters.descriptionTypePrefs, "regular,fqn")))).request()
							.header(Header.Accept.toString(), MediaType.APPLICATION_XML).get()).readEntity(String.class);
			final RestCoordinatesToken synonymDescriptionPreferredToken = XMLUtils.unmarshalObject(RestCoordinatesToken.class, result);
			// Get token with specified default descriptionTypePrefs (FQN,REGULAR_NAME_DESCRIPTION_TYPE____SOLOR)
			parameters.clear();
			parameters.put(RequestParameters.descriptionTypePrefs, "fqn,regular");
			result = checkFail((target = target(requestUrl = coordinatesTokenRequestPath, parameters)).request()
					.header(Header.Accept.toString(), MediaType.APPLICATION_XML).get()).readEntity(String.class);
			final RestCoordinatesToken fqnDescriptionPreferredToken = XMLUtils.unmarshalObject(RestCoordinatesToken.class, result);

			// confirm that constructed token has descriptionTypePrefs ordered as in
			// parameters used to construct token
			result = checkFail((target = target(requestUrl = languageCoordinateRequestPath,
					parameters = buildParams(param(RequestParameters.coordToken, synonymDescriptionPreferredToken.token)))).request()
							.header(Header.Accept.toString(), MediaType.APPLICATION_XML).get()).readEntity(String.class);
			retrievedLanguageCoordinate = XMLUtils.unmarshalObject(RestLanguageCoordinate.class, result);
			Assert.assertTrue(retrievedLanguageCoordinate.descriptionTypePreferences.length == 2);
			Assert.assertTrue(retrievedLanguageCoordinate.descriptionTypePreferences[0].nid == MetaData.REGULAR_NAME_DESCRIPTION_TYPE____SOLOR.getNid());
			Assert.assertTrue(retrievedLanguageCoordinate.descriptionTypePreferences[1].nid == MetaData.FULLY_QUALIFIED_NAME_DESCRIPTION_TYPE____SOLOR.getNid());

			// test token passed as argument along with RequestParameters.stated parameter
			// ensure that descriptionTypePrefs order specified in token is maintained
			result = checkFail((target = target(requestUrl = languageCoordinateRequestPath,
					parameters = buildParams(param(RequestParameters.coordToken, synonymDescriptionPreferredToken.token),
							param(RequestParameters.stated, "true")))).request().header(Header.Accept.toString(), MediaType.APPLICATION_XML).get())
									.readEntity(String.class);
			retrievedLanguageCoordinate = XMLUtils.unmarshalObject(RestLanguageCoordinate.class, result);
			Assert.assertTrue(retrievedLanguageCoordinate.descriptionTypePreferences.length == 2);
			Assert.assertTrue(retrievedLanguageCoordinate.descriptionTypePreferences[0].nid == MetaData.REGULAR_NAME_DESCRIPTION_TYPE____SOLOR.getNid());
			Assert.assertTrue(retrievedLanguageCoordinate.descriptionTypePreferences[1].nid == MetaData.FULLY_QUALIFIED_NAME_DESCRIPTION_TYPE____SOLOR.getNid());

			// test passed descriptionTypePrefs on taxonomy
			// test regular as preference
			result = checkFail((target = target(requestUrl = taxonomyRequestPath,
					parameters = buildParams(param(RequestParameters.childDepth, 1), param(RequestParameters.descriptionTypePrefs, "regular,fqn")))).request()
							.header(Header.Accept.toString(), MediaType.APPLICATION_XML).get()).readEntity(String.class);
			xpath = "/restConceptVersion/children/results/conChronology[identifiers/nid=" + MetaData.HEALTH_CONCEPT____SOLOR.getNid() + "]/description";
			node = XMLUtils.getNodeFromXml(result, xpath);
			nodeList = null;
			Assert.assertTrue(node != null && node.getNodeType() == Node.ELEMENT_NODE);
			Assert.assertTrue(node.getTextContent().equals(MetaData.HEALTH_CONCEPT____SOLOR.getRegularName().get()));

			// test fqn as preference using token
			result = checkFail((target = target(requestUrl = taxonomyRequestPath,
					parameters = buildParams(param(RequestParameters.childDepth, 1), param(RequestParameters.coordToken, fqnDescriptionPreferredToken.token))))
							.request().header(Header.Accept.toString(), MediaType.APPLICATION_XML).get()).readEntity(String.class);
			xpath = "/restConceptVersion/children/results/conChronology[identifiers/nid=" + MetaData.HEALTH_CONCEPT____SOLOR.getNid() + "]/description";
			node = XMLUtils.getNodeFromXml(result, xpath);
			nodeList = null;
			Assert.assertTrue(node != null && node.getNodeType() == Node.ELEMENT_NODE);
			Assert.assertTrue(node.getTextContent().equals(MetaData.HEALTH_CONCEPT____SOLOR.getRegularName().get() + " (SOLOR)"));

			// test fqn as preference
			result = checkFail((target = target(requestUrl = taxonomyRequestPath,
					parameters = buildParams(param(RequestParameters.childDepth, 1), param(RequestParameters.descriptionTypePrefs, "fqn,regular")))).request()
							.header(Header.Accept.toString(), MediaType.APPLICATION_XML).get()).readEntity(String.class);
			xpath = "/restConceptVersion/children/results/conChronology[identifiers/nid=" + MetaData.HEALTH_CONCEPT____SOLOR.getNid() + "]/description";
			node = XMLUtils.getNodeFromXml(result, xpath);
			nodeList = null;
			Assert.assertTrue(node != null && node.getNodeType() == Node.ELEMENT_NODE);
			Assert.assertEquals(node.getTextContent(), MetaData.HEALTH_CONCEPT____SOLOR.getRegularName().get() + " (SOLOR)");

			// LogicCoordinate
			result = checkFail((target = target(requestUrl = logicCoordinateRequestPath,
					parameters = buildParams(param(RequestParameters.classifier, MetaData.SNOROCKET_CLASSIFIER____SOLOR.getNid())))).request()
							.header(Header.Accept.toString(), MediaType.APPLICATION_XML).get()).readEntity(String.class);
			RestLogicCoordinate retrievedLogicCoordinate = XMLUtils.unmarshalObject(RestLogicCoordinate.class, result);
			Assert.assertTrue(retrievedLogicCoordinate.classifier.nid == MetaData.SNOROCKET_CLASSIFIER____SOLOR.getNid());
		}
		catch (Throwable error)
		{
			System.out.println("Failing request target: " + target);
			System.out.println("Failing request URL: " + requestUrl);
			System.out.println("Failing request parameters: " + parameters);
			System.out.println("Failing result XPath: " + xpath);
			System.out.println("Failing result Node: " + XMLUtils.toString(node));
			System.out.println("Failing result NodeList: " + XMLUtils.toString(nodeList));
			System.out.println("Failing result XML: " + result);

			throw error;
		}
	}

	@Test
	public void testSystemAPIs()
	{
		/*
		 * These tests display the following values on fail
		 * Each test should initialize or set-to-null each of the following values
		 */
		Map<String, Object> parameters = new HashMap<>();
		WebTarget target = null;
		String result = null;
		String requestUrl = null;
		RestIdentifiedObjectsResult identifiedObjectsResult = null;
		RestSystemInfo systemInfo = null;
		try
		{
			// Get a semantic chronology by assemblage and extract one of its UUIDs
			result = checkFail(
					(target = target(requestUrl = semanticByAssemblageRequestPath + DynamicConstants.get().DYNAMIC_EXTENSION_DEFINITION.getPrimordialUuid(),
							parameters = buildParams(param(RequestParameters.expand, "chronology"), // Expand the chronology
									param(RequestParameters.maxPageSize, 1)))) // Request exactly 1 result
											.request().header(Header.Accept.toString(), MediaType.APPLICATION_XML).get()).readEntity(String.class);
			RestSemanticVersionPage semanticVersions = XMLUtils.unmarshalObject(RestSemanticVersionPage.class, result);
			UUID semanticUuid = semanticVersions.results[0].getSemanticChronology().identifiers.getFirst();

			// Test objectChronologyType of specified semantic UUID
			result = checkFail(
					(target = target(requestUrl = RestPaths.systemAPIsPathComponent + RestPaths.objectChronologyTypeComponent + semanticUuid.toString()))
							.request().header(Header.Accept.toString(), MediaType.APPLICATION_XML).get()).readEntity(String.class);
			RestObjectChronologyType objectChronologyType = XMLUtils.unmarshalObject(RestObjectChronologyType.class, result);
			// Test RestObjectChronologyType name
			Assert.assertTrue(objectChronologyType.toString().equalsIgnoreCase(IsaacObjectType.SEMANTIC.name()));
			// Test RestObjectChronologyType enumId ordinal
			Assert.assertTrue(objectChronologyType.enumId == IsaacObjectType.SEMANTIC.ordinal());

			// Test objectChronologyType of specified concept UUID
			result = checkFail((target = target(requestUrl = RestPaths.systemAPIsPathComponent + RestPaths.objectChronologyTypeComponent
					+ MetaData.SOLOR_CONCEPT____SOLOR.getPrimordialUuid().toString())).request().header(Header.Accept.toString(), MediaType.APPLICATION_XML)
							.get()).readEntity(String.class);
			objectChronologyType = XMLUtils.unmarshalObject(RestObjectChronologyType.class, result);
			// Test RestObjectChronologyType name
			Assert.assertTrue(objectChronologyType.toString().equalsIgnoreCase(IsaacObjectType.CONCEPT.name()));
			// Test RestObjectChronologyType enumId ordinal
			Assert.assertTrue(objectChronologyType.enumId == IsaacObjectType.CONCEPT.ordinal());

			// Test SystemInfo
			result = checkFail((target = target(requestUrl = RestPaths.systemAPIsPathComponent + RestPaths.systemInfoComponent)).request()
					.header(Header.Accept.toString(), MediaType.APPLICATION_XML).get()).readEntity(String.class);
			systemInfo = XMLUtils.unmarshalObject(RestSystemInfo.class, result);
			Assert.assertTrue(systemInfo.getSupportedAPIVersions().length > 0 && !StringUtils.isBlank(systemInfo.getSupportedAPIVersions()[0]));
			// TODO find a way to fully test systemInfoComponent with non-test configuration
			// Assert.assertTrue(! StringUtils.isBlank(systemInfo.apiImplementationVersion));
			// Assert.assertTrue(! StringUtils.isBlank(systemInfo.isaacVersion));
			// Assert.assertTrue(! StringUtils.isBlank(systemInfo.scmUrl));
			// Assert.assertNotNull(systemInfo.isaacDbDependency);
			// Assert.assertTrue(! StringUtils.isBlank(systemInfo.isaacDbDependency.groupId));
			// Assert.assertTrue(! StringUtils.isBlank(systemInfo.isaacDbDependency.artifactId));
			// Assert.assertTrue(! StringUtils.isBlank(systemInfo.isaacDbDependency.type));
			// Assert.assertTrue(! StringUtils.isBlank(systemInfo.isaacDbDependency.version));
			// Assert.assertTrue(systemInfo.appLicenses.size() > 0);
			// for (RestLicenseInfo licenseInfo : systemInfo.appLicenses) {
			// Assert.assertTrue(! StringUtils.isBlank(licenseInfo.name));
			// Assert.assertTrue(! StringUtils.isBlank(licenseInfo.url));
			// Assert.assertTrue(! StringUtils.isBlank(licenseInfo.comments));
			// }
			// for (RestDependencyInfo dependencyInfo : systemInfo.dbDependencies) {
			// Assert.assertTrue(! StringUtils.isBlank(dependencyInfo.groupId));
			// Assert.assertTrue(! StringUtils.isBlank(dependencyInfo.artifactId));
			// Assert.assertTrue(! StringUtils.isBlank(dependencyInfo.type));
			// Assert.assertTrue(! StringUtils.isBlank(dependencyInfo.version));
			// }
			// for (RestLicenseInfo licenseInfo : systemInfo.dbLicenses) {
			// Assert.assertTrue(! StringUtils.isBlank(licenseInfo.name));
			// Assert.assertTrue(! StringUtils.isBlank(licenseInfo.url));
			// Assert.assertTrue(! StringUtils.isBlank(licenseInfo.comments));
			// }

			// Test identifiedObjectsComponent request of specified semantic UUID
			result = checkFail(
					(target = target(requestUrl = RestPaths.systemAPIsPathComponent + RestPaths.identifiedObjectsComponent + semanticUuid.toString())).request()
							.header(Header.Accept.toString(), MediaType.APPLICATION_XML).get()).readEntity(String.class);
			identifiedObjectsResult = XMLUtils.unmarshalObject(RestIdentifiedObjectsResult.class, result);
			// Test RestSemanticChronology
			Assert.assertTrue(identifiedObjectsResult.getSemanticChronology().identifiers.uuids.contains(semanticUuid));
			Assert.assertNull(identifiedObjectsResult.getConcept());

			// Test identifiedObjectsComponent request of specified concept UUID
			result = checkFail((target = target(requestUrl = RestPaths.systemAPIsPathComponent + RestPaths.identifiedObjectsComponent
					+ MetaData.SOLOR_CONCEPT____SOLOR.getPrimordialUuid().toString())).request().header(Header.Accept.toString(), MediaType.APPLICATION_XML)
							.get()).readEntity(String.class);
			identifiedObjectsResult = XMLUtils.unmarshalObject(RestIdentifiedObjectsResult.class, result);
			// Test RestSemanticChronology
			Assert.assertTrue(identifiedObjectsResult.getConcept().getIdentifiers().uuids.contains(MetaData.SOLOR_CONCEPT____SOLOR.getPrimordialUuid()));
			Assert.assertNull(identifiedObjectsResult.getSemanticChronology());
		}
		catch (Throwable error)
		{
			System.out.println("Failing request target: " + target);
			System.out.println("Failing request URL: " + requestUrl);
			System.out.println("Failing request parameters: " + parameters);
			System.out.println("Failing result XML: " + result);
			System.out.println("Failing identified objects result: " + identifiedObjectsResult);
			System.out.println("Failing systemInfo result: " + systemInfo);

			throw error;
		}
	}

	@Test
	public void testParameterValidation()
	{
		/*
		 * These tests display the following values on fail
		 * Each test should initialize or set-to-null each of the following values
		 */
		Map<String, Object> parameters = new HashMap<>();
		WebTarget target = null;
		String result = null;
		String requestUrl = null;
		Optional<String> caughtExceptionMessage = Optional.empty();
		try
		{
			// Test any call with valid parameters
			result = checkFail(
					(target = target(requestUrl = semanticByAssemblageRequestPath + DynamicConstants.get().DYNAMIC_EXTENSION_DEFINITION.getPrimordialUuid(),
							parameters = buildParams(param(RequestParameters.expand, "chronology"), // Expand the chronology
									param(RequestParameters.maxPageSize, 1)))) // Request exactly 1 result
											.request().header(Header.Accept.toString(), MediaType.APPLICATION_XML).get()).readEntity(String.class);

			// Test same call with a bogus additional parameter
			String badParamName = "bogusParam";
			String badParamValue = "testValue";
			try
			{
				result = checkFail(
						(target = target(requestUrl = semanticByAssemblageRequestPath + DynamicConstants.get().DYNAMIC_EXTENSION_DEFINITION.getPrimordialUuid(),
								parameters = buildParams(param(RequestParameters.expand, "chronology"), // Expand the chronology
										param(RequestParameters.maxPageSize, 1), param("bogusParam", "testValue")))).request()
												.header(Header.Accept.toString(), MediaType.APPLICATION_XML).get()).readEntity(String.class);
			}
			catch (Throwable t)
			{
				caughtExceptionMessage = getCaughtParameterValidationExceptionMessage(badParamName, badParamValue, t);
			}
			Assert.assertTrue(caughtExceptionMessage.isPresent());

			// Test same call with a valid parameter made uppercase when IGNORE_CASE_VALIDATING_PARAM_NAMES == false
			badParamName = RequestParameters.maxPageSize.toUpperCase();
			badParamValue = "1";
			RequestParameters.IGNORE_CASE_VALIDATING_PARAM_NAMES = false;
			caughtExceptionMessage = Optional.empty();
			try
			{
				result = checkFail(
						(target = target(requestUrl = semanticByAssemblageRequestPath + DynamicConstants.get().DYNAMIC_EXTENSION_DEFINITION.getPrimordialUuid(),
								parameters = buildParams(param(RequestParameters.expand, "chronology"), // Expand the chronology
										param(badParamName, badParamValue)))).request().header(Header.Accept.toString(), MediaType.APPLICATION_XML).get())
												.readEntity(String.class);
			}
			catch (Throwable t)
			{
				caughtExceptionMessage = getCaughtParameterValidationExceptionMessage(badParamName, badParamValue, t);
			}
			Assert.assertTrue(caughtExceptionMessage.isPresent());

			// Test same call with a valid parameter made uppercase when IGNORE_CASE_VALIDATING_PARAM_NAMES == true
			badParamName = RequestParameters.maxPageSize.toUpperCase();
			badParamValue = "1";
			RequestParameters.IGNORE_CASE_VALIDATING_PARAM_NAMES = true;
			caughtExceptionMessage = Optional.empty();
			try
			{
				result = checkFail(
						(target = target(requestUrl = semanticByAssemblageRequestPath + DynamicConstants.get().DYNAMIC_EXTENSION_DEFINITION.getPrimordialUuid(),
								parameters = buildParams(param(RequestParameters.expand, "chronology"), // Expand the chronology
										param(badParamName, badParamValue)))).request().header(Header.Accept.toString(), MediaType.APPLICATION_XML).get())
												.readEntity(String.class);
			}
			catch (Throwable t)
			{
				caughtExceptionMessage = getCaughtParameterValidationExceptionMessage(badParamName, badParamValue, t);
			}
			Assert.assertTrue(!caughtExceptionMessage.isPresent());
			RequestParameters.IGNORE_CASE_VALIDATING_PARAM_NAMES = RequestParameters.IGNORE_CASE_VALIDATING_PARAM_NAMES_DEFAULT;

		}
		catch (Throwable error)
		{
			System.out.println("Failing request target: " + target);
			System.out.println("Failing request URL: " + requestUrl);
			System.out.println("Failing request parameters: " + parameters);
			System.out.println("Failing result XML: " + result);
			System.out.println("Failing exception: " + caughtExceptionMessage);

			throw error;
		}
		finally
		{
			RequestParameters.IGNORE_CASE_VALIDATING_PARAM_NAMES = RequestParameters.IGNORE_CASE_VALIDATING_PARAM_NAMES_DEFAULT;
		}
	}

	@Test
	public void testModuleSearch() throws RestException
	{

		int requestPageSize = 6;
		String xPathToken = "/restCoordinatesToken/token";

		String result = target(coordinatesTokenRequestPath)
				.queryParam(RequestParameters.modules,
						MetaData.LOINC_MODULES____SOLOR.getNid() + "," + MetaData.CORE_METADATA_MODULE____SOLOR.getNid() + "," + MetaData.VHAT_MODULES____SOLOR.getNid())
				.request().header(Header.Accept.toString(), MediaType.APPLICATION_XML).get().readEntity(String.class);
		Node token1 = XMLUtils.getNodeFromXml(result, xPathToken);
		String stampCoordTokenLoincIsaacVhat = token1.getTextContent();

		result = target(coordinatesTokenRequestPath).queryParam(RequestParameters.modules, MetaData.LOINC_MODULES____SOLOR.getNid()).request()
				.header(Header.Accept.toString(), MediaType.APPLICATION_XML).get().readEntity(String.class);
		Node token2 = XMLUtils.getNodeFromXml(result, xPathToken);
		String stampCoordTokenLoinc = token2.getTextContent();

		result = target(coordinatesTokenRequestPath).queryParam(RequestParameters.modules, MetaData.VHAT_MODULES____SOLOR.getNid()).request()
				.header(Header.Accept.toString(), MediaType.APPLICATION_XML).get().readEntity(String.class);
		Node token3 = XMLUtils.getNodeFromXml(result, xPathToken);
		String stampCoordTokenVhat = token3.getTextContent();

		result = target(coordinatesTokenRequestPath).queryParam(RequestParameters.modules, MetaData.CORE_METADATA_MODULE____SOLOR.getNid()).request()
				.header(Header.Accept.toString(), MediaType.APPLICATION_XML).get().readEntity(String.class);
		Node token4 = XMLUtils.getNodeFromXml(result, xPathToken);
		String stampCoordTokenIsaac = token4.getTextContent();

		String xPathResults = "count(/restSearchResultPage/results)";

		String result1 = target(descriptionSearchRequestPath).queryParam(RequestParameters.coordToken, stampCoordTokenLoincIsaacVhat)
				.queryParam(RequestParameters.query, "dynamic*").queryParam(RequestParameters.maxPageSize, requestPageSize).request()
				.header(Header.Accept.toString(), MediaType.APPLICATION_XML).get().readEntity(String.class);
		double count1 = XMLUtils.getNumberFromXml(result1, xPathResults);
		Assert.assertEquals(count1, (double) requestPageSize);

		String result2 = target(descriptionSearchRequestPath).queryParam(RequestParameters.coordToken, stampCoordTokenLoinc)
				.queryParam(RequestParameters.query, "dynamic*").queryParam(RequestParameters.maxPageSize, requestPageSize).request()
				.header(Header.Accept.toString(), MediaType.APPLICATION_XML).get().readEntity(String.class);
		double count2 = XMLUtils.getNumberFromXml(result2, xPathResults);
		Assert.assertEquals(count2, (double) 0);

		String result3 = target(descriptionSearchRequestPath).queryParam(RequestParameters.coordToken, stampCoordTokenVhat)
				.queryParam(RequestParameters.query, "dynamic*").queryParam(RequestParameters.maxPageSize, requestPageSize).request()
				.header(Header.Accept.toString(), MediaType.APPLICATION_XML).get().readEntity(String.class);
		double count3 = XMLUtils.getNumberFromXml(result3, xPathResults);
		Assert.assertEquals(count3, (double) 0);

		String result4 = target(descriptionSearchRequestPath).queryParam(RequestParameters.coordToken, stampCoordTokenIsaac)
				.queryParam(RequestParameters.query, "dynamic*").queryParam(RequestParameters.maxPageSize, requestPageSize).request()
				.header(Header.Accept.toString(), MediaType.APPLICATION_XML).get().readEntity(String.class);
		double count4 = XMLUtils.getNumberFromXml(result4, xPathResults);
		Assert.assertEquals(count4, (double) requestPageSize);
	}

	@Test
	public void testPathSearch() throws RestException
	{

		int requestPageSize = 6;
		String xPathToken = "/restCoordinatesToken/token";

		String result = target(coordinatesTokenRequestPath).queryParam(RequestParameters.path, MetaData.DEVELOPMENT_PATH____SOLOR.getNid()).request()
				.header(Header.Accept.toString(), MediaType.APPLICATION_XML).get().readEntity(String.class);
		Node token1 = XMLUtils.getNodeFromXml(result, xPathToken);
		String stampCoordTokenDevPath = token1.getTextContent();

		result = target(coordinatesTokenRequestPath).queryParam(RequestParameters.path, MetaData.MASTER_PATH____SOLOR.getNid()).request()
				.header(Header.Accept.toString(), MediaType.APPLICATION_XML).get().readEntity(String.class);
		Node token2 = XMLUtils.getNodeFromXml(result, xPathToken);
		String stampCoordTokenMasterPath = token2.getTextContent();

		String xPathResults = "count(/restSearchResultPage/results)";

		String result1 = target(descriptionSearchRequestPath).queryParam(RequestParameters.coordToken, stampCoordTokenDevPath)
				.queryParam(RequestParameters.query, "dynamic*").queryParam(RequestParameters.maxPageSize, requestPageSize).request()
				.header(Header.Accept.toString(), MediaType.APPLICATION_XML).get().readEntity(String.class);
		double count1 = XMLUtils.getNumberFromXml(result1, xPathResults);
		Assert.assertEquals(count1, (double) requestPageSize);

		String result2 = target(descriptionSearchRequestPath).queryParam(RequestParameters.coordToken, stampCoordTokenMasterPath)
				.queryParam(RequestParameters.query, "dynamic*").queryParam(RequestParameters.maxPageSize, requestPageSize).request()
				.header(Header.Accept.toString(), MediaType.APPLICATION_XML).get().readEntity(String.class);
		double count2 = XMLUtils.getNumberFromXml(result2, xPathResults);
		Assert.assertEquals(count2, (double) 0);
	}

	@Test
	public void testModuleAndPathSearch() throws RestException
	{

		int requestPageSize = 6;
		String xPathToken = "/restCoordinatesToken/token";

		String result = target(coordinatesTokenRequestPath)
				.queryParam(RequestParameters.modules, MetaData.CORE_METADATA_MODULE____SOLOR.getNid() + "," + MetaData.VHAT_MODULES____SOLOR.getNid())
				.queryParam(RequestParameters.path, MetaData.DEVELOPMENT_PATH____SOLOR.getNid()).request()
				.header(Header.Accept.toString(), MediaType.APPLICATION_XML).get().readEntity(String.class);
		Node token1 = XMLUtils.getNodeFromXml(result, xPathToken);
		String stampCoordTokenIsaacVhatDevPath = token1.getTextContent();

		result = target(coordinatesTokenRequestPath)
				.queryParam(RequestParameters.modules, MetaData.CORE_METADATA_MODULE____SOLOR.getNid() + "," + MetaData.VHAT_MODULES____SOLOR.getNid())
				.queryParam(RequestParameters.path, MetaData.MASTER_PATH____SOLOR.getNid()).request()
				.header(Header.Accept.toString(), MediaType.APPLICATION_XML).get().readEntity(String.class);
		Node token2 = XMLUtils.getNodeFromXml(result, xPathToken);
		String stampCoordTokenIsaacVhatMasterPath = token2.getTextContent();

		result = target(coordinatesTokenRequestPath).queryParam(RequestParameters.modules, MetaData.CORE_METADATA_MODULE____SOLOR.getNid())
				.queryParam(RequestParameters.path, MetaData.DEVELOPMENT_PATH____SOLOR.getNid()).request()
				.header(Header.Accept.toString(), MediaType.APPLICATION_XML).get().readEntity(String.class);
		Node token3 = XMLUtils.getNodeFromXml(result, xPathToken);
		String stampCoordTokenIsaacDevPath = token3.getTextContent();

		result = target(coordinatesTokenRequestPath).queryParam(RequestParameters.modules, MetaData.CORE_METADATA_MODULE____SOLOR.getNid())
				.queryParam(RequestParameters.path, MetaData.MASTER_PATH____SOLOR.getNid()).request()
				.header(Header.Accept.toString(), MediaType.APPLICATION_XML).get().readEntity(String.class);
		Node token4 = XMLUtils.getNodeFromXml(result, xPathToken);
		String stampCoordTokenIsaacMasterPath = token4.getTextContent();

		result = target(coordinatesTokenRequestPath).queryParam(RequestParameters.modules, MetaData.VHAT_MODULES____SOLOR.getNid())
				.queryParam(RequestParameters.path, MetaData.DEVELOPMENT_PATH____SOLOR.getNid()).request()
				.header(Header.Accept.toString(), MediaType.APPLICATION_XML).get().readEntity(String.class);
		Node token5 = XMLUtils.getNodeFromXml(result, xPathToken);
		String stampCoordTokenVhatDevPath = token5.getTextContent();

		result = target(coordinatesTokenRequestPath).queryParam(RequestParameters.modules, MetaData.VHAT_MODULES____SOLOR.getNid())
				.queryParam(RequestParameters.path, MetaData.MASTER_PATH____SOLOR.getNid()).request()
				.header(Header.Accept.toString(), MediaType.APPLICATION_XML).get().readEntity(String.class);
		Node token6 = XMLUtils.getNodeFromXml(result, xPathToken);
		String stampCoordTokenVhatMasterPath = token6.getTextContent();

		String xPathResults = "count(/restSearchResultPage/results)";

		String result1 = target(descriptionSearchRequestPath).queryParam(RequestParameters.coordToken, stampCoordTokenIsaacVhatDevPath)
				.queryParam(RequestParameters.query, "dynamic*").queryParam(RequestParameters.maxPageSize, requestPageSize).request()
				.header(Header.Accept.toString(), MediaType.APPLICATION_XML).get().readEntity(String.class);
		double count1 = XMLUtils.getNumberFromXml(result1, xPathResults);
		Assert.assertEquals(count1, (double) requestPageSize);

		String result2 = target(descriptionSearchRequestPath).queryParam(RequestParameters.coordToken, stampCoordTokenIsaacVhatMasterPath)
				.queryParam(RequestParameters.query, "dynamic*").queryParam(RequestParameters.maxPageSize, requestPageSize).request()
				.header(Header.Accept.toString(), MediaType.APPLICATION_XML).get().readEntity(String.class);
		double count2 = XMLUtils.getNumberFromXml(result2, xPathResults);
		Assert.assertEquals(count2, (double) 0);

		String result3 = target(descriptionSearchRequestPath).queryParam(RequestParameters.coordToken, stampCoordTokenIsaacDevPath)
				.queryParam(RequestParameters.query, "dynamic*").queryParam(RequestParameters.maxPageSize, requestPageSize).request()
				.header(Header.Accept.toString(), MediaType.APPLICATION_XML).get().readEntity(String.class);
		double count3 = XMLUtils.getNumberFromXml(result3, xPathResults);
		Assert.assertEquals(count3, (double) requestPageSize);

		String result4 = target(descriptionSearchRequestPath).queryParam(RequestParameters.coordToken, stampCoordTokenIsaacMasterPath)
				.queryParam(RequestParameters.query, "dynamic*").queryParam(RequestParameters.maxPageSize, requestPageSize).request()
				.header(Header.Accept.toString(), MediaType.APPLICATION_XML).get().readEntity(String.class);
		double count4 = XMLUtils.getNumberFromXml(result4, xPathResults);
		Assert.assertEquals(count4, (double) 0);

		String result5 = target(descriptionSearchRequestPath).queryParam(RequestParameters.coordToken, stampCoordTokenVhatDevPath)
				.queryParam(RequestParameters.query, "dynamic*").queryParam(RequestParameters.maxPageSize, requestPageSize).request()
				.header(Header.Accept.toString(), MediaType.APPLICATION_XML).get().readEntity(String.class);
		double count5 = XMLUtils.getNumberFromXml(result5, xPathResults);
		Assert.assertEquals(count5, (double) 0);

		String result6 = target(descriptionSearchRequestPath).queryParam(RequestParameters.coordToken, stampCoordTokenVhatMasterPath)
				.queryParam(RequestParameters.query, "dynamic*").queryParam(RequestParameters.maxPageSize, requestPageSize).request()
				.header(Header.Accept.toString(), MediaType.APPLICATION_XML).get().readEntity(String.class);
		double count6 = XMLUtils.getNumberFromXml(result6, xPathResults);
		Assert.assertEquals(count6, (double) 0);
	}
	
	//Prefer to run after other tests
	@Test(groups = "turtle", priority = 50)
	public void setupBeer() throws IOException
	{
		loadBeer();
	}
	
	@Test(dependsOnGroups = "turtle")
	public void testGetDescriptionStyle() throws RestException
	{
		String result = checkFail(target(RestPaths.systemAPIsPathComponent + RestPaths.descriptionStyle + MetaData.SNOMED_CT_CORE_MODULES____SOLOR.getNid())
				.request().header(Header.Accept.toString(), MediaType.APPLICATION_XML).get()).readEntity(String.class);
		
		Assert.assertEquals(XMLUtils.unmarshalObject(RestDescriptionStyle.class, result).enumName, DescriptionStyle.NATIVE.name());

		String bevonModules = "71ea2fcd-c6bf-50b0-a387-003c17eb84c5";  //from the beer ontology
		result = checkFail(target(RestPaths.systemAPIsPathComponent + RestPaths.descriptionStyle + bevonModules)
				.request().header(Header.Accept.toString(), MediaType.APPLICATION_XML).get()).readEntity(String.class);
		
		Assert.assertEquals(XMLUtils.unmarshalObject(RestDescriptionStyle.class, result).enumName, DescriptionStyle.EXTERNAL.name());
		
		//TODO need to load something else to test extended types
	}

	@Test
	public void testQueryAPIs() throws IOException, JAXBException
	{
		// Retrieve valid input XML from file
		final String testInputFilename = "src/test/resources/testdata/readonlyresttest.testqueryapis.testflworquery.input.flwor";
		final String flworQueryXml = FileUtils.readFileToString(new File(testInputFilename), Charsets.UTF8_CHARSET);

		// Retrieve expected output XML which should result from valid input
		final String validTestOutputFilename = "src/test/resources/testdata/readonlyresttest.testqueryapis.testflworquery.output.xml";
		final String validTestOutputXml = FileUtils.readFileToString(new File(validTestOutputFilename), Charsets.UTF8_CHARSET);

		// Test FLWOR query with valid input, all pages
		Response response = target(RestPaths.queryAPIsPathComponent + RestPaths.flworComponent).request().header(Header.Accept.toString(), MediaType.APPLICATION_XML).post(Entity.xml(flworQueryXml));

		// Ensure request succeeded and retrieve serialized response
		String receivedResultXml = checkFail(response).readEntity(String.class);

//		System.out.println("FLWOR QUERY RETURNED: " + XmlPrettyPrinter.toString(receivedResultXml));
//		System.out.println("FLWOR QUERY EXPECTED: " + XmlPrettyPrinter.toString(validTestOutputXml));

		// Deserialize receivedResultJson into receivedResultObject
		RestQueryResultPage receivedResultObject = XMLUtils.unmarshalObject(RestQueryResultPage.class, receivedResultXml);

		// Deserialize validTestOutputXml into RestQueryResults
		RestQueryResultPage expectedResultObject = XMLUtils.unmarshalObject(RestQueryResultPage.class, validTestOutputXml);

		// Compare receivedResultObject to expectedResultObject
		Assert.assertTrue(Arrays.equals(receivedResultObject.getResults(), expectedResultObject.getResults()));
		
		if (receivedResultObject.getResults().length <= 1) {
			// If only one result row, then no need to test pagination
			return;
		}
		
		// If more than 1 result row
		final RestQueryResult expectedPage1Result = receivedResultObject.getResults()[0];
		final RestQueryResult expectedPage2Result = receivedResultObject.getResults()[1];
		
		// Test FLWOR query with valid input, page 1 of size 1
		response = target(RestPaths.queryAPIsPathComponent + RestPaths.flworComponent)
				.queryParam(RequestParameters.pageNum, 1)
				.queryParam(RequestParameters.maxPageSize, 1)
				.request().header(Header.Accept.toString(), MediaType.APPLICATION_XML).post(Entity.xml(flworQueryXml));

		// Ensure request succeeded and retrieve serialized response
		receivedResultXml = checkFail(response).readEntity(String.class);
		
//		System.out.println("FLWOR QUERY RETURNED PAGE 1: " + XmlPrettyPrinter.toString(receivedResultXml));
//		System.out.println("FLWOR QUERY EXPECTED FIRST ENTRY FROM: " + XmlPrettyPrinter.toString(validTestOutputXml));

		// Deserialize receivedResultJson into receivedResultObject
		receivedResultObject = XMLUtils.unmarshalObject(RestQueryResultPage.class, receivedResultXml);

		// Compare receivedResultObject to expectedPage1Result
		Assert.assertTrue(Arrays.equals(receivedResultObject.getResults(), new RestQueryResult[] { expectedPage1Result }));
		
		// Test FLWOR query with valid unput, page 2 of size 1
		response = target(RestPaths.queryAPIsPathComponent + RestPaths.flworComponent)
				.queryParam(RequestParameters.pageNum, 2)
				.queryParam(RequestParameters.maxPageSize, 1)
				.request().header(Header.Accept.toString(), MediaType.APPLICATION_XML).post(Entity.xml(flworQueryXml));

		// Ensure request succeeded and retrieve serialized response
		receivedResultXml = checkFail(response).readEntity(String.class);

//		System.out.println("FLWOR QUERY RETURNED PAGE 2: " + XmlPrettyPrinter.toString(receivedResultXml));
//		System.out.println("FLWOR QUERY EXPECTED SECOND ENTRY FROM: " + XmlPrettyPrinter.toString(validTestOutputXml));

		// Deserialize receivedResultJson into receivedResultObject
		receivedResultObject = XMLUtils.unmarshalObject(RestQueryResultPage.class, receivedResultXml);

		// Compare receivedResultObject to expectedPage2Result
		Assert.assertTrue(Arrays.equals(receivedResultObject.getResults(), new RestQueryResult[] { expectedPage2Result }));
	}

	
	/**
	 * fullResultSize, maxPageSize and pageNum
	 * @throws RestException 
	 */
	@Test
	public void testPaginationUtil() throws RestException {
		// Create a fullResult of size 95
		final int fullResultSize = 95;
		List<List<String>> fullResult = new LinkedList<>();
		for (int i = 1; i <= fullResultSize; ++i) {
			fullResult.add(Arrays.asList(new String[] { i + "", i + "", i + "", i + "", i + "" }));
		}
		
		List<List<String>> page = new LinkedList<>();
		List<List<String>> expectedResult = new LinkedList<>();

		// Check request for a page in the middle of the fullResult
		int maxPageSize = 10;
		int pageNum = 2;
		page = PaginationUtils.getResults(fullResult, pageNum, maxPageSize);
		Assert.assertTrue(page.size() == maxPageSize);
		expectedResult = new LinkedList<>();
		for (int row = 10; row < 20; ++row) {
			expectedResult.add(fullResult.get(row));
		}
		Assert.assertEquals(page, expectedResult);

		// Check request for a page outside of the fullResult
		maxPageSize = 1000;
		pageNum = 2;
		page = PaginationUtils.getResults(fullResult, pageNum, maxPageSize);
		Assert.assertFalse(page.size() > 0);

		// Throw RestException on bad maxPageSize
		RestException caughtException = null;
		try {
			maxPageSize = -1;
			pageNum = 2;
			page = PaginationUtils.getResults(fullResult, pageNum, maxPageSize);
		} catch (RestException e) {
			caughtException = e;
		}
		Assert.assertNotNull(caughtException);

		// Throw RestException on bad pageNum
		caughtException = null;
		try {
			maxPageSize = 10;
			pageNum = 0;
			page = PaginationUtils.getResults(fullResult, pageNum, maxPageSize);
		} catch (RestException e) {
			caughtException = e;
		}
		Assert.assertNotNull(caughtException);

		// Check for page straddling the end of fullResult
		maxPageSize = 10;
		pageNum = 10;
		page = PaginationUtils.getResults(fullResult, pageNum, maxPageSize);
		Assert.assertTrue(page.size() == ((maxPageSize * pageNum) - fullResult.size()));
		expectedResult = new LinkedList<>();
		for (int row = 90; row < fullResultSize; ++row) {
			expectedResult.add(fullResult.get(row));
		}
		Assert.assertEquals(page, expectedResult);
	}
}
