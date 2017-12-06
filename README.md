# PoE stash API JSON statistics generator

## What it does? 
TL;DR: calculates how much an item is worth (see [poe.ninja](http://poe.ninja)), writes result in JSON

## Workflow
1. Concurrently downloads JSON-formatted strings from the official Path of Exile stash API
2. Deserializes the JSON string and matches found items against filters
3. Adds item and its listed price to temporary database
4. Runs service at a timed interval to purge and clean database of wrongfully priced items
5. Generates JSON string containing mean, median prices and count of almost every single item found

## Usage
1. Compile
2. Pick number of workers (1 - 5)
3. Attach a changeID

## Dependencies
* [Jackson](https://github.com/FasterXML/jackson-core) for JSON deserialization 