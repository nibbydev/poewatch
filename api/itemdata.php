<?php
  // Set header to json
  header('Content-Type: application/json');

  // Get file
  $file = file_get_contents(dirname(getcwd(), 2) . "/data/itemData.json");

  // Get url params and allow only alphanumeric inputs
  if (array_key_exists("index", $_GET)) {
    $paramIndex = trim(preg_replace("/[^A-Za-z0-9-,]/", '', $_GET["index"]));
    if (!$paramIndex) die("{'error': 'Invalid params', 'field': 'index'}");
  } else {
    echo $file;
    return;
  }

  $payload = new stdClass;
  $indexList = explode(",", $paramIndex);

  if (sizeof($indexList) > 100) {
    die("{'error': 'Invalid params', 'field': 'index'}");
  }
  
  $jsonFile = json_decode( $file, true );

  // Loop through the JSON file
  foreach ($indexList as $index) {
    if ((int)$index >= 0) $payload->{$index} = $jsonFile[$index];
  }

  // echo constructed payload
  echo json_encode( (array) $payload, true );
?>