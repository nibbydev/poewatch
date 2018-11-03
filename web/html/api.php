<?php 
  include_once ( "../details/pdo.php" );
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
<?php GenNavbar($pdo) ?>
<!--/Primary navbar/-->
<!-- Page body -->
<div class="container-fluid">
  <div class="row">
    <div class="col d-flex my-3">

      <!-- Menu -->
      <?php GenCatMenuHTML($pdo) ?>
      <!--/Menu/-->

      <!-- Main content -->
      <div class="d-flex w-100 justify-content-center"> 
        <div class='body-boundaries w-100'> 
          <div class="row mb-4">
            
            <div class="col-12">
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

            <div class="col-12">
              <div class="card-deck">
                <div class="card custom-card mb-3">
                  <div class="card-header">
                    <h2 class="m-0"><a href='https://api.poe.watch/categories' target='_blank'>categories</a></h2>
                  </div>
                  <div class="card-body">
                    <p class="card-text">List of categories currently in use.</p>
                  </div>
                  <div class="card-footer slim-card-edge"></div>
                </div>

                <div class="card custom-card mb-3">
                  <div class="card-header">
                    <h2 class="m-0"><a href='https://api.poe.watch/itemdata' target='_blank'>itemdata</a></h2>
                  </div>
                  <div class="card-body">
                    <p class="card-text">All items found in the stash API and their defining properties. Category IDs can be found in category API.</p>
                  </div>
                  <div class="card-footer slim-card-edge"></div>
                </div>
              </div>
            </div>

            <div class="col-12">
              <div class="card custom-card mb-3">
                <div class="card-header">
                  <h2 class="m-0"><a href='https://api.poe.watch/get?league=Standard&category=flask' target='_blank'>get</a></h2>
                </div>
                <div class="card-body">
                  <p class="card-text">Returns price and item data for specified league and category. Items are listed in decreasing order from most expensive to least expensive. Updated every minute. Capitalization does not matter for request fields.</p>

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

            <div class="col-12">
              <div class="card custom-card mb-3">
                <div class="card-header">
                  <h2 class="m-0"><a href='https://api.poe.watch/item?id=259' target='_blank'>item</a></h2>
                </div>
                <div class="card-body">
                  <p class="card-text">Retreive detailed information about an item, including its entire price history. Requires an ID, which can be found in itemdata API described above. Currently returns data from all past leagues.</p>

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

            <div class="col-12">
              <div class="card custom-card mb-3">
                <div class="card-header">
                  <h2 class="m-0"><a href='https://api.poe.watch/compact?league=Standard' target='_blank'>compact</a> <span class='badge custom-badge-green'>New</span></h2>
                </div>
                <div class="card-body">
                <p class="card-text">Return price data (id, mean, median, mode, min, max, count, quantity, exalted) of all items of the provided league. Works only with active leagues. IDs can be found in itemdata API described above.</p>

                  <!-- Request fields -->
                  <h5 class="card-title">Request fields</h5>
                  <div class="card api-data-table px-2 pt-1 pb-1 mb-3">
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

          </div>
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
