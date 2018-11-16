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

package net.sagebits.tmp.isaac.rest.api1.data.comment;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import net.sagebits.tmp.isaac.rest.api1.data.RestIdentifiedObject;
import net.sagebits.tmp.isaac.rest.api1.data.RestStampedVersion;
import sh.isaac.api.component.semantic.version.DynamicVersion;
import sh.isaac.api.constants.DynamicConstants;

/**
 * A comment attached to a component
 * 
 * {@link RestCommentVersion}
 *
 * @author <a href="mailto:daniel.armbrust.list@sagebits.net">Dan Armbrust</a>
 */
@XmlRootElement
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE)
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY)
public class RestCommentVersion extends RestCommentVersionBase
{
	/**
	 * The identifier data for the comment itself
	 */
	@XmlElement
	public RestIdentifiedObject identifiers;

	/**
	 * The identifier data of the item the comment is placed on
	 */
	@XmlElement
	public RestIdentifiedObject commentedItem;

	/**
	 * The StampedVersion details for this comment
	 */
	@XmlElement
	public RestStampedVersion commentStamp;

	RestCommentVersion()
	{
		// For JAXB
		super();
	}

	public RestCommentVersion(DynamicVersion<?> commentSemantic)
	{
		super(commentSemantic.getData()[0].getDataObject().toString(),
				(commentSemantic.getData().length > 1 && commentSemantic.getData()[1] != null) ? commentSemantic.getData()[1].getDataObject().toString() : null);
		identifiers = new RestIdentifiedObject(commentSemantic.getChronology());
		commentStamp = new RestStampedVersion(commentSemantic);
		commentedItem = new RestIdentifiedObject(commentSemantic.getReferencedComponentNid());
		if (commentSemantic.getAssemblageNid() != DynamicConstants.get().DYNAMIC_COMMENT_ATTRIBUTE.getNid())
		{
			throw new RuntimeException("The provided semantic isn't a comment!");
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString()
	{
		return "RestCommentVersion [identifiers=" + identifiers + ", commentStamp=" + commentStamp + ", commentedItem=" + commentedItem + ", comment=" + comment
				+ ", commentContext=" + commentContext + "]";
	}
}
