ISAAC-rest Changelog 

This changelog summarizes changes and fixes which are a part of each revision.  For more details on the fixes, refer tracking numbers where provided, and the git commit history.  Note that this is not the same as the API Changelog.md.  This file will contain information on all changes - including bug fixes.  The API Changelog will only contain documentation on changes of the API - and those are tied to the 
API version number, not the release version number.

* 2019/06/?? - 6.28
    * Corrected documentation for expand params on /logicGraph/chronology calls.
    * Added validation / error checking to expand parameter values, so it will now flag ones that are unknown and/or unsupported by a method.
    * Corrected / enhanced documentation on versionAll expansion options.  Fixed implemenation issues with versionAll, so that nested or referenced
        components are populated with a version that corresponds to the version of the requested item being returned.

* 2019/06/07 - 6.27
    * Fixed a bug where it wasn't rejecting /write/ requests that were missing an edit token with the correct error message.
    * Fixed a bug (upstream) where the index wasn't flagging metadata properly, leading to prefixSearchs with a metadata restriction to not work properly.
        Existing databases must be re-indexed with the latest code, to start working properly.  .../rest/write/1/system/rebuildIndex?editToken=.....
    * Fix a bug that prevented altIDs from being populated for the referenced component in certain semantic calls.

* 2019/04/19 - 6.26
    * API enhancements per the API changelog
    * Fixed a bug with paged result counts when filtering off path results

* 2019/03/28 - 6.25
    * Lots of internal changes to migrate auth over to the new uts-auth-api code.
    * Used a more standard approach for validating authentication
    * Cleanup of property / config file naming.  The file to configure this server is now uts-rest-api.properties.  src/test/resources contains a 
        documented example.
    * Allowed anonymous read to the system/systemImfo call even when anonymous read is disabled elsewhere - this may be changed again in the future.
        But before that can happen, we need to have keys for a AUTOMATED user populated into the web-editor.

* 2019/02/06 - 6.24
    * added an api for storing user prefs, etc

* 2019/01/25 - 6.23
    * fixing the classifier return results
    * better debug output on classification runs

* 2019/01/24 - 6.22
    * updates to classifier results handling

* 2019/01/22 - 6.21
    * Fixed bug with terminology type lookup (in core)
    * Fixed bugs with DB build process that was leading to runtime errors and bad hierarchy on certain concepts
    * API tweaks to createConcept, to make it more permissive during semantic tag creation with snomed concepts that co-exist in metadata.
    * Fix a bug with mapset update where we mis-handled the optional displayField and failed to process an update if it wasn't provided.
    * Handling for more ID types, new options for returning alternate IDs.

* 2019/01/10 - 6.20
    * Fix a regression with the handling of name, inverse name and description on mapsets, related to external description types.
    * Fix a bug with update of mapping items where null's in the data arraylist lead to a null pointer.

* 2018/12/31 - 6.19
    * Reworking the storage of classifier results, to improve performance.
    * API additions for management of stored classifier data

* 2018/12/28 - 6.18
    * API updates to the classifier API per the API log.

* 2018/12/28 - 6.17
    * API additions per the API change log
    * Bug fixes for testing with local DEFAULT user.

* 2018/12/14 - 6.16
    * API tweaks in the LogicGraph returns, per the API changelog
    * Fix a bug with parents / parent counts in search results.

* 2018/11/20 - 6.15
    * improve description reading for RestIdentifiedObjects, so it follows the coord prefs better
    * small API changes to hierarchy under RestLogicNode.  See API changelog.
    * API enhancements to support logic graph editing.

* 2018/10/30 - 6.14
    * Performance improvements from a new version of core.
    * Switch over to new non-tree APIs for most hierarchy calls.
    * Add a request ID to the logs for easier tracking
    * Add a slow query log

* 2018/10/29 - 6.13
    * Fixed a regression with coordinates on the initial edit
    * Update tests to run tests for the beer ontology using the correct module
    * Add concept create validations for UTS-154
    * Bugfixes from a new version of core

* 2018/10/23 - 6.12
    * Added support to change the return type to xml or json by appending a file extension (.json or .xml) to the request
    * Fixed a bug where several SystemAPI methods were not honoring (or allowing) individual coordinate parameters, such as 'stated'
    * fix a bug with returning XML for RestIdentifiedObject
    * Fix a bug (in core) with handling external description types related to getting duplicate external types.

* 2018/10/15 - 6.11
    * non-breaking API enhancements per the API change log - 1.17.2 release
    * removed unused and inoperative 'expand' type of 'descriptions'
    * bugfixes from new relase of core

* 2018/10/05 - 6.10
    * Include bug fixes from core

* 2018/10/02 - 6.09
    * Mostly changes from upstream - there were a number of changes to how the metadata was structured, and changes to how the 
       yaml constants are generated.  

* 2018/08/30 - 6.08
    * Fix naming conventions / metadata reading to align with upstream changes.

* 2018/08/24 - 6.07
    * Just an internal build

* 2018/08/23 - 6.06
    * Fix startup sequence bug related to git config

* 2018/08/22 - 6.05
    * Build for testing jenkins pipelines, update core code.

* 2018/08/01 - 6.04
    * Support encrypted passwords in the prisme.properties file.  Improve internal handling of prisme.properties, restore ability to download
        a DB at startup time.
    * Fix bug with finding the metadata to log during startup
    
* 2018/07/23 - 6.03
    * Just an internal build

* 2018/07/23 - 6.02
    * Fix bugs with dialect parsing, so it handles more than just 'en' and 'us'.
    * All other changes in the API change log, since the last major update.

* 2018/??/?? - 6.01
    * An unofficial release with changes for moving over to the oshera isaac.