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
              <h2 class="text-white mb-3">About</h2>

              <ul class="nav nav-tabs card-header-tabs">
                <li class="nav-item">
                  <button class="nav-link pagination-btn active" value='faq'>FAQ</button>
                </li>
                <li class="nav-item">
                  <button class="nav-link pagination-btn" value='contact'>Contact</button>
                </li>
                <li class="nav-item">
                  <button class="nav-link pagination-btn" value='legal'>Legal</button>
                </li>
              </ul>
            </div>

            <div class="card-body pagination-page" id='page-faq'>
              <p>Where do you get the prices?<br><span class='custom-text-gray-lo'>The official stash API over from pathofexile.com. Prices are automatically generated from the items players publicly list for sale.</span></p>
              <p>How up to date are the prices?<br><span class='custom-text-gray-lo'>All prices are recalculated within 60 second intervals. Prices on the website are always the most recent unless stated otherwise.</span></p>
              <p>What do the 'change' and 'count' columns mean?<br><span class='custom-text-gray-lo'>Change refers to how much the price has changed when comparing the price right now and 7 days ago. Count means how many of that item is listed every 24 hours.</span></p>
              <p>The currency page is cluttered with too many different items.<br><span class='custom-text-gray-lo'>Use the Group search option to filter based on Currency, Essences and other groups.</span></p>
              <p>How do you acquire account and character names?<br><span class='custom-text-gray-lo'>Through the stash API. Meaning that if a player has listed an item in a public stash tab, that character is recorded.</span></p>
              <p>Why doesn't a character from X league show up in the characters page?<br><span class='custom-text-gray-lo'>Only users who have listed something on sale using public stash tabs have that specific character recorded.</span></p>
              <p>Is this site somehow related to poe.ninja?<br><span class='custom-text-gray-lo'>No, although it was inspired by it.</span></p>
              <p>What's the benefit of using this over poe.ninja?<br><span class='custom-text-gray-lo'>Additional features such as the ability to view prices from past leagues, see the quantities of items being listed each day, character search, linking specific items, saving searches, and a sleek UI.</span></p>
              <p>Item X has an incredibly high price at league start. Why?<br><span class='custom-text-gray-lo'>Quite often the first person to find a particular item during a new league will list it for much more than it's actually worth.</span></p>
              <p>Can you make feature X on this site less frustrating to use?<br><span class='custom-text-gray-lo'>Do let me know and I'll see how it can be improved.</span></p>
              <p>Why is there no data for day one of new leagues?<br><span class='custom-text-gray-lo'>Timezones. Leagues usually start at 8 PM UTC, which leaves a total of 4 hours for day one. Not really enough to get any useful statistics. Which is why the first day is skipped and the next one includes the whole 24 + 4 hours of data.</span></p>
              <p>What are the API limitations?<br><span class='custom-text-gray-lo'>Based on fair use. Though it does have a rate limitation of 5 requests per second - 60 second timeout.</span></p>
              <p>Some of the pages don't work with Internet Explorer<br><span class='custom-text-gray-lo'>More like Internet Explorer doesn't work with some of the pages. But seriously, it's an outdated browser. You should upgrade to something newer.</span></p>
            </div>
            <div class="card-body pagination-page d-none" id='page-contact'>Currently none</div>
            <div class="card-body pagination-page d-none" id='page-legal'>
              <div>This site uses cookies. By continuing to browse the site, you are agreeing to our use of cookies.</div>
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
<script>
$("button.pagination-btn").click(function(){
  $(".pagination-page").addClass("d-none");
  $(".pagination-btn").removeClass("active");
  $("#page-" + $(this).val()).removeClass("d-none");
  $(this).addClass("active")
});
</script>
</body>
</html>
