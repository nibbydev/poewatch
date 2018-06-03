<?php
header("Content-Type: application/json");
include_once ( "details/db_connect.php" );

$query = "SELECT `changeid` as `id`, `time` FROM `changeid`";
$result = mysqli_query($conn, $query);

echo json_encode(mysqli_fetch_assoc($result));