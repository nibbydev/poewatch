<?php
// General page-related data array
$DATA = array(
  // Search results per page                [not in use]
  "limit"      => 25, 
  // Nr of results from search
  "count"      => null,
  // Current (user-provided) page nmber     [not in use]
  "page"       => isset($_GET["page"])   && $_GET["page"]   ? intval($_GET["page"]) : 1,
  // Total pages based on: count / limit    [not in use]
  "pages"      => null,
  // Search string
  "search"     => isset($_GET["search"]) ? $_GET["search"] : null,
  // Search mode
  "mode"       => isset($_GET["mode"])   ? $_GET["mode"]   : null,
  // Table sizes for MotD
  "totalAccs"  => null,
  "totalChars" => null,
  "totalRels"  => null,
  // Error handling. Explanation under CheckVariableErrors()
  "errorCode"  => null,
  "errorMsg"   => null,
  // Values that can be passed to $_GET["mode"]
  "validModes" => array("account", "character", "transfer"),
  // Array consisting of pure HTML '<tr></tr>' elements
  "resultRows" => array()
);

$DATA = CheckVariableErrors ($DATA);
$DATA = GetTotalCounts($pdo, $DATA);
$DATA = MakeSearch    ($pdo, $DATA);

//------------------------------------------------------------------------------------------------------------
// General display functions
//------------------------------------------------------------------------------------------------------------

// Shows a MotD message of table sizes
function DisplayMotD($DATA) {
  $accDisplay = number_format($DATA["totalAccs"]);
  $charDisplay = number_format($DATA["totalChars"]);
  $relDisplay = number_format($DATA["totalRels"]);

  $accDisplay = "<span class='custom-text-green'>$accDisplay</span>";
  $charDisplay = "<span class='custom-text-green'>$charDisplay</span>";
  $relDisplay = "<span class='custom-text-green'>$relDisplay</span>";
  $timeDisplay = "<span class='custom-text-green'>" . FormatTimestamp("2018-07-14 00:00:00") . "</span>";

  echo "Explore $accDisplay account names, $charDisplay character names and $relDisplay relations collected from the stash API since $timeDisplay. <br>Only characters that have listed something through the public stash API will appear here.";
}

// Display notification
function DisplayNotification($DATA) {
  if ( $DATA["errorCode"] ) {
    echo "<span class='custom-text-red'>Error: {$DATA["errorMsg"]}</span>"; 
  } else if ($DATA["count"] !== null && $DATA["search"] !== null) {
    $countDisplay = "<span class='custom-text-green'>{$DATA["count"]}</span>";
    $nameDisplay = "<span class='custom-text-orange'>{$DATA["search"]}</span>";
    $results = $DATA["count"] === 1 ? "result" : "results";
  
    echo "$countDisplay $results for {$DATA["mode"]} names matching '$nameDisplay'";
  }
}

// Shows pagination if results exceeded limit
function DisplayPagination($DATA, $pos) {
  if ($DATA["pages"] <= 1) return;

  if ($pos === "top") {
    echo "<div class='btn-toolbar justify-content-center my-3'><div class='btn-group mr-2'>";
  } else {
    echo "<div class='btn-toolbar justify-content-center mt-3'><div class='btn-group mr-2'>";
  }

  if ($DATA["page"] > 1) {
    $url = FormSearchURL($DATA["mode"], $DATA["search"], $DATA["page"] - 1);
    echo "<a class='btn btn-outline-dark' href='$url'>«</a>";
  }

  if ($DATA["pages"] > 1) {
    $counter = $DATA["page"] . " / " . $DATA["pages"];
    echo "<div class='mx-3 d-flex'><span class='justify-content-center align-self-center'>$counter</span></div>";
  }

  if ($DATA["page"] < $DATA["pages"]) {
    $url = FormSearchURL($DATA["mode"], $DATA["search"], $DATA["page"] + 1);
    echo "<a class='btn btn-outline-dark' href='$url'>»</a>";
  }

  echo "</div></div>";
}

// Displays radio buttons and sets their active state
function CreateModeRadios($DATA) {
  foreach ($DATA["validModes"] as $index => $mode) {
    if ($DATA['mode'] === null) {
      $active   = $mode === 'account' ? 'active'  : '';
      $checked  = $mode === 'account' ? 'checked' : '';
    } else {
      $active   = $mode === $DATA['mode'] ? 'active'  : '';
      $checked  = $mode === $DATA['mode'] ? 'checked' : '';  
    }

    $name = ucwords($mode);

    echo "<label class='btn btn-outline-dark $active'>
      <input type='radio' name='mode' value='$mode' $checked><a>$name</a>
    </label>";
  }
}

