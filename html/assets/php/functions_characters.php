<?php
function CheckGETVariableError($DATA) {
  // 0    - Request was not a GET request
  // 1    - Invalid mode
  // 2    - Search string too short
  // 3, 4 - Invalid page

  if ( empty($_GET) ) return 0;

  if ( !$DATA["mode"] ) return 1;
  if ($DATA["mode"] !== "account" && 
    $DATA["mode"] !== "character" && 
    $DATA["mode"] !== "transfer") return 1;

  if ( $DATA["search"] && strlen($DATA["search"]) < 3 ) return 2;
  if ( $DATA["search"] && strlen($DATA["search"]) > 32 ) return 2;
  if ( !$DATA["page"] ) return 3;
  if ( $DATA["pages"] && $DATA["page"] > $DATA["pages"] ) return 4;

  return 0;
}

function DisplayError($code) {
  switch ($code) {
    case 0:  $msg = "There was not error";            break;
    case 1:  $msg = "Invalid mode";                   break;
    case 2:  $msg = "Invalid search string length";   break;
    case 3:
    case 4:  $msg = "Invalid page";                   break;
    default: $msg = "Unknown error";                  break;
  }

  echo "<span class='custom-text-red'>Error: " . $msg . "</span>";
}

function DisplayMotD($DATA) {
  $accDisplay = number_format($DATA["totalAccs"]);
  $charDisplay = number_format($DATA["totalChars"]);
  $relDisplay = number_format($DATA["totalRels"]);

  $accDisplay = "<span class='custom-text-green'>$accDisplay</span>";
  $charDisplay = "<span class='custom-text-green'>$charDisplay</span>";
  $relDisplay = "<span class='custom-text-green'>$relDisplay</span>";
  $timeDisplay = "<span class='custom-text-green'>" . FormatTimestamp("2018-07-14 00:00:00") . "</span>";

  echo "Explore $accDisplay account names, $charDisplay character names and $relDisplay relations from $timeDisplay.";
}

function SetCheckboxState($DATA, $state, $mode) {
  if ($DATA["mode"] !== null) {
    echo ($DATA["mode"] === $mode) ? $state : "";
  } else if ($mode === "account") {
    echo $state;
  }
}

function FillTable($pdo, $DATA) {
  if ($DATA["mode"] === "account") {
    CharacterSearch($pdo, $DATA);
  } else if ($DATA["mode"] === "character") {
    AccountSearch($pdo, $DATA);
  } else if ($DATA["mode"] === "transfer") {
    TransferSearch($pdo, $DATA);
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
  LIMIT    ?
  OFFSET   ?";

  $escapedSearch = likeEscape($DATA["search"]);
  $offset = ($DATA["page"] - 1) * $DATA["limit"];

  // Execute get query and get the data
  $stmt = $pdo->prepare($query);
  $stmt->execute([$escapedSearch, $DATA["limit"], $offset]);

  while ($row = $stmt->fetch()) {
    $displayStamp = FormatTimestamp($row["seen"]);

    $displayChar = FormSearchHyperlink("character", $row["character"], $row["character"]);

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
    r.seen
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
  $escapedSearch = likeEscape($DATA["search"]);

  $stmt = $pdo->prepare($query);
  $stmt->execute([$escapedSearch, $DATA["limit"], $offset]);

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

function TransferSearch($pdo, $DATA) {
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
  ORDER BY a2.found DESC";

  $stmt = $pdo->prepare($query);
  $stmt->execute([ likeEscape($DATA["search"]) ]);

  while ($row = $stmt->fetch()) {
    $displayStamp = FormatTimestamp($row["found"]);

    $displayOldAcc = HighLightMatch($DATA["search"], $row["newName"]);
    $displayOldAcc = FormSearchHyperlink("account", $row["newName"], $displayOldAcc);

    $displayNewAcc = FormSearchHyperlink("account", $row["oldName"], $row["oldName"]);

    echo "<tr>
      <td>$displayOldAcc</td>
      <td>$displayNewAcc</td>
      <td>$displayStamp</td>
    </tr>";
  }
}

function likeEscape($s) {
  return str_replace(array("=", "_", "%"), array("==", "=_", "=%"), $s);
}