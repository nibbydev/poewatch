<!DOCTYPE html>
<html lang="en">
<head>
  <title>Poe.ovh - Prices</title>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <link rel="stylesheet" href="assets/css/bootstrap.min.css">
  <link rel="stylesheet" href="assets/css/main.css">
  <link rel="stylesheet" href="assets/css/prices.css">
  <script type="text/javascript" src="assets/js/jquery.min.js"></script>
  <script type="text/javascript" src="assets/js/bootstrap.min.js"></script>
  <script type="text/javascript" src="assets/js/sparkline.min.js"></script>
  <script type="text/javascript" src="assets/js/main.js"></script>
</head>
<body>

<?php
  include "assets/php/header.php";
?>
  <div class="container-fluid p-0">
    <div class="row justify-content-end m-0 py-1 pr-3 second-navbar">
      <div class="col text-right">
        <span>Selected league: <button class='btn btn-link p-0'>Bestiary</button></span>
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
      <div class="row mb-3">
        <div class="col-lg-4"> 

          <?php
            $pageTitle = ucwords(strtolower(trim($_GET["category"])));
            echo "<h1>" . $pageTitle . "</h1>";
          ?>

        </div>
      </div>
      <div class="row mb-3">
        <div class="col-sm-6"> 
          <h4>League</h4>
          <select class="form-control custom-select" id="search-league">

            <?php 
              $jsonFile = json_decode( file_get_contents( dirname(getcwd(), 2) . "/data/leagues.json"), true );
              foreach ($jsonFile as $leagueName) echo "<option>" . $leagueName . "</option>";
            ?>

          </select>
        </div>
        <div class="col-sm-6">
          <h4>Sub-category</h4>
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
      <div class="row mb-3">
        <div class="col-md-6 link-fields">
          <h4>Links</h4>
          <div class="btn-group btn-group-toggle" data-toggle="buttons" id="radio-links">
            <label class="btn btn-outline-secondary active">
              <input type="radio" name="links" value="" checked>None
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
        <div class="col-sm-9">
          <h4>Search</h4>
          <input type="text" class="form-control" id="search-searchbar" placeholder="Search">
        </div>
        <div class="col-sm-3">
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
        <div class="col-lg">
          <div class="card custom-card">
            <div class="card-body">
              <table class="table price-table table-hover" id="searchResults">
                <thead>
                  <tr>
                  
                    <?php 
                      include "assets/php/price_table_header.php";
                    ?>
                    
                  </tr>
                </thead>
                <tbody>
                </tbody>
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

<?php 
  include "assets/php/footer.php";
?>

<script src="https://cdnjs.cloudflare.com/ajax/libs/Chart.js/2.7.2/Chart.min.js"></script>
</body>
</html>
