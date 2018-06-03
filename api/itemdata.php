<?php
header("Content-Type: application/json");
include_once ( "details/db_connect.php" );

$query = "SELECT
  p.`sup`, b.`sub`,
  p.`parent`, p.`child`,
  p.`name`, p.`type`,
  p.`frame`, b.`icon`,
  p.`key` AS 'generic_key', b.`key` AS 'specific_key',
  b.`var`, b.`tier`, b.`lvl`, b.`quality`, b.`corrupted`, b.`links` 
FROM `item_data_sub` AS b 
JOIN `item_data_sup` AS p 
  ON b.`sup` = p.`sup`";

$result = mysqli_query($conn, $query);

$rows = array();

while ($row = mysqli_fetch_assoc($result)) {
  $rows[] = $row;
}

echo json_encode($rows);