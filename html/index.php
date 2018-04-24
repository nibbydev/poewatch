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
  <nav class="navbar navbar-expand-lg navbar-dark">
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
          <li class="nav-item"><a class="nav-link" href="about">About</a></li>
        </ul>
      </div>
    </div>
  </nav>
<div class="container-fluid">    
  <div class="row">
    <div class="col-lg-3"> 
      <div class="list-group sidebar-left" id="sidebar-link-container">

        <?php 
          include "assets/php/menu.php";
        ?>

      </div>
    </div>
    <div class="col-lg-8 main-content"> 
      <div class="row mb-3">
        <div class="col-lg">
          <div class="card custom-card">
            <div class="card-body">
              <h2 class="text-center">Poe-Stats</h2>
              <hr>
              <h5>Overview</h5>
              <p>This site collects data over time from various items (uniques, gems, currency and the like) in Path of Exile and calculates their average prices. It also provides users a possibility to compare items' prices against previous leagues. This page is still in development and there has not been any official releases yet.</p>
              <h5>The general idea</h5>
              <p>The general goal is to combine the functionality of <a href="http://poe.ninja">poe.ninja</a> and <a href="http://poe-antiquary.xyz">poe-antiquary</a> with the smallest possible budget.</a></p>
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
              <div><em>24/04/2018</em> - Added Cloudflare</div>
              <div><em>24/04/2018</em> - Added exalted prices</div>
              <div><em>23/04/2018</em> - Added support for enchantments</div>
              <div><em>22/04/2018</em> - Changed domains</div>
              <div><em>14/04/2018</em> - Fix Beachhead variants not being indexed</div>
              <div><em>10/04/2018</em> - Fix Kaom's Heart not being indexed</div>
              <div><em>09/04/2018</em> - Add item price history graphs</div>
              <div><em>08/04/2018</em> - Create and add logo</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</div>
<footer class="container-fluid text-center">
  <p>Poe-Stats © 2018</p>
</footer>
<script src="https://ajax.googleapis.com/ajax/libs/jquery/3.3.1/jquery.min.js"></script>
</body>
</html>
