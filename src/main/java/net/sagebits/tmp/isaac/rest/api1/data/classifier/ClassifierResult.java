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
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.apache.mahout.math.list.IntArrayList;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import sh.isaac.api.classifier.ClassifierResults;

/**
 * {@link ClassifierResult}
 *
 * A simple class for doing JSON storage of classifier results into the metadata store.
 * 
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a> 
 */

@XmlRootElement
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE)
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY)
public class ClassifierResult implements Comparable<ClassifierResult>
{
	/**
	 * The time when this classification was started (in standard java form)
	 */
	@XmlElement
	protected long launchTime;
	
	/**
	 * The time when this classification was completed (in standard java form).  Null / not provided if it is still running.
	 */	
	@XmlElement
	@JsonInclude(JsonInclude.Include.NON_NULL)
	protected Long completeTime;
	
	/**
	 * The concepts affected by this classification.
	 */
	@XmlElement
	@JsonInclude(JsonInclude.Include.NON_NULL)
	protected int[] affectedConcepts;
	
	/**
	 * The equivalent sets identified by the classifier.  Only provided upon successful completion.  
	 */
	@XmlElement
	@JsonInclude(JsonInclude.Include.NON_NULL)
	protected List<int[]> equivalentSets;
	
	/**
	 * The cycles identified by the classification process.
	 */
	@XmlElement
	@JsonInclude(JsonInclude.Include.NON_NULL)
	protected List<ClassifierCycle> cycles;
	
	/**
	 * Orphaned concepts identified by the classification process.
	 */
	@XmlElement
	@JsonInclude(JsonInclude.Include.NON_NULL)
	protected int[] orphanedConcepts;
	
	/**
	 * The ID of this classification run
	 */
	@XmlElement
	protected String classificationId;
	
	/**
	 * A simple summary of the current status, such as Running, Failed, or Completed.
	 * If failed, this will contain some information on the cause of the failure.
	 */
	@XmlElement
	protected String status;
	
	protected ClassifierResult()
	{
		//For jaxb
	}
	
	public ClassifierResult(UUID classificationId)
	{
		this.classificationId = classificationId.toString();
		this.launchTime = System.currentTimeMillis();
		this.status = "Running";
	}
	
	public void completed(ClassifierResults cr)
	{
		this.completeTime = System.currentTimeMillis();
		this.status = "Done";
		
		if (cr.getAffectedConcepts() != null && cr.getAffectedConcepts().size() > 0)
		{
			this.affectedConcepts = new int[cr.getAffectedConcepts().size()];
			int i = 0;
			for (int nid : cr.getAffectedConcepts())
			{
				this.affectedConcepts[i++] = nid;
			}
		}
		
		if (cr.getEquivalentSets() != null && cr.getEquivalentSets().size() > 0)
		{
			this.equivalentSets = new ArrayList<int[]>(cr.getEquivalentSets().size());
			for (IntArrayList ial : cr.getEquivalentSets())
			{
				int j = 0;
				int[] set = new int[ial.size()];
				this.equivalentSets.add(set);
				for (int nid : ial.elements())
				{
					set[j++] = nid;
				}
			}
		}
		
		if (cr.getCycles().isPresent() && cr.getCycles().get().size() > 0)
		{
			cycles = new ArrayList<>();
			for (Entry<Integer, Set<int[]>> cycle : cr.getCycles().get().entrySet())
			{
				cycles.add(new ClassifierCycle(cycle.getKey(), cycle.getValue()));
			}
		}
		
		if (cycles != null && cycles.size() > 0)
		{
			status = "Failed due to cycles";
		}
		
		if (cr.getOrphans() != null && cr.getOrphans().size() > 0)
		{
			this.orphanedConcepts = new int[cr.getOrphans().size()];
			int j = 0;
			for (Integer orphan : cr.getOrphans())
			{
				orphanedConcepts[j++] = orphan;
			}
		}
	}
	
	public void failed(Exception reason)
	{
		this.completeTime = System.currentTimeMillis();
		this.status = "Failed: " + reason;
	}

	/**
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(ClassifierResult o)
	{
		return Long.compare(this.launchTime, o.launchTime);
	}
}
