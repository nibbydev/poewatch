<?php
function GenFooter() {
  echo "<footer class='container-fluid d-flex flex-column justify-content-center align-items-center p-0'>
    <div>PoeWatch © " . date('Y') . "</div>
    <div><a href='http://github.com/siegrest/poewatch' target='_blank'>Available on Github</a></div>
  </footer>";
}

function GenNavbar($pdo) {
  $elements = array(
    array(
      'name' => 'Front',
      'href' => '/'
    ),
    array(
      'name' => 'Prices',
      'href' => '/prices'
    ),
    array(
      'name' => 'API',
      'href' => '/api'
    ),
    array(
      'name' => 'Leagues',
      'href' => '/leagues'
    ),
    array(
      'name' => 'Characters',
      'href' => '/characters'
    ),
    array(
      'name' => 'About',
      'href' => '/about'
    )
  );

  echo "<nav class='navbar navbar-expand-md navbar-dark'>
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

  foreach ($elements as $element) {
    $active = explode('?', $_SERVER['REQUEST_URI'])[0] === $element['href'] ? 'active' : '';
    echo "<li class='nav-item'><a class='nav-link $active' href='{$element['href']}'>{$element['name']}</a></li>";
  }

  echo "</ul>
  </div>
  </div>
  </nav>";

}

function GenCatMenuHTML($pdo) {
  $elements = array(
    array(
      'display' => 'Accessories',
      'icon'    => 'https://web.poecdn.com/image/Art/2DItems/Amulets/EyeOfInnocence.png?scale=1&w=1&h=1',
      'href'    => 'prices?category=accessory'
    ),
    array(
      'display' => 'All relics',
      'icon'    => 'https://web.poecdn.com/image/Art/2DItems/Rings/MoonstoneRingUnique.png?scale=1&w=1&h=1&relic=1',
      'href'    => 'prices?category=relic'
    ),
    array(
      'display' => 'Armour',
      'icon'    => 'https://web.poecdn.com/image/Art/2DItems/Armours/Gloves/AtzirisAcuity.png?scale=1&w=1&h=1',
      'href'    => 'prices?category=armour'
    ),
    array(
      'display' => 'Bases',
      'icon'    => 'https://web.poecdn.com/image/Art/2DItems/Rings/OpalRing.png?scale=1&w=1&h=1',
      'href'    => 'prices?category=base'
    ),
    array(
      'display' => 'Currency',
      'icon'    => 'https://web.poecdn.com/image/Art/2DItems/Currency/CurrencyAddModToRare.png?scale=1&w=1&h=1',
      'href'    => 'prices?category=currency'
    ),
    array(
      'display' => 'Div cards',
      'icon'    => 'https://web.poecdn.com/image/Art/2DItems/Divination/InventoryIcon.png?scale=1&w=1&h=1',
      'href'    => 'prices?category=card'
    ),
    array(
      'display' => 'Enchantments',
      'icon'    => 'https://web.poecdn.com/image/Art/2DItems/Currency/Enchantment.png?scale=1&w=1&h=1',
      'href'    => 'prices?category=enchantment'
    ),
    array(
      'display' => 'Flasks',
      'icon'    => 'https://web.poecdn.com/gen/image/WzksNCx7ImYiOiJBcnRcLzJESXRlbXNcL0ZsYXNrc1wvU2hhcGVyc0ZsYXNrIiwic3AiOjAuNjA4NSwibGV2ZWwiOjB9XQ/4369b8fcb9/Item.png?scale=1&w=1&h=1',
      'href'    => 'prices?category=flask'
    ),
    array(
      'display' => 'Gems',
      'icon'    => 'https://web.poecdn.com/image/Art/2DItems/Gems/VaalGems/VaalBreachPortal.png?scale=1&w=1&h=1',
      'href'    => 'prices?category=gem'
    ),
    array(
      'display' => 'Jewels',
      'icon'    => 'https://web.poecdn.com/image/Art/2DItems/Jewels/GolemInfernal.png?scale=1&w=1&h=1',
      'href'    => 'prices?category=jewel'
    ),
    array(
      'display' => 'Maps',
      'icon'    => 'https://web.poecdn.com/image/Art/2DItems/Maps/Atlas2Maps/Chimera.png?scale=1&w=1&h=1',
      'href'    => 'prices?category=map'
    ),
    array(
      'display' => 'Prophecy',
      'icon'    => 'https://web.poecdn.com/image/Art/2DItems/Currency/ProphecyOrbRed.png?scale=1&w=1&h=1',
      'href'    => 'prices?category=prophecy'
    ),
    array(
      'display' => 'Weapons',
      'icon'    => 'https://web.poecdn.com/image/Art/2DItems/Weapons/OneHandWeapons/Claws/TouchOfAnguish.png?scale=1&w=1&h=1',
      'href'    => 'prices?category=weapon'
    ),
    
  );

  echo "<div class='custom-sidebar-column mr-3'>
  <div class='card custom-card'>
  <div class='card-header slim-card-edge'></div>
  <div class='card-body p-0'>";

  foreach ($elements as $element) {
    // If category param matches current category, mark it as active
    $active = "";
    if (isset($_GET["category"])) {
      if ($_GET["category"] === explode('=', $element["href"])[1]) {
        $active = "active";
      }
    }

    echo "
    <a class='custom-menu-item d-flex p-2 $active' href='{$element['href']}'>
      <div class='img-container img-container-sm'>
        <img src='{$element['icon']}'>
      </div>
      <div class='custom-menu-name align-self-center mx-2'>
        <span>{$element['display']}</span>
      </div>
    </a>";
  }

  echo "</div>
  <div class='card-footer slim-card-edge'>
  </div>
  </div>
  </div>";
}

function GenMotDBox() {
  // I can't be bothered to add website config database tables atm so just
  // remove this line to enable the motd box
  return;

  echo "
  <div class='row d-block mb-3'>
    <div class='col'> 
      <div class='card custom-card custom-badge-red'>
        <div class='card-header slim-card-edge'></div>
        <div class='card-body p-1'>
          <p class='mb-0 text-center text-white'>
            [allan please add advertisment]
          </p>
        </div>
        <div class='card-footer slim-card-edge'></div>
      </div>
    </div>
  </div>";
}

function GenHeaderMetaTags($title, $description) {
  echo "
  <title>$title</title>
  <meta charset='utf-8'>
  <meta property='og:site_name' content='Poe Watch'>
  <meta property='og:locale' content='en_US'>
  <meta property='og:title' content='$title'>
  <meta property='og:type' content='website'>
  <meta property='og:image' content='https://poe.watch/assets/img/ico/96.png'>
  <meta property='og:description' content='$description'>
  <link rel='icon' type='image/png' href='assets/img/ico/192.png' sizes='192x192'>
  <link rel='icon' type='image/png' href='assets/img/ico/96.png' sizes='96x96'>
  <link rel='icon' type='image/png' href='assets/img/ico/32.png' sizes='32x32'>
  <link rel='icon' type='image/png' href='assets/img/ico/16.png' sizes='16x16'>
  <meta name='viewport' content='width=device-width, initial-scale=1'>";
}
