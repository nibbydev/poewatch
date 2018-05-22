<!DOCTYPE html>
<html lang="en">
<head>
  <title>Poe-Stats - Pricer</title>
  <meta charset="utf-8">
  <link rel="icon" type="image/png" href="assets/img/ico/192.png" sizes="192x192">
  <link rel="icon" type="image/png" href="assets/img/ico/96.png" sizes="96x96">
  <link rel="icon" type="image/png" href="assets/img/ico/32.png" sizes="32x32">
  <link rel="icon" type="image/png" href="assets/img/ico/16.png" sizes="16x16">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/4.0.0/css/bootstrap.min.css">
  <link rel="stylesheet" href="assets/css/main.css">
</head>
<body>
<!-- Primary navbar -->
<nav class="navbar navbar-expand-md navbar-dark">
  <div class="container-fluid">
    <a href="/" class="navbar-brand">
      <img src="assets/img/favico.png" class="d-inline-block align-top mr-2">
      Poe-Stats
    </a>
    <button class="navbar-toggler" type="button" data-toggle="collapse" data-target="#navbarNavDropdown" aria-controls="navbarNavDropdown" aria-expanded="false" aria-label="Toggle navigation">
      <span class="navbar-toggler-icon"></span>
    </button>
    <div class="collapse navbar-collapse" id="navbarNavDropdown">
      <ul class="navbar-nav mr-auto">
        <li class="nav-item"><a class="nav-link" href="/">Front</a></li>
        <li class="nav-item"><a class="nav-link" href="prices">Prices</a></li>
        <li class="nav-item"><a class="nav-link" href="api">API</a></li>
        <li class="nav-item"><a class="nav-link active" href="progress">Progress</a></li>
        <li class="nav-item"><a class="nav-link" href="about">About</a></li>
      </ul>
    </div>
  </div>
</nav>
<!--/Primary navbar/-->
<!-- Page body -->
<div class="container-fluid pb-4">    
  <div class="row">
    <!-- Menu -->
    <div class="col-xl-3 custom-sidebar-column col-lg-10 offset-xl-0 offset-lg-1 offset-md-0"> 
      <div class="row mt-4 mb-xl-4">

          <?php include ( "assets/php/menu.php" ) ?>

      </div>
    </div>
    <!--/Menu/-->
    <!-- Main content -->
    <div class="col-xl-9 col-lg-10 offset-xl-0 offset-lg-1 offset-md-0 mt-4">
      <div class="card custom-card">
        <div class="card-header">
          <h2 class="text-white">Hey, you!</h3>
          <div>Ever felt a need for a tool for quickly adding buyout notes to masses of items based on their average prices? Well, look no further!</div>
        </div>
        <div class="card-body">
          <img src="assets/img/pricer.gif" class="float-right ml-3 rounded img-fluid">
          <h5 class="text-white">Where can I get one?</h5>
          <a class="btn btn-outline-dark mb-2" href="https://github.com/siegrest/Pricer/releases/latest" role="button">Over at GitHub</a>
          <h5 class="text-white">How does it work?</h5>
          <p class="paragraph-text">When you right-click an item in a premium tab (or any tab), this handy program copies the item data, matches it against its database and instantly pastes a buyout note containing the average price.</p>
          <h5 class="text-white">Who is it for?</h5>
          <p class="paragraph-text">Players who are tired of induvidually pricing hundreds of items in their stash tabs. Be you a labrunner or an Uber Atziri farmer who has dozens upon dozens of gems or maps that need prices or maybe just your average player who wants to actually enjoy the game instead of playing Shoppe Keep all day long. Well, in that case this program is perfect for you.</p>
          <h5 class="text-white">What can it price?</h5>
          <p class="paragraph-text">Armours, weapons, accessories, flasks, 6-links, 5-links, jewels, gems, divination cards, maps, currency, prophecies, essences, normal, magic and rare items. In short: if it's an item, this program can find a price for it.</p>
        </div>
        <div class="card-footer slim-card-edge"></div>
      </div>
    </div>
    <!--/Main content/-->
  </div>
</div>
<!--/Page body/-->
<!-- Footer -->
<footer class="container-fluid text-center">
  <p>Poe-Stats © 2018</p>
</footer>
<!--/Footer/-->
<script src="https://ajax.googleapis.com/ajax/libs/jquery/3.3.1/jquery.min.js"></script>
<script type="text/javascript" src="assets/js/progress.js"></script>
<script src="https://maxcdn.bootstrapcdn.com/bootstrap/4.0.0/js/bootstrap.min.js"></script>
<link rel="stylesheet" href="assets/css/responsive.css">
</body>
</html>
