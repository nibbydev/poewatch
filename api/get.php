<?php
  // Set header to json
  header('Content-Type: application/json');

  if (array_key_exists("league", $_GET)) {
    $PARAM_league = ucwords(strtolower(trim(preg_replace("/[^A-Za-z ]/", '', $_GET["league"]))));
    if (!$PARAM_league) die("{'error': 'Invalid params', 'field': 'league'}");
  } else die("{'error': 'Invalid params'}");

  if (array_key_exists("parent", $_GET)) {
    $PARAM_parent = strtolower(trim(preg_replace("/[^A-Za-z]/", '', $_GET["parent"])));
    if (!$PARAM_parent) die("{'error': 'Invalid params', 'field': 'parent'}");
  } else die("{'error': 'Invalid params'}");

  if (array_key_exists("exclude", $_GET)) {
    $PARAM_exclude = strtolower(trim(preg_replace("/[^A-Za-z,]/", '', $_GET["exclude"])));
    if (!$PARAM_exclude) die("{'error': 'Invalid params'}");
    $PARAM_exclude = explode(",", $PARAM_exclude);
    if (sizeof($PARAM_exclude) > 15) die("{'error': 'Too many params', 'field': 'exclude'}"); 
  } else $PARAM_exclude = "";

  if (array_key_exists("child", $_GET)) {
    $PARAM_child = strtolower(trim(preg_replace("/[^A-Za-z]/", '', $_GET["child"])));
    if (!$PARAM_child) die("{'error': 'Invalid params', 'field': 'child'}");
  } else $PARAM_child = "";

  if (array_key_exists("from", $_GET)) {
    $PARAM_from = (int)trim(preg_replace("/[^0-9\-]/", '', $_GET["from"]));
    if ($PARAM_from !== 0 && !$PARAM_from) die("{'error': 'Invalid params', 'field': 'from'}");
  } else $PARAM_from = 0;

  if (array_key_exists("to", $_GET)) {
    $PARAM_to = (int)trim(preg_replace("/[^0-9\-]/", '', $_GET["to"]));
    if ($PARAM_to !== 0 && !$PARAM_to) die("{'error': 'Invalid params', 'field': 'to'}");
  } else $PARAM_to = 0;

  if (array_key_exists("links", $_GET)) {
    $PARAM_links = trim(preg_replace("/[^0-9]/", '', $_GET["links"]));
    if ($PARAM_links !== '0' && $PARAM_links !== '5' && $PARAM_links !== '6') {
      die("{'error': 'Invalid params', 'field': 'links'}");
    }
  } else $PARAM_links = '-';

  // If user requested whole file
  if ($PARAM_parent === "all") $PARAM_parent = null;
  if ($PARAM_from === "all") $PARAM_child = null;
  if ($PARAM_child === "all") $PARAM_child = null;

  // Check if user inputted the correct league
  $leagueJSON = json_decode( file_get_contents(dirname(getcwd(), 2) . "/data/leagues.json") , true );
  if ($PARAM_league && !in_array($PARAM_league, $leagueJSON)) {
    die("{'error': 'Invalid params', 'field': 'league'}");
  }

  // Check if user inputted the correct category
  $categoryJSON = json_decode( file_get_contents(dirname(getcwd(), 2) . "/data/categories.json") , true );
  if ($PARAM_parent && !array_key_exists($PARAM_parent, $categoryJSON)) {
    if (!$PARAM_child) die("{'error': 'Invalid params', 'field': 'parent'}");
  }
  if ($PARAM_parent && $PARAM_child && !in_array($PARAM_child, $categoryJSON[$PARAM_parent])){
    if (!$PARAM_child) die("{'error': 'Invalid params', 'field': 'child'}");
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
      
      // If user specified links
      if ($PARAM_links !== '-') {
        if ($PARAM_links === '0') {
          // Skip if user requested 0 links but item had more
          if (array_key_exists("links", $item)) continue;
        } else if (array_key_exists("links", $item)) {
          // Skip if user requested x links but item had different links
          if ($item["links"] !== $PARAM_links) continue;
        } else continue;
      }

      $counter++;

      // Set starting position
      if ($PARAM_from > 0 && $counter <= $PARAM_from) continue;
      // Set ending position
      if ($PARAM_to > 0 && $counter > $PARAM_to) break;

      // If user requested specific exclude
      if ($PARAM_exclude) {
        $filtered_item = [];

        foreach ($item as $item_key => $item_value) {
          // If the field exists in the item array, add it to the filtered item array
          if (!in_array($item_key, $PARAM_exclude)) $filtered_item[$item_key] = $item_value;
        }

        $item = $filtered_item;
      }

      // Add item to response payload
      if (!empty($item)) array_push($payload, $item);
    }
  }

  echo json_encode($payload, true);
?>
