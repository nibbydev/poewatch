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
<div class="container-fluid">    
  <div class="row">
    <div class="col d-flex my-3">

      <!-- Menu -->
      <?php GenCatMenuHTML() ?>
      <!--/Menu/-->

      <!-- Main content -->
      <div class="d-flex w-100 justify-content-center"> 
        <div class='body-boundaries w-100'> 
          <div class="card custom-card">
            <div class="card-header slim-card-edge"></div>
            <div class="card-body d-flex flex-column" id="content">
              <div class="buffering align-self-center mb-2"></div>
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
