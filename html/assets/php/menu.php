<?php
$hrefs = array(
  "prices?category=enchantments",
  "prices?category=accessories",
  "prices?category=armour",
  "prices?category=currency",
  "prices?category=cards",
  "prices?category=essence",
  "prices?category=flasks",
  "prices?category=gems",
  "prices?category=jewels",
  "prices?category=maps",
  "prices?category=prophecy",
  "prices?category=weapons"
);
$imgs = array(
  "http://web.poecdn.com/image/Art/2DItems/Currency/Enchantment.png?scale=1&w=1&h=1",
  "http://web.poecdn.com/image/Art/2DItems/Amulets/EyeOfInnocence.png?scale=1&w=1&h=1",
  "http://web.poecdn.com/image/Art/2DItems/Armours/Gloves/AtzirisAcuity.png?scale=1&w=1&h=1",
  "http://web.poecdn.com/image/Art/2DItems/Currency/CurrencyAddModToRare.png?scale=1&w=1&h=1",
  "http://web.poecdn.com/image/Art/2DItems/Divination/InventoryIcon.png?scale=1&w=1&h=1",
  "http://web.poecdn.com/image/Art/2DItems/Currency/Essence/Misery7.png?scale=1&w=1&h=1",
  "http://web.poecdn.com/gen/image/YTo3OntzOjEwOiJsZWFn/dWVOYW1lIjtzOjg6IkJl/c3RpYXJ5IjtzOjk6ImFj/Y291bnRJZCI7TzoxODoi/R3JpbmRiXERhdGFiYXNl/XElkIjoxOntzOjI6Imlk/IjtpOjA7fXM6MTA6InNp/bXBsaWZpZWQiO2I6MTtz/OjEzOiJpbnZlbnRvcnlU/eXBlIjtpOjE7aToyO2E6/Mzp7czoxOiJmIjtzOjMx/OiJBcnQvMkRJdGVtcy9G/bGFza3MvU2hhcGVyc0Zs/YXNrIjtzOjI6InNwIjtk/OjAuNjA4NTE5MjY5Nzc2/ODc2MztzOjU6ImxldmVs/IjtpOjA7fWk6MTtpOjQ7/aTowO2k6OTt9/635b9a3208/Item.png?scale=1&w=1&h=1",
  "http://web.poecdn.com/image/Art/2DItems/Gems/VaalGems/VaalBreachPortal.png?scale=1&w=1&h=1",
  "http://web.poecdn.com/image/Art/2DItems/Jewels/GolemInfernal.png?scale=1&w=1&h=1",
  "http://web.poecdn.com/image/Art/2DItems/Maps/Atlas2Maps/Chimera.png?scale=1&w=1&h=1",
  "http://web.poecdn.com/image/Art/2DItems/Currency/ProphecyOrbRed.png?scale=1&w=1&h=1",
  "http://web.poecdn.com/image/Art/2DItems/Weapons/OneHandWeapons/Claws/TouchOfAnguish.png?scale=1&w=1&h=1"
);
$titles = array(
  "Enchantments <span class='badge badge-light'>New</span>",
  "Accessories",
  "Armour",
  "Currency",
  "Div cards",
  "Essences",
  "Flasks",
  "Gems",
  "Jewels",
  "Maps",
  "Prophecy",
  "Weapons"
);

$category = isset($_GET["category"]) ? strtolower(trim($_GET["category"])) : "";

for ($i = 0; $i < sizeof($titles); $i++) { 
  $href = $hrefs[$i];
  $img = $imgs[$i];
  $title = $titles[$i];
  $active = ($category && strpos(strtolower(trim($title)), $category) !== false) ? " active" : "";
  
  echo "<a class='list-group-item sidebar-link$active' href='$href'><div class='sidebar-img-container text-center'><img src='$img'></div>$title</a>";
}
