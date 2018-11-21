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

package net.sagebits.tmp.isaac.rest.api1.data;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import net.sagebits.tmp.isaac.rest.ApplicationConfig;
import net.sagebits.tmp.isaac.rest.api1.data.systeminfo.RestDependencyInfo;
import net.sagebits.tmp.isaac.rest.api1.data.systeminfo.RestLicenseInfo;
import net.sagebits.tmp.isaac.rest.session.RestConfig;
import sh.isaac.api.Get;
import sh.isaac.api.util.metainf.MavenArtifactInfo;
import sh.isaac.api.util.metainf.MetaInfReader;

/**
 * {@link RestSystemInfo}
 * 
 * This class carries back various system information about this deployment.
 *
 * @author <a href="mailto:daniel.armbrust.list@sagebits.net">Dan Armbrust</a>
 * 
 * 
 */
@XmlRootElement
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE)
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY)
public class RestSystemInfo
{
	private transient static Logger log_ = LogManager.getLogger();
	/**
	 * The full version number of this API. Note, this is an array, because in the future
	 * the API may simultaneously support versions such as [1.3, 2.0] for reverse compatibility.
	 * 
	 * The agreement with Komet is that we do "Major.Minor.Revision"
	 * The Major version only changes in concert with the rest API paths changing from /1/ to /2/ for example.
	 * The Minor version is changed whenever we change a previously existing API or data structure - such that it
	 * may break existing code in KOMET. Note, you can add new APIs / properties to existing data structures without
	 * breaking KOMET.
	 * The Revision is changed whenever we make a change that modifies the API, but only in a way that won't impact
	 * existing KOMET functionality - such as adding a new API, adding a new data structure, adding a field to an existing
	 * data structure.
	 */
	@XmlElement
	String[] supportedAPIVersions = new String[] { "1.19.1" };

	/**
	 * REST API Implementation Version - aka the version number of the software running here.
	 */
	@XmlElement
	String apiImplementationVersion;

	/**
	 * The version number of the database being used by this instance.
	 */
	@XmlElement
	RestDependencyInfo isaacDbDependency;

	/**
	 * The globally unique UUID assigned to the database (and changesets) that this instance of isaac-rest is running
	 * on top of
	 */
	@XmlElement
	String isaacDbId;

	/**
	 * The globally unique UUID assigned to the deployment of isaac-rest. This is assigned by PRISME at the time that PRISME
	 * is deployed (and will only be available if PRISME deployed the service). This is read from prisme.properties: war_uuid
	 */
	@XmlElement
	String warId;

	/**
	 * Source Code Management URL that contains the source code for the software running here.
	 */
	@XmlElement
	String scmUrl;

	/**
	 * The version of ISAAC that the rest service is running on top of.
	 */
	@XmlElement
	String isaacVersion;

	/**
	 * Software Licenses
	 */
	@XmlElement
	List<RestLicenseInfo> appLicenses = new ArrayList<>();

	/**
	 * Database Licenses
	 */
	@XmlElement
	List<RestLicenseInfo> dbLicenses = new ArrayList<>();

	/**
	 * The source content that was built into the underlying database.
	 */
	@XmlElement
	List<RestDependencyInfo> dbDependencies = new ArrayList<>();

