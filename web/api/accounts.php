<?php
function error($code, $msg) {
  http_response_code($code);
  die( json_encode( array("error" => $msg) ) );
}

function check_errors() {
  if ( !isset($_GET["character"]) )    {
    error(400, "Missing character");
  }
}

function get_accounts_by_character($pdo, $name) { 
  $query = "
  SELECT 
    accounts.name AS name, 
    l.name AS league, 
    DATE_FORMAT(relations.found, '%Y-%m-%dT%TZ') AS found,
    DATE_FORMAT(relations.seen, '%Y-%m-%dT%TZ') AS seen
  FROM account_relations AS relations
  JOIN account_accounts AS accounts 
    ON relations.id_a = accounts.id
  JOIN data_leagues AS l 
    ON relations.id_l = l.id
  WHERE id_c = (SELECT id FROM account_characters WHERE name = ? LIMIT 1)
  ORDER BY seen DESC
  LIMIT 128
  ";

  $stmt = $pdo->prepare($query);
  $stmt->execute([$name]);

  return $stmt;
}

function parse_data($stmt) {
  $payload = array();

  while ($row = $stmt->fetch()) {
    // Form a temporary row array
    $tmp = array(
      'account' => $row['name'],
      'found'   => $row['found'],
      'seen'    => $row['seen'],
      'league'  => $row['league']
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

$stmt = get_accounts_by_character($pdo, $_GET["character"]);
$data = parse_data($stmt);

// Display generated data
echo json_encode($data, JSON_PRESERVE_ZERO_FRACTION);
