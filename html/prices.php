<!DOCTYPE html>
<html lang="en">
<head>
  <title>Poe.ovh - Prices</title>
  <meta charset="utf-8">
  <link rel="icon" type="image/png" href="assets/img/favico.png">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <link rel="stylesheet" href="assets/css/bootstrap.min.css">
  <link rel="stylesheet" href="assets/css/main.css">
  <link rel="stylesheet" href="assets/css/prices.css">
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
          <li class="nav-item"><a class="nav-link active" href="prices">Prices</a></li>
          <li class="nav-item"><a class="nav-link" href="api">API</a></li>
          <li class="nav-item"><a class="nav-link" href="about">About</a></li>
        </ul>
      </div>
    </div>
  </nav>

  <div class="container-fluid p-0">
    <div class="row m-0 py-1 pr-3 second-navbar">
      <div class="col text-right">
        <div class="form-group m-0">
          <div class="btn-group btn-group-toggle" data-toggle="buttons" id="search-league"></div>
        </div>
      </div>
    </div>
  </div>
  
<div class="container-fluid">    
  <div class="row">
    <div class="col-lg-3"> 
      <div class="list-group sidebar-left" id="sidebar-link-container">

        <?php 
          include "assets/php/menu.php" ;
        ?>

      </div>
    </div>
    <div class="col-lg-8 main-content"> 
      <div class="row mb">
        <div class="col-lg"> 
          <h1 id="page-title">.</h1>
        </div>
      </div>
      <div class="row mb">
        <div class="col-sm">
          <h4>Sub-category</h4>
          <select class="form-control custom-select" id="search-sub">
          </select>
        </div>
      </div>
      <div class="row mb-3">
        <div class="col-sm">
          <h4>Low count</h4>
          <div class="btn-group btn-group-toggle" data-toggle="buttons" id="radio-confidence">
            <label class="btn btn-outline-secondary active">
              <input type="radio" name="confidence" value="1" checked>Hide
            </label>
            <label class="btn btn-outline-secondary">
              <input type="radio" name="confidence" value="">Show
            </label>
          </div>
        </div>

      </div>
      <div class="row mb-3">
        <div class="col-sm link-fields">
          <h4>Links</h4>
          <div class="btn-group btn-group-toggle" data-toggle="buttons" id="radio-links">
            <label class="btn btn-outline-secondary active">
              <input type="radio" name="links" value="0" checked>None
            </label>
            <label class="btn btn-outline-secondary">
              <input type="radio" name="links" value="5">5L
            </label>
            <label class="btn btn-outline-secondary">
              <input type="radio" name="links" value="6">6L
            </label>
          </div>
        </div>
      </div>
      <div class="row mb-3 gem-fields">
        <div class="col-sm-4">
          <h4>Level</h4>
          <div class="form-group">
            <select class="form-control" id="select-level">
              <option value="">All</option>
              <option value="1">1</option>
              <option value="2">2</option>
              <option value="3">3</option>
              <option value="4">4</option>
              <option value="10">10</option>
              <option value="20">20</option>
              <option value="21" selected="selected">21</option>
            </select>
          </div>
        </div>
        <div class="col-sm-4">
          <h4>Quality</h4>
          <div class="form-group">
            <select class="form-control" id="select-quality">
              <option value="">All</option>
              <option value="0">0</option>
              <option value="10">10</option>
              <option value="20" selected="selected">20</option>
              <option value="23">23</option>
            </select>
          </div>
        </div>
        <div class="col-sm-4">
          <h4>Corrupted</h4>
          <div class="btn-group btn-group-toggle" data-toggle="buttons" id="radio-corrupted">
            <label class="btn btn-outline-secondary active">
              <input type="radio" name="corrupted" value="">Either
            </label>
            <label class="btn btn-outline-secondary">
              <input type="radio" name="corrupted" value="0">No
            </label>
            <label class="btn btn-outline-secondary">
              <input type="radio" name="corrupted" value="1" checked>Yes
            </label>
          </div>
        </div>
      </div>
      <div class="row mb-3">
        <div class="col-sm">
          <h4>Search</h4>
          <input type="text" class="form-control" id="search-searchbar" placeholder="Search">
        </div>
      </div>
      <div class="row mb-3">
        <div class="col-lg">
          <div class="card custom-card">
            <div class="card-body">
              <table class="table price-table table-hover" id="searchResults">
                <thead><tr></tr></thead>
                <tbody></tbody>
              </table>
              <div class="loadall">
                <p class="text-center"><span class="badge badge-danger">Warning:</span> Some categories (e.g. gems) have ~5000 entries. It will be slow</p>
                <button type="button" class="btn btn-dark btn-block" id="button-loadall">Load more</button>
              </div>
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

<div class="service-container" id="service-leagues" data-payload="<?php echo str_replace('"', "'", file_get_contents( dirname( getcwd(), 2) . "/data/leagues.json" ) ); ?>">
<div class="service-container" id="service-categories" data-payload="<?php echo str_replace('"', "'", file_get_contents( dirname( getcwd(), 2) . "/data/categories.json" ) ); ?>">

<script type="text/javascript" src="assets/js/jquery.min.js"></script>
<script type="text/javascript" src="assets/js/main.js"></script>
<script type="text/javascript" src="assets/js/sparkline.min.js"></script>
<script type="text/javascript" src="assets/js/bootstrap.min.js"></script>
<script src="https://cdnjs.cloudflare.com/ajax/libs/Chart.js/2.7.2/Chart.min.js"></script>
</body>
</html>
