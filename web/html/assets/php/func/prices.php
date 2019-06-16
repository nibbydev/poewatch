<?php

// Get all leagues that have items
function GetItemLeagues($pdo) {
  $query = "
  SELECT l.name, l.display, l.active
  FROM data_leagues AS l 
  JOIN ( 
    SELECT DISTINCT id_l FROM league_items 
  ) AS leagues ON l.id = leagues.id_l 
  ORDER BY active DESC, id DESC
  ";

  $stmt = $pdo->query($query);
  $payload = array();

  while ($row = $stmt->fetch()) {
    $payload[] = $row;
  }

  return $payload;
}

// Get all categories
function GetCategories($pdo) {
  $query = "SELECT `name` FROM data_categories";
  $stmt = $pdo->query($query);

  $payload = array();
  while ($row = $stmt->fetch()) {
    $payload[] = $row['name'];
  }

  return $payload;
}

// Redirects user to url with necessary query parameters, if missing
function CheckQueryParams($leagues, $categories) {
  // Check if user-provided league param is valid
  if (isset($_GET['league'])) {
    $found = false;

    foreach ($leagues as $league) {
      if ($league['name'] === $_GET['league']) {
        $found = true;
        break;
      }
    }

    if (!$found) {
      header("Location: prices");
      exit();
    }
  }

  // Check if user-provided category param is valid
  if (isset($_GET['category'])) {
    if (!in_array($_GET['category'], $categories, true)) {
      header("Location: prices");
      exit();
    }
  }
}

function GetGroups($pdo, $category) {
  $query = "
  SELECT dg.name, dg.display 
  FROM data_groups as dg
  join (
    select DISTINCT id_grp 
    from data_item_data 
    where id_cat = (SELECT id FROM data_categories WHERE name = ? LIMIT 1)
  ) as grps on dg.id = grps.id_grp
  ";

  $stmt = $pdo->prepare($query);
  $stmt->execute([$category]);

  $payload = array();
  while ($row = $stmt->fetch()) {
    $payload[] = array(
      "name" => $row["name"],
      "display" => !$row["display"] ? $row["name"] : $row["display"]
    );
  }

  return $payload;
}
