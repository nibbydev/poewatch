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
  $PARAM_fields = explode(',', strtolower(trim($PARAM_fields)));
  $PARAM_parent = strtolower(trim($PARAM_parent));
  $PARAM_child = strtolower(trim($PARAM_child));
  $PARAM_from = (int)trim($PARAM_from);
  $PARAM_to = (int)trim($PARAM_to);

  // Don't run with missing params
  if (!$PARAM_league || !$PARAM_parent) {
    echo "{\"error\": \"Missing params\"}";
    return;
  }
  // Don't run with too many params
  if (sizeof($PARAM_fields) > 20) {
    echo "{\"error\": \"Too many params\"}";
    return;
  }
  // Check if user inputted an invalid string that got filtered out
  if ($_GET["league"] && !$PARAM_league || $_GET["parent"] && !$PARAM_parent ||
    $_GET["child"] && !$PARAM_child || $_GET["from"] && !$PARAM_from ||
    $_GET["to"] && !$PARAM_to) {
    echo "{\"error\": \"Invalid params\"}";
    return;
  }

  // If user requested whole file
  if ($PARAM_parent === "all") $PARAM_parent = null;
  // If user requested whole category
  if ($PARAM_child === "all") $PARAM_child = null;

  // Check if user inputted the correct league
  $leagueJSON = json_decode( file_get_contents(dirname(getcwd(), 2) . "/data/leagues.json") , true );
  if ($PARAM_league && !in_array($PARAM_league, $leagueJSON)) {
    echo "{\"error\": \"Invalid league\"}";
    return;
  }

  // Check if user inputted the correct category
  $categoryJSON = json_decode( file_get_contents(dirname(getcwd(), 2) . "/data/categories.json") , true );
  if ($PARAM_parent && !array_key_exists($PARAM_parent, $categoryJSON)) {
    echo "{\"error\": \"Invalid parent category\"}";
    return;
  }
  if ($PARAM_parent && $PARAM_child && !in_array($PARAM_child, $categoryJSON[$PARAM_parent])){
    echo "{\"error\": \"Invalid child category\"}";
    return;
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

      if (!$PARAM_parent) $item["parent"] = $parentCategoryKey;

      if (array_key_exists("links", $item)) $item["links"] = $item["links"];
      if (array_key_exists("lvl", $item)) $item["lvl"] = $item["lvl"];
      if (array_key_exists("quality", $item)) $item["quality"] = $item["quality"];
      if (array_key_exists("tier", $item)) $item["tier"] = $item["tier"];
      if (array_key_exists("corrupted", $item)) $item["corrupted"] = $item["corrupted"];

      array_push($payload, $item);
    }
  }

  echo json_encode( $payload, true );
?>