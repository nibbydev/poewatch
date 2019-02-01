<?php
function get_all_data($pdo) {
  $query = "SELECT 
    did.id, did.name, did.type, did.frame, did.stack, 
    did.tier, did.lvl, did.quality, did.corrupted, 
    did.links, did.ilvl, did.var, did.icon, 
    dc.name AS category, dg.name AS `group`
  FROM data_itemData AS did 
  LEFT JOIN data_categories AS dc ON dc.id = did.id_cat
  LEFT JOIN data_groups     AS dg ON dg.id = did.id_grp";

  return $pdo->query($query);
}

function get_category_data($pdo, $category) {
  $query = "SELECT 
    did.id, did.name, did.type, did.frame, did.stack, 
    did.tier, did.lvl, did.quality, did.corrupted, 
    did.links, did.ilvl, did.var, did.icon, 
    dc.name AS category, dg.name AS `group`
  FROM data_itemData AS did 
  LEFT JOIN data_categories AS dc ON dc.id = did.id_cat
  LEFT JOIN data_groups     AS dg ON dg.id = did.id_grp
  WHERE dc.name = ?";

  $stmt = $pdo->prepare($query);
  $stmt->execute([$category]);

  return $stmt;
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
      'stack'     => $row['stack'] === NULL ? null : (int) $row['stack'],
      'lvl'       => $row['lvl'],
      'quality'   => $row['quality'],
      'corrupted' => $row['corrupted'] === NULL ? null : (bool) $row['corrupted'],
      'links'     => $row['links'],
      'ilvl'      => $row['ilvl'],
      'variation' => $row['var'],
      'icon'      => $row['icon'],
      'category'  => $row['category'],
      'group'     => $row['group'],
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
if (isset($_GET["category"])) {
  $stmt = get_category_data($pdo, $_GET["category"]);
} else {
  $stmt = get_all_data($pdo);
}

$payload = parse_data($stmt);

// Display generated data
echo json_encode($payload);
