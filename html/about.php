<!DOCTYPE html>
<html lang="en">
<head>
  <title>Poe-Stats - About</title>
  <meta charset="utf-8">
  <link rel="icon" type="image/png" href="assets/img/favico.png">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/4.0.0/css/bootstrap.min.css">
  <link rel="stylesheet" href="assets/css/main.css">
</head>
<body>
<!-- Primary navbar -->
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
        <li class="nav-item"><a class="nav-link" href="/">Front</a></li>
        <li class="nav-item"><a class="nav-link" href="prices">Prices</a></li>
        <li class="nav-item"><a class="nav-link" href="api">API</a></li>
        <li class="nav-item"><a class="nav-link active" href="about">About</a></li>
      </ul>
    </div>
  </div>
</nav>
<!--/Primary navbar/-->
<!-- Page body -->
<div class="container-fluid">    
  <div class="row">
    <!-- Menu -->
    <div class="col-xl-3"> 
      <div class="row mt-4 mb-xl-4">

          <?php include ( "assets/php/menu.php" ) ?>

      </div>
    </div>
    <!--/Menu/-->
    <!-- Main content -->
    <div class="col-xl-8 col-lg-10 offset-xl-0 offset-lg-1 offset-md-0 mt-4">
      <div class="row">
        <div class="col-lg">
          <div class="alert custom-card" role="alert">
            <h3 class="alert-heading text-center">Attention!</h3>
            <hr>
            <p>This site is still a work in progress. Data is wiped regularly, API endpoints may change, layout will change.</p>
          </div>
        </div>
      </div> 
      <div class="row mb-3">
        <div class="col-lg">
          <div class="card custom-card">
            <div class="card-body">
            <h5 id="leagueProgressHeader">League progress</h5>
              <div class="progress" style="height: 20px;">
                <div class="progress-bar bg-secondary" role="progressbar" id="leagueProgressBar"></div>
              </div>
            </div>
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
              <p><em>How up to date are the prices?</em><br>All data is recalculated within 60 second intervals. Prices on the website are always the most recent unless stated otherwise.</p>
              <hr>
              <h5>Legal text</h5>
              <p>As this is a relatively new service, price history for Abyss, Breach, Harbinger and Legacy leagues is provided by <a href="http://poe.ninja">poe.ninja</a> under the <a href="https://creativecommons.org/licenses/by-sa/3.0/">SA 3.0</a> license.</p>
              <p>This site uses cookies. By continuing to browse the site, you are agreeing to our use of cookies.</p>
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
<script type="text/javascript">
$(document).ready(function() {
  $.each(JSON.parse( "<?php echo str_replace('"', "'", file_get_contents( dirname( getcwd(), 2) . "/data/length.json" ) ); ?>".replace(/'/g, '"') ), function(index, element) {
    if (element["id"].indexOf("SSF") !== -1 || element["id"] === "Standard" || element["id"].indexOf("Hardcore") !== -1) return;
    $("#leagueProgressBar").css("width", Math.round(element["elapse"] / element["total"] * 100)+"%");
    $("#leagueProgressHeader").text("League progress ("+element["elapse"]+" days out of "+element["total"]+", "+element["remain"]+" remaining)");
  });
})
</script>
</body>
</html>
