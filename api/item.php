<?php
function get_item($pdo, $league, $sup, $sub) {
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
    WHERE b.`sup` = ? AND b.`sub` = ?
  ) AS d ON i.`sup` = d.`sup` AND i.`sub` = d.`sub`
  ORDER BY i.`mean` DESC ";

  $stmt = $pdo->prepare($query);
  $stmt->execute([$sup, $sub]);

  return $stmt->fetch();
}

function get_history($pdo, $league, $sup, $sub) {
  $query = "SELECT * FROM `#_history_$league` WHERE `sup` = ? AND `sub` = ? AND `type`='hourly'";

  $stmt = $pdo->prepare($query);
  $stmt->execute([$sup, $sub]);
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

header("Content-Type: application/json");

if ( !isset($_GET["league"]) ) die("{\"error\": \"Invalid params\", \"field\": \"league\"}");
if ( !isset($_GET["index"])  ) die("{\"error\": \"Invalid params\", \"field\": \"index\"}" );

$league = trim(preg_replace("/[^A-Za-z0-9-]/", "", strtolower($_GET["league"])));
$index  = trim(preg_replace("/[^0-9]/",        "", strtolower($_GET["index" ])));

if ( !$league || strlen($league) <  3 ) die("{\"error\": \"Invalid params\", \"field\": \"league\"}");
if ( !$index  || strlen($index ) != 7 ) die("{\"error\": \"Invalid params\", \"field\": \"index\"}" );

include_once ( "details/pdo.php" );

$sup = substr($index, 0, 5);
$sub = substr($index, 5);

$item = get_item($pdo, $league, $sup, $sub);
$item = prep_item($item);

$history = get_history($pdo, $league, $sup, $sub);

while ($row = $history->fetch()) {
  $item["history"]["mean"][] = floatval($row["mean"]);
  $item["history"]["median"][] = floatval($row["median"]);
  $item["history"]["mode"][] = floatval($row["mode"]);
  $item["history"]["exalted"][] = floatval($row["exalted"]);
  $item["history"]["count"][] = $row["count"];
  $item["history"]["quantity"][] = $row["quantity"];
}

echo json_encode($item) . "\n\n";

