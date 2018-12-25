<?php 
  require_once "assets/php/pageData.php";
  require_once "assets/php/func/leagues.php";

  $PAGEDATA["title"] = "Leagues - PoeWatch";
  $PAGEDATA["description"] = "Countdowns for active and upcoming leagues";
  $PAGEDATA["jsIncludes"][] = "leagues.js";

  $leagueList = getLeagues();
  $leagueData = formatLeagueData($leagueList);
  $leagueCount = sizeof($leagueList);

  include "assets/php/templates/header.php";
  include "assets/php/templates/navbar.php";
  include "assets/php/templates/priceNav.php";
?>

<div class='container-fluid d-flex justify-content-center p-0'>
  <div class='row body-boundaries w-100 py-3'>
    <?php for ($i = 0; $i < $leagueCount; $i++): ?>
      <div class="<?php echo $leagueData[$i]["wrap"] ?>">
        <div class="card custom-card league-element mb-3">
          <div class="card-header h-100">
            <h4 class="card-title nowrap mb-0"><?php echo $leagueData[$i]["title"]; echo $leagueData[$i]["status"] ?></h4>
          </div>
          <div class="card-body px-3 py-2">

            <div class="row">
              <div class="col nowrap">
                <table>
                  <tr>
                    <td class="pr-2">Start:</td>
                    <td><span class="subtext-1"><?php echo $leagueData[$i]["start"] ?></span></td>
                  </tr>
                  <tr>
                    <td class="pr-2">End:</td>
                    <td><span class="subtext-1"><?php echo $leagueData[$i]["end"] ?></span></td>
                  </tr>
                </table>
              </div>

              <div class="col nowrap league-countdown"></div>
            </div>
            
            <div class="league-upcoming d-none" value="<?php echo $leagueList[$i]["upcoming"] ?>"></div>
            <div class="league-active d-none" value="<?php echo $leagueList[$i]["active"] ?>"></div>
            <div class="league-start d-none" value="<?php echo $leagueList[$i]["start"] ?>"></div>
            <div class="league-end d-none" value="<?php echo $leagueList[$i]["end"] ?>"></div>

          </div>
          <div class="card-footer progressbar-box border-0 p-0" style="height: 1.25rem;">
            <div class="progressbar-bar progress-bar-striped progress-bar-animated custom-badge-green rounded-bottom h-100" style="width: 0px;"></div>
          </div>
        </div>
      </div>
    <?php endfor ?>
  </div>
</div>
<?php include "assets/php/templates/footer.php" ?>
