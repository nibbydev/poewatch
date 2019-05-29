<?php
require_once "../details/pdo.php";
require_once "assets/php/pageData.php";
require_once "assets/php/func/leagues.php";
require_once "assets/php/templates/body.php";

$PAGE_DATA["title"] = "Leagues - PoeWatch";
$PAGE_DATA["description"] = "Countdowns for active and upcoming leagues";
$PAGE_DATA["jsIncludes"][] = "main.js";

$leagueData = getLeagueData($pdo);

include "assets/php/templates/header.php";
include "assets/php/templates/navbar.php";
include "assets/php/templates/priceNav.php";
genBodyHeader();
?>
<div class="col p-0">
  <?php if ($leagueData["upcoming"]) { ?>
    <div class="row">
      <div class="col-12 d-flex align-content-center">
        <h1 class="display-4 mb-4 mx-auto">Upcoming leagues</h1>
      </div>

      <?php foreach ($leagueData["upcoming"] as $element) { ?>
        <div class="col-sm-6 col-12">
          <div class="card custom-card element-id mb-3">
            <div class="card-header h-100">
              <h4 class="card-title nowrap mb-0"><?php echo $element["title"] ?></h4>
            </div>
            <div class="card-body px-3 py-2">


              <div class="div nowrap overflow-hidden">
                <table>
                  <tr>
                    <td class="pr-2">Start:</td>
                    <td><span class="subtext-0"><?php echo $element["startDisplay"] ?></span></td>
                  </tr>
                  <tr>
                    <td class="pr-2">End:</td>
                    <td><span class="subtext-0"><?php echo $element["endDisplay"] ?></span></td>
                  </tr>
                  <tr>
                    <td class="pr-2"><?php echo $element["cdTitle1"] ?></td>
                    <td><span class="subtext-0 element-cd-id-1-text"></span></td>
                  </tr>
                  <tr>
                    <td class="pr-2"><?php echo $element["cdTitle2"] ?></td>
                    <td><span class="subtext-0 element-cd-id-2-text"></span></td>
                  </tr>
                </table>
              </div>

              <div class="element-data-upcoming d-none" value="<?php echo $element["upcoming"] ?>"></div>
              <div class="element-data-start d-none" value="<?php echo $element["start"] ?>"></div>
              <div class="element-data-end d-none" value="<?php echo $element["end"] ?>"></div>

            </div>
            <div class="card-footer progressbar-box border-0 p-0" style="height: 1.25rem;">
              <div
                class="element-cdbar-id progress-bar-striped progress-bar-animated custom-badge-green rounded-bottom h-100"
                style="width: 0px;"></div>
            </div>
          </div>
        </div>
      <?php } ?>
    </div>
  <?php } ?>

  <?php if ($leagueData["main"]) { ?>
    <div class="row">
      <div class="col-12 d-flex align-content-center">
        <h1 class="display-4 mb-4 mx-auto">Active leagues</h1>
      </div>

      <?php foreach ($leagueData["main"] as $element) { ?>
        <div class="col-sm-6 col-12">
          <div class="card custom-card element-id mb-3">
            <div class="card-header h-100">
              <h4 class="card-title nowrap mb-0"><?php echo $element["title"] ?></h4>
            </div>
            <div class="card-body px-3 py-2">


              <div class="div nowrap overflow-hidden">
                <table>
                  <tr>
                    <td class="pr-2">Start:</td>
                    <td><span class="subtext-0"><?php echo $element["startDisplay"] ?></span></td>
                  </tr>
                  <tr>
                    <td class="pr-2">End:</td>
                    <td><span class="subtext-0"><?php echo $element["endDisplay"] ?></span></td>
                  </tr>
                  <tr>
                    <td class="pr-2"><?php echo $element["cdTitle1"] ?></td>
                    <td><span class="subtext-0 element-cd-id-1-text"></span></td>
                  </tr>
                  <tr>
                    <td class="pr-2"><?php echo $element["cdTitle2"] ?></td>
                    <td><span class="subtext-0 element-cd-id-2-text"></span></td>
                  </tr>
                </table>
              </div>

              <div class="element-data-upcoming d-none" value="<?php echo $element["upcoming"] ?>"></div>
              <div class="element-data-start d-none" value="<?php echo $element["start"] ?>"></div>
              <div class="element-data-end d-none" value="<?php echo $element["end"] ?>"></div>

            </div>
            <div class="card-footer progressbar-box border-0 p-0" style="height: 1.25rem;">
              <div
                class="element-cdbar-id progress-bar-striped progress-bar-animated custom-badge-green rounded-bottom h-100"
                style="width: 0px;"></div>
            </div>
          </div>
        </div>
      <?php } ?>
    </div>
  <?php } ?>

  <?php if ($leagueData["inactive"]) { ?>
    <div class="row">
      <div class="col-12 d-flex align-content-center">
        <h1 class="display-4 mb-4 mx-auto">Ended leagues</h1>
      </div>

      <?php foreach ($leagueData["inactive"] as $element) { ?>
        <div class="col-lg-4 col-md-6 col-12">
          <div class="card custom-card mb-3">
            <div class="card-header h-100">
              <h4 class="card-title nowrap overflow-hidden mb-0"><?php echo $element["title"] ?></h4>
            </div>
            <div class="card-body px-3 py-2">
              <div class="div nowrap overflow-hidden">
                <table>
                  <tr>
                    <td class="pr-2">Start:</td>
                    <td><span class="subtext-1"><?php echo $element["startDisplay"] ?></span></td>
                  </tr>
                  <tr>
                    <td class="pr-2">End:</td>
                    <td><span class="subtext-1"><?php echo $element["endDisplay"] ?></span></td>
                  </tr>
                </table>
              </div>
            </div>
            <div class="card-footer border-0 p-0" style="height: 1.25rem;">
            </div>
          </div>
        </div>
      <?php } ?>
    </div>
  <?php } ?>
</div>
<?php
genBodyFooter();
include "assets/php/templates/footer.php"
?>
