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

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import net.sagebits.tmp.isaac.rest.api.exceptions.RestException;
import sh.isaac.api.component.semantic.version.ComponentNidVersion;
import sh.isaac.api.component.semantic.version.DynamicVersion;
import sh.isaac.api.component.semantic.version.LongVersion;
import sh.isaac.api.component.semantic.version.SemanticVersion;
import sh.isaac.api.component.semantic.version.StringVersion;
import sh.isaac.api.component.semantic.version.dynamic.DynamicData;
import sh.isaac.model.semantic.types.DynamicLongImpl;
import sh.isaac.model.semantic.types.DynamicNidImpl;
import sh.isaac.model.semantic.types.DynamicStringImpl;

/**
 * 
 * {@link RestDynamicSemanticVersion}
 *
 * @author <a href="mailto:daniel.armbrust.list@sagebits.net">Dan Armbrust</a>
 */
@XmlRootElement
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY)
public class RestDynamicSemanticVersion extends RestSemanticVersion
{
	/**
	 * The data attached to this semantic instance (if any). The 'semantic/semanticDefinition/{id}'
	 * can be read to determine the potential types and descriptions of these columns.
	 */
	@XmlElement
	List<RestDynamicSemanticData> dataColumns;

	protected RestDynamicSemanticVersion()
	{
		// for Jaxb
	}

	public RestDynamicSemanticVersion(DynamicVersion<?> dsv, boolean includeChronology, boolean expandNested, boolean expandReferenced, 
			boolean useLatestStamp) throws RestException
	{
		super(dsv, includeChronology, expandNested, expandReferenced, useLatestStamp, null);
		dataColumns = translateData(dsv.getData());
	}

	public RestDynamicSemanticVersion(StringVersion sv, boolean includeChronology, boolean expandNested, boolean expandReferenced, 
			boolean useLatestStamp) throws RestException
	{
		super(sv, includeChronology, expandNested, expandReferenced, useLatestStamp, null);
		dataColumns = translateData(new DynamicData[] { new DynamicStringImpl(sv.getString()) });
	}

	public RestDynamicSemanticVersion(LongVersion sv, boolean includeChronology, boolean expandNested, boolean expandReferenced, 
			boolean useLatestStamp) throws RestException
	{
		super(sv, includeChronology, expandNested, expandReferenced, useLatestStamp, null);
		dataColumns = translateData(new DynamicData[] { new DynamicLongImpl(sv.getLongValue()) });
	}

	public RestDynamicSemanticVersion(ComponentNidVersion sv, boolean includeChronology, boolean expandNested, boolean expandReferenced, 
			boolean useLatestStamp) throws RestException
	{
		super(sv, includeChronology, expandNested, expandReferenced, useLatestStamp, null);
		dataColumns = translateData(new DynamicData[] { new DynamicNidImpl(sv.getComponentNid()) });
	}

	public RestDynamicSemanticVersion(SemanticVersion sv, boolean includeChronology, boolean expandNested, boolean expandReferenced, 
			boolean useLatestStamp) throws RestException
	{
		super(sv, includeChronology, expandNested, expandReferenced, useLatestStamp, null);
		// no data
	}

	/**
	 * @return the dataColumns
	 */
	@XmlTransient
	public List<RestDynamicSemanticData> getDataColumns()
	{
		return dataColumns;
	}

	public static List<RestDynamicSemanticData> translateData(DynamicData[] data)
	{
		if (data != null)
		{
			List<RestDynamicSemanticData> translatedData = new ArrayList<>();
			for (int i = 0; i < data.length; i++)
			{
				translatedData.add(RestDynamicSemanticData.translate(i, data[i]));
			}
			return translatedData;
		}
		return null;
	}
}
