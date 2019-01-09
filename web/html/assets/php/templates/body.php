<?php function genBodyHeader() { ?>
<div class='container-fluid d-flex justify-content-center p-0'>
  <div class='row body-boundaries w-100 p-3'>
    
    <div class='col-2 d-none d-xl-block pl-0'>
      <div class='row m-0'>
        <div class="card w-100 custom-card">
          <div class="card-header slim-card-edge"></div>
          <div class="card-body d-flex flex-column p-0">
<?php global $PAGEDATA; foreach($PAGEDATA["priceCategories"] as $category): ?>
            <a class="d-flex align-items-center p-1 pw-hover-light <?php if (isset($_GET["category"]) && $_GET["category"] === $category['name']) echo "pw-highlight " ?>rounded" href="<?php echo $category["href"] ?>" title="<?php echo $category["display"] ?>">
              <div class="img-container-sm d-flex justify-content-center align-items-center mr-1">
                <img class="img-fluid" src="<?php echo $category["icon"] ?>">
              </div>
              <span class="nowrap"><?php echo $category["display"] ?></span>
            </a>
<?php endforeach; ?>
          </div>
          <div class="card-footer slim-card-edge"></div>
        </div>
      </div>
    </div>

    <div class='col-12 col-xl-10'>
      <div class='row'>
<?php } ?>

<?php function genBodyFooter() { ?>
      </div>
    </div>
  </div>
</div>
<?php } ?>
