<?php
function GenCatMenuHTML() {
  $data = array(
    array(
      "href" => "prices?category=enchantments",
      "icon" => "https://web.poecdn.com/image/Art/2DItems/Currency/Enchantment.png?scale=1&w=1&h=1",
      "name" => "Enchantments",
      "round" => "rounded-top",
      "order" => "order-1 order-md-1 order-lg-1 order-xl-1"
    ),
    array(
      "href" => "prices?category=accessories",
      "icon" => "https://web.poecdn.com/image/Art/2DItems/Amulets/EyeOfInnocence.png?scale=1&w=1&h=1",
      "name" => "Accessories",
      "round" => "rounded-0",
      "order" => "order-3 order-md-4 order-lg-5 order-xl-2"
    ),
    array(
      "href" => "prices?category=armour",
      "icon" => "https://web.poecdn.com/image/Art/2DItems/Armours/Gloves/AtzirisAcuity.png?scale=1&w=1&h=1",
      "name" => "Armour",
      "round" => "rounded-xl-0 rounded-lg-bottom rounded-md-0 rounded-sm-0",
      "order" => "order-5 order-md-7 order-lg-9 order-xl-3"
    ),
    array(
      "href" => "prices?category=currency",
      "icon" => "https://web.poecdn.com/image/Art/2DItems/Currency/CurrencyAddModToRare.png?scale=1&w=1&h=1",
      "name" => "Currency",
      "round" => "rounded-xl-0 rounded-lg-top rounded-lg-bottom-0 rounded-md-bottom rounded-sm-0",
      "order" => "order-7 order-md-10 order-lg-2 order-xl-4"
    ),
    array(
      "href" => "prices?category=cards",
      "icon" => "https://web.poecdn.com/image/Art/2DItems/Divination/InventoryIcon.png?scale=1&w=1&h=1",
      "name" => "Div cards",
      "round" => "rounded-xl-0 rounded-lg-0 rounded-md-top rounded-sm-0",
      "order" => "order-9 order-md-2 order-lg-6 order-xl-5"
    ),
    array(
      "href" => "prices?category=essence",
      "icon" => "https://web.poecdn.com/image/Art/2DItems/Currency/Essence/Misery7.png?scale=1&w=1&h=1",
      "name" => "Essences",
      "round" => "rounded-xl-0 rounded-lg-bottom rounded-md-0 rounded-bottom",
      "order" => "order-11 order-md-5 order-lg-10 order-xl-6"
    ),
    array(
      "href" => "prices?category=flasks",
      "icon" => "https://web.poecdn.com/gen/image/YTo3OntzOjEwOiJsZWFn/dWVOYW1lIjtzOjg6IkJl/c3RpYXJ5IjtzOjk6ImFj/Y291bnRJZCI7TzoxODoi/R3JpbmRiXERhdGFiYXNl/XElkIjoxOntzOjI6Imlk/IjtpOjA7fXM6MTA6InNp/bXBsaWZpZWQiO2I6MTtz/OjEzOiJpbnZlbnRvcnlU/eXBlIjtpOjE7aToyO2E6/Mzp7czoxOiJmIjtzOjMx/OiJBcnQvMkRJdGVtcy9G/bGFza3MvU2hhcGVyc0Zs/YXNrIjtzOjI6InNwIjtk/OjAuNjA4NTE5MjY5Nzc2/ODc2MztzOjU6ImxldmVs/IjtpOjA7fWk6MTtpOjQ7/aTowO2k6OTt9/635b9a3208/Item.png?scale=1&w=1&h=1",
      "name" => "Flasks",
      "round" => "rounded-xl-0 rounded-lg-top rounded-md-0 rounded-top",
      "order" => "order-2 order-md-8 order-lg-3 order-xl-7"
    ),
    array(
      "href" => "prices?category=gems",
      "icon" => "https://web.poecdn.com/image/Art/2DItems/Gems/VaalGems/VaalBreachPortal.png?scale=1&w=1&h=1",
      "name" => "Gems",
      "round" => "rounded-xl-0 rounded-lg-0 rounded-md-bottom rounded-sm-0",
      "order" => "order-4 order-md-11 order-lg-7 order-xl-8"
    ),
    array(
      "href" => "prices?category=jewels",
      "icon" => "https://web.poecdn.com/image/Art/2DItems/Jewels/GolemInfernal.png?scale=1&w=1&h=1",
      "name" => "Jewels",
      "round" => "rounded-xl-0 rounded-lg-bottom rounded-lg-top-0 rounded-md-top rounded-sm-0",
      "order" => "order-6 order-md-3 order-lg-11 order-xl-9"
    ),
    array(
      "href" => "prices?category=maps",
      "icon" => "https://web.poecdn.com/image/Art/2DItems/Maps/Atlas2Maps/Chimera.png?scale=1&w=1&h=1",
      "name" => "Maps",
      "round" => "rounded-xl-0 rounded-lg-top rounded-md-0 rounded-sm-0",
      "order" => "order-8 order-md-6 order-lg-4 order-xl-10"
    ),
    array(
      "href" => "prices?category=prophecy",
      "icon" => "https://web.poecdn.com/image/Art/2DItems/Currency/ProphecyOrbRed.png?scale=1&w=1&h=1",
      "name" => "Prophecy",
      "round" => "rounded-0",
      "order" => "order-10 order-md-9 order-lg-8 order-xl-11"
    ),
    array(
      "href" => "prices?category=weapons",
      "icon" => "https://web.poecdn.com/image/Art/2DItems/Weapons/OneHandWeapons/Claws/TouchOfAnguish.png?scale=1&w=1&h=1",
      "name" => "Weapons",
      "round" => "rounded-bottom",
      "order" => "order-12 order-md-12 order-lg-12 order-xl-12"
    ),
  );

  echo "<div class='col-xl-3 custom-sidebar-column'><div class='row mt-4 mb-xl-4'>";

  for ($i = 0; $i < sizeof($data); $i++) { 
    // If category param matches current category, mark it as active
    $active = "";
    if (isset($_GET["category"])) {
      if ($_GET["category"] === strtolower($data[$i]["name"])) {
        $active = "active";
      }
    }

    echo "<div class='col-xl-12 col-lg-3 col-md-4 col-6 {$data[$i]['order']}'>
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
