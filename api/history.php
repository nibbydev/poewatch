<?php
// Set header to json
header('Content-Type: application/json');

if (array_key_exists("league", $_GET)) {
  $PARAM_league = ucwords(strtolower(trim(preg_replace("/[^A-Za-z ]/", '', $_GET["league"]))));
  if (!$PARAM_league) die("{'error': 'Invalid params', 'field': 'league'}");
} else die("{'error': 'Invalid params', 'field': 'league'}");

if (array_key_exists("key", $_GET)) {
  $PARAM_key = preg_replace("/[^A-Za-z0-9|:' ]/", '', $_GET["key"]);
  if (!$PARAM_key) die("{'error': 'Invalid params', 'field': 'key'}");
} else die("{'error': 'Invalid params', 'field': 'key'}");


$splitKey = explode('|', $PARAM_key);
if (count($splitKey) < 4) die("{'error': 'Invalid params', 'field': 'key'}");

$category = strtolower(trim($splitKey[1]));
$genericKey = implode('|', array_slice($splitKey, 2));

//echo $PARAM_league . "\n";
//echo $category . "\n";
//echo $genericKey . "\n";

if ($category !== "currency") $category = "items";

$fileName = dirname(getcwd(), 2) . "/data/history/$PARAM_league.$category.json";
$json = json_decode( file_get_contents( $fileName) , true );

if (array_key_exists($genericKey, $json)) {
  echo json_encode($json[$genericKey]);
} else die("{\"prices\": [], \"tags\": []}");