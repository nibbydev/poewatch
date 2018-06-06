<?php
include_once ( "details/pdo.php" );

$queryParent = "SELECT * FROM `category_parent`";
$queryChild = "SELECT * FROM `category_child`";

$stmtParent = $pdo->query($queryParent);
$stmtChild = $pdo->query($queryChild);

$SERVICE_categories = array();

while ($row = $stmtParent->fetch()) {
  $SERVICE_categories[] = array(
    "name" => $row["parent"],
    "display" => $row["display"],
    "members" => array()
  );
}

while ($row = $stmtChild->fetch()) {
  for ($i=0; $i < count($SERVICE_categories); $i++) { 
    if ($SERVICE_categories[$i]["name"] === $row["parent"]) {
      $SERVICE_categories[$i]["members"][] = array(
        "name" => $row["child"],
        "display" => $row["display"]
      );

      break;
    }
  }
}
