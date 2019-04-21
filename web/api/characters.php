<?php
function error($code, $msg) {
  http_response_code($code);
  die(json_encode(array("error" => $msg)));
}

function check_errors() {
  if (!isset($_GET["account"]) || !$_GET["account"]) {
    error(400, "Missing account");
  }

  if (strlen($_GET["account"]) > 32) {
    error(400, "Parameter too long");
  }

  if (strlen($_GET["account"]) < 3) {
    error(400, "Parameter too short");
  }
}

function get_characters_by_account($pdo, $name) {
  $query = "
  SELECT 
    ac.name AS `character`, 
    l.name AS league, 
    DATE_FORMAT(ac.found, '%Y-%m-%dT%TZ') AS found,
    DATE_FORMAT(ac.seen, '%Y-%m-%dT%TZ') AS seen
  from account_characters as ac
  join data_leagues as l 
    on ac.id_l = l.id
  where id_a = (select id from account_accounts where name = ? limit 1)
  order by seen desc
  limit 128
  ";

  $stmt = $pdo->prepare($query);
  $stmt->execute([$name]);

  return $stmt;
}

function parse_data($stmt) {
  $payload = array();

  while ($row = $stmt->fetch()) {
    $payload[] = $row;
  }

  return $payload;
}

// Define content type
header("Content-Type: application/json");

// Check parameter errors
check_errors();

// Connect to database
include_once("../details/pdo.php");

$stmt = get_characters_by_account($pdo, $_GET["account"]);
$data = parse_data($stmt);

// Display generated data
echo json_encode($data);
