<!DOCTYPE html>
<html lang="en">
<head>
  <title>Poe.ovh - About</title>
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
        Poe.Ovh
      </a>
      <button class="navbar-toggler" type="button" data-toggle="collapse" data-target="#navbarNavDropdown" aria-controls="navbarNavDropdown" aria-expanded="false" aria-label="Toggle navigation">
        <span class="navbar-toggler-icon"></span>
      </button>
      <div class="collapse navbar-collapse" id="navbarNavDropdown">
        <ul class="navbar-nav mr-auto">
          <li class="nav-item"><a class="nav-link" href="/">Front</a></li>
          <li class="nav-item"><a class="nav-link" href="prices">Prices</a></li>
          <li class="nav-item"><a class="nav-link" href="api">API</a></li>
          <li class="nav-item"><a class="nav-link active" href="about">About</a></li>
        </ul>
      </div>
    </div>
  </nav>
  
<div class="container-fluid">    
  <div class="row">
    <div class="col-lg-3"> 
      <div class="list-group sidebar-left" id="sidebar-link-container">
        <?php include "assets/php/menu.php" ?>
      </div>
    </div>

    <div class="col-lg-8 main-content">
    <div class="row">
        <div class="col-lg">
          <div class="alert alert-warning" role="alert">
            <h3 class="alert-heading text-center">Attention!</h3>
            <p>This site is still a work in progress. Data is wiped regularly, API endpoints may change, layout will change.</p>
          </div>
        </div>
      </div> 
      <div class="row mb-3">
        <div class="col-lg">
          <div class="card custom-card">
            <div class="card-body">
              <h2 class="card-title text-center">About</h2>
              <hr>
              <h5>Got a question/suggestion or notice something wrong with an item?</h5>
              <p>Drop me a message @ Siegrest#1851</p>
              <hr>
              <h5>FAQ</h5>
              <p><em>Where do you get your prices?</em><br>The public stash API over at pathofexile.com. Prices are automatically generated from the items players list for sale.</p>
              <p><em>How up to date are the prices?</em><br>All data is recalculated in 1 minute intervals. Prices on the website are always the most recent unless stated otherwise.</p>
              <hr>
              <h5>Legal text</h5>
              <p>As this is a relatively new service, price history for Abyss, Breach, Harbinger and Legacy leagues is provided by <a href="http://poe.ninja">poe.ninja</a> under the <a href="https://creativecommons.org/licenses/by-sa/3.0/">SA 3.0</a> license.</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</div>
<footer class="container-fluid text-center">
  <p>Poe.ovh © 2018</p>
</footer>
<script src="https://ajax.googleapis.com/ajax/libs/jquery/3.3.1/jquery.min.js"></script>
</body>
</html>
