<?php
header("Content-Type: application/json");
include_once ( "details/db_connect.php" );

$queryParent = "SELECT * FROM `category_parent`";
$queryChild = "SELECT * FROM `category_child`";

$resultParent = mysqli_query($conn, $queryParent);
$resultChild = mysqli_query($conn, $queryChild);

$rows = array();

while ($row = mysqli_fetch_assoc($resultParent)) {
  $rows[] = array(
    "name" => $row["parent"],
    "display" => $row["display"],
    "members" => array()
  );
}

while ($row = mysqli_fetch_assoc($resultChild)) {
  for ($i=0; $i < count($rows); $i++) { 
    if ($rows[$i]["name"] === $row["parent"]) {
      $rows[$i]["members"][] = array(
        "name" => $row["child"],
        "display" => $row["display"]
      );

      break;
    }
  }
}

echo json_encode($rows);