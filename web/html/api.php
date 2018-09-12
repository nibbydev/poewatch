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
                  <h2><a href='https://api.poe.watch/id' target='_blank'>id</a></h2>
                </div>
                <div class="card-body">
                  <!-- Description -->
                  <h5 class="card-title">Description</h5>
                  <p class="card-text">Latest change ID from the top of the river and the time it was fetched.</p>
                  <!--/Description/-->
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
                  <h2><a href='https://api.poe.watch/leagues' target='_blank'>leagues</a></h2>
                </div>
                <div class="card-body">
                  <!-- Description -->
                  <h5 class="card-title">Description</h5>
                  <p class="card-text">List of current leagues. Entries are sorted such that event leagues appear first, followed by the challenge leagues and then the permanent leagues. SSF entries are omitted.</p>
                  <!--/Description/-->
                </div>
                <div class="card-footer slim-card-edge"></div>
              </div>
            </div>
          </div>
          <!--/API: leagueList/-->
          <!-- API: categories -->
          <div class="row mb-4">
            <div class="col-lg">
              <div class="card custom-card">
                <div class="card-header">
                  <h2><a href='https://api.poe.watch/categories' target='_blank'>categories</a></h2>
                </div>
                <div class="card-body">
                  <!-- Description -->
                  <h5 class="card-title">Description</h5>
                  <p class="card-text">List of parent and child catetgories currently in use.</p>
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
                  <h2><a href='https://api.poe.watch/itemdata' target='_blank'>itemdata</a></h2>
                </div>
                <div class="card-body">
                  <!-- Description -->
                  <h5 class="card-title">Description</h5>
                  <p class="card-text">All items found in the stash API and their defining properties.</p>
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
                  <h2><a href='https://api.poe.watch/get' target='_blank'>get</a></h2>
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
                          <td>Full name of a league</td>
                        </tr>
                        <tr>
                          <td>category</td>
                          <td><span class='badge badge-success'>✓</span></td>
                          <td>Parent cateogry (see category API)</td>
                        </tr>
                      </tbody>
                    </table>
                  </div>
                  <!--/Request fields/-->
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
                          <td>Unique ID for items</td>
                        </tr>
                        <tr>
                          <td>name</td>
                          <td><span class='badge badge-success'>✓</span></td>
                          <td>Name of item</td>
                        </tr>
                        <tr>
                          <td>type</td>
                          <td><span class='badge badge-danger'>✕</span></td>
                          <td>Typeline of the item, if present</td>
                        </tr>
                        <tr>
                          <td>frame</td>
                          <td><span class='badge badge-success'>✓</span></td>
                          <td>Frametype of item</td>
                        </tr>
                        <tr>
                          <td>category</td>
                          <td><span class='badge badge-danger'>✕</span></td>
                          <td>Child category of item</td>
                        </tr>
                        <tr>
                          <td>mean</td>
                          <td><span class='badge badge-success'>✓</span></td>
                          <td>Price of item in chaos calculated as mean</td>
                        </tr>
                        <tr>
                          <td>exalted</td>
                          <td><span class='badge badge-success'>✓</span></td>
                          <td>Mean price of item in exalted</td>
                        </tr>
                        <tr>
                          <td>quantity</td>
                          <td><span class='badge badge-success'>✓</span></td>
                          <td>Avg amount of items listed per 24h</td>
                        </tr>
                        <tr>
                          <td>icon</td>
                          <td><span class='badge badge-success'>✓</span></td>
                          <td>Item's icon</td>
                        </tr>
                        <tr>
                          <td>spark</td>
                          <td><span class='badge badge-success'>✓</span></td>
                          <td>Price changes as percentages for the past week</td>
                        </tr>
                        <tr>
                          <td>chane</td>
                          <td><span class='badge badge-success'>✓</span></td>
                          <td>Total price change as percentage since one week</td>
                        </tr>
                        <tr>
                          <td>var</td>
                          <td><span class='badge badge-danger'>✕</span></td>
                          <td>Variant of the item</td>
                        </tr>
                        <tr>
                          <td>links</td>
                          <td><span class='badge badge-danger'>✕</span></td>
                          <td>Largest link group of the item</td>
                        </tr>
                        <tr>
                          <td>tier</td>
                          <td><span class='badge badge-danger'>✕</span></td>
                          <td>Map tier</td>
                        </tr>
                        <tr>
                          <td>lvl</td>
                          <td><span class='badge badge-danger'>✕</span></td>
                          <td>Gem level</td>
                        </tr>
                        <tr>
                          <td>quality</td>
                          <td><span class='badge badge-danger'>✕</span></td>
                          <td>Gem quality</td>
                        </tr>
                        <tr>
                          <td>corrupted</td>
                          <td><span class='badge badge-danger'>✕</span></td>
                          <td>Gem corrupted state</td>
                        </tr>
                        <tr>
                          <td>ilvl</td>
                          <td><span class='badge badge-danger'>✕</span></td>
                          <td>Item level of bases</td>
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
