<?php 
  require_once "assets/php/pageData.php";
  require_once "../details/pdo.php";
  require_once "assets/php/func/prices.php"; 
  require_once "assets/php/templates/priceForms.php"; 

  $PAGEDATA["title"] = "Prices - PoeWatch";
  $PAGEDATA["description"] = "Discover the average price of almost any item";
  $PAGEDATA["cssIncludes"][] = "prices.css";
  $PAGEDATA["jsIncludes"][] = "https://cdnjs.cloudflare.com/ajax/libs/Chart.js/2.7.2/Chart.min.js";
  $PAGEDATA["jsIncludes"][] = "prices.js";
  $PAGEDATA["jsIncludes"][] = "sparkline.js";

  // Get list of leagues that have items
  $leagueList = GetItemLeagues($pdo);
  // Get all available categories
  $categories = GetCategories($pdo);
  // Check if query string contains valid params
  CheckQueryParams($leagueList, $categories);
  // Get valid category or default
  $category = isset($_GET["category"]) ? $_GET["category"] : "currency";
  // Get all groups that category
  $groups = GetGroups($pdo, $category);

  include "assets/php/templates/header.php";
  include "assets/php/templates/navbar.php";
  include "assets/php/templates/priceNav.php";
?>

<div id="modal-details" class="modal" tabindex="-1" role="dialog" aria-labelledby="myLargeModalLabel" aria-hidden="true">
  <div class="modal-dialog pw-modal-lg" role="document">

    <div class="modal-content w-100">
      <div class="modal-header d-flex align-items-center">
        <div class="row d-flex justify-content-between w-100 m-0">
          <div class="col-12 col-sm-6 d-flex align-items-center p-0 mb-3 mb-sm-0">
            <span class="img-container img-container-lg text-center mr-1">
              <img id="modal-icon">
            </span>
            <h4 id="modal-name" target="_blank"></h4>
          </div>

          <div class="col-12 col-sm-5 d-flex align-items-center p-0 ml-3 ml-sm-0 justify-content-start justify-content-sm-end">
            <select class="form-control form-control-sm w-auto mr-2" id="modal-leagues"></select>
          </div>
        </div>
        <button type="button" class="close" data-dismiss="modal" aria-label="Close">
          <span aria-hidden="true">&times;</span>
        </button>
      </div>

      <div id="modal-body-buffer" class="modal-body d-flex justify-content-center">
        <div class="row m-0">
          <div class="buffering" id="buffering-modal"></div>
        </div>
      </div>

      <div id="modal-body-content" class="modal-body">
        <div class="container-fluid p-0">
          <div class="row m-0">
            <div class="col-12 col-sm-6 mb-2 mb-sm-0 d-flex">
              <table class="table table-sm details-table table-striped p-0">
                <tbody>
                  <tr>
                    <td class="nowrap w-100">Mean</td>
                    <td class="nowrap">
                      <span class="img-container img-container-xs text-center mr-1">
                        <img src="https://web.poecdn.com/image/Art/2DItems/Currency/CurrencyRerollRare.png?scale=1&amp;w=1&amp;h=1">
                      </span>
                      <span id="modal-mean"></span>
                    </td>
                  </tr>
                  <tr>
                    <td class="nowrap w-100">Median</td>
                    <td class="nowrap">
                      <span class="img-container img-container-xs text-center mr-1">
                        <img src="https://web.poecdn.com/image/Art/2DItems/Currency/CurrencyRerollRare.png?scale=1&amp;w=1&amp;h=1">
                      </span>
                      <span id="modal-median"></span>
                    </td>
                  </tr>
                  <tr>
                    <td class="nowrap w-100">Mode</td>
                    <td class="nowrap">
                      <span class="img-container img-container-xs text-center mr-1">
                        <img src="https://web.poecdn.com/image/Art/2DItems/Currency/CurrencyRerollRare.png?scale=1&amp;w=1&amp;h=1">
                      </span>
                      <span id="modal-mode"></span>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>

            <div class="col-12 col-sm-6 mt-2 mt-sm-0 d-flex">
              <table class="table table-sm details-table table-striped p-0">
                <tbody>
                  <tr>
                    <td class="nowrap pw-100">Total amount listed</td>
                    <td class="nowrap"><span id="modal-total"></span></td>
                  </tr>
                  <tr>
                    <td class="nowrap w-100">Listed every 24h</td>
                    <td class="nowrap"><span id="modal-daily"></span></td>
                  </tr>
                  <tr>
                    <td class="nowrap w-100">Price in exalted</td>
                    <td class="nowrap">
                      <span class="img-container img-container-xs text-center mr-1">
                        <img src="https://web.poecdn.com/image/Art/2DItems/Currency/CurrencyAddModToRare.png?scale=1&amp;w=1&amp;h=1">
                      </span>
                      <span id="modal-exalted"></span>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
          <hr>
          <div class="row m-0">
            <div class="col">
              <h4>Past data</h4>
              <div class="btn-group btn-group-toggle mt-1 mb-3" data-toggle="buttons" id="modal-radio">
                <label class="btn btn-outline-dark btn-sm p-0 px-1 active"><input type="radio" name="dataset" value="1">Mean</label>
                <label class="btn btn-outline-dark btn-sm p-0 px-1"><input type="radio" name="dataset" value="2">Median</label>
                <label class="btn btn-outline-dark btn-sm p-0 px-1"><input type="radio" name="dataset" value="3">Mode</label>
                <label class="btn btn-outline-dark btn-sm p-0 px-1"><input type="radio" name="dataset" value="4">Daily</label>
              </div>

              <div class="chart-large">
                <canvas id="modal-chart" height="250" width="574"></canvas>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="modal-footer slim-card-edge"></div>
    </div>
    
  </div>
