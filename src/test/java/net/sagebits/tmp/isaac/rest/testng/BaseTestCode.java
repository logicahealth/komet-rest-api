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
 */
package net.sagebits.tmp.isaac.rest.testng;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.grizzly.http.util.Header;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.JerseyTestNg;
import org.testng.Assert;
import net.sagebits.tmp.isaac.rest.api.exceptions.RestException;
import net.sagebits.tmp.isaac.rest.api1.RestPaths;
import net.sagebits.tmp.isaac.rest.api1.data.RestCoordinatesToken;
import net.sagebits.tmp.isaac.rest.api1.data.RestEditToken;
import net.sagebits.tmp.isaac.rest.api1.data.RestId;
import net.sagebits.tmp.isaac.rest.api1.data.semantic.RestSemanticDescriptionVersion;
import net.sagebits.tmp.isaac.rest.session.RequestParameters;
import net.sagebits.tmp.isaac.rest.session.RestUserService;
import net.sagebits.tmp.isaac.rest.tokens.CoordinatesToken;
import net.sagebits.tmp.isaac.rest.tokens.CoordinatesTokens;
import net.sagebits.uts.auth.rest.session.AuthRequestParameters;
import sh.isaac.api.Get;
import sh.isaac.api.LookupService;
import sh.isaac.api.coordinate.ManifoldCoordinate;
import sh.isaac.convert.mojo.turtle.TurtleImportMojoDirect;

/**
 * {@link BaseTestCode}
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a> 
 */
public class BaseTestCode
{
	public static Logger log = LogManager.getLogger(ReadOnlyRestTest.class);

	final static String taxonomyCoordinateRequestPath = RestPaths.coordinateAPIsPathComponent + RestPaths.taxonomyCoordinatePathComponent;
	final static String stampCoordinateRequestPath = RestPaths.coordinateAPIsPathComponent + RestPaths.stampCoordinatePathComponent;
	final static String languageCoordinateRequestPath = RestPaths.coordinateAPIsPathComponent + RestPaths.languageCoordinatePathComponent;
	final static String logicCoordinateRequestPath = RestPaths.coordinateAPIsPathComponent + RestPaths.logicCoordinatePathComponent;
	final static String descriptionSearchRequestPath = RestPaths.searchAPIsPathComponent + RestPaths.descriptionsComponent;
	final static String taxonomyRequestPath = RestPaths.taxonomyAPIsPathComponent + RestPaths.versionComponent;
	final static String editTokenRequestPath = RestPaths.coordinateAPIsPathComponent + RestPaths.editTokenComponent;

	final static String semanticSearchRequestPath = RestPaths.searchAPIsPathComponent + RestPaths.semanticsComponent;
	final static String prefixSearchRequestPath = RestPaths.searchAPIsPathComponent + RestPaths.prefixComponent;
	final static String byRefSearchRequestPath = RestPaths.searchAPIsPathComponent + RestPaths.forReferencedComponentComponent;

	final static String conceptDescriptionsRequestPath = RestPaths.conceptAPIsPathComponent + RestPaths.descriptionsComponent;
	final static String conceptVersionRequestPath = RestPaths.conceptAPIsPathComponent + RestPaths.versionComponent;
	final static String coordinatesTokenRequestPath = RestPaths.coordinateAPIsPathComponent + RestPaths.coordinatesTokenComponent;
	final static String semanticByAssemblageRequestPath = RestPaths.semanticAPIsPathComponent + RestPaths.forAssemblageComponent;
	final static String semanticByReferencedComponentRequestPath = RestPaths.semanticAPIsPathComponent + RestPaths.forReferencedComponentComponent;
	
	public static String TEST_READ_ONLY_SSO_TOKEN;
	public static String TEST_SSO_TOKEN;
	public static JerseyTest jt;
	
