<?php
function getTimeSinceLastRequest($pdo) {
  $query = "SELECT timediff(now(), time) as diff from data_change_id";

  $stmt = $pdo->query($query);
  return $stmt->fetch()["diff"];
}

function getStats($pdo, $types) {
  $query = "SELECT * from (
      select DATE_FORMAT(time, '%Y-%m-%dT%TZ') as time, value
      from data_statistics
      where type = ? and time >= ?
      order by time desc
    ) foo
    order by foo.time asc
  ";

  $buffer = array();
  $payload = array(
    "types" => $types,
    "labels" => array(),
    "series" => array()
  );

  $end    = (new DateTime())->modify("+1 hour");
  $start  = (new DateTime())->modify("-128 hour");
  $period = new DatePeriod($start, new DateInterval('PT1H'), $end);

  foreach ($period as $day) {
    $day = $day->format('Y-m-d\TH:00:00\Z');
    $buffer[$day] = array();
    $payload["labels"][] = $day;
  }

  foreach ($types as $type) {
    $stmt = $pdo->prepare($query);
    $stmt->execute([$type, $start->format('Y-m-d H:00:00')]);

    while ($row = $stmt->fetch()) {
      $buffer[$row["time"]][$type] = $row["value"];
    }
  }

  foreach ($types as $type) {
    $typeValues = array();
    
    foreach ($buffer as $day => $values) {
      if (key_exists($type, $values)) {
        $typeValues[] = $values[$type];
      } else {
        $typeValues[] = null;
      }
    }
    
    $payload["series"][] = $typeValues;
  }

  return $payload;
}

// Define content type
header("Content-Type: application/json");

$types = array(
  "time" => array(
    "TIME_API_REPLY_DOWNLOAD",
    "TIME_PARSE_REPLY",
    "TIME_API_TTFB"
  ),

  "count" => array(
    "COUNT_API_CALLS",
    "COUNT_REPLY_SIZE",
    "COUNT_TOTAL_STASHES",
    "COUNT_TOTAL_ITEMS",
    "COUNT_ACCEPTED_ITEMS",
    "COUNT_ACTIVE_ACCOUNTS"
  ),

  "error" => array(
    "COUNT_API_ERRORS_READ_TIMEOUT",
    "COUNT_API_ERRORS_CONNECT_TIMEOUT",
    "COUNT_API_ERRORS_CONNECTION_RESET",
    "COUNT_API_ERRORS_5XX",
    "COUNT_API_ERRORS_429",
    "COUNT_API_ERRORS_DUPLICATE"
  ),
);

if (!isset($_GET["type"]) || !array_key_exists($_GET["type"], $types)) {
  die("ya cheeky lil wanker");
}

include_once ( "../details/pdo.php" );
$payload = getStats($pdo, $types[$_GET["type"]]);
$payload["query"] = getTimeSinceLastRequest($pdo);
echo json_encode($payload);
