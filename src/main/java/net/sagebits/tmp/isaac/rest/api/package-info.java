/**
 * 
 * <h1>ISAAC Web Server</h1>
 * <p>
 * A REST server with a simple data model for accessing <u>ISAAC</u> functionality in the SOLOR data model.
 * Primarily supports the <u>UTS Web Editor</u>.
 * </p>
 * 
 * <p></p>
 * <p></p>
 * <p></p>
 * 
 * <h2>Authentication</h2>
 * <p>
 * The server can be used in local authentication mode, or configured against a uts-auth-api instance, which would be more typical deployment 
 * when the uts-web-editor is also deployed.
 * 
 * To configure the auth mode, see the src/test/resources/uts-rest-api.properties file.
 * 
 * In local auth mode, all queries support the following parameters:
 * </p>
 * <p>
 * <code>ssoToken - optional - a previously issued token that represents an authenticated user.</code>
 * </p><p>
 * <code>userName - optional - a user name to authenticate against local auth - must be used with password</code>
 * </p><p>
 * <code>email - optional - an e-mail address (instead of a username) to authenticate against local auth - must be used with password</code>
 * </p><p>
 * <code>password - optional - the password to authenticate against local in combination with  username or email.</code>
 * </p>
 * 
 * <p>
 * When using the remote auth server, auth should be done against that server, which will return an ssoToken - which can then be passed here.
 * In remote auth mode, if userName, email, password are sent here, they are delegated to the configured remote-auth server.
 * </p>
 * 
 * <h2>Return Types</h2>
 * <p>
 * Typically, return types are specified by passing an Accept header.  The primary supported types are application/xml and application/json.
 * Alternatively, you can bypass setting a header, and specifying an extension on each request - .json for a json response, and .xml for an 
 * xml response.
 * </p>
 * 
 * <h2>Coordinate Parameters</h2>
 * <p>
 * Most ISAAC REST Server calls return results that are contingent upon the value of coordinates. Passing relevant parameters can change the results
 * of the query, depending on the data, the request and whether or not the parameter value differs from its respective default value.
 * </p>
 *
 * <h3><u>Coordinates Token (<code>RestCoordinatesToken</code>)</u></h3>
 * <p>
 * The RestCoordinatesToken token serializes all values comprising the Taxonomy, Stamp, Language and Logic coordinates
 * </p>
 * 
 * <p>
 * The pattern to use is to call getCoordinatesToken() with or without additional coordinate-specific parameters to get a token configured and
 * returned.
 * From then on, the returned serialized token string is passed to each subsequent API call as an argument to the <code>coordToken</code> parameter,
 * along with any additional modifying parameters.
 * </p>
 *
 * <h3><u>Taxonomy Coordinate (<code>RestManifoldCoordinate</code>) Parameters</u></h3>
 * <p>
 * The <code>RestManifoldCoordinate</code> comprises a Premise/Taxonomy type (represented as boolean <code>stated</code>), as well as the other
 * coordinates <code>RestStampCoordinate</code>, <code>RestLanguageCoordinate</code> and <code>RestLogicCoordinate</code>
 * </p><p>
 * <code>stated</code> - specifies premise/taxonomy type of <i>stated</i> when <code>stated</code> is <code>true</code> and <i>inferred</i> when
 * <code>false</code>.
 * </p>
 * 
 * <h3><u>Stamp Coordinate (<code>RestStampCoordinate</code>) Parameters</u></h3>
 * <p>
 * <code>modules</code> - specifies modules of the StampCoordinate. Value may be a comma delimited list of module concept UUID or int ids. If a
 * specified module concept such as "VHA Modules" also contains nested modules, such as "VHAT 2016.08.18", "VHAT 2016.09.25", then the calculated 
 * stamp coordinate will behave as if you had passed the parent module and each of the children modules.
 * </p><p>
 * <code>path</code> - specifies path component of StampPosition component of the StampCoordinate. Values is path UUID, int id or the term 
 * "development" or "master".  The default is "development".
 * </p><p>
 * <code>precedence</code> - specifies precedence of the StampCoordinate. Values are either "path" or "time". The default is "path".
 * </p>
 * <p>
 * <code>allowedStates</code> - specifies allowed states of the StampCoordinate. Value may be a comma delimited list of State enum names. The default
 * is "active".
 * </p><p>
 * <code>time</code> - specifies time component of StampPosition component of the StampCoordinate. Values are Long time values or "latest". The
 * default is "latest".
 * </p>
 *
 * <h3><u>Language Coordinate (<code>RestLanguageCoordinate</code>) Parameters</u></h3>
 * <p>
 * <code>descriptionTypePrefs</code> - specifies the order preference of description types for the LanguageCoordinate. Values are description type
 * UUIDs, int ids or the terms "fqn", "regular" and/or "definition". The default is "fqn,regular".  The system also supports deprecated names 
 * for the description types - 'fsn' is the same as 'fqn', and 'synonym' is the same as 'regular'.
 * </p>
 * <p>
 * <code>dialectPrefs</code> - specifies the order preference of dialects for the LanguageCoordinate. Values are description type UUIDs, int ids or
 * the terms "us" or "gb" (which are convenenience terms) or the special term 'recurse'. The default is "us,gb,recurse".
 *
 * In general, dialect concepts are children of the concept "Dialect assemblage (SOLOR)" - 
 * f5d5c959-a9b2-57aa-b88b-3676e7147265.  Note, however, this is a hierarchy of less specific to more specific, for example, 
 * English Dialect -> US English Dialect -> US Nursing Dialect.
 * 
 * When 'recurse' is specified, the specified dialects will be used first, followed by their children.  If you specify the dialects of
 * 'us,gb,recurse' you will get the resulting dialect list:
 * 
 * [US English Dialect, GB English dialect, US Nursing Dialect].  When it looks for designations to return, it evaluates this list in order.
 * 
 * </p><p>
 * <code>language</code> - specifies language of the LanguageCoordinate. Value may be a language UUID, int id or one of the following terms:
 * "english", "spanish", "french", "danish", "polish", "dutch", "lithuanian", "chinese", "japanese", or "swedish". The default is "english".
 * If a concept is specified, it must be a direct child of the concept 'Language (SOLOR)' - f56fa231-10f9-5e7f-a86d-a1d61b5b56e3.
 * </p>
 * 
 * <h3><u>Logic Coordinate (<code>RestLogicCoordinate</code>) Parameters</u></h3>
 * <p>
 * <code>logicStatedAssemblage</code> - specifies stated assemblage of the LogicCoordinate. Value may be a concept UUID string or int id.
 * </p><p>
 * <code>logicInferredAssemblage</code> - specifies inferred assemblage of the LogicCoordinate. Value may be a concept UUID string or int id.
 * </p><p>
 * <code>descriptionLogicProfile</code> - specifies description profile assemblage of the LogicCoordinate. Value may be a concept UUID string or int
 * id.
 * </p><p>
 * <code>classifier</code> - specifies classifier assemblage of the LogicCoordinate. Value may be a concept UUID string or int id.
 * </p>
 *
 * <h2>Expandables</h2>
 * <p>
 * The server has the ability to return data to help understand the API in the form of "expandables". By default, expandables are on, when the server
 * is deployed in a debug environment. Expandables are disabled when deployed in a production environment. To override the default, and return
 * expandable metadata in a production environment, add this parameter to any call: <code>expandables=true</code>.  This primarily enables a feature in 
 * in the API known as expandable links - for performance reasons, they are not calculated or included by default in production mode.
 * These links are the urls that the end user could call back to get additional data that wasn't fetched in the first call.
 * Enabling or disabling the expandable links has no impact on individual expand parameters that get passed to an individual call.
 * </p>
 *
 * <h2>Edits/Updates</h2>
 * <p>
 * Updates/edits of existing concepts and components require an <code>editToken</code>, to specify the author, module and path for the change,
 * and use optionally-passed (or defaulted) stamp coordinate <code>RestStampCoordinate</code> information (either default or passed {@code coordToken}
 * or individual parameters: only <code>modules</code>, <code>path</code>, <code>precedence</code>), to specify a specific existing version against which 
 * passed, presumably updated data, are to be compared. Parameters <code>time</code> and <code>allowedStates</code> are not allowed (or are ignored, as
 * components of passed <code>coordToken</code>), because all comparisons are made with <code>time=LATEST</code> and <code>allowedStates=ANY</code>. If 
 * no <code>coordToken</code> or stamp coordinate parameters are passed, then default values are used.
 * <br>
 * <br>
 * The update API first attempts to retrieve the most recent <code>time=LATEST</code> version of the specified chronology conforming to the module
 * specified in the <code>editToken</code> and the non-module parameters of the effective stamp coordinate (with <code>time=LATEST</code> and 
 * <code>allowedStates=ANY</code>).
 * <br>
 * If a matching version is found, then its data fields are compared to the corresponding data fields in the passed form data.
 * <br>
 * If no differences are found, then the update write is not performed.
 * <br>
 * If differences are found, then the update write is performed.
 * <br>
 * If no matching version is found (based on the <code>editToken</code> module), then the update API attempts to retrieve the most recent
 * (<code>time=LATEST</code>) version of ANY state (<code>allowedStates=ANY</code>) of the specified chronology conforming to the module and non-module 
 * parameters of the effective stamp coordinate.
 * <br>
 * If no differences are found, then the update write is not performed.
 * <br>
 * If differences are found, then the update write is performed.
 * </p>
 * 
 * <h2>Data Model</h2>
 * <p>
 * The only native data types required for storing terminology information within ISAAC are Concepts and Semantics. Semantics are highly flexible and configurable
 * constructs in the system.
 * <br>
 * <br>
 * Each semantic that is defined (at runtime, by the needs of the terminology) can be thought of as a database table of its own, in a traditional database system.
 * All of the typical elements from a terminology data model, such as descriptions, relationships, attributes, refsets, etc. - can be mapped into either ISAAC
 * concepts or ISAAC semantics, with no loss of fidelity. Furthermore, the semantics can be defined at the time that the data is imported, dynamically.
 * The data model of the system does not have to change to be able to store new types of data.
 * <br>
 * <br>
 * Another core notion of the system is a Chronology and a Version. Each unique Concept or semantic in the system has a Chronology - which carries that attributes
 * of the entry that never change. For example, the identifier is never allowed to change - if the identifier is entered wrong, the item must be retired, and 
 * replaced by a new item with the correct identifier.
 * <br>
 * <br>
 * The Version carries that attributes of the item that DO change from version to version. For example, in a semantic that carries a definition - if
 * there is a misspelling in the text value, a new version of the semantic would be created that has the corrected text. Both versions of the semantic link to the 
 * same semantic chronology, and have the same identifier. Now there are two different versions.
 * <br>
 * <br>
 * Chronology objects carry a list of all versions of the object. Chronology objects also all extend from OchreExternalizable, which means that they know how to serialize
 * and deserialize themselves to an array of bytes - their most compact representation for storage.
 * <br>
 * <br>
 * The primary import, export, and change set formats of the system revolve around reading and writing the byte representation of the chronologies and versions from and
 * to storage.
 * <br>
 * The ISAAC-Rest APIs are layered on top of this low-level java implementation - and provide access to both the lowest level of storage (concepts, semantics, chronologies,
 * versions) but also provide various convenience abstractions, such as the notion of associations, mapsets, descriptions, etc.
 * <br>
 * The Rest APIs also provide extensive search capabilities across the content of the system.
 * <br>
 * <br>
 * On failure a ISAAC REST API will return an HTTP response containing a JSON-serialized RestExceptionResponse object.
 * <br>
 * <table>
 * <tr>
 * <td style="padding:0 15px 0 15px;">Status</td>
 * <td style="padding:0 15px 0 15px;">Code</td>
 * <td style="padding:0 15px 0 15px;">Description</td>
 * </tr>
 * <tr>
 * <td style="padding:0 15px 0 15px;">BAD_REQUEST</td>
 * <td style="padding:0 15px 0 15px;">400</td>
 * <td style="padding:0 15px 0 15px;">Bad Request - API client passed a query a bad parameter, parameter value or combination of parameters</td>
 * </tr>
 * <tr>
 * <td style="padding:0 15px 0 15px;">UNAUTHORIZED</td>
 * <td style="padding:0 15px 0 15px;">401</td>
 * <td style="padding:0 15px 0 15px;">Unauthorized - API user is unauthorized</td>
 * </tr>
 * <tr>
 * <td style="padding:0 15px 0 15px;">INTERNAL_SERVER_ERROR</td>
 * <td style="padding:0 15px 0 15px;">500</td>
 * <td style="padding:0 15px 0 15px;">Internal Server Error - default, undifferentiated server-side error</td>
 * </tr>
 * <tr>
 * <td style="padding:0 15px 0 15px;">SERVICE_UNAVAILABLE</td>
 * <td style="padding:0 15px 0 15px;">503</td>
 * <td style="padding:0 15px 0 15px;">Service Unavailable - ISAAC is not yet initialized</td>
 * </tr>
 * </table>
 * <br>
 * <br>
 * <img src="doc/ISAAC Core API.png"/>
 * </p>
 */
package net.sagebits.tmp.isaac.rest.api;