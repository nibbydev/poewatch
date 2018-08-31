<?php 
  include_once ( "assets/php/details/pdo.php" );
  include_once ( "assets/php/functions_progress.php" ); 
  include_once ( "assets/php/functions.php" );

  $SERVICE_leagues = GetLeagues($pdo);
?>
<!DOCTYPE html>
<html lang="en">
<head>
  <?php GenHeaderMetaTags("Progress - PoeWatch", "Countdowns for active and upcoming leagues") ?>
  <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/4.0.0/css/bootstrap.min.css">
  <link rel="stylesheet" href="assets/css/main.css">
</head>
<body>
<!-- Primary navbar -->
<?php GenNavbar() ?>
<!--/Primary navbar/-->
<!-- Page body -->
<div class="container-fluid">
  <div class="row">
    <div class="col d-flex my-3">

      <!-- Menu -->
      <?php GenCatMenuHTML() ?>
      <!--/Menu/-->

      <!-- Main content -->
      <div class="d-flex w-100 justify-content-center"> 
        <div class='body-boundaries w-100'> 
          <!-- MotD -->
          <?php GenMotDBox(); ?>
          <!--/MotD/-->

          <div class="card custom-card">
            <div class="card-header slim-card-edge"></div>
            <div class="card-body pb-0">
              <p class="mb-0">A list of currently active main leagues, their start and end dates, progressbars and countdowns until their end. All dates displayed are in your local timezone.</p>
              <hr>
              <div class="row" id="main"></div>
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
  var SERVICE_leagues = <?php echo json_encode($SERVICE_leagues); ?>;
</script>
<!--/Service script/-->
<script src="https://ajax.googleapis.com/ajax/libs/jquery/3.3.1/jquery.min.js"></script>
<script src="https://maxcdn.bootstrapcdn.com/bootstrap/4.0.0/js/bootstrap.min.js"></script>
<script type="text/javascript" src="assets/js/progress.js"></script>
</body>
</html>
