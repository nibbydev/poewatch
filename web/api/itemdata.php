<?php
function get_data($pdo) {
  $query = "SELECT 
    did.id, did.name, did.type, did.frame, 
    did.tier, did.lvl, did.quality, did.corrupted, 
    did.links, did.ilvl, did.var, did.icon, 
    cc.id AS category
  FROM data_itemData AS did 
  LEFT JOIN category_child AS cc ON cc.id = did.id_cc";

  return $pdo->query($query);
}

function parse_data($stmt) {
  $payload = array();

  while ($row = $stmt->fetch()) {
    $tmp = array(
      'id'        => $row['id'],
      'name'      => $row['name'],
      'type'      => $row['type'],
      'frame'     => $row['frame'],
      'tier'      => $row['tier'],
      'lvl'       => $row['lvl'],
      'quality'   => $row['quality'],
      'corrupted' => $row['corrupted'],
      'links'     => $row['links'],
      'ilvl'      => $row['ilvl'],
      'var'       => $row['var'],
      'icon'      => $row['icon'],
      'category'  => $row['category'],
    );
    
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
