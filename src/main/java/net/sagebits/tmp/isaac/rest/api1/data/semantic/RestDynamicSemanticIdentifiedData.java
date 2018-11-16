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

package net.sagebits.tmp.isaac.rest.api1.data.semantic;

import java.util.UUID;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.webcohesion.enunciate.metadata.json.JsonSeeAlso;
import net.sagebits.tmp.isaac.rest.Util;
import net.sagebits.tmp.isaac.rest.api1.data.RestIdentifiedObject;
import net.sagebits.tmp.isaac.rest.api1.data.semantic.dataTypes.RestDynamicSemanticNid;
import net.sagebits.tmp.isaac.rest.api1.data.semantic.dataTypes.RestDynamicSemanticUUID;
import sh.isaac.api.externalizable.IsaacObjectType;

/**
 * 
 * {@link RestDynamicSemanticIdentifiedData}
 *
 * @author <a href="mailto:daniel.armbrust.list@sagebits.net">Dan Armbrust</a>
 */
@XmlSeeAlso({ RestDynamicSemanticNid.class, RestDynamicSemanticUUID.class, RestDynamicSemanticNid[].class, RestDynamicSemanticUUID[].class })
@JsonSeeAlso({ RestDynamicSemanticNid.class, RestDynamicSemanticUUID.class, RestDynamicSemanticNid[].class, RestDynamicSemanticUUID[].class })
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY)
@XmlRootElement
public abstract class RestDynamicSemanticIdentifiedData extends RestDynamicSemanticData
{
	/**
	 * When the data type of the semantic is nid, or UUID, this usually represents a concept or a semantic in the system. This field carries
	 * all of the information about that object, when it is a concept or semantic in the system. For example, if the data type is nid, the 'data'
	 * field will be an int that contains the nid. This field will contain the (same) nid, but also the data type (concept or semantic).
	 * 
	 * In some cases, where the data type is a UUID - the UUID may not represent a concept or semantic, in which case, the dataIdentified will carry
	 * the UUID, and the data type unknown (but nid would be blank).
	 * 
	 */
	@XmlElement
	private RestIdentifiedObject dataIdentified;

	/**
	 * If the dataObjectType represents is a concept, then this carries the "best" description for that concept. This is selected based on the
	 * attributes within the session for stamp and language coordinates - or - if none present - the server default. This is not populated if the
	 * dataObjectType is not a concept type.
	 * Only populated when the expand parameter 'referencedDetails' is passed.
	 */
	@XmlElement
	String conceptDescription;

	protected RestDynamicSemanticIdentifiedData(Integer columnNumber, Object data)
	{
		super(columnNumber, data);
		setTypedData();
	}

	protected void setTypedData()
	{
		if (data != null)
		{
			if (data instanceof Integer)
			{
				dataIdentified = new RestIdentifiedObject((int) data);
			}
			else if (data instanceof UUID)
			{
				dataIdentified = new RestIdentifiedObject((UUID) data);
			}
			else
			{
				throw new RuntimeException("Unexpected");
			}
			if (dataIdentified.type.enumId == IsaacObjectType.CONCEPT.ordinal())
			{
				conceptDescription = Util.readBestDescription(dataIdentified.nid);
			}
		}
	}

	protected RestDynamicSemanticIdentifiedData()
	{
		// for jaxb
	}
}
