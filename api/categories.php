<?php
  // Set header to json
  header("Content-Type: application/json");

  // Get and echo file contents
  echo file_get_contents(dirname(getcwd(), 2) . "/data/categories.json");
?>