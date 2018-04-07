<!DOCTYPE html>
<html lang="en">
<head>
  <title>Poe.ovh</title>
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
          include "assets/php/menu.php"
        ?>

      </div>
    </div>
    <div class="col-lg-8 main-content"> 
      <div class="row mb-3">
        <div class="col-lg">
          <div class="card custom-card">
            <div class="card-body">
              <h2 class="card-title">Poe.Ovh</h2>
              <hr>
              <h5>Overview</h5>
              <p>Collects data over time from various items on Path of Exile and shows their average price. This page is still in development and there has not been any official releases yet.</p>
              <h5>What's to come</h5>
              <p>The general goal is to have this site combine the functionality of <a href="http://poe.ninja">poe.ninja</a> and <a href="http://poe-antiquary.xyz">poe-antiquary</a> without relying on any APIs (apart from the official stash API, of course).</a></p>
              <p>There's a lot to do. Graphs need to be implemented. Search options are clunky. Frontend is a complete mess.</p>
              <h5>Got a question/suggestion?</h5>
              <p>Sucks, I haven't set up any contact forms. But if you really really need to get in touch you can find me on <a href="https://github.com/siegrest">Github</a> or the tool-dev channel on the official PoE Discord.</p>
              <h5>The API</h5>
              <p>Link's in the navbar up top. It's pretty self-explanatory. Most API pages are listed there and info should be generally pretty up to date. There probably will be some small chances to the API but nothing major.</p>
            </div>
          </div>
        </div>
      </div>
      <div class="row">
        <div class="col-lg">
          <div class="alert alert-warning" role="alert">
            <h3 class="alert-heading">Attention!</h3>
            <p>This site is still a work in progress. Data is wiped regularly, API endpoints may change, layout will change.</p>
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
