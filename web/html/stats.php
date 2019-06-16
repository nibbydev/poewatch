<?php
require_once "assets/php/pageData.php";
require_once "assets/php/templates/body.php";
require_once "../details/pdo.php";

$PAGE_DATA["title"] = "Stats - PoeWatch";
$PAGE_DATA["description"] = "Statistics about the site";
$PAGE_DATA["pageHeader"] = "Statistics";

$PAGE_DATA["cssIncludes"][] = "https://cdn.jsdelivr.net/chartist.js/latest/chartist.min.css";
$PAGE_DATA["jsIncludes"][] = "https://cdn.jsdelivr.net/chartist.js/latest/chartist.min.js";
$PAGE_DATA["jsIncludes"][] = "chartist-plugin-tooltip2.js";

$page = "count";
if (isset($_GET['type'])) {
  if ($_GET['type'] === "error") {
    $page = "error";
  } elseif ($_GET['type'] === "time") {
    $page = "time";
  }
}

include "assets/php/templates/header.php";
include "assets/php/templates/navbar.php";
include "assets/php/templates/priceNav.php";
?>

<style>
  /* hide chart points */
  .ct-point {stroke-width: 0 !important}
  /* bar width to disable overlap */
  .ct-bar {stroke-width: .25em !important}
</style>

<?php genBodyHeader() ?>

<div class="col-12 p-0">
  <div class="card custom-card w-100 mb-3">
    <div class="card-body">
      <div class="row">
        <div class="col-4 d-flex justify-content-center">
          <button value="count"
                  class="btn btn-block btn-outline-dark <?php if ($page === "count") echo "active" ?> statSelect">
            Count
          </button>
        </div>
        <div class="col-4 d-flex justify-content-center">
          <button value="error"
                  class="btn btn-block btn-outline-dark <?php if ($page === "error") echo "active" ?> statSelect">
            Error
          </button>
        </div>
        <div class="col-4 d-flex justify-content-center">
          <button value="time"
                  class="btn btn-block btn-outline-dark <?php if ($page === "time") echo "active" ?> statSelect">
            Time
          </button>
        </div>
      </div>
    </div>
  </div>
</div>

<div class="col-12 p-0" id="main">

</div>
<?php
genBodyFooter();
include "assets/php/templates/footer.php"
?>
