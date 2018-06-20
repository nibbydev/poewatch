<?php
header("Content-Type: application/json");
include_once ( "details/pdo.php" );

$query = "SELECT 
  `cp`.`id` AS 'cp-id', `cp`.`name` AS 'cp-name', `cp`.`display` AS `cp-display`,
  `cc`.`id` AS 'cc-id', `cc`.`name` AS 'cc-name', `cc`.`display` AS `cc-display`
FROM `category-parent` AS `cp`
LEFT JOIN `category-child` AS `cc`
  ON `cp`.`id` = `cc`.`id-cp`
ORDER BY `cp`.`id`, `cc`.`id` ASC";

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