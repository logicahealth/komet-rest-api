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

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.sagebits.tmp.isaac.rest.ApplicationConfig;
import net.sagebits.tmp.isaac.rest.api.exceptions.RestException;
import net.sagebits.tmp.isaac.rest.tokens.CoordinatesToken;
import net.sagebits.tmp.isaac.rest.tokens.CoordinatesTokens;
import net.sagebits.tmp.isaac.rest.tokens.EditToken;
import sh.isaac.MetaData;
import sh.isaac.api.Get;
import sh.isaac.api.LookupService;
import sh.isaac.api.Status;
import sh.isaac.api.bootstrap.TermAux;
import sh.isaac.api.collections.NidSet;
import sh.isaac.api.coordinate.EditCoordinate;
import sh.isaac.api.coordinate.LanguageCoordinate;
import sh.isaac.api.coordinate.LogicCoordinate;
import sh.isaac.api.coordinate.ManifoldCoordinate;
import sh.isaac.api.coordinate.PremiseType;
import sh.isaac.api.coordinate.StampCoordinate;
import sh.isaac.api.coordinate.StampPrecedence;
import sh.isaac.misc.security.SystemRole;
import sh.isaac.misc.security.User;
import sh.isaac.model.configuration.EditCoordinates;
import sh.isaac.model.coordinate.EditCoordinateImpl;
import sh.isaac.utility.Frills;

/**
 * {@link RequestInfo}
 * This class is intended to hold a cache of global request info that we tie to the request / session being processed.
 * Things like the STAMP that applies, the expandable parameters, etc.
 * We will (likely) set this up on the thread local with a request filter that looks at every request before it arrives
 * at the implementing method.
 * 
 * @author <a href="mailto:daniel.armbrust.list@sagebits.net">Dan Armbrust</a>
 */
public class RequestInfo
{
	private static Logger log = LogManager.getLogger(RequestInfo.class);

	private static EditCoordinate DEFAULT_EDIT_COORDINATE = null;
	
	private static AtomicLong requestIdInc = new AtomicLong();

	private Map<String, List<String>> parameters_ = new HashMap<>();

	private String coordinatesToken_ = null;

	private Optional<User> user_ = null;
	private EditToken editToken_ = null;
	private long createTime_;
	private long requestId_;

	private EditCoordinate editCoordinate_ = null;

	// just a cache
//	private static WorkflowProvider wfp_;

	private Set<String> expandablesForDirectExpansion_ = new HashSet<>(0);
	// Default to this, users may override by specifying expandables=true
	private boolean returnExpandableLinks_ = ApplicationConfig.getInstance().isDebugDeploy();

	public static EditCoordinate getDefaultEditCoordinate()
	{
		if (DEFAULT_EDIT_COORDINATE == null)
		{
			DEFAULT_EDIT_COORDINATE = new EditCoordinateImpl(MetaData.USER____SOLOR.getNid(), 
					Frills.createAndGetDefaultEditModule(MetaData.SNOMED_CT_CORE_MODULES____SOLOR.getNid()),
					TermAux.DEVELOPMENT_PATH.getNid());
			CoordinatesUtil.clearCache();
		}
		return DEFAULT_EDIT_COORDINATE;
	}

	private static final ThreadLocal<RequestInfo> requestInfo = new ThreadLocal<RequestInfo>()
	{
		@Override
		protected RequestInfo initialValue()
		{
			return new RequestInfo();
		}
	};

	public static RequestInfo get()
	{
		return requestInfo.get();
	}

	private RequestInfo()
	{
		createTime_ = System.currentTimeMillis();
		requestId_ = requestIdInc.getAndIncrement();
	}

	public static RequestInfo remove()
	{
		RequestInfo ri = requestInfo.get();
		requestInfo.remove();
		return ri;
	}

