<?php
header("Content-Type: application/json");
include_once ( "details/pdo.php" );

$queryParent = "SELECT * FROM `category_parent`";
$queryChild = "SELECT * FROM `category_child`";

$stmtParent = $pdo->query($queryParent);
$stmtChild = $pdo->query($queryChild);

$payload = array();

while ($row = $stmtParent->fetch()) {
  $payload[] = array(
    "name" => $row["parent"],
    "display" => $row["display"],
    "members" => array()
  );
}

while ($row = $stmtChild->fetch()) {
  for ($i=0; $i < count($payload); $i++) { 
    if ($payload[$i]["name"] === $row["parent"]) {
      $payload[$i]["members"][] = array(
        "name" => $row["child"],
        "display" => $row["display"]
      );

      break;
    }
  }
}

echo json_encode($payload);