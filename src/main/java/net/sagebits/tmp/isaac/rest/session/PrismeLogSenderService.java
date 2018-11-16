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

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.ThreadContext.ContextStack;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.impl.ThrowableProxy;
//import org.apache.logging.log4j.core.time.Instant;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.spi.StandardLevel;
import org.apache.logging.log4j.util.ReadOnlyStringMap;
import org.codehaus.plexus.util.StringUtils;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.sagebits.tmp.isaac.rest.ApplicationConfig;
import sh.isaac.api.LookupService;

/**
 *
 * {@link PrismeLogSenderService}
 *
 * Create and manage a thread that performs a blocking read on a static EVENT_QUEUE populated by {@link PrismeLogAppender}
 * and sends {@link LogEvent} events to PRISME via rest call if service prisme_notify_url is configured in PRISME properties
 *
 * @author <a href="mailto:joel.kniaz.list@gmail.com">Joel Kniaz</a>
 */
//We start this service, when we reach a level where isaac starts.
@RunLevel(LookupService.SL_L1)
@Service
public class PrismeLogSenderService
{
	private static Logger LOGGER = LogManager.getLogger(PrismeLogSenderService.class);

	/**
	 * POISON_PILL_SHUTDOWN_MARKER ends blocking read on EVENT_QUEUE
	 */
	private final static LogEvent POISON_PILL_SHUTDOWN_MARKER = new LogEvent() {
		private static final long serialVersionUID = 1L;
		@SuppressWarnings("deprecation")  //Eclipse claims this isn't necessary, but it is, to make javac be quiet in maven
		@Override public Map<String, String> getContextMap() { return null; }
		@Override public ContextStack getContextStack() { return null; }
		@Override public String getLoggerFqcn() { return null; }
		@Override public Level getLevel() { return null; }
		@Override public String getLoggerName() { return null; }
		@Override public Marker getMarker() { return null; }
		@Override public Message getMessage() { return null; }
		@Override public long getTimeMillis() { return 0; }
		@Override public StackTraceElement getSource() { return null; }
		@Override public String getThreadName() { return null; }
		@Override public Throwable getThrown() { return null; }
		@Override public ThrowableProxy getThrownProxy() {return null;}
		@Override public boolean isEndOfBatch() { return false; }
		@Override public boolean isIncludeLocation() { return false; }
		@Override public void setEndOfBatch(boolean endOfBatch) {}
		@Override public void setIncludeLocation(boolean locationRequired) {}
		@Override public long getNanoTime() { return 0;}
		@Override public ReadOnlyStringMap getContextData() { return null;}
		@Override public long getThreadId(){ return 0;}
		@Override public int getThreadPriority() {return 0;}
		@Override public LogEvent toImmutable() {return null;}
//		@Override public Instant getInstant() {return null;}
	};

	/*
	 * Attempt to load PRISME notification API rest URL from PRISME properties
	 */
	private String PRISME_NOTIFY_URL;

	/*
	 * Cached Client instance
	 */
	private Client CLIENT = null;

	/*
	 * The static event queue. PrismeLogAppender is a noop while this is null.
	 */
	private static BlockingQueue<LogEvent> EVENT_QUEUE = new LinkedBlockingQueue<>();
	private volatile static boolean sendEvents = false;

	private final Object waitLockObject_ = new Object();

	/**
	 * Constructor to be invoked only by HK2
	 */
	private PrismeLogSenderService()
	{
		// For HK2
		// Construct WebTarget from Client getClient(), constructing and caching new Client getClient() if null
		CLIENT = ClientBuilder.newClient();
	}

	public static void enqueue(LogEvent logEvent)
	{
		if (sendEvents)
		{
			EVENT_QUEUE.add(logEvent);
		}
	}

	public String getPrismeNotifyUrl()
	{
		return RestConfig.getInstance().getPrismeNotifyURL();
	}

	public String getApplicationName()
	{
		return ApplicationConfig.getInstance().getContextPath();
	}

