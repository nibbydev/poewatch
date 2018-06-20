<?php
function get_leagues($pdo) {
  $query = "SELECT `name` FROM `sys-leagues`";
  $stmt = $pdo->query($query);

  $rows = array();
  while ($row = $stmt->fetch(PDO::FETCH_NUM)) {
    $rows[] = str_replace(" ", "", strtolower($row[0]));
  }

  return $rows;
}

function get_item($pdo, $league, $id) {
  $query = "SELECT 
    `i`.`mean`, `i`.`median`, `i`.`mode`, `i`.`exalted`, 
    `i`.`count`, `i`.`quantity`, `i`.`inc`, `i`.`dec`, 
    `idp`.`name`, `idp`.`type`, `idp`.`frame`, `idp`.`key` AS 'key_parent', 
    `idc`.`tier`, `idc`.`lvl`, `idc`.`quality`, `idc`.`corrupted`, 
    `idc`.`links`, `idc`.`var`, `idc`.`key` AS 'key_child', `idc`.`icon`, 
    `cc`.`name` AS 'category_child', `cp`.`name` AS 'category_parent'
  FROM `#_$league-items` AS `i` 
  JOIN `itemdata-child` AS `idc` ON `i`.`id-idc` = `idc`.`id` 
  JOIN `itemdata-parent` AS `idp` ON `i`.`id-idp` = `idp`.`id` 
  LEFT JOIN `category-parent` AS `cp` ON `idp`.`id-cp` = `cp`.`id` 
  LEFT JOIN `category-child` AS `cc` ON `idp`.`id-cc` = `cc`.`id` 
  WHERE `i`.`id` = ?";

  $stmt = $pdo->prepare($query);
  $stmt->execute([$id]);

  return $stmt->fetch();
}

function get_history($pdo, $league, $id) {
  $query = "SELECT * FROM `#_$league-history` 
    WHERE `id-i` = ? AND `id-ch` = 3 
    ORDER BY `time` DESC
    LIMIT 7";

  $stmt = $pdo->prepare($query);
  $stmt->execute([$id]);
  return $stmt;
}

function prep_item($item) {
  $item["history"] = array(
    "mean" => array(null, null, null, null, null, null, null),
    "median" => array(null, null, null, null, null, null, null),
    "mode" => array(null, null, null, null, null, null, null),
    "exalted" => array(null, null, null, null, null, null, null),
    "count" => array(null, null, null, null, null, null, null),
    "quantity" => array(null, null, null, null, null, null, null)
  );

  $item["mean"] = floatval($item["mean"]);
  $item["median"] = floatval($item["median"]);
  $item["mode"] = floatval($item["mode"]);
  $item["exalted"] = floatval($item["exalted"]);

  return $item;
}

header("Content-Type: application/json");

if (!isset($_GET["id"])) die("{\"error\": \"Invalid params\"}" );
if (!isset($_GET["league"])) die("{\"error\": \"Invalid params\"}" );

$id = $_GET["id"];
$league = $_GET["league"];

include_once ( "details/pdo.php" );

$leagues = get_leagues($pdo);
if (!in_array($league, $leagues)) die("{\"error\": \"Invalid params\"}" );

$payload = get_item($pdo, $league, $id);
if (!$payload) die("{\"error\": \"Invalid params\"}" );
$payload = prep_item($payload);

$history = get_history($pdo, $league, $id);

$counter = 6;
while ($row = $history->fetch()) {
  $payload["history"]["mean"][$counter] = floatval($row["mean"]);
  $payload["history"]["median"][$counter] = floatval($row["median"]);
  $payload["history"]["mode"][$counter] = floatval($row["mode"]);
  $payload["history"]["exalted"][$counter] = floatval($row["exalted"]);
  $payload["history"]["count"][$counter] = $row["count"];
  $payload["history"]["quantity"][$counter] = $row["quantity"];

  $counter--;
}

echo json_encode($payload);
