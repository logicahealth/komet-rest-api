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

//import java.util.Optional;
//import javax.xml.bind.annotation.XmlRootElement;
//import sh.isaac.api.util.NumericUtils;
//
///**
// * {@link RestWorkflowProcessStatusType}
// * A class that maps ISAAC {@link ProcessDetail.ProcessStatus} values to REST.
// * 
// * @author <a href="mailto:joel.kniaz.list@gmail.com">Joel Kniaz</a>
// */
//@XmlRootElement
public class RestWorkflowProcessStatusType extends Enumeration
{
//	/**
//	 * Displayable text
//	 */
//	public String text;
//
//	protected RestWorkflowProcessStatusType()
//	{
//		// for jaxb
//	}
//
//	public RestWorkflowProcessStatusType(ProcessStatus dt)
//	{
//		super(dt.name(), null, dt.ordinal());
//		text = dt.toString();
//	}
//
//	public static RestWorkflowProcessStatusType[] getAll()
//	{
//		RestWorkflowProcessStatusType[] result = new RestWorkflowProcessStatusType[ProcessStatus.values().length];
//		for (int i = 0; i < ProcessStatus.values().length; i++)
//		{
//			result[i] = new RestWorkflowProcessStatusType(ProcessStatus.values()[i]);
//		}
//		return result;
//	}
//
//	public static RestWorkflowProcessStatusType valueOf(String str)
//	{
//		for (ProcessStatus enumValue : ProcessStatus.values())
//		{
//			if (enumValue.name().equals(str.trim()) || enumValue.toString().equals(str.trim()))
//			{
//				return new RestWorkflowProcessStatusType(enumValue);
//			}
//			else
//			{
//				Optional<Integer> intOptional = NumericUtils.getInt(str.trim());
//				if (intOptional.isPresent() && intOptional.get() == enumValue.ordinal())
//				{
//					return new RestWorkflowProcessStatusType(enumValue);
//				}
//			}
//		}
//		throw new IllegalArgumentException("invalid RestWorkflowProcessStatusType value \"" + str + "\"");
//	}
}
