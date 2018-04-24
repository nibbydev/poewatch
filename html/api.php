<!DOCTYPE html>
<html lang="en">
<head>
  <title>Poe-Stats - API</title>
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
          <li class="nav-item"><a class="nav-link" href="/">Front</a></li>
          <li class="nav-item"><a class="nav-link" href="prices">Prices</a></li>
          <li class="nav-item"><a class="nav-link active" href="api">API</a></li>
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
            <div class="card-header">
              <h2>api.poe-stats/get</h2>
            </div>
            <div class="card-body">
              <h5 class="card-title">Description</h5>
              <p class="card-text">Main interface for the price API. Items are listed in decreasing order from most expensive to least expensive. Updated every minute.</p>
              <h5 class="card-title">Request fields</h5>
              <div class="custom-table">
                <table class="table table-hover table-sm">
                  <thead>
                    <tr>
                      <th>Param</th>
                      <th>Required</th>
                      <th>Description</th>
                      <th>Example</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr>
                      <td>league</td>
                      <td>Yes</td>
                      <td>One of the active primary leagues</td>
                      <td>Hardcore Bestiary</td>
                    </tr>
                    <tr>
                      <td>category</td>
                      <td>Yes</td>
                      <td>Parent cateogry</td>
                      <td>Armour</td>
                    </tr>
                  </tbody>
                </table>
              </div>
              <h5 class="card-title">Examples</h5>
              <a class="btn btn-dark mt-1" href="http://api.poe-stats.com/get?league=standard&category=armour">Example</a>
            </div>
          </div>
        </div>
      </div>
      <div class="row mb-3">
        <div class="col-lg">
          <div class="card custom-card">
            <div class="card-header">
              <h2>api.poe-stats.com/itemdata</h2>
            </div>
            <div class="card-body">
              <h5 class="card-title">Description</h5>
              <p class="card-text">Allows retrieving item data based on indexes. Indexes are not, however, permanent and can change (but only on manual intervention). Updated dynamically.</p>
              <h5 class="card-title">Examples</h5>
              <a class="btn btn-dark mt-1" href="http://api.poe-stats.com/itemdata?index=00fa,053e,0729">Example (specific)</a>
              <a class="btn btn-dark mt-1" href="http://api.poe-stats.com/itemdata">Example (all)</a>
            </div>
          </div>
        </div>
      </div>
      <div class="row mb-3">
        <div class="col-lg">
          <div class="card custom-card">
            <div class="card-header">
              <h2>api.poe-stats.com/categories</h2>
            </div>
            <div class="card-body">
              <h5 class="card-title">Description</h5>
              <p class="card-text">Provides a list of catetgories currently in use. Updated dynamically.</p>
              <h5 class="card-title">Examples</h5>
              <a class="btn btn-dark mt-1" href="http://api.poe-stats.com/categories">Example</a>
            </div>
          </div>
        </div>
      </div>
      <div class="row mb-3">
        <div class="col-lg">
          <div class="card custom-card">
            <div class="card-header">
              <h2>api.poe-stats.com/leagues</h2>
            </div>
            <div class="card-body">
              <h5 class="card-title">Description</h5>
              <p class="card-text">Provides a list of current active leagues. Will be sorted so that challenge league is first, followed by the hardcore version of the challenge league. SSF leagues are omitted. Updated every 30 minutes.</p>
              <h5 class="card-title">Examples</h5>
              <a class="btn btn-dark mt-1" href="http://api.poe-stats.com/leagues">Example</a>
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
