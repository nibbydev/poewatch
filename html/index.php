<?php 
  include_once ( "assets/php/functions.php" );
?>
<!DOCTYPE html>
<html lang="en">
<head>
  <title>PoeWatch</title>
  <meta charset="utf-8">
  <link rel="icon" type="image/png" href="assets/img/ico/192.png" sizes="192x192">
  <link rel="icon" type="image/png" href="assets/img/ico/96.png" sizes="96x96">
  <link rel="icon" type="image/png" href="assets/img/ico/32.png" sizes="32x32">
  <link rel="icon" type="image/png" href="assets/img/ico/16.png" sizes="16x16">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/4.0.0/css/bootstrap.min.css">
  <link rel="stylesheet" href="assets/css/main.css">
</head>
<body>
<!-- Primary navbar -->
<?php GenNavbar() ?>
<!--/Primary navbar/-->
<!-- Page body -->
<div class="container-fluid">
  <div class="row">
    <div class="col d-flex my-3">

      <!-- Menu -->
      <?php GenCatMenuHTML() ?>
      <!--/Menu/-->

      <!-- Main content -->
      <div class="d-flex w-100 justify-content-center"> 
        <div class='body-boundaries w-100'> 
          <div class="card custom-card">
            <div class="card-header">
              <h2 class="text-center">PoeWatch</h2>
            </div>
            <div class="card-body">
              <img src="assets/img/img1.png" class="float-right ml-3 img-fluid">
              <h5>Overview</h5>
              <p>A Path of Exile statistics and price data collection page. This site gathers data over time from various items (uniques, gems, currency, you name it) from the ARPG Path of Exile and calculates their average prices. It also provides users a possibility to compare items' prices against previous leagues. This page is still in development and there has not been any official releases yet.</p>
              <h5>The general idea</h5>
              <p>The general goal is to combine the functionality of <a href="http://poe.ninja">poe.ninja</a> and <a href="http://poe-antiquary.xyz">poe-antiquary</a> with a nice sleek style while being as user friendly as possible.</a></p>
              <h5>The API</h5>
              <p>Of course, all the data displayed here can be accessed through an API. Link's in the navbar up top. It's fairly out of date at the moment but will be updated before the release.</p>
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
