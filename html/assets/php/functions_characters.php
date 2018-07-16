<?php
function CheckGETVariableError($DATA) {
  // 0 - Request was not a GET request
  // 1 - Invalid mode
  // 2 - Search string too short
  // 3 - Invalid page

  if ( empty($_GET) ) return 0;
  if ( !$DATA["mode"] || $DATA["mode"] !== "account" && $DATA["mode"] !== "character") return 1;

  if ( !$DATA["search"] || strlen($DATA["search"]) < 3 ) return 2;

  if ( !$DATA["page"] || $DATA["page"] < 1 || $DATA["page"] > 999 ) return 3;

  return 0;
}

function DisplayError($code) {
  switch ($code) {
    case 0:  $msg = "There was not error";            break;
    case 1:  $msg = "Invalid mode";                   break;
    case 2:  $msg = "Minimum length is 3 characters"; break;
    case 3:  $msg = "Invalid page";                   break;
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

  $accDisplay = "<span class='custom-text-green'>{$row["accCount"]}</span>";
  $charDisplay = "<span class='custom-text-green'>{$row["charCount"]}</span>";
  $timeDisplay = "<span class='custom-text-green'>" . FormatTimestamp("2018-07-14 00:00:00") . "</span>";

  echo "Explore $accDisplay account names, $charDisplay character names and their history since $timeDisplay.";
}

function SetCheckboxState($DATA, $state, $mode) {
  if ($DATA["mode"] !== null) {
    echo ($DATA["mode"] === $mode) ? $state : "";
  } else if ($mode === "account") {
    echo $state;
  }
}


function FillTable($pdo, $DATA) {
  $len = strlen($DATA["search"]);

  if ($len > 2 && $DATA["mode"] === "account") {
    CharacterSearch($pdo, $DATA);
  } else if ($len > 2 && $DATA["mode"] === "character") {
    AccountSearch($pdo, $DATA);
  } else {
    GetData($pdo, $DATA);
  }
}

function DisplayResultCount($DATA) {
  if ($DATA["count"] === null) return;

  $countDisplay = "<span class='custom-text-green'>{$DATA["count"]}</span>";
  $nameDisplay = "<span class='custom-text-orange'>{$DATA["search"]}</span>";

  echo "$countDisplay matches for {$DATA["mode"]} names containing '$nameDisplay'";
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
  if ($DATA["mode"] === "account") {
    return CharacterCount($pdo, $DATA["search"]);
  } else if ($DATA["mode"] === "character") {
    return AccountCount($pdo, $DATA["search"]);
  }
}

function DisplayPagination($DATA) {
  $pageCount = ceil($DATA["count"] / $DATA["limit"]);

  if ($DATA["page"] > 1) {
    $url = FormSearchURL($DATA["mode"], $DATA["search"], $DATA["page"] - 1);
    echo "<a class='btn btn-outline-dark' href='$url'>«</a>";
  }

  if ($pageCount > 1) {
    $counter = $DATA["page"] . " / " . $pageCount;
    echo "<div class='mx-3 d-flex'><span class='justify-content-center align-self-center'>$counter</span></div>";
  }

  if ($DATA["page"] < $pageCount) {
    $url = FormSearchURL($DATA["mode"], $DATA["search"], $DATA["page"] + 1);
    echo "<a class='btn btn-outline-dark' href='$url'>»</a>";
  }
}

function GetData($pdo, $DATA) {
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
  LIMIT    ?
  OFFSET   ?";

  $offset = ($DATA["page"] - 1) * $DATA["limit"];

  $stmt = $pdo->prepare($query);
  $stmt->execute([$DATA["limit"], $offset]);

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
  WHERE    a.name LIKE ? ESCAPE '='";

  $preppedString = "%" . likeEscape($name) . "%";

  // Execute count query and see how many results there are
  $stmt = $pdo->prepare($query);
  $stmt->execute([$preppedString]);
  return $stmt->fetch()["count"];
}
// Search based on account name
function CharacterSearch($pdo, $DATA) {
  $query = "SELECT
	  a.name AS account, 
    c.name AS `character`,
    l.display AS league,
    r.seen,
    a.hidden
  FROM     account_relations  AS r
  JOIN     account_accounts   AS a ON a.id = r.id_a
  JOIN     account_characters AS c ON c.id = r.id_c
  JOIN     data_leagues       AS l ON r.id_l = l.id
  WHERE    a.name LIKE ? ESCAPE '='
  ORDER BY r.seen DESC
  LIMIT    ?
  OFFSET   ?";

  $preppedString = "%" . likeEscape($DATA["search"]) . "%";
  $offset = ($DATA["page"] - 1) * $DATA["limit"];

  // Execute get query and get the data
  $stmt = $pdo->prepare($query);
  $stmt->execute([$preppedString, $DATA["limit"], $offset]);

  while ($row = $stmt->fetch()) {
    $displayStamp = FormatTimestamp($row["seen"]);

    if ($row["hidden"]) $displayChar = "<span class='custom-text-dark'>Requested privacy</span>";
    else $displayChar = FormSearchHyperlink("character", $row["character"], $row["character"]);

    $displayAcc = HighLightMatch($DATA["search"], $row["account"]);
    $displayAcc = FormSearchHyperlink("account", $row["account"], $displayAcc);

    echo "<tr>
    <td>$displayAcc</td>
    <td>$displayChar</td>
    <td>{$row["league"]}</td>
    <td>$displayStamp</td>
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
  WHERE    c.name LIKE ? ESCAPE '=' AND a.hidden = 0";

  $preppedString = "%" . likeEscape($name) . "%";

  // Execute count query and see how many results there are
  $stmt = $pdo->prepare($query);
  $stmt->execute([$preppedString]);
  return $stmt->fetch()["count"];
}
// Search based on character name
function AccountSearch($pdo, $DATA) {
  $query = "SELECT   
    a.name AS account,
	  c.name AS `character`,
    l.display AS league,
    r.seen
  FROM     account_relations  AS r
  JOIN     account_accounts   AS a ON a.id   = r.id_a
  JOIN     account_characters AS c ON c.id   = r.id_c
  JOIN     data_leagues       AS l ON r.id_l = l.id
  WHERE    c.name LIKE ? ESCAPE '=' AND a.hidden = 0
  ORDER BY r.seen DESC
  LIMIT    ?
  OFFSET   ?";

  $preppedString = "%" . likeEscape($DATA["search"]) . "%";
  $offset = ($DATA["page"] - 1) * $DATA["limit"];

  $stmt = $pdo->prepare($query);
  $stmt->execute([$preppedString, $DATA["limit"], $offset]);

  while ($row = $stmt->fetch()) {
    $displayStamp = FormatTimestamp($row["seen"]);

    $displayChar = HighLightMatch($DATA["search"], $row["character"]);
    $displayChar = FormSearchHyperlink("character", $row["character"], $displayChar);

    $displayAcc = FormSearchHyperlink("account", $row["account"], $row["account"]);

    echo "<tr>
      <td>$displayAcc</td>
      <td>$displayChar</td>
      <td>{$row["league"]}</td>
      <td>$displayStamp</td>
    </tr>";
  }
}

function likeEscape($s) {
  return str_replace(array("=", "_", "%"), array("==", "=_", "=%"), $s);
}