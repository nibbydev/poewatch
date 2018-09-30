<?php 
  include_once ( "../details/pdo.php" );
  include_once ( "assets/php/functions.php" );
?>
<!DOCTYPE html>
<html lang="en">
<head>
  <?php GenHeaderMetaTags("Item - PoeWatch", "Detailed price history from past leagues for a specific item") ?>
  <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/4.0.0/css/bootstrap.min.css">
  <link rel="stylesheet" href="assets/css/main.css">
  <link rel="stylesheet" href="assets/css/prices.css">
</head>
<body>
<!-- Primary navbar -->
<?php GenNavbar($pdo) ?>
<!--/Primary navbar/-->
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
          <div class="card custom-card">
            <div class="card-header slim-card-edge">

              <div class="content d-none">
            
                <!-- Ico+name+league+price row -->
                <div class="row d-flex mx-0">
                  <div class="col d-flex p-0">
                  
                    <!-- Large ico col -->
                    <div class="d-flex">
                      <div class="img-container img-container-xl mr-3">
                        <img id="item-icon">
                      </div>
                    </div>
                    <!--/Large ico col/-->

                    <!-- Name+league col -->
                    <div class="d-flex flex-column justify-content-around mh-item-largeCol mr-5">
                      <div>
                        <h4 id="item-name"></h4>
                      </div>
                      <div class="d-flex">
                        <h5 class="mr-2">League</h5>
                        <select class="form-control form-control-sm w-auto" id="history-league-selector"></select>
                      </div>
                    </div>
                    <!--/Name+league col/-->

                    <!-- Large price col -->
                    <div class="d-flex flex-column justify-content-around mr-4">
                      <div class="d-flex">
                        <div class="img-container img-container-md mr-1">
                          <img src="https://web.poecdn.com/image/Art/2DItems/Currency/CurrencyRerollRare.png?scale=1&amp;w=1&amp;h=1">
                        </div>
                        <div class="align-self-center">
                          <h4 id="item-chaos"></h4>
                        </div>
                      </div>
                      <div class="d-flex">
                        <div class="img-container img-container-md mr-1">
                          <img src="https://web.poecdn.com/image/Art/2DItems/Currency/CurrencyAddModToRare.png?scale=1&amp;w=1&amp;h=1">
                        </div>
                        <div class="align-self-center">
                          <h4 id="item-exalt"></h4>
                        </div>
                      </div>
                    </div>
                    <!--/Large price col/-->

                  </div>
                </div>
                <!--/Ico+name+league+price row/-->

              </div>

            </div>
            <div class="card-body d-flex flex-column py-2">
              <div class="buffering align-self-center"></div>
              <div class="content d-none">

                <!-- Details table row -->
                <div class='row m-1 mt-2'>
                  <div class='col d-flex'>
                    <table class="table table-sm details-table table-striped table-hover mw-item-dTable mr-4">
                      <tbody>
                        <tr>
                          <td class='nowrap w-100'>Mean</td>
                          <td class='nowrap'>
                            <span class="img-container img-container-xs mr-1">
                              <img src="https://web.poecdn.com/image/Art/2DItems/Currency/CurrencyRerollRare.png?scale=1&w=1&h=1">
                            </span>
                            <span id='details-table-mean'></span>
                          </td>
                        </tr>
                        <tr>
                          <td class='nowrap w-100'>Median</td>
                          <td class='nowrap'>
                            <span class="img-container img-container-xs mr-1">
                              <img src="https://web.poecdn.com/image/Art/2DItems/Currency/CurrencyRerollRare.png?scale=1&w=1&h=1">
                            </span>
                            <span id='details-table-median'></span>
                          </td>
                        </tr>
                        <tr>
                          <td class='nowrap w-100'>Mode</td>
                          <td class='nowrap'>
                            <span class="img-container img-container-xs mr-1">
                              <img src="https://web.poecdn.com/image/Art/2DItems/Currency/CurrencyRerollRare.png?scale=1&w=1&h=1">
                            </span>
                            <span id='details-table-mode'></span>
                          </td>
                        </tr>
                      </tbody>
                    </table>

                    <table class="table table-sm details-table table-striped table-hover mw-item-dTable">
                      <tbody>
                        <tr>
                          <td class='nowrap w-100'>Total amount listed</td>
                          <td class='nowrap'>
                            <span id='details-table-count'></span>
                          </td>
                        </tr>
                        <tr>
                          <td class='nowrap w-100'>Listed every 24h</td>
                          <td class='nowrap'>
                            <span id='details-table-1d'></span>
                          </td>
                        </tr>
                        <tr>
                          <td class='nowrap w-100'>Price in exalted</td>
                          <td class='nowrap'>
                            <span class="img-container img-container-xs mr-1">
                              <img src="https://web.poecdn.com/image/Art/2DItems/Currency/CurrencyAddModToRare.png?scale=1&w=1&h=1">
                            </span>
                            <span id='details-table-exalted'></span>
                          </td>
                        </tr>
                      </tbody>
                    </table>
                  </div>
                </div>
                <!--/Details table row/-->

                <hr>
                
                <!-- Past data row -->
                <div class='row m-1 mb-3'>
                  <div class='col-sm'>
                    <div class="btn-group btn-group-toggle mt-1 mb-3" data-toggle="buttons" id="history-dataset-radio">
                      <label class="btn btn-sm btn-outline-dark p-0 px-1 <?php if (isset($_GET['dataset']) && $_GET['dataset'] ===      'mean') echo 'active' ?>"><input type="radio" name="dataset" value='mean'     <?php if (isset($_GET['dataset']) && $_GET['dataset'] ===     'mean') echo 'checked' ?>>Mean</label>
                      <label class="btn btn-sm btn-outline-dark p-0 px-1 <?php if (isset($_GET['dataset']) && $_GET['dataset'] ===    'median') echo 'active' ?>"><input type="radio" name="dataset" value='median'   <?php if (isset($_GET['dataset']) && $_GET['dataset'] ===   'median') echo 'checked' ?>>Median</label>
                      <label class="btn btn-sm btn-outline-dark p-0 px-1 <?php if (isset($_GET['dataset']) && $_GET['dataset'] ===      'mode') echo 'active' ?>"><input type="radio" name="dataset" value='mode'     <?php if (isset($_GET['dataset']) && $_GET['dataset'] ===     'mode') echo 'checked' ?>>Mode</label>
                      <label class="btn btn-sm btn-outline-dark p-0 px-1 <?php if (isset($_GET['dataset']) && $_GET['dataset'] ===  'quantity') echo 'active' ?>"><input type="radio" name="dataset" value='quantity' <?php if (isset($_GET['dataset']) && $_GET['dataset'] === 'quantity') echo 'checked' ?>>Quantity</label>
                    </div>
                    <div class='chart-large'><canvas id="chart-past"></canvas></div>
                  </div>
                </div>
                <!--/Past data row/-->
              </div>
            </div>
            <div class="card-footer slim-card-edge"></div>
          </div>
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
  var ID = <?php 
    if (isset($_GET['id']) && is_numeric($_GET['id'])) {
      echo $_GET['id'];
    } else {
      echo 'null';
    }
  ?>;
  var LEAGUE  = <?php 
    if (isset($_GET['league'])) {
      echo "'" . htmlentities($_GET['league']) . "'";
    } else {
      echo 'null'; 
    }

  ?>;
</script>
<!--/Service script/-->
<script src="https://ajax.googleapis.com/ajax/libs/jquery/3.3.1/jquery.min.js"></script>
<script src="https://cdnjs.cloudflare.com/ajax/libs/Chart.js/2.7.2/Chart.min.js"></script>
<script type="text/javascript" src="assets/js/item.js"></script>
<script src="https://maxcdn.bootstrapcdn.com/bootstrap/4.0.0/js/bootstrap.min.js"></script>
</body>
</html>
