<?php

function getLeagues() {
  require_once "../details/pdo.php";

  $query = "
    SELECT name, display, start, end, active, upcoming, event
    FROM data_leagues 
    WHERE id > 2
    ORDER BY id DESC
    LIMIT 10
  ";

  $stmt = $pdo->query($query);
  
  $leagues = array();
  while ($row = $stmt->fetch()) {
    $leagues[] = $row;
  }

  return $leagues;
}

function formatLeagueData($leagues) {
  $formatData = array();

  for ($i = 0; $i < sizeof($leagues); $i++) { 
    $formatData[$i] = array();

    if ($leagues[$i]["upcoming"]) {
      $formatData[$i]["status"] = "<span class='badge badge-light ml-1'>Upcoming</span>";
    } else if ($leagues[$i]["active"]) {
      $formatData[$i]["status"] = "<span class='badge custom-badge-green ml-1'>Ongoing</span>";
    } else {
      $formatData[$i]["status"] = "<span class='badge custom-badge-gray ml-1'>Ended</span>";
    }
  
    $formatData[$i]["title"] = $leagues[$i]["display"] ? $leagues[$i]["display"] : $leagues[$i]["name"];
    $formatData[$i]["start"] = $leagues[$i]["start"]   ? date("j M Y, H:i (\U\TC)", strtotime($leagues[$i]["start"])) : "Unavailable";
    $formatData[$i]["end"]   = $leagues[$i]["end"]     ? date("j M Y, H:i (\U\TC)", strtotime($leagues[$i]["end"]))   : "Unavailable";
    $formatData[$i]["wrap"]  = $leagues[$i]["active"] || $leagues[$i]["upcoming"] ? "col-md-6 col-12" : "col-xl-4 col-md-6 col-12";
  }

  return $formatData;
}