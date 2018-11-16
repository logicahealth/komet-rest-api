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

package net.sagebits.tmp.isaac.rest.session;

import net.sagebits.tmp.isaac.rest.api.exceptions.RestException;
import sh.isaac.api.Status;
import sh.isaac.api.chronicle.Chronology;
import sh.isaac.api.chronicle.LatestVersion;
import sh.isaac.api.chronicle.Version;
import sh.isaac.api.component.concept.ConceptChronology;
import sh.isaac.api.component.concept.ConceptVersion;
import sh.isaac.api.component.semantic.SemanticChronology;
import sh.isaac.api.component.semantic.version.SemanticVersion;
import sh.isaac.api.coordinate.StampCoordinate;
import sh.isaac.api.identity.StampedVersion;
import sh.isaac.utility.Frills;

/**
 * 
 * {@link LatestVersionUtils}
 *
 * @author <a href="mailto:joel.kniaz.list@gmail.com">Joel Kniaz</a>
 *
 */
public class LatestVersionUtils
{
//	private static Logger log = LogManager.getLogger(LatestVersionUtils.class);

	private LatestVersionUtils()
	{
	}

	/**
	 * Calls {@link #getLatestVersionForUpdate(Chronology, Class)} with ConceptVersion.class for the Class
	 * 
	 * @param conceptChronology
	 * @return the concept
	 * @throws RestException
	 */
	public static <T extends ConceptVersion> LatestVersion<ConceptVersion> getLatestVersionForUpdate(ConceptChronology conceptChronology) throws RestException
	{
		return getLatestVersionForUpdate(conceptChronology, ConceptVersion.class);
	}

	/**
	 * Calls {@link #getLatestVersionForUpdate(Chronology, Class)} with SemanticVersion.class for the Class
	 * 
	 * @param semanticChronology
	 * @return the Semantic
	 * @throws RestException
	 */
	public static <T extends SemanticVersion> LatestVersion<SemanticVersion> getLatestVersionForUpdate(SemanticChronology semanticChronology)
			throws RestException
	{
		return getLatestVersionForUpdate(semanticChronology, SemanticVersion.class);
	}

	/**
	 * First, tries to read the current version using the passed in stamp (but any state), and the module from the edit coordinate.
	 * If no version is present using the edit coordinate module, then it tries again using the module(s) from the current read coordinate, with any
	 * state.
	 * 
	 * @param objectChronology
	 * @param clazz
	 * @return The object version
	 * @throws RestException
	 */
	@SuppressWarnings("unchecked")
	public static <T extends StampedVersion> LatestVersion<T> getLatestVersionForUpdate(Chronology objectChronology, Class<T> clazz) throws RestException
	{
		StampCoordinate sc = Frills.makeStampCoordinateAnalogVaryingByModulesOnly(RequestInfo.get().getStampCoordinate(),
				RequestInfo.get().getEditCoordinate().getModuleNid(), null).makeCoordinateAnalog(Status.values()).makeCoordinateAnalog(Long.MAX_VALUE);

		LatestVersion<Version> latestVersion = objectChronology.getLatestVersion(sc);
		if (!latestVersion.isPresent())
		{
			sc = RequestInfo.get().getStampCoordinate().makeCoordinateAnalog(Status.values()).makeCoordinateAnalog(Long.MAX_VALUE);
			latestVersion = objectChronology.getLatestVersion(sc);
		}

		return (LatestVersion<T>) latestVersion;
	}
}
