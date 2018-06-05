<?php
header("Content-Type: application/json");
include_once ( "details/pdo.php" );

$query = "SELECT `changeid` as `id`, `time` FROM `changeid`";
$stmt = $pdo->query($query);
$row = $stmt->fetch();

echo json_encode($row);