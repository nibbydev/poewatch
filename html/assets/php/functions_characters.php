<?php
function CheckPOSTVariableError() {
  // 0 - Request was not a POST request
  // 1 - POST request didn't contain expected fields
  // 2 - POST name field was too short

  if ( empty($_POST) ) return 0;
  if ( !isset($_POST["type"]) ) return 1;
  if ( !isset($_POST["name"]) ) return 1;

  if ( $_POST["name"] && strlen($_POST["name"]) < 3 ) return 2;

  return 0;
}

function DisplayError($code) {
  switch ($code) {
    case 0:  $msg = "There was not error";            break;
    case 1:  $msg = "Missing search fields";          break;
    case 2:  $msg = "Minimum length is 3 characters"; break;
    default: $msg = "Unknown error";                  break;
  }

  echo "<span class='custom-text-red'>Error: " . $msg . "</span>";
}

function DisplayMotD($pdo) {
  $query = "SELECT
  (SELECT FORMAT(COUNT(*), 0) FROM account_accounts  ) AS accCount, 
  (SELECT FORMAT(COUNT(*), 0) FROM account_characters) AS charCount";

  $stmt = $pdo->query($query);
  $row = $stmt->fetch();

  echo "Explore <span class='custom-text-green'>{$row["accCount"]}</span> account names, <span class='custom-text-green'>{$row["charCount"]}</span> character names and their history";
}

function SetCheckboxState($mode, $type) {
  if (isset($_POST["type"])) {
    echo ($_POST["type"] === $type) ? $mode : "";
  } else if ($type === "account") {
    echo $mode;
  }
}


function FillTable($pdo, $DATA) {
  $len = strlen($DATA["searchString"]);

  if ($len > 2 && $DATA["searchType"] === "account") {
    CharacterSearch($pdo, $DATA["searchString"], 0);
  } else if ($len > 2 && $DATA["searchType"] === "character") {
    AccountSearch($pdo, $DATA["searchString"], 0);
  } else {
    GetData($pdo, 0);
  }
}

function DisplayResultCount($DATA) {
  if ($DATA["resultCount"] !== null) {
    echo "<span class='custom-text-green'>{$DATA["resultCount"]}</span> matches for string '{$DATA["searchString"]}' in {$DATA["searchType"]}s";
  }
}


function HighLightMatch($needle, $haystack) {
  $pos = stripos($haystack, $needle);
  
  $haystack = substr_replace($haystack, "</span>", $pos + strlen($needle), 0);
  $haystack = substr_replace($haystack, "<span class='custom-text-orange'>", $pos, 0);

  return $haystack;
}

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

function GetResultCount($pdo, $DATA) {
  if ($DATA["searchType"] === "account") {
    return CharacterCount($pdo, $_POST["name"]);
  } else if ($DATA["searchType"] === "character") {
    return AccountCount($pdo, $_POST["name"]);
  }
}

function DisplayPagination() {

}




function GetData($pdo, $offset) {
  $query = "SELECT
    a.name AS account,
	  c.name AS `character`, 
    l.display AS league,
    r.seen
  FROM     account_relations  AS r
  JOIN     account_accounts   AS a ON a.id   = r.id_a
  JOIN     account_characters AS c ON c.id   = r.id_c
  JOIN     data_leagues       AS l ON r.id_l = l.id
  ORDER BY r.seen DESC
  LIMIT    15
  OFFSET   ?";

  $stmt = $pdo->prepare($query);
  $stmt->execute([$offset]);

  while ($row = $stmt->fetch()) {
    $timestamp = FormatTimestamp($row["seen"]);

    echo "<tr>
      <td>{$row["account"]}</td>
      <td>{$row["character"]}</td>
      <td>{$row["league"]}</td>
      <td>$timestamp</td>
    </tr>";
  }
}

// Search based on account name
function CharacterCount($pdo, $name) {
  $query = "SELECT COUNT(*) AS count
  FROM     account_relations  AS r
  JOIN     account_accounts   AS a ON a.id = r.id_a
  JOIN     account_characters AS c ON c.id = r.id_c
  JOIN     data_leagues       AS l ON r.id_l = l.id
  WHERE    a.name LIKE ?";

  // Execute count query and see how many results there are
  $stmt = $pdo->prepare($query);
  $stmt->execute(["%$name%"]);
  return $stmt->fetch()["count"];
}
// Search based on account name
function CharacterSearch($pdo, $name, $offset) {
  $query = "SELECT   
	  a.name AS account, 
    c.name AS `character`,
    l.display AS league,
    r.seen
  FROM     account_relations  AS r
  JOIN     account_accounts   AS a ON a.id = r.id_a
  JOIN     account_characters AS c ON c.id = r.id_c
  JOIN     data_leagues       AS l ON r.id_l = l.id
  WHERE    a.name LIKE ?
  ORDER BY a.name, r.seen
  LIMIT    15
  OFFSET   ?";

  // Execute get query and get the data
  $stmt = $pdo->prepare($query);
  $stmt->execute(["%$name%", $offset]);

  while ($row = $stmt->fetch()) {
    $highlighted = HighLightMatch($name, $row["account"]);
    $timestamp = FormatTimestamp($row["seen"]);

    echo "<tr>
    <td>$highlighted</td>
    <td>{$row["character"]}</td>
    <td>{$row["league"]}</td>
    <td>$timestamp</td>
    </tr>";
  }
}

// Search based on character name
function AccountCount($pdo, $name) {
  $query = "SELECT COUNT(*) AS count
  FROM     account_relations  AS r
  JOIN     account_accounts   AS a ON a.id = r.id_a
  JOIN     account_characters AS c ON c.id = r.id_c
  JOIN     data_leagues       AS l ON r.id_l = l.id
  WHERE    c.name LIKE ?";

  // Execute count query and see how many results there are
  $stmt = $pdo->prepare($query);
  $stmt->execute(["%$name%"]);
  return $stmt->fetch()["count"];
}
// Search based on character name
function AccountSearch($pdo, $name, $offset) {
  $query = "SELECT   
    a.name AS account,
	  c.name AS `character`,
    l.display AS league,
    r.seen
  FROM     account_relations  AS r
  JOIN     account_accounts   AS a ON a.id   = r.id_a
  JOIN     account_characters AS c ON c.id   = r.id_c
  JOIN     data_leagues       AS l ON r.id_l = l.id
  WHERE    c.name LIKE ?
  ORDER BY c.name, r.seen
  LIMIT    15
  OFFSET   ?";

  $stmt = $pdo->prepare($query);
  $stmt->execute(["%$name%", $offset]);

  while ($row = $stmt->fetch()) {
    $highlighted = HighLightMatch($name, $row["character"]);
    $timestamp = FormatTimestamp($row["seen"]);

    echo "<tr>
      <td>{$row["account"]}</td>
      <td>$highlighted</td>
      <td>{$row["league"]}</td>
      <td>$timestamp</td>
    </tr>";
  }
}
