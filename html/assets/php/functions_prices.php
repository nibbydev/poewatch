<?php
if ( !isset($_GET["category"]) ) {
  header('location:/prices?category=currency');
  die();
}

include_once ( "details/pdo.php" );

// Define variables that will have their values set
$SERVICE_leagues = null;
$SERVICE_categories = null;
$SERVICE_category = $_GET["category"];

// Get list of categories from DB
function GetCategories($pdo) {
  $queryParent = "SELECT * FROM `category_parent`";
  $queryChild = "SELECT * FROM `category_child`";

  $stmtParent = $pdo->query($queryParent);
  $stmtChild = $pdo->query($queryChild);

  $SERVICE_categories = array();

  while ($row = $stmtParent->fetch()) {
    $SERVICE_categories[] = array(
      "name" => $row["parent"],
      "display" => $row["display"],
      "members" => array()
    );
  }

  while ($row = $stmtChild->fetch()) {
    for ($i=0; $i < count($SERVICE_categories); $i++) { 
      if ($SERVICE_categories[$i]["name"] === $row["parent"]) {
        $SERVICE_categories[$i]["members"][] = array(
          "name" => $row["child"],
          "display" => $row["display"]
        );

        break;
      }
    }
  }

  return $SERVICE_categories;
}

// Get list of leagues from DB
function GetLeagues($pdo) {
  $query = "SELECT * FROM `leagues`";
  $stmt = $pdo->query($query);
  
  $SERVICE_leagues = array();
  
  while ($row = $stmt->fetch()) {
    $SERVICE_leagues[] = $row["id"];
  }
  
  unset($query);
  unset($stmt);
  unset($row);

  return $SERVICE_leagues;
}

// Add category-specific selector fields to sub-category selector
function AddSubCategorySelectors($categories) {
  if ( !isset($_GET["category"]) ) return;

  foreach ($categories as $categoryElement) {
    if ( $categoryElement["name"] !== $_GET["category"] ) continue;

    echo "<option value='all'>All</option>";

    foreach ($categoryElement["members"] as $member) {
      $outString = "
      <option value='{$member["name"]}'>{$member["display"]}</option>";
  
      echo $outString;
    }

    break;
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

$SERVICE_categories = GetCategories($pdo);
$SERVICE_leagues = GetLeagues($pdo);

