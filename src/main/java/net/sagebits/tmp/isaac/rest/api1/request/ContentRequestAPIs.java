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

package net.sagebits.tmp.isaac.rest.api1.request;

import java.util.UUID;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;
import net.sagebits.tmp.isaac.rest.api.exceptions.RestException;
import net.sagebits.tmp.isaac.rest.api1.RestPaths;
import net.sagebits.uts.auth.data.UserRole.SystemRoleConstants;

/**
 * {@link ContentRequestAPIs}
 * 
 * @author <a href="mailto:daniel.armbrust.list@sagebits.net">Dan Armbrust</a>
 */
@Path(RestPaths.contentRequestAPIsPathComponent)
@RolesAllowed({ SystemRoleConstants.AUTOMATED, SystemRoleConstants.ADMINISTRATOR, SystemRoleConstants.SYSTEM_MANAGER, SystemRoleConstants.CONTENT_MANAGER,
	SystemRoleConstants.EDITOR, SystemRoleConstants.READ })
public class ContentRequestAPIs
{
	@Context
	private SecurityContext securityContext;

	/**
	 * An initial sample of accepting a POST request - this particular one is for testing with the NDS team.
	 * 
	 * @param data - this API simply accepts a string value - either plain text, JSON, or XML encoded.
	 * @return 406 error if unparsable, 202 if acceptable - and returns an identifier for the request.
	 * @throws RestException
	 */
	@POST
	@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN })
	@Produces({ MediaType.TEXT_PLAIN })
	@Path(RestPaths.termRequestComponent)
	public Response putNewTermRequest(String data) throws RestException
	{
		// System.out.println("received media type: " + request.getMediaType());
		System.out.println("got data '" + data + "'");
		if (data.contains("BAD"))
		{
			return Response.status(Status.NOT_ACCEPTABLE).entity("Invalid Request").build();
		}
		else
		{
			return Response.status(Status.ACCEPTED).entity("Assigned ID of " + UUID.randomUUID() + " to your request").build();
		}
	}
}
