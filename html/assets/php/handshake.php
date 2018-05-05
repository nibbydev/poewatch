<?php

if ( isset($_POST["code"]) ) {
  if ( $_POST["code"] === "notverysecure") {
    $_SESSION["logged_in"] = True;
    unset( $_SESSION["error"] );
  } else {
    $_SESSION["error"] = True;
    unset( $_SESSION["logged_in"] );
    header('location: unavailable');
  }
}
