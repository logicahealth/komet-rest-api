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
package net.sagebits.tmp.isaac.rest.api1.data.concept;

import java.util.Optional;
import javax.xml.bind.annotation.XmlElement;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import net.sagebits.tmp.isaac.rest.Util;
import net.sagebits.tmp.isaac.rest.api1.data.RestIdentifiedObject;
import net.sagebits.tmp.isaac.rest.api1.data.enumerations.RestDescriptionStyle;
import net.sagebits.tmp.isaac.rest.session.RequestInfo;
import sh.isaac.MetaData;
import sh.isaac.api.Get;
import sh.isaac.api.chronicle.LatestVersion;
import sh.isaac.api.chronicle.VersionType;
import sh.isaac.api.component.concept.ConceptChronology;
import sh.isaac.api.component.semantic.SemanticChronology;
import sh.isaac.api.component.semantic.version.ComponentNidVersion;
import sh.isaac.api.component.semantic.version.DescriptionVersion;

/**
 * {@link RestTerminologyConcept}
 * 
 * A convenience class that is used to return a high-level summary of the Terminologies available in the system.
 * For more details on specific terminologies available, see 1/system/systemInfo
 * 
 * When this is returned, the 'description' field will be populated with the best short-name of the Terminology,
 * while the 'definition' field will be populated with the the longer extended name of a terminology.
 * 
 * There are no supported expandables, and the versions field will not be populated.
 * 
 * This will also tell you if the terminology type uses a description style of native, extended, or external.
 * See {@link RestDescriptionStyle}
 *
 * @author <a href="mailto:daniel.armbrust.list@sagebits.net">Dan Armbrust</a>
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE)
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY)
public class RestTerminologyConcept extends RestConceptChronology
{
	private transient static Logger log = LogManager.getLogger();
	
	/**
	 * The (optional) longer name of a terminology (if available)
	 */
	@XmlElement
	String definition;
	
	/**
	 * The description style used by this terminology
	 */
	@XmlElement
	RestDescriptionStyle descriptionStyle;

	public RestTerminologyConcept(ConceptChronology cc, RestDescriptionStyle descriptionStyle)
	{
		super();
		identifiers = new RestIdentifiedObject(cc);
		expandables = null;
		this.descriptionStyle = descriptionStyle;

		Get.assemblageService().getDescriptionsForComponent(cc.getNid()).forEach(desc -> {
			LatestVersion<DescriptionVersion> lv = desc.getLatestVersion(RequestInfo.get().getStampCoordinate());
			Util.logContradictions(log, lv);
			if (lv.isPresent())
			{
				if (lv.get().getDescriptionTypeConceptNid() == MetaData.REGULAR_NAME_DESCRIPTION_TYPE____SOLOR.getNid())
				{
					// I want the non-preferred regular in this case - as the way the metadata is set up, the non-preferred regular is the one without
					// the word "Modules" in it.

					Optional<SemanticChronology> acceptabilitySemantic = Get.assemblageService()
							.getSemanticChronologyStreamForComponent(lv.get().getNid()).findAny();
					if (acceptabilitySemantic.isPresent() && acceptabilitySemantic.get().getVersionType() == VersionType.COMPONENT_NID)
					{
						LatestVersion<ComponentNidVersion> value = acceptabilitySemantic.get().getLatestVersion(RequestInfo.get().getStampCoordinate());
						Util.logContradictions(log, value);
						if (value.isPresent())
						{
							if (value.get().getComponentNid() == MetaData.ACCEPTABLE____SOLOR.getNid())
							{
								description = lv.get().getText();
							}
						}
					}

				}
				else if (lv.get().getDescriptionTypeConceptNid() == MetaData.DEFINITION_DESCRIPTION_TYPE____SOLOR.getNid())
				{
					definition = lv.get().getText();
				}
			}
		});
		if (StringUtils.isBlank(description))
		{
			LogManager.getLogger().info("Unable to find expected description type for Terminology Concept " + cc.getPrimordialUuid());
			description = Util.readBestDescription(cc.getNid(), RequestInfo.get().getStampCoordinate(), RequestInfo.get().getLanguageCoordinate());
		}
	}
}
