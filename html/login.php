<?php 
session_start();

if ( isset($_SESSION["logged_in"]) ) {
  unset($_SESSION["logged_in"]);
  header("location: /");
} else {
  $_SESSION["logged_in"] = True;
  header("location: prices?category=currency");
}