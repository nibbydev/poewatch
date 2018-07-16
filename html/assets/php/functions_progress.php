<?php

// Get list of leagues and their display names from DB
function GetLeagues($pdo) {
  $query = "SELECT name, display, start, end FROM data_leagues WHERE active = 1 ORDER BY id DESC";
  $stmt = $pdo->query($query);
  
  $rows = array();
  while ($row = $stmt->fetch()) {
    $rows[] = $row;
  }

  return $rows;
}
