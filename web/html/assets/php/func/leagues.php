<?php
/**
 * Queries leagues from DB
 *
 * @param $pdo PDO DB connector
 * @return array League list
 */
function getLeagues($pdo) {
  $stmt = $pdo->query("select name, display, start, end, active, upcoming, event from data_leagues order by id desc");

  $leagues = [];
  while ($row = $stmt->fetch()) {
    $leagues[] = $row;
  }

  return $leagues;
}

/**
 * Converts timestamp string to display string
 *
 * @param $time string ISO 8601 UTC time string
 * @return false|string
 */
function formatTimestamp($time) {
  return $time ? date("j M Y, H:i (\U\TC)", strtotime($time)) : "Unavailable";
}

/**
 * Formats database league data for display
 *
 * @param $leagues array League list
 * @return array Formatted league list
 */
function formatLeagueData($leagues) {
  $formatData = [
    "main" => [],
    "upcoming" => [],
    "inactive" => []
  ];

  foreach ($leagues as $league) {
    $payload = [
      "title" => $league["display"] ? $league["display"] : $league["name"],
      "startDisplay" => formatTimestamp($league["start"]),
      "endDisplay" => formatTimestamp($league["end"]),
      "start" => $league["start"],
      "end" => $league["end"],
      "upcoming" => $league["upcoming"],
      "cdTitle1" => $league["upcoming"] ? "Starts in:" : "Elapsed:",
      "cdTitle2" => $league["upcoming"] ? "Ends in:" : "Remaining:",
    ];

    // Format title colors
    if ($league["active"]) {
      $payload["title"] = "<span class='custom-text-green'>{$payload["title"]}</span>";
    } else if ($league["upcoming"]) {
      $payload["title"] = "<span class='subtext-0'>{$payload["title"]}</span>";
    } else {
      $payload["title"] = "<span class='subtext-0'>{$payload["title"]}</span>";
    }

    // Sort to groups
    if ($league["upcoming"]) {
      $formatData["upcoming"][] = $payload;
    } else if ($league["active"]) {
      $formatData["main"][] = $payload;
    } else {
      $formatData["inactive"][] = $payload;
    }
  }

  return $formatData;
}

/**
 * Gets and formats league data from database
 *
 * @param $pdo PDO DB connector
 * @return array Formatted league list
 */
function getLeagueData($pdo) {
  return formatLeagueData(getLeagues($pdo));
}
