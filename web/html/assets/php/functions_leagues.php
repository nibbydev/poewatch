<?php
// Get list of leagues and their display names from DB
function GetLeagues($pdo) {
  $query = "(
    SELECT name, display, start, end, active, upcoming, event
    FROM      data_leagues 
    WHERE     id > 2
      AND     active = 1
      OR      upcoming = 1
    ORDER BY  id DESC
  ) UNION (
    SELECT name, display, start, end, active, upcoming, event
    FROM      data_leagues 
    WHERE     id > 2
      AND     active = 0
    ORDER BY  id DESC
    LIMIT     4
  )";
  $stmt = $pdo->query($query);
  
  $rows = array();
  while ($row = $stmt->fetch()) {
    $rows[] = $row;
  }

  return $rows;
}

function GenLeagueEntries($pdo) {
  $leagues = GetLeagues($pdo);

  foreach($leagues as $league) {
    if ($league["upcoming"]) {
      $status = "<span class='badge badge-light ml-1'>Upcoming</span>";
    } else if (!$league["active"]) {
      $status = "<span class='badge custom-badge-gray-lo ml-1'>Ended</span>";
    } else {
      $status = "<span class='badge badge-success ml-1'>Ongoing</span>";
    }

    $title  = $league["display"] ? $league["display"] : $league["name"];
    $start  = $league["start"]   ? date('j M Y, H:i (\U\TC)', strtotime($league["start"])) : 'Unavailable';
    $end    = $league["end"]     ? date('j M Y, H:i (\U\TC)', strtotime($league["end"]))   : 'Unavailable';

    echo "
    <div class='league-element col-md-6 mb-4'>
      <h4>$title $status</h4>
      <div class='mb-1'>Start: <span class='subtext-1'>$start</span></div>
      <div class='mb-1'>End: <span class='subtext-1'>$end</span></div>
      <div class='league-description mt-3 mb-0'> </div>
      <div class='progressbar-box rounded'><div class='progressbar-bar rounded h-100'></div></div>
      <div class='league-start d-none' value='{$league["start"]}'></div>
      <div class='league-end d-none' value='{$league["end"]}'></div>
    </div>";
  }
}
