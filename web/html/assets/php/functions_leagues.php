<?php
// Get list of leagues and their display names from DB
function GetLeagues($pdo) {
  $query = "
    SELECT name, display, start, end, active, upcoming, event
    FROM data_leagues 
    WHERE id > 2
    ORDER BY id DESC
    LIMIT 10
  ";

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
    } else if ($league["active"]) {
      $status = "<span class='badge custom-badge-green ml-1'>Ongoing</span>";
    } else {
      $status = "<span class='badge custom-badge-gray ml-1'>Ended</span>";
    }

    $title  = $league["display"] ? $league["display"] : $league["name"];
    $start  = $league["start"]   ? date('j M Y, H:i (\U\TC)', strtotime($league["start"])) : 'Unavailable';
    $end    = $league["end"]     ? date('j M Y, H:i (\U\TC)', strtotime($league["end"]))   : 'Unavailable';
    $wrap   = $league["active"] || $league["upcoming"] ? "col-md-6 col-12" : "col-xl-4 col-md-6 col-12";

    echo "
    <div class='$wrap'>
      <div class='card custom-card league-element mb-3'>
        <div class='card-header h-100'>
          <h4 class='card-title nowrap mb-0'>$title $status</h4>
        </div>
        <div class='card-body px-3 py-2'>

          <div class='row'>
            <div class='col nowrap'>
              <table>
                <tr>
                  <td class='pr-2'>Start:</td>
                  <td><span class='subtext-1'>$start</span></td>
                </tr>
                <tr>
                  <td class='pr-2'>End:</td>
                  <td><span class='subtext-1'>$end</span></td>
                </tr>
              </table>
            </div>

            <div class='col nowrap league-countdown'></div>
          </div>
          
          <div class='league-upcoming d-none' value='{$league["upcoming"]}'></div>
          <div class='league-active d-none' value='{$league["active"]}'></div>
          <div class='league-start d-none' value='{$league["start"]}'></div>
          <div class='league-end d-none' value='{$league["end"]}'></div>

        </div>
        <div class='card-footer progressbar-box border-0 p-0' style='height: 1.25rem;'>

          <div class='progressbar-bar progress-bar-striped progress-bar-animated custom-badge-green rounded-bottom h-100' style='width: 0px;'></div>
        
        </div>
        </div>
      </div>";
  }
}
