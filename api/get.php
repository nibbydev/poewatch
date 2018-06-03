<?php
function get_param_league() {
  if ( !isset($_GET["league"]) ) {
    die("{\"error\": \"Invalid params\", \"field\": \"league\"}");
  }

  $league = strtolower(trim(preg_replace("/[^A-Za-z0-9-]/", "", $_GET["league"])));

  if (!$league || strlen($league) <  3) {
    die("{\"error\": \"Invalid params\", \"field\": \"league\"}");
  }

  return $league;
}

function get_param_category() {
  if ( !isset($_GET["category"]) ) {
    die("{\"error\": \"Invalid params\", \"field\": \"category\"}");
  }

  $category = strtolower(trim(preg_replace("/[^A-Za-z]/", "", $_GET["category"])));

  if (!$category || strlen($category) < 3) {
    die("{\"error\": \"Invalid params\", \"field\": \"category\"}");
  }

  return $category;
}

header("Content-Type: application/json");

$league = get_param_league();
$category = get_param_category();

include_once ( "details/pdo.php" );

$stmt = $pdo->prepare("SELECT `path` FROM `output_files` WHERE `league`=? AND `category`=?");
$stmt->execute([$league, $category]);
$row = $stmt->fetch();

if ($row && file_exists($row["path"]) ) {

  $data = file_get_contents( $row["path"] );
  if ($data === false) echo "{\"error\": \"An error occurred\"}";
  else echo $data;

} else {
  echo "{\"error\": \"Invalid params\"}";
}
