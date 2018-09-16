<?php
// Checks whether a category param was passed on to the request
function CheckAndGetCategoryParam() {
  if ( !isset($_GET["category"]) ) {
    $_GET["category"] = "currency";
  }
  
  return $_GET["category"];
}

// Get list of child categories and their display names from DB
function GetCategories($pdo, $category) {
  $query = "SELECT cc.name, cc.display
    FROM category_child AS cc
    JOIN category_parent AS cp
    ON cp.id = cc.id_cp
    WHERE cp.name = ?";

  $stmt = $pdo->prepare($query);
  $stmt->execute([$category]);

  $payload = array();

  while ($row = $stmt->fetch()) {
    $payload[] = array($row["name"], $row["display"]);
  }

  return $payload;
}

// Get list of leagues and their display names from DB
function GetLeagues($pdo) {
  $query = "SELECT l.id, l.name, l.display, l.active, l.upcoming, l.event 
  FROM data_leagues AS l 
  JOIN ( 
    SELECT DISTINCT id_l FROM league_history_daily_rolling 
    UNION  DISTINCT 
    SELECT DISTINCT id_l FROM league_history_daily_inactive 
  ) AS leagues ON l.id = leagues.id_l 
  ORDER BY active DESC, id DESC";

  $stmt = $pdo->query($query);
  $payload = array();
  
  while ($row = $stmt->fetch()) {
    $payload[] = $row;
  }

  return $payload;
}

// Add category-specific selector fields to sub-category selector
function AddSubCategorySelectors($categories) {
  echo "<option value='all'>All</option>";

  foreach ($categories as $entry) {
    $value = $entry[0];
    $display = $entry[1] ? $entry[1] : ucwords($entry[0]);

    echo "<option value='$value'>$display</option>";
  }
}

// Add league select fields to second navbar
function AddLeagueSelects($leagues) {
  // Loop through all available leagues
  foreach ($leagues as $leagueEntry) {
    $value = $leagueEntry['name'];

    $display = $leagueEntry['active'] ? "" : "● ";
    $display .= $leagueEntry['display'] === null ? $leagueEntry['name'] : $leagueEntry['display'];

    echo "<option value='$value'>$display</option>";
  }
}

// Add table headers based on category
function AddTableHeaders($category) {
  echo "<th class='w-100' scope='col'>Item</th>";

  if ( $category === "gem" ) {
    echo "<th scope='col'>Lvl</th>";
    echo "<th scope='col'>Qual</th>";
    echo "<th scope='col'>Corr</th>";
  } else if ( $category === "base" ) {
    echo "<th scope='col'>iLvl</th>";
  } else if ( $category === "map" ) {
    echo "<th scope='col'>Tier</th>";
  }
  
  echo "<th scope='col'>Chaos</th>";
  echo "<th scope='col'>Exalted</th>";
  echo "<th scope='col' class='fixedSizeCol'>Change</th>";
  echo "<th scope='col' class='fixedSizeCol'>Count</th>";
}

function GenSpecificSearchForm($category) {
  switch ($category) {
    case "gem": 
      include_once ("assets/php/form_templates/gem.php");
      break;
    case "armour":
    case "weapon":
      include_once ("assets/php/form_templates/armour_weapon.php");
      break;
    case "map":
      include_once ("assets/php/form_templates/map.php");
      break;
    case "base":
      include_once ("assets/php/form_templates/base.php");
      break;
    default:
      break;
  }
}

