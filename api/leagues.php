<?php
function get_data($pdo) {
  $query = 'SELECT * FROM data_leagues WHERE active = 1';

  $stmt = $pdo->query($query);

  return $stmt;
}

function parse_data($stmt) {
  $payload = array();

  while ($row = $stmt->fetch()) {
    unset( $row['active'] );
    $row['event'] = $row['event'] === 1 ? true : false;

    $payload[] = $row;
  }

  return $payload;
}

// Define content type
header('Content-Type: application/json');

// Connect to database
include_once ( 'details/pdo.php' );

// Get data from database
$stmt = get_data($pdo);

// Parse received data
$payload = parse_data($stmt);

// Display generated data
echo json_encode($payload);
