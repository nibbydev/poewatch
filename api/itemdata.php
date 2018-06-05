<?php
function get_sup($pdo) {
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

  $stmt = $pdo->query($query);
  return $stmt;
}

header("Content-Type: application/json");
include_once ( "details/pdo.php" );


$stmt = get_sup($pdo);

$payload = array();

while ($row = $stmt->fetch()) {
  $payload[] = $row;
}

echo json_encode($payload);