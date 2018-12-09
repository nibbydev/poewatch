<?php 
  include_once ( "../details/pdo.php" );
  include_once ( "assets/php/functions_feedback.php" ); 
  include_once ( "assets/php/functions.php" );
  
  $postStatus = processPOST($pdo);
?>
<!DOCTYPE html>
<html lang="en">
<head>
  <?php GenHeaderMetaTags("Feedback - PoeWatch", "Suggest new features or report broken ones") ?>
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
          
          <!-- Feedback form card -->
          <div class="card custom-card mb-5">
            <div class="card-header">
              <h2 class="text-white">Feedback</h2>
              <div>Something's not working? Found a typo? The site is missing an amazing feature?</div>
              <div>Do let me know and I'll see what I can do about it.</div>
              <div>Also, please provide a contact address, eg email/discord tag/reddit username/poe account or similar.</div>
            </div>

            <!-- Main card body -->
            <div class="card-body">
              <form method="POST">
                <div class="form-group">
                  <?php GenTextArea($postStatus); ?>
                </div>
                <div class="btn-group float-right">
                  <?php GenContactField($postStatus); ?>
                  <button type="submit" class="btn btn-outline-dark">Send</button>
                </div>
              </form>

              <?php GenStatusMessage($postStatus); ?>

            </div>
            <!--/Main card body/-->

            <div class="card-footer slim-card-edge"></div>
          </div>
          <!--/Feedback form card/-->

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
