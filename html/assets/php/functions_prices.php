<?php
// Checks whether a category param was passed on to the request
function CheckAndGetCategoryParam() {
  if ( !isset($_GET["category"]) ) {
    header('location:/prices?category=currency');
    die();
  } else return $_GET["category"];
}

// Get list of categories from DB
function GetCategories($pdo, $category) {
  $query = "SELECT cc.name, cc.display
    FROM `category-child` AS cc
    JOIN `category-parent` AS cp
    ON cp.id = cc.`id-cp`
    WHERE cp.name = ?";

  $stmt = $pdo->prepare($query);
  $stmt->execute([$category]);

  $payload = array();

  while ($row = $stmt->fetch()) {
    $payload[$row["name"]] = $row["display"];
  }

  return $payload;
}

// Get list of leagues from DB
function GetLeagues($pdo, $short) {
  $query = "SELECT * FROM `sys-leagues`";
  $stmt = $pdo->query($query);
  
  $rows = array();
  
  while ($row = $stmt->fetch()) {
    if ($short) $rows[] = $row["name"];
    else $rows[] = $row;
  }

  return $rows;
}

// Add category-specific selector fields to sub-category selector
function AddSubCategorySelectors($categories) {
  echo "<option value='all'>All</option>";

  foreach ($categories as $key => $value) {
    echo "
    <option value='$key'>$value</option>";
  }
}

// Add league radio buttons to second navbar
function AddLeagueRadios($leagues) {
  foreach ($leagues as $league) {
    $outString = "
    <label class='btn btn-sm btn-outline-dark p-0 px-1 {{active}}'>
      <input type='radio' name='league' value='$league'>$league
    </label>";

    echo $outString;
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
  
  echo "<th scope='col'>Chaos</th>";
  echo "<th scope='col'>Exalted</th>";
  echo "<th scope='col'>Change</th>";
  echo "<th scope='col'>Count</th>";
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
