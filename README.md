# PoE stash API JSON statistics generator

## What it does? 
TL;DR: calculates how much an item is worth (see [poe.ninja](http://poe.ninja)) based on Path of Exile's public [stash API](www.pathofexile.com/api/public-stash-tabs), writes results in JSON

## Workflow
1. Concurrently downloads JSON-formatted strings from the official Path of Exile stash API
2. Deserializes the JSON string and matches found items against filters
3. Adds item and its listed price to temporary database
4. Runs service at a timed interval to purge and clean database of wrongfully priced items
5. Generates JSON string containing mean, median prices and count of almost every single item found

## Usage
1. Edit config to likings
2. Compile and run via CLI
3. Enter initial launch parameters
4. Leave it running

## Notes
* May use up to 300MB of memory and 30MB of disk space
* JDK 9.0.1 used
* Maven was used to add Jackson
* This is still a work in progress

## Dependencies
* [Jackson](https://github.com/FasterXML/jackson-core) for JSON deserialization 