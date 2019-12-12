<?php
function getTimeSinceLastRequest($pdo) {
  $query = "SELECT time from data_change_id";
  $stmt = $pdo->query($query);
  return $stmt->fetch()["time"];
}

function getItemLeagues($pdo) {
  $query = "SELECT DISTINCT id_l FROM league_items";

  $stmt = $pdo->query($query);
  $payload = array();

  while ($row = $stmt->fetch()) {
    $payload[] = (int) $row["id_l"];
  }

  return $payload;
}

function getTableCounts($pdo) {
  $query = "
  SELECT  TABLE_NAME, TABLE_ROWS
  FROM    information_schema.TABLES
  WHERE   table_schema = 'pw'
    AND  (table_name = 'league_characters'
    OR    table_name = 'league_accounts'
    OR    table_name = 'data_item_data')
  ";

  $stmt = $pdo->query($query);
  $payload = [];

  while ($row = $stmt->fetch()) {
    if ($row['TABLE_NAME'] === 'league_characters') {
      $payload["characters"] = $row["TABLE_ROWS"];
    } elseif ($row['TABLE_NAME'] === 'league_accounts') {
      $payload["accounts"] = $row["TABLE_ROWS"];
    } elseif ($row['TABLE_NAME'] === 'data_item_data') {
      $payload["items"] = $row["TABLE_ROWS"];
    }
  }

  return $payload;
}

function getExaltedPrice($pdo) {
  $query = "SELECT li.id_l, li.mean, li.median from league_items as li join data_item_data as did on li.id_d = did.id where did.name = 'Exalted Orb'";
  $stmt = $pdo->query($query);
  $payload = array();

  while ($row = $stmt->fetch()) {
    $payload[] = [
      "id" => (int) $row["id_l"],
      "mean" => (double) $row["mean"],
      "median" => (double) $row["median"],
    ];
  }

  return $payload;
}

header("Content-Type: application/json");
include_once ( "../details/pdo.php" );

echo json_encode([
  "exalt" => getExaltedPrice($pdo),
  "leagues" => getItemLeagues($pdo),
  "counts" => getTableCounts($pdo),
  "last" => getTimeSinceLastRequest($pdo),
], JSON_PRETTY_PRINT);
