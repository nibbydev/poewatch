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
  <script src="assets/js/main.js"></script>
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
        <li class="nav-item active"><a class="nav-link" href="prices">Prices</a></li>
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

        <?php 
          include "assets/php/menu.php" 
        ?>

      </div>
    </div>

    <div class="col-lg-8 main-content"> 
      <div class="row nested-row">
        <div class="col-lg-4"> 

          <?php
            $pageTitle = ucwords(strtolower(trim($_GET["category"])));
            echo "<h1>" . $pageTitle . "</h1>";
          ?>

        </div>
      </div>

      <div class="row nested-row">
        <div class="col-lg-6"> 
          <h4>League</h4>
          <select class="form-control custom-select" id="search-league">

            <?php 
              $jsonFile = json_decode( file_get_contents( dirname(getcwd(), 2) . "/data/leagues.json"), true );
              echo dirname(getcwd(), 2) . "/data/leagues.json";
              foreach ($jsonFile as $leagueName) echo "<option>" . $leagueName . "</option>";
            ?>

          </select>
        </div>

        <div class="col-lg-6">
          <h4>Child category</h4>
          <select class="form-control custom-select" id="search-sub">

            <?php 
              $pageTitle = trim(strtolower($_GET["category"]));
              if (!$pageTitle) $pageTitle = "currency";
              $jsonFile = json_decode( file_get_contents(dirname(getcwd(), 2) . "/data/categories.json"), true );
              echo "<option>All</option>";
              if (!array_key_exists($pageTitle, $jsonFile)) return;
              foreach ($jsonFile[$pageTitle] as $item) echo "<option>" . ucwords($item) . "</option>";
            ?>

          </select>
        </div>
      </div>

      <div class="row nested-row">
        <div class="col-md-6">
          <h4>Low count</h4>
          <div class="btn-group btn-group-toggle" data-toggle="buttons" id="radio-confidence">
            <label class="btn btn-outline-secondary active">
              <input type="radio" name="confidence" value="1" autocomplete="off" checked>Hide
            </label>
            <label class="btn btn-outline-secondary">
              <input type="radio" name="confidence" value="0" autocomplete="off">Show
            </label>
          </div>
        </div>
        
        <?php
          $pageTitle = strtolower(trim($_GET["category"]));
          if ($pageTitle === "armour" || $pageTitle === "weapons") {
            echo "<div class=\"col-md-6\">";
          } else {
            echo "<div class=\"col-md-6\" style=\"display: none\">";
          }
        ?>

        <!--<div class="col-md-6">-->
          <h4>Links</h4>
          <div class="btn-group btn-group-toggle" data-toggle="buttons" id="radio-links">
            <label class="btn btn-outline-secondary active">
              <input type="radio" name="links" value="0" autocomplete="off" checked>None
            </label>
            <label class="btn btn-outline-secondary">
              <input type="radio" name="links" value="5" autocomplete="off">5L
            </label>
            <label class="btn btn-outline-secondary">
              <input type="radio" name="links" value="6" autocomplete="off">6L
            </label>
          </div>
        </div>
      </div>

      <?php
        $pageTitle = strtolower(trim($_GET["category"]));
        if ($pageTitle === "gems") {
          echo "<div class=\"row nested-row\">";
        } else {
          echo "<div class=\"row nested-row\" style=\"display: none\">";
        }
      ?>

      <!--<div class="row nested-row">-->
        <div class="col-md-4">
          <h4>Level</h4>
          <div class="form-group">
            <select class="form-control" id="select-level">
              <option value="-1">All</option>
              <option value="1">1</option>
              <option value="2">2</option>
              <option value="3">3</option>
              <option value="4">4</option>
              <option value="10">10</option>
              <option value="20">20</option>
              <option value="21">21</option>
            </select>
          </div>
        </div>

        <div class="col-md-4">
          <h4>Quality</h4>
          <div class="form-group">
            <select class="form-control" id="select-quality">
              <option value="-1">All</option>
              <option value="0">0</option>
              <option value="10">10</option>
              <option value="20">20</option>
              <option value="23">23</option>
            </select>
          </div>
        </div>

        <div class="col-md-4">
          <h4>Corrupted</h4>
          <div class="btn-group btn-group-toggle" data-toggle="buttons" id="radio-corrupted">
            <label class="btn btn-outline-secondary active">
              <input type="radio" name="corrupted" value="-1" autocomplete="off" checked>Either
            </label>
            <label class="btn btn-outline-secondary">
              <input type="radio" name="corrupted" value="0" autocomplete="off" checked>No
            </label>
            <label class="btn btn-outline-secondary">
              <input type="radio" name="corrupted" value="1" autocomplete="off">Yes
            </label>
          </div>
        </div>
      </div>

      <div class="row nested-row">
        <div class="col-lg">
          <h4>Search</h4>
          <input type="text" class="form-control" id="search-searchbar" placeholder="Search">
        </div>
      </div>

      <div class="row nested-row">
        <div class="col-lg">
          <div class="card custom-card">
            <div class="card-body">
              <div class="custom-table">
                <table class="table table-hover table-sm" id="searchResults">
                  <thead>
                    <tr>
                      <th scope="col">Item</th>

                      <?php
                        $pageTitle = strtolower(trim($_GET["category"]));
                        if ($pageTitle === "gems") {
                          echo "<th scope=\"col\">Level</th>";
                          echo "<th scope=\"col\">Quality</th>";
                          echo "<th scope=\"col\">Corrupted</th>";
                        }
                      ?>

                      <!--<th scope="col">Mean</th>-->
                      <th scope="col">Price (chaos)</th>
                      <!--<th scope="col">Mode</th>-->
                      <th scope="col">Count</th>
                    </tr>
                  </thead>
                  <tbody></tbody>
                </table>
                <button type="button" class="btn btn-dark btn-block" id="button-loadmore">Load more</button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</div>

<footer class="container-fluid bg-dark text-center">
  <p>Footer Text</p>
</footer>

</body>
</html>
