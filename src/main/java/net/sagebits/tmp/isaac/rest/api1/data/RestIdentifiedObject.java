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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import org.apache.mahout.math.Arrays;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import net.sagebits.tmp.isaac.rest.ApplicationConfig;
import net.sagebits.tmp.isaac.rest.api1.data.enumerations.RestObjectChronologyType;
import net.sagebits.tmp.isaac.rest.session.RequestInfo;
import sh.isaac.api.Get;
import sh.isaac.api.chronicle.Chronology;
import sh.isaac.api.chronicle.LatestVersion;
import sh.isaac.api.component.concept.ConceptChronology;
import sh.isaac.api.component.semantic.SemanticChronology;
import sh.isaac.api.component.semantic.version.DescriptionVersion;
import sh.isaac.api.externalizable.IsaacObjectType;

/**
 * Returns the UUIDs and nid associated with an object in the system
 * {@link RestIdentifiedObject}
 *
 * @author <a href="mailto:daniel.armbrust.list@sagebits.net">Dan Armbrust</a>
 */
@XmlRootElement
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE)
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY)
public class RestIdentifiedObject
{
	/**
	 * The globally unique, fixed, stable set of identifiers for the object. Typically populated, but may be null in edge cases - such as
	 * a nid stored in a dynamic semantic column - one has to refer to the dynamic semantic definition to determine if the nid represents
	 * a concept or a semantic - which is expensive, so it isn't pre-populated here - which will leave the UUIDs and nid blank.
	 */
	@XmlElement
	public List<UUID> uuids = new ArrayList<>();

	/**
	 * The local-database-only internal nid identifier for this object. Typically populated, but may be null in edge cases - such as
	 * a UUID stored in a dynamic semantic column which doesn't represent a known object.
	 */
	@XmlElement
	public Integer nid;

	/**
	 * A textual description of this identified object. This field is NOT always populated, and should not be relied on.
	 * 
	 * It currently always returns null in a production mode - it is only calculated when the service is in debug mode.
	 * 
	 * It is primarily a debugging aid for developers when looking at returned object in a browser. When concepts are returned, this will return an
	 * arbitrary description for the concept (sometimes - not always.)
	 * 
	 * When semantics are returned, this is currently not populated at all.
	 */
	@XmlElement
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public String description;

	/**
	 * The type of this object - concept, semantic, or unknown.
	 */
	@XmlElement
	public RestObjectChronologyType type;

	RestIdentifiedObject()
	{
		// For JAXB only
	}

	public RestIdentifiedObject(SemanticChronology semantic)
	{
		uuids.addAll(semantic.getUuidList());
		nid = semantic.getNid();
		type = new RestObjectChronologyType(IsaacObjectType.SEMANTIC);
	}

	public RestIdentifiedObject(ConceptChronology concept)
	{
		uuids.addAll(concept.getUuidList());
		nid = concept.getNid();
		type = new RestObjectChronologyType(IsaacObjectType.CONCEPT);
		if (ApplicationConfig.getInstance().isDebugDeploy())
		{
			description = Get.conceptDescriptionText(nid);
		}
	}

	public RestIdentifiedObject(UUID uuid)
	{
		if (uuid == null)
		{
			throw new RuntimeException("Attempted to return an empty RestIdentifiedObject!");
		}
		if (Get.identifierService().hasUuid(uuid))
		{
			nid = Get.identifierService().getNidForUuids(uuid);
			uuids.addAll(Get.identifierService().getUuidsForNid(nid));
			IsaacObjectType internalType = Get.identifierService().getObjectTypeForComponent(nid);
			type = new RestObjectChronologyType(internalType);
			if (internalType == IsaacObjectType.CONCEPT)
			{
				description = Get.conceptDescriptionText(nid);
			}
		}
		else
		{
			uuids.add(uuid);
			type = new RestObjectChronologyType(IsaacObjectType.UNKNOWN);
		}
	}

	public RestIdentifiedObject(UUID uuid, IsaacObjectType type)
	{
		if (uuid == null)
		{
			throw new RuntimeException("Attempted to return an empty RestIdentifiedObject!");
		}
		this.type = new RestObjectChronologyType(type);
		if (type == IsaacObjectType.UNKNOWN)
		{
			uuids.add(uuid);
		}
		else
		{
			switch (type)
			{
				case CONCEPT:
					nid = Get.identifierService().getNidForUuids(uuid);
					if (ApplicationConfig.getInstance().isDebugDeploy())
					{
						description = Get.conceptDescriptionText(nid);
					}
					break;
				case SEMANTIC:
					nid = Get.identifierService().getNidForUuids(uuid);
					break;
				default :
					throw new RuntimeException("Unexpected case");
			}
			uuids.addAll(Get.identifierService().getUuidsForNid(nid));
		}
	}

