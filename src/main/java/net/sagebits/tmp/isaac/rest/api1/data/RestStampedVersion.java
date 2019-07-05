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

import java.util.UUID;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import net.sagebits.tmp.isaac.rest.api1.data.enumerations.RestStateType;
import sh.isaac.api.Get;
import sh.isaac.api.commit.Stamp;
import sh.isaac.api.identity.StampedVersion;

/**
 * 
 * {@link RestStampedVersion}
 *
 * @author <a href="mailto:daniel.armbrust.list@sagebits.net">Dan Armbrust</a>
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE)
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY)
public class RestStampedVersion implements Comparable<RestStampedVersion>
{
	/**
	 * The Status of this version (active, inactive, primordial or cancelled)
	 */
	@XmlElement
	public RestStateType state;

	/**
	 * The time stamp of this version (in standard java form)
	 */
	@XmlElement
	public long time;

	/**
	 * The UUID of the concept that identifies the author of this version
	 */
	@XmlElement
	public UUID authorUUID;

	/**
	 * The UUID of the concept that identifies the module that this version is in
	 */
	@XmlElement
	public UUID moduleUUID;

	/**
	 * The UUID of the concept that identifies the path that this version is in
	 */
	@XmlElement
	public UUID pathUUID;

	@XmlTransient
	public RestStateType getStatus()
	{
		return state;
	}

	RestStampedVersion()
	{
		// For JAXB only
	}

	public RestStampedVersion(StampedVersion sv)
	{
		state = new RestStateType(sv.getStatus());
		time = sv.getTime();
		authorUUID = Get.identifierService().getUuidPrimordialForNid(sv.getAuthorNid());
		pathUUID = Get.identifierService().getUuidPrimordialForNid(sv.getPathNid());
		moduleUUID = Get.identifierService().getUuidPrimordialForNid(sv.getModuleNid());
	}

	public RestStampedVersion(Stamp s)
	{
		state = new RestStateType(s.getStatus());
		time = s.getTime();
		authorUUID = Get.identifierService().getUuidPrimordialForNid(s.getAuthorNid());
		pathUUID = Get.identifierService().getUuidPrimordialForNid(s.getPathNid());
		moduleUUID = Get.identifierService().getUuidPrimordialForNid(s.getModuleNid());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString()
	{
		return "RestStampedVersion [state=" + state + ", time=" + time + ", author=" + authorUUID + ", module=" + moduleUUID + ", path=" + pathUUID + "]";
	}

	/**
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(RestStampedVersion o)
	{
		return Long.compare(this.time, o.time);
	}
}
