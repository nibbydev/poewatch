<?php

// Get list of leagues and their display names from DB
function GetLeagues($pdo) {
  $query = "SELECT name, display, start, end FROM `sys-leagues`";
  $stmt = $pdo->query($query);
  
  $payload = array();
  
  while ($row = $stmt->fetch()) {
    $payload[] = $row;
  }

  return $payload;
}