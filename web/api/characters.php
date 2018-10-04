<?php
function error($code, $msg) {
  http_response_code($code);
  die( json_encode( array("error" => $msg) ) );
}

function check_errors() {
  if ( !isset($_GET["account"]) )    {
    error(400, "Missing account");
  }
}

function get_data($pdo, $account) { 
  $query = "SELECT ac.name AS `character`, l.name AS league, 
    DATE_FORMAT(ar.found, '%Y-%m-%dT%TZ') AS found,
    DATE_FORMAT(ar.seen, '%Y-%m-%dT%TZ') AS seen
  FROM account_relations AS ar
  JOIN account_characters AS ac ON ar.id_c = ac.id
  JOIN data_leagues AS l ON ar.id_l = l.id
  WHERE id_a = (SELECT id FROM account_accounts WHERE name LIKE ? LIMIT 1)
  ORDER BY seen DESC";

  $stmt = $pdo->prepare($query);
  $stmt->execute([$account]);

  return $stmt;
}

function parse_data($stmt) {
  $payload = array();

  while ($row = $stmt->fetch()) {
    // Form a temporary row array
    $tmp = array(
      'character' => $row['character'],
      'found'     => $row['found'],
      'seen'      => $row['seen'],
      'league'    => $row['league']
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

// Get database entries
$stmt = get_data($pdo, $_GET["account"]);
$data = parse_data($stmt);

// Display generated data
echo json_encode($data, JSON_PRESERVE_ZERO_FRACTION);
