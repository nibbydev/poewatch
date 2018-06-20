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

function get_file_path($pdo, $league, $category) {
  $query = "SELECT `path` FROM `sys-output_files` WHERE `league` = ? AND `category` = ?";

  $stmt = $pdo->prepare($query);
  $stmt->execute([$league, $category]);

  return $stmt->fetch();
}

header("Content-Type: application/json");

include_once ( "details/pdo.php" );

$league = get_param_league();
$category = get_param_category();

$payload = get_file_path($pdo, $league, $category);

if (!$payload || !file_exists($payload["path"])) {
  die ("{\"error\": \"Invalid params\", \"field\": \"category\"}");
}

echo file_get_contents($payload["path"]);
