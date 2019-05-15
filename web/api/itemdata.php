<?php
function get_data($pdo) {
  $query = "SELECT 
    did.id, did.name, did.type, did.frame, did.stack, 
    did.tier, did.lvl, did.tier, did.series, did.shaper, did.elder, 
    did.enchantMin, did.enchantMax ,did.quality, did.corrupted, 
    did.links, did.ilvl, did.var, did.icon, 
    dc.name AS category, dg.name AS `group`
  FROM data_itemData AS did 
  LEFT JOIN data_categories AS dc ON dc.id = did.id_cat
  LEFT JOIN data_groups     AS dg ON dg.id = did.id_grp";

  return $pdo->query($query);
}

function parse_data($stmt) {
  $payload = [];

  while ($row = $stmt->fetch()) {
    $itemData = [
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
    $payload[] = array_filter($itemData, function($value) {
      return $value !== null;
    });
  }

  return $payload;
}

header("Content-Type: application/json");
include_once ( "../details/pdo.php" );

$payload = parse_data(get_data($pdo));
echo json_encode($payload, JSON_PRETTY_PRINT);
