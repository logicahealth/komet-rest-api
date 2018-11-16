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

import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This class is a generator for testing log4j2 configuration.
 * It will print out logs in form of "Logging in user <some userame> with id <some id>.
 * It uses static lists of users and loggers.
 * 
 * @author <a href="mailto:joel.kniaz.list@gmail.com">Joel Kniaz</a>
 * 
 */
class LogGenerator
{
	/** List of users. */
	private static final List<String> users = new ArrayList<String>();
	/** List of loggers. */
	private static final List<Logger> loggers = new ArrayList<Logger>();

	/** Some static initialization of users- and logger-lists. */
	static
	{
		users.add("dan");
		users.add("joel");
		users.add("reema");
		users.add("tim");
		users.add("esigie");
		users.add("cris");
		users.add("vikas");
		users.add("jesse");
		users.add("frank");

		// the loggers that will appear in the log4j2.xml
		loggers.add(LogManager.getLogger("net.sagebits.tmp.isaac.rest"));
		loggers.add(LogManager.getLogger("gov.vha"));
		loggers.add(LogManager.getLogger(LogGenerator.class.getName()));
		loggers.add(LogManager.getLogger());
	}

	/**
	 * Entry-point.
	 * 
	 * @param args doesn't matter in this case
	 */
	public static void main(String[] args)
	{
		while (true)
		{
			int logLevel = getRandomNumber(0, 3); // out of 4 different log-levels: error, warn, info, debug
			int loggerId = getRandomNumber(0, loggers.size() - 1); // out of however many loggers
			Logger logger = loggers.get(loggerId);
			int userId = getRandomNumber(0, users.size() - 1); // out of however many users
			String user = users.get(userId);

			log(user, userId, logger, logLevel);

			try
			{
				long sleeptime = getRandomNumber(200, 500);
				Thread.sleep(sleeptime);
			}
			catch (InterruptedException e)
			{
				System.err.println(e.getMessage());
			}

		}
	}

	/**
	 * Method to do the actual logging.
	 * 
	 * @param user the username
	 * @param userId the user-id
	 * @param logger the logger to use
	 * @param logLevel the loglevel to use
	 */
	private static void log(String user, int userId, Logger logger, int logLevel)
	{
		switch (logLevel)
		{
			case 0:
				logger.error("Logging in user {} with id {} ", user, userId);
				logger.error("Caught exception", new Exception("Example Exception"));
				break;
			case 1:
				logger.warn("Logging in user {} with id {} ", user, userId);
				break;
			case 2:
				logger.info("Logging in user {} with id {} ", user, userId);
				break;
			case 3:
				logger.debug("Logging in user {} with id {} ", user, userId);
				break;
		}
	}

	/**
	 * Method to wrap random-mechanism. gets a random number in range of [min...max].
	 * 
	 * @param min
	 *            the minium-value
	 * @param max
	 *            the maximum-value
	 * @return the random int.
	 */
	private static int getRandomNumber(int min, int max)
	{
		int incMax = max + 1;
		int random = min + (int) (Math.random() * (incMax - min) + min);
		return random;
	}
}