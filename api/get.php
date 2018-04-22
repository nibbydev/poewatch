<?php
// Set header to json
header("Content-Type: application/json");

if (array_key_exists("league", $_GET)) {
  $PARAM_league = ucwords(strtolower(trim(preg_replace("/[^A-Za-z ]/", "", $_GET["league"]))));
  if (!$PARAM_league) die("{\"error\": \"Invalid params\", \"field\": \"league\"}");
} else die("{\"error\": \"Invalid params\", \"field\": \"league\"}");

if (array_key_exists("category", $_GET)) {
  $PARAM_category = strtolower(trim(preg_replace("/[^A-Za-z ]/", "", $_GET["category"])));
  if (!$PARAM_category) die("{\"error\": \"Invalid params\", \"field\": \"category\"}");
} else die("{\"error\": \"Invalid params\", \"field\": \"category\"}");

// Check if file exists
$filePath = dirname(getcwd(), 2) . "/data/output/$PARAM_league/$PARAM_category.json";
if ( !file_exists($filePath) ) die("{\"error\": \"Invalid params\"}");

$data = file_get_contents( $filePath );
// If there was an error opening the file
if ($data === false) die("{\"error\": \"An error occurred\"}");

echo $data;
