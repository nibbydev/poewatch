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
    echo "<th title='Gem level'>Lvl</th>";
    echo "<th title='Gem quality'>Qual</th>";
    echo "<th title='Is the gem corrupted'>Corr</th>";
  } else if ( $category === "base" ) {
    echo "<th title='Item level'>iLvl</th>";
  } else if ( $category === "map" ) {
    echo "<th title='Map tier'>Tier</th>";
  }
  
  echo "<th class='d-none d-md-block'>Spark</th>";
  echo "<th><span class='sort-column'>Chaos</span></th>";
  echo "<th class='d-none d-md-block'><span class='sort-column'>Exalted</span></th>";
  echo "<th><span class='sort-column' title='Price compared to 7d ago'>Change</span></th>";
  echo "<th><span class='sort-column' title='Number of items currently on sale'>Now</span></th>";
  echo "<th><span class='sort-column' title='Number of items listed every 24h'>Daily</span></th>";
  echo "<th><span class='sort-column' title='Total number of items listed'>Total</span></th>";
}

function GetGroups($pdo, $category) {
  $query = "
  SELECT dg.name, dg.display 
  FROM data_groups as dg
  join (
    select DISTINCT id_grp 
    from data_itemData 
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
