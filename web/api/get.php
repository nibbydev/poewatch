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

function get_data($pdo, $league, $category) {
  $query = "SELECT 
    i.id_d, i.mean, i.exalted, 
    i.quantity + i.inc AS quantity, 
    did.name, did.type, did.frame, 
    did.tier, did.lvl, did.quality, did.corrupted, 
    did.links, did.ilvl, did.var, did.icon, 
    cc.name AS ccName,
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

function parse_data($stmt) {
  $payload = array();

  while ($row = $stmt->fetch()) {
    // Form a temporary row array
    $tmp = array(
      'id'            => (int)  $row['id_d'],
      'name'          =>        $row['name'],
      'type'          =>        $row['type'],
      'frame'         => (int)  $row['frame'],

      'mean'          =>        $row['mean']      === NULL ?  0.0 : (float) $row['mean'],
      'exalted'       =>        $row['exalted']   === NULL ?  0.0 : (float) $row['exalted'],
      'quantity'      =>        $row['quantity']  === NULL ?    0 :   (int) $row['quantity'],
      'spark'         =>        array(),
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

    // Convert CSV to array
    $history = explode(',', $row['history']);
    // Add current value to spark
    //$history[] = $tmp['mean'];

    // If there were history entries
    if ( !is_null($row['history']) ) {
      // Find total change
      $tmp['change'] = round((1 - ($history[0] / $history[sizeof($history) - 1])) * 100, 4);

      $firstPrice = $history[0];

      // Calculate each entry's change %-relation to current price
      for ($i = 0; $i < sizeof($history); $i++) { 
        $history[$i] = round((1 - ($firstPrice / $history[$i])) * 100, 4);
      }
    }

    // Pad missing fields with null
    $tmp['spark'] = array_pad($history, -7, null);

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

// Get database entries
$stmt = get_data($pdo, $_GET["league"], $_GET["category"]);
// If no results with provided id
if ($stmt->rowCount() === 0) {
  error(400, "No results");
}

$data = parse_data($stmt);

// Display generated data
echo json_encode($data, JSON_PRESERVE_ZERO_FRACTION | JSON_PRETTY_PRINT);
