<?php
header("Content-Type: application/json");
include_once ( "details/pdo.php" );

$query = "SELECT * FROM `leagues`";
$stmt = $pdo->query($query);

$rows = array();

while ($row = $stmt->fetch()) {
  $rows[] = $row;
}

echo json_encode($rows);