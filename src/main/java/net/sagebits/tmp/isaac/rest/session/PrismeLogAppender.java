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

import java.io.Serializable;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;

/**
 * 
 * {@link PrismeLogAppender}
 *
 * @author <a href="mailto:joel.kniaz.list@gmail.com">Joel Kniaz</a>
 *
 */
@Plugin(name = "PrismeLogAppender", category = "Core", elementType = "appender", printObject = true)
public class PrismeLogAppender extends AbstractAppender
{
	public static final long serialVersionUID = -228479087489358210L;

	/**
	 * @param name
	 * @param filter
	 * @param layout
	 */
	public PrismeLogAppender(String name, Filter filter, Layout<? extends Serializable> layout)
	{
		super(name, filter, layout);
	}

	/**
	 * @param name
	 * @param filter
	 * @param layout
	 * @param ignoreExceptions
	 */
	public PrismeLogAppender(String name, Filter filter, Layout<? extends Serializable> layout, boolean ignoreExceptions)
	{
		super(name, filter, layout, ignoreExceptions);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void append(LogEvent event)
	{
		PrismeLogSenderService.enqueue(event);
	}

	// Custom appenders need to declare a factory method
	// annotated with `@PluginFactory`. Log4j will parse the configuration
	// and call this factory method to construct an appender instance with
	// the configured attributes.
	@PluginFactory
	public static PrismeLogAppender createAppender(@PluginAttribute("name") String name, @PluginAttribute("ignoreExceptions") Boolean ignoreExceptions,
			@PluginElement("Filter") final Filter filter, @PluginElement("Layout") Layout<? extends Serializable> layout)
	{
		if (name == null)
		{
			LOGGER.error("No name provided for PrismeLogAppender");
			return null;
		}
		if (layout == null)
		{
			layout = PatternLayout.createDefaultLayout();
		}
		return new PrismeLogAppender(name, filter, layout, (ignoreExceptions != null && ignoreExceptions) ? ignoreExceptions : false);
	}
}
