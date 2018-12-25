<?php 
  require_once "assets/php/pageData.php";
  require_once "../details/pdo.php";
  require_once "assets/php/func/characters.php"; 

  $PAGEDATA["title"] = "Characters - PoeWatch";
  $PAGEDATA["description"] = "Find users based on character names, account names and more";


  include "assets/php/templates/header.php";
  include "assets/php/templates/navbar.php";
  include "assets/php/templates/priceNav.php";
?>

<div class='container-fluid d-flex justify-content-center p-0'>
  <div class='row body-boundaries w-100 p-3'>

    <div class="card custom-card w-100">
      <div class="card-header">
        <h2 class="text-white">Characters</h2>
        <div>
          <?php DisplayCharacterMotD($DATA); ?>
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
<?php include "assets/php/templates/footer.php" ?>
