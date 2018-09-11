<?php 
  include_once ( "../details/pdo.php" );
  include_once ( "assets/php/functions_prices.php" ); 
  include_once ( "assets/php/functions.php" );

  $SERVICE_category = CheckAndGetCategoryParam();
  $SERVICE_categories = GetCategories($pdo, $SERVICE_category);
  $SERVICE_leagues = GetLeagues($pdo);
?>
<!DOCTYPE html>
<html lang="en">
<head>
  <?php GenHeaderMetaTags("Prices - PoeWatch", "Discover the average price of almost any item") ?>
  <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/4.0.0/css/bootstrap.min.css">
  <link rel="stylesheet" href="assets/css/main.css">
  <link rel="stylesheet" href="assets/css/prices.css">
</head>
<body>
<!-- Primary navbar -->
<?php GenNavbar() ?>
<!--/Primary navbar/-->
<!-- Secondary navbar -->
<div class="container-fluid second-navbar d-flex justify-content-end align-items-center m-0 py-1 px-2"> 
  <div class="form-group live-updates d-flex float-right m-0 mr-3">
    <label for="live-updates" class="m-0 mr-2">Live updates</label>
    <div class="btn-group btn-group-toggle" data-toggle="buttons" id="live-updates">
      <label class="btn btn-sm btn-outline-dark p-0 px-1">
        <input name="live" type="radio" value="true">On
      </label>
      <label class="btn btn-sm btn-outline-dark p-0 px-1 active">
        <input name="live" type="radio" value="false">Off
      </label>
    </div>
  </div>
  <select class="form-control form-control-sm w-auto d-flex float-right" id="search-league">

    <?php AddLeagueSelects($SERVICE_leagues); ?>

  </select>
</div>
<!--/Secondary navbar/-->
<!-- Progressbar -->
<div class="container-fluid p-0 m-0">  
  <div class="progress progressbar-live m-0">
    <div class="progress-bar bg-secondary" role="progressbar" id="progressbar-live"></div>
  </div>
