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
package net.sagebits.tmp.isaac.rest.junit;

import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.glassfish.jersey.logging.LoggingFeature;
import org.testng.Assert;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import net.sagebits.tmp.isaac.rest.api1.RestPaths;

/**
 * @author vikas.sharma
 *
 */
public class TaxonomyUnitTest extends SSLWSRestClientHelper
{
	Logger logger = Logger.getLogger(getClass().getName());

	public static void main(String[] args) throws Exception
	{
		TaxonomyUnitTest taxonomyUnitTest = new TaxonomyUnitTest();
		taxonomyUnitTest.testGetConceptVersionTaxonomy(args[0]);
	}

	@Parameters(value = "baseURI")
	public void testGetConceptVersionTaxonomy(@Optional("http://localhost:8080/isaac-rest/rest/") String baseURI) throws Exception
	{
		final String url = baseURI + RestPaths.taxonomyAPIsPathComponent + RestPaths.versionComponent;
		Random randomGenerator = new Random();
		Feature feature = new LoggingFeature(logger, Level.INFO, null, null);
		WebTarget wt = getWebTarget(url);
		wt.register(feature);
		for (int idx = 1; idx <= 30; ++idx)
		{
			try
			{
				Response response = wt.queryParam("childDepth", "" + randomGenerator.nextInt(5)).queryParam("countChildren", "true")
						.queryParam("countChildren", "true").queryParam("expand", "chronology").queryParam("id", "" + randomGenerator.nextInt(50000))
						.queryParam("parentHeight", "1").queryParam("stated", "true").request().header("ACCEPT", MediaType.APPLICATION_JSON)
						.get();
				checkFail(response).readEntity(String.class);
			}
			catch (Exception e)
			{
				System.out.println(e.getMessage());
				throw new Exception(e);
			}
		}
	}

	private Response checkFail(Response response)
	{
		if (response.getStatus() != Status.OK.getStatusCode())
		{
			Assert.fail("Response code " + response.getStatus() + " - " + Status.fromStatusCode(response.getStatus()) + response.readEntity(String.class));
		}
		return response;
	}
}
