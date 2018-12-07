<?php
class FormGen {
  private function Dynamic_Tier() {
    echo "
    <div class='mr-3 mb-2'>
      <h4>Tier</h4>
      <select class='form-control' id='select-tier'>
        <option value='all' selected>All</option>
        <option value='none'>None</option>
        <option value='1'>1</option>
        <option value='2'>2</option>
        <option value='3'>3</option>
        <option value='4'>4</option>
        <option value='5'>5</option>
        <option value='6'>6</option>
        <option value='7'>7</option>
        <option value='8'>8</option>
        <option value='9'>9</option>
        <option value='10'>10</option>
        <option value='11'>11</option>
        <option value='12'>12</option>
        <option value='13'>13</option>
        <option value='14'>14</option>
        <option value='15'>15</option>
        <option value='16'>16</option>
      </select>
    </div>";
  }

  private function Dynamic_Quality() {
    echo "
    <div class='mr-3 mb-2'>
      <h4>Quality</h4>
      <select class='form-control' id='select-quality'>
        <option value='all' selected>All</option>
        <option value='0'>0</option>
        <option value='20'>20</option>
        <option value='23'>23</option>
      </select>
    </div>";
  }

  private function Dynamic_Level() {
    echo "
    <div class='mr-3 mb-2'>
      <h4>Level</h4>
      <select class='form-control' id='select-level'>
        <option value='all' selected>All</option>
        <option value='1'>1</option>
        <option value='2'>2</option>
        <option value='3'>3</option>
        <option value='4'>4</option>
        <option value='20'>20</option>
        <option value='21'>21</option>
      </select>
    </div>";
  }

  private function Dynamic_Influence() {
    echo "
    <div class='mr-3 mb-2'>
      <h4>Influence</h4>
      <select class='form-control' id='select-influence'>
        <option value='all' selected>All</option>
        <option value='none'>None</option>
        <option value='either'>Either</option>
        <option value='shaper'>Shaper</option>
        <option value='elder'>Elder</option>
      </select>
    </div>";
  }

  private function Dynamic_ItemLevel() {
    echo "
    <div class='mr-3 mb-2'>
      <h4>Ilvl</h4>
      <select class='form-control' id='select-ilvl'>
        <option value='all' selected>All</option>
        <option value='82'>82</option>
        <option value='83'>83</option>
        <option value='84'>84</option>
        <option value='85'>85</option>
        <option value='86'>86+</option>
      </select>
    </div>";
  }

  private function Dynamic_Corrupted() {
    $val = isset($_GET["corrupted"]) ? $_GET["corrupted"] : "all";
    $options = array(
      "all"   => "Both", 
      "true"  => "Yes",  
      "false" => "No"
    );

    echo "<div class='mr-3 mb-2'>
    <h4>Corrupted</h4>
    <div class='btn-group btn-group-toggle' data-toggle='buttons' id='radio-corrupted'>";

    foreach ($options as $value => $display) {
      $active = $val === $value ? "active" : "";
      $checked = $val === $value ? "checked" : "";

      echo "<label class='btn btn-outline-dark $active'>
      <input type='radio' name='corrupted' value='$value' $checked>$display
      </label>";
    }

    echo "</div>
    </div>";
  }

  private function Dynamic_Links() {
    $val = isset($_GET["links"]) ? $_GET["links"] : "none";
    $options = array(
      "none"  => "None", 
      "5"     => "5L",  
      "6"     => "6L",
      "all"   => "All"
    );

    echo "<div class='mr-3 mb-2'>
    <h4>Links</h4>
    <div class='btn-group btn-group-toggle' data-toggle='buttons' id='radio-links'>";

    foreach ($options as $value => $display) {
      $active = $val === (string)$value ? "active" : "";
      $checked = $val === (string)$value ? "checked" : "";

      echo "<label class='btn btn-outline-dark $active'>
      <input type='radio' name='links' value='$value' $checked>$display
      </label>";
    }

    echo "</div>
    </div>";
  }

  private function Dynamic_Rarity() {
    $val = isset($_GET["rarity"]) ? $_GET["rarity"] : "all";
    $options = array(
      "all"     => "Both", 
      "unique"  => "Unique",  
      "relic"   => "Relic"
    );

    echo "<div class='mr-3 mb-2'>
    <h4>Rarity</h4>
    <div class='btn-group btn-group-toggle' data-toggle='buttons' id='radio-rarity'>";

    foreach ($options as $value => $display) {
      $active = $val === $value ? "active" : "";
      $checked = $val === $value ? "checked" : "";

      echo "<label class='btn btn-outline-dark $active'>
      <input type='radio' name='rarity' value='$value' $checked>$display
      </label>";
    }

    echo "</div>
    </div>";
  }

  private function Static_Confidence() {
    $val = isset($_GET["confidence"]) ? $_GET["confidence"] : "false";
    $options = array(
      "false" => "Hide", 
      "true"  => "Show"
    );

    echo "<div class='mr-3'>
    <h4 class='nowrap'>Low daily</h4>
    <div class='btn-group btn-group-toggle' data-toggle='buttons' id='radio-confidence'>";

    foreach ($options as $value => $display) {
      $active = $val === $value ? "active" : "";
      $checked = $val === $value ? "checked" : "";

      echo "<label class='btn btn-outline-dark $active'>
      <input type='radio' name='confidence' value='$value' $checked>$display
      </label>";
    }

    echo "</div>
    </div>";
  }

