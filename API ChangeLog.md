ISAAC-Rest Changelog 

Any time a code change is made that impacts the API returned to callers, increment the value in API ChangeLog.md (here), and in RestSystemInfo

During development, we can increment this, so long as our client code (komet) is aware of the changes.

After we gain outside customers on the API, any change should be done by bumping the major version - and creating new rest paths (/rest/2/, /rest/write/2/)
If reverse compatibility is required to be maintained, then the rest/1 or rest/write/1 code must remain.

Until that happens, however - we follow the procedure of bumping the release version if it is an API change that won't break KOMET code - such as adding a new 
parameter.  However, any change that will break KOMET code - such as changing the operation of an existing method (which KOMET uses) - then the minor revision 
(and/or the major revision) should be bumped, to force a KOMET runtime breakage due to the incompatibility. 

Bug fixes should not be documented here, rather, than should be documented in the changelog file.

*** Don't forget to update the value in the class RestSystemInfo ***

* 2019/06/?? - 1.20.4
    * Removed deprecated / non-functional Workflow parameters
	
* 2019/06/07 - 1.20.3
    * Added support for 'refset' and 'property' as "restrictTo" criteria on /search/prefix
    * Added concept/versions/{id} which returns an array of all unique stamps on the concept and any nested semantics.
    * Added an optional boolean parameter of includeAllVersions to /concept/descriptions/{id}, /semantic/forAssemblage/{id}, 
        /semantic/forReferencedComponent/{id}

* 2019/04/19 - 1.20.2
    * Added REST API support for query/flowr.  XML formatted FLOWR queries can be submitted, and returned as XML results. 
    * Added write/1/intake/ibdf for posting / uploading an IBDF file, or a zipped IBDF file.
    * Added write/1/intake/ibdfArtifact for telling the server to go fetch an IBDF file from the artifact server, and import it.
    * Added sortFull option to 1/taxonomy/version call, which when set to true, will sort the entire result prior to breaking the result
        into pages.  This allows the sorting to be consistent from one page to the next.

