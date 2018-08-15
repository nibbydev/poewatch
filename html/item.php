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
  <link rel="stylesheet" href="assets/css/responsive.css">
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
<!-- Page body -->
<div class="container-fluid pb-4">    
  <div class="row">
    <!-- Menu -->
    <div class="col-xl-3 custom-sidebar-column col-lg-10 offset-xl-0 offset-lg-1 offset-md-0"> 
      <div class="row mt-4 mb-xl-4">

          <?php include ( "assets/php/menu.php" ) ?>

      </div>
    </div>
    <!--/Menu/-->
    <!-- Main content -->
    <div class="col-xl-9 col-lg-10 offset-xl-0 offset-lg-1 offset-md-0 mt-4">
      <div class="row">
        <div class="col-lg">
          <div class="card custom-card">
            <div class="card-header slim-card-edge"></div>
            <div class="card-body">

              <div class="row d-flex mx-1">




<div class="col-8 p-0">
  <div class="pricebox">

    <div class="img-container img-container-xl mr-3">
      <img id="item-icon" src="http://web.poecdn.com/image/Art/2DItems/Currency/CurrencyAddModToRare.png">
    </div>


    <div class="align-self-start">
      <h4 id="item-name">Double Strike has a 15% chance to deal Double Damage to Bleeding Enemies</h4>
    </div>

  </div>
</div>


<div class="col-4 p-0">
  <div class="pricebox">
    <div class="img-container img-container-lg mr-1">
      <img src="http://web.poecdn.com/image/Art/2DItems/Currency/CurrencyRerollRare.png?scale=1&amp;w=1&amp;h=1">
    </div>
    <div class="align-items-center">
      <h4 id="item-chaos">55.45</h4>
    </div>
  </div>

  <div class="pricebox">
    <div class="img-container img-container-lg mr-1">
      <img src="http://web.poecdn.com/image/Art/2DItems/Currency/CurrencyAddModToRare.png?scale=1&amp;w=1&amp;h=1">
    </div>
    <div class="align-items-center">
      <h4 id="item-exalt">0.054</h4>
    </div>
  </div>
</div>









              </div>
              <hr>

              

    <div class='row m-1'>
      <div class='col-sm'>
        <h4>League</h4>
        <select class="form-control form-control-sm small-selector mr-2" id="history-league-selector"></select>
      </div>
    </div>
    <hr>
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
    <hr>
    <div class='row m-1 mt-2'>
      <div class='col-md'>
        <table class="table table-sm details-table table-striped">
          <tbody>
            <tr>
              <td>Mean</td>
              <td>
                <div class='pricebox'>
                  <span class="img-container img-container-xs mr-1">
                    <img src="https://web.poecdn.com/image/Art/2DItems/Currency/CurrencyRerollRare.png?scale=1&w=1&h=1">
                  </span>
                  <span id='details-table-mean'>55.5654</span>
                </div>
              </td>
            </tr>
            <tr>
              <td>Median</td>
              <td>
                <div class='pricebox'>
                  <span class="img-container img-container-xs mr-1">
                    <img src="https://web.poecdn.com/image/Art/2DItems/Currency/CurrencyRerollRare.png?scale=1&w=1&h=1">
                  </span>
                  <span id='details-table-median'>55.5654</span>
                </div>
              </td>
            </tr>
            <tr>
              <td>Mode</td>
              <td>
                <div class='pricebox'>
                  <span class="img-container img-container-xs mr-1">
                    <img src="https://web.poecdn.com/image/Art/2DItems/Currency/CurrencyRerollRare.png?scale=1&w=1&h=1">
                  </span>
                  <span id='details-table-mode'>55.5654</span>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <div class='col-md'>
        <table class="table table-sm details-table table-striped">
          <tbody>
            <tr>
              <div class='pricebox'>
                <td>Total amount listed</td>
                <td>
                  <span id='details-table-count'>55.5654</span>
                </td>
              </div>
            </tr>
            <tr>
              <div class='pricebox'>
                <td>Listed every 24h</td>
                <td>
                  <span id='details-table-1d'>55.5654</span>
                </td>
              </div>
            </tr>
            <tr>
              <td>Price in exalted</td>
              <td>
                <div class='pricebox'>
                  <span class="img-container img-container-xs mr-1">
                    <img src="https://web.poecdn.com/image/Art/2DItems/Currency/CurrencyAddModToRare.png?scale=1&w=1&h=1">
                  </span>
                  <span id='details-table-exalted'>55.5654</span>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
    <hr>
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
<?php include_once ( "assets/php/footer.php" ); ?>
<!--/Footer/-->
<script src="https://ajax.googleapis.com/ajax/libs/jquery/3.3.1/jquery.min.js"></script>
<script src="https://cdnjs.cloudflare.com/ajax/libs/Chart.js/2.7.2/Chart.min.js"></script>
<script type="text/javascript" src="assets/js/sparkline.js"></script>
<script type="text/javascript" src="assets/js/item.js"></script>
<script src="https://maxcdn.bootstrapcdn.com/bootstrap/4.0.0/js/bootstrap.min.js"></script>
</body>
</html>
