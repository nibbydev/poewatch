<?php
header("Content-Type: application/json");
include_once ( "details/db_connect.php" );

$query = "SELECT * FROM `leagues`";
$result = mysqli_query($conn, $query);

$rows = array();

while ($row = mysqli_fetch_assoc($result)) {
  $rows[] = $row;
}

echo json_encode($rows);