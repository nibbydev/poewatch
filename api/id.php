<?php
header("Content-Type: application/json");
include_once ( "details/pdo.php" );

$query = "SELECT `change_id` as `id`, `time` FROM `sys-change_id`";
$stmt = $pdo->query($query);
$row = $stmt->fetch();

echo json_encode($row);
