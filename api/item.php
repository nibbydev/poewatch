<?php
function get_leagues($pdo) {
  $query = "SELECT `id` FROM `leagues`";

  $stmt = $pdo->query($query);
  return $stmt->fetch_num();
}

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
  $query = "SELECT * FROM `#_history_$league` 
    WHERE `sup` = ? AND `sub` = ? AND `type`='daily' 
    ORDER BY `time` DESC
    LIMIT 7";

  $stmt = $pdo->prepare($query);
  $stmt->execute([$sup, $sub]);
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

if ( !isset($_GET["index"])  ) die("{\"error\": \"Invalid params\", \"field\": \"index\"}" );
$index  = trim(preg_replace("/[^0-9]/",        "", strtolower($_GET["index" ])));
if ( !$index  || strlen($index ) != 7 ) die("{\"error\": \"Invalid params\", \"field\": \"index\"}" );

include_once ( "details/pdo.php" );

$sup = substr($index, 0, 5);
$sub = substr($index, 5);

$leagues = get_leagues($pdo);

echo json_encode($leagues);

die();






$payload = get_item($pdo, $league, $sup, $sub);

if (!$payload) {
  die("{\"error\": \"Invalid params\", \"field\": \"index\"}" );
}

$payload = prep_item($payload);

$history = get_history($pdo, $league, $sup, $sub);

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

echo json_encode($payload) . "\n\n";