	public RequestInfo readExpandables(Map<String, List<String>> parameters) throws RestException
	{
		requestInfo.get().expandablesForDirectExpansion_ = new HashSet<>(10);
		if (parameters.containsKey(RequestParameters.expand))
		{
			for (String expandable : RequestInfoUtils.expandCommaDelimitedElements(parameters.get(RequestParameters.expand)))
			{
				if (expandable != null)
				{
					requestInfo.get().expandablesForDirectExpansion_.add(expandable.trim());
				}
			}
		}
		if (parameters.containsKey(RequestParameters.returnExpandableLinks))
		{
			List<String> temp = parameters.get(RequestParameters.returnExpandableLinks);
			if (temp.size() > 0)
			{
				returnExpandableLinks_ = Boolean.parseBoolean(temp.get(0).trim());
			}
		}
		return get();
	}

	private static <E extends Enum<E>> byte[] byteArrayFromEnumSet(EnumSet<E> set)
	{
		byte[] returnValue = new byte[set.size()];
		int index = 0;
		for (Iterator<E> it = set.iterator(); it.hasNext();)
		{
			returnValue[index++] = (byte) it.next().ordinal();
		}

		return returnValue;
	}

	public RequestInfo readAll(Map<String, List<String>> parameters) throws Exception
	{
		parameters_.clear();
		for (Map.Entry<String, List<String>> entry : parameters.entrySet())
		{
			parameters_.put(entry.getKey(), Collections.unmodifiableList(entry.getValue()));
		}

		// Log value of ssoToken parameter, if any
		if (parameters_.get(RequestParameters.ssoToken) != null && parameters_.get(RequestParameters.ssoToken).size() == 1)
		{
			log.info(RequestParameters.ssoToken + "==\"" + parameters_.get(RequestParameters.ssoToken).iterator().next() + "\"");
		}

		readExpandables(parameters);

		String serializedCoordinatesTokenByParams = CoordinatesTokens.get(CoordinatesUtil.getCoordinateParameters(parameters));
		if (serializedCoordinatesTokenByParams != null)
		{
			log.debug("Using CoordinatesToken value cached by parameter");
			requestInfo.get().coordinatesToken_ = serializedCoordinatesTokenByParams;
		}
		else
		{
			log.debug("Constructing CoordinatesToken from parameters");

			// Set RequestInfo coordinatesToken string to parameter value if set, otherwise set to default
			Optional<CoordinatesToken> token = CoordinatesUtil.getCoordinatesTokenParameterTokenObjectValue(parameters);
			if (token.isPresent())
			{
				log.debug("Applying CoordinatesToken {} parameter \"{}\"", RequestParameters.coordToken, token.get().getSerialized());
				requestInfo.get().coordinatesToken_ = token.get().getSerialized();
			}
			else
			{
				log.debug("Applying default coordinates");

				requestInfo.get().coordinatesToken_ = CoordinatesTokens.getDefaultCoordinatesToken().getSerialized();
				token = Optional.of(CoordinatesTokens.getDefaultCoordinatesToken());
			}

			// Determine if any relevant coordinate parameters set
			Map<String, List<String>> coordinateParameters = new HashMap<>();
			coordinateParameters.putAll(RequestInfoUtils.getParametersSubset(parameters, RequestParameters.COORDINATE_PARAM_NAMES));

			// If no coordinate parameter or only coordToken value set, then use
			if (coordinateParameters.size() == 0 || (coordinateParameters.size() == 1 && coordinateParameters.containsKey(RequestParameters.coordToken)))
			{
				log.debug("No individual coordinate parameters to apply to token \"{}\"", requestInfo.get().coordinatesToken_);

			}
			else
			{ // If ANY coordinate parameter other than coordToken value set, then calculate new CoordinatesToken string
				log.debug("Applying {} individual parameters to coordinates token \"{}\": {}", requestInfo.get().coordinatesToken_, coordinateParameters.size(),
						coordinateParameters.toString());

				// TaxonomyCoordinate components
				boolean stated = CoordinatesUtil.getStatedFromParameter(coordinateParameters.get(RequestParameters.stated), token);

				// LanguageCoordinate components
				int langCoordLangSeq = CoordinatesUtil.getLanguageCoordinateLanguageNidFromParameter(coordinateParameters.get(RequestParameters.language),
						token);
				int[] langCoordDialectPrefs = CoordinatesUtil
						.getLanguageCoordinateDialectAssemblagePreferenceNidFromParameter(coordinateParameters.get(RequestParameters.dialectPrefs), token);
				int[] langCoordDescTypePrefs = CoordinatesUtil.getLanguageCoordinateDescriptionTypePreferenceNidsFromParameter(
						coordinateParameters.get(RequestParameters.descriptionTypePrefs), token);

				// StampCoordinate components
				long stampTime = CoordinatesUtil.getStampCoordinateTimeFromParameter(coordinateParameters.get(RequestParameters.time), token);
				int stampPathSeq = CoordinatesUtil.getStampCoordinatePathNidsFromParameter(coordinateParameters.get(RequestParameters.path), token);
				StampPrecedence stampPrecedence = CoordinatesUtil
						.getStampCoordinatePrecedenceFromParameter(coordinateParameters.get(RequestParameters.precedence), token);
				NidSet stampModules = CoordinatesUtil.getStampCoordinateModuleNidsFromParameter(coordinateParameters.get(RequestParameters.modules), token);
				EnumSet<Status> stampAllowedStates = CoordinatesUtil
						.getStampCoordinateAllowedStatesFromParameter(coordinateParameters.get(RequestParameters.allowedStates), token);

				// LogicCoordinate components
				int logicStatedSeq = CoordinatesUtil
						.getLogicCoordinateStatedAssemblageFromParameter(coordinateParameters.get(RequestParameters.logicStatedAssemblage), token);
				int logicInferredSeq = CoordinatesUtil
						.getLogicCoordinateInferredAssemblageFromParameter(coordinateParameters.get(RequestParameters.logicInferredAssemblage), token);
				int logicDescProfileSeq = CoordinatesUtil
						.getLogicCoordinateDescProfileAssemblageFromParameter(coordinateParameters.get(RequestParameters.descriptionLogicProfile), token);
				int logicClassifierSeq = CoordinatesUtil
						.getLogicCoordinateClassifierAssemblageFromParameter(coordinateParameters.get(RequestParameters.classifier), token);

				CoordinatesToken tokenObj = CoordinatesTokens.getOrCreate(stampTime, stampPathSeq, (byte) stampPrecedence.ordinal(), stampModules.asArray(),
						byteArrayFromEnumSet(stampAllowedStates), langCoordLangSeq, langCoordDialectPrefs, langCoordDescTypePrefs,
						(byte) (stated ? PremiseType.STATED : PremiseType.INFERRED).ordinal(), logicStatedSeq, logicInferredSeq, logicDescProfileSeq,
						logicClassifierSeq);

				requestInfo.get().coordinatesToken_ = tokenObj.getSerialized();

				CoordinatesTokens.put(CoordinatesUtil.getCoordinateParameters(parameters), tokenObj);

				log.debug("Created CoordinatesToken \"{}\"", requestInfo.get().coordinatesToken_);
			}
		}

		return requestInfo.get();
	}

