<?php
function get_data($pdo) {
  $query = "SELECT 
    dc.id                     AS categoryId, 
    dc.name                   AS categoryName, 
    dc.display                AS pcategoryDisplay,
    GROUP_CONCAT(dg.id)       AS memberIds,
    GROUP_CONCAT(dg.name)     AS memberNames,
    GROUP_CONCAT(dg.display)  AS memberDisplays
  FROM      data_categories   AS dc
  LEFT JOIN data_groups       AS dg
    ON      dc.id = dg.id_cat
  GROUP BY  dc.id
  ORDER BY  dc.id ASC";

  return $pdo->query($query);
}

function parse_data($stmt) {
  $payload = array();

  while ($row = $stmt->fetch()) {
    $tmp = array(
      'name'    => $row['categoryName'],
      'display' => $row['categoryDisplay'],
      'groups'  => array()
    );

    $explMemberIds      = explode(',', $row['memberIds']);
    $explMemberNames    = explode(',', $row['memberNames']);
    $explMemberDisplays = explode(',', $row['memberDisplays']);
    $count              = sizeof($explMemberIds);

    if ($count > 0) {
      for ($i = 0; $i < $count; $i++) { 
        $tmp['groups'][] = array(
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
