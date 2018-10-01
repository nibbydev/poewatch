<?php 
  include_once ( "../details/pdo.php" );
  include_once ( "assets/php/functions.php" );
?>
<!DOCTYPE html>
<html lang="en">
<head>
  <?php GenHeaderMetaTags("PoeWatch", "A collection of useful resources all combined under one roof") ?>
  <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/4.0.0/css/bootstrap.min.css">
  <link rel="stylesheet" href="assets/css/main.css">
</head>
<body>
<!-- Primary navbar -->
<?php GenNavbar($pdo) ?>
<!--/Primary navbar/-->
<!-- Page body -->
<div class="container-fluid">
  <div class="row">
    <div class="col d-flex my-3">

      <!-- Menu -->
      <?php GenCatMenuHTML($pdo) ?>
      <!--/Menu/-->

      <!-- Main content -->
      <div class="d-flex w-100 justify-content-center"> 
        <div class='body-boundaries w-100'> 
          <div class="card custom-card">
            <div class="card-header">
              <h2 class="text-white">PoeWatch</h2>
            </div>
            <div class="card-body">
              <img src="assets/img/img2.png" class="float-right ml-3 img-fluid">
              <h5>Overview</h5>
              <p>PoeWatch is a Path of Exile statistics and price data collection page that's been in the works since 2017. It gathers data over time for various items (such as uniques, gems, currency, you name it) from public trade listings and finds the average prices. The site is actively in development and welcomes any feedback users may have. </p>
              <h5>The general idea</h5>
              <p>The general goal was to make a statistics website with everything in one place. Users can check prices of almost any item type from the current or past leagues and look up character names.</a></p>
              <h5>The API</h5>
              <p>Of course, all the data displayed here can be accessed through various APIs. Currently available interfaces can be found under the API page.</p>
            </div>
            <div class="card-footer slim-card-edge"></div>
          </div>
        </div>
      </div>
      <!--/Main content/-->

    </div>
  </div>
</div>
<!--/Page body/-->
<!-- Footer -->
<?php GenFooter() ?>
<!--/Footer/-->
<script src="https://ajax.googleapis.com/ajax/libs/jquery/3.3.1/jquery.min.js"></script>
<script src="https://maxcdn.bootstrapcdn.com/bootstrap/4.0.0/js/bootstrap.min.js"></script>
</body>
</html>
