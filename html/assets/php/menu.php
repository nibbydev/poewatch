<?php
$jsonFile = json_decode( file_get_contents("assets/json/menu.json"), true );

foreach ($jsonFile as $item) {
  $category = isset($_GET["category"]) ? strtolower(trim($_GET["category"])) : "";
  $href = $item["href"];
  $img = $item["img"];
  $title = $item["title"];
  $active = $category === strtolower(trim($title)) ? " active" : "";

  echo <<<"MENU"
  <a class='list-group-item sidebar-link$active' href='$href'>
    <div class='sidebar-img-container text-center'>
      <img src='$img'>
    </div>
    $title
  </a>
MENU;
}