	public boolean shouldExpand(String expandable)
	{
		return expandablesForDirectExpansion_.contains(expandable);
	}

	public boolean returnExpandableLinks()
	{
		return returnExpandableLinks_;
	}

	/**
	 * @return parameters Map
	 */
	public Map<String, List<String>> getParameters()
	{
		return Collections.unmodifiableMap(parameters_);
	}

	/**
	 * @return the stamp coordinate as requested by the user.
	 */
	// TODO nearly every usage of this call, needs to be redone to utilize the Util.getPreWorkflowStampCoordinate call.
	// Anything else, leads to an incomplete return based on the requested stamps.
	// Until we actually put workflow in, however, it won't matter..
	public StampCoordinate getStampCoordinate()
	{
		return getCoordinatesToken().getManifoldCoordinate().getStampCoordinate();
	}

	public boolean hasUser()
	{
		return user_ != null && user_.isPresent();
	}

	public void setDefaultReadOnlyUser()
	{
		user_ = Optional.of(new User("READ_ONLY_USER", MetaData.USER____SOLOR.getPrimordialUuid(), null, SystemRole.READ_ONLY));
		LookupService.get().getService(UserProvider.class).addUser(user_.get());
	}

	public Optional<sh.isaac.misc.security.User> getUser() throws RestException
	{
		if (user_ == null)
		{
			user_ = Optional.of(getEditToken().getUser());
		}

		return user_;
	}

