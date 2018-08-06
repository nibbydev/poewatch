<?php
function error($code, $msg) {
  http_response_code($code);
  die( json_encode( array("error" => $msg) ) );
}

function get_data($pdo, $id) {
  $query = "SELECT 
    l.id AS id_l, l.name AS league, 
    i.mean, i.median, i.mode, i.exalted, i.count, i.quantity,
    d.name, d.type, d.frame, d.icon,
    d.tier, d.lvl, d.quality, d.corrupted, d.links, d.var,
    cc.name AS cc, cp.name AS cp,
    GROUP_CONCAT(h.mean      ORDER BY h.time DESC) AS mean_list,
    GROUP_CONCAT(h.median    ORDER BY h.time DESC) AS median_list,
    GROUP_CONCAT(h.mode      ORDER BY h.time DESC) AS mode_list,
    GROUP_CONCAT(h.quantity  ORDER BY h.time DESC) AS quantity_list,
    GROUP_CONCAT(h.inc       ORDER BY h.time DESC) AS inc_list,
    GROUP_CONCAT(h.time      ORDER BY h.time DESC) AS time_list
  FROM      league_history_daily_rolling AS h
  JOIN      data_leagues    AS l  ON h.id_l  = l.id
  JOIN      league_items    AS i  ON i.id_l  = l.id AND i.id_d = h.id_d
  JOIN      data_itemData   AS d  ON i.id_d  = d.id 
  JOIN      category_parent AS cp ON d.id_cp = cp.id 
  LEFT JOIN category_child  AS cc ON d.id_cc = cc.id 
  WHERE     h.id_d = ?
  GROUP BY  h.id_l, h.id_d";

  $stmt = $pdo->prepare($query);
  $stmt->execute([$id]);

  return $stmt;
}

function parse_data($stmt) {
  $payload = array();

  while ($row = $stmt->fetch()) {
    // Convert CSVs to arrays
    $means    = explode(',', $row['mean_list']);
    $medians  = explode(',', $row['median_list']);
    $modes    = explode(',', $row['mode_list']);
    $quants   = explode(',', $row['quantity_list']);
    $incs     = explode(',', $row['inc_list']);
    $times    = explode(',', $row['time_list']);

    // Form a temporary entry array
    $tmp = array(
      'leagueId'      => (int)    $row['id_l'],
      'league'        =>          $row['league'],
      'mean'          => (float)  $row['mean'],
      'median'        => (float)  $row['median'],
      'mode'          => (float)  $row['mode'],
      'exalted'       => (float)  $row['exalted'],
      'count'         => (int)    $row['count'],
      'quantity'      => (int)    $row['quantity'],
      'name'          =>          $row['name'],
      'type'          =>          $row['type'],
      'frame'         => (int)    $row['frame'],
      'icon'          =>          $row['icon'],
      'tier'          =>          $row['tier']      === null ? null : (int)   $row['tier'],
      'lvl'           =>          $row['lvl']       === null ? null : (int)   $row['lvl'],
      'quality'       =>          $row['quality']   === null ? null : (int)   $row['quality'],
      'corrupted'     =>          $row['corrupted'] === null ? null : (bool)  $row['corrupted'],
      'links'         =>          $row['links']     === null ? null : (int)   $row['links'],
      'variation'     =>          $row['var'],
      'categoryParent'=>          $row['cp'],
      'categoryChild' =>          $row['cc'],
      'history'       =>          array()
    );

    for ($i = 0; $i < sizeof($means); $i++) { 
      $tmp['history'][ $times[$i] ] = array(
        'mean'     => (float) $means[$i],
        'median'   => (float) $medians[$i],
        'mode'     => (float) $modes[$i],
        'quantity' => (int)   $quants[$i],
        'inc'      => (int)   $incs[$i]
      );
    }

    $payload[] = $tmp;
  }

  return $payload;
}

// Define content type
header("Content-Type: application/json");

// Get parameters
if (!isset($_GET["id"])) error(400, "Missing id parameter");

// Connect to database
include_once ( "details/pdo.php" );

// Get data from database
$stmt = get_data($pdo, $_GET["id"]);

// If no results with provided id
if ($stmt->rowCount() === 0) error(400, "Invalid id parameter");

// Parse received data
$payload = parse_data($stmt);

// Display generated data
echo json_encode($payload, JSON_PRESERVE_ZERO_FRACTION);
