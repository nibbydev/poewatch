<?php
require_once "assets/php/pageData.php";
require_once "../details/pdo.php";
require_once "assets/php/func/characters.php";
require_once "assets/php/templates/body.php";

$PAGE_DATA["title"] = "Characters - PoeWatch";
$PAGE_DATA["description"] = "Find users based on character names, account names and more";
$PAGE_DATA["pageHeader"] = "Character search";

// Page local data
$counts = GetTotalCounts($pdo);
$PAGE_DATA["page"]["totalAccs"] = $counts["totalAccs"];
$PAGE_DATA["page"]["totalChars"] = $counts["totalChars"];

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

    <!-- Search input -->
    <div class="d-flex align-items-center mb-3">
      <div class="btn-group btn-group-toggle mr-3" data-toggle="buttons" id="search-mode">
        <label class="btn btn-outline-dark active">
          <input type="radio" name="mode" value="account" checked>
          <a>Account</a>
        </label>
        <label class="btn btn-outline-dark">
          <input type="radio" name="mode" value="character">
          <a>Character</a>
        </label>
      </div>

      <div class="btn-group mr-3">
        <input type="text" class="form-control seamless-input" name="search" placeholder="Name" id="search-input">
        <button type="button" id="search-btn" class="btn btn-outline-light">Search</button>
      </div>

      <div id="search-status" class="d-none">Status messages can go here</div>
    </div>
    <!--/Search input/-->

    <!-- Spinner thingy -->
    <div class="d-flex justify-content-center mb-2">
      <div id="spinner" class="spinner-border d-none"></div>
    </div>
    <!--/Spinner thingy/-->

    <!-- Main table -->
    <table class="table price-table table-striped table-hover mb-0 table-responsive d-none" id="search-results">
      <thead>
      <tr>
        <th class="text-nowrap pr-2" title="Account name of the user">Account</th>
        <th class="text-nowrap w-100" title="Character name of the user">Has character</th>
        <th class="text-nowrap pr-2" title="First found in what league">In league</th>
        <th class="text-nowrap pr-2" title="Time first seen">First found</th>
        <th class="text-nowrap" title="Time last seen">Last seen</th>
      </tr>
      </thead>

      <tbody class="custom-text-gray-lo"></tbody>

    </table>
    <!--/Main table/-->
  </div>
  <!--/Main card body/-->

  <div class="card-footer slim-card-edge"></div>
</div>
<?php
genBodyFooter();
include "assets/php/templates/footer.php";
?>
