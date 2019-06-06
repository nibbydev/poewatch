<?php
/**
 * Gets approximate table sizes for account and character
 *
 * @param $pdo PDO DB Connector
 * @return array
 */
function GetTotalCounts($pdo) {
  $query = "
  SELECT  TABLE_NAME, TABLE_ROWS 
  FROM    information_schema.TABLES 
  WHERE   table_schema = 'pw'
    AND  (table_name = 'league_characters'
    OR    table_name = 'league_accounts')
  ";

  $stmt = $pdo->query($query);
  $payload = [
    'totalChars' => 0,
    'totalAccs' => 0
  ];
  
  while ($row = $stmt->fetch()) {
    if ($row['TABLE_NAME'] === 'league_characters') {
      $payload["totalChars"] = $row["TABLE_ROWS"];
    } elseif ($row['TABLE_NAME'] === 'league_accounts') {
      $payload["totalAccs"] = $row["TABLE_ROWS"];
    }
  }

  return $payload;
}

/**
 * Given a timestamp string, returns a display string
 *
 * @param $timestamp
 * @return string
 * @throws Exception
 */
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
