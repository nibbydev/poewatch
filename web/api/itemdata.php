<?php
header("Content-Type: application/json");
include_once ( "../details/pdo.php" );

$query = "SELECT 
  did.id, did.name, did.type, did.frame, 
  did.tier, did.lvl, did.quality, did.corrupted, 
  did.links, did.var, did.icon, 
  cp.name AS cp, cc.name AS cc 
FROM data_itemData AS did 
LEFT JOIN category_parent AS cp ON cp.id = did.id_cp 
LEFT JOIN category_child AS cc ON cc.id = did.id_cc";

$stmt = $pdo->query($query);

$payload = array();

while ($row = $stmt->fetch()) {
  $payload[] = $row;
}

echo json_encode($payload);