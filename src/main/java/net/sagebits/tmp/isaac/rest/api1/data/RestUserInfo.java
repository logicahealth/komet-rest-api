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

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import net.sagebits.tmp.isaac.rest.session.RequestInfo;
import sh.isaac.MetaData;
import sh.isaac.api.Get;
import sh.isaac.api.chronicle.LatestVersion;
import sh.isaac.api.component.semantic.version.DescriptionVersion;

/**
 * {@link RestUserInfo}
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
public class RestUserInfo
{
	private transient static Logger log = LogManager.getLogger();
	/**
	 * The identifiers of the concept which is a placeholder for a user in the system.
	 */
	@XmlElement
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public RestIdentifiedObject userId;

	/**
	 * The unique name of the user, used as the FQN on the concept that represents the user.
	 * This value was created from the information passed in via the SSO authentication.
	 */
	@XmlElement
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public String uniqueName;

	/**
	 * The user-preferred name of the current user. This may be identical to the unique name, or it may have been customized by the user.
	 * 
	 */
	@XmlElement
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public String preferredName;

	protected RestUserInfo()
	{
		// for jaxb
	}

	/**
	 * @param conceptNid
	 */
	public RestUserInfo(int conceptNid)
	{
		userId = new RestIdentifiedObject(conceptNid);
		Get.assemblageService().getDescriptionsForComponent(conceptNid).forEach(description -> {
			LatestVersion<DescriptionVersion> latest = description.getLatestVersion(RequestInfo.get().getStampCoordinate());
			DescriptionVersion ds = latest.get();

			if (ds.getDescriptionTypeConceptNid() == MetaData.FULLY_QUALIFIED_NAME_DESCRIPTION_TYPE____SOLOR.getNid())
			{
				uniqueName = ds.getText();
			}
			else if (ds.getDescriptionTypeConceptNid() == MetaData.REGULAR_NAME_DESCRIPTION_TYPE____SOLOR.getNid())
			{
				preferredName = ds.getText();
			}
		});

		if (StringUtils.isBlank(uniqueName) || StringUtils.isBlank(preferredName))
		{
			log.warn("Error reading description(s) for user concept " + conceptNid);
		}
	}
}
