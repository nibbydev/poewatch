<?php
class FormGen {
  private static $form_iLvl = "
  <div class='mr-3 mb-2'>
    <h4>Ilvl</h4>
    <select class='form-control' id='select-ilvl'>
      <option value='all' selected>All</option>
      <option value='68-74'>68 - 74</option>
      <option value='75-82'>75 - 82</option>
      <option value='83-84'>83 - 84</option>
      <option value='85-100'>85 - 100</option>
    </select>
  </div>";

  private static $form_influence = "
  <div class='mr-3 mb-2'>
    <h4>Influence</h4>
    <select class='form-control' id='select-influence'>
      <option value='all' selected>All</option>
      <option value='none'>None</option>
      <option value='either'>Either</option>
      <option value='shaped'>Shaper</option>
      <option value='elder'>Elder</option>
    </select>
  </div>";  
  
  private static $form_corrupted = "
  <div class='mr-3 mb-2'>
    <h4>Corrupted</h4>
    <div class='btn-group btn-group-toggle' data-toggle='buttons' id='radio-corrupted'>
      <label class='btn btn-outline-dark active'>
        <input type='radio' name='corrupted' value='all'>Both
      </label>
      <label class='btn btn-outline-dark'>
        <input type='radio' name='corrupted' value='0'>No
      </label>
      <label class='btn btn-outline-dark'>
        <input type='radio' name='corrupted' value='1' checked>Yes
      </label>
    </div>
  </div>";

  private static $form_level = "
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

  private static $form_quality = "
  <div class='mr-3 mb-2'>
    <h4>Quality</h4>
    <select class='form-control' id='select-quality'>
      <option value='all' selected>All</option>
      <option value='0'>0</option>
      <option value='20'>20</option>
      <option value='23'>23</option>
    </select>
  </div>";

  private static $form_tier = "
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

  private static $form_links = "
  <div class='mr-3 mb-2'>
    <h4>Links</h4>
    <div class='btn-group btn-group-toggle' data-toggle='buttons' id='radio-links'>
      <label class='btn btn-outline-dark active'>
        <input type='radio' name='links' value='none' checked>None
      </label>
      <label class='btn btn-outline-dark'>
        <input type='radio' name='links' value='5'>5L
      </label>
      <label class='btn btn-outline-dark'>
        <input type='radio' name='links' value='6'>6L
      </label>
      <label class='btn btn-outline-dark'>
        <input type='radio' name='links' value='all'>All
      </label>
    </div>
  </div>";

  private static $form_rarity = "
  <div class='mr-3 mb-2'>
    <h4>Rarity</h4>
    <div class='btn-group btn-group-toggle' data-toggle='buttons' id='radio-rarity'>
      <label class='btn btn-outline-dark active'>
        <input type='radio' name='rarity' value='all' checked>Both
      </label>
      <label class='btn btn-outline-dark'>
        <input type='radio' name='rarity' value='unique'>Unique
      </label>
      <label class='btn btn-outline-dark'>
        <input type='radio' name='rarity' value='relic'>Relic
      </label>
    </div>
  </div>";

  public static function GenForm($category) {
    echo "<div class='d-flex flex-wrap'>";

    switch ($category) {
      case "gem":
        echo FormGen::$form_corrupted;
        echo FormGen::$form_level;
        echo FormGen::$form_quality;
        break;
      case "armour":
      case "weapon":
        echo FormGen::$form_links;
        echo FormGen::$form_rarity;
        break;
      case "map":
        echo FormGen::$form_tier;
        echo FormGen::$form_rarity;
        break;
      case "base":
        echo FormGen::$form_iLvl;
        echo FormGen::$form_influence;
        break;
      case "flask":
      case "accessory":
      case "jewel":
        echo FormGen::$form_rarity;
        break;
      default:
        break;
    }

    echo "</div>";
  }
}

// Checks whether a category param was passed on to the request
function CheckAndGetCategoryParam() {
  if ( !isset($_GET["category"]) ) {
    $_GET["category"] = "currency";
  }
  
  return $_GET["category"];
}

// Get all available category relations
function GetCategoryTranslations($pdo) {
  $query = "SELECT name, display FROM category_child
  UNION
  SELECT name, display FROM category_parent";

  $stmt = $pdo->prepare($query);
  $stmt->execute();

  $payload = array();

  while ($row = $stmt->fetch()) {
    $payload[ $row['name'] ] = $row['display'];
  }

  return $payload;
}

// Get list of leagues and their display names from DB
function GetLeagues($pdo) {
  $query = "SELECT l.id, l.name, l.display, l.active, l.upcoming, l.event 
  FROM data_leagues AS l 
  JOIN ( 
    SELECT DISTINCT id_l FROM league_items_rolling 
    UNION  DISTINCT 
    SELECT DISTINCT id_l FROM league_items_inactive 
  ) AS leagues ON l.id = leagues.id_l 
  ORDER BY active DESC, id DESC";

  $stmt = $pdo->query($query);
  $payload = array();
  
  while ($row = $stmt->fetch()) {
    $payload[] = $row;
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
