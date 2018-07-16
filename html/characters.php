<?php 
  include_once ( "assets/php/details/pdo.php" );
  include_once ( "assets/php/functions_characters.php" ); 

  $DATA = array(
    "limit"  => 25,
    "count"  => null,
    "page"   => isset($_GET["page"])   && $_GET["page"]   ? intval($_GET["page"])   : 1,
    "search" => isset($_GET["search"]) && $_GET["search"] ? $_GET["search"]         : null,
    "mode"   => isset($_GET["mode"])   && $_GET["mode"]   ? $_GET["mode"]           : "account",
    "exact"  => isset($_GET["exact"])  && $_GET["exact"]  ? !!$_GET["exact"]        : false
  );

  $ERRORCODE = CheckGETVariableError($DATA);

  if (!$ERRORCODE && $DATA["search"]) {
    $DATA["count"] = GetResultCount($pdo, $DATA);
  }

  $displayPagination = ceil($DATA["count"] / $DATA["limit"]) > 1;
?>
<!DOCTYPE html>
<html lang="en">
<head>
  <title>Poe-Stats - Characters</title>
  <meta charset="utf-8">
  <link rel="icon" type="image/png" href="assets/img/ico/192.png" sizes="192x192">
  <link rel="icon" type="image/png" href="assets/img/ico/96.png" sizes="96x96">
  <link rel="icon" type="image/png" href="assets/img/ico/32.png" sizes="32x32">
  <link rel="icon" type="image/png" href="assets/img/ico/16.png" sizes="16x16">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/4.0.0/css/bootstrap.min.css">
  <link rel="stylesheet" href="assets/css/main.css">
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
        <li class="nav-item"><a class="nav-link" href="prices">Prices</a></li>
        <li class="nav-item"><a class="nav-link" href="api">API</a></li>
        <li class="nav-item"><a class="nav-link" href="progress">Progress</a></li>
        <li class="nav-item"><a class="nav-link active" href="characters">Characters</a></li>
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
    <!-- Main content -->
    <div class="col-md-10 offset-md-1 mt-4">
      <div class="card custom-card">
        <div class="card-header">
          <h2 class="text-white">Characters</h3>
          <div>
            <?php DisplayMotD($pdo); ?>
          </div>
        </div>

        <!-- Main card body -->
        <div class="card-body">
          <!-- Search options -->
          <form method="GET">
            <!-- Mode -->
            <div class="row mb-3">
              <div class="col">
                <div class="btn-group btn-group-toggle" data-toggle="buttons">
                  <label class="btn btn-outline-dark <?php SetCheckboxState($DATA, "active", "account"); ?>">
                    <input type="radio" name="mode" value="account" <?php SetCheckboxState($DATA, "checked", "account"); ?>><a>Account</a>
                  </label>
                  <label class="btn btn-outline-dark <?php SetCheckboxState($DATA, "active", "character"); ?>">
                    <input type="radio" name="mode" value="character" <?php SetCheckboxState($DATA, "checked", "character"); ?>><a>Character</a>
                  </label>
                </div>
              </div>
            </div>
            <!--/Mode/-->

            <!-- Search -->
            <div class="row">
              <div class="col-6 col-md-3 mb-2">
                <input type="text" class="form-control" name="search" placeholder="Name" value="<?php if (isset($_GET["search"])) echo $_GET["search"]; ?>">
              </div>

              <div class="col-6 col-md-4 mb-2">
                <button type="submit" class="btn btn-outline-dark">Search</button>
              </div>
            </div>
            <!--/Search/-->

            <?php 
              if ($ERRORCODE) DisplayError($ERRORCODE); 
              else DisplayResultCount($DATA); 
            ?>

          </form>
          <!--/Search options/-->

          <hr>

          <?php if ($displayPagination): ?>
          <!-- Top pagination -->
          <div class="btn-toolbar justify-content-center my-3">
            <div class="btn-group mr-2">
              <?php DisplayPagination($DATA); ?>
            </div>
          </div>
          <!--/Top pagination/-->
          <?php endif; ?>

          <!-- Main table -->
          <div class="card api-data-table">
            <table class="table table-striped table-hover mb-0">
              <thead>
                <tr>
                  <th>Account</th>
                  <th>Has character</th>
                  <th>In league</th>
                  <th>Last seen</th>
                </tr>
              </thead>
              <tbody>

                <?php if (!$ERRORCODE) FillTable($pdo, $DATA); ?>

              </tbody>
            </table>
          </div>
          <!--/Main table/-->

          <?php if ($displayPagination): ?>
          <!-- Bottom pagination -->
          <div class="btn-toolbar justify-content-center mt-3">
            <div class="btn-group mr-2">
              <?php DisplayPagination($DATA); ?>
            </div>
          </div>
          <!--/Bottom pagination/-->
          <?php endif; ?>

        </div>
        <!--/Main card body/-->

        <div class="card-footer slim-card-edge"></div>
      </div>
    </div>
    <!--/Main content/-->
  </div>
</div>
<!--/Page body/-->
<!-- Footer -->
<footer class="container-fluid text-center">
  <p>Poe-Stats © 2018</p>
</footer>
<!--/Footer/-->
<script src="https://ajax.googleapis.com/ajax/libs/jquery/3.3.1/jquery.min.js"></script>
<script src="https://maxcdn.bootstrapcdn.com/bootstrap/4.0.0/js/bootstrap.min.js"></script>
</body>
</html>
