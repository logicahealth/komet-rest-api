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

package net.sagebits.tmp.isaac.rest;

/**
 * 
 * {@link ExpandUtil}
 *
 * @author <a href="mailto:daniel.armbrust.list@sagebits.net">Dan Armbrust</a>
 */
public class ExpandUtil
{
	public static final String versionsAllExpandable = "versionsAll";
	public static final String versionsLatestOnlyExpandable = "versionsLatestOnly";
	public static final String versionExpandable = "version";
	public static final String chronologyExpandable = "chronology";
	public static final String uuid = "uuid";
	public static final String referencedConcept = "referencedConcept";
	public static final String nestedSemanticsExpandable = "nestedSemantics";
	public static final String referencedDetails = "referencedDetails";
	public static final String comments = "comments";
	public static final String source = "source";
	public static final String target = "target";
	public static final String countParents = "countParents";
	public static final String includeParents = "includeParents";
	public static final String terminologyType = "terminologyType";
}
