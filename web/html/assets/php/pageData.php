<?php 
$PAGEDATA = array(
  "title" => "PoeWatch",
  "pageHeader" => null,
  "description" => "A price statistics generation & collection page",
  "jsIncludes" => array(
    "https://ajax.googleapis.com/ajax/libs/jquery/3.3.1/jquery.min.js",
    "https://stackpath.bootstrapcdn.com/bootstrap/4.2.1/js/bootstrap.min.js"
  ),
  "cssIncludes" => array(
    "https://stackpath.bootstrapcdn.com/bootstrap/4.2.1/css/bootstrap.min.css",
    "main.css"
  ),
  "headerIncludes" => array(),
  "footerIncludes" => array(),
  "navs" => array(
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
      'name' => 'Stats',
      'href' => '/stats'
    ),
    array(
      'name' => 'About',
      'href' => '/about'
    )
  ),
  "priceCategories" => array(
    array(
      "display" => "Accessories",
      "name"    => "accessory",
      "icon"    => "https://web.poecdn.com/image/Art/2DItems/Amulets/YphethakksHeartUpgrade.png?scale=1&w=1&h=1",
      "href"    => "prices?category=accessory"
    ),
    array(
      "display" => "Armour",
      "name"    => "armour",
      "icon"    => "https://web.poecdn.com/image/Art/2DItems/Armours/Gloves/AtzirisAcuity.png?scale=1&w=1&h=1",
      "href"    => "prices?category=armour"
    ),
    array(
      "display" => "Bases",
      "name"    => "base",
      "icon"    => "https://web.poecdn.com/image/Art/2DItems/Rings/OpalRing.png?scale=1&w=1&h=1",
      "href"    => "prices?category=base"
    ),
    array(
      "display" => "Currency",
      "name"    => "currency",
      "icon"    => "https://web.poecdn.com/image/Art/2DItems/Currency/CurrencyAddModToRare.png?scale=1&w=1&h=1",
      "href"    => "prices?category=currency"
    ),
    array(
      "display" => "Div cards",
      "name"    => "card",
      "icon"    => "https://web.poecdn.com/image/Art/2DItems/Divination/InventoryIcon.png?scale=1&w=1&h=1",
      "href"    => "prices?category=card"
    ),
    array(
      "display" => "Enchantments",
      "name"    => "enchantment",
      "icon"    => "https://web.poecdn.com/image/Art/2DItems/Currency/Enchantment.png?scale=1&w=1&h=1",
      "href"    => "prices?category=enchantment"
    ),
    array(
      "display" => "Flasks",
      "name"    => "flask",
      "icon"    => "https://web.poecdn.com/gen/image/WzksNCx7ImYiOiJBcnRcLzJESXRlbXNcL0ZsYXNrc1wvVGFzdGVPZkhhdGUiLCJzcCI6MC42MDg1LCJsZXZlbCI6MH1d/fdc3742db8/Item.png",
      "href"    => "prices?category=flask"
    ),
    array(
      "display" => "Gems",
      "name"    => "gem",
      "icon"    => "https://web.poecdn.com/image/Art/2DItems/Gems/Support/Enlighten.png?scale=1&w=1&h=1",
      "href"    => "prices?category=gem"
    ),
    array(
      "display" => "Jewels",
      "name"    => "jewel",
      "icon"    => "https://web.poecdn.com/image/Art/2DItems/Jewels/GolemArctic.png?scale=1&w=1&h=1",
      "href"    => "prices?category=jewel"
    ),
    array(
      "display" => "Maps",
      "name"    => "map",
      "icon"    => "https://web.poecdn.com/image/Art/2DItems/Maps/Atlas2Maps/New/BurialChambers.png?scale=1&w=1&h=1&mr=1&mn=1&mt=16",
      "href"    => "prices?category=map"
    ),
    array(
      "display" => "Prophecy",
      "name"    => "prophecy",
      "icon"    => "https://web.poecdn.com/image/Art/2DItems/Currency/ProphecyOrbRed.png?scale=1&w=1&h=1",
      "href"    => "prices?category=prophecy"
    ),
    array(
      "display" => "Weapons",
      "name"    => "weapon",
      "icon"    => "https://web.poecdn.com/image/Art/2DItems/Weapons/OneHandWeapons/Claws/TouchOfAnguish.png?scale=1&w=1&h=1",
      "href"    => "prices?category=weapon"
    )
  )
);
