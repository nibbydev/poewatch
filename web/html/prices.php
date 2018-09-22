<?php 
  include_once ( "../details/pdo.php" );
  include_once ( "assets/php/functions_prices.php" ); 
  include_once ( "assets/php/functions.php" );

  $SERVICE_category = CheckAndGetCategoryParam();
  $SERVICE_categories = GetCategoryTranslations($pdo);
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
<?php GenNavbar($pdo) ?>
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
      <?php GenCatMenuHTML($pdo) ?>
      <!--/Menu/-->

      <!-- Main content -->
      <div class="d-flex w-100 justify-content-center"> 
        <div class='body-boundaries w-100'>

          <div class="row mb-3">
            <div class='col d-flex flex-column'>

              <?php FormGen::GenForm($SERVICE_category) ?>
              
              <div class='d-flex flex-wrap'>
                <div class='mr-3'>
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

                <div class='ml-auto mr-3'>
                  <h4>Sort</h4>
                  <select class="form-control custom-select" id="search-sub"></select>
                </div>

                <div>
                  <h4>Search</h4>
                  <input type="text" class="form-control" id="search-searchbar" placeholder="Search">
                </div>
              </div>
            </div>
          </div>

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
