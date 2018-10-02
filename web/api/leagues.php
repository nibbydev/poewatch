<?php
function get_data($pdo) {
  $query = "SELECT *,
    TIMESTAMPDIFF(SECOND, start, NOW()) AS elapseDiff,
    TIMESTAMPDIFF(SECOND, NOW(), end) AS remainDiff
  FROM data_leagues 
  WHERE active = 1";

  $stmt = $pdo->query($query);

  return $stmt;
}

function parse_data($stmt) {
  $payload = array();

  while ($row = $stmt->fetch()) {
    $tmp = array(
      'id'        => (int)  $row['id'],
      'name'      =>        $row['name'],
      'display'   =>        $row['display'],
      'hardcore'  => (bool) $row['hardcore'],
      'upcoming'  => (bool) $row['upcoming'],
      'active'    => (bool) $row['active'],
      'event'     => (bool) $row['event'],
      'start'     =>        $row['start'],
      'end'       =>        $row['end'],
      'duration'  => array(
        'total'  => $row['remainDiff'] + $row['elapseDiff'],
        'elapse' => $row['elapseDiff'],
        'remain' => $row['remainDiff']
      )
    );

    $payload[] = $tmp;
  }

  return $payload;
}

function parse_history_data($stmt) {
  $payload = array();

  while ($row = $stmt->fetch()) {
    // Form a temporary entry array
    $tmp = array(
      'league'      => array(
        'id'        => (int)  $row['leagueId'],
        'name'      =>        $row['leagueName'],
        'display'   =>        $row['leagueDisplay'],
        'active'    => (bool) $row['leagueActive'],
        'event'     => (bool) $row['leagueEvent'],
        'start'     =>        $row['leagueStart'],
        'end'       =>        $row['leagueEnd']
      ),
      'mean'      =>          $row['mean']      === NULL ? null : (float) $row['mean'],
      'median'    =>          $row['median']    === NULL ? null : (float) $row['median'],
      'mode'      =>          $row['mode']      === NULL ? null : (float) $row['mode'],
      'exalted'   =>          $row['exalted']   === NULL ? null : (float) $row['exalted'],
      'count'     =>          $row['count']     === NULL ? null :   (int) $row['count'],
      'quantity'  =>          $row['quantity']  === NULL ?    0 :   (int) $row['quantity'],
      'history'   => array()
    );

    if (!is_null($row['mean_list'])) {
      // Convert CSVs to arrays
      $means    = explode(',', $row['mean_list']);
      $medians  = explode(',', $row['median_list']);
      $modes    = explode(',', $row['mode_list']);
      $quants   = explode(',', $row['quantity_list']);
      $times    = explode(',', $row['time_list']);

      for ($i = 0; $i < sizeof($means); $i++) { 
        $tmp['history'][] = array(
          'time'     =>         $times[$i],
          'mean'     => (float) $means[$i],
          'median'   => (float) $medians[$i],
          'mode'     => (float) $modes[$i],
          'quantity' => (int)   $quants[$i],
        );
      }
    }

    $payload[] = $tmp;
  }

  return $payload;
}

// Define content type
header('Content-Type: application/json');

// Connect to database
include_once ( '../details/pdo.php' );

// Get data from database
$stmt = get_data($pdo);

// Parse received data
$payload = parse_data($stmt);

// Display generated data
echo json_encode($payload);
