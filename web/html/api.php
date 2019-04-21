<?php 
  require_once "assets/php/pageData.php";
  require_once "assets/php/templates/body.php"; 

  $PAGEDATA["title"] = "API - PoeWatch";
  $PAGEDATA["pageHeader"] = "API Resources";
  $PAGEDATA["description"] = "Resources for developers";

  $APIcolumns = array(
    "left" => array(
      array(
        "href" => "https://api.poe.watch/id",
        "name" => "id",
        "desc" => "Latest change ID from the top of the river and the time it was fetched.",
        "params" => null
      ),
      array(
        "href" => "https://api.poe.watch/leagues",
        "name" => "leagues",
        "desc" => "List of current leagues. Entries are sorted such that event leagues appear first, followed by the challenge leagues and then the permanent leagues. SSF entries are omitted.",
        "params" => null
      ),
      array(
        "href" => "https://api.poe.watch/itemdata",
        "name" => "itemdata",
        "desc" => "All items found in the stash API and their defining properties. Category IDs can be found in category API.",
        "params" => array(
          array(
            "param" => "category",
            "required" => false,
            "desc" => "Category name"
          ),
        )
      ),
      array(
        "href" => "https://api.poe.watch/characters?account=novynn",
        "name" => "characters",
        "desc" => "Get player character names found through the stash API. If a player has listed an item in a public stash tab, that character name is recorded.",
        "params" => array(
          array(
            "param" => "account",
            "required" => true,
            "desc" => "Case insensitive account name"
          ),
        )
      ),
      array(
        "href" => "https://api.poe.watch/accounts?character=quillhitman",
        "name" => "accounts",
        "desc" => "Get player account names found through the stash API. If a player has listed an item in a public stash tab, that account name is recorded.",
        "params" => array(
          array(
            "param" => "character",
            "required" => true,
            "desc" => "Case insensitive character name"
          ),
        )
      )
    ), 
    "right" => array(
      array(
        "href" => "https://api.poe.watch/categories",
        "name" => "categories",
        "desc" => "List of categories and groups currently in use.",
        "params" => null
      ),
      array(
        "href" => "https://api.poe.watch/get?league=Standard&category=flask",
        "name" => "get",
        "desc" => "Returns price and item data for specified league and category. Items are listed in decreasing order from most expensive to least expensive. Updated every 10 minutes. Capitalization does not matter for request fields.",
        "params" => array(
          array(
            "param" => "league",
            "required" => true,
            "desc" => "Full name of a league"
          ),
          array(
            "param" => "category",
            "required" => true,
            "desc" => "Category (see category API)"
          ),
        )
      ),
      array(
        "href" => "https://api.poe.watch/item?id=259",
        "name" => "item",
        "desc" => "Retreive detailed information about an item, including its entire price history. Requires an ID, which can be found in itemdata API described above. Currently returns data from all past leagues.",
        "params" => array(
          array(
            "param" => "id",
            "required" => true,
            "desc" => "Numeric id of an item"
          ),
        )
      ),
      array(
        "href" => "https://api.poe.watch/compact?league=Standard",
        "name" => "compact",
        "desc" => "Return price data (id, mean, median, mode, min, max, total, daily, exalted) of all items of the provided league. Works only with active leagues. IDs can be found in itemdata API described above.",
        "params" => array(
          array(
            "param" => "league",
            "required" => true,
            "desc" => "Valid league name"
          ),
          array(
            "param" => "category",
            "required" => false,
            "desc" => "Category name"
          ),
        )
      )
    )
  );

  include "assets/php/templates/header.php";
  include "assets/php/templates/navbar.php";
  include "assets/php/templates/priceNav.php";
?>

<?php genBodyHeader() ?>

<?php foreach ($APIcolumns as $APIcolumnSide => $APIcolumn) { ?>
  <div class="col-6 p-0 <?php echo $APIcolumnSide === "left" ? "pr-2" : "pl-2" ?>">
<?php foreach ($APIcolumn as $API) { ?>

  <div class="card custom-card mb-3">
    <div class="card-header">
      <h4 class="card-title nowrap mb-0">
        <span class='custom-text-green'>
          <a href="<?php echo $API["href"] ?>" target="_blank"><?php echo $API["name"] ?></a>
        </span>
      </h4>
    </div>
    <div class="card-body px-3 py-2">
      <?php echo $API["desc"] ?>

      <?php if ($API["params"]) { ?>
      <div class="card api-data-table p-1 m-0 mt-3 overflow-hidden">
        <table class="table table-sm">
          <thead>
            <tr>
              <th>Param</th>
              <th>Required</th>
              <th class="w-100">Description</th>
            </tr>
          </thead>
          <tbody>
            <?php foreach ($API["params"] as $APIparam) { ?>
            <tr>
              <td><?php echo $APIparam["param"] ?></td>
              <td><?php echo $APIparam["required"] ? "<span class='badge custom-badge-green'>✓</span>" : "<span class='badge custom-badge-red'>✕</span>" ?></td>
              <td><?php echo $APIparam["desc"] ?></td>
            </tr>
            <?php } ?>
          </tbody>
        </table>
      </div>
      <?php } ?>

    </div>
    <div class="card-footer slim-card-edge"></div>
  </div>

<?php } ?>
  </div>
<?php } ?>

<?php genBodyFooter() ?>
<?php include "assets/php/templates/footer.php" ?>
