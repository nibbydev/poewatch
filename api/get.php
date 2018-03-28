<?php
  // Set header to json
  header('Content-Type: application/json');

  // Get url params and allow only alphanumeric inputs
  $PARAM_league = preg_replace("/[^A-Za-z ]/", '', $_GET["league"]);
  $PARAM_fields = preg_replace("/[^A-Za-z,]/", '', $_GET["fields"]);
  $PARAM_parent = preg_replace("/[^A-Za-z]/", '', $_GET["parent"]);
  $PARAM_child = preg_replace("/[^A-Za-z]/", '', $_GET["child"]);
  $PARAM_from = preg_replace("/[^0-9]/", '', $_GET["from"]);
  $PARAM_to = preg_replace("/[^0-9]/", '', $_GET["to"]);

  // Trim and format whatever's left
  $PARAM_league = ucwords(strtolower(trim($PARAM_league)));
  $PARAM_fields = strtolower(trim($PARAM_fields));
  $PARAM_parent = strtolower(trim($PARAM_parent));
  $PARAM_child = strtolower(trim($PARAM_child));
  $PARAM_from = (int)trim($PARAM_from);
  $PARAM_to = (int)trim($PARAM_to);

  // Check if user inputted an invalid string that got filtered out
  if (!$_GET["league"] || !$PARAM_league || !$_GET["parent"] || !$PARAM_parent ||
    $_GET["child"] && !$PARAM_child || $_GET["from"] && !$PARAM_from ||
    $_GET["to"] && !$PARAM_to || $_GET["fields"] && !$PARAM_fields) {

    die("{\"error\": \"Invalid params\"}");
  }

  // If user requested whole file
  if ($PARAM_parent === "all") $PARAM_parent = null;
  // If user requested whole category
  if ($PARAM_child === "all") $PARAM_child = null;
  // If user requests all fields (not really needed)
  if ($PARAM_fields === "all") $PARAM_fields = null;

  // If user requests specific fields
  if ($PARAM_fields) {
    $PARAM_fields = explode(",", $PARAM_fields);
    if (sizeof($PARAM_fields) > 20) die("{\"error\": \"Too many fields\"}"); 
  }

  // Check if user inputted the correct league
  $leagueJSON = json_decode( file_get_contents(dirname(getcwd(), 2) . "/data/leagues.json") , true );
  if ($PARAM_league && !in_array($PARAM_league, $leagueJSON)) {
    die("{\"error\": \"Invalid league\"}");
  }

  // Check if user inputted the correct category
  $categoryJSON = json_decode( file_get_contents(dirname(getcwd(), 2) . "/data/categories.json") , true );
  if ($PARAM_parent && !array_key_exists($PARAM_parent, $categoryJSON)) {
    die("{\"error\": \"Invalid parent category\"}");
  }
  if ($PARAM_parent && $PARAM_child && !in_array($PARAM_child, $categoryJSON[$PARAM_parent])){
    die("{\"error\": \"Invalid child category\"}");
  }

  $payload = [];
  $counter = 0;

  // Get JSON object from file
  foreach ($categoryJSON as $categoryName => $tmp_val) {
    if ($PARAM_parent && $PARAM_parent !== $categoryName) continue;

    $jsonFile = json_decode( file_get_contents(dirname(getcwd(), 2) . "/data/output/" . $PARAM_league . "/" . $categoryName . ".json") , true );

    // Loop through items in a category
    foreach ($jsonFile as $item) {
      if ($PARAM_child && array_key_exists("child", $item) && $PARAM_child !== $item["child"]) continue;
      
      $counter++;
      // Set starting position
      if ($PARAM_from > 0 && $counter <= $PARAM_from) continue;
      // Set ending position
      if ($PARAM_to > 0 && $counter > $PARAM_to) break;

      if (!$PARAM_parent) $item["parent"] = $categoryName;

      if (array_key_exists("links", $item)) $item["links"] = $item["links"];
      if (array_key_exists("lvl", $item)) $item["lvl"] = $item["lvl"];
      if (array_key_exists("quality", $item)) $item["quality"] = $item["quality"];
      if (array_key_exists("tier", $item)) $item["tier"] = $item["tier"];
      if (array_key_exists("corrupted", $item)) $item["corrupted"] = $item["corrupted"];

      // If user requested specific fields
      if ($PARAM_fields) {
        $filtered_item = [];

        // Loop through the fields provided by the user
        foreach ($PARAM_fields as $field) {
          // If the field exists in the item array, add it to the filtered item array
          if (array_key_exists($field, $item)) $filtered_item[$field] = $item[$field];
        }

        // Add filtered item array to response payload
        if ($filtered_item) array_push($payload, $filtered_item);
      } else {
        // Otherwise add whole item with all fields to response payload
        array_push($payload, $item);
      }
    }
  }

  echo json_encode( $payload, true );
?>