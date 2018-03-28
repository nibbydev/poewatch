<?php
  $jsonFile = json_decode( file_get_contents("assets/json/menu.json"), true );

  foreach ($jsonFile as $item) {
    if (strtolower(trim($_GET["category"])) == strtolower(trim($item["title"]))) {
      $active = " active";
    } else {
      $active = "";
    }

    $row_data = "<a class=\"list-group-item sidebar-link" . $active . "\" href=\"" . $item["href"] . "\" >";

    $row_data .= "<div class=\"sidebar-img-container text-center\">";
    $row_data .= "<img src=\"" . $item["img"] . "\">";
    $row_data .= "</div>" . $item["title"] . "</a>";
    echo $row_data;
  }
?>