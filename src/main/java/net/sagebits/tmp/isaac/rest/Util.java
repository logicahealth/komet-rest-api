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

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.sagebits.tmp.isaac.rest.api.exceptions.RestException;
import net.sagebits.tmp.isaac.rest.session.RequestInfo;
import sh.isaac.api.Get;
import sh.isaac.api.Status;
import sh.isaac.api.chronicle.LatestVersion;
import sh.isaac.api.commit.CommitRecord;
import sh.isaac.api.commit.CommitTask;
import sh.isaac.api.component.concept.ConceptChronology;
import sh.isaac.api.component.semantic.version.DescriptionVersion;
import sh.isaac.api.coordinate.LanguageCoordinate;
import sh.isaac.api.coordinate.StampCoordinate;
import sh.isaac.api.util.NumericUtils;
import sh.isaac.api.util.UUIDUtil;

public class Util
{
	public static final DateTimeFormatter ISO_DATE_TIME_FORMATTER = DateTimeFormatter.ISO_DATE_TIME.withZone(ZoneId.systemDefault());
	private static Logger log = LogManager.getLogger();

	public static UUID convertToConceptUUID(String conceptId) throws RestException
	{
		Optional<UUID> uuid = UUIDUtil.getUUID(conceptId);
		if (uuid.isPresent())
		{
			if (Get.identifierService().hasUuid(uuid.get()) && Get.conceptService().getOptionalConcept(uuid.get()).isPresent())
			{
				return uuid.get();
			}
			else
			{
				throw new RestException("The UUID '" + conceptId + "' Is not known by the system");
			}
		}
		else
		{
			OptionalInt numId = NumericUtils.getInt(conceptId);
			if (numId.isPresent() && numId.getAsInt() < 0)
			{
				if (numId.getAsInt() < 0)
				{
					return Get.identifierService().getUuidPrimordialForNid(numId.getAsInt());
				}
				else
				{
					Optional<? extends ConceptChronology> c = Get.conceptService().getOptionalConcept(numId.getAsInt());
					if (c.isPresent())
					{
						return c.get().getPrimordialUuid();
					}
					else
					{
						throw new RestException("The concept nid '" + conceptId + "' is not known by the system");
					}
				}
			}
			else
			{
				throw new RestException("The id '" + conceptId + "' does not appear to be a valid UUID, or NID for a concept");
			}
		}
	}

	/**
	 * Utility method to find the 'best' description for the concept at hand.
	 * 
	 * @param conceptId (nid)
	 * @return a description for the concept
	 */
	public static String readBestDescription(int conceptId)
	{
		return readBestDescription(conceptId, RequestInfo.get().getStampCoordinate());
	}

	public static String readBestDescription(int conceptId, StampCoordinate sc)
	{
		return readBestDescription(conceptId, sc, RequestInfo.get().getLanguageCoordinate());
	}

	public static String readBestDescription(int conceptNid, StampCoordinate sc, LanguageCoordinate lc)
	{
		LatestVersion<DescriptionVersion> descriptionOptional = lc.getDescription(Get.assemblageService().getDescriptionsForComponent(conceptNid), sc);

		if (descriptionOptional.isPresent())
		{
			if (!descriptionOptional.contradictions().isEmpty())
			{
				// Prefer active descriptions over inactive, if there was a contradiction (which means they tied the sort - have the same time)
				// common for a replacement description to have the same time as the retired one.
				if (descriptionOptional.get().getStatus() == Status.ACTIVE)
				{
					return descriptionOptional.get().getText();
				}
				else
				{
					for (DescriptionVersion ds : descriptionOptional.contradictions())
					{
						if (ds.getStatus() == Status.ACTIVE)
						{
							return ds.getText();
						}
					}
				}
			}
			return descriptionOptional.get().getText();
		}
		else
		{
			return null;
		}
	}

//	/**
//	 * @param workflowProcessId - The optional workflowProcessId. If non-blank, this must be a valid process id.
//	 * @return - the validated UUID, or null, if no workflowProcessId is submitted.
//	 * @throws RestException - if the provided non-blank value isn't valid.
//	 */
//	public static UUID validateWorkflowProcess(String workflowProcessId) throws RestException
//	{
//		Optional<UUID> processIdOptional = RequestInfoUtils.parseUuidParameterIfNonBlank(RequestParameters.processId, workflowProcessId);
//		if (processIdOptional.isPresent())
//		{
//			UUID temp = processIdOptional.get();
//			if (RequestInfo.get().getWorkflow().getWorkflowAccessor().getProcessDetails(temp) == null)
//			{
//				throw new RestException(RequestParameters.processId, workflowProcessId, "Not a valid workflow process");
//			}
//			return temp;
//		}
//		else
//		{
//			return null;
//		}
//	}
//
//	/**
//	 * Calls {@link #getPreWorkflowStampCoordinate(UUID, int)} after calling {@link #validateWorkflowProcess(String)}
//	 * 
//	 * @param workflowProcessId
//	 * @param componentNid
//	 * @return the coordinate
//	 * @throws RestException
//	 */
//	public static StampCoordinate getPreWorkflowStampCoordinate(String workflowProcessId, int componentNid) throws RestException
//	{
//		return getPreWorkflowStampCoordinate(workflowProcessId, componentNid, RequestInfo.get().getStampCoordinate());
//	}
//
//	public static StampCoordinate getPreWorkflowStampCoordinate(String workflowProcessId, int componentNid, StampCoordinate sc) throws RestException
//	{
//		return getPreWorkflowStampCoordinate(
//				// validateWorkflowProcess(workflowProcessId),
//				(UUID) null, componentNid, sc);
//	}
//
//	/**
//	 * If a workflowProcessId is passed, and the componentNid is present in the workflow, return the stamp
//	 * that occurred prior to the first change in workflow. Otherwise, returns the user specified stamp coordinate, from
//	 * {@link RequestInfo#getStampCoordinate()}
//	 * 
//	 * @param workflowProcessId - the id of the workflow process. If not provided, this method returns the result of
//	 *            {@link RequestInfo#getStampCoordinate()}
//	 * @param componentNid - the component to check for in the workflow. The componentNid must be a valid component identifier.
//	 *            If the component is not found in the workflow, the result of this method is simply {@link RequestInfo#getStampCoordinate()}
//	 * @return the found coordinate
//	 */
//	public static StampCoordinate getPreWorkflowStampCoordinate(UUID workflowProcessId, int componentNid)
//	{
//		return getPreWorkflowStampCoordinate(workflowProcessId, componentNid, RequestInfo.get().getStampCoordinate());
//	}
//
//	public static StampCoordinate getPreWorkflowStampCoordinate(UUID workflowProcessId, int componentNid, StampCoordinate sc)
//	{
//		if (workflowProcessId == null)
//		{
//		return sc;
//		}
//		else
//		{
//			if (RequestInfo.get().getWorkflow().getWorkflowAccessor().isComponentInProcess(workflowProcessId, componentNid))
//			{
//				StampedVersion version = RequestInfo.get().getWorkflow().getWorkflowAccessor().getVersionPriorToWorkflow(workflowProcessId, componentNid);
//				return Frills.getStampCoordinateFromVersion(version);
//			}
//			else
//			{
//				return sc;
//			}
//		}
//	}

