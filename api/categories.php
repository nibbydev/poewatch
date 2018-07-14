<?php
header("Content-Type: application/json");
include_once ( "details/pdo.php" );

$query = <<<EOT
SELECT 
  cp.id AS 'cp-id', cp.name AS 'cp-name', cp.display AS 'cp-display',
  cc.id AS 'cc-id', cc.name AS 'cc-name', cc.display AS 'cc-display'
FROM category_parent AS cp
LEFT JOIN category_child AS cc
  ON cp.id = cc.id_cp
ORDER BY cp.id, cc.id ASC
EOT;

$stmt = $pdo->query($query);
$payload = array();

$tmp = null;

while ($row = $stmt->fetch()) {
  if ($tmp === null) {
    $tmp = array(
      "id" => $row["cp-id"],
      "name" => $row["cp-name"],
      "display" => $row["cp-display"],
      "members" => array()
    );
  } else if ($tmp["name"] !== $row["cp-name"]) {
    $payload[] = $tmp;

    $tmp = array(
      "id" => $row["cp-id"],
      "name" => $row["cp-name"],
      "display" => $row["cp-display"],
      "members" => array()
    );
  }

  if ($row["cc-name"] !== null) {
    $tmp["members"][] = array(
      "id" => $row["cc-id"],
      "name" => $row["cc-name"],
      "display" => $row["cc-display"]
    );
  }
}

echo json_encode($payload);