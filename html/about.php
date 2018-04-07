<!DOCTYPE html>
<html lang="en">
<head>
  <title>Poe.ovh - About</title>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <link rel="stylesheet" href="assets/css/bootstrap.min.css">
  <link rel="stylesheet" href="assets/css/main.css">
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
      <div class="row mb-3">
        <div class="col-lg">
          <div class="card custom-card">
            <div class="card-body">
              <h2 class="card-title">About</h2>
              <hr>
              <p>Yeah, most on the info is on the front page.</p>
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

<script src="assets/js/jquery.min.js"></script>
</body>
</html>
