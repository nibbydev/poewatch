<?php 
  include_once ( "../details/pdo.php" );
  include_once ( "assets/php/functions.php" );
?>
<!DOCTYPE html>
<html lang="en">
<head>
  <?php GenHeaderMetaTags("About - PoeWatch", "Some information about the site") ?>
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
              <h2 class="text-white">About</h2>
            </div>
            <div class="card-body">
              <h5>Contact</h5>
              <p>At the moment, none.</p>
              <hr>
              <h5>FAQ</h5>
              <p>Where do you get the prices?<br><span class='custom-text-gray'>The public stash API over at pathofexile.com. Prices are automatically generated from the items players list for sale.</span></p>
              <p>How up to date are the prices?<br><span class='custom-text-gray'>All prices are recalculated within 60 second intervals. Prices on the website are always the most recent unless stated otherwise.</span></p>
              <p>How do you acquire account and character names?<br><span class='custom-text-gray'>Through the stash API. Meaning that if a player has listed an item in a public stash tab, that character is recorded.</span></p>
              <p>Is this site related to poe.ninja?<br><span class='custom-text-gray'>No, although I can say it was inspired by it.</span></p>
              <p>What is the purpose of making this?<br><span class='custom-text-gray'>Had to take a programming course at uni and wanted to learn programming. This is the outcome.</span></p>
              <p>What's the benefit of using this over poe.ninja?<br><span class='custom-text-gray'>Additional features such as the ability to view prices from past leagues, see the quantities of items being listed each day, character search, and a sleek UI.</span></p>
              <p>Item X has an incredibly high price at league start. Why?<br><span class='custom-text-gray'>Sometimes the first person to find a particular item during a new league will list it for much more than it's actually worth.</span></p>
              <p>Can you make feature X on this site less frustrating to use.<br><span class='custom-text-gray'>Do let me know through the contact details above and I'll see how it can be improved.</span></p>
              <p>Why is there no data for day one of new leagues?<br><span class='custom-text-gray'>Timezones. Leagues usually start at 8 PM UTC, which leaves a total of 4 hours for day one. Not really enough to get any useful statistics. Which is why the first day is skipped and the next one includes the whole 24 + 4 hours of data.</span></p>
              <hr>
              <h5>Legal text</h5>
              <p>This site uses cookies. By continuing to browse the site, you are agreeing to our use of cookies.</p>
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