</div>
<!--/Progressbar/-->
<!-- Page body -->
<div class="container-fluid">
  <div class="row">
    <div class="col d-flex my-3">

      <!-- Menu -->
      <?php GenCatMenuHTML() ?>
      <!--/Menu/-->

      <!-- Main content -->
      <div class="d-flex w-100 justify-content-center"> 
        <div class='body-boundaries w-100'>
          <!-- MotD -->
          <?php GenMotDBox(); ?>
          <!--/MotD/-->

          <?php if ($SERVICE_category === "gems"): ?>
          
          <!-- Gem field row -->
          <div class="row mb-3 gem-fields">
            <div class="col-6 col-md-4 mb-2 order-1 order-sm-1 order-md-1 mb-3">
              <h4>Corrupted</h4>
              <div class="btn-group btn-group-toggle" data-toggle="buttons" id="radio-corrupted">
                <label class="btn btn-outline-dark active">
                  <input type="radio" name="corrupted" value="all">Both
                </label>
                <label class="btn btn-outline-dark">
                  <input type="radio" name="corrupted" value="0">No
                </label>
                <label class="btn btn-outline-dark">
                  <input type="radio" name="corrupted" value="1" checked>Yes
                </label>
              </div>
            </div>
            <div class="col-6 col-md-4 mb-2 order-3 order-sm-3 order-md-2">
              <h4>Level</h4>
              <div class="form-group">
                <select class="form-control" id="select-level">
                  <option value="all" selected>All</option>
                  <option value="1">1</option>
                  <option value="2">2</option>
                  <option value="3">3</option>
                  <option value="4">4</option>
                  <option value="20">20</option>
                  <option value="21">21</option>
                </select>
              </div>
            </div>
            <div class="col-6 col-md-4 mb-2 order-5 order-sm-5 order-md-3">
              <h4>Quality</h4>
              <div class="form-group">
                <select class="form-control" id="select-quality">
                  <option value="all" selected>All</option>
                  <option value="0">0</option>
                  <option value="20">20</option>
                  <option value="23">23</option>
                </select>
              </div>
            </div>
            <div class="col-6 col-md-4 mb-2 order-2 order-sm-2 order-md-4 mb-3">
              <h4 class='nowrap'>Low count</h4>
              <div class="btn-group btn-group-toggle" data-toggle="buttons" id="radio-confidence">
                <label class="btn btn-outline-dark active">
                  <input type="radio" name="confidence" value="0" checked><a>Hide</a>
                </label>
                <label class="btn btn-outline-dark">
                  <input type="radio" name="confidence" value="1"><a>Show</a>
                </label>
              </div>
            </div>
            <div class="col-6 col-md-4 mb-2 order-4 order-sm-4 order-md-5">
              <h4>Category</h4>
              <select class="form-control custom-select" id="search-sub">
              
                <?php AddSubCategorySelectors($SERVICE_categories); ?>

              </select>
            </div>
            <div class="col-6 col-md-4 mb-2 order-6 order-sm-6 order-md-6">
              <h4>Search</h4>
              <input type="text" class="form-control" id="search-searchbar" placeholder="Search">
            </div>
          </div>
          <!--/Gem field row/-->

          <?php elseif ($SERVICE_category === "armour" || $SERVICE_category === "weapons"): ?>

          <!-- Link + generic field row -->
          <div class="row mb-3">
            <div class="col-6 col-md-3 mb-2">
              <h4 class='nowrap'>Low count</h4>
              <div class="btn-group btn-group-toggle" data-toggle="buttons" id="radio-confidence">
                <label class="btn btn-outline-dark active">
                  <input type="radio" name="confidence" value="0" checked><a>Hide</a>
                </label>
                <label class="btn btn-outline-dark">
                  <input type="radio" name="confidence" value="1"><a>Show</a>
                </label>
              </div>
            </div>
            <div class="col-6 col-md-3 mb-2 link-fields">
              <h4>Links</h4>
              <select class="form-control custom-select" id="select-links">
                <option value="all" selected>All</option>
                <option value="none">None</option>
                <option value="5">5 Links</option>
                <option value="6">6 Links</option>
              </select>
            </div>
            <div class="col-6 col-md-3 mb-2">
              <h4>Category</h4>
              <select class="form-control custom-select" id="search-sub">

                <?php AddSubCategorySelectors($SERVICE_categories); ?>

              </select>
            </div>
            <div class="col-6 col-md-3 mb-2">
              <h4>Search</h4>
              <input type="text" class="form-control" id="search-searchbar" placeholder="Search">
            </div>
          </div>
          <!--/Link + generic field row/-->

          <?php elseif ($SERVICE_category === "maps"): ?>

          <!-- Map tier + generic field row -->
          <div class="row mb-3">
            <div class="col-6 col-md-3 mb-2">
              <h4 class='nowrap'>Low count</h4>
              <div class="btn-group btn-group-toggle" data-toggle="buttons" id="radio-confidence">
                <label class="btn btn-outline-dark active">
                  <input type="radio" name="confidence" value="0" checked><a>Hide</a>
                </label>
                <label class="btn btn-outline-dark">
                  <input type="radio" name="confidence" value="1"><a>Show</a>
                </label>
              </div>
            </div>
            <div class="col-6 col-md-3 mb-2 link-fields">
              <h4>Tier</h4>
              <div class="form-group">
                <select class="form-control" id="select-tier">
                  <option value="all" selected>All</option>
                  <option value="none">None</option>
                  <option value="1">1</option>
                  <option value="2">2</option>
                  <option value="3">3</option>
                  <option value="4">4</option>
                  <option value="5">5</option>
                  <option value="6">6</option>
                  <option value="7">7</option>
                  <option value="8">8</option>
                  <option value="9">9</option>
                  <option value="10">10</option>
                  <option value="11">11</option>
                  <option value="12">12</option>
                  <option value="13">13</option>
                  <option value="14">14</option>
                  <option value="15">15</option>
                  <option value="16">16</option>
                </select>
              </div>
            </div>
            <div class="col-6 col-md-3 mb-2">
              <h4>Category</h4>
              <select class="form-control custom-select" id="search-sub">

                <?php AddSubCategorySelectors($SERVICE_categories); ?>

              </select>
            </div>
            <div class="col-6 col-md-3 mb-2">
              <h4>Search</h4>
              <input type="text" class="form-control" id="search-searchbar" placeholder="Search">
            </div>
          </div>
          <!--/Map tier + generic field row/-->

          <?php elseif ($SERVICE_category === "bases"): ?>

          <!-- Base + generic field row -->
          <div class="row mb-3">
            <div class="col-6 col-md-3 mb-2">
              <h4 class='nowrap'>Low count</h4>
              <div class="btn-group btn-group-toggle" data-toggle="buttons" id="radio-confidence">
                <label class="btn btn-outline-dark active">
                  <input type="radio" name="confidence" value="0" checked><a>Hide</a>
                </label>
                <label class="btn btn-outline-dark">
                  <input type="radio" name="confidence" value="1"><a>Show</a>
                </label>
              </div>
            </div>
            <div class="col-6 col-md-3 mb-2 link-fields">
              <h4>Ilvl</h4>
              <div class="form-group">
                <select class="form-control" id="select-ilvl">
                  <option value="all" selected>All</option>
                  <option value="68-74">68 - 74</option>
                  <option value="75-82">75 - 82</option>
                  <option value="83-84">83 - 84</option>
                  <option value="85-100">85 - 100</option>
                </select>
              </div>
            </div>
            <div class="col-6 col-md-3 mb-2">
              <h4>Influence</h4>
              <select class="form-control custom-select" id="select-influence">
                <option value="all" selected>All</option>
                <option value="none">None</option>
                <option value="either">Either</option>
                <option value="shaped">Shaper</option>
                <option value="elder">Elder</option>
              </select>
            </div>
            <div class="col-6 col-md-3 mb-2">
              <h4>Category</h4>
              <select class="form-control custom-select" id="search-sub">

                <?php AddSubCategorySelectors($SERVICE_categories); ?>

              </select>
            </div>
            <div class="col-6 col-md-3 mb-2">
              <h4>Search</h4>
              <input type="text" class="form-control" id="search-searchbar" placeholder="Search">
            </div>
          </div>
          <!--/Base + generic field row/-->

          <?php else: ?>

          <!-- Generic field row -->
          <div class="row mb-3">
            <div class="col-6 col-md-3 mb-2">
              <h4 class='nowrap'>Low count</h4>
              <div class="btn-group btn-group-toggle" data-toggle="buttons" id="radio-confidence">
                <label class="btn btn-outline-dark active">
                  <input type="radio" name="confidence" value="0" checked><a>Hide</a>
                </label>
                <label class="btn btn-outline-dark">
                  <input type="radio" name="confidence" value="1"><a>Show</a>
                </label>
              </div>
            </div>
            <div class="col-6 col-md-3 mb-2 offset-md-3">
              <h4>Category</h4>
              <select class="form-control custom-select" id="search-sub">
              
                <?php AddSubCategorySelectors($SERVICE_categories); ?>
              
              </select>
            </div>
            <div class="col-6 col-md-3 mb-2 offset-md-0 offset-6">
              <h4>Search</h4>
              <input type="text" class="form-control" id="search-searchbar" placeholder="Search">
            </div>
          </div>
          <!--/Generic field row/-->

          <?php endif; ?>

          <!-- Main table -->
          <div class="card custom-card">
            <div class="card-header slim-card-edge"></div>
            <div class="card-body d-flex flex-column p-2">
              <table class="table price-table table-striped table-hover mb-0" id="searchResults">
                <thead>
                  <tr>
                
                    <?php AddTableHeaders($SERVICE_category); ?>
                
                  </tr>
                </thead>
                <tbody></tbody>
              </table>
              <div class="buffering align-self-center mb-2"></div>
              <button type="button" class="btn btn-block btn-outline-dark mt-2" id="button-showAll">Show all</button>
            </div>
            <div class="card-footer slim-card-edge"></div>
          </div>
          <!--/Main table/-->

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
<!-- Service script -->
<script>
  var SERVICE_leagues = <?php echo json_encode($SERVICE_leagues); ?>;
  var SERVICE_categories = <?php echo json_encode($SERVICE_categories); ?>;
  var SERVICE_category = <?php echo "\"" . $SERVICE_category . "\"" ?>;
</script>
<!--/Service script/-->
<script src="https://ajax.googleapis.com/ajax/libs/jquery/3.3.1/jquery.min.js"></script>
<script type="text/javascript" src="assets/js/prices.js"></script>
<script type="text/javascript" src="assets/js/sparkline.js"></script>
<script src="https://maxcdn.bootstrapcdn.com/bootstrap/4.0.0/js/bootstrap.min.js"></script>
<script src="https://cdnjs.cloudflare.com/ajax/libs/Chart.js/2.7.2/Chart.min.js"></script>
</body>
</html>
