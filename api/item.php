<?php
function error($msg) {
  //header("location:/");
  die(json_encode( array("error" => $msg) ));
}

function get_leagues($pdo) {
  $query = "SELECT `name` FROM `sys-leagues`";
  $stmt = $pdo->query($query);

  $rows = array();
  while ($row = $stmt->fetch(PDO::FETCH_NUM)) {
    $rows[] = $row[0];
  }

  return $rows;
}

function get_item($pdo, $league, $id) {
  $league = format_league($league);

  $query = "SELECT 
    i.mean, i.median, i.mode, i.exalted, 
    i.count, i.quantity, i.inc, i.dec, 
    idp.name, idp.type, idp.frame, idp.key AS key_parent, 
    idc.tier, idc.lvl, idc.quality, idc.corrupted, 
    idc.links, idc.var, idc.key AS key_child, idc.icon, 
    cc.name AS category_child, cp.name AS category_parent
  FROM `#_$league-items` AS i
  JOIN `itemdata-child` AS idc ON i.`id-idc` = idc.id 
  JOIN `itemdata-parent` AS idp ON i.`id-idp` = idp.id 
  LEFT JOIN `category-parent` AS cp ON idp.`id-cp` = cp.id 
  LEFT JOIN `category-child` AS cc ON idp.`id-cc` = cc.id 
  WHERE i.id = ?";

  $stmt = $pdo->prepare($query);
  $stmt->execute([$id]);

  return $stmt->fetch();
}

function get_history($pdo, $league, $id, $payload) {
  $league = format_league($league);

  $query = "SELECT * FROM `#_$league-history` 
    WHERE `id-i` = ? AND `id-ch` = 3 
    ORDER BY `time` DESC
    LIMIT 6";

  $stmt = $pdo->prepare($query);
  $stmt->execute([$id]);

  $counter = 5;
  while ($row = $stmt->fetch()) {
    $payload["history"]["mean"][$counter] = floatval($row["mean"]);
    $payload["history"]["median"][$counter] = floatval($row["median"]);
    $payload["history"]["mode"][$counter] = floatval($row["mode"]);
    $payload["history"]["exalted"][$counter] = floatval($row["exalted"]);
    $payload["history"]["count"][$counter] = $row["count"];
    $payload["history"]["quantity"][$counter] = $row["quantity"];

    $counter--;
  }

  $payload["history"]["mean"][6] = $payload["mean"];
  $payload["history"]["median"][6] = $payload["median"];
  $payload["history"]["mode"][6] = $payload["mode"];
  $payload["history"]["exalted"][6] = $payload["exalted"];
  $payload["history"]["count"][6] = $payload["count"];
  $payload["history"]["quantity"][6] = $payload["quantity"];

  return $payload;
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

function format_league($league) {
  $league = str_replace(" ", "_", $league);
  return strtolower($league);
}

// Define content type
header("Content-Type: application/json");

// Get parameters
if (!isset($_GET["league"])) error("Missing league");
if (!isset($_GET["id"])) error("Missing id");
$league = $_GET["league"];
$id = $_GET["id"];

// Connect to database
include_once ( "details/pdo.php" );

// Get list of leagues and check if user-provided league was an actual league
$leagues = get_leagues($pdo);
if (!in_array($league, $leagues)) error("Invalid league");

// Get basic item data
$payload = get_item($pdo, $league, $id);
if (!$payload) error("Invalid id");

// Add expected fields and covert some strings into decimals
$payload = prep_item($payload);
// Get and append history data to payload
$payload = get_history($pdo, $league, $id, $payload);

echo json_encode($payload);