	@PostConstruct
	public void startupPrismeLogSenderService()
	{
		final Runnable runnable = new Runnable()
		{
			@Override
			public void run()
			{

				// disable() if not configured
				LOGGER.info("prisme log sender thread init");
				PRISME_NOTIFY_URL = getPrismeNotifyUrl();

				if (StringUtils.isBlank(PRISME_NOTIFY_URL))
				{
					LOGGER.warn("CANNOT LOG EVENTS TO PRISME LOGGER API BECAUSE prisme_notify_url NOT CONFIGURED IN prisme.properties");
					shutdownPrismeLogSenderService();
					return;
				}

				String temp = PRISME_NOTIFY_URL;
				if (temp.contains("="))
				{
					temp = temp.substring(0, temp.indexOf('='));
				}

				LOGGER.info("Configuring the logger API to send to '{}'", temp);
				sendEvents = true;

				final int maxAttemptsForMessage = 10;
				int attemptsForMessage = 0;

				final int initialWait = 0;
				int wait = initialWait;

				final int initialIncrement = 100;
				int increment = initialIncrement;

				final int maxWait = 1000 * 60 * 2; // 2 minute maximum wait
				final int maxQueueSize = 200;
				LogEvent eventToSend = null;
				while (sendEvents)
				{
					try
					{
						// Read log event, blocking on empty queue
						if (eventToSend != null && attemptsForMessage < maxAttemptsForMessage)
						{
							attemptsForMessage++;
							LOGGER.info("PrismeLogSenderService: retrying send of log event for attempt " + attemptsForMessage + " of " + maxAttemptsForMessage
									+ ": " + eventToSend);
						}
						else
						{
							attemptsForMessage = 0;
							eventToSend = sendEvents ? EVENT_QUEUE.take() : POISON_PILL_SHUTDOWN_MARKER;

							// If read POISON_PILL_SHUTDOWN_MARKER from queue then shutdown
							if (eventToSend == POISON_PILL_SHUTDOWN_MARKER)
							{
								// Shutdown
								shutdownPrismeLogSenderService();
								return; // exit thread
							}
						}

						if (!sendEvents)
						{
							return;
						}

						if (eventToSend != null)
						{
							// Send log event
							sendEvent(eventToSend);
						}
						else
						{
							// This should never happen
						}

						// If sendEvent succeeded then set eventToSend = null so it won't attempt to resend
						eventToSend = null;
						wait = initialWait;
						increment = initialIncrement;
					}
					catch (Exception re)
					{
						if (!sendEvents)
						{
							// No problem. Just shutting down anyway.
							return;
						}

						if (eventToSend != null)
						{
							LOGGER.error("FAILED SENDING LOG EVENT TO PRISME: " + eventToSend, re);
						}
						else
						{
							// This should never happen
							LOGGER.error("FAILED SENDING null LOG EVENT TO PRISME", re);
						}

						// Wait (WAIT_LOCK_OBJECT.wait(wait)), if wait > 0
						if (wait > 0)
						{
							try
							{
								LOGGER.debug("PrismeLogSenderService: WAITING " + (wait / 1000) + " SECONDS");
								synchronized (waitLockObject_)
								{
									waitLockObject_.wait(wait); // TODO Joel should handle spurious wakeup?
								}
							}
							catch (InterruptedException e)
							{
								// ignore
							}
						}

						// If less than maxWait, then increment wait and increment
						if (wait < maxWait)
						{
							wait = wait + increment;
							increment = wait;
						}

						// Enforce cap on wait
						if (wait >= maxWait)
						{
							wait = maxWait;
							increment = 0;
						}
					}

					// Remove and discard excess events,
					// calling disable() and break if encountering POISON_PILL_SHUTDOWN_MARKER
					while (sendEvents && EVENT_QUEUE.size() > maxQueueSize && EVENT_QUEUE.size() > 0)
					{
						LogEvent eventToDiscard = EVENT_QUEUE.remove();
						if (eventToDiscard == POISON_PILL_SHUTDOWN_MARKER)
						{
							// Shutdown
							shutdownPrismeLogSenderService();
							return; // exit thread
						}
					}
				} // End run() outer while loop
			} // End run()
		}; // End Runnable declaration

		LOGGER.info("Starting {}...", this.getClass().getName());
		Thread t = new Thread(runnable);
		t.setDaemon(true);
		t.setName("PrismeLogSender");
		t.start();
		// force this one onto the queue, as the thread above likely won't be started yet, and the normal add method will toss it.
		EVENT_QUEUE.add(new Log4jLogEvent("LoggerConnectivity", null, "gov.vha.vuid.rest.session.LoggerConnectivity", Level.INFO,
				new SimpleMessage("Log forwarding started"), null, null));
	}

	@PreDestroy
	public void shutdownPrismeLogSenderService()
	{
		LOGGER.info("Disabling {}...", this.getClass().getName());
		if (sendEvents)
		{
			try
			{
				sendEvent(new Log4jLogEvent("LoggerConnectivity", null, "gov.vha.vuid.rest.session.LoggerConnectivity", Level.INFO,
						new SimpleMessage("Log forwarding stopping"), null, null));
			}
			catch (Exception e)
			{
				LOGGER.info("Failed to send the shutdown notification to prisme", e);
			}
		}
		sendEvents = false;

		synchronized (waitLockObject_)
		{
			waitLockObject_.notifyAll();
		}

		// End sleep wait, if waiting
		EVENT_QUEUE.clear();
		EVENT_QUEUE.add(POISON_PILL_SHUTDOWN_MARKER);
		// Don't null EVENT_QUEUE, causes all sorts of headaches.

		Client temp = CLIENT;
		CLIENT = null;
		if (temp != null)
		{
			temp.close();
		}

		LOGGER.info("Disabled {}", this.getClass().getName());
	}

