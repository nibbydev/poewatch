<?php 
  include_once ( "assets/php/details/pdo.php" );
  include_once ( "assets/php/functions_characters.php" ); 

  $DATA = array(
    "resultLimit" => 25,
    "resultCount" => null,
    "resultOffset" => isset($_POST["offset"]) ? intval($_POST["offset"]) : 0,
    "searchString" => isset($_POST["name"]) ? $_POST["name"] : null,
    "searchType" => isset($_POST["type"]) ? $_POST["type"] : null
  );

  $ERRORCODE = CheckPOSTVariableError();

  if (!$ERRORCODE && $DATA["searchString"]) {
    $DATA["resultCount"] = GetResultCount($pdo, $DATA);
  }
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
    <div class="col-lg-10 offset-lg-1 mt-4">
      <div class="card custom-card">
        <div class="card-header">
          <h2 class="text-white">Characters</h3>
          <div>
            <?php DisplayMotD($pdo); ?>
          </div>
        </div>

        <div class="card-body">
          <form method="POST">
            <!-- Search options -->
            <div class="row">
              <input type="hidden" name="offset" value="0">

              <div class="col-6 col-md-3 mb-2">
                <input type="text" class="form-control" name="name" placeholder="Name" value="<?php if (isset($_POST["name"])) echo $_POST["name"]; ?>">
              </div>

              <div class="col-6 col-md-3 mb-2">
                <div class="btn-group btn-group-toggle" data-toggle="buttons">
                  <label class="btn btn-outline-dark <?php SetCheckboxState("active", "account"); ?>">
                    <input type="radio" name="type" value="account" <?php SetCheckboxState("checked", "account"); ?>><a>Account</a>
                  </label>
                  <label class="btn btn-outline-dark <?php SetCheckboxState("active", "character"); ?>">
                    <input type="radio" name="type" value="character" <?php SetCheckboxState("checked", "character"); ?>><a>Character</a>
                  </label>
                </div>
              </div>

              <div class="col-6">
                <button type="submit" class="btn btn-outline-dark">Search</button>
              </div>

            </div>
            <!--/Search options/-->
          </form>

          <?php 
            if ($ERRORCODE) DisplayError($ERRORCODE); 
            else DisplayResultCount($DATA); 
          ?>

          <hr>

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

          <!-- Pagination -->
          <div class="btn-toolbar justify-content-center mt-3">
            <form method="POST">
              <input type="hidden" name="name" value="<?php echo $DATA["searchString"]; ?>">
              <input type="hidden" name="type" value="<?php echo $DATA["searchType"]; ?>">

              <div class="btn-group mr-2">
                <?php DisplayPagination($DATA); ?>
              </div>

            </form>
          </div>
        <!--/Pagination/-->

        </div>

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