	/**
	 *
	 * @return EditToken
	 * @throws RestException
	 */
	public EditToken getEditToken() throws RestException
	{
		if (editToken_ == null)
		{
			try
			{
				// FAIL if ssoToken and editToken parameters both set
				// FAIL if userId and editToken parameters both set
				// FAIL if userId and ssoToken parameters both set
				RequestInfoUtils.validateIncompatibleParameters(parameters_, RequestParameters.editToken, RequestParameters.ssoToken, RequestParameters.userId);

				Integer module = null;
				Integer path = null;
				UUID workflowProcessid = null;

				EditCoordinate defaultEditCoordinate = getDefaultEditCoordinate();

				// Set default EditToken parameters to values in passedEditToken if set, otherwise set to default
				Optional<String> passedEditTokenSerialized = RequestInfoUtils.getEditTokenParameterStringValue(parameters_);
				PrismeUserService userService = LookupService.getService(PrismeUserService.class);

				if (passedEditTokenSerialized.isPresent())
				{
					// Found valid EditToken passed as parameter
					log.debug("Applying EditToken {} parameter \"{}\"", RequestParameters.editToken, passedEditTokenSerialized.get());
					EditToken passedEditToken = EditToken.read(passedEditTokenSerialized.get());

					// Set local values to values from passed EditToken
					module = passedEditToken.getModuleNid();
					path = passedEditToken.getPathNid();
					workflowProcessid = passedEditToken.getActiveWorkflowProcessId();

					// Override values from passed EditToken with values from parameters
					if (parameters_.containsKey(RequestParameters.processId))
					{
						RequestInfoUtils.validateSingleParameterValue(parameters_, RequestParameters.processId);
						workflowProcessid = RequestInfoUtils.parseUuidParameter(RequestParameters.processId,
								parameters_.get(RequestParameters.processId).iterator().next());
					}
					if (parameters_.containsKey(RequestParameters.editModule))
					{
						module = RequestInfoUtils.getConceptNidFromParameter(parameters_, RequestParameters.editModule);
					}
					if (parameters_.containsKey(RequestParameters.editPath))
					{
						path = RequestInfoUtils.getConceptNidFromParameter(parameters_, RequestParameters.editPath);
					}

					passedEditToken.updateValues(module, path, workflowProcessid);

					User userFromPassedEditToken = passedEditToken.getUser();

					if (!userFromPassedEditToken.rolesStillValid())
					{
						if (userService.usePrismeForRolesByToken())
						{
							log.info("Rechecking roles for user '{}' because its '{}' and roles were last checked at '{}' ", userFromPassedEditToken.getName(),
									System.currentTimeMillis(), userFromPassedEditToken.rolesCheckedAt());
							try
							{
								// we don't need to call updateRoles here, because prismeServiceUtils updates the user cache, when it does a prisme
								// read.
								userService.getUser(userFromPassedEditToken.getSSOToken().get());
								log.debug("Roles updated: " + passedEditToken.getUser().toString());
							}
							catch (Exception e)
							{
								log.error("Failed to refresh roles", e);
								throw new RestException("Failed to revalidate roles");
							}
						}
						else
						{
							// if we aren't using prisme, roles can't expire...
							passedEditToken.getUser().updateRoles(passedEditToken.getUser().getRoles().toArray(new SystemRole[0]));
						}
					}
					editToken_ = passedEditToken;
					user_ = Optional.of(passedEditToken.getUser());
				}
				else
				{
					// No valid EditToken passed as parameter
					Optional<User> userOptional = Optional.empty();

					// IF prisme.properties properties found then MUST use SSO token
					if (userService.usePrismeForRolesByToken())
					{
						// FAIL if userId parameter set
						if (parameters_.containsKey(RequestParameters.userId))
						{
							throw new SecurityException(new RestException(RequestParameters.userId, parameters_.get(RequestParameters.userId) + "",
									"Cannot specify userId parameter when PRISME configured"));
						}
						log.debug("Constructing new EditToken from User from PRISME with SSO token {}", parameters_.get(RequestParameters.ssoToken));
						// Validate ssoToken parameter
						RequestInfoUtils.validateSingleParameterValue(parameters_, RequestParameters.ssoToken);
						userOptional = userService.getUser(parameters_.get(RequestParameters.ssoToken).iterator().next());
					}
					else
					{
						// IF prisme.properties properties NOT found or does not specify the prisme roles URL

						// Check for passed userId parameter, which can either be a concept id (UUID or nid),
						// the string "DEFAULT", or a valid existing username
						if (parameters_.containsKey(RequestParameters.userId))
						{
							log.info("Constructing new EditToken from test User with ALL ROLES with passed userId {}",
									parameters_.get(RequestParameters.userId));
							// Validate userId parameter
							RequestInfoUtils.validateSingleParameterValue(parameters_, RequestParameters.userId);
							String userIdParameterValue = parameters_.get(RequestParameters.userId).iterator().next();
							Integer userConceptNid = null;
							if (userIdParameterValue.equalsIgnoreCase("DEFAULT"))
							{
								userConceptNid = EditCoordinates.getDefaultUserMetadata().getAuthorNid();
							}
							if (userConceptNid == null)
							{
								try
								{
									// Attempt to parse as concept id
									userConceptNid = RequestInfoUtils.getConceptNidFromParameter(RequestParameters.userId, userIdParameterValue);
								}
								catch (Exception e)
								{}
							}
							if (userConceptNid == null)
							{
								try
								{
									// Attempt to retrieve UUID generated by hashing the passed string
									// as an FQN, which must be in the MetaData.USER UUID domain
									userConceptNid = RequestInfoUtils.getConceptNidFromParameter(RequestParameters.userId,
											UserProvider.getUuidFromUserName(userIdParameterValue).toString());
								}
								catch (Exception e)
								{}
							}

							if (userConceptNid == null)
							{
								throw new RestException(RequestParameters.userId, userIdParameterValue,
										"Unable to determine test User concept nid from parameter.  Must be a concept id, an existing User FQN, or the (case insensitive)"
												+ " word \"DEFAULT\"");
							}

							String userName = Get.conceptDescriptionText(userConceptNid);
							userOptional = Optional
									.of(new User(userName, Get.identifierService().getUuidPrimordialForNid(userConceptNid), "", SystemRole.values()));
							LookupService.get().getService(UserProvider.class).addUser(userOptional.get());
						}
						else
						{
							if (parameters_.containsKey(RequestParameters.ssoToken))
							{
								RequestInfoUtils.validateSingleParameterValue(parameters_, RequestParameters.ssoToken);
								userOptional = userService.getUser(parameters_.get(RequestParameters.ssoToken).iterator().next());
							}
						}
					}

					if (parameters_.containsKey(RequestParameters.processId))
					{
						RequestInfoUtils.validateSingleParameterValue(parameters_, RequestParameters.processId);
						workflowProcessid = RequestInfoUtils.parseUuidParameter(RequestParameters.processId,
								parameters_.get(RequestParameters.processId).iterator().next());
					}
					if (parameters_.containsKey(RequestParameters.editModule))
					{
						module = RequestInfoUtils.getConceptNidFromParameter(parameters_, RequestParameters.editModule);
					}
					if (parameters_.containsKey(RequestParameters.editPath))
					{
						path = RequestInfoUtils.getConceptNidFromParameter(parameters_, RequestParameters.editPath);
					}

					// Must have EditToken, SSO token (real or parsable, if in src/test) or userId in order to get author
					user_ = userOptional;
					if (user_ == null || !user_.isPresent())
					{
						throw new RestException("Edit token cannot be constructed without user information!");
					}
					editToken_ = new EditToken(UserProvider.getAuthorNid(user_.get().getName()),
							module != null ? module : defaultEditCoordinate.getModuleNid(), path != null ? path : defaultEditCoordinate.getPathNid(),
							workflowProcessid);
				}

				log.debug("Populated EditToken '{}' into RequestInfo", editToken_);
			}
			catch (RestException e)
			{
				throw e;
			}
			catch (RuntimeException e)
			{
				throw e;
			}
			catch (Exception e)
			{
				throw new RuntimeException(e);
			}
		}

		return editToken_;
	}

