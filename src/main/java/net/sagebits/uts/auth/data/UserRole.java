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

package net.sagebits.uts.auth.data;

import java.util.Optional;
import java.util.OptionalInt;
import sh.isaac.api.util.NumericUtils;

/**
 * {@link UserRole}
 * The roles that will be utilized across the rest API, and the editor.
 * 
 * These roles are ordered from the least powerful to the most powerful (in general).
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
public enum UserRole
{
	// DO NOT change these enum names without making the corresponding changes in the uts-web-shared / uts-web-editor
	
	READ(SystemRoleConstants.READ, "Provides read access to terminology content.  Users with any role in the system will always have the read role, as well."),
	EDITOR(SystemRoleConstants.EDITOR, "Editors can modify terminology, make comments, and submit edited content to the review queue.  Editors are also content " + 
			"reviewers who can make comments on edits, reject them sending them back to the editor, or approve them."),
	CONTENT_MANAGER(SystemRoleConstants.CONTENT_MANAGER, "The content manager has all of the permissions of editor.  In addition, the content " +
			"manager can approve changes, run release validations, and perform full content releases."),
	SYSTEM_MANAGER(SystemRoleConstants.SYSTEM_MANAGER, "The system manager has administrative control of the system allowing them to manage users and roles."),
	ADMINISTRATOR(SystemRoleConstants.ADMINISTRATOR, "The system administrator has full access to the system, including logs, diagnostic data, and system configuration."), 
	AUTOMATED(SystemRoleConstants.AUTOMATED, "This role cannot be assigned to users.  It exists for system to system access.");

	/**
	 * {@link SystemRoleConstants}
	 * We need string constants for these, to use in annotations in the REST APIs.
	 *  
	 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
	 */
	public class SystemRoleConstants
	{
		public final static String READ = "Read";
		public final static String EDITOR = "Editor";
		public final static String CONTENT_MANAGER = "Content Manager";
		public final static String SYSTEM_MANAGER = "System Manager";
		public final static String ADMINISTRATOR = "Administrator";
		public final static String AUTOMATED = "Automated";
	}

	private String niceName;
	private String roleDescription;

	private UserRole(String niceName, String roleDescription)
	{
		this.niceName = niceName;
		this.roleDescription = roleDescription;
	}


	/**
	 * @see java.lang.Enum#toString()
	 */
	public String toString()
	{
		return niceName;
	}
	
	/**
	 * @return the user-friendly name of the role
	 */
	public String getNiceName()
	{
		return this.niceName;
	}

	
	/**
	 * @return the description of the role
	 */
	public String getRoleDescription()
	{
		return this.roleDescription;
	}

	/**
	 * Determine the role, if possible, from the input.
	 * @param value - enunName, niceName, or ordinal
	 * @return The matching role, if any.
	 */
	public static Optional<UserRole> parse(String value)
	{
		String valueTrimmed = value.trim();
		OptionalInt oi = NumericUtils.getInt(valueTrimmed);
		
		for (UserRole role : UserRole.values())
		{
			if (role.name().equalsIgnoreCase(valueTrimmed) || role.niceName.equalsIgnoreCase(valueTrimmed) 
					|| (oi.isPresent() && role.ordinal() == oi.getAsInt()))
			{
				return Optional.of(role);
			}
		}
		return Optional.empty();
	}
}
