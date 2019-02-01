<?php
function error($code, $msg) {
  http_response_code($code);
  die( json_encode( array("error" => $msg) ) );
}

function check_errors() {
  if (!isset($_GET["id"])) {
    error(400, "Missing id");
  }

  if (!ctype_digit($_GET["id"])) {
    error(400, "Invalid id");
  }
}

function get_league_data($pdo, $id) {
  $query = "SELECT 
    i.mean, i.median, i.mode, i.min, i.max, 
    i.exalted, i.total, i.daily, i.current, i.accepted,
    l.id        AS leagueId,
    l.active    AS leagueActive, 
    l.upcoming  AS leagueUpcoming, 
    l.event     AS leagueEvent, 
    l.hardcore  AS leagueHardcore, 
    l.name      AS leagueName, 
    l.display   AS leagueDisplay, 
    l.start     AS leagueStart,
    l.end       AS leagueEnd
  FROM league_items AS i
  JOIN data_leagues AS l
    ON l.id = i.id_l
  WHERE i.id_d = ?
  ORDER BY l.active DESC, l.id DESC";

  $stmt = $pdo->prepare($query);
  $stmt->execute([$id]);

  return $stmt;
}

function get_history_entries($pdo, $leagueId, $itemId) {
  $query = "SELECT * from (
    SELECT 
      mean, median, mode, daily, current, accepted,
      DATE_FORMAT(time, '%Y-%m-%dT%H:00:00Z') as `time`
    FROM league_history_daily
    WHERE id_l = ? AND id_d = ?
    ORDER BY `time` DESC
    LIMIT 120
  ) as foo ORDER BY foo.`time` ASC";

  $stmt = $pdo->prepare($query);
  $stmt->execute([$leagueId, $itemId]);

  return $stmt;
}

function get_item_data($pdo, $id) {
  $query = "
  SELECT 
    d.name, d.type, d.frame, d.icon,
    d.tier, d.lvl, d.quality, d.corrupted, 
    d.links, d.ilvl, d.var AS variation,
    dc.name AS category, dg.name AS `group`
  FROM      data_itemData   AS d
  LEFT JOIN data_categories AS dc ON d.id_cat = dc.id 
  LEFT JOIN data_groups     AS dg ON d.id_grp = dg.id 
  WHERE     d.id = ?
  LIMIT     1
  ";

  $stmt = $pdo->prepare($query);
  $stmt->execute([$id]);

  return $stmt;
}

function build_history_payload($pdo, $id) {
  $payload = array();

  $stmt = get_league_data($pdo, $id);
  while ($row = $stmt->fetch()) {
    $tmp = array(
      'league'        => array(
        'id'          => (int)  $row['leagueId'],
        'active'      => (bool) $row['leagueActive'],
        'upcoming'    => (bool) $row['leagueUpcoming'],
        'event'       => (bool) $row['leagueEvent'],
        'hardcore'    => (bool) $row['leagueHardcore'],
        'name'        =>        $row['leagueName'],
        'display'     =>        $row['leagueDisplay'],
        'start'       =>        $row['leagueStart'],
        'end'         =>        $row['leagueEnd']
      ),
      'mean'      => (float) $row['mean'],
      'median'    => (float) $row['median'],
      'mode'      => (float) $row['mode'],
      'min'       => (float) $row['min'],
      'max'       => (float) $row['max'],
      'exalted'   => (float) $row['exalted'],
      'total'     => (int) $row['total'],
      'daily'     => (int) $row['daily'],
      'current'   => (int) $row['current'],
      'accepted'  => (int) $row['accepted'],
      'history'   => array()
    );

    $historyStmt = get_history_entries($pdo, $row['leagueId'], $id);
    while ($historyRow = $historyStmt->fetch()) {
      $tmp['history'][] = array(
        'time'     =>         $historyRow["time"],
        'mean'     => (float) $historyRow["mean"],
        'median'   => (float) $historyRow["median"],
        'mode'     => (float) $historyRow["mode"],
        'daily'    => (int)   $historyRow["daily"],
        'current'  => (int)   $historyRow["current"],
        'accepted' => (int)   $historyRow["accepted"],
      );
    }

    $payload[] = $tmp;
  }

  return $payload;
}

function build_payload($pdo, $id) {
  // Get item's name, frame, icon, etc.
  $itemDataStmt = get_item_data($pdo, $id);

  // If there is no item with the provided id
  if ($itemDataStmt->rowCount() === 0) {
    error(400, "Invalid id");
  }

  // Get the one item data row
  $itemData = $itemDataStmt->fetch();

  // Get prices on a per-league basis
  $historyData = build_history_payload($pdo, $id);

  // Form payload with predefined fields
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
    'category'  => $itemData['category'],
    'group'     => $itemData['group'],
    'data'      => $historyData
  );

  return $payload;
}

// Define content type
header("Content-Type: application/json");

check_errors();

include_once ( "../details/pdo.php" );

// Form the payload
$payload = build_payload($pdo, $_GET["id"]);

// Display generated data
echo json_encode($payload, JSON_PRESERVE_ZERO_FRACTION);
