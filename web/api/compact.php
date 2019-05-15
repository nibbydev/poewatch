<?php
function error($code, $msg) {
  http_response_code($code);
  die(json_encode(array("error" => $msg)));
}

function check_errors() {
  if (!isset($_GET["league"])) {
    error(400, "Missing league");
  }
}

function get_data($pdo, $league) {
  $query1 = "SELECT 
    i.id_d, i.mean, i.median, i.mode, i.min, i.max, 
    i.exalted, i.total, i.daily, i.current, i.accepted
  FROM      league_items AS i 
  JOIN      data_leagues AS l 
    ON      l.id = i.id_l 
  WHERE     l.name   = ?
    AND     l.active = 1 
    AND     i.total  > 1 
  ORDER BY  id ASC";

  $stmt = $pdo->prepare($query1);
  $stmt->execute([$league]);

  return $stmt;
}

function parse_data($stmt) {
  $payload = array();

  while ($row = $stmt->fetch()) {
    // Form a temporary row array
    $tmp = array(
      'id' => (int)$row['id_d'],

      'mean' => (float)$row['mean'],
      'median' => (float)$row['median'],
      'mode' => (float)$row['mode'],
      'min' => (float)$row['min'],
      'max' => (float)$row['max'],
      'exalted' => (float)$row['exalted'],

      'total' => (int)$row['total'],
      'daily' => (int)$row['daily'],
      'current' => (int)$row['current'],
      'accepted' => (int)$row['accepted'],
    );

    // Append row to payload
    $payload[] = $tmp;
  }

  return $payload;
}


header("Content-Type: application/json");
check_errors();
include_once("../details/pdo.php");

// Get database entries
$stmt = get_data($pdo, $_GET["league"]);
$data = parse_data($stmt);

// Display generated data
echo json_encode($data, JSON_PRESERVE_ZERO_FRACTION);