	public static void configure(JerseyTestNg jt) throws RestException
	{
		BaseTestCode.jt = jt;
		
		Map<String, List<String>> params = new HashMap<>();
		params.put(AuthRequestParameters.userName, Arrays.asList(new String[] {"readOnly"}));
		params.put(AuthRequestParameters.password, Arrays.asList(new String[] {"readOnly"}));
		TEST_READ_ONLY_SSO_TOKEN = LookupService.getService(RestUserService.class).getUser(params, null).get().ssoToken;

		params.put(AuthRequestParameters.userName, Arrays.asList(new String[] {"admin"}));
		params.put(AuthRequestParameters.password, Arrays.asList(new String[] {"admin"}));
		TEST_SSO_TOKEN = LookupService.getService(RestUserService.class).getUser(params, null).get().ssoToken;
	}

	public CoordinatesToken getDefaultCoordinatesToken() throws RestException
	{
		String editCoordinatesTokenXml = checkFail(
				(jt.target(coordinatesTokenRequestPath)).request().header(Header.Accept.toString(), MediaType.APPLICATION_XML).get()).readEntity(String.class);
		RestCoordinatesToken coordinatesToken = XMLUtils.unmarshalObject(RestCoordinatesToken.class, editCoordinatesTokenXml);

		return CoordinatesTokens.getOrCreate(coordinatesToken.token);
	}

	public ManifoldCoordinate getDefaultCoordinates() throws RestException
	{
		return getDefaultCoordinatesToken().getManifoldCoordinate();
	}
	
	public Response checkFail(Response response)
	{
		return assertResponseStatus(response, Status.OK.getStatusCode());
	}

	public Response assertResponseStatus(Response response, int expectedStatus)
	{
		if (response.getStatus() != expectedStatus)
		{
			Assert.fail("Unexpected response code " + response.getStatus() + " \"" + Status.fromStatusCode(response.getStatus()) + "\". Expected "
					+ expectedStatus + " \"" + Status.fromStatusCode(expectedStatus) + "\". " + " " + response.readEntity(String.class));
		}
		return response;
	}
	
	public WebTarget target(String url)
	{
		return jt.target(url);
	}

	public WebTarget target(String url, Map<String, Object> testParameters)
	{
		WebTarget target = jt.target(url);
		if (testParameters != null)
		{
			for (Map.Entry<String, Object> testParameter : testParameters.entrySet())
			{
				target = target.queryParam(testParameter.getKey(), testParameter.getValue());
			}
		}

		return target;
	}

	@SafeVarargs
	public final Map<String, Object> buildParams( Map.Entry<String, Object>... parameters)
	{
		Map<String, Object> map = new HashMap<>();
		for (Map.Entry<String, Object> testParameter : parameters)
		{
			map.put(testParameter.getKey(), testParameter.getValue());
		}

		return map;
	}

	public Map.Entry<String, Object> param(String key, Object value)
	{
		Map<String, Object> map = new HashMap<>();
		map.put(key, value);
		return map.entrySet().iterator().next();
	}

	public Optional<String> getCaughtParameterValidationExceptionMessage(String badParamName, String badParamValue, Throwable t)
	{
		for (Throwable ex : getAllExceptionsAndCauses(t))
		{
			if (ex.getLocalizedMessage().contains(
					"The parameter '" + badParamName + "' with value '[" + badParamValue + "]'  resulted in the error: Invalid or unsupported parameter name"))
			{
				return Optional.of(ex.getLocalizedMessage());
			}
		}

		return Optional.empty();
	}

	public List<Throwable> getAllExceptionsAndCauses(Throwable t)
	{
		List<Throwable> list = new ArrayList<>();

		if (t != null)
		{
			if (t.getCause() == null || t.getCause() == t)
			{
				list.add(t);
			}
			else
			{
				list.addAll(getAllExceptionsAndCauses(t.getCause()));
			}
		}

		return list;
	}

