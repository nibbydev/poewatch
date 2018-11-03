<?php
function error($code, $msg) {
  http_response_code($code);
  die( json_encode( array("error" => $msg) ) );
}

function check_errors() {
  if ( !isset($_GET["league"]) )    {
    error(400, "Missing league");
  }

  if ( !isset($_GET["category"]) )  {
    error(400, "Missing category");
  }
}

function check_league($pdo, $league) {
  $query = "SELECT l.id, l.active
  FROM data_leagues AS l
  JOIN (
    SELECT DISTINCT id_l FROM league_items_rolling
    UNION  DISTINCT
    SELECT DISTINCT id_l FROM league_items_inactive
  ) AS leagues ON l.id = leagues.id_l
  WHERE l.name = ?
  LIMIT 1";

  $stmt = $pdo->prepare($query);
  $stmt->execute([$league]);

  return $stmt->rowCount() === 0 ? null : $stmt->fetch();
}

function get_data_rolling($pdo, $league, $category) {
  $query = "SELECT 
    i.id_d, i.mean, i.median, i.mode, i.min, i.max, i.exalted, 
    i.count, i.quantity + i.inc AS quantity, 
    did.name, did.type, did.frame, 
    did.tier, did.lvl, did.quality, did.corrupted, 
    did.links, did.ilvl, did.var, did.icon, 
    dc.name AS category, dg.name AS `group`,
    i.spark AS history
  FROM      league_items_rolling          AS i 
  JOIN      data_itemData                 AS did 
    ON      i.id_d = did.id 
  JOIN      data_leagues                  AS l 
    ON      l.id = i.id_l 
  JOIN      data_categories               AS dc
    ON      did.id_cat = dc.id 
  LEFT JOIN data_groups                   AS dg 
    ON      did.id_grp = dg.id 
  WHERE     l.name   = ?
    AND     dc.name  = ?
    AND     l.active = 1 
    AND     i.count  > 1 
  ORDER BY  i.mean DESC";

  $stmt = $pdo->prepare($query);
  $stmt->execute([$league, $category]);

  return $stmt;
}

function get_data_inactive($pdo, $league, $category) {
  $query = "SELECT 
    i.id_d, i.mean, i.median, i.mode, i.min, i.max, i.exalted, 
    i.count, i.quantity, 
    did.name, did.type, did.frame, 
    did.tier, did.lvl, did.quality, did.corrupted, 
    did.links, did.ilvl, did.var, did.icon, 
    dc.name AS category, dg.name AS `group`,
    NULL AS history
  FROM      league_items_inactive         AS i 
  JOIN      data_itemData                 AS did 
    ON      i.id_d = did.id 
  JOIN      data_leagues                  AS l 
    ON      l.id = i.id_l 
  JOIN      data_categories               AS dc 
    ON      did.id_cat = dc.id 
  LEFT JOIN data_groups                   AS dg 
    ON      did.id_grp = dg.id 
  WHERE     l.name   = ?
    AND     dc.name  = ?
    AND     i.count  > 1 
  ORDER BY  i.mean DESC";

  $stmt = $pdo->prepare($query);
  $stmt->execute([$league, $category]);

  return $stmt;
}

function get_data_rolling_relic($pdo, $league) {
  $query = "SELECT 
    i.id_d, i.mean, i.median, i.mode, i.min, i.max, i.exalted, 
    i.count, i.quantity + i.inc AS quantity, 
    did.name, did.type, did.frame, 
    did.tier, did.lvl, did.quality, did.corrupted, 
    did.links, did.ilvl, did.var, did.icon, 
    dc.name AS category, dg.name AS `group`,
    i.spark AS history
  FROM      league_items_rolling          AS i 
  JOIN      data_itemData                 AS did 
    ON      i.id_d = did.id 
  JOIN      data_leagues                  AS l 
    ON      l.id = i.id_l 
  JOIN      data_categories               AS dc 
    ON      did.id_cat = dc.id 
  LEFT JOIN data_groups                   AS dg 
    ON      did.id_grp = dg.id 
  WHERE     l.name   = ?
    AND     did.frame = 9
    AND     did.links IS NULL
    AND     l.active = 1 
    AND     i.count  > 1 
  ORDER BY  i.mean DESC";

  $stmt = $pdo->prepare($query);
  $stmt->execute([$league]);

  return $stmt;
}

