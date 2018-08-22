<?php 
  include_once ( "assets/php/functions.php" );
?>
<!DOCTYPE html>
<html lang="en">
<head>
  <title>Item - PoeWatch</title>
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
<?php GenNavbar() ?>
<!--/Primary navbar/-->
<!-- Page body -->
<div class="container-fluid pb-4">    
  <div class="row">

    <!-- Menu -->
    <?php GenCatMenuHTML() ?>
    <!--/Menu/-->

    <!-- Main content -->
    <div class="col-xl-9 col-lg mt-4">
      <div class="row">
        <div class="col-lg">
          <div class="card custom-card">
            <div class="card-header slim-card-edge"></div>
            <div class="card-body" id="content">

              <!-- Ico+name+league+price row -->
              <div class="row d-flex mx-1">
                <div class="col d-flex p-0">
                
                  <!-- Large ico col -->
                  <div class="d-flex">
                    <div class="img-container img-container-xl mr-3">
                      <img id="item-icon">
                    </div>
                  </div>
                  <!--/Large ico col/-->

                  <!-- Name+league col -->
                  <div class="d-flex flex-column justify-content-around mr-5">
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
                  <div class="d-flex flex-column justify-content-around">
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

              <hr>

              <!-- Small chart row -->
              <div class='row m-1'>
                <div class='col-md'>
                  <h4>Chaos value</h4>
                  <div class='chart-small'><canvas id="chart-price"></canvas></div>
                </div>
                <div class='col-md'>
                  <h4>Listed per 24h</h4>
                  <div class='chart-small'><canvas id="chart-quantity"></canvas></div>
                </div>
              </div>
              <!--/Small chart row/-->

              <hr>

              <!-- Details table row -->
              <div class='row m-1 mt-2'>
                <div class='col-md'>
                  <table class="table table-sm details-table table-striped table-hover">
                    <tbody>
                      <tr>
                        <td>Mean</td>
                        <td>
                          <span class="img-container img-container-xs mr-1">
                            <img src="https://web.poecdn.com/image/Art/2DItems/Currency/CurrencyRerollRare.png?scale=1&w=1&h=1">
                          </span>
                          <span id='details-table-mean'></span>
                        </td>
                      </tr>
                      <tr>
                        <td>Median</td>
                        <td>
                          <span class="img-container img-container-xs mr-1">
                            <img src="https://web.poecdn.com/image/Art/2DItems/Currency/CurrencyRerollRare.png?scale=1&w=1&h=1">
                          </span>
                          <span id='details-table-median'></span>
                        </td>
                      </tr>
                      <tr>
                        <td>Mode</td>
                        <td>
                          <span class="img-container img-container-xs mr-1">
                            <img src="https://web.poecdn.com/image/Art/2DItems/Currency/CurrencyRerollRare.png?scale=1&w=1&h=1">
                          </span>
                          <span id='details-table-mode'></span>
                        </td>
                      </tr>
                    </tbody>
                  </table>
                </div>
                <div class='col-md'>
                  <table class="table table-sm details-table table-striped table-hover">
                    <tbody>
                      <tr>
                        <td>Total amount listed</td>
                        <td>
                          <span id='details-table-count'></span>
                        </td>
                      </tr>
                      <tr>
                        <td>Listed every 24h</td>
                        <td>
                          <span id='details-table-1d'></span>
                        </td>
                      </tr>
                      <tr>
                        <td>Price in exalted</td>
                        <td>
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
                  <h4>Past data</h4>
                  <div class="btn-group btn-group-toggle mt-1 mb-3" data-toggle="buttons" id="history-dataset-radio">
                    <label class="btn btn-sm btn-outline-dark p-0 px-1 active"><input type="radio" name="dataset" value=1>Mean</label>
                    <label class="btn btn-sm btn-outline-dark p-0 px-1"><input type="radio" name="dataset" value=2>Median</label>
                    <label class="btn btn-sm btn-outline-dark p-0 px-1"><input type="radio" name="dataset" value=3>Mode</label>
                    <label class="btn btn-sm btn-outline-dark p-0 px-1"><input type="radio" name="dataset" value=4>Quantity</label>
                  </div>
                  <div class='chart-large'><canvas id="chart-past"></canvas></div>
                </div>
              </div>
              <!--/Past data row/-->

            </div>
            <div class="card-footer slim-card-edge"></div>
          </div>
        </div>
      </div>
    </div>
    <!--/Main content/-->
  </div>
</div>
<!--/Page body/-->

<!-- Footer -->
<?php GenFooter() ?>
<!--/Footer/-->

<!-- Service script -->
<script>
  var ID      = <?php echo $_GET['id']      ?    $_GET['id']        : 'null'; ?>;
  var LEAGUE  = <?php echo $_GET['league']  ? "'{$_GET['league']}'" : 'null'; ?>;
</script>
<!--/Service script/-->
<script src="https://ajax.googleapis.com/ajax/libs/jquery/3.3.1/jquery.min.js"></script>
<script src="https://cdnjs.cloudflare.com/ajax/libs/Chart.js/2.7.2/Chart.min.js"></script>
<script type="text/javascript" src="assets/js/sparkline.js"></script>
<script type="text/javascript" src="assets/js/item.js"></script>
<script src="https://maxcdn.bootstrapcdn.com/bootstrap/4.0.0/js/bootstrap.min.js"></script>
</body>
</html>
