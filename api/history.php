<?php
// Set header to json
header('Content-Type: application/json');

if (array_key_exists("category", $_GET)) {
  $PARAM_category = strtolower(trim(preg_replace("/[^A-Za-z-]/", '', $_GET["category"])));
  if (!$PARAM_category) die("{'error': 'Invalid params', 'field': 'category'}");
} else die("{'error': 'Invalid params', 'field': 'category'}");

if (array_key_exists("index", $_GET)) {
  $PARAM_index = preg_replace("/[^A-Za-z0-9-]/", '', $_GET["index"]);
  if (!$PARAM_index) die("{'error': 'Invalid params', 'field': 'index'}");
} else die("{'error': 'Invalid params', 'field': 'index'}");

// Test parameter lengths
if (strlen($PARAM_index) > 20) die("{'error': 'Invalid params', 'field': 'index'}");
if (strlen($PARAM_category) > 20) die("{'error': 'Invalid params', 'field': 'category'}");

// Test index integrity
$splitIndex = explode('-', $PARAM_index);
if (sizeof($splitIndex) !== 2) die("{'error': 'Invalid params', 'field': 'index'}");
else if (strlen($splitIndex[0]) !== 4) die("{'error': 'Invalid params', 'field': 'index'}");
else if (strlen($splitIndex[1]) !== 2) die("{'error': 'Invalid params', 'field': 'index'}");

// Form file name
$fileName = dirname(getcwd(), 2) . "/data/history/$PARAM_category.json";

// Check if file exists
if (!file_exists($fileName)) die("{'error': 'Invalid params', 'field': 'category'}");

// Get JSON
$json = json_decode( file_get_contents( $fileName ) , true );

if (array_key_exists($PARAM_index, $json)) {
  echo json_encode($json[$PARAM_index]);
} else die("{\"prices\": [], \"tags\": []}");