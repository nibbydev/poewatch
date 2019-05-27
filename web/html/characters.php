<?php
require_once "assets/php/pageData.php";
require_once "../details/pdo.php";
require_once "assets/php/func/characters.php";
require_once "assets/php/templates/body.php";

$PAGE_DATA["title"] = "Characters - PoeWatch";
$PAGE_DATA["description"] = "Find users based on character names, account names and more";
$PAGE_DATA["pageHeader"] = "Character search";

// Page local data
$PAGE_DATA["page"]["searchString"] = isset($_GET["search"]) ? $_GET["search"] : null;
$PAGE_DATA["page"]["searchMode"] = isset($_GET["mode"]) ? $_GET["mode"] : null;
$PAGE_DATA["page"]["totalAccs"] = 0;
$PAGE_DATA["page"]["totalChars"] = 0;
$PAGE_DATA["page"]["errorMsg"] = null;
$PAGE_DATA["page"]["searchResults"] = [];

CheckQueryParamErrors();
GetTotalCounts($pdo);
MakeSearch($pdo);

include "assets/php/templates/header.php";
include "assets/php/templates/navbar.php";
include "assets/php/templates/priceNav.php";
genBodyHeader();
?>
<div class="card custom-card w-100">
  <div class="card-header">
    <div>Explore
      <span class='custom-text-green'><?php echo number_format($PAGE_DATA["page"]["totalAccs"]) ?></span>
      account names and <span class='custom-text-green'><?php echo number_format($PAGE_DATA["page"]["totalChars"]) ?></span>
      character names collected from the stash API since <span class='custom-text-green'>
      <?php echo FormatTimestamp("2018-07-14 00:00:00") ?></span>. Only characters that have listed something through
      the public stash API will appear here. This is also available as an API.
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
            <label
              class="btn btn-outline-dark<?php if ($PAGE_DATA["page"]["searchMode"] === "account" || !$PAGE_DATA["page"]["searchMode"]) echo " active" ?>">
              <input type="radio" name="mode"
                     value="account"<?php if ($PAGE_DATA["page"]["searchMode"] === "account" || !$PAGE_DATA["page"]["searchMode"]) echo " checked" ?>>
              <a>Account</a>
            </label>
            <label
              class="btn btn-outline-dark<?php if ($PAGE_DATA["page"]["searchMode"] === "character") echo " active" ?>">
              <input type="radio" name="mode"
                     value="character" <?php if ($PAGE_DATA["page"]["searchMode"] === "character") echo " checked" ?>>
              <a>Character</a>
            </label>
          </div>

          <div class="btn-group mb-3">
            <input type="text" class="form-control seamless-input" name="search" placeholder="Name"
                   value="<?php if ($PAGE_DATA["page"]["searchString"]) echo htmlentities($_GET["search"]) ?>">
            <button type="submit" class="btn btn-outline-dark">Search</button>
          </div>
        </div>
      </div>
      <!--/Mode/-->
      <?php
      if ($PAGE_DATA["page"]["errorMsg"]) {
        echo "<span class='custom-text-red'>Error: " . $PAGE_DATA["page"]["errorMsg"] . "</span>";
      } else if ($PAGE_DATA["page"]["searchResults"]) {
        echo "<span class='custom-text-green'>" . sizeof($PAGE_DATA["page"]["searchResults"]) . "</span> " .
          "result" . (sizeof($PAGE_DATA["page"]["searchResults"]) === 1 ? "" : "s") . " for " .
          "'<span class='custom-text-orange'>" . htmlentities($PAGE_DATA["page"]["searchString"]) . "</span>'";
      } else if ($PAGE_DATA["page"]["searchString"] && !sizeof($PAGE_DATA["page"]["searchResults"])) {
        echo "<span class='custom-text-red'>Not found!</span>";
      }
      ?>
    </form>
    <!--/Search form/-->

    <?php if ($PAGE_DATA["page"]["searchResults"]) { ?>
      <hr>
      <!-- Content card -->
      <div class='card api-data-table'>
        <table class='table table-striped table-hover mb-0'>
          <thead>
          <tr>
            <th>Account</th>
            <th>Has character</th>
            <th>In league</th>
            <th>Last seen</th>
            <th>Profile</th>
          </tr>
          </thead>
          <tbody>
          <?php foreach ($PAGE_DATA["page"]["searchResults"] as $row) { ?>
            <tr>
              <td>
                <a href='characters?mode=account&search=<?php echo $row["account"] ?>'>
                  <?php if ($PAGE_DATA["page"]["searchMode"] === "account"): ?>
                    <span class='custom-text-orange'><?php echo $row["account"] ?></span>
                  <?php else: ?>
                    <span><?php echo $row["account"] ?></span>
                  <?php endif ?>
                </a>
              </td>
              <td>
                <a href='characters?mode=character&search=<?php echo $row["character"] ?>'>
                  <?php if ($PAGE_DATA["page"]["searchMode"] === "character"): ?>
                    <span class='custom-text-orange'><?php echo $row["character"] ?></span>
                  <?php else: ?>
                    <span><?php echo $row["character"] ?></span>
                  <?php endif ?>
                </a>
              </td>
              <td><?php echo $row["active"] ? $row["league"] : "Standard  ({$row["league"]})" ?></td>
              <td><?php echo FormatTimestamp($row["seen"]) ?></td>
              <td>
                <a href='https://www.pathofexile.com/account/view-profile/<?php echo $row["account"] ?>'
                   target="_blank">
                  <span class="custom-text-green">(New tab ⬈)</span>
                </a>
              </td>
            </tr>
          <?php } ?>
          </tbody>
        </table>
      </div>
      <!--/Content card/-->
    <?php } ?>
  </div>
  <!--/Main card body/-->

  <div class="card-footer slim-card-edge"></div>
</div>
<?php
genBodyFooter();
include "assets/php/templates/footer.php";
?>
