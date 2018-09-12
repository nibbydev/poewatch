<?php 
  include_once ( "assets/php/functions.php" );
?>
<!DOCTYPE html>
<html lang="en">
<head>
  <?php GenHeaderMetaTags("API - PoeWatch", "Resources for developers") ?>
  <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/4.0.0/css/bootstrap.min.css">
  <link rel="stylesheet" href="assets/css/main.css">
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
          <!-- API: id -->
          <div class="row mb-4">
            <div class="col-lg">
              <div class="card custom-card">
                <div class="card-header">
                  <h2>api.poe.watch/id</h2>
                </div>
                <div class="card-body">
                  <!-- Description -->
                  <h5 class="card-title">Description</h5>
                  <p class="card-text">Provides a lightweight API for the latest change ID from the top of the river and the time in MS the change ID was fetched. Updated realtime.</p>
                  <!--/Description/-->
                  <hr>
                  <!-- Response fields -->
                  <h5 class="card-title">Response fields</h5>
                  <div class="card api-data-table px-2 pt-1 pb-1">
                    <table class="table table-sm">
                      <thead>
                        <tr>
                          <th>Param</th>
                          <th>Persistent</th>
                          <th class="w-100">Description</th>
                        </tr>
                      </thead>
                      <tbody>
                        <tr>
                          <td>id</td>
                          <td><span class='badge badge-success'>✓</span></td>
                          <td>The change ID</td>
                        </tr>
                        <tr>
                          <td>lastUpdate</td>
                          <td><span class='badge badge-success'>✓</span></td>
                          <td>Milliseconds since changeID was updated</td>
                        </tr>
                      </tbody>
                    </table>
                  </div>
                  <!--/Response fields/-->
                </div>
                <div class="card-footer slim-card-edge"></div>
              </div>
            </div>
          </div>
          <!--/API: id/-->
          <!-- API: leagueList -->
          <div class="row mb-4">
            <div class="col-lg">
              <div class="card custom-card">
                <div class="card-header">
                  <h2>api.poe.watch/leagueList</h2>
                </div>
                <div class="card-body">
                  <!-- Description -->
                  <h5 class="card-title">Description</h5>
                  <p class="card-text">Provides a list of current active leagues. Entries are sorted such that event leagues appear first, followed by the challenge leagues and then the permanent leagues. SSF entries are omitted. Updated every 30 minutes.</p>
                  <!--/Description/-->
                </div>
                <div class="card-footer slim-card-edge"></div>
              </div>
            </div>
          </div>
          <!--/API: leagueList/-->
          <!-- API: leagueData -->
          <div class="row mb-4">
            <div class="col-lg">
              <div class="card custom-card">
                <div class="card-header">
                  <h2>api.poe.watch/leagueData</h2>
                </div>
                <div class="card-body">
                  <!-- Description -->
                  <h5 class="card-title">Description</h5>
                  <p class="card-text">Provides a list of current active leagues as well as their durations. Not sorted. Pretty much the same as the official API but not as slow.</p>
                  <!--/Description/-->
                  <hr>
                  <!-- Response fields -->
                  <h5 class="card-title">Response fields</h5>
                  <div class="card api-data-table px-2 pt-1 pb-1">
                    <table class="table table-sm">
                      <thead>
                        <tr>
                          <th>Param</th>
                          <th>Persistent</th>
                          <th class="w-100">Description</th>
                        </tr>
                      </thead>
                      <tbody>
                        <tr>
                          <td>id</td>
                          <td><span class='badge badge-success'>✓</span></td>
                          <td>Name of the league</td>
                        </tr>
                        <tr>
                          <td>startAt</td>
                          <td><span class='badge badge-danger'>✕</span></td>
                          <td>If present, indicates when the league started in ISO 8601 yyyy-MM-dd'T'HH:mm:ss'Z' standard</td>
                        </tr>
                        <tr>
                          <td>endAt</td>
                          <td><span class='badge badge-danger'>✕</span></td>
                          <td>If present, indicates when the league will end in ISO 8601 yyyy-MM-dd'T'HH:mm:ss'Z' standard</td>
                        </tr>
                      </tbody>
                    </table>
                  </div>
                  <!--/Response fields/-->
                </div>
                <div class="card-footer slim-card-edge"></div>
              </div>
            </div>
          </div>
          <!--/API: leagueData/-->
          <!-- API: categories -->
          <div class="row mb-4">
            <div class="col-lg">
              <div class="card custom-card">
                <div class="card-header">
                  <h2>api.poe.watch/categories</h2>
                </div>
                <div class="card-body">
                  <!-- Description -->
                  <h5 class="card-title">Description</h5>
                  <p class="card-text">Provides a list of catetgories currently in use. Updated dynamically.</p>
                  <!--/Description/-->
                </div>
                <div class="card-footer slim-card-edge"></div>
              </div>
            </div>
          </div>
          <!--/API: categories/-->
          <!-- API: itemdata -->
          <div class="row mb-4">
            <div class="col-lg">
              <div class="card custom-card">
                <div class="card-header">
                  <h2>api.poe.watch/itemdata</h2>
                </div>
                <div class="card-body">
                  <!-- Description -->
                  <h5 class="card-title">Description</h5>
                  <p class="card-text">For all your poewatch-index-to-item-data-mappings needs. Indexes are not, however, permanent and can change (but only on manual intervention). Updated dynamically.</p>
                  <!--/Description/-->
                </div>
                <div class="card-footer slim-card-edge"></div>
              </div>
            </div>
          </div>
          <!--/API: itemdata/-->
          <!-- API: get -->
          <div class="row">
            <div class="col-lg">
              <div class="card custom-card">
                <div class="card-header">
                  <h2>api.poe.watch/get</h2>
                </div>
                <div class="card-body">
                  <!-- Description -->
                  <h5 class="card-title">Description</h5>
                  <p class="card-text">Main interface for the price API. Items are listed in decreasing order from most expensive to least expensive. Updated every minute. Capitalization does not matter for request fields.</p>
                  <!--/Description/-->
                  <hr>
                  <!-- Request fields -->
                  <h5 class="card-title">Request fields</h5>
                  <div class="card api-data-table px-2 pt-1 pb-1">
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
                          <td><span class='badge badge-success'>✓</span></td>
                          <td>One of the active primary leagues</td>
                        </tr>
                        <tr>
                          <td>category</td>
                          <td><span class='badge badge-success'>✓</span></td>
                          <td>Primary cateogry (see category API)</td>
                        </tr>
                      </tbody>
                    </table>
                  </div>
                  <!--/Request fields/-->
                  <hr>
                  <!-- Response fields -->
                  <h5 class="card-title">Response fields - generic</h5>
                  <div class="card api-data-table px-2 pt-1 pb-1">
                    <table class="table table-sm">
                      <thead>
                        <tr>
                          <th>Param</th>
                          <th>Persistent</th>
                          <th class="w-100">Description</th>
                        </tr>
                      </thead>
                      <tbody>
                        <tr>
                          <td>mean</td>
                          <td><span class='badge badge-success'>✓</span></td>
                          <td>Price of item in chaos calculated as mean</td>
                        </tr>
                        <tr>
                          <td>median</td>
                          <td><span class='badge badge-success'>✓</span></td>
                          <td>Price of item in chaos calculated as median</td>
                        </tr>
                        <tr>
                          <td>mode</td>
                          <td><span class='badge badge-success'>✓</span></td>
                          <td>Price of item in chaos calculated as mode</td>
                        </tr>
                        <tr>
                          <td>exalted</td>
                          <td><span class='badge badge-success'>✓</span></td>
                          <td>Price of item in exalted calculated as mode</td>
                        </tr>
                        <tr>
                          <td>count</td>
                          <td><span class='badge badge-success'>✓</span></td>
                          <td>Total amount of items listed during league</td>
                        </tr>
                        <tr>
                          <td>quantity</td>
                          <td><span class='badge badge-success'>✓</span></td>
                          <td>Avg amount of items listed in 24h (past the last 7d, as mean)</td>
                        </tr>
                        <tr>
                          <td>frame</td>
                          <td><span class='badge badge-success'>✓</span></td>
                          <td>Frametype of item (-1 for enchantments)</td>
                        </tr>
                        <tr>
                          <td>index</td>
                          <td><span class='badge badge-success'>✓</span></td>
                          <td>Hexadecimal index in two parts used to keep track of item. First 4 digits specify the base item. Followed by a dash, two more digits mark item's variant.</td>
                        </tr>
                        <tr>
                          <td>specificKey</td>
                          <td><span class='badge badge-success'>✓</span></td>
                          <td>Unique item key that consists of all item's possible variables (e.g see gems)</td>
                        </tr>
                        <tr>
                          <td>genericKey</td>
                          <td><span class='badge badge-success'>✓</span></td>
                          <td>Non-unique item key that consists of some of item's variables (e.g see gems)</td>
                        </tr>
                        <tr>
                          <td>parent</td>
                          <td><span class='badge badge-success'>✓</span></td>
                          <td>Parent category of item</td>
                        </tr>
                        <tr>
                          <td>child</td>
                          <td><span class='badge badge-success'>✓</span></td>
                          <td>Child category of item</td>
                        </tr>
                        <tr>
                          <td>name</td>
                          <td><span class='badge badge-success'>✓</span></td>
                          <td>Name of item</td>
                        </tr>
                        <tr>
                          <td>icon</td>
                          <td><span class='badge badge-success'>✓</span></td>
                          <td>Item's icon</td>
                        </tr>
                        <tr>
                          <td>history</td>
                          <td><span class='badge badge-success'>✓</span></td>
                          <td>Data from past 7 days, excluding current day. `change` corresponds to `spark` and marks difference between past week.</td>
                        </tr>
                        <tr>
                          <td>type</td>
                          <td><span class='badge badge-danger'>✕</span></td>
                          <td>Typeline of the item, if present</td>
                        </tr>
                        <tr>
                          <td>var</td>
                          <td><span class='badge badge-danger'>✕</span></td>
                          <td>Variant of the item (e.g "spells"/"attacks" for Vessel of Vinktar), if present</td>
                        </tr>
                        <tr>
                          <td>links</td>
                          <td><span class='badge badge-danger'>✕</span></td>
                          <td>Largest link group of the item as string, if present</td>
                        </tr>
                      </tbody>
                    </table>
                  </div>
                  <h5 class="card-title mt-3">Response fields - gems <span class="subtext-1">(Fields unique to gem entries)</span></h5>
                  <div class="card api-data-table px-2 pt-1 pb-1">
                    <table class="table table-sm">
                      <thead>
                        <tr>
                          <th>Param</th>
                          <th>Persistent</th>
                          <th class="w-100">Description</th>
                        </tr>
                      </thead>
                      <tbody>
                        <tr>
                          <td>corrupted</td>
                          <td><span class='badge badge-success'>✓</span></td>
                          <td>Corruption flag ("1" for corrupted, "0" for uncorrupted)</td>
                        </tr>
                        <tr>
                          <td>lvl</td>
                          <td><span class='badge badge-success'>✓</span></td>
                          <td>Level of gem as string</td>
                        </tr>
                        <tr>
                          <td>quality</td>
                          <td><span class='badge badge-success'>✓</span></td>
                          <td>Quality of gem as string</td>
                        </tr>
                      </tbody>
                    </table>
                  </div>
                  <h5 class="card-title mt-3">Response fields - enchantments <span class="subtext-1">(Fields unique to enchantment entries)</span></h5>
                  <div class="card api-data-table px-2 pt-1 pb-1">
                    <table class="table table-sm">
                      <thead>
                        <tr>
                          <th>Param</th>
                          <th>Persistent</th>
                          <th class="w-100">Description</th>
                        </tr>
                      </thead>
                      <tbody>
                        <tr>
                          <td>frame</td>
                          <td><span class='badge badge-success'>✓</span></td>
                          <td>As enchantments don't explicitly have a frametype, this is set to -1 to be compatible with the rest of the API.</td>
                        </tr>
                        <tr>
                          <td>var</td>
                          <td><span class='badge badge-danger'>✕</span></td>
                          <td>The value of the enchantment. E.g "90" from "90% of Glacial Cascade Physical..." or "88-132" from "Adds 88 to 132 Chaos Damage if..."</td>
                        </tr>
                      </tbody>
                    </table>
                  </div>
                  <!--/Response fields/-->
                  <hr>
                  <!-- Error fields -->
                  <h5 class="card-title">Response fields - errors <span class="subtext-1">(Responses upon entering invalid paramters)</span></h5>
                  <div class="card api-data-table px-2 pt-1 pb-1">
                    <table class="table table-sm">
                      <thead>
                        <tr>
                          <th>Param</th>
                          <th>Persistent</th>
                          <th class="w-100">Description</th>
                        </tr>
                      </thead>
                      <tbody>
                        <tr>
                          <td>error</td>
                          <td><span class='badge badge-success'>✓</span></td>
                          <td>Contains generic error message</td>
                        </tr>
                        <tr>
                          <td>field</td>
                          <td><span class='badge badge-danger'>✕</span></td>
                          <td>Contains name of invalid field</td>
                        </tr>
                      </tbody>
                    </table>
                  </div>
                  <!--/Error fields/-->
                </div>
                <div class="card-footer slim-card-edge"></div>
              </div>
            </div>
          </div>
          <!--/API: get/-->
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
