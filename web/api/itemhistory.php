<?php
function error($code, $msg) {
  http_response_code($code);
  die( json_encode( array("error" => $msg) ) );
}

function check_errors() {
  if (!isset($_GET["id"])) {
    error(400, "Missing id");
  }

  if (!ctype_digit($_GET["id"])) {
    error(400, "Invalid id");
  }

  if ( !isset($_GET["league"]) )    {
    error(400, "Missing league");
  }

  if ( strlen($_GET["league"]) > 64 )    {
    error(400, "League too long");
  }

  if ( strlen($_GET["league"]) < 3 )    {
    error(400, "League too short");
  }
}

function get_data($pdo, $league, $id) {
  $query = "
    select * from (
      select lhd.mean, lhd.median, lhd.mode, 
             lhd.daily, lhd.current, lhd.accepted,
             DATE_FORMAT(lhd.time, '%Y-%m-%dT%H:00:00Z') as time
      from league_history_daily as lhd
      join data_leagues as l 
        on lhd.id_l = l.id
      where l.name = ? 
        and lhd.id_d = ?
      order by lhd.time desc
      limit 120
    ) as foo order by foo.time asc";

  $stmt = $pdo->prepare($query);
  $stmt->execute([$league, $id]);
  $payload = [];

  while ($row = $stmt->fetch()) {
    $payload[] = [
      'time'     =>         $row["time"],
      'mean'     => (float) $row["mean"],
      'median'   => (float) $row["median"],
      'mode'     => (float) $row["mode"],
      'daily'    => (int)   $row["daily"],
      'current'  => (int)   $row["current"],
      'accepted' => (int)   $row["accepted"]
    ];
  }

  return $payload;
}


header("Content-Type: application/json");
check_errors();
include_once ( "../details/pdo.php" );
$payload = get_data($pdo, $_GET["league"], $_GET["id"]);
echo json_encode($payload, JSON_PRESERVE_ZERO_FRACTION | JSON_PRETTY_PRINT);
