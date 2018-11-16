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

package net.sagebits.tmp.isaac.rest.tokens;

import sh.isaac.api.Get;

/**
 * 
 * {@link CoordinatesTokenDebugUtil}
 *
 * @author <a href="mailto:joel.kniaz.list@gmail.com">Joel Kniaz</a>
 *
 */
class CoordinatesTokenDebugUtil
{
	private CoordinatesTokenDebugUtil()
	{
	}

	public static String conceptsToString(int[] nids)
	{
		if (nids == null)
		{
			return null;
		}
		StringBuilder sb = new StringBuilder();
		sb.append("Concept Array [");

		for (int i = 0; i < nids.length; ++i)
		{
			if (i > 0)
			{
				sb.append(',');
			}
			sb.append(Get.conceptDescriptionText(nids[i]));
		}

		sb.append("]");

		return sb.toString();
	}

	public static String toString(CoordinatesToken token)
	{
		return "CoordinatesToken [getStampTime()=" + token.getStampTime() + ", getStampPath()=" + Get.conceptDescriptionText(token.getStampPath())
				+ ", getStampPrecedence()=" + token.getStampPrecedence() + ", getStampModules()=" + token.getStampModules() + ", getStampStates()="
				+ token.getStampStates() + ", getLangCoord()=" + Get.conceptDescriptionText(token.getLangCoord()) + ", getLangDialects()="
				+ conceptsToString(token.getLangDialects()) + ", getLangDescTypePrefs()=" + conceptsToString(token.getLangDescTypePrefs())
				+ ", getTaxonomyType()=" + token.getTaxonomyType() + ", getLogicStatedAssemblage()="
				+ Get.conceptDescriptionText(token.getLogicStatedAssemblage()) + ", getLogicInferredAssemblage()="
				+ Get.conceptDescriptionText(token.getLogicInferredAssemblage()) + ", getLogicDescLogicProfile()=" + token.getLogicDescLogicProfile()
				+ ", getLogicClassifier()=" + Get.conceptDescriptionText(token.getLogicClassifier()) + ", getSerialized()=" + token.getSerialized() + "]";
	}
}
