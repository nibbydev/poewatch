<?php

function get_data($pdo) {
  $query = "SELECT 
    did.id, did.name, did.type, did.frame, did.stack, 
    did.tier, did.lvl, did.tier, did.shaper, did.elder, 
    did.enchantMin, did.enchantMax ,did.quality, did.corrupted, 
    did.links, did.ilvl, did.var, did.icon, 
    dc.name AS category, dg.name AS `group`
  FROM data_itemData AS did 
  LEFT JOIN data_categories AS dc ON dc.id = did.id_cat
  LEFT JOIN data_groups     AS dg ON dg.id = did.id_grp";

  if ( isset($_GET["category"]) ) {
    $query .= " WHERE dc.name = ?";
    $stmt = $pdo->prepare($query);
    $stmt->execute([$_GET["category"]]);
    return $stmt;
  } else {
    return $pdo->query($query);
  }
}

function parse_data($stmt) {
  $payload = array();

  while ($row = $stmt->fetch()) {
    $tmp = array(
      'id'        => $row['id'],
      'name'      => $row['name'],
      'type'      => $row['type'],
      'frame'     => $row['frame'],
      'category'  => $row['category'],
      'group'     => $row['group'],

      'tier'      => $row['tier'],
      'stack'     => $row['stack'],

      'base'      => null,
      'enchant'   => null,
      'gem'       => null,
      'links'     => $row['links'],
      'variation' => $row['var'],
      'icon'      => $row['icon']
    );

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
    
    $payload[] = $tmp;
  }

  return $payload;
}

// Define content type
header("Content-Type: application/json");

// Connect to database
include_once ( "../details/pdo.php" );

// Get item entries
$stmt = get_data($pdo);
$payload = parse_data($stmt);

// Display generated data
echo json_encode($payload);
