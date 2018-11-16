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
package net.sagebits.tmp.isaac.rest.jaxbCrutch;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import net.sagebits.tmp.isaac.rest.api1.data.association.RestAssociationItemVersion;
import net.sagebits.tmp.isaac.rest.api1.data.association.RestAssociationTypeVersion;
import net.sagebits.tmp.isaac.rest.api1.data.comment.RestCommentVersion;
import net.sagebits.tmp.isaac.rest.api1.data.concept.RestConceptChronology;
import net.sagebits.tmp.isaac.rest.api1.data.enumerations.RestMapSetItemComponentType;
import net.sagebits.tmp.isaac.rest.api1.data.mapping.RestMappingItemVersion;
import net.sagebits.tmp.isaac.rest.api1.data.mapping.RestMappingSetDisplayField;
import net.sagebits.tmp.isaac.rest.api1.data.mapping.RestMappingSetDisplayFieldCreate;
import net.sagebits.tmp.isaac.rest.api1.data.mapping.RestMappingSetVersion;
import net.sagebits.tmp.isaac.rest.api1.data.semantic.RestSemanticDescriptionVersion;
//import net.sagebits.tmp.isaac.rest.api1.data.workflow.RestWorkflowAvailableAction;
//import net.sagebits.tmp.isaac.rest.api1.data.workflow.RestWorkflowDefinition;
//import net.sagebits.tmp.isaac.rest.api1.data.workflow.RestWorkflowProcess;
//import net.sagebits.tmp.isaac.rest.api1.data.workflow.RestWorkflowProcessHistoriesMapEntry;
//import net.sagebits.tmp.isaac.rest.api1.data.workflow.RestWorkflowProcessHistory;

/**
 * {@link ArrayUnwrappers}
 *
 * Jersey already enhances JaxB to properly handle arrays and lists when is marshalls to XML. However, we are using raw jaxb, not
 * jersey enhanced jaxb to desserialize, so we need these silly stubs to deserialize arrays properly.
 *
 *
 * @author <a href="mailto:daniel.armbrust.list@sagebits.net">Dan Armbrust</a>
 */
public class ArrayUnwrappers
{
	@XmlRootElement
	public static class RestCommentVersions implements ArrayUnwrapper
	{
		@XmlElement
		protected RestCommentVersion[] restCommentVersion;

		@Override
		public Object[] getValues()
		{
			return restCommentVersion;
		}
	}

	@XmlRootElement
	public static class RestSemanticDescriptionVersions implements ArrayUnwrapper
	{
		@XmlElement
		protected RestSemanticDescriptionVersion[] restSemanticDescriptionVersion = new RestSemanticDescriptionVersion[] {};

		@Override
		public Object[] getValues()
		{
			return restSemanticDescriptionVersion;
		}
	}

	@XmlRootElement
	public static class RestMappingSetVersions implements ArrayUnwrapper
	{
		@XmlElement
		protected RestMappingSetVersion[] restMappingSetVersion;

		@Override
		public Object[] getValues()
		{
			return restMappingSetVersion;
		}
	}

	@XmlRootElement
	public static class RestMappingItemVersions implements ArrayUnwrapper
	{
		@XmlElement
		protected RestMappingItemVersion[] restMappingItemVersion;

		@Override
		public Object[] getValues()
		{
			return restMappingItemVersion;
		}
	}

//	@XmlRootElement
//	public static class RestWorkflowDefinitions implements ArrayUnwrapper
//	{
//		@XmlElement
//		protected RestWorkflowDefinition[] restWorkflowDefinition;
//
//		@Override
//		public Object[] getValues()
//		{
//			return restWorkflowDefinition;
//		}
//	}
//
//	@XmlRootElement
//	public static class RestWorkflowProcessHistories implements ArrayUnwrapper
//	{
//		@XmlElement
//		protected RestWorkflowProcessHistory[] restWorkflowProcessHistory;
//
//		@Override
//		public Object[] getValues()
//		{
//			return restWorkflowProcessHistory;
//		}
//	}
//
//	@XmlRootElement
//	public static class RestWorkflowProcessHistoriesMapEntries implements ArrayUnwrapper
//	{
//		@XmlElement
//		protected RestWorkflowProcessHistoriesMapEntry[] restWorkflowProcessHistoriesMapEntry;
//
//		@Override
//		public Object[] getValues()
//		{
//			return restWorkflowProcessHistoriesMapEntry;
//		}
//	}
//
//	@XmlRootElement
//	public static class RestWorkflowAvailableActions implements ArrayUnwrapper
//	{
//		@XmlElement
//		protected RestWorkflowAvailableAction[] restWorkflowAvailableAction;
//
//		@Override
//		public Object[] getValues()
//		{
//			return restWorkflowAvailableAction;
//		}
//	}

	@XmlRootElement
	public static class RestAssociationTypeVersions implements ArrayUnwrapper
	{
		@XmlElement
		protected RestAssociationTypeVersion[] restAssociationTypeVersion = new RestAssociationTypeVersion[] {};

		@Override
		public Object[] getValues()
		{
			return restAssociationTypeVersion;
		}
	}

	@XmlRootElement
	public static class RestAssociationItemVersions implements ArrayUnwrapper
	{
		@XmlElement
		protected RestAssociationItemVersion[] restAssociationItemVersion = new RestAssociationItemVersion[] {};

		@Override
		public Object[] getValues()
		{
			return restAssociationItemVersion;
		}
	}

//	@XmlRootElement
//	public static class RestWorkflowProcesses implements ArrayUnwrapper
//	{
//		@XmlElement
//		protected RestWorkflowProcess[] restWorkflowProcess;
//
//		@Override
//		public Object[] getValues()
//		{
//			return restWorkflowProcess;
//		}
//	}

	@XmlRootElement
	public static class RestMappingSetDisplayFields implements ArrayUnwrapper
	{
		@XmlElement
		protected RestMappingSetDisplayField[] restMappingSetDisplayField;

		@Override
		public Object[] getValues()
		{
			return restMappingSetDisplayField;
		}
	}

	@XmlRootElement
	public static class RestMappingSetDisplayFieldCreates implements ArrayUnwrapper
	{
		@XmlElement
		protected RestMappingSetDisplayFieldCreate[] restMappingSetDisplayFieldCreate;

		@Override
		public Object[] getValues()
		{
			return restMappingSetDisplayFieldCreate;
		}
	}

	@XmlRootElement
	public static class RestMapSetItemComponentTypes implements ArrayUnwrapper
	{
		@XmlElement
		protected RestMapSetItemComponentType[] restMapSetItemComponentType;

		@Override
		public Object[] getValues()
		{
			return restMapSetItemComponentType;
		}
	}

	@XmlRootElement
	public static class RestConceptChronologies implements ArrayUnwrapper
	{
		@XmlElement
		protected RestConceptChronology[] restConceptChronology;

		@Override
		public Object[] getValues()
		{
			return restConceptChronology;
		}
	}
}
