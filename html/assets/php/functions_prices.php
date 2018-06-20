<?php
// Checks whether a category param was passed on to the request
function CheckAndGetCategoryParam() {
  if ( !isset($_GET["category"]) ) {
    header('location:/prices?category=currency');
    die();
  } else return $_GET["category"];
}

// Get list of categories from DB
function GetCategories($pdo) {
  $query = "SELECT 
    `cp`.`name` AS 'parent_name', `cc`.`name` AS 'child_name', 
    `cp`.`display` AS 'parent_display', `cc`.`display` AS 'child_display' 
    FROM `category_parent` AS `cp`
    LEFT JOIN `category_child` AS `cc`
    ON `cp`.`id` = `cc`.`id_parent`";

  $stmt = $pdo->query($query);

  $payload = array();

  $prevParent = null;
  $parentArray = array();

  while ($row = $stmt->fetch()) {
    if ($prevParent == null) {
      $prevParent = $row["parent_name"];

      $prevParentArray = array(
        "name" => $row["parent_name"],
        "display" => $row["parent_display"],
        "members" => array(
          array(
            "name" => $row["child_name"],
            "display" => $row["child_display"]
          )
        )
      );
    } else if ($prevParent !== $row["parent_name"]) {
      $prevParent = $row["parent_name"];
      $payload[] = $prevParentArray;

      $prevParentArray = array(
        "name" => $row["parent_name"],
        "display" => $row["parent_display"],
        "members" => array(
          array(
            "name" => $row["child_name"],
            "display" => $row["child_display"]
          )
        )
      );
    } else {
      $parentArray["members"][] = array(
        "name" => $row["child_name"],
        "display" => $row["child_display"]
      );
    }
  }

  return $payload;
}

// Get list of leagues from DB
function GetLeagues($pdo, $short) {
  $query = "SELECT * FROM `leagues`";
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
