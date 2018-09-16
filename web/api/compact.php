<?php
function error($code, $msg) {
  http_response_code($code);
  die( json_encode( array("error" => $msg) ) );
}

function check_errors() {
  if ( !isset($_GET["league"]) )    {
    error(400, "Missing league");
  }
}

function check_league($pdo, $league) {
  $query = "SELECT l.id, l.active
  FROM data_leagues AS l
  JOIN (
    SELECT DISTINCT id_l FROM league_history_daily_rolling
    UNION  DISTINCT
    SELECT DISTINCT id_l FROM league_history_daily_inactive
  ) AS leagues ON l.id = leagues.id_l
  WHERE l.name = ?
  LIMIT 1";

  $stmt = $pdo->prepare($query);
  $stmt->execute([$league]);

  return $stmt->rowCount() === 0 ? null : $stmt->fetch();
}

function get_data_rolling($pdo, $league) {
  $query = "SELECT 
    i.id_d, i.mean, i.median, i.mode, i.exalted, 
    i.quantity + i.inc AS quantity
  FROM      league_items_rolling AS i 
  JOIN      data_leagues  AS l 
    ON      l.id = i.id_l 
  WHERE     l.name   = ?
    AND     l.active = 1 
    AND     i.count  > 1 
  ORDER BY  id ASC";

  $stmt = $pdo->prepare($query);
  $stmt->execute([$league]);

  return $stmt;
}

function get_data_inactive($pdo, $league) {
  $query = "SELECT 
    i.id_d, i.mean, i.median, i.mode, i.exalted, 
    0 AS quantity
  FROM      league_items_inactive AS i 
  JOIN      data_leagues  AS l 
    ON      l.id = i.id_l 
  WHERE     l.name   = ?
    AND     l.active = 0
    AND     i.count  > 1 
  ORDER BY  id ASC";

  $stmt = $pdo->prepare($query);
  $stmt->execute([$league]);

  return $stmt;
}

function parse_data($stmt, $active) {
  $payload = array();

  while ($row = $stmt->fetch()) {
    // Form a temporary row array
    $tmp = array(
      'id'            => (int)  $row['id_d'],

      'mean'          =>        $row['mean']      === NULL ?  0.0 : (float) $row['mean'],
      'mean'          =>        $row['median']    === NULL ?  0.0 : (float) $row['median'],
      'mean'          =>        $row['mode']      === NULL ?  0.0 : (float) $row['mode'],
      'exalted'       =>        $row['exalted']   === NULL ?  0.0 : (float) $row['exalted'],
      'quantity'      =>        $row['quantity']  === NULL ?    0 :   (int) $row['quantity']
    );

    // Append row to payload
    $payload[] = $tmp;
  }

  return $payload;
}

// Define content type
header("Content-Type: application/json");

// Check parameter errors
check_errors();

// Connect to database
include_once ( "../details/pdo.php" );

// Get league id and active state
$state = check_league($pdo, $_GET["league"]);
if ($state === null) {
  error(400, "Invalid league");
}

// Get database entries based on league state
if ($state["active"]) {
  $stmt = get_data_rolling($pdo, $_GET["league"]);
} else {
  $stmt = get_data_inactive($pdo, $_GET["league"]);
}

// If no results with provided id
if ($stmt->rowCount() === 0) {
  error(400, "No results");
}

$data = parse_data($stmt, $state["active"]);

// Display generated data
echo json_encode($data, JSON_PRESERVE_ZERO_FRACTION);
