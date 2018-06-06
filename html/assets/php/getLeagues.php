<?php
include_once ( "details/pdo.php" );

$query = "SELECT * FROM `leagues`";
$stmt = $pdo->query($query);

$SERVICE_leagues = array();

while ($row = $stmt->fetch()) {
  $SERVICE_leagues[] = $row["id"];
}

unset($query);
unset($stmt);
unset($row);
