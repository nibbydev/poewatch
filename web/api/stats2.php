<?php
function getStats($pdo) {
  $query = "SELECT type, DATE_FORMAT(time, '%Y-%m-%dT%TZ') as time, value from data_statistics where time > date_sub(now(), interval 128 hour)";
  $stmt = $pdo->query($query);

  $payload = array();

  while ($row = $stmt->fetch()) {
    $payload[] = array(
      'type'   =>        $row['type'],
      'time'   =>        $row['time'],
      'value'  => (int)  $row['value']
    );
  }

  return $payload;
}

header("Content-Type: application/json");
include_once ( "../details/pdo.php" );
$payload = getStats($pdo);

echo json_encode($payload);
