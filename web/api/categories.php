<?php
function get_data($pdo) {
  $query = "SELECT 
    categories.id as categoryId, 
    categories.name as categoryName, 
    categories.display as categoryDisplay,
    group_concat(groups.id) as memberIds,
    group_concat(groups.name) as memberNames,
    group_concat(IFNULL(groups.display, '')) as memberDisplays
  from (select distinct id_cat, id_grp from data_itemData) as did
  join data_categories as categories on did.id_cat = categories.id
  join data_groups as groups on did.id_grp = groups.id
  group by categories.id
  order by categories.id asc";

  return $pdo->query($query);
}

function parse_data($stmt) {
  $payload = array();

  while ($row = $stmt->fetch()) {
    $tmp = array(
      'id'      => $row['categoryId'],
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
          'display' => $explMemberDisplays[$i] ? $explMemberDisplays[$i] : null
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