	/**
	 * @param uuid
	 * @param outputType parsable individual or comma-delimited list of IdType enum values: i.e.: uuid, nid, conceptNid, sctid,
	 *            vuid
	 * @return
	 */
	public int getIntegerIdForUuid(UUID uuid, String outputType)
	{
		final String url = RestPaths.idAPIsPathComponent + RestPaths.idTranslateComponent + uuid.toString();
		Response response = jt.target(url).queryParam(RequestParameters.inputType, "uuid").queryParam(RequestParameters.outputType, outputType).request()
				.header(Header.Accept.toString(), MediaType.APPLICATION_XML).get();
		String idXml = checkFail(response).readEntity(String.class);
		RestId restId = XMLUtils.unmarshalObject(RestId.class, idXml);
		return Integer.parseInt(restId.value);
	}

	public RestSemanticDescriptionVersion[] getDescriptionsForConcept(Object id)
	{
		return getDescriptionsForConcept(id, (Map<String, Object>) null);
	}

	@SafeVarargs
	public final RestSemanticDescriptionVersion[] getDescriptionsForConcept(Object id, Map.Entry<String, Object>... params)
	{
		WebTarget webTarget = jt.target(conceptDescriptionsRequestPath + id.toString());
		if (params != null)
		{
			for (Map.Entry<String, Object> entry : params)
			{
				webTarget = webTarget.queryParam(entry.getKey(), entry.getValue());
			}
		}
		Response getDescriptionVersionsResponse = webTarget.request().header(Header.Accept.toString(), MediaType.APPLICATION_XML).get();
		String descriptionVersionsResult = checkFail(getDescriptionVersionsResponse).readEntity(String.class);
		return XMLUtils.unmarshalObjectArray(RestSemanticDescriptionVersion.class, descriptionVersionsResult);
	}

	@SuppressWarnings("unchecked")
	public RestSemanticDescriptionVersion[] getDescriptionsForConcept(Object id, Map<String, Object> params)
	{
		@SuppressWarnings("rawtypes")
		Map.Entry[] entries = (params != null) ? params.entrySet().toArray(new Map.Entry[params.entrySet().size()]) : null;
		return getDescriptionsForConcept(id, entries);
	}

	public String getEditTokenString(String ssoTokenString)
	{
		Response getEditTokenResponse = jt.target(editTokenRequestPath.replaceFirst(RestPaths.appPathComponent, ""))
				.queryParam(AuthRequestParameters.ssoToken, ssoTokenString).request().header(Header.Accept.toString(), MediaType.APPLICATION_XML).get();
		String getEditTokenResponseResult = checkFail(getEditTokenResponse).readEntity(String.class);
		RestEditToken restEditTokenObject = XMLUtils.unmarshalObject(RestEditToken.class, getEditTokenResponseResult);
		return restEditTokenObject.token;
	}
	
	public String getEditTokenString(String ssoTokenString, int module)
	{
		Response getEditTokenResponse = jt.target(editTokenRequestPath.replaceFirst(RestPaths.appPathComponent, ""))
				.queryParam(AuthRequestParameters.ssoToken, ssoTokenString)
				.queryParam(RequestParameters.editModule, module).request().header(Header.Accept.toString(), MediaType.APPLICATION_XML).get();
		String getEditTokenResponseResult = checkFail(getEditTokenResponse).readEntity(String.class);
		RestEditToken restEditTokenObject = XMLUtils.unmarshalObject(RestEditToken.class, getEditTokenResponseResult);
		return restEditTokenObject.token;
	}
	
	public void loadBeer() throws IOException
	{
		File beer = new File("src/test/resources/turtle/bevontology-0.8.ttl");
		TurtleImportMojoDirect timd = new TurtleImportMojoDirect();
		timd.configure(null, beer.toPath(), "0.8", null);
		timd.convertContent(update -> {}, (work, total) -> {});
		Get.indexDescriptionService().refreshQueryEngine();
		CoordinatesTokens.clearCache();
	}
}
