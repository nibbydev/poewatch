<?php 
  require_once "assets/php/pageData.php";
  require_once "assets/php/templates/body.php"; 

  $PAGEDATA["title"] = "API - PoeWatch";
  $PAGEDATA["description"] = "Resources for developers";

  include "assets/php/templates/header.php";
  include "assets/php/templates/navbar.php";
  include "assets/php/templates/priceNav.php";
?>

<?php genBodyHeader() ?>
  <div class="col-12 p-0">
    <div class="card-deck">
      <div class="card custom-card mb-3">
        <div class="card-header">
          <h2 class="m-0"><a href='https://api.poe.watch/leagues' target='_blank'>leagues</a></h2>
        </div>
        <div class="card-body">
          <p class="card-text">List of current leagues. Entries are sorted such that event leagues appear first, followed by the challenge leagues and then the permanent leagues. SSF entries are omitted.</p>
        </div>
        <div class="card-footer slim-card-edge"></div>
      </div>


      <div class="card custom-card mb-3">
        <div class="card-header">
          <h2 class="m-0"><a href='https://api.poe.watch/id' target='_blank'>id</a></h2>
        </div>
        <div class="card-body">
          <p class="card-text">Latest change ID from the top of the river and the time it was fetched.</p>
        </div>
        <div class="card-footer slim-card-edge"></div>
      </div>
    </div>
  </div>

  <div class="col-12 p-0">
    <div class="card-deck">
      <div class="card custom-card mb-3">
        <div class="card-header">
          <h2 class="m-0"><a href='https://api.poe.watch/categories' target='_blank'>categories</a></h2>
        </div>
        <div class="card-body">
          <p class="card-text">List of categories and groups currently in use.</p>
        </div>
        <div class="card-footer slim-card-edge"></div>
      </div>

    </div>
  </div>

  <div class="col-12 p-0">
    <div class="card custom-card mb-3">
      <div class="card-header">
        <h2 class="m-0"><a href='https://api.poe.watch/itemdata' target='_blank'>itemdata</a></h2>
      </div>
      <div class="card-body">
        <p class="card-text">All items found in the stash API and their defining properties. Category IDs can be found in category API.</p>

        <!-- Request fields -->
        <h5 class="card-title">Request fields</h5>
        <div class="card api-data-table p-1 m-0">
          <table class="table table-sm">
            <thead>
              <tr>
                <th>Param</th>
                <th>Required</th>
                <th class="w-100">Description</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td>category</td>
                <td><span class='badge custom-badge-red'>✕</span></td>
                <td>Category name</td>
              </tr>
            </tbody>
          </table>
        </div>
        <!--/Request fields/-->
      </div>
      <div class="card-footer slim-card-edge"></div>
    </div>
  </div>

  <div class="col-12 p-0">
    <div class="card custom-card mb-3">
      <div class="card-header">
        <h2 class="m-0"><a href='https://api.poe.watch/get?league=Standard&category=flask' target='_blank'>get</a></h2>
      </div>
      <div class="card-body">
        <p class="card-text">Returns price and item data for specified league and category. Items are listed in decreasing order from most expensive to least expensive. Updated every minute. Capitalization does not matter for request fields.</p>

        <!-- Request fields -->
        <h5 class="card-title">Request fields</h5>
        <div class="card api-data-table p-1 m-0">
          <table class="table table-sm">
            <thead>
              <tr>
                <th>Param</th>
                <th>Required</th>
                <th class="w-100">Description</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td>league</td>
                <td><span class='badge custom-badge-green'>✓</span></td>
                <td>Full name of a league</td>
              </tr>
              <tr>
                <td>category</td>
                <td><span class='badge custom-badge-green'>✓</span></td>
                <td>Category (see category API)</td>
              </tr>
            </tbody>
          </table>
        </div>
        <!--/Request fields/-->

      </div>
      <div class="card-footer slim-card-edge"></div>
    </div>
  </div>

  <div class="col-12 p-0">
    <div class="card custom-card mb-3">
      <div class="card-header">
        <h2 class="m-0"><a href='https://api.poe.watch/item?id=259' target='_blank'>item</a></h2>
      </div>
      <div class="card-body">
        <p class="card-text">Retreive detailed information about an item, including its entire price history. Requires an ID, which can be found in itemdata API described above. Currently returns data from all past leagues.</p>

        <!-- Request fields -->
        <h5 class="card-title">Request fields</h5>
        <div class="card api-data-table p-1 m-0">
          <table class="table table-sm">
            <thead>
              <tr>
                <th>Param</th>
                <th>Required</th>
                <th class="w-100">Description</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td>id</td>
                <td><span class='badge custom-badge-green'>✓</span></td>
                <td>Numeric id of an item</td>
              </tr>
            </tbody>
          </table>
        </div>
        <!--/Request fields/-->

      </div>
      <div class="card-footer slim-card-edge"></div>
    </div>
  </div>

  <div class="col-12 p-0">
    <div class="card custom-card mb-3">
      <div class="card-header">
        <h2 class="m-0"><a href='https://api.poe.watch/compact?league=Standard' target='_blank'>compact</a></h2>
      </div>
      <div class="card-body">
        <p class="card-text">Return price data (id, mean, median, mode, min, max, total, daily, exalted) of all items of the provided league. Works only with active leagues. IDs can be found in itemdata API described above.</p>

        <!-- Request fields -->
        <h5 class="card-title">Request fields</h5>
        <div class="card api-data-table p-1 m-0">
          <table class="table table-sm">
            <thead>
              <tr>
                <th>Param</th>
                <th>Required</th>
                <th class="w-100">Description</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td>league</td>
                <td><span class='badge custom-badge-green'>✓</span></td>
                <td>Valid league name</td>
              </tr>
              <tr>
                <td>category</td>
                <td><span class='badge custom-badge-red'>✕</span></td>
                <td>Category name</td>
              </tr>
            </tbody>
          </table>
        </div>
        <!--/Request fields/-->

      </div>
      <div class="card-footer slim-card-edge"></div>
    </div>
  </div>

  <div class="col-12 p-0">
    <div class="card-deck">
      <div class="card custom-card mb-3">
        <div class="card-header">
          <h2 class="m-0">
            <a href='https://api.poe.watch/characters?account=novynn' target='_blank'>characters</a>
          </h2>
        </div>
        <div class="card-body">
          <p class="card-text">Get player character names found through the stash API. If a player has listed an item in a public stash tab, that character name is recorded.</p>
        
          <!-- Request fields -->
          <h5 class="card-title">Request fields</h5>
          <div class="card api-data-table p-1 m-0">
            <table class="table table-sm">
              <thead>
                <tr>
                  <th>Param</th>
                  <th>Required</th>
                  <th class="w-100">Description</th>
                </tr>
              </thead>
              <tbody>
                <tr>
                  <td>account</td>
                  <td><span class='badge custom-badge-green'>✓</span></td>
                  <td>Case insensitive account name</td>
                </tr>
              </tbody>
            </table>
          </div>
          <!--/Request fields/-->
        </div>


        <div class="card-footer slim-card-edge"></div>
      </div>

      <div class="card custom-card mb-3">
        <div class="card-header">
          <h2 class="m-0">
            <a href='https://api.poe.watch/accounts?character=quillhitman' target='_blank'>accounts</a>
          </h2>
        </div>
        <div class="card-body">
          <p class="card-text">Get player account names found through the stash API. If a player has listed an item in a public stash tab, that account name is recorded.</p>
        
          <!-- Request fields -->
          <h5 class="card-title">Request fields</h5>
          <div class="card api-data-table p-1 m-0">
            <table class="table table-sm">
              <thead>
                <tr>
                  <th>Param</th>
                  <th>Required</th>
                  <th class="w-100">Description</th>
                </tr>
              </thead>
              <tbody>
                <tr>
                  <td>character</td>
                  <td><span class='badge custom-badge-green'>✓</span></td>
                  <td>Case insensitive character name</td>
                </tr>
              </tbody>
            </table>
          </div>
          <!--/Request fields/-->

        </div>
        <div class="card-footer slim-card-edge"></div>
      </div>
    </div>
  </div>
<?php genBodyFooter() ?>

<?php include "assets/php/templates/footer.php" ?>
