<?php 
  require_once "assets/php/pageData.php";
  require_once "assets/php/templates/body.php"; 

  $PAGEDATA["title"] = "Lab - PoeWatch";
  $PAGEDATA["description"] = "Just the images from poelab.com";
  $PAGEDATA["headerIncludes"][] = "<meta name='referrer' content='no-referrer'/>";
  $PAGEDATA["jsIncludes"][] = "lab.js";

  include "assets/php/templates/header.php";
  include "assets/php/templates/navbar.php";
  include "assets/php/templates/priceNav.php";
?>

<?php genBodyHeader() ?>
<?php foreach(array("uber", "merciless", "cruel", "normal") as $lab): ?>
  <div class="col-12 mb-3 p-0">
    <div class="card custom-card w-100">
      <div class="card-header">
        <h4 class="card-title mb-0"><?php echo ucfirst($lab) ?> (<span id="pw-lab-<?php echo $lab ?>-status"></span>)</h4>
      </div>
      <div class="card-body p-0">
        <a target="_blank" title="Open <?php echo $lab ?> lab layout in new tab" id="pw-lab-<?php echo $lab ?>">
          <img class="img-fluid">
        </a>
      </div>
    </div>
  </div>
<?php endforeach ?>
<?php genBodyFooter() ?>

<?php include "assets/php/templates/footer.php" ?>