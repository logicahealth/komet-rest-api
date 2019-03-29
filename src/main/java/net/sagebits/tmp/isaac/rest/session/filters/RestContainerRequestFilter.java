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

package net.sagebits.tmp.isaac.rest.session.filters;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Priority;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.ext.Provider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;
import net.sagebits.tmp.isaac.rest.ApplicationConfig;
import net.sagebits.tmp.isaac.rest.api.exceptions.RestException;
import net.sagebits.tmp.isaac.rest.api1.RestPaths;
import net.sagebits.tmp.isaac.rest.session.RequestInfo;
import net.sagebits.tmp.isaac.rest.session.RequestParameters;
import net.sagebits.tmp.isaac.rest.session.RestApplicationSecurityContext;
import net.sagebits.tmp.isaac.rest.tokens.EditToken;
import net.sagebits.uts.auth.rest.api1.data.RestUser;
import net.sagebits.uts.auth.rest.session.AuthRequestParameters;

/**
 * 
 * {@link RestContainerRequestFilter}
 *
 * @author <a href="mailto:joel.kniaz.list@gmail.com">Joel Kniaz</a>
 *
 * Initializes RequestInfo ThreadLocal if necessary and initializes StampCoordinate and LanguageCoordinate based on intercepted 
 * query parameters, or default values if no relevant parameters are present.
 * 
 * Priority is set to Priorities.AUTHENTICATION - 1000 to ensure that this filter is run before other user filters, and puts 
 * the user info in earlier than the {@link RolesAllowedDynamicFeature} runs at.
 * 
 */
@Priority(Priorities.AUTHENTICATION)
@Provider
public class RestContainerRequestFilter implements ContainerRequestFilter
{
	private static Logger LOG = LogManager.getLogger();

	public RestContainerRequestFilter()
	{
	}

	private void authenticate(ContainerRequestContext requestContext) throws RestException
	{
		// GET, POST, PUT, ...
		String method = requestContext.getMethod();

		String path = requestContext.getUriInfo().getPath(true);

		// Allow wadl to be retrieved
		if (method.equals(HttpMethod.GET) && (path.equals("application.wadl") || path.equals("application.wadl/xsd0.xsd")))
		{
			return;
		}

		// Get user
		Optional<RestUser> user = RequestInfo.get().getUser();
		
		if (!user.isPresent())
		{
			throw new RestException("No user information was supplied, and anonymous read access to this service is disabled");
		}

		// Configure Security Context here
		String scheme = requestContext.getUriInfo().getRequestUri().getScheme();
		requestContext.setSecurityContext(new RestApplicationSecurityContext(user.get(), scheme));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException
	{
		LOG.debug("{} - Running filter on request {} {}", RequestInfo.get().getUniqueId(), requestContext.getRequest().getMethod(), requestContext.getUriInfo().getPath(true));
		RequestInfo.get();  // Just setting the start time of the request
		if (requestContext.getUriInfo().getPathParameters().size() > 0)
		{
			LOG.debug("{} - Path parameters: {}", RequestInfo.get().getUniqueId(), requestContext.getUriInfo().getPathParameters().keySet());
			for (Map.Entry<String, List<String>> parameter : requestContext.getUriInfo().getPathParameters().entrySet())
			{
				LOG.debug("{} - Path parameter \"{}\"=\"{}\"", RequestInfo.get().getUniqueId(), parameter.getKey(), 
						parameter.getKey().equals(AuthRequestParameters.password) ? "***" : parameter.getValue());
			}
		}

		// Note, this call, DECODES all of the parameters. These days, it should be safe to decode the ssoToken / editToken, because they are base64 url-safe encoded.
		//decoding them will do no harm, if they were not encoded, and they need to be decoded if someone did form-encode them.
		HashMap<String, List<String>> queryParams = new HashMap<>();
		queryParams.putAll(requestContext.getUriInfo().getQueryParameters());

		if (queryParams.size() > 0)
		{
			LOG.debug("{} - Query parameters: {}", RequestInfo.get().getUniqueId(), queryParams.keySet());
			for (Map.Entry<String, List<String>> parameter : queryParams.entrySet())
			{
				LOG.debug("{} - Query parameter \"{}\"=\"{}\"", RequestInfo.get().getUniqueId(), parameter.getKey(), 
						parameter.getKey().equals(AuthRequestParameters.password) ? "***" : parameter.getValue());
			}
		}

		if (!ApplicationConfig.getInstance().isIsaacReady())
		{
			LOG.debug("{} - Rejecting request as ISAAC is not yet ready", RequestInfo.get().getUniqueId());
			throw new IOException("The system is not yet ready.  Status: " + ApplicationConfig.getInstance().getStatusMessage());
		}

		try
		{
			RequestInfo.get().readAll(queryParams, requestContext);

			// If they are asking for an edit token, or attempting to do a write, we need a valid editToken.
			if (requestContext.getUriInfo().getPath().contains(RestPaths.writePathComponent)
					|| requestContext.getUriInfo().getPath().contains(RestPaths.coordinateAPIsPathComponent + RestPaths.editTokenComponent)
					|| queryParams.containsKey(RequestParameters.editToken))
			{

				EditToken et = RequestInfo.get().getEditToken();
				
				if (et != null)  //If they are requesting an editToken, there may not yet be one.
				{
					if (requestContext.getUriInfo().getPath().contains(RestPaths.writePathComponent)
							// VuidWriteAPIs does not require or return an EditCoordinate,
							// so calling isValidForWrite(), which invalidates the existing token, requiring its renewal,
							// will break things. Therefore, do not call isValidForWrite() for vuidAPIsPathComponent
							// Likewise with the user data store reads - they need a token for the user name, but don't do a write, 
							// and don't return an updated token.
							&& !requestContext.getUriInfo().getPath().contains(RestPaths.vuidAPIsPathComponent)
							&& !requestContext.getUriInfo().getPath().contains(RestPaths.userDataStorePathComponent))
					{
	
						// If it is a write request, the edit token needs to be valid for write.
						if (!et.isValidForWrite())
						{
							throw new IOException("Edit Token is no longer valid for write - please renew the token.");
						}
					}
					RequestInfo.get().getEditCoordinate();
				}
			}

			authenticate(requestContext); // Apply after readAll() in order to populate User, if possible
		}
		catch (RestException e)
		{
			throw e;
		}
		catch (IOException e)
		{
			throw e;
		}
		catch (Exception e)
		{
			throw new IOException(e);
		}
	}
}