// Prints main content table
function CreateTable($DATA) {
  if ($DATA["errorCode"] || !$DATA["search"] || !$DATA["count"]) {
    return;
  }

  // Create div and table
  $table = "<hr><div class='card api-data-table'><table class='table table-striped table-hover mb-0'>";

  // Create table header
  $table .= "<thead>";
  // Pick table header based on mode
  if ($DATA["mode"] === "transfer") {
    $table .= "
    <tr>
      <th>Account</th>
      <th>Was account</th>
      <th>Changed</th>
    </tr>";
  } else {
    $table .= "
    <tr>
      <th>Account</th>
      <th>Has character</th>
      <th>In league</th>
      <th>Last seen</th>
    </tr>";
  }
  // Close table header
  $table .= "</thead>";

  // Create table body
  $table .= "<tbody>";
  // Fill with data from database
  foreach ($DATA["resultRows"] as $index => $row) {
    $table .= $row;
  }
  // Close body, table and div
  $table .= "</tbody>";
  $table .= "</table></div>";

  // Print the full table
  echo $table;
}

//------------------------------------------------------------------------------------------------------------
// DB queries
//------------------------------------------------------------------------------------------------------------

// Pick a function based on mode
function MakeSearch($pdo, $DATA) {
  if ($DATA["errorCode"] || $DATA["search"] === null) {
    return $DATA;
  }
  
  switch ($DATA["mode"]) {
    case 'account':   return CharacterSearch ($pdo, $DATA);
    case 'character': return AccountSearch   ($pdo, $DATA);
    case 'transfer':  return TransferSearch  ($pdo, $DATA);
    default:          return $DATA;
  }
}

// Get table sizes
function GetTotalCounts($pdo, $DATA) {
  $query = "SELECT  TABLE_NAME, TABLE_ROWS 
  FROM    information_schema.TABLES 
  WHERE   table_schema = 'pw'
    AND ( table_name = 'account_characters'
    OR    table_name = 'account_accounts'
    OR    table_name = 'account_relations')";

  $stmt = $pdo->query($query);
  while ($row = $stmt->fetch()) {
    switch ($row['TABLE_NAME']) {
      case 'account_characters':  $DATA["totalChars"] = $row["TABLE_ROWS"]; break;
      case 'account_accounts':    $DATA["totalAccs"]  = $row["TABLE_ROWS"]; break;
      case 'account_relations':   $DATA["totalRels"]  = $row["TABLE_ROWS"]; break;
      default:  break;
    }
  }

  return $DATA;
}

// Search for characters based on account name
function CharacterSearch($pdo, $DATA) {
  $DATA["count"] = 0;

  $query = "SELECT   
    a.name AS account,
    c.name AS `character`,
    l.display AS league,
    l.active AS active,
    r.seen
  FROM (
    SELECT *
    FROM   account_accounts 
    WHERE  name LIKE ? ESCAPE '=' 
  ) AS a
  JOIN     account_relations  AS r ON r.id_a = a.id
  JOIN     account_characters AS c ON r.id_c = c.id
  JOIN     data_leagues       AS l ON r.id_l = l.id
  ORDER BY r.seen DESC, c.name DESC
  LIMIT 200";

  // Execute get query and get the data
  $escapedSearch = likeEscape( $DATA["search"] );
  $stmt = $pdo->prepare($query);
  $stmt->execute([ $escapedSearch ]);

  while ($row = $stmt->fetch()) {
    $DATA["count"]++;

    $displayStamp = FormatTimestamp($row["seen"]);
    $displayChar = FormSearchHyperlink("character", $row["character"], $row["character"]);
    $displayAcc = HighLightMatch($DATA["search"], $row["account"]);
    $displayAcc = FormSearchHyperlink("account", $row["account"], $displayAcc);
    $displayLeague = $row["active"] ? $row["league"] : "Standard  ({$row["league"]})";

    $DATA["resultRows"][] = "<tr>
    <td>$displayAcc</td>
    <td>$displayChar</td>
    <td>$displayLeague</td>
    <td>$displayStamp</td>
    </tr>";
  }

  return $DATA;
}

// Search for accounts based on character name
function AccountSearch($pdo, $DATA) {
  $DATA["count"] = 0;

  $query = "SELECT   
    a.name AS account,
    c.name AS `character`,
    l.display AS league,
    l.active AS active,
    r.seen
  FROM (
    SELECT *
    FROM   account_characters 
    WHERE  name LIKE ? ESCAPE '='
  ) AS c
  JOIN     account_relations  AS r ON r.id_c = c.id
  JOIN     account_accounts   AS a ON r.id_a = a.id
  JOIN     data_leagues       AS l ON r.id_l = l.id
  ORDER BY r.seen DESC, c.name DESC";

  $escapedSearch = likeEscape( $DATA["search"] );
  $stmt = $pdo->prepare($query);
  $stmt->execute([ $escapedSearch ]);

  while ($row = $stmt->fetch()) {
    $DATA["count"]++;

    $displayStamp = FormatTimestamp($row["seen"]);
    $displayChar = HighLightMatch($DATA["search"], $row["character"]);
    $displayChar = FormSearchHyperlink("character", $row["character"], $displayChar);
    $displayAcc = FormSearchHyperlink("account", $row["account"], $row["account"]);
    $displayLeague = $row["active"] ? $row["league"] : "Standard  ({$row["league"]})";

    $DATA["resultRows"][] = "<tr>
      <td>$displayAcc</td>
      <td>$displayChar</td>
      <td>$displayLeague</td>
      <td>$displayStamp</td>
    </tr>";
  }

  return $DATA;
}

