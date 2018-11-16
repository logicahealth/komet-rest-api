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

package net.sagebits.tmp.isaac.rest.api1.export;

import java.io.IOException;
import java.io.OutputStream;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.StreamingOutput;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.sagebits.tmp.isaac.rest.Util;
import net.sagebits.tmp.isaac.rest.api.exceptions.RestException;
import net.sagebits.tmp.isaac.rest.api1.RestPaths;
import net.sagebits.tmp.isaac.rest.session.RequestInfo;
import net.sagebits.tmp.isaac.rest.session.RequestParameters;
import net.sagebits.tmp.isaac.rest.session.SecurityUtils;
import sh.isaac.misc.exporters.VetsExporter;
import sh.isaac.misc.security.SystemRoleConstants;

/**
 * {@link ExportAPIs}
 * 
 * @author <a href="mailto:daniel.armbrust.list@sagebits.net">Dan Armbrust</a>
 */
@Path(RestPaths.exportAPIsPathComponent)
@RolesAllowed({ SystemRoleConstants.AUTOMATED, SystemRoleConstants.SUPER_USER, SystemRoleConstants.ADMINISTRATOR, SystemRoleConstants.READ_ONLY,
		SystemRoleConstants.EDITOR, SystemRoleConstants.REVIEWER, SystemRoleConstants.APPROVER, SystemRoleConstants.DEPLOYMENT_MANAGER })
public class ExportAPIs
{
	private static Logger log = LogManager.getLogger(ExportAPIs.class);

	@Context
	private SecurityContext securityContext;

	/**
	 * This method will stream back an XML file. It may take some time to stream the entire file, depending on the filter criteria.
	 * 
	 * parameters that represent times may be passed in one of two formats. The parameter is sent as a string - if the parameter is parseable
	 * as a numeric long value, then it will be treated as a java date - the number of milliseconds since January 1, 1970, 00:00:00 GMT.
	 * 
	 * If it is not parseable as a numeric long value, then it is parsed as a {@link DateTimeFormatter#ISO_DATE_TIME}
	 * https://docs.oracle.com/javase/8/docs/api/java/time/format/DateTimeFormatter.html#ISO_DATE_TIME
	 * 
	 * @param changedAfter - optional - if provided, only exports content created or modified on or after this time. If not provided, returns the
	 *            latest version of every component (up to changedBefore) from VHAT.
	 * @param changedBefore - optional - if provided, only exports content created or modified on or before this time. If not provided,
	 *            includes any content created or modified up to now.
	 * @return an VETs schema valid XML file with the VHAT content that meets the date filters.
	 * @throws RestException
	 */
	@GET
	@Produces({ MediaType.APPLICATION_XML })
	@Path(RestPaths.vetsXMLComponent)
	public Response export(@QueryParam(RequestParameters.changedAfter) String changedAfter, @QueryParam(RequestParameters.changedBefore) String changedBefore)
			throws RestException
	{
		SecurityUtils.validateRole(securityContext, getClass());

		RequestParameters.validateParameterNamesAgainstSupportedNames(RequestInfo.get().getParameters(), RequestParameters.changedAfter,
				RequestParameters.changedBefore, RequestParameters.COORDINATE_PARAM_NAMES);

		long changedAfterL;
		long changedBeforeL;
		try
		{
			changedAfterL = Util.parseDate(changedAfter);
		}
		catch (DateTimeParseException e)
		{
			throw new RestException("changedAfter", "Could not be parsed as ISO-8601");
		}
		try
		{
			changedBeforeL = Util.parseDate(changedBefore);
		}
		catch (DateTimeParseException e)
		{
			throw new RestException("changedBefore", "Could not be parsed as ISO-8601");
		}
		if (changedAfterL == Long.MAX_VALUE)
		{
			throw new RestException("changedAfter", "Cannot be set to 'latest'");
		}
		if (changedAfterL > System.currentTimeMillis())
		{
			throw new RestException("changedAfter", "Cannot be set to a future time");
		}
		if (changedBeforeL < changedAfterL)
		{
			throw new RestException("changedAfter", "Cannot be set to a time greater than changedBefore");
		}

		log.info("Export VETs XML with the filter " + (changedAfterL > 0 ? "After: " + new Date(changedAfterL).toString() + " " : "")
				+ (changedBeforeL > 0 ? "Before: " + new Date(changedBeforeL).toString() + " " : ""));

		StreamingOutput stream = new StreamingOutput()
		{
			@Override
			public void write(OutputStream output) throws IOException, WebApplicationException
			{
				VetsExporter ve = new VetsExporter();
				ve.export(output, changedAfterL, changedBeforeL, false);
			}
		};
		// In order to make the file download compliant with the needed file download javascript library
		// https://github.com/johnculviner/jquery.fileDownload
		// We must set the cookie fileDownload, this enables the GUI to provide feedback to the user telling them the file download
		// was a success or a failure.
		try
		{
			return Response.ok(stream).header("content-disposition", "attachment; filename = export.xml")
					.cookie(new NewCookie(new Cookie("fileDownload", "true", "/", null))).build();
		}
		catch (Exception e)
		{
			log.warn("Error streaming the XML file back", e);
			// ClassNotFoundException should not happen in a well built system.
			throw new IllegalStateException(e);
		}
	}
}
