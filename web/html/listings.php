<?php
require_once "assets/php/pageData.php";
require_once "../details/pdo.php";
require_once "assets/php/templates/body.php";
require_once "assets/php/func/leagues.php";

$PAGE_DATA["title"] = "Listings - PoeWatch";
$PAGE_DATA["description"] = "Find items sold by users";
$PAGE_DATA["pageHeader"] = "Item listings";
$PAGE_DATA["jsIncludes"][] = "main.js";

// Page-local data
$PAGE_DATA["page"]["leagues"] = getLeagues($pdo);

include "assets/php/templates/header.php";
include "assets/php/templates/navbar.php";
include "assets/php/templates/priceNav.php";
genBodyHeader();
?>
<div class="card custom-card w-100">
  <div class="card-header">
    Find all items on sale from a user. Type in a <span class='custom-text-green'>case sensitive</span> account name
    and click search. Note that only items tracked by the site are returned. This means most rare/magic/normal items
    are excluded. This is also available as an API.
  </div>


  <div class="card-body">

    <div class="d-flex align-items-center mb-3">
      <div class="btn-group mr-3">
        <select class="form-control custom-select mr-2" id="search-league">
          <?php foreach ($PAGE_DATA["page"]["leagues"] as $league) {
            if ($league["active"]) echo "<option value='", $league["name"], "'>", $league["display"], "</option>";
          } ?>
        </select>

        <input type="text" class="form-control seamless-input" name="search" placeholder="Account name"
               value="<?php if (isset($_GET["account"])) echo htmlentities($_GET["account"]) ?>"
               id="search-input">
        <button type="button" id="search-btn" class="btn btn-outline-dark">Search</button>
      </div>

      <div id="search-status" class="d-none">Status messages can go here</div>
    </div>

    <div class="d-flex justify-content-center mb-2">
      <div id="spinner" class="spinner-border d-none"></div>
    </div>

    <table class="table price-table table-striped table-hover mb-0 table-responsive d-none" id="search-results">
      <thead>
      <tr>
        <th title="Item title" class="w-100">
          <span class='sort-column'>Item</span>
        </th>
        <th title="Nr of items for sale">
          <span class='sort-column'>Count</span>
        </th>
        <th title="First buyout price of the item">
          <span class='sort-column'>Price</span>
        </th>
        <th title="When the item was first found">
          <span class='sort-column'>Found</span>
        </th>
        <th title="When the item was last changed">
          <span class='sort-column'>Updated</span>
        </th>
      </tr>
      </thead>

      <tbody></tbody>

    </table>
  </div>


  <div class="card-footer slim-card-edge"></div>
</div>
<?php
genBodyFooter();
include "assets/php/templates/footer.php";
?>
