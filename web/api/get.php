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
    SELECT DISTINCT id_l FROM league_items
  ) AS leagues ON l.id = leagues.id_l
  WHERE l.name = ?
  LIMIT 1";

  $stmt = $pdo->prepare($query);
  $stmt->execute([$league]);

  return $stmt->rowCount() === 0 ? null : $stmt->fetch();
}

function get_data($pdo, $league, $category) {
  $query = "SELECT 
    i.id_d, i.mean, i.median, i.mode, i.min, i.max, i.exalted, 
    i.total, i.daily, i.current, i.accepted,
    did.name, did.type, did.frame, did.tier, did.series,
    did.shaper, did.elder, did.enchantMin, did.enchantMax,
    did.lvl, did.quality, did.corrupted, did.stack, 
    did.links, did.ilvl, did.var, did.icon, 
    dc.name AS category, dg.name AS `group`,
    i.spark AS history
  FROM      league_items AS i 
  JOIN      data_itemData AS did 
    ON      i.id_d = did.id 
  JOIN      data_leagues AS l 
    ON      l.id = i.id_l 
  JOIN      data_categories AS dc
    ON      did.id_cat = dc.id 
  LEFT JOIN data_groups AS dg 
    ON      did.id_grp = dg.id 
  WHERE     l.name   = ?
    AND     dc.name  = ?
  ORDER BY  i.mean DESC";

  $stmt = $pdo->prepare($query);
  $stmt->execute([$league, $category]);

  return $stmt;
}

function parse_data($stmt, $active) {
  $payload = array();

  while ($row = $stmt->fetch()) {
    // Form a temporary row array
    $tmp = array(
      'id'            => (int)   $row['id_d'],
      'name'          =>         $row['name'],
      'type'          =>         $row['type'],
      'category'      =>         $row['category'],
      'group'         =>         $row['group'],
      'frame'         => (int)   $row['frame'],

      'mean'          => (float) $row['mean'],
      'median'        => (float) $row['median'],
      'mode'          => (float) $row['mode'],
      'min'           => (float) $row['min'],
      'max'           => (float) $row['max'],
      'exalted'       => (float) $row['exalted'],

      'total'         => (int)   $row['total'],
      'daily'         => (int)   $row['daily'],
      'current'       => (int)   $row['current'],
      'accepted'      => (int)   $row['accepted'],
      'history'       =>         null,

      'base'          =>         null,
      'enchant'       =>         null,
      'gem'           =>         null,
      'map'           =>         null,

      'stack'         =>         $row['stack']     === NULL ? null :   (int) $row['stack'],
      'links'         =>         $row['links']     === NULL ? null :   (int) $row['links'],
      'variation'     =>         $row['var'],
      'icon'          =>         $row['icon']
    );
    
    if ($row["category"] === "map") {
      $tmp['map'] = array(
        "series" => $row['series'] === null ? null : (int) $row['series'],
        "tier" => (int) $row['tier']
      );
    }

    if ($row["category"] === "base") {
      $tmp['base'] = array(
        "shaper" => (bool) $row['shaper'],
        "elder" => (bool) $row['elder'],
        "itemLevel" => $row['ilvl'] === null ? null : (int) $row['ilvl']
      );
    }

    if ($row["category"] === "gem") {
      $tmp['gem'] = array(
        "level" => (int) $row['lvl'],
        "quality" => (int) $row['quality'],
        "corrupted" => (bool) $row['corrupted']
      );
    }

    if ($row["category"] === "enchantment") {
      $tmp['enchant'] = array(
        "min" => $row['enchantMin'] === null ? null : (float) $row['enchantMin'],
        "max" => $row['enchantMax'] === null ? null : (float) $row['enchantMax']
      );
    }

    if ($active) {
      // If there were history entries
      if ( is_null($row['history']) ) {
        $tmp['history'] = array(null, null, null, null, null, null, $tmp['mean']);
      } else {
        // Convert CSV to array
        $history = array_map('doubleval', array_reverse(explode(',', $row['history'])));

        array_push($history, $tmp['mean']);

        // Pad missing fields with null
        $tmp['history'] = array_pad($history, -7, null);
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
$stmt = get_data($pdo, $_GET["league"], $_GET["category"]);
$data = parse_data($stmt, $state["active"]);

// Display generated data
echo json_encode($data, JSON_PRESERVE_ZERO_FRACTION);
