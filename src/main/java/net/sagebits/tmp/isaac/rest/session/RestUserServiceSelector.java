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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.inject.Singleton;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jvnet.hk2.annotations.Service;
import net.sagebits.tmp.isaac.rest.api.exceptions.RestException;
import net.sagebits.tmp.isaac.rest.tokens.EditToken;
import net.sagebits.uts.auth.rest.api1.data.RestUser;

/**
 * {@link RestUserServiceSelector}
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
@Service
@Singleton
class RestUserServiceSelector implements RestUserService
{
	private Logger log = LogManager.getLogger(RestUserServiceSelector.class);

	private RestUserService impl;

	public RestUserServiceSelector()
	{
		if (StringUtils.isNotBlank(RestConfig.getInstance().getAuthURL()))
		{
			log.info("All auth will be done via remote auth service {}", RestConfig.getInstance().getAuthURL());
			impl = new RestUserServiceAuthIntegrated();
		}
		else
		{
			log.info("All auth will be done via local service");
			impl = new RestUserServiceLocal();
		}
	}

	/**
	 * @see net.sagebits.tmp.isaac.rest.session.RestUserService#getUser(java.util.Map, net.sagebits.tmp.isaac.rest.tokens.EditToken)
	 */
	@Override
	public Optional<RestUser> getUser(Map<String, List<String>> requestParameters, EditToken editToken) throws RestException
	{
		return impl.getUser(requestParameters, editToken);
	}
}