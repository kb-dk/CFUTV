# Changelog

## Version 1.9
Bugfix Update
* Don't truncate to and from in search to pure dates, but preserve timestamps when querying the database


## Version 1.9
Maintenance Update
* Switch runtime to Java 11 and Tomcat 9
* Convert most java.util.Dates into java.time equivalents.
* Comply with forbidden api
* Update junit and corresponding unit tests
* Update most external dependencies

## Version 1.8
* Use explicit new version of channel-archiving-requester-core dependency to stop leaks of database connections
* Fix null-pointer exception when missing to and/or from date

## Version 1.7
* Addition of changelog
* Find programs by looking in the table mirroredritzauprogram. This change was needed as ritzau harvesting changed, and a combined view of mirroredritzauprogram and ritzauprogram was not working properly. Lookup in mirroredritzauprogram table is done in 'slices' so that only the newest version of programming for a given day is shown. Lookup on 'id' still looks for a specific entry in the database even if it is not the newest version (to keep cutting functioning when something has been ordered ahead of time).
* Stop trying to match with TvMeter data, as this feature has not been used by CFU for a long time. Hooks for re-implementing TvMeter maching is left in-place should need arise.
* Add configuration parameter days_ahead. Should be set to match the number of days ahead of time we have Ritzau metadata for. i.e. 7.

