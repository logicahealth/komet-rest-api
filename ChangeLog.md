ISAAC-rest Changelog 

This changelog summarizes changes and fixes which are a part of each revision.  For more details on the fixes, refer tracking numbers where provided, and the git commit history.  Note that this is not the same as the API Changelog.md.  This file will contain information on all changes - including bug fixes.  The API Changelog will only contain documentation on changes of the API - and those are tied to the 
API version number, not the release version number.

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