<?php
function error($code, $msg) {
  http_response_code($code);
  die( json_encode( array("error" => $msg) ) );
}

function check_errors() {
  if ( !isset($_GET["league"]) )    {
    error(400, "Missing league param");
  }

  if ( !isset($_GET["category"]) )  {
    error(400, "Missing category param");
  }
}

function check_league($pdo, $league) {
  $query = "SELECT l.id, l.active
  FROM data_leagues AS l
  JOIN (
    SELECT DISTINCT id_l FROM league_history_daily_rolling
    UNION  DISTINCT
    SELECT DISTINCT id_l FROM league_history_daily_inactive
  ) AS leagues ON l.id = leagues.id_l
  WHERE l.name = ?
  LIMIT 1";

  $stmt = $pdo->prepare($query);
  $stmt->execute([$league]);

  return $stmt->rowCount() === 0 ? null : $stmt->fetch();
}

function get_data($pdo, $league, $category) {
  $query = "SELECT 
    i.id_d, i.mean, i.exalted, 
    i.quantity + i.inc AS quantity, 
    did.name, did.type, did.frame, 
    did.tier, did.lvl, did.quality, did.corrupted, 
    did.links, did.ilvl, did.var, did.icon, 
    cc.name AS category,
    SUBSTRING_INDEX(GROUP_CONCAT(lhdr.mean ORDER BY lhdr.time ASC SEPARATOR ','), ',', 7) AS history
  FROM      league_items                  AS i 
  JOIN      data_itemData                 AS did 
    ON      i.id_d = did.id 
  JOIN      data_leagues                  AS l 
    ON      l.id = i.id_l 
  JOIN      category_parent               AS cp 
    ON      did.id_cp = cp.id 
  JOIN      league_history_daily_rolling  AS lhdr 
    ON      lhdr.id_d = i.id_d 
      AND   lhdr.id_l = l.id
  LEFT JOIN category_child                AS cc 
    ON      did.id_cc = cc.id 
  WHERE     l.name   = ?
    AND     cp.name  = ?
    AND     l.active = 1 
    AND     i.count  > 1 
  GROUP BY  i.id_d, i.mean, i.exalted, i.quantity, i.inc
  ORDER BY  i.mean DESC";

  $stmt = $pdo->prepare($query);
  $stmt->execute([$league, $category]);

  return $stmt;
}

function get_data2($pdo, $league, $category) {
  $query = "SELECT 
    lhdi.id_d, lhdi.mean, lhdi.exalted, lhdi.count AS quantity, 
    did.name, did.type, did.frame, 
    did.tier, did.lvl, did.quality, did.corrupted, 
    did.links, did.ilvl, did.var, did.icon, 
    cc.name AS category,
    NULL AS history
  FROM league_history_daily_inactive AS lhdi
  JOIN data_itemData AS did ON lhdi.id_d = did.id 
  LEFT JOIN category_child AS cc ON did.id_cc = cc.id 
  WHERE lhdi.id_l = (SELECT id FROM data_leagues WHERE name = ?)
  AND did.id_cp = (SELECT id FROM category_parent WHERE name = ?)
  AND lhdi.time = (
    SELECT time FROM league_history_daily_inactive
    WHERE id_l = (SELECT id FROM data_leagues WHERE name = ?)
    ORDER BY time DESC
    LIMIT 1) 
  ORDER BY lhdi.mean DESC";

  $stmt = $pdo->prepare($query);
  $stmt->execute([$league, $category, $league]);

  return $stmt;
}

function parse_data($stmt) {
  $payload = array();

  while ($row = $stmt->fetch()) {
    // Form a temporary row array
    $tmp = array(
      'id'            => (int)  $row['id_d'],
      'name'          =>        $row['name'],
      'type'          =>        $row['type'],
      'category'      =>        $row['category'],
      'frame'         => (int)  $row['frame'],

      'mean'          =>        $row['mean']      === NULL ?  0.0 : (float) $row['mean'],
      'exalted'       =>        $row['exalted']   === NULL ?  0.0 : (float) $row['exalted'],
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

    // If there were history entries
    if ( !is_null($row['history']) ) {
      // Convert CSV to array
      $history = explode(',', $row['history']);

      // Find total change
      $tmp['change'] = round((1 - ($history[0] / $history[sizeof($history) - 1])) * 100, 4);

      $firstPrice = $history[0];

      // Calculate each entry's change %-relation to current price
      for ($i = 0; $i < sizeof($history); $i++) { 
        $history[$i] = round((1 - ($firstPrice / $history[$i])) * 100, 4);
      }

      // Pad missing fields with null
      $tmp['spark'] = array_pad($history, -7, null);
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
include_once ( "details/pdo.php" );

// Get league id and active state
$state = check_league($pdo, $_GET["league"]);
if ($state === null) {
  error(400, "Invalid league param");
}

// Get database entries based on league state
if ($state["active"]) {
  $stmt = get_data($pdo, $_GET["league"], $_GET["category"]);
} else {
  $stmt = get_data2($pdo, $_GET["league"], $_GET["category"]);
}

// If no results with provided id
if ($stmt->rowCount() === 0) {
  error(400, "No results");
}

$data = parse_data($stmt);

// Display generated data
echo json_encode($data, JSON_PRESERVE_ZERO_FRACTION | JSON_PRETTY_PRINT);
