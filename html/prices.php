<?php 
  include_once ( "assets/php/details/pdo.php" );
  include_once ( "assets/php/functions_prices.php" ); 

  $SERVICE_category = CheckAndGetCategoryParam();
  $SERVICE_categories = GetCategories($pdo, $SERVICE_category);
  $SERVICE_leagues = GetLeagues($pdo);
?>
<!DOCTYPE html>
<html lang="en">
<head>
  <title>Prices - PoeWatch</title>
  <meta charset="utf-8">
  <link rel="icon" type="image/png" href="assets/img/ico/192.png" sizes="192x192">
  <link rel="icon" type="image/png" href="assets/img/ico/96.png" sizes="96x96">
  <link rel="icon" type="image/png" href="assets/img/ico/32.png" sizes="32x32">
  <link rel="icon" type="image/png" href="assets/img/ico/16.png" sizes="16x16">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/4.0.0/css/bootstrap.min.css">
  <link rel="stylesheet" href="assets/css/main.css">
  <link rel="stylesheet" href="assets/css/prices.css">
</head>
<body>
<!-- Primary navbar -->
<nav class="navbar navbar-expand-md navbar-dark">
  <div class="container-fluid">
    <a href="/" class="navbar-brand">
      <img src="assets/img/favico.png" class="d-inline-block align-top mr-2">
      PoeWatch
    </a>
    <button class="navbar-toggler" type="button" data-toggle="collapse" data-target="#navbarNavDropdown" aria-controls="navbarNavDropdown" aria-expanded="false" aria-label="Toggle navigation">
      <span class="navbar-toggler-icon"></span>
    </button>
    <div class="collapse navbar-collapse" id="navbarNavDropdown">
      <ul class="navbar-nav mr-auto">
        <li class="nav-item"><a class="nav-link" href="/">Front</a></li>
        <li class="nav-item"><a class="nav-link active" href="prices">Prices</a></li>
        <li class="nav-item"><a class="nav-link" href="api">API</a></li>
        <li class="nav-item"><a class="nav-link" href="progress">Progress</a></li>
        <li class="nav-item"><a class="nav-link" href="characters">Characters</a></li>
        <li class="nav-item"><a class="nav-link" href="easybuyout">EasyBuyout</a></li>
        <li class="nav-item"><a class="nav-link" href="about">About</a></li>
      </ul>
    </div>
  </div>
</nav>
<!--/Primary navbar/-->
<!-- Secondary navbar -->
<div class="container-fluid second-navbar d-inline-block m-0 py-1 pr-3">
  <select class="form-control form-control-sm w-auto d-inline-block float-right ml-2" id="search-league">

    <?php AddLeagueSelects($SERVICE_leagues); ?>

  </select>
  <div class="form-group live-updates float-right m-0">
    <label for="live-updates" class="m-0">Live updates</label>
    <div class="btn-group btn-group-toggle" data-toggle="buttons" id="live-updates">
      <label class="btn btn-sm btn-outline-dark p-0 px-1">
        <input name="live" type="radio" value="true">On
      </label>
      <label class="btn btn-sm btn-outline-dark p-0 px-1 active">
        <input name="live" type="radio" value="false">Off
      </label>
    </div>
  </div>
</div>
<!--/Secondary navbar/-->
<div class="container-fluid p-0 m-0">  
  <div class="progress progressbar-live m-0">
    <div class="progress-bar bg-secondary" role="progressbar" id="progressbar-live"></div>
  </div>
</div>
<!-- Page body -->
<div class="container-fluid pb-4">    
  <div class="row">
    <!-- Menu -->
    <div class="col-xl-3 custom-sidebar-column"> 
      <div class="row mt-4 mb-xl-4">

          <?php include ( "assets/php/menu.php" ) ?>

      </div>
    </div>
    <!--/Menu/-->
    <!-- Main content -->
    <div class="col-xl-9 col-lg mt-4"> 
      <!-- Title row -->
      <div class="row d-none d-xl-block mb-3">
        <div class="col-xl col-lg-8 col-md-8 col-sm"> 
          <div class="card custom-card">
            <div class="card-header slim-card-edge"></div>
            <div class="card-body p-1">

              <?php AddMotdMessage($SERVICE_category); ?>

            </div>
            <div class="card-footer slim-card-edge"></div>
          </div>
        </div>
      </div>
      <!--/Title row/-->
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
              <option value="10">10</option>
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
              <option value="10">10</option>
              <option value="20">20</option>
              <option value="23">23</option>
            </select>
          </div>
        </div>
        <div class="col-6 col-md-4 mb-2 order-2 order-sm-2 order-md-4 mb-3">
          <h4>Low count</h4>
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
          <h4>Low count</h4>
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
          <div class="btn-group btn-group-toggle" data-toggle="buttons" id="radio-links">
            <label class="btn btn-outline-dark active">
              <input type="radio" name="links" value="none" checked>None
            </label>
            <label class="btn btn-outline-dark">
              <input type="radio" name="links" value="5">5L
            </label>
            <label class="btn btn-outline-dark">
              <input type="radio" name="links" value="6">6L
            </label>
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
      <!--/Link + generic field row/-->

      <?php elseif ($SERVICE_category === "maps"): ?>

      <!-- Map tier + generic field row -->
      <div class="row mb-3">
        <div class="col-6 col-md-3 mb-2">
          <h4>Low count</h4>
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
              <option value="1">1</option>
              <option value="2">2</option>
              <option value="3">3</option>
              <option value="4">3</option>
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

      <?php else: ?>

      <!-- Generic field row -->
      <div class="row mb-3">
        <div class="col-6 col-md-3 mb-2">
          <h4>Low count</h4>
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
      <div class="row">
        <div class="col-lg">
          <div class="card custom-card">
            <div class="card-header slim-card-edge"></div>
            <div class="card-body p-2">
              <table class="table price-table table-striped table-hover mb-0" id="searchResults">
                <thead>
                  <tr>
                
                    <?php AddTableHeaders($SERVICE_category); ?>
                
                  </tr>
                </thead>
                <tbody></tbody>
              </table>
              <div class="loadall mt-2">
                <button type="button" class="btn btn-block btn-outline-dark" id="button-loadall">Load more</button>
              </div>
            </div>
            <div class="card-footer slim-card-edge"></div>
          </div>
        </div>
      </div>
      <!--/Main table/-->
    </div>
    <!--/Main content/-->
  </div>
</div>
<!--/Page body/-->
<!-- Footer -->
<?php include_once ( "assets/php/footer.php" ); ?>
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
<link rel="stylesheet" href="assets/css/responsive.css">
</body>
</html>
