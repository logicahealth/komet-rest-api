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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.core.SecurityContext;
import org.apache.mahout.math.Arrays;

/**
 * 
 * {@link SecurityUtils}
 *
 * @author <a href="mailto:joel.kniaz.list@gmail.com">Joel Kniaz</a>
 *
 */
public class SecurityUtils
{
	private SecurityUtils()
	{
	}

	public static void validateRole(SecurityContext securityContext, Class<?> apiClass)
	{
		// Confirm that no method-level role-related security annotations are declared
		// Only class-level role-related security annotations supported
		validateRoleBasedSecurityAnnotations(apiClass);

		for (Method method : apiClass.getDeclaredMethods())
		{
			validateRole(securityContext, method);
		}
	}

	public static void validateRole(SecurityContext securityContext, Method method)
	{
		// Confirm that no method-level role-related security annotations are declared
		// Only class-level role-related security annotations supported
		validateRoleBasedSecurityAnnotations(method.getDeclaringClass());
		validateRoleBasedSecurityAnnotations(method);

		boolean userAuthorized = false;
		DenyAll denyAll = method.getDeclaringClass().getAnnotation(DenyAll.class);
		// If @DenyAll exists then fail
		if (denyAll != null)
		{
			throw new SecurityException("User not authorized: " + securityContext.getUserPrincipal() + " to access methods of class "
					+ method.getDeclaringClass().getName() + ". @DenyAll is set.");
		}
		// If @RolesAllowed exists then check each of @RolesAllowed against securityContext.isUserInRole(),
		// failing if no match found
		RolesAllowed ra = method.getDeclaringClass().getAnnotation(RolesAllowed.class);
		if (ra != null)
		{
			for (String role : ra.value())
			{
				if (securityContext.isUserInRole(role))
				{
					userAuthorized = true;
					break;
				}
			}
		}
		else
		{
			// If @RolesAllowed is not set and @PermitAll is, then succeed, else fail
			PermitAll permitAll = method.getDeclaringClass().getAnnotation(PermitAll.class);
			if (permitAll != null)
			{
				userAuthorized = true;
			}
		}
		if (!userAuthorized)
		{
			throw new SecurityException("User not authorized: " + securityContext.getUserPrincipal().getName() + " to access methods of class "
					+ method.getDeclaringClass().getName() + ". Must have one of following role(s): " + (ra != null ? Arrays.toString(ra.value()) : null));
		}
	}

	/*
	 * @PermitAll, @DenyAll, and @RolesAllowed annotations must not be applied on the same method or class.
	 * 
	 * In the following cases, the method level annotations take precedence over the class level annotation:
	 * 
	 * @PermitAll is specified at the class level and @RolesAllowed or @DenyAll are specified on methods of the same class;
	 * 
	 * @DenyAll is specified at the class level and @PermitAll or @RolesAllowed are specified on methods of the same class;
	 * 
	 * @RolesAllowed is specified at the class level and @PermitAll or @DenyAll are specified on methods of the same class.
	 */
	private static void validateRoleBasedSecurityAnnotations(Class<?> apiClass)
	{
		if (apiClass == null)
		{
			return;
		}

		Annotation existingRoleBasedSecurityAnnotation = null;
		for (Annotation annotation : apiClass.getAnnotations())
		{
			if (annotation.annotationType() == RolesAllowed.class || annotation.annotationType() == PermitAll.class
					|| annotation.annotationType() == DenyAll.class)
			{
				if (existingRoleBasedSecurityAnnotation == null)
				{
					existingRoleBasedSecurityAnnotation = annotation;
				}
				else
				{
					throw new RuntimeException("Cannot apply both annotation-based role-related security constraints to class " + apiClass.getSimpleName()
							+ ": " + annotation.annotationType().getSimpleName() + " and "
							+ existingRoleBasedSecurityAnnotation.annotationType().getSimpleName());
				}
			}
		}
	}

	private static void validateRoleBasedSecurityAnnotations(Method passedMethod)
	{
		Annotation existingRoleBasedSecurityAnnotation = null;
		for (Annotation annotation : passedMethod.getAnnotations())
		{
			if (annotation.annotationType() == RolesAllowed.class || annotation.annotationType() == PermitAll.class
					|| annotation.annotationType() == DenyAll.class)
			{
				if (existingRoleBasedSecurityAnnotation == null)
				{
					existingRoleBasedSecurityAnnotation = annotation;
				}
				else
				{
					throw new RuntimeException("Cannot apply both annotation-based role-related security constraints to method " + passedMethod.getName()
							+ " of " + passedMethod.getDeclaringClass().getSimpleName() + ": " + annotation.annotationType().getSimpleName() + " and "
							+ existingRoleBasedSecurityAnnotation.annotationType().getSimpleName());
				}
			}
		}
	}
}
