<!DOCTYPE html>
<html lang="en">
<head>
  <title>Poe-Stats</title>
  <meta charset="utf-8">
  <link rel="icon" type="image/png" href="assets/img/favico.png">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/4.0.0/css/bootstrap.min.css">
  <link rel="stylesheet" href="assets/css/main.css">
</head>
<body>
<!-- Primary navbar -->
<nav class="navbar navbar-expand-sm navbar-dark">
  <div class="container-fluid">
    <a href="/" class="navbar-brand">
      <img src="assets/img/favico.png" class="d-inline-block align-top mr-2" alt="">
      Poe-Stats
    </a>
    <button class="navbar-toggler" type="button" data-toggle="collapse" data-target="#navbarNavDropdown" aria-controls="navbarNavDropdown" aria-expanded="false" aria-label="Toggle navigation">
      <span class="navbar-toggler-icon"></span>
    </button>
    <div class="collapse navbar-collapse" id="navbarNavDropdown">
      <ul class="navbar-nav mr-auto">
        <li class="nav-item"><a class="nav-link active" href="/">Front</a></li>
        <li class="nav-item"><a class="nav-link" href="prices">Prices</a></li>
        <li class="nav-item"><a class="nav-link" href="api">API</a></li>
        <li class="nav-item"><a class="nav-link" href="progress">Progress</a></li>
        <li class="nav-item"><a class="nav-link" href="about">About</a></li>
      </ul>
    </div>
  </div>
</nav>
<!--/Primary navbar/-->
<!-- Page body -->
<div class="container-fluid">
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
      <div class="row mb-3">
        <div class="col-lg">
          <div class="card custom-card">
            <div class="card-body">
              <h2 class="text-center">Poe-Stats</h2>
              <hr>
              <img src="assets/img/img1.png" class="main-img-1 ml-3">
              <h5>Overview</h5>
              <p>A Path of Exile statistics and price data collection page. This site gathers data over time from various items (uniques, gems, currency, you name it) from the ARPG Path of Exile and calculates their average prices. It also provides users a possibility to compare items' prices against previous leagues. This page is still in development and there has not been any official releases yet.</p>
              <h5>The general idea</h5>
              <p>The general goal is to combine the functionality of <a href="http://poe.ninja">poe.ninja</a> and <a href="http://poe-antiquary.xyz">poe-antiquary</a> with a nice sleek style while being as user friendly as possible.</a></p>
              <h5>The API</h5>
              <p>Of course, all the data displayed here can be accessed through an API. Link's in the navbar up top. It's fairly out of date at the moment but will be updated before the release.</p>
            </div>
          </div>
        </div>
      </div>
      <div class="row mb-3">
        <div class="col-lg">
          <div class="card custom-card">
            <div class="card-body">
              <h2 class="text-center">News</h2>
              <hr>
              <div><em>03/05/2018</em> - Redesign page breakpoints</div>
              <div><em>03/05/2018</em> - Rework Progress page</div>
              <div><em>02/05/2018</em> - Add Progress page</div>
              <div><em>01/05/2018</em> - Add dynamic sparklines (now display current price)</div>
              <div><em>30/04/2018</em> - Add animated progressbar for live updates</div>
              <div><em>30/04/2018</em> - Fix rigid Mirror of Kalandra price</div>
              <div><em>29/04/2018</em> - Add league duration bar under About page</div>
              <div><em>29/04/2018</em> - Add league length API</div>
              <div><em>29/04/2018</em> - Rewrite backend for history data collectors</div>
              <div><em>27/04/2018</em> - Add live updates</div>
              <div><em>26/04/2018</em> - Improve page responsiveness</div>
              <div><em>25/04/2018</em> - Update API pages</div>
              <div><em>24/04/2018</em> - Enable Cloudflare</div>
              <div><em>24/04/2018</em> - Add exalted prices</div>
              <div><em>23/04/2018</em> - Add support for enchantments</div>
              <div><em>22/04/2018</em> - Change domains</div>
              <div><em>14/04/2018</em> - Fix Beachhead variants not being indexed</div>
              <div><em>10/04/2018</em> - Fix Kaom's Heart not being indexed</div>
              <div><em>09/04/2018</em> - Add item price history graphs</div>
              <div><em>08/04/2018</em> - Create logo</div>
            </div>
          </div>
        </div>
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
<script src="https://maxcdn.bootstrapcdn.com/bootstrap/4.0.0/js/bootstrap.min.js"></script>
<link rel="stylesheet" href="assets/css/responsive.css">
</body>
</html>
