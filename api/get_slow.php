<?php
function get_param_league() {
  if ( !isset($_GET["league"]) ) {
    die("{\"error\": \"Invalid params\", \"field\": \"league\"}");
  }

  $league = strtolower(trim(preg_replace("/[^A-Za-z0-9-]/", "", $_GET["league"])));

  if (!$league || strlen($league) <  3) {
    die("{\"error\": \"Invalid params\", \"field\": \"league\"}");
  }

  return $league;
}

function get_param_category() {
  if ( !isset($_GET["category"]) ) {
    die("{\"error\": \"Invalid params\", \"field\": \"category\"}");
  }

  $category = strtolower(trim(preg_replace("/[^A-Za-z]/", "", $_GET["category"])));

  if (!$category || strlen($category) < 3) {
    die("{\"error\": \"Invalid params\", \"field\": \"category\"}");
  }

  return $category;
}

function get_items($pdo, $league, $category) {
  $query = "SELECT 
    i.`sup`, i.`sub`, d.`child`, 
    d.`name`, d.`type`, d.`frame`, d.`icon`, 
    d.`var`, d.`tier`, d.`lvl`, d.`quality`, d.`corrupted`, d.`links`,
    i.`mean`, i.`median`, i.`mode`, i.`exalted`, 
    i.`count`, i.`quantity`
  FROM `#_item_$league` AS i
  JOIN (
    SELECT 
        p.`sup`, b.`sub`,
        p.`child`,
        p.`name`, p.`type`,
        p.`frame`, b.`icon`,
        p.`key` AS 'generic_key', b.`key` AS 'specific_key',
        b.`var`, b.`tier`, b.`lvl`, b.`quality`, b.`corrupted`, b.`links` 
    FROM `item_data_sub` AS b
    JOIN `item_data_sup` AS p
        ON b.`sup` = p.`sup`
    WHERE p.`parent` = ?
  ) AS d ON i.`sup` = d.`sup` AND i.`sub` = d.`sub`
  ORDER BY i.`mean` DESC ";

  $stmt = $pdo->prepare($query);
  $stmt->execute([$category]);

  return $stmt;
}

function prep_item($item) {
  $item["history"] = array(
    "mean" => array(),
    "median" => array(),
    "mode" => array(),
    "exalted" => array(),
    "count" => array(),
    "quantity" => array()
  );

  $item["mean"] = floatval($item["mean"]);
  $item["median"] = floatval($item["median"]);
  $item["mode"] = floatval($item["mode"]);
  $item["exalted"] = floatval($item["exalted"]);

  return $item;
}

function prepare_history_query($pdo, $league) {
  $query = "SELECT * FROM `#_history_$league` WHERE `sup` = ? AND `sub` = ? AND `type`='minutely' LIMIT 7";
  return $pdo->prepare($query);
}


header("Content-Type: application/json");

$league = get_param_league();
$category = get_param_category();

include_once ( "details/pdo.php" );

$items_stmt = get_items($pdo, $league, $category);
$history_stmt = prepare_history_query($pdo, $league);

$items = array();

while ($item = $items_stmt->fetch()) {
  $item = prep_item($item);

  $history_stmt->execute([$item["sup"], $item["sub"]]);
  while ($row = $history_stmt->fetch()) {
    $item["history"]["mean"][] = floatval($row["mean"]);
    $item["history"]["median"][] = floatval($row["median"]);
    $item["history"]["mode"][] = floatval($row["mode"]);
    $item["history"]["exalted"][] = floatval($row["exalted"]);
    $item["history"]["count"][] = $row["count"];
    $item["history"]["quantity"][] = $row["quantity"];
  }

  $items[] = $item;
}

print json_encode($items);