function get_data_inactive_relic($pdo, $league) {
  $query = "SELECT 
    i.id_d, i.mean, i.median, i.mode, i.min, i.max, i.exalted, 
    i.count, i.quantity, 
    did.name, did.type, did.frame, 
    did.tier, did.lvl, did.quality, did.corrupted, 
    did.links, did.ilvl, did.var, did.icon, 
    dc.name AS category, dg.name AS `group`
    NULL AS history
  FROM      league_items_inactive         AS i 
  JOIN      data_itemData                 AS did 
    ON      i.id_d = did.id 
  JOIN      data_leagues                  AS l 
    ON      l.id = i.id_l 
  JOIN      data_categories               AS dc 
    ON      did.id_cat = dc.id 
  LEFT JOIN data_groups                   AS dg 
    ON      did.id_grp = dg.id 
  WHERE     l.name   = ?
    AND     did.frame = 9
    AND     did.links IS NULL
    AND     i.count  > 1 
  ORDER BY  i.mean DESC";

  $stmt = $pdo->prepare($query);
  $stmt->execute([$league]);

  return $stmt;
}

function parse_data($stmt, $active) {
  $payload = array();

  while ($row = $stmt->fetch()) {
    // Form a temporary row array
    $tmp = array(
      'id'            => (int)  $row['id_d'],
      'name'          =>        $row['name'],
      'type'          =>        $row['type'],
      'category'      =>        $row['category'],
      'group'         =>        $row['group'],
      'frame'         => (int)  $row['frame'],

      'mean'          =>        $row['mean']      === NULL ?  0.0 : (float) $row['mean'],
      'median'        =>        $row['median']    === NULL ?  0.0 : (float) $row['median'],
      'mode'          =>        $row['mode']      === NULL ?  0.0 : (float) $row['mode'],
      'min'           =>        $row['min']       === NULL ?  0.0 : (float) $row['min'],
      'max'           =>        $row['max']       === NULL ?  0.0 : (float) $row['max'],
      'exalted'       =>        $row['exalted']   === NULL ?  0.0 : (float) $row['exalted'],
      
      'count'         =>        $row['count']     === NULL ?    0 :   (int) $row['count'],
      'quantity'      =>        $row['quantity']  === NULL ?    0 :   (int) $row['quantity'],
      'spark'         =>        null,
      'change'        =>        0.0,

      'tier'          =>        $row['tier']      === NULL ? null :   (int) $row['tier'],
      'lvl'           =>        $row['lvl']       === NULL ? null :   (int) $row['lvl'],
      'quality'       =>        $row['quality']   === NULL ? null :   (int) $row['quality'],
      'corrupted'     =>        $row['corrupted'] === NULL ? null :  (bool) $row['corrupted'],
      'links'         =>        $row['links']     === NULL ? null :   (int) $row['links'],
      'ilvl'          =>        $row['ilvl']      === NULL ? null :   (int) $row['ilvl'],
      'var'           =>        $row['var'],
      'icon'          =>        $row['icon']
    );

    if ($active) {
      // If there were history entries
      if ( is_null($row['history']) ) {
        $tmp['spark'] = array(null, null, null, null, null, null, $tmp['mean']);
      } else {
        // Convert CSV to array
        $history = array_reverse(explode(',', $row['history']));
        array_push($history, $tmp['mean']);

        // Find total change
        $lastVal = $history[sizeof($history) - 1];
        if ($lastVal > 0) {
          $tmp['change'] = round((1 - ($history[0] / $history[sizeof($history) - 1])) * 100, 4);
        }

        $firstPrice = $history[0];

        // Calculate each entry's change %-relation to current price
        for ($i = 0; $i < sizeof($history); $i++) { 
          if ($history[$i] > 0) {
            $history[$i] = round((1 - ($firstPrice / $history[$i])) * 100, 4);
          }
        }

        // Pad missing fields with null
        $tmp['spark'] = array_pad($history, -7, null);
      }
    }

    // Append row to payload
    $payload[] = $tmp;
  }

  return $payload;
}

// Define content type
header("Content-Type: application/json");

// Check parameter errors
check_errors();

// Connect to database
include_once ( "../details/pdo.php" );

// Get league id and active state
$state = check_league($pdo, $_GET["league"]);
if ($state === null) {
  error(400, "Invalid league");
}

// Get database entries based on league state
if ($state["active"]) {
  if ($_GET["category"] === "relic") {
    $stmt = get_data_rolling_relic($pdo, $_GET["league"]);
  } else {
    $stmt = get_data_rolling($pdo, $_GET["league"], $_GET["category"]);
  }
} else {
  if ($_GET["category"] === "relic") {
    $stmt = get_data_inactive_relic($pdo, $_GET["league"]);
  } else {
    $stmt = get_data_inactive($pdo, $_GET["league"], $_GET["category"]);
  }
}

// If no results with provided id
if ($stmt->rowCount() === 0) {
  error(400, "No results");
}

$data = parse_data($stmt, $state["active"]);

// Display generated data
echo json_encode($data, JSON_PRESERVE_ZERO_FRACTION);
