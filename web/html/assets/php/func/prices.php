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

// Add table headers based on category
function AddTableHeaders($category) {
  echo "<th class='w-100'><span class='sort-column'>Item</span></th>";

  if ( $category === "gem" ) {
    echo "<th>Lvl</th>";
    echo "<th>Qual</th>";
    echo "<th>Corr</th>";
  } else if ( $category === "base" ) {
    echo "<th>iLvl</th>";
  } else if ( $category === "map" ) {
    echo "<th>Tier</th>";
  }
  
  echo "<th><span class='sort-column'>Spark</span></th>";
  echo "<th><span class='sort-column'>Chaos</span></th>";
  echo "<th><span class='sort-column'>Exalted</span></th>";
  echo "<th><span class='sort-column'>Change</span></th>";
  echo "<th><span class='sort-column'>Daily</span></th>";
  echo "<th><span class='sort-column'>Total</span></th>";
}

function GetGroups($pdo, $category) {
  $query = "
  SELECT name, display FROM data_groups
  WHERE id_cat = (SELECT id FROM data_categories WHERE name = ? LIMIT 1)
  ";

  $stmt = $pdo->prepare($query);
  $stmt->execute([$category]);

  $payload = array();
  while ($row = $stmt->fetch()) {
    $payload[] = array(
      "name" => $row["name"],
      "display" => $row["display"]
    );
  }

  return $payload;
}

function AddGroups($groups) {
  if (sizeof($groups) > 1) {
    echo "<option value='all'>All</option>";
  }

  foreach ($groups as $group) {
    echo "<option value='{$group['name']}'>{$group['display']}</option>";
  }
}