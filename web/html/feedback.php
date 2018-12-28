<?php 
  require_once "assets/php/pageData.php";
  require_once "assets/php/func/feedback.php";
  require_once "assets/php/templates/body.php"; 
  require_once "../details/pdo.php";

  $PAGEDATA["title"] = "Feedback - PoeWatch";
  $PAGEDATA["description"] = "Suggest new features or report broken ones";

  $postStatus = processPOST($pdo);

  include "assets/php/templates/header.php";
  include "assets/php/templates/navbar.php";
  include "assets/php/templates/priceNav.php";
?>

<?php genBodyHeader() ?>
  <div class="card custom-card w-100">
    <div class="card-header">
      <h2 class="text-white">Feedback</h2>
      <div>Something's not working? Found a typo? The site is missing an amazing feature?</div>
      <div>Do let me know and I'll see what I can do about it.</div>
      <div>Also, please provide a contact address, eg email/discord tag/reddit username/poe account or similar.</div>
    </div>

    <!-- Main card body -->
    <div class="card-body">
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
    <!--/Main card body/-->

    <div class="card-footer slim-card-edge"></div>
  </div>
<?php genBodyFooter() ?>

<?php include "assets/php/templates/footer.php" ?>
