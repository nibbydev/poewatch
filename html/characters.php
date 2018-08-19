<?php 
  include_once ( "assets/php/details/pdo.php" );
  include_once ( "assets/php/functions_characters.php" ); 
?>
<!DOCTYPE html>
<html lang="en">
<head>
  <title>Characters - PoeWatch</title>
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
      PoeWatch
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
          <!-- Search form -->
          <form method="GET">
            <!-- Mode -->
            <div class="row">
              <div class="col">
                <div class="btn-group btn-group-toggle mr-3 mb-3" data-toggle="buttons">
                  <?php CreateModeRadios($DATA); ?>
                </div>

                <div class="btn-group mb-3">
                  <input type="text" class="form-control seamless-input" name="search" placeholder="Name" value="<?php if (isset($_GET["search"])) echo $_GET["search"]; ?>">
                  <button type="submit" class="btn btn-outline-dark">Search</button>
                </div>
              </div>
            </div>
            <!--/Mode/-->

            <?php DisplayNotification($DATA); ?>

          </form>
          <!--/Search form/-->

          <!-- Content card -->
          <?php CreateTable($DATA); ?>
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
