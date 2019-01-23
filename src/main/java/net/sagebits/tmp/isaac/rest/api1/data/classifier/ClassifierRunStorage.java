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
 */
package net.sagebits.tmp.isaac.rest.api1.data.classifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.cedarsoftware.util.io.JsonReader;
import com.cedarsoftware.util.io.JsonWriter;
import sh.isaac.api.Get;
import sh.isaac.api.classifier.ClassifierResults;
import sh.isaac.api.util.UuidT5Generator;

/**
 * {@link ClassifierRunStorage}
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a> 
 */
public class ClassifierRunStorage
{
	private static Logger log = LogManager.getLogger();
	private static final String CLASSIFICATION_STORE = "classificationStore";
	private static final UUID hashNamespace = UUID.randomUUID();  //We randomize this, because the ids given back by classifier runs are simple
	//ints, which are only unique in a JVM.  We want to store classifier results, so I need globally unique IDs.  Creating UUIDs from the ints, 
	//combined with a per-jvm unique namespace will give me globally unique ids.
	
	/**
	 * For use by the read API to read back the classifier results. 
	 * @param classifyKey - the classification to read.
	 * @return The ClassifierResults, if available, null if the key is unknown.
	 */
	public static ClassifierResult getClassificationResults(UUID classifyKey)
	{
		String temp = Get.metaContentService().<UUID, String>openStore(CLASSIFICATION_STORE).get(classifyKey);
		if (temp == null)
		{
			return null;
		}
		else
		{
			return (ClassifierResult)JsonReader.jsonToJava(temp);
		}
	}
	
	/**
	 * Get all stored classifier info, sorted by launch time, most recent to oldest.
	 * @return
	 */
	public static List<ClassifierResult> getClassificationResults()
	{
		ArrayList<ClassifierResult> results = new ArrayList<>();
		for(String s : Get.metaContentService().<UUID, String>openStore(CLASSIFICATION_STORE).values())
		{
			ClassifierResult rcr = (ClassifierResult)JsonReader.jsonToJava(s); 
			results.add(rcr);
		}
		Collections.sort(results);
		return results;
	}
	
	public static void storeResult(UUID classifyKey, ClassifierResults cr)
	{
		ClassifierResult rcr = getClassificationResults(classifyKey);
		//clearStoredData called during classify
		if (rcr == null)
		{
			rcr = new ClassifierResult(classifyKey);
		}
		rcr.completed(cr);
		String temp = JsonWriter.objectToJson(rcr);
		log.debug("Classification Results:" + cr.toString());
		Get.metaContentService().<UUID, String>openStore(CLASSIFICATION_STORE).put(classifyKey, temp);
	}
	
	public static void storeFailure(UUID classifyKey, Exception e)
	{
		ClassifierResult rcr = getClassificationResults(classifyKey);
		//clearStoredData called during classify
		if (rcr == null)
		{
			rcr = new ClassifierResult(classifyKey);
		}
		rcr.failed(e);
		String temp = JsonWriter.objectToJson(rcr);
		log.info("Classification Failure Results:" + temp);
		Get.metaContentService().<UUID, String>openStore(CLASSIFICATION_STORE).put(classifyKey, temp);
	}
	
	public static UUID classifyStarted(int classifierTaskId)
	{
		final UUID classifyKey = UuidT5Generator.get(hashNamespace, "classifierHash" + classifierTaskId);
		String temp = JsonWriter.objectToJson(new ClassifierResult(classifyKey));
		Get.metaContentService().<UUID, String>openStore(CLASSIFICATION_STORE).put(classifyKey, temp);
		return classifyKey;
	}
	
	public static void clearStoredData()
	{
		Get.metaContentService().<UUID, String>openStore(CLASSIFICATION_STORE).clear();
		log.info("Classifier run data cleared");
	}
}
