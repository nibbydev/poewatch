<?php
header("Content-Type: application/json");
include_once ( "details/pdo.php" );

$query = "SELECT
  `idp`.`id` AS 'idp-id',

  `idp`.`name`, `idp`.`type`, `idp`.`frame`, `idp`.`key` AS 'parent-key', 

  `idc`.`id` AS 'idc-id',

  `idc`.`tier`, `idc`.`lvl`, `idc`.`quality`, `idc`.`corrupted`, `idc`.`links`, `idc`.`var`, `idc`.`key` AS 'child-key', `idc`.`icon`,

  `idp`.`id-cp` AS 'cp-id', `idp`.`id-cc` AS 'cc-id'

FROM `itemdata-parent` AS `idp`
LEFT JOIN `itemdata-child` AS `idc` ON `idp`.`id` = `idc`.`id-idp`";

$stmt = $pdo->query($query);

$payload = array();
$tmp = null;

while ($row = $stmt->fetch()) {
  if ($tmp === null) {
    $tmp = array(
      "id" => $row["idp-id"],
      "name" => $row["name"],
      "type" => $row["type"],
      "frame" => $row["frame"],
      "key" => $row["parent-key"],
      "category-parent-id" => $row["cp-id"],
      "category-child-id" => $row["cc-id"],
      "members" => array()
    );
  } else if ($tmp["id"] !== $row["idp-id"]) {
    $payload[] = $tmp;

    $tmp = array(
      "id" => $row["idp-id"],
      "name" => $row["name"],
      "type" => $row["type"],
      "frame" => $row["frame"],
      "key" => $row["parent-key"],
      "category-parent-id" => $row["cp-id"],
      "category-child-id" => $row["cc-id"],
      "members" => array()
    );
  }

  if ($row["idc-id"] !== null) {
    $tmp["members"][] = array(
      "id" => $row["idc-id"],
      "tier" => $row["tier"],
      "lvl" => $row["lvl"],
      "quality" => $row["quality"],
      "corrupted" => $row["corrupted"],
      "links" => $row["links"],
      "var" => $row["var"],
      "key" => $row["child-key"],
      "icon" => $row["icon"]
    );
  }
}

echo json_encode($payload);