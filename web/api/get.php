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

  if ( strlen($_GET["league"]) > 64 )    {
    error(400, "League too long");
  }

  if ( strlen($_GET["league"]) < 3 )    {
    error(400, "League too short");
  }

  if ( strlen($_GET["category"]) > 32 )    {
    error(400, "Category too long");
  }

  if ( strlen($_GET["category"]) < 3 )    {
    error(400, "Category too short");
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

function get_data($pdo) {
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
  $stmt->execute([$_GET["league"], $_GET["category"]]);

  return $stmt;
}

function parse_data($stmt, $active) {
  $payload = [];

  // Loop though all items
  while ($row = $stmt->fetch()) {
    $itemData = [
      'id'              => (int)  $row['id_d'],
      'name'            =>        $row['name'],
      'type'            =>        $row['type'],
      'category'        =>        $row['category'],
      'group'           =>        $row['group'],
      'frame'           => (int)  $row['frame'],

      'mapSeries'       =>        $row['series']     === null ? null : (int)    $row['series'],
      'mapTier'         =>        $row['tier']       === null ? null : (int)    $row['tier'],
      'baseIsShaper'    =>        $row['shaper']     === null ? null : (bool)   $row['shaper'],
      'baseIsElder'     =>        $row['elder']      === null ? null : (bool)   $row['elder'],
      'baseItemLevel'   =>        $row['ilvl']       === null ? null : (int)    $row['ilvl'],
      'gemLevel'        =>        $row['lvl']        === null ? null : (int)    $row['lvl'],
      'gemQuality'      =>        $row['quality']    === null ? null : (int)    $row['quality'],
      'gemIsCorrupted'  =>        $row['corrupted']  === null ? null : (bool)   $row['corrupted'],
      'enchantMin'      =>        $row['enchantMin'] === null ? null : (float)  $row['enchantMin'],
      'enchantMax'      =>        $row['enchantMax'] === null ? null : (float)  $row['enchantMax'],
      'stackSize'       =>        $row['stack']      === null ? null : (int)    $row['stack'],
      'linkCount'       =>        $row['links']      === null ? null : (int)    $row['links'],

      'variation'       =>        $row['var'],
      'icon'            =>        $row['icon']
    ];

    // Filter out null values
    $itemData = array_filter($itemData, function($value) {
      return $value !== null;
    });

    $priceData = [
      'mean'            => (float)  $row['mean'],
      'median'          => (float)  $row['median'],
      'mode'            => (float)  $row['mode'],
      'min'             => (float)  $row['min'],
      'max'             => (float)  $row['max'],
      'exalted'         => (float)  $row['exalted'],
      'total'           => (int)    $row['total'],
      'daily'           => (int)    $row['daily'],
      'current'         => (int)    $row['current'],
      'accepted'        => (int)    $row['accepted'],
      'change'          =>          0.0,
      'history'         =>          null
    ];

    if ($active) {
      // If there were history entries
      if ( is_null($row['history']) ) {
        $priceData['history'] = [null, null, null, null, null, null, $priceData['mean']];
      } else {
        // Convert CSV to array
        $history = array_map('doubleval', array_reverse(explode(',', $row['history'])));
        array_push($history, $priceData['mean']);

        // Calculate change %
        if ($priceData['mean']) {
          $priceData['change'] = round((1 - $history[0] / $priceData['mean']) * 100, 2);
        }

        // Pad missing fields with null
        $priceData['history'] = array_pad($history, -7, null);
      }
    }

    // Append row to payload
    $payload[] = array_merge($itemData, $priceData);
  }

  return $payload;
}

header("Content-Type: application/json");
check_errors();
include_once ( "../details/pdo.php" );

// Get league id and active state
$state = check_league($pdo, $_GET["league"]);
if ($state === null) {
  error(400, "Invalid league");
}

// Get database entries based on league state
$data = parse_data(get_data($pdo), $state["active"]);
echo json_encode($data, JSON_PRESERVE_ZERO_FRACTION | JSON_PRETTY_PRINT);
