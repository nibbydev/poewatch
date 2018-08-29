<?php
function GenFooter() {
  echo "<footer class='container-fluid text-center'><p>PoeWatch © " . date('Y') . "</p></footer>";
}

function GenNavbar() {
  $items = array(
    array(
      'display' => 'Front',
      'href'    => '/'
    ),
    array(
      'display' => 'Prices',
      'href'    => '/prices'
    ),
    array(
      'display' => 'API',
      'href'    => '/api'
    ),
    array(
      'display' => 'Progress',
      'href'    => '/progress'
    ),
    array(
      'display' => 'Characters',
      'href'    => '/characters'
    ),
    array(
      'display' => 'About',
      'href'    => '/about'
    )
  );

  echo "
  <nav class='navbar navbar-expand-md navbar-dark'>
    <div class='container-fluid'>
      <a href='/' class='navbar-brand'>
        <img src='assets/img/favico.png' class='d-inline-block align-top mr-2'>
        PoeWatch
      </a>
      <button class='navbar-toggler' type='button' data-toggle='collapse' data-target='#navbarNavDropdown' aria-controls='navbarNavDropdown' aria-expanded='false' aria-label='Toggle navigation'>
        <span class='navbar-toggler-icon'></span>
      </button>
      <div class='collapse navbar-collapse' id='navbarNavDropdown'>
        <ul class='navbar-nav mr-auto'>";

  for ($i = 0; $i < sizeof($items); $i++) {
    $active = explode('?', $_SERVER['REQUEST_URI'])[0] === $items[$i]['href'] ? 'active' : '';
    echo "<li class='nav-item'><a class='nav-link $active' href='{$items[$i]['href']}'>{$items[$i]['display']}</a></li>";
  }

  echo  "
        </ul>
      </div>
    </div>
  </nav>";

}

function GenCatMenuHTML() {
  $data = array(
    array(
      "href"  => "prices?category=accessories",
      "icon"  => "https://web.poecdn.com/image/Art/2DItems/Amulets/EyeOfInnocence.png?scale=1&w=1&h=1",
      "name"  => "Accessories"
    ),
    array(
      "href"  => "prices?category=armour",
      "icon"  => "https://web.poecdn.com/image/Art/2DItems/Armours/Gloves/AtzirisAcuity.png?scale=1&w=1&h=1",
      "name"  => "Armour"
    ),
    array(
      "href"  => "prices?category=bases",
      "icon"  => "https://web.poecdn.com/image/Art/2DItems/Armours/BodyArmours/BodyStr3C.png?scale=1&w=1&h=1",
      "name"  => "Bases"
    ),
    array(
      "href"  => "prices?category=currency",
      "icon"  => "https://web.poecdn.com/image/Art/2DItems/Currency/CurrencyAddModToRare.png?scale=1&w=1&h=1",
      "name"  => "Currency"
    ),
    array(
      "href"  => "prices?category=cards",
      "icon"  => "https://web.poecdn.com/image/Art/2DItems/Divination/InventoryIcon.png?scale=1&w=1&h=1",
      "name"  => "Div cards"
    ),
    array(
      "href"  => "prices?category=enchantments",
      "icon"  => "https://web.poecdn.com/image/Art/2DItems/Currency/Enchantment.png?scale=1&w=1&h=1",
      "name"  => "Enchantments"
    ),
    array(
      "href"  => "prices?category=essence",
      "icon"  => "https://web.poecdn.com/image/Art/2DItems/Currency/Essence/Misery7.png?scale=1&w=1&h=1",
      "name"  => "Essences"
    ),
    array(
      "href"  => "prices?category=flasks",
      "icon"  => "https://web.poecdn.com/gen/image/YTo3OntzOjEwOiJsZWFn/dWVOYW1lIjtzOjg6IkJl/c3RpYXJ5IjtzOjk6ImFj/Y291bnRJZCI7TzoxODoi/R3JpbmRiXERhdGFiYXNl/XElkIjoxOntzOjI6Imlk/IjtpOjA7fXM6MTA6InNp/bXBsaWZpZWQiO2I6MTtz/OjEzOiJpbnZlbnRvcnlU/eXBlIjtpOjE7aToyO2E6/Mzp7czoxOiJmIjtzOjMx/OiJBcnQvMkRJdGVtcy9G/bGFza3MvU2hhcGVyc0Zs/YXNrIjtzOjI6InNwIjtk/OjAuNjA4NTE5MjY5Nzc2/ODc2MztzOjU6ImxldmVs/IjtpOjA7fWk6MTtpOjQ7/aTowO2k6OTt9/635b9a3208/Item.png?scale=1&w=1&h=1",
      "name"  => "Flasks"
    ),
    array(
      "href"  => "prices?category=gems",
      "icon"  => "https://web.poecdn.com/image/Art/2DItems/Gems/VaalGems/VaalBreachPortal.png?scale=1&w=1&h=1",
      "name"  => "Gems"
    ),
    array(
      "href"  => "prices?category=jewels",
      "icon"  => "https://web.poecdn.com/image/Art/2DItems/Jewels/GolemInfernal.png?scale=1&w=1&h=1",
      "name"  => "Jewels"
    ),
    array(
      "href"  => "prices?category=maps",
      "icon"  => "https://web.poecdn.com/image/Art/2DItems/Maps/Atlas2Maps/Chimera.png?scale=1&w=1&h=1",
      "name"  => "Maps"
    ),
    array(
      "href"  => "prices?category=prophecy",
      "icon"  => "https://web.poecdn.com/image/Art/2DItems/Currency/ProphecyOrbRed.png?scale=1&w=1&h=1",
      "name"  => "Prophecy"
    ),
    array(
      "href"  => "prices?category=weapons",
      "icon"  => "https://web.poecdn.com/image/Art/2DItems/Weapons/OneHandWeapons/Claws/TouchOfAnguish.png?scale=1&w=1&h=1",
      "name"  => "Weapons"
    ),
  );

  echo "<div class='custom-sidebar-column mr-3'>
          <div class='card custom-card'>
            <div class='card-header slim-card-edge'></div>
              <div class='card-body p-0'>";

  for ($i = 0; $i < sizeof($data); $i++) { 
    // If category param matches current category, mark it as active
    $active = "";
    if (isset($_GET["category"])) {
      if ($_GET["category"] === explode('=', $data[$i]["href"])[1]) {
        $active = "active";
      }
    }

    echo "
    <a class='custom-menu-item d-flex p-2 $active' href='{$data[$i]['href']}'>
      <div class='img-container img-container-sm'>
        <img src='{$data[$i]['icon']}'>
      </div>
      <div class='custom-menu-name align-self-center mx-2'>
        <span>{$data[$i]['name']}</span>
      </div>
    </a>";
  }

  echo "  </div>
        <div class='card-footer slim-card-edge'>
      </div>
    </div>
  </div>";
}

function GenMotDBox() {
  echo "
  <div class='row d-block mb-3'>
    <div class='col'> 
      <div class='card custom-card'>
        <div class='card-header slim-card-edge'></div>
        <div class='card-body p-1'>
          <p class='mb-0 text-center subtext-1'>
            [ allan please add advertisement ]
          </p>
        </div>
        <div class='card-footer slim-card-edge'></div>
      </div>
    </div>
  </div>";
}
