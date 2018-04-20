<?php
  // Set header to json
  header("Content-Type: application/json");

  if (array_key_exists("league", $_GET)) {
    $PARAM_league = ucwords(strtolower(trim(preg_replace("/[^A-Za-z ]/", "", $_GET["league"]))));
    if (!$PARAM_league) die("{\"error\": \"Invalid params\", \"field\": \"league\"}");
  } else die("{\"error\": \"Invalid params\", \"field\": \"league\"}");

  // Check if user inputted the correct league
  $leagueJSON = json_decode( file_get_contents(dirname(getcwd(), 2) . "/data/leagues.json") , true );
  if ($PARAM_league && !in_array($PARAM_league, $leagueJSON)) {
    die("{\"error\": \"Invalid params\", \"field\": \"league\"}");
  }

  // Check if user inputted the correct category
  $categoryJSON = json_decode( file_get_contents(dirname(getcwd(), 2) . "/data/categories.json") , true );

  $payload = [];
  $counter = 0;

  // Get JSON object from file
  foreach ($categoryJSON as $categoryName => $tmp_val) {
    $jsonFile = json_decode( file_get_contents(dirname(getcwd(), 2) . "/data/output/" . $PARAM_league . "/" . $categoryName . ".json") , true );

    foreach ($jsonFile as $item) {
      $temp = [
        "mean" => $item["mean"],
        "median" => $item["median"],
        "mode" => $item["mode"],
        "count" => $item["count"],
        "key" => $item["specificKey"]
      ];

      array_push($payload, $temp);
    }
  }

  echo json_encode($payload, true);
?>