	/**
	 * @param dateString - if null or blank, returns 0.
	 *            if long, parsed as a java time.
	 *            If "latest" - set to Long.MAX_VALUE.
	 *            Otherwise, parsed as {@link DateTimeFormatter#ISO_DATE_TIME}
	 * @return the date
	 * @throws DateTimeParseException 
	 */
	public static long parseDate(String dateString) throws DateTimeParseException
	{
		Optional<Long> l = NumericUtils.getLong(dateString);
		if (l.isPresent())
		{
			return l.get();
		}
		else
		{
			if (StringUtils.isBlank(dateString))
			{
				return 0;
			}
			if (dateString.trim().toLowerCase(Locale.ENGLISH).equals("latest"))
			{
				return Long.MAX_VALUE;
			}
			else
			{
				return Date.from(Instant.from(ISO_DATE_TIME_FORMATTER.parse(dateString))).getTime();
			}
		}
	}

//	public static InputStream getTerminologyConfigData()
//	{
//		// Prisme injects this into the war file, at deployment time.
//		log.debug("Looking for TerminologyConfig.xml from prisme");
//		InputStream is = Util.class.getClassLoader().getResourceAsStream("/prisme_files/TerminologyConfig.xml");
//		if (is == null)
//		{
//			log.warn("Failed to find TerminologyConfig.xml from prisme!  Using embedded default config!");
//			// this file comes from the vhat-constants module
//			is = Util.class.getClassLoader().getResourceAsStream("/TerminologyConfigDefault.xml");
//		}
//		if (is == null)
//		{
//			throw new RuntimeException("Unable to find Terminology Config!");
//		}
//		return is;
//	}
//
//	public static InputStream getTerminologyConfigSchema()
//	{
//		// Prisme injects this into the war file, at deployment time.
//		log.debug("Looking for TerminologyConfig.xsd from prisme");
//		InputStream is = Util.class.getClassLoader().getResourceAsStream("/prisme_files/TerminologyConfig.xsd");
//		if (is == null)
//		{
//			log.warn("Failed to find TerminologyConfig.xsd from prisme!  Using embedded default config!");
//			// this file comes from the vhat-constants module
//			is = Util.class.getClassLoader().getResourceAsStream("/TerminologyConfig.xsd.hidden");
//		}
//		if (is == null)
//		{
//			throw new RuntimeException("Unable to find Terminology Config Schema!!");
//		}
//		return is;
//	}

	public static CommitRecord commitCheck(CommitTask commitTask) throws RestException
	{
		try
		{
			Optional<CommitRecord> cr = commitTask.get();
			if (!cr.isPresent())
			{
				log.error("Commit Failure - reasons - {}", Arrays.toString(commitTask.getAlerts().toArray()));
				throw new RestException("Internal commit failure");
			}
			return cr.get();
		}
		catch (RestException e)
		{
			throw e;
		}
		catch (Exception e)
		{
			log.error("Commit Failure", e);
			throw new RestException("Internal commit failure!");
		}
	}

	/**
	 * @param logger 
	 * @param latestVersion
	 */
	public static void logContradictions(Logger logger, LatestVersion<?> latestVersion)
	{
		if (latestVersion.isContradicted())
		{
			logger.warn("Contradictions exist on the object {} - {}", latestVersion, Arrays.toString(latestVersion.contradictions().toArray()));
		}
	}
}