	public RestSystemInfo()
	{
		// Read in other information from the package (pom.properties file during normal runtime, pom.xml files if running in a dev env)
		try
		{
			loadIsaacMetdata();
			MavenArtifactInfo mai = MetaInfReader.readDbMetadata();
			isaacDbDependency = new RestDependencyInfo(mai);
			isaacDbId = Get.conceptService().getDataStoreId().toString();
			warId = RestConfig.getInstance().getApplicationWarFileUUID();
			mai.dbLicenses.forEach(mli -> dbLicenses.add(new RestLicenseInfo(mli)));
			mai.dbDependencies.forEach(dd -> dbDependencies.add(new RestDependencyInfo(dd)));
		}
		catch (Exception ex)
		{
			log_.error("Unexpected error reading app configuration information", ex);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString()
	{
		return "SystemInfo [supportedAPIVersions=" + Arrays.toString(supportedAPIVersions) + ", apiImplementationVersion=" + apiImplementationVersion
				+ ", isaacDbDependency=" + isaacDbDependency + ", scmUrl=" + scmUrl + ", isaacVersion=" + isaacVersion + ", appLicenses=" + appLicenses
				+ ", dbLicenses=" + dbLicenses + ", dbDependencies=" + dbDependencies + "]";
	}

	// Read the DB metadata

	private void loadIsaacMetdata() throws ParserConfigurationException, SAXException, IOException, DOMException, XPathExpressionException
	{
		// read the ISAAC metadata
		AtomicBoolean readIsaacAppMetadata = new AtomicBoolean(false);

		// if running from eclipse - our launch folder should be "ISAAC-rest".
		File f = new File("").getAbsoluteFile();
		DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
		domFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
		DocumentBuilder builder = domFactory.newDocumentBuilder();

		if (ApplicationConfig.getInstance().getServletContext() != null)
		{
			InputStream is = ApplicationConfig.getInstance().getServletContext()
					.getResourceAsStream("/META-INF/maven/net.sagebits.uts.rest/uts-rest-api/pom.xml");
			if (is == null)
			{
				log_.warn("Can't locate pom.xml file in deployment to read metadata");
			}
			else
			{
				try
				{
					readFromPomFile(builder.parse(is));
				}
				finally
				{
					is.close();
				}
				readIsaacAppMetadata.set(true);
			}
		}
		else if (f.getName().toLowerCase().endsWith("rest-api"))  // context null, are we running in a local jetty runner?
		{
			File pom = new File(f, "pom.xml");
			if (pom.isFile())
			{
				readFromPomFile(builder.parse(pom));
				readIsaacAppMetadata.set(true);
			}
		}
		else
		{
			log_.warn("No Servlet Context available to utilize to locate the metadata!");
		}

		if (!readIsaacAppMetadata.get())
		{
			log_.warn("Failed to read the metadata about the ISAAC-rest instance");
		}
	}

	private void readFromPomFile(Document dDoc) throws DOMException, XPathExpressionException
	{
		XPath xPath = XPathFactory.newInstance().newXPath();
		isaacVersion = ((Node) xPath.evaluate("/project/properties/isaac.version", dDoc, XPathConstants.NODE)).getTextContent();
		if (isaacVersion.equals("${project.parent.version}"))
		{
			isaacVersion = ((Node) xPath.evaluate("/project/parent/version", dDoc, XPathConstants.NODE)).getTextContent();
		}
		scmUrl = ((Node) xPath.evaluate("/project/scm/url", dDoc, XPathConstants.NODE)).getTextContent();
		apiImplementationVersion = ((Node) xPath.evaluate("/project/version", dDoc, XPathConstants.NODE)).getTextContent();

		log_.debug("API implementation version: {} scmUrl: {} isaacVersion: {}", apiImplementationVersion, scmUrl, isaacVersion);

		NodeList appLicensesNodes = ((NodeList) xPath.evaluate("/project/licenses/license/name", dDoc, XPathConstants.NODESET));

		log_.debug("Found {} license names", appLicensesNodes.getLength());
		for (int i = 0; i < appLicensesNodes.getLength(); i++)
		{
			Node currentLicenseNameNode = appLicensesNodes.item(i);
			String name = currentLicenseNameNode.getTextContent();

			RestLicenseInfo appLicenseInfo = new RestLicenseInfo(name,
					((Node) xPath.evaluate("/project/licenses/license[name='" + name + "']/url", dDoc, XPathConstants.NODE)).getTextContent(),
					((Node) xPath.evaluate("/project/licenses/license[name='" + name + "']/comments", dDoc, XPathConstants.NODE)).getTextContent());
			appLicenses.add(appLicenseInfo);

			log_.debug("Extracted license \"{}\" from ISAAC pom.xml: {}", name, appLicenseInfo.toString());
		}
	}

	/**
	 * @return the supportedAPIVersions
	 */
	@XmlTransient
	public String[] getSupportedAPIVersions()
	{
		return Arrays.copyOf(supportedAPIVersions, supportedAPIVersions.length);
	}

	/**
	 * @return the apiImplementationVersion
	 */
	@XmlTransient
	public String getApiImplementationVersion()
	{
		return apiImplementationVersion;
	}

	/**
	 * @return the isaacDbDependency
	 */
	@XmlTransient
	public RestDependencyInfo getIsaacDbDependency()
	{
		return isaacDbDependency;
	}

	/**
	 * @return the scmUrl
	 */
	@XmlTransient
	public String getScmUrl()
	{
		return scmUrl;
	}

	/**
	 * @return the isaacVersion
	 */
	@XmlTransient
	public String getIsaacVersion()
	{
		return isaacVersion;
	}

	/**
	 * @return the appLicenses
	 */
	@XmlTransient
	public List<RestLicenseInfo> getAppLicenses()
	{
		return Collections.unmodifiableList(appLicenses);
	}

	/**
	 * @return the dbLicenses
	 */
	@XmlTransient
	public List<RestLicenseInfo> getDbLicenses()
	{
		return Collections.unmodifiableList(dbLicenses);
	}

	/**
	 * @return the dbDependencies
	 */
	@XmlTransient
	public List<RestDependencyInfo> getDbDependencies()
	{
		return Collections.unmodifiableList(dbDependencies);
	}
}
