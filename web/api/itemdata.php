<?php
function get_data_itemdata($pdo) {
  $query = "SELECT 
    did.id, did.name, did.type, did.frame, 
    did.tier, did.lvl, did.quality, did.corrupted, 
    did.links, did.var, did.icon, 
    cc.id AS category
  FROM data_itemData AS did 
  LEFT JOIN category_child AS cc ON cc.id = did.id_cc";

  return $pdo->query($query);
}

function get_data_categories($pdo) {
  $query = "SELECT 
    cp.id                     AS parentId, 
    cp.name                   AS parentName, 
    cp.display                AS parentDisplay,
    GROUP_CONCAT(cc.id)       AS memberIds,
    GROUP_CONCAT(cc.name)     AS memberNames,
    GROUP_CONCAT(cc.display)  AS memberDisplays
  FROM      category_parent   AS cp
  LEFT JOIN category_child    AS cc
    ON      cp.id = cc.id_cp
  GROUP BY  cp.id
  ORDER BY  cp.id ASC";

  return $pdo->query($query);
}

function parse_data_itemdata($stmt) {
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
      'var'       => $row['var'],
      'icon'      => $row['icon'],
      'category'  => $row['category'],
    );
    
    $payload[] = $tmp;
  }

  return $payload;
}

function parse_data_categories($stmt) {
  $payload = array();

  while ($row = $stmt->fetch()) {
    $tmp = array(
      'name'    => $row['parentName'],
      'display' => $row['parentDisplay'],
      'members' => array()
    );

    $explMemberIds      = explode(',', $row['memberIds']);
    $explMemberNames    = explode(',', $row['memberNames']);
    $explMemberDisplays = explode(',', $row['memberDisplays']);
    $count              = sizeof($explMemberIds);

    if ($count > 0) {
      for ($i = 0; $i < $count; $i++) { 
        $tmp['members'][] = array(
          'id'      => (int) $explMemberIds[$i],
          'name'    => $explMemberNames[$i],
          'display' => $explMemberDisplays[$i]
        );
      }
    }

    $payload[] = $tmp;
  }

  return $payload;
}

// Define content type
header("Content-Type: application/json");

// Connect to database
include_once ( "../details/pdo.php" );

// Form payload
$payload = array();

// Get category entries
$stmt = get_data_categories($pdo);
$payload['categories'] = parse_data_categories($stmt);

// Get item entries
$stmt = get_data_itemdata($pdo);
$payload['items'] = parse_data_itemdata($stmt);

// Display generated data
echo json_encode($payload);
