<?php
function error($code, $msg) {
  http_response_code($code);
  die( json_encode( array("error" => $msg) ) );
}

function get_data($pdo, $league, $category) {
  $query = "SELECT path FROM data_outputFiles WHERE league = ? AND category = ? LIMIT 1";

  $stmt = $pdo->prepare($query);
  $stmt->execute([$league, $category]);

  //return $stmt->fetch();
  return $stmt;
}

// Define content type
header("Content-Type: application/json");

// Check parameters
if ( !isset($_GET["league"]) )    error(400, "Invalid league param");
if ( !isset($_GET["category"]) )  error(400, "Invalid category param");

// Connect to database
include_once ( "details/pdo.php" );

// Get file path from table using provided parameters
$stmt = get_data($pdo, $_GET["league"], $_GET["category"]);
// If no results with provided params
if ($stmt->rowCount() === 0) error(400, "Invalid parameters");
// Get the path string
$path = $stmt->fetch()["path"];

// Display generated data
echo file_get_contents($path);
