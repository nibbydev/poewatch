<?php
function get_data($pdo) {
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

function parse_data($stmt) {
  $payload = array();

  while ($row = $stmt->fetch()) {
    $tmp = array(
      'name'    => $row['parentName'],
      'display' => $row['parentDisplay'],
      'members'       => array()
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
header('Content-Type: application/json');

// Connect to database
include_once ( '../details/pdo.php' );

// Get data from database
$stmt = get_data($pdo);

// Parse received data
$payload = parse_data($stmt);

// Display generated data
echo json_encode($payload);
