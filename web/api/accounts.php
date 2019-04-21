<?php
function error($code, $msg) {
  http_response_code($code);
  die(json_encode(array("error" => $msg)));
}

function check_errors() {
  if (!isset($_GET["character"]) || !$_GET["character"]) {
    error(400, "Missing account");
  }

  if (strlen($_GET["character"]) > 32) {
    error(400, "Parameter too long");
  }

  if (strlen($_GET["character"]) < 3) {
    error(400, "Parameter too short");
  }
}

function get_accounts_by_character($pdo, $name) { 
  $query = "
  select name, 
    DATE_FORMAT(found, '%Y-%m-%dT%TZ') AS found,
    DATE_FORMAT(seen, '%Y-%m-%dT%TZ') AS seen
  from account_accounts
  where id = (select id_a from account_characters where name = ? limit 1)
  limit 1
  ";

  $stmt = $pdo->prepare($query);
  $stmt->execute([$name]);

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
include_once ( "../details/pdo.php" );

$data = get_accounts_by_character($pdo, $_GET["character"]);

// Display generated data
echo json_encode($data);
