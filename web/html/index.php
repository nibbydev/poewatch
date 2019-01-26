<?php 
  require_once "assets/php/pageData.php";
  require_once "assets/php/templates/body.php"; 

  $PAGEDATA["pageHeader"] = "PoeWatch";

  include "assets/php/templates/header.php";
  include "assets/php/templates/navbar.php";
  include "assets/php/templates/priceNav.php";
?>

<?php genBodyHeader() ?>
  <div class="card custom-card">
    <div class="card-header slim-card-edge"></div>
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
<?php genBodyFooter() ?>

<?php include "assets/php/templates/footer.php" ?>