// Search for accounts based on account name
function TransferSearch($pdo, $DATA) {
  $DATA["count"] = 0;

  $query = "SELECT   a1.name AS oldName, a2.name AS newName, a2.found
  FROM     account_accounts AS a1
  JOIN (
    SELECT h.id_old AS id,
           h.found,
           a.name
    FROM (
      SELECT id, name
      FROM   account_accounts 
      WHERE  name LIKE ? ESCAPE '='
      LIMIT  1
    ) AS   a
    JOIN   account_history AS h 
      ON   h.id_new = a.id
  ) AS     a2 
    ON     a1.id = a2.id
  ORDER BY a2.found DESC
  LIMIT 200";

  $escapedSearch = likeEscape( $DATA["search"] );
  $stmt = $pdo->prepare($query);
  $stmt->execute([ $escapedSearch ]);

  while ($row = $stmt->fetch()) {
    $DATA["count"]++;

    $displayStamp = FormatTimestamp($row["found"]);
    $displayOldAcc = HighLightMatch($DATA["search"], $row["newName"]);
    $displayOldAcc = FormSearchHyperlink("account", $row["newName"], $displayOldAcc);
    $displayNewAcc = FormSearchHyperlink("account", $row["oldName"], $row["oldName"]);

    $DATA["resultRows"][] = "<tr>
      <td>$displayOldAcc</td>
      <td>$displayNewAcc</td>
      <td>$displayStamp</td>
    </tr>";
  }

  return $DATA;
}

//------------------------------------------------------------------------------------------------------------
// Utility functions
//------------------------------------------------------------------------------------------------------------

// Check for errors in user-provided parameters
function CheckVariableErrors($DATA) {
  // 1    - Invalid mode
  // 2    - Search string too short
  // 3    - Search string too long

  if ( empty($_GET) ) {
    return $DATA;
  }

  // User-provided mode was not in list of accepted modes
  if ( !in_array($DATA["mode"], $DATA["validModes"]) ) {
    $DATA["errorCode"] = 1;
    $DATA["errorMsg"] = "Invalid mode";
    return $DATA;
  }

  // Search string too small
  if ( $DATA["search"] !== null && strlen($DATA["search"]) < 3 ) {
    $DATA["errorCode"] = 2;
    $DATA["errorMsg"] = "Minimum 3 characters";
    return $DATA;
  }

  // Search string too large
  if ( $DATA["search"] !== null && strlen($DATA["search"]) > 42 ) {
    $DATA["errorCode"] = 3;
    $DATA["errorMsg"] = "Maximum 42 characters";
    return $DATA;
  }

  return $DATA;
}

// Escape MySQL LIKE syntax
function likeEscape($s) {
  return str_replace(array("=", "_", "%"), array("==", "=_", "=%"), $s);
}

// Turn timestamp into readable string
function FormatTimestamp($timestamp) {
  $time = new DateTime($timestamp);
  $now = new DateTime();
  $interval = $time->diff($now, true);

  if     ($interval->y) return ($interval->y === 1) ? "1 year ago"   : $interval->y . " years ago";
  elseif ($interval->m) return ($interval->m === 1) ? "1 month ago"  : $interval->m . " months ago";
  elseif ($interval->d) return ($interval->d === 1) ? "1 day ago"    : $interval->d . " days ago";
  elseif ($interval->h) return ($interval->h === 1) ? "1 hour ago"   : $interval->h . " hours ago";
  elseif ($interval->i) return ($interval->i === 1) ? "1 minute ago" : $interval->i . " minutes ago";
  else return "Moments ago";
}

function FormSearchHyperlink($mode, $search, $display) {
  return "<a href='characters?mode=$mode&search=$search'>$display</a>";
}

function FormSearchURL($mode, $search, $page) {
  return "characters?mode=$mode&search=$search&page=$page";
}

function HighLightMatch($needle, $haystack) {
  $pos = stripos($haystack, $needle);
  
  $haystack = substr_replace($haystack, "</span>", $pos + strlen($needle), 0);
  $haystack = substr_replace($haystack, "<span class='custom-text-orange'>", $pos, 0);

  return $haystack;
}