	public RestIdentifiedObject(List<UUID> uuids)
	{
		if (uuids == null || uuids.size() == 0)
		{
			throw new RuntimeException("Attempted to return an empty RestIdentifiedObject!");
		}
		if (Get.identifierService().hasUuid(uuids))
		{
			nid = Get.identifierService().getNidForUuids(uuids);
			this.uuids.addAll(Get.identifierService().getUuidsForNid(nid));
			IsaacObjectType internalType = Get.identifierService().getObjectTypeForComponent(nid);
			type = new RestObjectChronologyType(internalType);
			if (internalType == IsaacObjectType.CONCEPT)
			{
				description = Get.conceptDescriptionText(nid);
			}
		}
		else
		{
			this.uuids.addAll(uuids);
			type = new RestObjectChronologyType(IsaacObjectType.UNKNOWN);
		}
	}

	public RestIdentifiedObject(Chronology object)
	{
		nid = object.getNid();
		uuids.addAll(object.getUuidList());
		switch (object.getIsaacObjectType())
		{
			case CONCEPT:
				type = new RestObjectChronologyType(IsaacObjectType.CONCEPT);
				if (ApplicationConfig.getInstance().isDebugDeploy())
				{
					description = Get.conceptDescriptionText(nid);
				}
				break;
			case SEMANTIC:
				type = new RestObjectChronologyType(IsaacObjectType.SEMANTIC);
				break;
			default :
				throw new RuntimeException("Unexpected case");
		}
	}

	public RestIdentifiedObject(int id, IsaacObjectType type)
	{
		this.type = new RestObjectChronologyType(type);
		nid = id;
		switch (type)
		{
			case CONCEPT:
				if (ApplicationConfig.getInstance().isDebugDeploy())
				{
					LatestVersion<DescriptionVersion> ldv = RequestInfo.get().getLanguageCoordinate().getDescription(id, RequestInfo.get().getStampCoordinate());
					description = ldv.isPresent() ? ldv.get().getText() : Get.conceptDescriptionText(id);
				}
				break;
			case SEMANTIC:
				break;
			case UNKNOWN:
			default :
				throw new RuntimeException("Unexpected case");
		}
		uuids.addAll(Get.identifierService().getUuidsForNid(nid));
	}

	public RestIdentifiedObject(int id)
	{
		if (id < 0)
		{
			this.nid = id;
			uuids.addAll(Get.identifierService().getUuidsForNid(nid));
			IsaacObjectType internalType = Get.identifierService().getObjectTypeForComponent(nid);
			type = new RestObjectChronologyType(internalType);
			if (internalType == IsaacObjectType.CONCEPT)
			{
				description = Get.conceptDescriptionText(nid);
			}
		}
	}

	@XmlTransient
	public UUID getFirst()
	{
		return uuids.get(0);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((nid == null) ? 0 : nid.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());

		// Generate based only on initial, presumably primordial, uuid
		result = prime * result + ((uuids == null) ? 0 : (uuids.size() == 0 ? 0 : uuids.get(0).hashCode()));
		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RestIdentifiedObject other = (RestIdentifiedObject) obj;
		if (nid == null)
		{
			if (other.nid != null)
				return false;
		}
		else if (!nid.equals(other.nid))
			return false;
		if (type == null)
		{
			if (other.type != null)
				return false;
		}
		else if (!type.equals(other.type))
			return false;

		// Compare based only on initial, presumably primordial, uuid
		if (uuids == null)
		{
			if (other.uuids != null)
				return false;
		}
		else if (uuids.size() == 0 && other.uuids.size() != 0)
		{
			return false;
		}
		else if (uuids.size() != 0 && other.uuids.size() == 0)
		{
			return false;
		}
		else if (uuids.size() > 0)
		{
			if (!uuids.get(0).equals(other.uuids.get(0)))
			{
				return false;
			}
		}

		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString()
	{
		return "RestIdentifiedObject [type=" + type + ", nid=" + nid + ", uuids=" + (uuids != null ? Arrays.toString(uuids.toArray()) : null) + "]";
	}
}
