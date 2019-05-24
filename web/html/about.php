<?php 
  require_once "assets/php/pageData.php";
  require_once "assets/php/templates/body.php"; 
  require_once "assets/php/func/feedback.php";
  require_once "../details/pdo.php";

  $PAGEDATA["title"] = "About - PoeWatch";
  $PAGEDATA["description"] = "Information about the site";
  $PAGEDATA["pageHeader"] = "About";
  $postStatus = processPOST($pdo);

  include "assets/php/templates/header.php";
  include "assets/php/templates/navbar.php";
  include "assets/php/templates/priceNav.php";
?>

<?php genBodyHeader() ?>

<div class="col-12 mb-4">
  <div class="card custom-card w-100">
    <div class="card-header">Contact</div>

    <div class="card-body">
        <p>
          Something's not working? Found a typo? The site is missing an amazing feature?
          Do let me know and I'll see what I can do about it. Also, please provide some
          sort of contact address. Discord tag is generally preferred, reddit username
          works too, poe account if you're really disconnected from society. For updates
          and general questions feel free to join the server at
          <a class="custom-text-green" href="https://discord.gg/YTt2AdG">https://discord.gg/YTt2AdG</a>.
        </p>

        <form method="POST">
          <div class="form-group">
            <textarea class='form-control' name='message' placeholder='Type a message' rows='4'><?php if ($postStatus && $postStatus['status'] === 'error' && isset($_POST['message'])) echo htmlentities($_POST['message']) ?></textarea>
          </div>
          <div class="btn-group float-right">
            <input type='text' class='form-control seamless-input' name='contact' value='<?php if ($postStatus && $postStatus['status'] === 'error' && isset($_POST['contact'])) echo htmlentities($_POST['contact']) ?>' placeholder='Contact address'>
            <button type="submit" class="btn btn-outline-dark">Send</button>
          </div>
        </form>

        <?php if ($postStatus): ?>
          <span class='custom-text-<?php echo $postStatus['status'] === "error" ? "red" : "green" ?>'><?php echo $postStatus['message'] ?></span>
        <?php endif ?>
    </div>

    <div class="card-footer slim-card-edge"></div>
  </div>
</div>

<div class="col-12">
  <div class="card custom-card w-100">
    <div class="card-header">FAQ</div>

    <div class="card-body">
      <p>
        Where do you get the prices?
        <br>
        <span class='custom-text-gray-lo'>The official stash API over from pathofexile.com.
            Prices are automatically generated from the items players publicly list for sale.
          </span>
      </p>
      <p>
        How up to date are the prices?
        <br>
        <span class='custom-text-gray-lo'>
          Prices are recalculated within 10 minutes intervals.
          Prices on the website are always the most recent unless stated otherwise.
        </span>
      </p>
      <p>
        What do the 'change', 'daily' and 'total' columns mean?
        <br>
        <span class='custom-text-gray-lo'>
          Change refers to how much the price has changed when comparing the price right now and 7 days ago.
          Daily means how many of that item is listed every 24 hours.
          Total is the total number of times the item has been listed during that league.
        </span>
      </p>
      <p>The currency page is cluttered with too many different items.
        <br>
        <span class='custom-text-gray-lo'>
          Use the Group search option to filter based on Currency, Essences and other groups.
        </span>
      </p>
      <p>
        How do you acquire account and character names?
        <br>
        <span class='custom-text-gray-lo'>
          Through the stash API.
          Meaning that if a player has listed an item in a public stash tab, that character is recorded.
        </span>
      </p>
      <p>
        Why doesn't a character from X league show up in the characters page?
        <br>
        <span class='custom-text-gray-lo'>
          Only users who have listed something on sale using public stash tabs have that specific character recorded.
        </span>
      </p>
      <p>
        Item X has an incredibly high price at league start. Why?
        <br>
        <span class='custom-text-gray-lo'>
          Quite often the first person to find a particular item during
          a new league will list it for much more than it's actually worth.
        </span>
      </p>
      <p>
        Can you make feature X on this site less frustrating to use?
        <br>
        <span class='custom-text-gray-lo'>
          Do let me know and I'll see how it can be improved.
        </span>
      </p>
      <p>
        What are the API limitations?
        <br>
        <span class='custom-text-gray-lo'>
          Based on fair use. Most endpoints are cached for 1 minute.
          Excessively requesting resources will enforce a delay between requests
        </span>
      </p>
      <p>
        Some of the pages don't work with Internet Explorer
        <br><span class='custom-text-gray-lo'>
          More like Internet Explorer doesn't work with some of the pages.
          But seriously, it's an outdated browser.
          You should upgrade to something newer.
        </span>
      </p>
      <p>
        I'm creating a new tool, but I'm unsure about X or have a question about Y.
        <br>
        <span class='custom-text-gray-lo'>
          Drop a message in the contact box and I'll get back to you.
        </span>
      </p>
    </div>

    <div class="card-footer slim-card-edge"></div>
  </div>
</div>

<?php genBodyFooter() ?>

<?php include "assets/php/templates/footer.php" ?>
