<?php
function getStats($pdo, $types) {
  $query = "
    select DATE_FORMAT(time, '%Y-%m-%dT%TZ') as time, value
    from data_statistics
    where statType = ?
    order by time asc
  ";

  $payload = array(
    "labels" => array(),
    "series" => array(),
    "types" => $types
  );
  
  foreach ($types as $type) {
    $stmt = $pdo->prepare($query);
    $stmt->execute([$type]);

    $times = array();
    $vals = array();

    while ($row = $stmt->fetch()) {
      $times[] = $row["time"];
      $vals[] = $row["value"];
    }

    if (sizeof($payload["labels"]) < sizeof($times)) {
      $payload["labels"] = $times;
    }
    
    $payload["series"][] = $vals;
  }

  return $payload;
}

// Define content type
header("Content-Type: application/json");

$types = array(
  "m" => array(
    "COUNT_REPLY_SIZE",
    "TIME_CYCLE_TOTAL",
    "TIME_CALC_PRICES",
    "TIME_UPDATE_COUNTERS",
    "TIME_CALC_EXALT",
    "TIME_REPLY_DOWNLOAD",
    "TIME_PARSE_REPLY",
    "TIME_UPLOAD_ACCOUNTS",
    "TIME_RESET_STASHES",
    "TIME_UPLOAD_ENTRIES",
    "TIME_UPLOAD_USERNAMES"
  ),
  "h" => array(
    "TIME_ADD_HOURLY",
    "TIME_CALC_DAILY",
    "TIME_RESET_COUNTERS",
    "TIME_CALC_CURRENT"
  ),
  "d" => array(
    "TIME_REMOVE_ENTRIES",
    "TIME_ADD_DAILY",
    "TIME_CALC_SPARK",
    "TIME_ACCOUNT_CHANGES"
  ),

  "0" => array(
    "COUNT_API_CALLS",
    "COUNT_TOTAL_STASHES",
    "COUNT_TOTAL_ITEMS",
    "COUNT_ACCEPTED_ITEMS",
    "COUNT_ACTIVE_ACCOUNTS"
  ),
);

if (!isset($_GET["type"]) || !array_key_exists($_GET["type"], $types)) {
  die("ya cheeky lil wanker");
}

include_once ( "../details/pdo.php" );
$payload = getStats($pdo, $types[$_GET["type"]]);
echo json_encode($payload);
