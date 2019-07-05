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

package net.sagebits.tmp.isaac.rest;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Provider;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.jetty.JettyHttpContainerFactory;
import org.glassfish.jersey.message.MessageProperties;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;
import org.glassfish.jersey.servlet.ServletContainer;
import eu.infomas.annotation.AnnotationDetector;
import net.sagebits.HK2Utilities.AnnotatedClasses;
import net.sagebits.HK2Utilities.AnnotationReporter;
import sh.isaac.api.constants.SystemPropertyConstants;

/**
 * 
 * {@link LocalServerRunner}
 *
 * @author <a href="mailto:daniel.armbrust.list@sagebits.net">Dan Armbrust</a>
 */
public class LocalServerRunner
{
	private static final URI BASE_URI = URI.create("http://0.0.0.0:8180");
	
	public static ResourceConfig configureJerseyServer() throws IOException, ClassNotFoundException
	{
		// Find all classes with the specified annotations:
		AnnotatedClasses ac = new AnnotatedClasses();

		@SuppressWarnings("unchecked")
		AnnotationDetector cf = new AnnotationDetector(new AnnotationReporter(ac, new Class[] { Path.class, ApplicationPath.class, Provider.class }));
		cf.detect(new String[] { "net.sagebits.tmp.isaac.rest" });

		Set<Class<?>> temp = new HashSet<Class<?>>(Arrays.asList(ac.getAnnotatedClasses()));
		temp.add(JacksonFeature.class);  // No annotations in this class
		temp.add(RolesAllowedDynamicFeature.class);

		ResourceConfig rc = new ResourceConfig(temp);

		Map<String, Object> properties = new HashMap<>();
		properties.put(MessageProperties.XML_FORMAT_OUTPUT, true);
		//this is for supporting .xml and .json for setting the return type
		properties.put(ServerProperties.MEDIA_TYPE_MAPPINGS, "xml : " + MediaType.APPLICATION_XML + ", json : " + MediaType.APPLICATION_JSON);
		rc.addProperties(properties);

		return rc;
	}
	

	public static void main(String[] args) throws Exception
	{
		System.out.println("Launching Jetty Server for FHIR and REST APIs");
		
		if (args != null && args.length >= 1 && args[0].startsWith("isaacDatabaseLocation="))
		{
			System.setProperty(SystemPropertyConstants.DATA_STORE_ROOT_LOCATION_PROPERTY, args[0].substring("isaacDatabaseLocation=".length()));
		}
		
		HandlerCollection hc = new HandlerCollection();
		
		//Rest API
		final ResourceConfig resourceConfig = configureJerseyServer();
		Map<String, Object> properties = new HashMap<>();
		properties.put(MessageProperties.XML_FORMAT_OUTPUT, true);
		resourceConfig.addProperties(properties);
		
		ServletContainer jersey = new ServletContainer(resourceConfig);
		ServletContextHandler restContext = new ServletContextHandler();
		restContext.setContextPath("/rest/");
		restContext.addServlet(new ServletHolder(jersey), "/*");
		
		hc.addHandler(restContext);

		final Server server = JettyHttpContainerFactory.createServer(BASE_URI, false);
		server.setHandler(hc);
		server.start();

		System.out.println("ISAAC is starting in a background thread, it may be some time before it can serve requests");
		Thread.sleep(3000);
		System.out.println("You're using Eclipse; click in this console and " + "press ENTER to call System.exit() and run the shutdown routine.");
		System.out.println("Try http://localhost:8180/fhir/r4/TerminologyCapabilities?_format=json");

		try
		{
			System.in.read();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		server.stop();
		System.out.println("Goodbye");
		System.exit(0);
	}
}
