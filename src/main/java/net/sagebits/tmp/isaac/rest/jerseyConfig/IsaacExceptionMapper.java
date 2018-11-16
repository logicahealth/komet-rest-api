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

package net.sagebits.tmp.isaac.rest.jerseyConfig;


import java.io.IOException;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.sagebits.tmp.isaac.rest.api.exceptions.RestException;
import net.sagebits.tmp.isaac.rest.api.exceptions.RestExceptionResponse;

/**
 * 
 * {@link IsaacExceptionMapper}
 *
 * @author <a href="mailto:daniel.armbrust.list@sagebits.net">Dan Armbrust</a>
 */
@Provider
public class IsaacExceptionMapper implements ExceptionMapper<Exception>
{
	private static Logger log = LogManager.getLogger("web");
	
	private static String serialize(RestExceptionResponse restExceptionResponse) throws JsonProcessingException {
		return new ObjectMapper().writeValueAsString(restExceptionResponse);
	}

	private static Response buildResponse(RestExceptionResponse response) {
		String json = null;
		try {
			json = serialize(response);
		} catch (JsonProcessingException e) {
			log.error("Failed serializing-to-json RestExceptionResponse " + response);
			json = "{\"conciseMessage\":\"Failed serializing response\"}";
		}
		Status status = response.getStatus() != null ? response.getStatus(): Status.INTERNAL_SERVER_ERROR;
		
		return Response.status(status).entity(json).type(MediaType.APPLICATION_JSON).build();
	}
	
	@Override
	public Response toResponse(Exception ex)
	{
		Status status = Status.INTERNAL_SERVER_ERROR; // Default is INTERNAL_SERVER_ERROR
		
		// Place any Exception with its own response status and handling here
		if (ex instanceof SecurityException || (ex instanceof IOException && ((IOException)ex).getCause() instanceof SecurityException))
		{
			log.info("SecurityException: " + ex.getMessage());

			RestExceptionResponse response = new RestExceptionResponse(
					"SecurityException",
					ex.getMessage(),
					null,
					null,
					Status.FORBIDDEN);
			return buildResponse(response);
		}

		boolean sendMessage = false;

		// Place any Exceptions that fall through to 500 here
		if (ex instanceof ClientErrorException)
		{
			log.info("ClientError:" + ex.toString());
		}
		else if (ex.getMessage() != null && ex.getMessage().startsWith("The system is not yet ready"))
		{
			status = Status.SERVICE_UNAVAILABLE;
			sendMessage = true;
			log.warn(ex.getMessage());
		}
		else if (ex.getMessage() != null && ex.getMessage().startsWith("Edit Token is no longer valid for write"))
		{
			status = Status.UNAUTHORIZED;
			sendMessage = true;
			log.info(ex.getMessage());
		}
		else if (ex instanceof RestException)
		{
			log.info("RestException: " + ex.toString());
		}
		else
		{
			log.error("Unexpected internal error", ex);
		}
		
		if (ex instanceof ClientErrorException)
		{
			status =  Status.fromStatusCode(((ClientErrorException)ex).getResponse().getStatus());
			RestExceptionResponse exceptionResponse = new RestExceptionResponse(
					ex.getMessage(),
					ex.getMessage(),
					null,
					null,
					status);
			return buildResponse(exceptionResponse);
		}
		else if (sendMessage)
		{
			// Assume that message is explicit
			String response = ex.getMessage();
			RestExceptionResponse exceptionResponse = new RestExceptionResponse(
					response,
					ex.toString(),
					null,
					null,
					status);
			return buildResponse(exceptionResponse);
		}
		else if (ex instanceof RestException)
		{			
			RestException re = (RestException) ex;
			
			// Assume that RestException indicates a BAD_REQUEST
			status = Status.BAD_REQUEST;
			RestExceptionResponse exceptionResponse = new RestExceptionResponse(
					re.toString(),
					re.toString(),
					re.getParameterName(),
					re.getParameterValue(),
					status);
			return buildResponse(exceptionResponse);
		}
		else
		{
			RestExceptionResponse exceptionResponse = new RestExceptionResponse(
					"Unexpected Internal Error: " + ex.getMessage() == null ? "" : ex.getMessage(),
					ex.toString(),
					null,
					null,
					status);
			return buildResponse(exceptionResponse);
		}
	}
}
