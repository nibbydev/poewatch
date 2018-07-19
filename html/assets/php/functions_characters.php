<?php
function CheckGETVariableError($DATA) {
  // 0    - Request was not a GET request
  // 1    - Invalid mode
  // 2    - Search string too short
  // 3, 4 - Invalid page

  if ( empty($_GET) ) return 0;
  if ( !$DATA["mode"] || $DATA["mode"] !== "account" && $DATA["mode"] !== "character") return 1;
  if ( $DATA["search"] && strlen($DATA["search"]) < 3 ) return 2;
  if ( !$DATA["page"] ) return 3;
  if ( $DATA["pages"] && $DATA["page"] > $DATA["pages"] ) return 4;

  return 0;
}


function DisplayError($code) {
  switch ($code) {
    case 0:  $msg = "There was not error";            break;
    case 1:  $msg = "Invalid mode";                   break;
    case 2:  $msg = "Minimum length is 3 characters"; break;
    case 3:
    case 4:  $msg = "Invalid page";                   break;
    default: $msg = "Unknown error";                  break;
  }

  echo "<span class='custom-text-red'>Error: " . $msg . "</span>";
}

function DisplayMotD($DATA) {
  $accDisplay = "<span class='custom-text-green'>{$DATA["totalAccs"]}</span>";
  $charDisplay = "<span class='custom-text-green'>{$DATA["totalChars"]}</span>";
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
  $results = $DATA["count"] === 1 ? "result" : "results";

  echo "$countDisplay $results for {$DATA["mode"]} names matching '$nameDisplay'";
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
    return CharacterCount($pdo, $DATA);
  } else if ($DATA["mode"] === "character") {
    return AccountCount($pdo, $DATA);
  }
}

function DisplayPagination($DATA) {
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
}


function GetTotalCounts($pdo, $DATA) {
  $query = "SELECT
  (SELECT COUNT(*) FROM account_accounts  ) AS accCount, 
  (SELECT COUNT(*) FROM account_relations ) AS relCount, 
  (SELECT COUNT(*) FROM account_characters) AS charCount";

  $stmt = $pdo->query($query);
  $row = $stmt->fetch();

  $DATA["totalAccs"]  = $row["accCount"];
  $DATA["totalRels"]  = $row["relCount"];
  $DATA["totalChars"] = $row["charCount"];

  return $DATA;
}


function GetData($pdo, $DATA) {
  $query = "SELECT
    a.name AS account,
    c.name AS `character`, 
    l.display AS league,
    r.seen
  FROM (
    SELECT   *
    FROM     account_relations 
    ORDER BY seen DESC 
    LIMIT    ?
    OFFSET   ?
  ) AS r
  JOIN     account_accounts   AS a ON a.id = r.id_a
  JOIN     account_characters AS c ON c.id = r.id_c
  JOIN     data_leagues       AS l ON l.id = r.id_l";

  $offset = ($DATA["page"] - 1) * $DATA["limit"];

  $stmt = $pdo->prepare($query);
  $stmt->execute([$DATA["limit"], $offset]);

  while ($row = $stmt->fetch()) {
    $timestamp = FormatTimestamp($row["seen"]);

    $displayAcc  = FormSearchHyperlink("account",   $row["account"],   $row["account"]);
    $displayChar = FormSearchHyperlink("character", $row["character"], $row["character"]);

    echo "<tr>
      <td>$displayAcc</td>
      <td>$displayChar</td>
      <td>{$row["league"]}</td>
      <td>$timestamp</td>
    </tr>";
  }
}

// Search based on account name
function CharacterCount($pdo, $DATA) {
  $query = "SELECT COUNT(*) AS count 
  FROM   account_relations
  WHERE  id_a = (
    SELECT id 
    FROM   account_accounts 
    WHERE  name LIKE ? ESCAPE '=' 
    LIMIT  1
  )";

  // Execute count query and see how many results there are
  $stmt = $pdo->prepare($query);
  $stmt->execute([likeEscape($DATA["search"])]);
  return $stmt->fetch()["count"];
}
// Search based on account name
function CharacterSearch($pdo, $DATA) {
  $query = "SELECT   
    a.name AS account,
    c.name AS `character`,
    l.display AS league,
    r.seen,
    a.hidden,
    r.inactive
  FROM (
    SELECT *
    FROM   account_accounts 
    WHERE  name LIKE ? ESCAPE '=' 
  ) AS a
  JOIN     account_relations  AS r ON r.id_a = a.id
  JOIN     account_characters AS c ON r.id_c = c.id
  JOIN     data_leagues       AS l ON r.id_l = l.id
  ORDER BY r.seen DESC, c.name DESC
  LIMIT    ?
  OFFSET   ?";

  $preppedString = likeEscape($DATA["search"]);
  $offset = ($DATA["page"] - 1) * $DATA["limit"];

  // Execute get query and get the data
  $stmt = $pdo->prepare($query);
  $stmt->execute([$preppedString, $DATA["limit"], $offset]);

  while ($row = $stmt->fetch()) {
    $displayStamp = FormatTimestamp($row["seen"]);

    if ($row["hidden"]) {
      $displayChar = "<span class='custom-text-dark'>Requested privacy</span>";
    } else {
      $tmp = $row["inactive"] ? "<span class='custom-text-dark'>{$row["character"]}</span>" : $row["character"];
      $displayChar = FormSearchHyperlink("character", $row["character"], $tmp);
    }

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
function AccountCount($pdo, $DATA) {
  $query = "SELECT COUNT(*) AS count 
  FROM   account_relations
  WHERE  id_c = (
    SELECT id 
    FROM   account_characters 
    WHERE  name LIKE ? ESCAPE '=' 
    LIMIT  1
  )";

  // Execute count query and see how many results there are
  $stmt = $pdo->prepare($query);
  $stmt->execute([likeEscape($DATA["search"])]);
  return $stmt->fetch()["count"];
}
// Search based on character name
function AccountSearch($pdo, $DATA) {
  $query = "SELECT   
    a.name AS account,
    c.name AS `character`,
    l.display AS league,
    r.seen,
    r.inactive
  FROM (
    SELECT *
    FROM   account_characters 
    WHERE  name LIKE ? ESCAPE '='
  ) AS c
  JOIN     account_relations  AS r ON r.id_c = c.id
  JOIN     account_accounts   AS a ON r.id_a = a.id
  JOIN     data_leagues       AS l ON r.id_l = l.id
  ORDER BY r.seen DESC, c.name DESC
  LIMIT    ?
  OFFSET   ?";

  $offset = ($DATA["page"] - 1) * $DATA["limit"];

  $stmt = $pdo->prepare($query);
  $stmt->execute([likeEscape($DATA["search"]), $DATA["limit"], $offset]);

  while ($row = $stmt->fetch()) {
    $displayStamp = FormatTimestamp($row["seen"]);

    if ($row["inactive"]) {
      $displayChar = "<span class='custom-text-dark'>{$row["character"]}</span>";
      $displayChar = FormSearchHyperlink("character", $row["character"], $displayChar);
    } else {
      $displayChar = HighLightMatch($DATA["search"], $row["character"]);
      $displayChar = FormSearchHyperlink("character", $row["character"], $displayChar);
    }

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