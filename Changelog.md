# Changelog

## Version 1.7
* Addition of changelog
* Find programs by looking in the table mirroredritzauprogram. This change was needed as ritzau harvesting changed, and a combined view of mirroredritzauprogram and ritzauprogram was not working properly. Lookup in mirroredritzauprogram table is done in 'slices' so that only the newest version of programming for a given day is shown. Lookup on 'id' still looks for a specific entry in the database even if it is not the newest version (to keep cutting functioning when something has been ordered ahead of time).
* Stop trying to match with TvMeter data, as this feature has not been used by CFU for a long time. Hooks for re-implementing TvMeter maching is left in-place should need arise.
