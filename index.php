<!DOCTYPE html>
<html lang="en">
<head>
  <title>Poe.ovh</title>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <link rel="stylesheet" href="assets/css/bootstrap.min.css">
  <link rel="stylesheet" href="assets/css/main.css">
  <script src="assets/js/jquery.min.js"></script>
  <script src="assets/js/bootstrap.min.js"></script>
</head>
<body>

<nav class="navbar navbar-expand-lg navbar-dark bg-dark">
  <div class="container-fluid">
    <a href="/" class="navbar-brand">Poe.Ovh</a>

    <button class="navbar-toggler" type="button" data-toggle="collapse" data-target="#navbarNavDropdown" aria-controls="navbarNavDropdown" aria-expanded="false" aria-label="Toggle navigation">
      <span class="navbar-toggler-icon"></span>
    </button>

    <div class="collapse navbar-collapse" id="navbarNavDropdown">
      <ul class="navbar-nav mr-auto">
        <li class="nav-item active"><a class="nav-link" href="/">Front</a></li>
        <li class="nav-item"><a class="nav-link" href="prices">Prices</a></li>
        <li class="nav-item"><a class="nav-link" href="#">About</a></li>
      </ul>
      <div class="navbar-nav ml-auto">
        <a href="api"><span class="badge badge-secondary">API</span></a>
      </div>
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
      <div class="row nested-row">
        <div class="col-lg">
          <div class="alert alert-warning" role="alert">
            <h3 class="alert-heading">Attention!</h3>
            <p>This site is still a work in progress</p>
            <hr>
            <p>Data is wiped regularly, API endpoints may change, layout will change.</p>
          </div>
        </div>
      </div>

      <div class="row nested-row">
        <div class="col-lg">
          <div class="card custom-card">
            <div class="card-body">
              <h2 class="card-title">Poe.Ovh</h2>
              <h5>Short description</h5>
              <p>Site for interfacing the poe.ovh api for the everyday user</p>
              <h5>The interface</h5>
              <p>It's still in development, I can't be bothered to write a guide at this point</p>
              <h5>The API</h5>
              <p>Top-right of the screen - click that, it's pretty self-explanatory</p>
            </div>
          </div>
        </div>
      </div>

    </div>
  </div>
</div>

<?php
  include "assets/php/footer.php";
?>

</body>
</html>
