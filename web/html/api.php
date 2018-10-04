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
            <div class="col-lg">
              <div class="card custom-card">
                <div class="card-header">
                  <h2>API</h2>
                  <div>Use the data in your applications. Rate limiting is 5 requests per 1 second, 1 minute timeout.</div>
                </div>
                <div class="card-body">

                  <h3><a href='https://api.poe.watch/leagues' target='_blank'>leagues</a></h3>
                  <p class="card-text">List of current leagues. Entries are sorted such that event leagues appear first, followed by the challenge leagues and then the permanent leagues. SSF entries are omitted.</p>

                  <hr>
                                    
                  <h3><a href='https://api.poe.watch/id' target='_blank'>id</a></h3>
                  <p class="card-text">Latest change ID from the top of the river and the time it was fetched.</p>

                  <hr>

                  <h3><a href='https://api.poe.watch/categories' target='_blank'>categories</a></h3>
                  <p class="card-text">List of categories currently in use.</p>

                  <hr>

                  <h3><a href='https://api.poe.watch/itemdata' target='_blank'>itemdata</a></h3>
                  <p class="card-text">All items found in the stash API and their defining properties. Category IDs can be found in category API.</p>

                  <hr>

                  <h3><a href='https://api.poe.watch/get?league=Standard&category=flask' target='_blank'>get</a></h3>
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
                          <td><span class='badge badge-success'>✓</span></td>
                          <td>Full name of a league</td>
                        </tr>
                        <tr>
                          <td>category</td>
                          <td><span class='badge badge-success'>✓</span></td>
                          <td>Parent category (see category API)</td>
                        </tr>
                      </tbody>
                    </table>
                  </div>
                  <!--/Request fields/-->

                  <hr>

                  <h3><a href='https://api.poe.watch/item?id=259' target='_blank'>item</a></h3>
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
                          <td><span class='badge badge-success'>✓</span></td>
                          <td>Numeric id of an item</td>
                        </tr>
                      </tbody>
                    </table>
                  </div>
                  <!--/Request fields/-->

                  <hr>

                  <h3><a href='https://api.poe.watch/compact?league=Standard' target='_blank'>compact</a> <span class='badge custom-badge-green'>New</span></h3>
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
                          <td><span class='badge badge-success'>✓</span></td>
                          <td>Valid league name</td>
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
