<?php
$PAGE_DATA = [
  "title" => "PoeWatch",
  "pageHeader" => null,
  "description" => "A price statistics generation & collection page",
  "jsIncludes" => [
    "https://ajax.googleapis.com/ajax/libs/jquery/3.3.1/jquery.min.js",
    "https://stackpath.bootstrapcdn.com/bootstrap/4.2.1/js/bootstrap.min.js",
    "main.js?04062019"
  ],
  "cssIncludes" => [
    "https://stackpath.bootstrapcdn.com/bootstrap/4.2.1/css/bootstrap.min.css",
    "main.css?04062019"
  ],
  "headerIncludes" => [],
  "footerIncludes" => [],
  "navs" => [
    [
      'name' => 'Front',
      'href' => '/'
    ],
    [
      'name' => 'Prices',
      'href' => '/prices'
    ],
    [
      'name' => 'API',
      'href' => '/api'
    ],
    [
      'name' => 'Leagues',
      'href' => '/leagues'
    ],
    [
      'name' => 'Characters',
      'href' => '/characters'
    ],
    [
      'name' => 'Listings',
      'href' => '/listings'
    ],
    [
      'name' => 'Stats',
      'href' => '/stats'
    ],
    [
      'name' => 'About',
      'href' => '/about'
    ]
  ],
  "priceCategories" => [
    [
      "display" => "Accessories",
      "name" => "accessory",
      "icon" => "https://web.poecdn.com/image/Art/2DItems/Amulets/YphethakksHeartUpgrade.png?scale=1&w=1&h=1",
      "href" => "prices?category=accessory"
    ],
    [
      "display" => "Armour",
      "name" => "armour",
      "icon" => "https://web.poecdn.com/image/Art/2DItems/Armours/Gloves/AtzirisAcuity.png?scale=1&w=1&h=1",
      "href" => "prices?category=armour"
    ],
    [
      "display" => "Bases",
      "name" => "base",
      "icon" => "https://web.poecdn.com/image/Art/2DItems/Rings/OpalRing.png?scale=1&w=1&h=1",
      "href" => "prices?category=base"
    ],
    [
      "display" => "Beasts",
      "name" => "beast",
      "icon" => "http://web.poecdn.com/image/Art/2DItems/Currency/BestiaryOrbFull.png?scale=1&w=1&h=1",
      "href" => "prices?category=beast"
    ],
    [
      "display" => "Currency",
      "name" => "currency",
      "icon" => "https://web.poecdn.com/image/Art/2DItems/Currency/CurrencyAddModToRare.png?scale=1&w=1&h=1",
      "href" => "prices?category=currency"
    ],
    [
      "display" => "Div cards",
      "name" => "card",
      "icon" => "https://web.poecdn.com/image/Art/2DItems/Divination/InventoryIcon.png?scale=1&w=1&h=1",
      "href" => "prices?category=card"
    ],
    [
      "display" => "Enchantments",
      "name" => "enchantment",
      "icon" => "https://web.poecdn.com/image/Art/2DItems/Currency/Enchantment.png?scale=1&w=1&h=1",
      "href" => "prices?category=enchantment"
    ],
    [
      "display" => "Flasks",
      "name" => "flask",
      "icon" => "https://web.poecdn.com/gen/image/WzksNCx7ImYiOiJBcnRcLzJESXRlbXNcL0ZsYXNrc1wvVGFzdGVPZkhhdGUiLCJzcCI6MC42MDg1LCJsZXZlbCI6MH1d/fdc3742db8/Item.png",
      "href" => "prices?category=flask"
    ],
    [
      "display" => "Gems",
      "name" => "gem",
      "icon" => "https://web.poecdn.com/image/Art/2DItems/Gems/Support/Enlighten.png?scale=1&w=1&h=1",
      "href" => "prices?category=gem"
    ],
    [
      "display" => "Jewels",
      "name" => "jewel",
      "icon" => "https://web.poecdn.com/image/Art/2DItems/Jewels/GolemArctic.png?scale=1&w=1&h=1",
      "href" => "prices?category=jewel"
    ],
    [
      "display" => "Maps",
      "name" => "map",
      "icon" => "https://web.poecdn.com/image/Art/2DItems/Maps/Atlas2Maps/New/BurialChambers.png?scale=1&w=1&h=1&mr=1&mn=1&mt=16",
      "href" => "prices?category=map"
    ],
    [
      "display" => "Prophecy",
      "name" => "prophecy",
      "icon" => "https://web.poecdn.com/image/Art/2DItems/Currency/ProphecyOrbRed.png?scale=1&w=1&h=1",
      "href" => "prices?category=prophecy"
    ],
    [
      "display" => "Weapons",
      "name" => "weapon",
      "icon" => "https://web.poecdn.com/image/Art/2DItems/Weapons/OneHandWeapons/Claws/TouchOfAnguish.png?scale=1&w=1&h=1",
      "href" => "prices?category=weapon"
    ]
  ],
  "page" => [
    // page-local data can go here
  ]
];
