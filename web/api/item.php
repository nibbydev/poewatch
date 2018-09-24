<?php
function error($code, $msg) {
  http_response_code($code);
  die( json_encode( array("error" => $msg) ) );
}

function get_league_data($pdo, $id) {
  $query = "SELECT * FROM (
    SELECT 
      l.id      AS leagueId, 
      l.name    AS leagueName, 
      l.display AS leagueDisplay, 
      l.active  AS leagueActive, 
      l.event   AS leagueEvent, 
      DATE_FORMAT(l.start,  '%Y-%m-%dT%TZ')         AS leagueStart,
      DATE_FORMAT(l.end,    '%Y-%m-%dT%TZ')         AS leagueEnd,
      GROUP_CONCAT(h.mean      ORDER BY h.time ASC) AS mean_list,
      GROUP_CONCAT(h.median    ORDER BY h.time ASC) AS median_list,
      GROUP_CONCAT(h.mode      ORDER BY h.time ASC) AS mode_list,
      GROUP_CONCAT(h.quantity  ORDER BY h.time ASC) AS quantity_list,
      GROUP_CONCAT(DATE_FORMAT(h.time, '%Y-%m-%dT%H:00:00Z') ORDER BY h.time ASC) AS time_list,
      i.mean, i.median, i.mode, i.exalted, i.count, NULL AS quantity
    FROM      league_items_inactive         AS i
    JOIN      data_leagues                  AS l ON i.id_l = l.id
    LEFT JOIN league_history_daily_inactive AS h ON h.id_l = l.id AND h.id_d = i.id_d
    WHERE     i.id_d = ?
    GROUP BY  i.id_l, i.id_d
  
    UNION ALL 
  
    SELECT 
      l.id      AS leagueId, 
      l.name    AS leagueName, 
      l.display AS leagueDisplay, 
      l.active  AS leagueActive, 
      l.event   AS leagueEvent, 
      DATE_FORMAT(l.start,  '%Y-%m-%dT%TZ')         AS leagueStart,
      DATE_FORMAT(l.end,    '%Y-%m-%dT%TZ')         AS leagueEnd,
      GROUP_CONCAT(h.mean      ORDER BY h.time ASC) AS mean_list,
      GROUP_CONCAT(h.median    ORDER BY h.time ASC) AS median_list,
      GROUP_CONCAT(h.mode      ORDER BY h.time ASC) AS mode_list,
      GROUP_CONCAT(h.quantity  ORDER BY h.time ASC) AS quantity_list,
      GROUP_CONCAT(DATE_FORMAT(h.time, '%Y-%m-%dT%H:00:00Z') ORDER BY h.time ASC) AS time_list,
      i.mean, i.median, i.mode, i.exalted, i.count, i.quantity
    FROM      league_items_rolling         AS i
    JOIN      data_leagues                 AS l ON i.id_l = l.id
    LEFT JOIN league_history_daily_rolling AS h ON h.id_l = l.id AND h.id_d = i.id_d
    WHERE     i.id_d = ?
    GROUP BY  i.id_l, i.id_d
  ) AS un
  ORDER BY un.leagueActive DESC, un.leagueId DESC";

  $stmt = $pdo->prepare($query);
  $stmt->execute([$id, $id]);

  return $stmt;
}

function get_item_data($pdo, $id) {
  $query = "SELECT 
    d.name, d.type, d.frame, d.icon,
    d.tier, d.lvl, d.quality, d.corrupted, 
    d.links, d.ilvl, d.var AS variation,
    cc.id AS category
  FROM      data_itemData   AS d
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

function form_payload($itemData, $historyData) {
  $payload = array(
    'name'      => $itemData['name'],
    'type'      => $itemData['type'],
    'frame'     => $itemData['frame'],
    'icon'      => $itemData['icon'],
    'tier'      => $itemData['tier']      === NULL ? null :  (int) $itemData['tier'],
    'lvl'       => $itemData['lvl']       === NULL ? null :  (int) $itemData['lvl'],
    'quality'   => $itemData['quality']   === NULL ? null :  (int) $itemData['quality'],
    'corrupted' => $itemData['corrupted'] === NULL ? null : (bool) $itemData['corrupted'],
    'links'     => $itemData['links']     === NULL ? null :  (int) $itemData['links'],
    'ilvl'      => $itemData['ilvl'],
    'variation' => $itemData['variation'],
    'category'  => (int) $itemData['category'],
    'data' => $historyData
  );

  return $payload;
}

// Define content type
header("Content-Type: application/json");

// Get parameters
if (!isset($_GET["id"])) error(400, "Missing id");

// Connect to database
include_once ( "../details/pdo.php" );

// Get item's name, frame, icon, etc.
$stmt = get_item_data($pdo, $_GET["id"]);
// If no results with provided id
if ($stmt->rowCount() === 0) error(400, "Invalid id");
// Get the one row of item data
$itemData = $stmt->fetch();

// Get league-specific data from database
$stmt = get_league_data($pdo, $_GET["id"]);
// Parse received league-specific data
$historyData = parse_history_data($stmt);

// Form the payload
$payload = form_payload($itemData, $historyData);

// Display generated data
echo json_encode($payload, JSON_PRESERVE_ZERO_FRACTION);
