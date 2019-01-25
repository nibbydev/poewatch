<?php
function getLeagues() {
  require_once "../details/pdo.php";

  $query = "
    SELECT name, display, start, end, active, upcoming, event
    FROM data_leagues
    ORDER BY id DESC
  ";

  $stmt = $pdo->query($query);
  
  $leagues = array();
  while ($row = $stmt->fetch()) {
    $leagues[] = $row;
  }

  return $leagues;
}

function formatTimestamp($time) {
  if (!$time) {
    return "Unavailable";
  }

  return date("j M Y, H:i (\U\TC)", strtotime($time));
}

function formatLeagueData($leagues) {
  $formatData = array(
    "main" => array(),
    "other" => array(),
    "inactive" => array()
  );

  foreach($leagues as $league) {
    // Format payload
    $payload = array(
      "title"         => $league["display"] ? $league["display"] : $league["name"],
      "startDisplay"  => formatTimestamp($league["start"]),
      "endDisplay"    => formatTimestamp($league["end"]),
      "start"         => $league["start"],
      "end"           => $league["end"],
      "upcoming"      => $league["upcoming"],
      "cdTitle1"      => $league["upcoming"] ? "Starts in:" : "Elapsed:",
      "cdTitle2"      => $league["upcoming"] ? "Ends in:" : "Remaining:",
    );

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
      $formatData["other"][] = $payload;
    } else if ($league["active"]) {
      $formatData["main"][] = $payload;
    } else {
      $formatData["inactive"][] = $payload;
    }
  }

  return $formatData;
}