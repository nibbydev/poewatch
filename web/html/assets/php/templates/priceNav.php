<div class="container-fluid second-navbar d-flex d-xl-none align-items-center justify-content-center m-0 p-0">
  <div class="row body-boundaries w-100 m-0 px-3">
    <?php foreach ($PAGE_DATA["priceCategories"] as $tmp) {
      $highlight = isset($_GET["category"]) && $_GET["category"] === $tmp['name']
        || !isset($_GET["category"]) && $tmp['name'] === "currency" && explode(".", basename($_SERVER['PHP_SELF']))[0] === "prices";
      ?>
      <a class="col-2 col-md-1 p-0 py-1 d-flex justify-content-center pw-hover-light rounded <?php if ($highlight) echo "pw-highlight" ?>"
         href="<?php echo $tmp["href"] ?>" title="<?php echo $tmp["display"] ?>">
        <div class="img-container-sm d-flex justify-content-center align-items-center">
          <img class="img-fluid" src="<?php echo $tmp["icon"] ?>" alt="...">
        </div>
      </a>
    <?php } ?>
  </div>
</div>