* 2019/03/28 - 1.20.1
    * Moved the codebase over to depending on the uts-auth-api.  No longer works against prisme.
    * Removed warId from the RestSystemInfo
    * Removed some role attributes from the Workflow APIs (but those aren't functional right now as it is)
    * Required edit permissions, or higher, in order to request an edit token

* 2019/02/06 - 1.19.8
    * Added a user data read/write APIs for clients to use to persist arbitrary data - intended to be used for storing json formatted user prefs, 
        etc, the sorts of things a GUI needs to be able to store to persist user options. 

* 2019/01/25 - 1.19.7
    * Reworked the cycle path and equivalent set part of the classifier returns, because the generated enunciate code couldn't handle arrays 
        nested in lists.

* 2019/01/24 - 1.19.6
    * Added processedConceptCount to the RestClassifierResult, and changed the data that is put into affectedConcepts and affectedConceptCount.

* 2019/01/22 - 1.19.5
    * Tweak the concept create API so that the automated semantic tag creation doesn't fail if there are two semantic tags, and one 
          of them is the metadata tag "SOLOR".  The metadata tag will be ignored, using the more specific tag.
    * Added the optional parameter altId - to most get methods that return RestIdentifiedObject structures (directly or nested).  
        When specified (with one or more values) from /1/id/types or the value 'ANY' then those ids will also be popualted in each of the 
        RestIdentifiedObjects returned.  This option is now available on most calls under /association/, /classifier/, /concept/, /logicGraph/,
        /mapping/, /search/, /semantic/, /system/, /taxonomy/, /validation/.  The specific calls that accept the parameter have the parameter
        documented. 
    * /id/* methods were enhanced to handle any type of system-flagged ID - all of the children of IDENTIFIER_SOURCE____SOLOR.

* 2018/12/31 - 1.19.4
    * Add the ability to clear the stored classifier run data via a rest call.

* 2018/12/28 - 1.19.3
    * Change the RestClassifierResult structure to add count variables for all of the fields that are arrays or lists.
    * Add a largeResults parameter to classifier readback APIs.  By default, arrays and lists are now trimmed to 100 results, unless
        largeResults=true is passed.

* 2018/12/28 - 1.19.2
    * Added system/descriptionTypes to fetch all description types, sorted and organized under the core types across all terminologies.
    * Added write/1/classifier/classify API for triggering a classification
    * Added /1/classifier/classifications and /1/classifier/classification/{id} for reading back in progress and completed classifications.
    * Added cycles and orphan information into classification result APIs (and added those checks into the classification process itself)

* 2018/12/14 - 1.19.1
    * Put back 'connectorTypeDescription' in the RestTypedConnectorNode, as the comment below in 2.18.1 was wrong - RestIdentifiedObject doesn't 
        reliably contain a description. 
    * Added an expand option of 'countParents' to the LogicGraph read API, which in combination with the existing 'version' expand parameter
        will cause a RestConceptVersion object to be returned inside of RestConceptNode and RestTypedConnectorNode, which has the parent
        count populated.
    * Removed isConceptDefined from both RestConceptNode and RestTypedConnectorNode, as this information is already included in the nested
        RestConceptVersion object, so long as expand='version' is passed in.
    * Updated the result of taxonomy/version so that if a parent count greater than 0 is requested, and children are requested, the immediate level 
        of parents for each child will be returned as well.
    * Added an expand option of  'includeParents' to the logicGraph/Version call.
    * Added an expand option of 'terminologyType' to the logicGraph/Version call.
    * Added expand options of 'countParents' and 'includeParents' to all of the search API calls.
    * Added more validation to prevent more invalid logic graph construction.

* 2018/11/20 - 1.18.1
    * Adding measureSemanticConcept to RestFeatureNode to align with internal API
    * Renaming RestUntypedConnectorNode to RestConnectorNode
    * Moving 'children' from RestLogicNode down into RestConnectorNode, so that it properly aligns with the API, and allows / doesn't 
      allow children in the right parts of the model.  Literals and concept nodes will no longer have an API that shows the possibility of children.
      Only RestConnectorNode and RestTypedConnectorNode objects may have children.
    * Removed 'connectorTypeConceptDescription' from RestTypedConnectorNode, and it only provided duplicate information already available in 
      the RestIdentifiedObject 'connectorTypeConcept'.
    * Added 1/write/logicGraph/create for updating and creating logic graphs.
    * Added 1/validation/logicGraph/ for testing the validity of a logic graph, without storing the passed logic graph.
    * Added 'isConceptDefined' to RestTypedConnectorNode

* 2018/10/23 - 1.17.2
    * Adding nextPriorityLanguageCoordinate and modulePreferences to the RestLanguageCoordinate object.
    * Adding 1/validation/findFQN to the API for checking to see if a description would lead to a duplicate FQN prior to concept create
    * Adding system/descriptionStyle/{id} which allows you to determine at runtime, whether a terminology was loaded with the native
      (snomed style) descriptions, extended description style - which is how we histocially loaded external terminologies, or external,
      where we use description types directly from the external terminology.
    * Adding system/externalDescriptionTypes which gives you a list of the description types applicable to a terminology that uses the 
      external description type style, in a hierarchical form.
    * Adding "externalDescriptionTypeConcept" to RestConceptCreateData, which allows you to specify the description type for concept creation
      when working with a terminology that uses the external description style.  Updated documentation throughout the class to document
      the behavior when this parameter is utilized.
    * Added "externalDescriptionSemantic" to RestWriteResponseConceptCreate to handle the return when "externalDescriptionTypeConcept" is 
      used during create.
    * Added "descriptionStyle" to RestTerminologyConcept which is the return type of 1/system/terminologyTypes
    * Added 1/coordinate/editModule/{id} to return the appropriate (default) edit module for a particular terminology

* 2018/07/23 - 1.17.1
    * The coordinate APIs now allow the special keyword 'recurse' as a dialect, to properly support nested dialects like 
      en -> en-us -> en-nursing.  Other bugs and documentation issues around dialects were corrected.

* 2018/01/31 - 1.17.0
    * Moving over to the new OSEHRA ISAAC.  A few API updates to correspond.
    * Things that were previously called "sememe" are now called "semantic.
    * Sequence IDs are no longer part of the data model, and are not returned in any object.
    * Any API that took in a string identifier that accepted sequence IDs now only accepts nids (and/or UUIDs, as applicable)
    * RestTaxonomyCoordinate turned into RestManifoldCoordinate - this includes paths like restTaxonomyCoordinate/ becoming 
      restManifoldCoordinate/
    * RestSememeType becomes RestSemanticType
    * The parameter "descriptionType" became "descriptionTypes", and now accepts more than one descriptionType.  It also now accepts
      nids and uuids, in addition to the string constants it previously supported.
    * The parameter "extendedDescriptionTypeId" became extendedDescriptionTypes, and now accepts more than one type.  
    * The parameter value "sememe" for a search restriction type was changed to "semantic"
    * The parameter 'dynamicSememeColumns' was changed to 'dynamicSemanticColumns'
    * The parameter 'sememeMembership' was changed to 'semanticMembership' (both in query and return objects)
    * The workflow API has been disabled for now
    * The parameter 'nestedSememes' turned into 'nestedSemantics'
    * RestIdentifiedObject no longer carries a sequence id
    * All "Dynamic" transport objects had their naming changed from "Sememe" to "Semantic"
    * RestLiteralNodeInstant definition has been fixed, to now it passes a long and an int, rather than trying to serialize an Instant.
    * in RestWriteResponseConceptCreate fsnDescriptionSememe, preferredDescriptionSememe, extendedDescriptionTypeSememe,
      hasParentAssociationSememe, dialectSememes all changed to end with "Semantic" instead
    * in RestAssociationItemVersion, sourceSememe and targetSememe changed to end with "Semantic"
    * The path restSememeType/ changed to restSemanticType/
    * RestLiteralNodeFloat changed to RestLiteralNodeDouble
    * All parameters and variables named 'fsn' became 'fqn'.  Parameters will still accept the old values, but classes with variables such as 
      RestConceptCreateData, RestWriteResponseConceptCreate have variables that changed names to match.