<?php
$pageTitle = strtolower(trim($_GET["category"]));

$sections = "<th scope='col'>Item</th>";

if ($pageTitle === "gems") {
  $sections .= "<th scope='col'>Lvl</th>";
  $sections .= "<th scope='col'>Qual</th>";
  $sections .= "<th scope='col'>Corr</th>";
}

$sections .= "<th scope='col'>Price</th>";
$sections .= "<th scope='col'>Change</th>";
$sections .= "<th scope='col'>Count</th>";

echo $sections;