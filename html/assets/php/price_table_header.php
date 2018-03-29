<?php
$pageTitle = strtolower(trim($_GET["category"]));

$sections = "<th scope='col'>Item</th>";

if ($pageTitle === "gems") {
  $sections .= "<th scope='col'>Level</th>";
  $sections .= "<th scope='col'>Quality</th>";
  $sections .= "<th scope='col'>Corrupted</th>";
}

$sections .= "<th scope='col'>Price (chaos)</th>";
$sections .= "<th scope='col'>Quantity (24h)</th>";
$sections .= "<th scope='col'>Count (total)</th>";

echo $sections;