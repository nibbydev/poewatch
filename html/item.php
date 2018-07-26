<!DOCTYPE html>
<html lang="en">
<head>
  <title>Poe-Stats - Item</title>
  <meta charset="utf-8">
  <link rel="icon" type="image/png" href="assets/img/ico/192.png" sizes="192x192">
  <link rel="icon" type="image/png" href="assets/img/ico/96.png" sizes="96x96">
  <link rel="icon" type="image/png" href="assets/img/ico/32.png" sizes="32x32">
  <link rel="icon" type="image/png" href="assets/img/ico/16.png" sizes="16x16">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/4.0.0/css/bootstrap.min.css">
  <link rel="stylesheet" href="assets/css/main.css">
  <link rel="stylesheet" href="assets/css/prices.css">
  <link rel="stylesheet" href="assets/css/item.css">
  <link rel="stylesheet" href="assets/css/responsive.css">
</head>
<body>
<!-- Primary navbar -->
<nav class="navbar navbar-expand-md navbar-dark">
  <div class="container-fluid">
    <a href="/" class="navbar-brand">
      <img src="assets/img/favico.png" class="d-inline-block align-top mr-2">
      Poe-Stats
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

              <div class="row m-1">

<div class="d-flex mr-3">
  <div class="mega-img-container rounded mr-3 p-2 d-flex align-items-center">
    <img id="item-icon" src="assets/img/missing.png">
  </div>


  <div class="d-flex flex-column mr-3">
    <div class="d-flex mr-3 mb-auto">
      <div class="rounded mr-3 p-2 d-flex align-items-center">
        <img src="http://web.poecdn.com/image/Art/2DItems/Currency/CurrencyAddModToRare.png?scale=1&amp;w=1&amp;h=1">
      </div>
      <div class="d-flex align-items-center">
        <h4 id="item-exalt">?</h4>
      </div>
    </div>
    <div class="d-flex mr-3">
      <div class="rounded mr-3 p-2 d-flex align-items-center">
        <img src="http://web.poecdn.com/image/Art/2DItems/Currency/CurrencyRerollRare.png?scale=1&amp;w=1&amp;h=1">
      </div>
      <div class="d-flex align-items-center">
        <h4 id="item-chaos">?</h4>
      </div>
    </div>
  </div>

  <div class="d-flex flex-column">
    <div class="mt-3">
      <h4 id="item-name">?</h4>
    </div>
    <div class="d-flex align-items-center mt-auto mb-3">
      <svg class="mega-sparkline mr-2"></svg>
      <span class="d-flex mr-2"><h4 id="item-change">?</h4></span>
    </div>
  </div>

</div>



              </div>
              <hr>
              <div class="row m-1 mt-2">
                <div class="col-sm">
                  <table class="table table-sm details-table table-striped">
                    <tbody>
                      <tr>
                        <td>Current mean</td>
                        <td id="item-mean">?</td>
                      </tr>
                      <tr>
                        <td>Current median</td>
                        <td id="item-median">?</td>
                      </tr>
                      <tr>
                        <td>Current mode</td>
                        <td id="item-mode">?</td>
                      </tr>
                    </tbody>
                  </table>
                </div>
                <div class="col-sm">
                  <table class="table table-sm details-table table-striped">
                    <tbody>
                      <tr>
                        <td>Total amount listed</td>
                        <td id="item-count">?</td>
                      </tr>
                      <tr>
                        <td>Listed per 24h</td>
                        <td id="item-quantity">?</td>
                      </tr>
                      <tr>
                        <td>Price since 1 week</td>
                        <td id="item-1w">?</td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              </div>
              <hr>
              <div class="row m-1">
                <div class="col-sm">
                  <h4>Chaos value</h4>
                  <div class="chart-small"><canvas id="chart-price"></canvas></div>
                </div>
                <div class="col-sm">
                  <h4>Listed per 24h</h4>
                  <div class="chart-small"><canvas id="chart-quantity"></canvas></div>
                </div>
              </div>
              <hr>
              <div class="row m-1 mb-3">
                <div class="col-sm">
                  <h4>League charts</h4>
                  <div class="mb-1">
                    <label class="m-0 mr-2" for="history-league-radio-new">Current leagues:</label>
                    <div class="btn-group btn-group-toggle" data-toggle="buttons" id="history-league-radio-new"></div>
                  </div>
                  <div class="mb-3">
                    <label class="m-0 mr-2" for="history-league-radio-old">Past leagues: </label>
                    <div class="btn-group btn-group-toggle" data-toggle="buttons" id="history-league-radio-old"></div>
                  </div>
                  <div class="chart-large"><canvas id="chart-past"></canvas></div>
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
<!-- Service script -->
<script>
  var index = <?php echo isset($_GET["index"])    ? "\"" . $_GET["index"]    . "\"" : "null" ?>;
</script>
<!--/Service script/-->
<script src="https://ajax.googleapis.com/ajax/libs/jquery/3.3.1/jquery.min.js"></script>
<script src="https://cdnjs.cloudflare.com/ajax/libs/Chart.js/2.7.2/Chart.min.js"></script>
<script type="text/javascript" src="assets/js/sparkline.js"></script>
<script type="text/javascript" src="assets/js/item.js"></script>
<script src="https://maxcdn.bootstrapcdn.com/bootstrap/4.0.0/js/bootstrap.min.js"></script>
</body>
</html>
