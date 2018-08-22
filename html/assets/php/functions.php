<?php
function GenFooter() {
  echo "<footer class='container-fluid text-center'><p>PoeWatch © " . date('Y') . "</p></footer>";
}

function GenCatMenuHTML() {
  $data = array(
    array(
      "href" => "prices?category=enchantments",
      "icon" => "https://web.poecdn.com/image/Art/2DItems/Currency/Enchantment.png?scale=1&w=1&h=1",
      "name" => "Enchantments",
      "round" => "rounded-top",
      "order" => ""
    ),
    array(
      "href" => "prices?category=accessories",
      "icon" => "https://web.poecdn.com/image/Art/2DItems/Amulets/EyeOfInnocence.png?scale=1&w=1&h=1",
      "name" => "Accessories",
      "round" => "",
      "order" => ""
    ),
    array(
      "href" => "prices?category=armour",
      "icon" => "https://web.poecdn.com/image/Art/2DItems/Armours/Gloves/AtzirisAcuity.png?scale=1&w=1&h=1",
      "name" => "Armour",
      "round" => "",
      "order" => ""
    ),
    array(
      "href" => "prices?category=currency",
      "icon" => "https://web.poecdn.com/image/Art/2DItems/Currency/CurrencyAddModToRare.png?scale=1&w=1&h=1",
      "name" => "Currency",
      "round" => "",
      "order" => ""
    ),
    array(
      "href" => "prices?category=cards",
      "icon" => "https://web.poecdn.com/image/Art/2DItems/Divination/InventoryIcon.png?scale=1&w=1&h=1",
      "name" => "Div cards",
      "round" => "",
      "order" => ""
    ),
    array(
      "href" => "prices?category=essence",
      "icon" => "https://web.poecdn.com/image/Art/2DItems/Currency/Essence/Misery7.png?scale=1&w=1&h=1",
      "name" => "Essences",
      "round" => "",
      "order" => ""
    ),
    array(
      "href" => "prices?category=flasks",
      "icon" => "https://web.poecdn.com/gen/image/YTo3OntzOjEwOiJsZWFn/dWVOYW1lIjtzOjg6IkJl/c3RpYXJ5IjtzOjk6ImFj/Y291bnRJZCI7TzoxODoi/R3JpbmRiXERhdGFiYXNl/XElkIjoxOntzOjI6Imlk/IjtpOjA7fXM6MTA6InNp/bXBsaWZpZWQiO2I6MTtz/OjEzOiJpbnZlbnRvcnlU/eXBlIjtpOjE7aToyO2E6/Mzp7czoxOiJmIjtzOjMx/OiJBcnQvMkRJdGVtcy9G/bGFza3MvU2hhcGVyc0Zs/YXNrIjtzOjI6InNwIjtk/OjAuNjA4NTE5MjY5Nzc2/ODc2MztzOjU6ImxldmVs/IjtpOjA7fWk6MTtpOjQ7/aTowO2k6OTt9/635b9a3208/Item.png?scale=1&w=1&h=1",
      "name" => "Flasks",
      "round" => "",
      "order" => ""
    ),
    array(
      "href" => "prices?category=gems",
      "icon" => "https://web.poecdn.com/image/Art/2DItems/Gems/VaalGems/VaalBreachPortal.png?scale=1&w=1&h=1",
      "name" => "Gems",
      "round" => "",
      "order" => ""
    ),
    array(
      "href" => "prices?category=jewels",
      "icon" => "https://web.poecdn.com/image/Art/2DItems/Jewels/GolemInfernal.png?scale=1&w=1&h=1",
      "name" => "Jewels",
      "round" => "",
      "order" => ""
    ),
    array(
      "href" => "prices?category=maps",
      "icon" => "https://web.poecdn.com/image/Art/2DItems/Maps/Atlas2Maps/Chimera.png?scale=1&w=1&h=1",
      "name" => "Maps",
      "round" => "",
      "order" => ""
    ),
    array(
      "href" => "prices?category=prophecy",
      "icon" => "https://web.poecdn.com/image/Art/2DItems/Currency/ProphecyOrbRed.png?scale=1&w=1&h=1",
      "name" => "Prophecy",
      "round" => "",
      "order" => ""
    ),
    array(
      "href" => "prices?category=weapons",
      "icon" => "https://web.poecdn.com/image/Art/2DItems/Weapons/OneHandWeapons/Claws/TouchOfAnguish.png?scale=1&w=1&h=1",
      "name" => "Weapons",
      "round" => "rounded-bottom",
      "order" => ""
    ),
  );

  echo "<div class='col-2 custom-sidebar-column'><div class='row'>";

  for ($i = 0; $i < sizeof($data); $i++) { 
    // If category param matches current category, mark it as active
    $active = "";
    if (isset($_GET["category"])) {
      if ($_GET["category"] === strtolower($data[$i]["name"])) {
        $active = "active";
      }
    }

    echo "<div class='col-12'>
      <a href='{$data[$i]['href']}'>
        <div class='custom-menu-item d-flex p-2 {$data[$i]['round']} $active'>
          <div class='img-container img-container-sm mr-2'>
            <img src='{$data[$i]['icon']}'>
          </div>
          <div class='align-self-center'>
            <span>{$data[$i]['name']}</span>
          </div>
        </div>
      </a>
    </div>";
  }

  echo "</div></div>";
}
