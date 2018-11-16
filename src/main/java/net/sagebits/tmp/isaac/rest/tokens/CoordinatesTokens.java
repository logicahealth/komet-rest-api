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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.sagebits.tmp.isaac.rest.api.exceptions.RestException;
import net.sagebits.tmp.isaac.rest.session.CoordinatesUtil;
import sh.isaac.api.coordinate.LanguageCoordinate;
import sh.isaac.api.coordinate.LogicCoordinate;
import sh.isaac.api.coordinate.ManifoldCoordinate;
import sh.isaac.api.coordinate.PremiseType;
import sh.isaac.api.coordinate.StampCoordinate;
import sh.isaac.model.configuration.LanguageCoordinates;
import sh.isaac.model.configuration.ManifoldCoordinates;
import sh.isaac.model.configuration.StampCoordinates;

/**
 * 
 * {@link CoordinatesTokens}
 *
 * @author <a href="mailto:joel.kniaz.list@gmail.com">Joel Kniaz</a>
 *
 */
public class CoordinatesTokens
{
	private final static Object OBJECT_BY_TOKEN_CACHE_LOCK = new Object();
	private final static Object TOKEN_BY_PARAMS_CACHE_LOCK = new Object();

	private static final int DEFAULT_MAX_SIZE = 1024;
	private static CoordinatesToken defaultCoordinatesToken = null;
	private static Map<String, CoordinatesToken> OBJECT_BY_TOKEN_CACHE = null;
	private static Map<String, String> TOKEN_BY_PARAMS_CACHE = null;

	private static void init(final int maxEntries)
	{
		synchronized (TOKEN_BY_PARAMS_CACHE_LOCK)
		{
			synchronized (OBJECT_BY_TOKEN_CACHE_LOCK)
			{
				if (OBJECT_BY_TOKEN_CACHE == null)
				{
					OBJECT_BY_TOKEN_CACHE = new LinkedHashMap<String, CoordinatesToken>(maxEntries, 0.75F, true)
					{
						private static final long serialVersionUID = -1236481390177598762L;

						@Override
						protected boolean removeEldestEntry(Map.Entry<String, CoordinatesToken> eldest)
						{
							return size() > maxEntries;
						}
					};

					defaultCoordinatesToken = CoordinatesTokens.getOrCreate(getDefaultManifoldCoordinate().getStampCoordinate(),
							getDefaultManifoldCoordinate().getLanguageCoordinate(), getDefaultManifoldCoordinate().getLogicCoordinate(),
							getDefaultManifoldCoordinate().getTaxonomyPremiseType());
				}
			}
			if (TOKEN_BY_PARAMS_CACHE == null)
			{
				TOKEN_BY_PARAMS_CACHE = new LinkedHashMap<String, String>(maxEntries, 0.75F, true)
				{
					private static final long serialVersionUID = -2638577900934193146L;

					@Override
					protected boolean removeEldestEntry(Map.Entry<String, String> eldest)
					{
						return size() > maxEntries;
					}
				};
			}
		}
	}

	private static ManifoldCoordinate getDefaultManifoldCoordinate()
	{
		return ManifoldCoordinates.getStatedManifoldCoordinate(StampCoordinates.getDevelopmentLatest(),
				LanguageCoordinates.getUsEnglishLanguageFullySpecifiedNameCoordinate());
	}

	/**
	 * This may need to be cleared after loading new content at runtime, due to language types having dynamic expansion
	 */
	public static void clearCache()
	{
		synchronized (OBJECT_BY_TOKEN_CACHE_LOCK)
		{
			OBJECT_BY_TOKEN_CACHE.clear();
		}
		synchronized (TOKEN_BY_PARAMS_CACHE_LOCK)
		{
			TOKEN_BY_PARAMS_CACHE.clear();
		}
		defaultCoordinatesToken = CoordinatesTokens.getOrCreate(getDefaultManifoldCoordinate().getStampCoordinate(),
				getDefaultManifoldCoordinate().getLanguageCoordinate(), getDefaultManifoldCoordinate().getLogicCoordinate(),
				getDefaultManifoldCoordinate().getTaxonomyPremiseType());
	}
	
	/**
	 * @return CoordinatesToken object containing components for default coordinates
	 */
	public static CoordinatesToken getDefaultCoordinatesToken()
	{
		if (OBJECT_BY_TOKEN_CACHE == null)
		{
			init(DEFAULT_MAX_SIZE);
		}

		return defaultCoordinatesToken;
	}