</div>



<div class='container-fluid d-flex justify-content-center p-0'>
  <div class='row body-boundaries w-100 px-3 py-3'>
    
    <!-- Search forms -->
    <div class="col-12 p-0 mb-3">
      <div class='d-flex flex-wrap'>
        <?php 
          switch ($category) {
            case "gem":
              corruptedForm();
              levelForm();
              qualityForm();
              break;
            case "armour":
            case "weapon":
              linksForm();
              rarityForm();
              break;
            case "map":
              tierForm();
              rarityForm();
              break;
            case "base":
              itemLevelForm();
              influenceForm();
              break;
            case "flask":
            case "accessory":
            case "jewel":
              rarityForm();
              break;
            default:
              break;
          }
        ?>
      </div>

      <div class='row'>
        <!-- League -->
        <div class="col-6 col-sm">
          <h4 class="nowrap">League</h4>
          <select class="form-control form-control-sm custom-select w-auto" id="search-league">
            <?php foreach ($leagueList as $league): ?>
            <option value="<?php echo $league["name"] ?>"><?php if (!$league["active"]) echo "● "; echo $league['display'] ? $league['display'] : $league['name'] ?></option>
            <?php endforeach ?>
          </select>
        </div>
        <!--//League//-->

        <!-- Confidence -->
        <div class="col-6 col-sm">
          <h4 class="nowrap">Low daily</h4>
          <div class="btn-group btn-group-toggle" data-toggle="buttons" id="radio-confidence">
            <label class="btn btn-outline-dark active">
              <input type="radio" name="confidence" value="false" checked>Hide
            </label>
            <label class="btn btn-outline-dark">
              <input type="radio" name="confidence" value="true">Show
            </label>
          </div>
        </div>
        <!--//Confidence//-->

        <!-- Group -->
        <div class="col-6 col-sm">
          <h4>Group</h4>
          <select class="form-control custom-select" id="search-group">
            <?php AddGroups($groups) ?>
          </select>
        </div>
        <!--//Group//-->

        <!-- Search -->
        <div class="col-6 col-sm">
          <h4>Search</h4>
          <input type="text" class="form-control" id="search-searchbar" placeholder="Search">
        </div>
        <!--//Search//-->
      </div>
    </div>
    <!--//Search forms//-->

    <div class='col-12 p-0'>
      <div class="card custom-card">
        <div class="card-header slim-card-edge"></div>
        <div class="card-body d-flex flex-column p-2">
          <table class="table price-table table-striped table-hover mb-0 table-responsive" id="searchResults">
            <thead>
              <tr>
            
                <?php AddTableHeaders($category) ?>
            
              </tr>
            </thead>
            <tbody></tbody>
          </table>
          <div class="buffering align-self-center mb-2" id="buffering-main"></div>
          <button type="button" class="btn btn-block btn-outline-dark mt-2" id="button-showAll">Show all</button>
        </div>
        <div class="card-footer slim-card-edge"></div>
      </div>
    </div>

  </div>
</div>
<script>
  var SERVICE_leagues = <?php echo json_encode($leagueList) ?>;
</script>
<?php include "assets/php/templates/footer.php" ?>