	/**
	 * Lazily create, cache and return an EditCoordinate
	 *
	 * @return the edit coordinate
	 * @throws RestException
	 */
	public EditCoordinate getEditCoordinate() throws RestException
	{
		if (editCoordinate_ == null)
		{
			editCoordinate_ = new EditCoordinateImpl(getEditToken().getAuthorNid(), getEditToken().getModuleNid(), getEditToken().getPathNid());
		}

		return editCoordinate_;
	}

	public UUID getActiveWorkflowProcessId() throws RestException
	{
		return getEditToken().getActiveWorkflowProcessId();
	}

	public LanguageCoordinate getLanguageCoordinate()
	{
		return getCoordinatesToken().getManifoldCoordinate().getLanguageCoordinate();
	}

	public LogicCoordinate getLogicCoordinate()
	{
		return getCoordinatesToken().getManifoldCoordinate().getLogicCoordinate();
	}

	public ManifoldCoordinate getManifoldCoordinate()
	{
		return getCoordinatesToken().getManifoldCoordinate();
	}

	public ManifoldCoordinate getManifoldCoordinate(boolean stated)
	{
		if (stated)
		{
			return getManifoldCoordinate().getTaxonomyPremiseType() == PremiseType.STATED ? getManifoldCoordinate()
					: getManifoldCoordinate().makeCoordinateAnalog(PremiseType.STATED);
		}
		else // (! stated)
		{
			return getManifoldCoordinate().getTaxonomyPremiseType() == PremiseType.INFERRED ? getManifoldCoordinate()
					: getManifoldCoordinate().makeCoordinateAnalog(PremiseType.INFERRED);
		}
	}

	public boolean getStated()
	{
		return getManifoldCoordinate().getTaxonomyPremiseType() == PremiseType.STATED;
	}

	/**
	 * @return CoordinatesToken created from existing coordinates
	 */
	public CoordinatesToken getCoordinatesToken()
	{
		if (coordinatesToken_ != null)
		{
			try
			{
				return CoordinatesTokens.getOrCreate(coordinatesToken_);
			}
			catch (Exception e)
			{
				// Should never fail because validated on readAll()
				log.error("Unexpected", e);
				throw new RuntimeException(e);
			}
		}
		else
		{
			coordinatesToken_ = CoordinatesTokens.getDefaultCoordinatesToken().getSerialized();
			return CoordinatesTokens.getDefaultCoordinatesToken();
		}
	}

//	public WorkflowProvider getWorkflow()
//	{
//		if (wfp_ == null)
//		{
//			wfp_ = LookupService.getService(WorkflowProvider.class);
//			if (wfp_ == null)
//			{
//				throw new RuntimeException("Workflow service not available!");
//			}
//		}
//		return wfp_;
//	}

	public long getCreateTime()
	{
		return createTime_;
	}
	
	public long getUniqueId()
	{
		return requestId_;
	}
}
