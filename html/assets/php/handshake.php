<?php

include_once ("config.php");

if ( isset($_POST["code"]) ) {
  if ( in_array($_POST["code"], $randomStrings) ) {
    $_SESSION["logged_in"] = True;
    unset( $_SESSION["error"] );
  } else {
    $_SESSION["error"] = True;
    unset( $_SESSION["logged_in"] );
    header('location: unavailable');
  }
}
