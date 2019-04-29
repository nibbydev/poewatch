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
    d.id, d.name, d.type, d.frame, d.icon, d.stack, 
    d.tier, d.lvl, d.quality, d.corrupted, 
    d.links, d.ilvl, d.series, d.shaper, d.elder, 
    d.enchantMin, d.enchantMax, d.var,
    dc.name as category, dg.name as `group`
  from      data_itemData   as d
  left join data_categories as dc on d.id_cat = dc.id 
  left join data_groups     as dg on d.id_grp = dg.id 
  where     d.id = ?
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
    return array_filter($payload, function($value) {
      return $value !== null;
    });
  }

  return null;
}

function get_history_data($pdo, $id) {
  $query = "select 
    i.mean, i.median, i.mode, i.min, i.max, i.exalted, 
    i.total, i.daily, i.current, i.accepted, i.id_l,
    l.name
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
      'id'        => (int)    $row['id_l'],
      'name'      =>          $row['name'],
      'mean'      => (float)  $row['mean'],
      'median'    => (float)  $row['median'],
      'mode'      => (float)  $row['mode'],
      'min'       => (float)  $row['min'],
      'max'       => (float)  $row['max'],
      'exalted'   => (float)  $row['exalted'],
      'total'     => (int)    $row['total'],
      'daily'     => (int)    $row['daily'],
      'current'   => (int)    $row['current'],
      'accepted'  => (int)    $row['accepted']
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
echo json_encode($payload, JSON_PRESERVE_ZERO_FRACTION | JSON_PRETTY_PRINT);
