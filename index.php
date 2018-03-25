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

<?php
  include "assets/php/header.php";
?>
  
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
          <div class="card custom-card">
            <div class="card-body">
              <h2 class="card-title">Poe.Ovh</h2>
              <hr>
              <h5>Overview</h5>
              <p>Collects data over time from various items on Path of Exile and shows their average price. This page is still in development and there has not been any official releases yet.</p>
              <h5>The API</h5>
              <p>Top-right of the screen - click that, it's pretty self-explanatory. Most API pages are listed there.</p>
            </div>
          </div>
        </div>
      </div>

      <div class="row nested-row">
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

</body>
</html>