  private function Static_Group($pdo, $category) {
    echo "<div class='ml-auto mr-3'>
    <h4>Group</h4>
    <select class='form-control custom-select' id='search-group'>";
      
    if ($category === "relic") {
      $query = "SELECT
        1 AS tmpId,
        GROUP_CONCAT(   name ORDER BY name ASC) AS names, 
        GROUP_CONCAT(display ORDER BY name ASC) AS displays
      FROM     data_categories
      JOIN (
        SELECT DISTINCT id_cat 
        FROM            data_itemData
        WHERE           frame = 9
      ) AS     tmp 
        ON     tmp.id_cat = id
      GROUP BY tmpId";
  
      $stmt = $pdo->prepare($query);
      $stmt->execute();
    } else {
      $query = "SELECT 
        GROUP_CONCAT(name ORDER BY name ASC)                  AS names, 
        GROUP_CONCAT(IFNULL(display, name) ORDER BY name ASC) AS displays
      FROM data_groups
      WHERE id_cat = (SELECT id FROM data_categories WHERE name = ? LIMIT 1)
      GROUP BY id_cat";
  
      $stmt = $pdo->prepare($query);
      $stmt->execute([$category]);
    }
  
    if ($stmt->rowCount()) {
      $row      = $stmt->fetch();
      $names    = explode(',',    $row['names']);
      $displays = explode(',', $row['displays']);
  
      if (sizeof($names) > 1) {
        echo "<option value='all'>All</option>";
      }
  
      for ($i = 0; $i < sizeof($names); $i++) { 
        echo "<option value='{$names[$i]}'>{$displays[$i]}</option>";
      }
    }

    echo "</select>
    </div>";
  }

  private function Static_Search() {
    $val = isset($_GET['search']) ? $_GET['search'] : '';

    echo "<div>
    <h4>Search</h4>
    <input type='text' class='form-control' id='search-searchbar' placeholder='Search' value='$val'>
    </div>";
  }

  private function Gen_Dynamic($category) {
    echo "<div class='d-flex flex-wrap'>";

    switch ($category) {
      case "gem":
        $this->Dynamic_Corrupted();
        $this->Dynamic_Level();
        $this->Dynamic_Quality();
        break;
      case "armour":
      case "weapon":
        $this->Dynamic_Links();
        $this->Dynamic_Rarity();
        break;
      case "map":
        $this->Dynamic_Tier();
        $this->Dynamic_Rarity();
        break;
      case "base":
        $this->Dynamic_ItemLevel();
        $this->Dynamic_Influence();
        break;
      case "flask":
      case "accessory":
      case "jewel":
        $this->Dynamic_Rarity();
        break;
      default:
        break;
    }

    echo "</div>";
  }

  private function Gen_Static($pdo, $category) {
    echo "<div class='d-flex flex-wrap'>";

    echo $this->Static_Confidence();
    echo $this->Static_Group($pdo, $category);
    echo $this->Static_Search();

    echo "</div>";
  }

  public function __construct($pdo, $category) {
    $this->Gen_Dynamic($category);
    $this->Gen_Static($pdo, $category);
  }
}

// Get list of leagues and their display names from DB
function GetLeagues($pdo) {
  $query = "SELECT l.name, l.display, l.active
  FROM data_leagues AS l 
  JOIN ( 
    SELECT DISTINCT id_l FROM league_items 
  ) AS leagues ON l.id = leagues.id_l 
  ORDER BY active DESC, id DESC";

  $stmt = $pdo->query($query);
  $payload = array();
  
  while ($row = $stmt->fetch()) {
    $payload[] = $row;
  }

  return $payload;
}

// Get all available category relations
function GetCategories($pdo) {
  $stmt = $pdo->query("SELECT name FROM data_categories");
  
  $payload = array();
  while ($row = $stmt->fetch()) {
    $payload[] = $row['name'];
  }

  return $payload;
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
  
  echo "<th><span class='sort-column'>Chaos</span></th>";
  echo "<th><span class='sort-column'>Exalted</span></th>";
  echo "<th><span class='sort-column'>Change</span></th>";
  echo "<th><span class='sort-column'>Daily</span></th>";
  echo "<th><span class='sort-column'>Total</span></th>";
}

// Redirects user to url with necessary query parameters, if missing
function CheckQueryParams($leagues, $categories) {
  // Check if user-provided league param is valid
  if (isset($_GET['league'])) {
    $found = false;
    
    foreach ($leagues as $lEntry) {
      if ($lEntry['name'] === $_GET['league']) {
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
  if (isset($_GET['league'])) {
    if ($_GET['category'] !== 'relic') {
      if (!in_array($_GET['category'], $categories, true)) {
        header("Location: prices");
        exit();
      }
    }
  }
}
