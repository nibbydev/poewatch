<?php 
  include_once ( "assets/php/functions.php" );
?>
<!DOCTYPE html>
<html lang="en">
<head>
  <title>About - PoeWatch</title>
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
<div class="container-fluid pb-4">    
  <div class="row">
    <!-- Main content -->
    <div class="col-md-10 offset-md-1 mt-4">
      <div class="row">
        <div class="col-lg">
          <div class="card custom-card">
            <div class="card-header">
              <h2 class="text-center">About</h2>
            </div>
            <div class="card-body">
              <h5>Got a question/suggestion or notice something wrong with an item?</h5>
              <p>Drop me a message @ Siegrest#1851</p>
              <hr>
              <h5>FAQ</h5>
              <p><em>Where do you get your prices?</em><br>The public stash API over at pathofexile.com. Prices are automatically generated from the items players list for sale.</p>
              <p><em>How up to date are the prices?</em><br>All data is recalculated within 60 second intervals. Prices on the website are always the most recent unless stated otherwise.</p>
              <p><em>How do you acquire account and character names?</em><br>Through the stash API. Meaning that if a player has listed an item in a public stash tab, that character is recorded.</p>
              <hr>
              <h5>Legal text</h5>
              <p>As this is a relatively new service, price history for Abyss, Breach, Harbinger and Legacy leagues is provided by <a href="http://poe.ninja">poe.ninja</a> under the <a href="https://creativecommons.org/licenses/by-sa/3.0/">SA 3.0</a> license.</p>
              <p>This site uses cookies. By continuing to browse the site, you are agreeing to our use of cookies.</p>
            </div>
            <div class="card-footer slim-card-edge"></div>
          </div>
        </div>
      </div>
    </div>
    <!--/Main content/-->
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