	/**
	 * 
	 * This method caches a CoordinatesToken object,
	 * automatically serializing itself to generate its key
	 * 
	 * @param value CoordinatesToken object
	 * @throws Exception
	 */
	private static void put(CoordinatesToken value)
	{
		if (OBJECT_BY_TOKEN_CACHE == null)
		{
			init(DEFAULT_MAX_SIZE);
		}

		synchronized (OBJECT_BY_TOKEN_CACHE_LOCK)
		{
			OBJECT_BY_TOKEN_CACHE.put(value.getSerialized(), value);
		}
	}

	/**
	 * 
	 * This method caches a CoordinatesToken object by the provided key
	 * and also caching the key by a hash of the parameters presumably
	 * used to generate the object
	 * 
	 * @param params parameter name to value-list map provided in UriInfo by ContainerRequestContext
	 * @param value CoordinatesToken object
	 */
	public static void put(Map<String, List<String>> params, CoordinatesToken value)
	{
		if (OBJECT_BY_TOKEN_CACHE == null)
		{
			init(DEFAULT_MAX_SIZE);
		}
		synchronized (TOKEN_BY_PARAMS_CACHE_LOCK)
		{
			TOKEN_BY_PARAMS_CACHE.put(CoordinatesUtil.encodeCoordinateParameters(params), value.getSerialized());
		}
	}

	/**
	 * 
	 * This method attempts to retrieve the CoordinatesToken object
	 * corresponding to the passed serialized CoordinatesToken string key.
	 * 
	 * @param key serialized CoordinatesToken string
	 * @return CoordinatesToken object
	 * @throws Exception
	 */
	private static CoordinatesToken get(String key)
	{
		if (OBJECT_BY_TOKEN_CACHE == null)
		{
			init(DEFAULT_MAX_SIZE);
		}
		synchronized (OBJECT_BY_TOKEN_CACHE_LOCK)
		{
			return OBJECT_BY_TOKEN_CACHE.get(key);
		}
	}

	public static CoordinatesToken getOrCreate(String key) throws RestException
	{
		CoordinatesToken token = get(key);

		if (token == null)
		{
			token = new CoordinatesToken(key);
			put(token);
		}

		return get(key);
	}

	public static CoordinatesToken getOrCreate(StampCoordinate stamp, LanguageCoordinate lang, LogicCoordinate logic, PremiseType taxType)
	{
		CoordinatesToken constructedToken = new CoordinatesToken(stamp, lang, logic, taxType);

		CoordinatesToken cachedToken = get(constructedToken.getSerialized());

		if (cachedToken == null)
		{
			cachedToken = constructedToken;
			put(cachedToken);
		}

		return get(cachedToken.getSerialized());
	}

	public static CoordinatesToken getOrCreate(long stampTime, int stampPath, byte stampPrecedence, int[] stampModules, byte[] stampStates, int langCoord,
			int[] langDialects, int[] langDescTypePrefs, byte taxonomyType, int logicStatedAssemblage, int logicInferredAssemblage, int logicDescLogicProfile,
			int logicClassifier)
	{

		CoordinatesToken constructedToken = new CoordinatesToken(stampTime, stampPath, stampPrecedence, stampModules, stampStates, langCoord, langDialects,
				langDescTypePrefs, taxonomyType, logicStatedAssemblage, logicInferredAssemblage, logicDescLogicProfile, logicClassifier);

		CoordinatesToken cachedToken = get(constructedToken.getSerialized());

		if (cachedToken == null)
		{
			cachedToken = constructedToken;
			put(cachedToken);
		}

		return get(cachedToken.getSerialized());
	}

	/**
	 * Attempt to retrieve CoordinatesToken serialization key string
	 * by a hash of the parameters presumably used to generate the object
	 * 
	 * @param params parameter name to value-list map provided in UriInfo by ContainerRequestContext
	 * @return the serialized token
	 */
	public static String get(Map<String, List<String>> params)
	{
		if (OBJECT_BY_TOKEN_CACHE == null)
		{
			return null;
		}
		else
		{
			synchronized (TOKEN_BY_PARAMS_CACHE_LOCK)
			{
				if (TOKEN_BY_PARAMS_CACHE != null)
				{
					return TOKEN_BY_PARAMS_CACHE.get(CoordinatesUtil.encodeCoordinateParameters(params));
				}
				return null;
			}
		}
	}
}
