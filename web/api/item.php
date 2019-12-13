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

function get_item_data($pdo, $id) {
  $query = "select 
    did.id, did.name, did.type, did.frame, did.stack, 
    did.map_tier, did.map_series, did.shaper, did.elder, did.crusader, did.redeemer, did.hunter, did.warlord, did.base_level, 
    did.enchant_min, did.enchant_max ,did.gem_lvl, did.gem_quality, did.gem_corrupted, 
    did.links, did.var, did.icon, 
    dc.name as category, dg.name as `group`
  from      data_item_data  as did
  left join data_categories as dc on did.id_cat = dc.id 
  left join data_groups     as dg on did.id_grp = dg.id 
  where     did.id = ?
  limit     1";

  $stmt = $pdo->prepare($query);
  $stmt->execute([$id]);

  if ($row = $stmt->fetch()) {
    $payload = [
      'id'              => (int)  $row['id'],
      'name'            =>        $row['name'],
      'type'            =>        $row['type'],
      'category'        =>        $row['category'],
      'group'           =>        $row['group'],
      'frame'           => (int)  $row['frame'],

      'mapSeries'       =>        $row['map_series']    === null ? null : (int)    $row['map_series'],
      'mapTier'         =>        $row['map_tier']      === null ? null : (int)    $row['map_tier'],
      'influences'      =>        [],
      'baseIsShaper'    =>        $row['shaper']        === null ? null : (bool)   $row['shaper'],
      'baseIsElder'     =>        $row['elder']         === null ? null : (bool)   $row['elder'],
      'baseItemLevel'   =>        $row['base_level']    === null ? null : (int)    $row['base_level'],
      'gemLevel'        =>        $row['gem_lvl']       === null ? null : (int)    $row['gem_lvl'],
      'gemQuality'      =>        $row['gem_quality']   === null ? null : (int)    $row['gem_quality'],
      'gemIsCorrupted'  =>        $row['gem_corrupted'] === null ? null : (bool)   $row['gem_corrupted'],
      'enchantMin'      =>        $row['enchant_min']   === null ? null : (float)  $row['enchant_min'],
      'enchantMax'      =>        $row['enchant_max']   === null ? null : (float)  $row['enchant_max'],
      'stackSize'       =>        $row['stack']         === null ? null : (int)    $row['stack'],
      'linkCount'       =>        $row['links']         === null ? null : (int)    $row['links'],

      'variation'       =>        $row['var'],
      'icon'            =>        $row['icon']
    ];

    // Add false fields
    if ($payload['category'] === 'base') {
      if (!$payload['baseIsShaper']) {
        $payload['baseIsShaper'] = false;
      }

      if (!$payload['baseIsElder']) {
        $payload['baseIsElder'] = false;
      }
    }

    foreach (['shaper', 'elder', 'crusader', 'redeemer', 'hunter', 'warlord'] as $influence) {
      if ($row[$influence]) {
        $itemData['influences'][] = $influence;
      }
    }

    // Filter out null values
    $payload = array_filter($payload, function($value) {
      return $value !== null;
    });

    return $payload;
  }

  return null;
}

function get_history_data($pdo, $id) {
  $query = "select 
    i.mean, i.median, i.mode, i.min, i.max, i.exalted, 
    i.total, i.daily, i.current, i.accepted,
    DATE_FORMAT(l.start, '%Y-%m-%dT%H:00:00Z') as leagueStart,
    DATE_FORMAT(l.end, '%Y-%m-%dT%H:00:00Z') as leagueEnd,
    l.name as leagueName, l.active as leagueActive, l.id as leagueId,
    l.display as leagueDisplay
  from league_items as i
  join data_leagues as l
    on l.id = i.id_l
  where i.id_d = ?
  order by l.active desc, l.id desc";

  $stmt = $pdo->prepare($query);
  $stmt->execute([$id]);
  $payload = [];

  // loop through leagues
  while ($row = $stmt->fetch()) {
    $payload[] = [
      'id'          => (int)    $row['leagueId'],
      'name'        =>          $row['leagueName'],
      'display'     =>          $row['leagueDisplay'],
      'active'      => (bool)   $row['leagueActive'],
      'start'       =>          $row['leagueStart'],
      'end'         =>          $row['leagueEnd'],
      'mean'        => (float)  $row['mean'],
      'median'      => (float)  $row['median'],
      'mode'        => (float)  $row['mode'],
      'min'         => (float)  $row['min'],
      'max'         => (float)  $row['max'],
      'exalted'     => (float)  $row['exalted'],
      'total'       => (int)    $row['total'],
      'daily'       => (int)    $row['daily'],
      'current'     => (int)    $row['current'],
      'accepted'    => (int)    $row['accepted']
    ];
  }

  return $payload;
}

function build_payload($pdo, $id) {
  // Get item's name, frame, icon, etc.
  $payload = get_item_data($pdo, $id);

  // If there is no item with the provided id
  if (!$payload) error(400, "Invalid id");
  $payload['leagues'] = get_history_data($pdo, $id);

  return $payload;
}


header("Content-Type: application/json");
check_errors();
include_once ( "../details/pdo.php" );
$payload = build_payload($pdo, $_GET["id"]);
echo json_encode($payload, JSON_PRESERVE_ZERO_FRACTION);
