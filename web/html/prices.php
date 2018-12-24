<?php 
  require_once "assets/php/pageData.php";
  require_once "../details/pdo.php";
  require_once "assets/php/func/prices.php"; 

  $PAGEDATA["title"] = "Prices - PoeWatch";
  $PAGEDATA["description"] = "Discover the average price of almost any item";
  $PAGEDATA["cssIncludes"][] = "prices.css";
  $PAGEDATA["jsIncludes"][] = "prices.js";
  $PAGEDATA["jsIncludes"][] = "sparkline.js";
  $PAGEDATA["jsIncludes"][] = "https://cdnjs.cloudflare.com/ajax/libs/Chart.js/2.7.2/Chart.min.js";

  // Get list of leagues that have items
  $leagueList = GetItemLeagues();
  // Get all available categories
  $categories = GetCategories();
  // Check if query string contains valid params
  CheckQueryParams($leagueList, $categories);
  // Get valid category or default
  $category = isset($_GET["category"]) ? $_GET["category"] : "currency";

  include "assets/php/templates/header.php";
  include "assets/php/templates/navbar.php";
?>

<div class='container-fluid d-flex justify-content-center'>
  <div class='row body-boundaries w-100 py-3'>
    <div class='<?php echo $PAGEDATA["cols"]["priceNav"] ?>'>
      <?php include "assets/php/templates/priceNav.php" ?>
    </div>
    <div class='<?php echo $PAGEDATA["cols"]["mainContent"] ?>'>
      <div class='row m-0'>

        <div class="col d-flex flex-column mb-3 p-0">
          <?php new FormGen($pdo, $category); ?>
        </div>

        <!-- Main table -->
        <div class='col-12 p-0'>
          <div class="card custom-card">
            <div class="card-header slim-card-edge"></div>
            <div class="card-body d-flex flex-column p-2">
              <table class="table price-table table-striped table-hover mb-0 table-responsive" id="searchResults">
                <thead>
                  <tr>
                
                    <?php AddTableHeaders($category); ?>
                
                  </tr>
                </thead>
                <tbody></tbody>
              </table>
              <div class="buffering align-self-center mb-2"></div>
              <button type="button" class="btn btn-block btn-outline-dark mt-2" id="button-showAll">Show all</button>
            </div>
            <div class="card-footer slim-card-edge"></div>
          </div>
        </div>
        <!--/Main table/-->

      </div>
    </div>
  </div>
</div>
<script>
  var SERVICE_leagues = <?php echo json_encode($leagueList); ?>;
</script>
<?php include "assets/php/templates/footer.php" ?>
