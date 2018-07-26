<?php 
  include_once ( "assets/php/details/pdo.php" );
  include_once ( "assets/php/functions_characters.php" ); 

  // General page-related data array
  $DATA = array(
    "limit"      => 25,
    "count"      => null,
    "page"       => isset($_GET["page"])   && $_GET["page"]   ? intval($_GET["page"]) : 1,
    "pages"      => null,
    "search"     => isset($_GET["search"]) && $_GET["search"] ? $_GET["search"]       : null,
    "mode"       => isset($_GET["mode"])   && $_GET["mode"]   ? $_GET["mode"]         : "account",
    "totalAccs"  => null,
    "totalChars" => null,
    "totalRels"  => null
  );

  // Check if user-provided parameters are valid
  $ERRORCODE = CheckGETVariableError($DATA);

  // Get total number of unique account and character names
  $DATA = GetTotalCounts($pdo, $DATA);

  // If there was no problem with the user-provided parameters, check how many results there 
  // would be to create accurate pagination
  if (!$ERRORCODE && $DATA["search"]) {
    $DATA["count"] = GetResultCount($pdo, $DATA);

    // Find the number of pages there should be
    $DATA["pages"] = ceil($DATA["count"] / $DATA["limit"]);

    // Check if the user-provided page number exceeds the actual page count
    $ERRORCODE = CheckGETVariableError($DATA);
  } else {
    $DATA["pages"] = ceil($DATA["totalRels"] / $DATA["limit"]);
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
    <div class="col-md-10 offset-md-1 mt-4">
      <div class="card custom-card">
        <div class="card-header">
          <h2 class="text-white">Characters</h3>
          <div>
            <?php DisplayMotD($DATA); ?>
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
            <div class="row mb-3">
              <div class="col">
                <div class="btn-group">
                  <input type="text" class="form-control seamless-input" name="search" placeholder="Name" value="<?php if (isset($_GET["search"])) echo $_GET["search"]; ?>">
                  <button type="submit" class="btn btn-outline-dark">Search</button>
                </div>
              </div>
            </div>
            <!--/Search/-->

            <?php 
              if ($ERRORCODE) DisplayError($ERRORCODE); 
              else DisplayResultCount($DATA); 
            ?>

          </form>
          <!--/Search options/-->

          <!-- Content card -->
          <?php if (!$ERRORCODE && $DATA["search"]): ?>

          <hr>

          <!-- Top pagination -->
          <?php if (!$ERRORCODE) DisplayPagination($DATA, "top"); ?>
          <!--/Top pagination/-->

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

          <!-- Bottom pagination -->
          <?php if (!$ERRORCODE) DisplayPagination($DATA, "bottom"); ?>
          <!--/Bottom pagination/-->

          <?php endif; ?>
          <!--/Content card/-->

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
<?php include_once ( "assets/php/footer.php" ); ?>
<!--/Footer/-->
<script src="https://ajax.googleapis.com/ajax/libs/jquery/3.3.1/jquery.min.js"></script>
<script src="https://maxcdn.bootstrapcdn.com/bootstrap/4.0.0/js/bootstrap.min.js"></script>
</body>
</html>
