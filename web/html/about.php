<?php 
  require_once "assets/php/pageData.php";
  require_once "assets/php/templates/body.php"; 
  require_once "assets/php/func/feedback.php";
  require_once "../details/pdo.php";

  $PAGEDATA["title"] = "About - PoeWatch";
  $PAGEDATA["description"] = "Information about the site";
  $postStatus = processPOST($pdo);

  include "assets/php/templates/header.php";
  include "assets/php/templates/navbar.php";
  include "assets/php/templates/priceNav.php";
?>

<?php genBodyHeader() ?>
  <div class="card custom-card w-100">
    <div class="card-header">
      <h2 class="text-white mb-3">About</h2>

      <ul class="nav nav-tabs card-header-tabs">
        <li class="nav-item">
          <button class="nav-link pagination-btn cursor-pointer active" value='faq'>FAQ</button>
        </li>
        <li class="nav-item">
          <button class="nav-link pagination-btn cursor-pointer" value='feedback'>Feedback</button>
        </li>
        <li class="nav-item">
          <button class="nav-link pagination-btn cursor-pointer" value='legal'>Legal</button>
        </li>
      </ul>
    </div>

    <div class="card-body pagination-page" id='page-faq'>
      <p>Where do you get the prices?<br><span class='custom-text-gray-lo'>The official stash API over from pathofexile.com. Prices are automatically generated from the items players publicly list for sale.</span></p>
      <p>How up to date are the prices?<br><span class='custom-text-gray-lo'>All prices are recalculated within 60 second intervals. Prices on the website are always the most recent unless stated otherwise.</span></p>
      <p>What do the 'change', 'daily' and 'total' columns mean?<br><span class='custom-text-gray-lo'>Change refers to how much the price has changed when comparing the price right now and 7 days ago. Daily means how many of that item is listed every 24 hours. Total is the total number of times the item has been listed during that league.</span></p>
      <p>The currency page is cluttered with too many different items.<br><span class='custom-text-gray-lo'>Use the Group search option to filter based on Currency, Essences and other groups.</span></p>
      <p>How do you acquire account and character names?<br><span class='custom-text-gray-lo'>Through the stash API. Meaning that if a player has listed an item in a public stash tab, that character is recorded.</span></p>
      <p>Why doesn't a character from X league show up in the characters page?<br><span class='custom-text-gray-lo'>Only users who have listed something on sale using public stash tabs have that specific character recorded.</span></p>
      <p>Is this site somehow related to poe.ninja?<br><span class='custom-text-gray-lo'>No, although it was inspired by it.</span></p>
      <p>What's the benefit of using this over poe.ninja?<br><span class='custom-text-gray-lo'>Additional features such as the ability to view prices from past leagues, see the daily totals of items listed each day, character search, linking specific items, saving searches, and a sleek UI.</span></p>
      <p>Item X has an incredibly high price at league start. Why?<br><span class='custom-text-gray-lo'>Quite often the first person to find a particular item during a new league will list it for much more than it's actually worth.</span></p>
      <p>Can you make feature X on this site less frustrating to use?<br><span class='custom-text-gray-lo'>Do let me know and I'll see how it can be improved.</span></p>
      <p>Why is there no data for day one of new leagues?<br><span class='custom-text-gray-lo'>Timezones. Leagues usually start at 8 PM UTC, which leaves a total of 4 hours for day one. Not really enough to get any useful statistics. Which is why the first day is skipped and the next one includes the whole 24 + 4 hours of data.</span></p>
      <p>What are the API limitations?<br><span class='custom-text-gray-lo'>Based on fair use. Though it does have a rate limitation of 4 requests per second - 10 second timeout.</span></p>
      <p>Some of the pages don't work with Internet Explorer<br><span class='custom-text-gray-lo'>More like Internet Explorer doesn't work with some of the pages. But seriously, it's an outdated browser. You should upgrade to something newer.</span></p>
    </div>
    <div class="card-body pagination-page d-none" id='page-feedback'>
      <div>Something's not working? Found a typo? The site is missing an amazing feature?</div>
      <div>Do let me know and I'll see what I can do about it.</div>
      <p>Also, please provide a contact address, eg email/discord tag/reddit username/poe account or similar.</p>
      
      <form method="POST">
        <div class="form-group">
          <?php GenTextArea($postStatus); ?>
        </div>
        <div class="btn-group float-right">
          <?php GenContactField($postStatus); ?>
          <button type="submit" class="btn btn-outline-dark">Send</button>
        </div>
      </form>

      <?php GenStatusMessage($postStatus); ?>

    </div>
    <div class="card-body pagination-page d-none" id='page-legal'>
      <p>This site uses cookies. By continuing to browse the site, you are agreeing to our use of cookies.</p>
    </div>

    <div class="card-footer slim-card-edge"></div>
  </div>
<?php genBodyFooter() ?>

<?php include "assets/php/templates/footer.php" ?>

<script>
  $(document).ready(function() {
    if (tmp = parseQueryParam("page")) {
      $(".pagination-page").addClass("d-none");
      $(".pagination-btn").removeClass("active");
      $("#page-" + tmp).removeClass("d-none");
      $("button.pagination-btn[value='" + tmp +  "']").addClass("active")
    }
  }); 

  $("button.pagination-btn").click(function(){
    $(".pagination-page").addClass("d-none");
    $(".pagination-btn").removeClass("active");
    var page = $(this).val();
    $("#page-" + page).removeClass("d-none");
    $(this).addClass("active")
    if (page === "faq") page = null;
    updateQueryParam("page", page);
  });

  function updateQueryParam(key, value) {
    var url = document.location.href;
    var re = new RegExp("([?&])" + key + "=.*?(&|#|$)(.*)", "gi");
    var hash;

    if (re.test(url)) {
      if (typeof value !== 'undefined' && value !== null) {
        url = url.replace(re, '$1' + key + "=" + value + '$2$3');
      } else {
        hash = url.split('#');
        url = hash[0].replace(re, '$1$3').replace(/(&|\?)$/, '');
        
        if (typeof hash[1] !== 'undefined' && hash[1] !== null) {
          url += '#' + hash[1];
        }
      }
    } else if (typeof value !== 'undefined' && value !== null) {
      var separator = url.indexOf('?') !== -1 ? '&' : '?';
      hash = url.split('#');
      url = hash[0] + separator + key + '=' + value;

      if (typeof hash[1] !== 'undefined' && hash[1] !== null) {
        url += '#' + hash[1];
      }
    }

    history.replaceState({}, "foo", url);
  }

  function parseQueryParam(key) {
    let url = window.location.href;
    key = key.replace(/[\[\]]/g, '\\$&');
    
    var regex = new RegExp('[?&]' + key + '(=([^&#]*)|&|#|$)'),
        results = regex.exec(url);
        
    if (!results   ) return null;
    if (!results[2]) return   '';

    return decodeURIComponent(results[2].replace(/\+/g, ' '));
  }
</script>