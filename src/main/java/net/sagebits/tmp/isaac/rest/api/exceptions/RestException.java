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

package net.sagebits.tmp.isaac.rest.api.exceptions;

import java.io.IOException;
import org.apache.commons.lang3.StringUtils;

/**
 * 
 * {@link RestException}
 *
 * @author <a href="mailto:daniel.armbrust.list@sagebits.net">Dan Armbrust</a>
 */
public class RestException extends IOException
{
	private static final long serialVersionUID = 1L;

	String parameterName_;
	String parameterValue_;

	public RestException(String message)
	{
		super(message);
	}

	public RestException(String parameterName, String message)
	{
		super(message);
		parameterName_ = parameterName;
	}

	public RestException(String parameterName, String parameterValue, String message)
	{
		super(message);
		parameterName_ = parameterName;
		parameterValue_ = parameterValue;
	}

	/**
	 * @return the parameterName
	 */
	public String getParameterName()
	{
		return parameterName_;
	}

	/**
	 * @return the parameterValue
	 */
	public String getParameterValue()
	{
		return parameterValue_;
	}

	public String getParameterSpecificMessage()
	{
		if (StringUtils.isNotBlank(parameterName_))
		{
			return "The parameter '" + parameterName_ + "' " + (StringUtils.isNotBlank(parameterValue_) ? "with value '" + parameterValue_ + "'" : "")
					+ "  resulted in the error: " + getMessage();
		}
		else if (StringUtils.isNotBlank(parameterValue_))
		{
			return "The parameter value '" + parameterValue_ + "'" + "  resulted in the error: " + getMessage();
		}
		else
		{
			return null;
		}
	}

	@Override
	public String toString()
	{
		String parameterSpecificMessage = getParameterSpecificMessage();
		if (parameterSpecificMessage != null)
		{
			return parameterSpecificMessage;
		}
		else
		{
			return getMessage();
		}
	}
}
