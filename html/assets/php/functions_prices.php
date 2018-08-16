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
  $query = "SELECT name, display 
  FROM data_leagues 
  WHERE active = 1
  ORDER BY id DESC";

  $stmt = $pdo->query($query);
  $payload = array();
  
  while ($row = $stmt->fetch()) {
    $payload[] = array($row["name"], $row["display"]);
  }

  return $payload;
}

// Add category-specific selector fields to sub-category selector
function AddSubCategorySelectors($categories) {
  echo "<option value='all'>All</option>";

  foreach ($categories as $entry) {
    echo "
    <option value='{$entry[0]}'>{$entry[1]}</option>";
  }
}

// Add league select fields to second navbar
function AddLeagueSelects($leagues) {
  foreach ($leagues as $entry) {
    if ($entry[1] === null) $entry[1] = $entry[0];
    echo "<option value='{$entry[0]}'>{$entry[1]}</option>";
  }
}

// Add table headers based on category
function AddTableHeaders($category) {
  echo "<th class='w-100' scope='col'>Item</th>";

  if ( $category === "gems" ) {
    echo "<th scope='col'>Lvl</th>";
    echo "<th scope='col'>Qual</th>";
    echo "<th scope='col'>Corr</th>";
  }
  
  echo "<th scope='col'></th>";
  echo "<th scope='col'>Chaos</th>";
  echo "<th scope='col'>Exalted</th>";
  echo "<th scope='col' class='fixedSizeCol'>Change</th>";
  echo "<th scope='col' class='fixedSizeCol'>Count</th>";
}

// Adds a message to the MotD box
function AddMotdMessage($category) {
  echo "<p class='mb-0 text-center subtext-1'>";

  if ($category === "enchantments") {
    echo "[ Enchantment prices <i>might</i> be inaccurate at this point in time ]";
  } else {
    echo "[ allan please add advertisement ]";
  }

  echo "</p>";
}