	private void sendEvent(LogEvent event)
	{
		/*
		 * property name:
		 * prisme_notify_url
		 *
		 * example property:
		 * prisme_notify_url=http://localhost:3000/log_event?security_token=%5B%22u%5Cf%5Cx92%5CxBC%5Cx17%7D%5CxD1%5CxE4%5CxFB%5CxE5%5Cx99%5CxA3%5C%22
		 * %5CxE8%5C%5CK%22%2C+%22%3E%5Cx16%5CxDE%5CxA8v%5Cx14%5CxFF%5CxD2%5CxC6%5CxDD%5CxAD%5Cx9F%5Cx1D%5CxD1cF%22%5D
		 *
		 * example target with path:
		 * http://localhost:3000/log_event
		 *
		 * example parameters and values:
		 * security_token=%5B%22u%5Cf%5Cx92%5CxBC%5Cx17%7D%5CxD1%5CxE4%5CxFB%5CxE5%5Cx99%5CxA3%5C%22%5CxE8%5C%5CK%22%2C+%22%3E%5Cx16%5CxDE%5CxA8v%
		 * 5Cx14%5CxFF%5CxD2%5CxC6%5CxDD%5CxAD%5Cx9F%5Cx1D%5CxD1cF%22%5D
		 * application_name=isaac
		 * level=1
		 * tag=SOME_TAG
		 * message=broken
		 */

		if (event == POISON_PILL_SHUTDOWN_MARKER || event == null)
		{
			// Shutting down. Shouldn't even get here.
			return;
		}
		else
		{
			Message message = event.getMessage();
			if (message == null || message.getFormattedMessage() == null)
			{
				// Ignore events with null log messages
				return;
			}
		}
		if (event.getLoggerName() != null && LOGGER.getName().equalsIgnoreCase(event.getLoggerName()))
		{
			// Ignore log messages queued from this logger to avoid recursion
			return;
		}

		final String event_logged_key = "event_logged";
		final String validation_errors_key = "validation_errors";
		final String level_key = "level";
		final String application_name_key = "application_name";
		final String tag_key = "tag";
		final String message_key = "message";
		final String security_token_key = "security_token";
		final String token_error_key = "token_error";

		// This must be called after ApplicationConfig.onStartup()
		final String application_name_value = getApplicationName();

		Map<String, Object> dto = new HashMap<>();

		int prismeLevel = 1; // ALWAYS

		Level level = (event != null && event.getLevel() != null) ? event.getLevel() : Level.ALL;
		StandardLevel standardLevel = (level != null && level.getStandardLevel() != null) ? level.getStandardLevel() : StandardLevel.ALL;

		// LEVELS = {ALWAYS: 1, WARN: 2, ERROR: 3, FATAL: 4}
		switch (standardLevel)
		{
			case FATAL:
				prismeLevel = 4;
				break;
			case ERROR:
				prismeLevel = 3;
				break;
			case WARN:
				prismeLevel = 2;
				break;
			case ALL:
			case INFO:
			case OFF:
			case TRACE:
			case DEBUG:
				prismeLevel = 1;
				break;
			default :
				LOGGER.warn("ENCOUNTERED UNEXPECTED/UNSUPPORTED LogEvent StandardLevel VALUE: {}", standardLevel);
				break;
		}

		String tag = null;
		try
		{
			// If logger name is a class then make tag the class simple name
			tag = Class.forName(event.getLoggerName()).getSimpleName();
		}
		catch (Exception e)
		{
			// If logger name is not a class name then don't modify it
			tag = event.getLoggerName() != null ? event.getLoggerName() : "";
		}
		dto.put(level_key, prismeLevel);
		dto.put(application_name_key, application_name_value);
		dto.put(tag_key, tag);
		Message message = event.getMessage();
		dto.put(message_key, (message != null) ? message.getFormattedMessage() : "");

		String eventInputJson = null;
		try
		{
			// Attempt to convert input DTO into json
			eventInputJson = jsonIze(dto);
		}
		catch (IOException e)
		{
			LOGGER.error("FAILED GENERATING LOG EVENT JSON FROM MAP OF PRISME LOGGER API PARAMETERS: " + dto.toString(), e);

			// Manually craft json for an error message about this log failure
			eventInputJson = "{ \"" + level_key + "\":\"3\", \"" + tag_key + "\":\"LOGGING ERROR\", \"" + message_key
					+ "\":\"FAILED TO LOG EVENT TO PRISME. CHECK ISAAC LOGS.\", \"" + application_name_key + "\":\"" + application_name_value + "\" }";
		}

		// If PRISME_NOTIFY_URL config property is unset then disable()
		if (StringUtils.isBlank(PRISME_NOTIFY_URL))
		{
			shutdownPrismeLogSenderService();
			LOGGER.warn("CANNOT LOG EVENT TO PRISME LOGGER API BECAUSE prisme_notify_url NOT CONFIGURED IN prisme.properties: {}", dto.toString());
			return;
		}

		// Extract target with path from PRISME_NOTIFY_URL config property
		String targetWithPath = PRISME_NOTIFY_URL.replaceAll("\\?.*", "");
		// Extract security token from PRISME_NOTIFY_URL config property
		String securityToken = PRISME_NOTIFY_URL.replaceFirst(".*\\?" + security_token_key + "=", "");

		Client c = CLIENT;
		if (c == null)
		{
			// Shutdown while this was executing? Abort
			return;
		}
		WebTarget webTargetWithPath = c.target(targetWithPath);

		// Create map to store request params
		Map<String, String> params = new HashMap<>();
		// Add security token param to params map
		params.put(security_token_key, securityToken);

		// Post eventInputJson to webTargetWithPath with params
		String responseJson = PrismeServiceUtils.postJsonToPrisme(webTargetWithPath, eventInputJson, params);

		// Construct json ObjectMapper to read response
		ObjectMapper mapper = new ObjectMapper();
		Map<?, ?> map = null;
		try
		{
			// Read json response into Map object
			map = mapper.readValue(responseJson, Map.class);
		}
		catch (IOException e)
		{
			// If fail to read response then throw exception to be logged by catcher
			throw new RuntimeException("FAILED TO READ RESPONSE TO SUBMISSION OF LOG EVENT TO PRISME: " + eventInputJson, e);
		}

		// Attempt to evaluate response
		if (map == null || map.get(event_logged_key) == null || !map.get(event_logged_key).toString().equalsIgnoreCase("true"))
		{
			if (map != null && map.containsKey(validation_errors_key) && map.get(validation_errors_key) != null
					&& ((Map<?, ?>) (map.get(validation_errors_key))).size() > 0)
			{
				LOGGER.error("FAILED PUBLISHING LOG EVENT TO PRISME WITH {} VALIDATION ERRORS: {}, EVENT: {}",
						((Map<?, ?>) (map.get(validation_errors_key))).size(), ((Map<?, ?>) map.get(validation_errors_key)).toString(), eventInputJson);
			}
			else if (map != null && map.containsKey(token_error_key) && map.get(token_error_key) != null
					&& StringUtils.isNotBlank(map.get(token_error_key).toString()))
			{
				LOGGER.error("FAILED PUBLISHING LOG EVENT TO PRISME WITH TOKEN ERROR: {}, EVENT: {}", map.toString(), eventInputJson);
			}
			else if (map == null || (map != null && (map.get(event_logged_key) == null || !map.get(event_logged_key).toString().equalsIgnoreCase("false"))))
			{
				LOGGER.error("SENDING LOG EVENT {} RETURNED INVALID RESPONSE MESSAGE FROM PRISME: {}", eventInputJson, map);
				throw new RuntimeException("Invalid response received from PRISME");
			}
			else
			{
				LOGGER.error("FAILED PUBLISHING LOG EVENT {} TO PRISME WITH NO VALIDATION ERRORS IN RESPONSE: {}", eventInputJson, map);
			}
		}
		else
		{
			LOGGER.debug("SUCCEEDED publishing log event to PRISME: {}", eventInputJson);
		}
	}

	private String toJson(ObjectNode root) throws JsonProcessingException, IOException
	{
		StringWriter ws = new StringWriter();
		new ObjectMapper().writeTree(new JsonFactory().createGenerator(ws).setPrettyPrinter(new DefaultPrettyPrinter()), root);
		return ws.toString();
	}

	private String jsonIze(Map<String, Object> map) throws JsonProcessingException, IOException
	{
		ObjectNode root = JsonNodeFactory.instance.objectNode();
		for (Map.Entry<String, Object> entry : map.entrySet())
		{
			root.put(entry.getKey(), entry.getValue() != null ? (entry.getValue() + "") : null);
		}
		return toJson(root);
	}
}
