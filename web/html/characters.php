<?php 
  include_once ( "../details/pdo.php" );
  include_once ( "assets/php/functions_characters.php" ); 
  include_once ( "assets/php/functions.php" );
?>
<!DOCTYPE html>
<html lang="en">
<head>
  <?php GenHeaderMetaTags("Characters - PoeWatch", "Find users based on character names, account name changes and much more") ?>
  <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/4.0.0/css/bootstrap.min.css">
  <link rel="stylesheet" href="assets/css/main.css">
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

          <?php GenMotDBox(); ?>

          <div class="card custom-card">
            <div class="card-header">
              <h2 class="text-white">Characters</h2>
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
                      <input type="text" class="form-control seamless-input" name="search" placeholder="Name" value="<?php if (isset($_GET["search"])) echo htmlentities($_GET["search"]); ?>">
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
      </div>
      <!--/Main content/-->

    </div>
  </div>
</div>
<!--/Page body/-->
<!-- Footer -->
<?php GenFooter() ?>
<!--/Footer/-->
<script src="https://ajax.googleapis.com/ajax/libs/jquery/3.3.1/jquery.min.js"></script>
<script src="https://maxcdn.bootstrapcdn.com/bootstrap/4.0.0/js/bootstrap.min.js"></script>
</body>
</html>
