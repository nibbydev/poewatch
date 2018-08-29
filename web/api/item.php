<?php
function error($code, $msg) {
  http_response_code($code);
  die( json_encode( array("error" => $msg) ) );
}

function get_league_data($pdo, $id) {
  $query = "SELECT 
    l.id      AS leagueId, 
    l.name    AS leagueName, 
    l.display AS leagueDisplay, 
    l.active  AS leagueActive, 
    l.event   AS leagueEvent, 
    DATE_FORMAT(l.start,  '%Y-%m-%d') AS leagueStart,
    DATE_FORMAT(l.end,    '%Y-%m-%d') AS leagueEnd,
    i.mean, i.median, i.mode, i.exalted, i.count, i.quantity,
    GROUP_CONCAT(h.mean      ORDER BY h.time ASC) AS mean_list,
    GROUP_CONCAT(h.median    ORDER BY h.time ASC) AS median_list,
    GROUP_CONCAT(h.mode      ORDER BY h.time ASC) AS mode_list,
    GROUP_CONCAT(h.quantity  ORDER BY h.time ASC) AS quantity_list,
    GROUP_CONCAT(DATE_FORMAT(h.time, '%Y-%m-%d') ORDER BY h.time ASC) AS time_list
  FROM      league_items                 AS i
  JOIN      data_leagues                 AS l ON i.id_l = l.id
  LEFT JOIN league_history_daily_rolling AS h ON h.id_l = l.id AND h.id_d = i.id_d
  WHERE     i.id_d = ?
  GROUP BY  i.id_l, i.id_d
  ORDER BY  l.id DESC";

  $stmt = $pdo->prepare($query);
  $stmt->execute([$id]);

  return $stmt;
}

function get_item_data($pdo, $id) {
  $query = "SELECT 
    d.name, d.type, d.frame, d.icon,
    d.tier, d.lvl, d.quality, d.corrupted, 
    d.links, d.ilvl, d.var AS variation,
    cp.name AS categoryParent, cc.name AS categoryChild
  FROM      data_itemData   AS d
  JOIN      category_parent AS cp ON d.id_cp = cp.id 
  LEFT JOIN category_child  AS cc ON d.id_cc = cc.id 
  WHERE     d.id = ?
  LIMIT     1";

  $stmt = $pdo->prepare($query);
  $stmt->execute([$id]);

  return $stmt;
}

function parse_history_data($stmt) {
  $payload = array();

  while ($row = $stmt->fetch()) {
    // Convert CSVs to arrays
    $means    = explode(',', $row['mean_list']);
    $medians  = explode(',', $row['median_list']);
    $modes    = explode(',', $row['mode_list']);
    $quants   = explode(',', $row['quantity_list']);
    $times    = explode(',', $row['time_list']);

    // Form a temporary entry array
    $tmp = array(
      'leagueId'      => (int)    $row['leagueId'],
      'leagueName'    =>          $row['leagueName'],
      'leagueDisplay' =>          $row['leagueDisplay'],
      'leagueActive'  => (bool)   $row['leagueActive'],
      'leagueEvent'   => (bool)   $row['leagueEvent'],
      'leagueStart'   =>          $row['leagueStart'],
      'leagueEnd'     =>          $row['leagueEnd'],
      'mean'          => (float)  $row['mean'],
      'median'        => (float)  $row['median'],
      'mode'          => (float)  $row['mode'],
      'exalted'       => (float)  $row['exalted'],
      'count'         => (int)    $row['count'],
      'quantity'      => (int)    $row['quantity'],
      'history'       =>          array()
    );

    if (!is_null($row['mean_list'])) {
      // Add null values to counter missing entries from league start
      // Don't run for Hardcore (id 1) nor Standard (id 2)
      if ($tmp['leagueId'] > 2) {
        // Get the two dates as Unix timestamps (ie number of seconds 
        // since January 1 1970 00:00:00 UTC)
        $startDate = strtotime($tmp['leagueStart']);
        $firstDate = strtotime($times[0]);

        // Find the number of days of difference between the two dates
        $dateDiff = ceil( ($firstDate - $startDate) / 60 / 60 / 24);

        // Fill tmp array with null values for the amount of missing days
        for ($i = 0; $i < $dateDiff; $i++) { 
          $stamp = date('Y-m-d', $startDate + $i * 86400);
          $tmp['history'][ $stamp ] = null;
        }
      }

      for ($i = 0; $i < sizeof($means); $i++) { 
        $tmp['history'][ $times[$i] ] = array(
          'mean'     => (float) $means[$i],
          'median'   => (float) $medians[$i],
          'mode'     => (float) $modes[$i],
          'quantity' => (int)   $quants[$i],
        );
      }
    }

  /*
    // Add null values to counter missing entries after latest entry
    // Don't run for Hardcore (id 1) nor Standard (id 2)
    if ($tmp['leagueId'] > 2) {
      // Get the two dates as Unix timestamps (ie number of seconds 
      // since January 1 1970 00:00:00 UTC)
      $endDate = strtotime($tmp['leagueEnd']);
      $lastDate = strtotime(end($times));

      // Find the number of days of difference between the two dates
      $dateDiff = ceil( ($endDate - $lastDate) / 60 / 60 / 24);

      // Fill tmp array with null values for the amount of missing days
      for ($i = 0; $i <= $dateDiff; $i++) { 
        $stamp = date('Y-m-d', $lastDate + $i * 86400);
        $tmp['history'][ $stamp ] = null;
      }
    }
  */

    $payload[] = $tmp;
  }

  return $payload;
}

function form_payload($itemData, $historyData) {
  $payload = $itemData;

  if ($payload['tier']      !== null) $payload['tier']      = (int)   $payload['tier'];
  if ($payload['lvl']       !== null) $payload['lvl']       = (int)   $payload['lvl'];
  if ($payload['quality']   !== null) $payload['quality']   = (int)   $payload['quality'];
  if ($payload['corrupted'] !== null) $payload['corrupted'] = (bool)  $payload['corrupted'];
  if ($payload['links']     !== null) $payload['links']     = (int)   $payload['links'];

  $payload["leagues"] = $historyData;
  return $payload;
}

// Define content type
header("Content-Type: application/json");

// Get parameters
if (!isset($_GET["id"])) error(400, "Missing id parameter");

// Connect to database
include_once ( "details/pdo.php" );

// Get item's name, frame, icon, etc.
$stmt = get_item_data($pdo, $_GET["id"]);
// If no results with provided id
if ($stmt->rowCount() === 0) error(400, "Invalid id parameter");
// Get the one row of item data
$itemData = $stmt->fetch();

// Get league-specific data from database
$stmt = get_league_data($pdo, $_GET["id"]);
// Parse received league-specific data
$historyData = parse_history_data($stmt);

// Form the payload
$payload = form_payload($itemData, $historyData);

// Display generated data
echo json_encode($payload, JSON_PRESERVE_ZERO_FRACTION | JSON_PRETTY_PRINT);
