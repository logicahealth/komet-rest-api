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

package net.sagebits.tmp.isaac.rest.api1.data.enumerations;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;
import java.util.Map.Entry;
import javax.xml.bind.annotation.XmlRootElement;
import org.apache.commons.lang3.StringUtils;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import net.sagebits.tmp.isaac.rest.session.RequestInfo;
import sh.isaac.MetaData;
import sh.isaac.api.component.concept.ConceptChronology;
import sh.isaac.api.coordinate.LanguageCoordinate;
import sh.isaac.api.coordinate.StampCoordinate;
import sh.isaac.api.util.NumericUtils;
import sh.isaac.utility.Frills;

/**
 * {@link RestSupportedIdType}
 * A class that maps a few common ID types into rest, for convenience in the REST APIs.
 * 
 * This enumeration is a little bit different than the other enumerations, in that it isn't limited
 * only to a few hard-coded values.  It also includes other id types in the system - typically the children 
 * of {@link MetaData#IDENTIFIER_SOURCE____SOLOR} but more specifically any semantic types that are a member 
 * of the refset {@link MetaData#IDENTIFIER_SOURCE____SOLOR}.  It does include types like 'nid' which are 
 * not members of that refset.
 * 
 * @author <a href="mailto:daniel.armbrust.list@sagebits.net">Dan Armbrust</a>
 */
@XmlRootElement
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY)
public class RestSupportedIdType extends Enumeration
{
	private static transient RestSupportedIdType[] cache_ = null;
	
	protected RestSupportedIdType()
	{
		// for jaxb
	}

	public RestSupportedIdType(IdType idType)
	{
		super(idType.name(), idType.getDisplayName(), idType.getId());
	}
	
	public RestSupportedIdType(String name, int id)
	{
		super(name, name, id);
	}
	
	public static Optional<RestSupportedIdType> parse(String input)
	{
		if (StringUtils.isBlank(input))
		{
			return Optional.empty();
		}
		String temp = input.trim().toLowerCase();
		int inputAsInt = NumericUtils.getInt(temp).orElse(Integer.MAX_VALUE);
		
		for (RestSupportedIdType rsit : getAll())
		{
			if (rsit.enumName.toLowerCase().equals(temp))
			{
				return Optional.of(rsit);
			}
			else if (rsit.friendlyName.toLowerCase().equals(temp))
			{
				return Optional.of(rsit);
			}
			else if (rsit.enumId == inputAsInt)
			{
				return Optional.of(rsit);
			}
		}
		return Optional.empty();
	}

	/**
	 * @return all of the currently active types, sorted by displayName
	 */
	public static RestSupportedIdType[] getAll()
	{
		RestSupportedIdType[] answer = cache_;
		
		if (answer == null)
		{
			HashMap<Integer, IdType> temp = new HashMap<>();
			for (IdType id : IdType.values())
			{
				temp.put(id.getId(), id);
			}
			for (ConceptChronology cc : Frills.getIdentifierAssemblages())
			{
				temp.put(cc.getNid(), null);
			}
			
			LanguageCoordinate lc = RequestInfo.get().getLanguageCoordinate();
			StampCoordinate sc = RequestInfo.get().getStampCoordinate();
			RestSupportedIdType[] result = new RestSupportedIdType[temp.size()];
			int i = 0;
			for (Entry<Integer, IdType> entry : temp.entrySet())
			{
				result[i++] = entry.getValue() == null ? new RestSupportedIdType(lc.getRegularName(entry.getKey(), sc).orElse("_ERROR_"), entry.getKey()) 
						: new RestSupportedIdType(entry.getValue());
			}
			
			Arrays.sort(result);
			answer = result;
			cache_ = result;
		}
		return answer;
	}

	@Override
	public int compareTo(Enumeration o)
	{
		return this.friendlyName.compareToIgnoreCase(o.friendlyName);
	}
	
	public static void clearCache()
	{
		cache_ = null;
	}
}
