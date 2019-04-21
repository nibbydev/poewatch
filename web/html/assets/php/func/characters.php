<?php
// Check for errors in user-provided parameters
function CheckQueryParamErrors() {
  global $PAGEDATA;

  // Was not a search
  if (empty($_GET)) {
    return;
  }

  // Mode was not in list of accepted modes
  if (!in_array($PAGEDATA["page"]["searchMode"], array("account", "character"))) {
    $PAGEDATA["page"]["errorMsg"] = "Invalid mode";
    return;
  }

  // Search string not provided
  if ($PAGEDATA["page"]["searchString"] === null) {
    $PAGEDATA["page"]["errorMsg"] = "No username defined";
    return;
  }

  // Search string too short
  if (strlen($PAGEDATA["page"]["searchString"]) < 3) {
    $PAGEDATA["page"]["errorMsg"] = "Search string too short";
    return;
  }

  // Search string too long
  if (strlen($PAGEDATA["page"]["searchString"]) > 42) {
    $PAGEDATA["page"]["errorMsg"] = "Search string too long";
    return;
  }
}

// Get table sizes
function GetTotalCounts($pdo) {
  global $PAGEDATA;

  $query = "
  SELECT  TABLE_NAME, TABLE_ROWS 
  FROM    information_schema.TABLES 
  WHERE   table_schema = 'pw'
    AND  (table_name = 'account_characters'
    OR    table_name = 'account_accounts')
  ";

  $stmt = $pdo->query($query);
  
  while ($row = $stmt->fetch()) {
    switch ($row['TABLE_NAME']) {
      case 'account_characters':
        $PAGEDATA["page"]["totalChars"] = $row["TABLE_ROWS"];
        break;

      case 'account_accounts':
        $PAGEDATA["page"]["totalAccs"] = $row["TABLE_ROWS"];
        break;

      default:
        break;
    }
  }
}


//------------------------------------------------------------------------------------------------------------
// DB queries
//------------------------------------------------------------------------------------------------------------

// Pick a function based on mode
function MakeSearch($pdo) {
  global $PAGEDATA;
  
  if ($PAGEDATA["page"]["errorMsg"] || 
    $PAGEDATA["page"]["searchString"] === null || 
    $PAGEDATA["page"]["searchMode"] === null) {
    return;
  }
  
  switch ($PAGEDATA["page"]["searchMode"]) {
    case 'account': 
      CharacterSearch($pdo);
      return;
    case 'character': 
      AccountSearch($pdo);
      return;
    default: 
      return;
  }
}

// Search for characters based on account name
function CharacterSearch($pdo) {
  global $PAGEDATA;

  $query = "
    select   
      a.name as account,
      c.name as `character`,
      l.display as league,
      l.active as active,
      c.seen
    from (
      select *
      from account_accounts 
      where name like ? escape '=' 
    ) as a
    join     account_characters as c on c.id_a = a.id
    join     data_leagues       as l on c.id_l = l.id
    order by c.seen desc, c.name desc
    limit 128
  ";

  $stmt = $pdo->prepare($query);
  $stmt->execute([likeEscape($PAGEDATA["page"]["searchString"])]);

  while ($row = $stmt->fetch()) {
    $PAGEDATA["page"]["searchResults"][] = $row;
  }
}

// Search for accounts based on character name
function AccountSearch($pdo) {
  global $PAGEDATA;

  $query = "
    select   
      a.name as account,
      c.name as `character`,
      l.display as league,
      l.active as active,
      c.seen
    from (
      select *
      from account_characters 
      where name like ? escape '=' 
    ) as c
    join     account_accounts as a on c.id_a = a.id
    join     data_leagues     as l on c.id_l = l.id
    order by c.seen desc, c.name desc
    limit 128
  ";

  $stmt = $pdo->prepare($query);
  $stmt->execute([likeEscape($PAGEDATA["page"]["searchString"])]);

  while ($row = $stmt->fetch()) {
    $PAGEDATA["page"]["searchResults"][] = $row;
  }
}

//------------------------------------------------------------------------------------------------------------
// Utility functions
//------------------------------------------------------------------------------------------------------------

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
