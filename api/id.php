<?php 
  header("Content-Type: application/json");
  echo file_get_contents(dirname(getcwd(), 2) . "/data/changeID.json");
?>
