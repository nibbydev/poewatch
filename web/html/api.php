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

          <div class="row mb-4">
            <div class="col-lg">
              <div class="card custom-card">
                <div class="card-header">
                  <h2>API</h2>
                  <div>Get data from this site to use in your applications</div>
                </div>
                <div class="card-body">

                  <h4><a href='https://api.poe.watch/leagues' target='_blank'>leagues</a></h4>
                  <p class="card-text">List of current leagues. Entries are sorted such that event leagues appear first, followed by the challenge leagues and then the permanent leagues. SSF entries are omitted.</p>

                  <hr>
                                    
                  <h4><a href='https://api.poe.watch/id' target='_blank'>id</a></h4>
                  <p class="card-text">Latest change ID from the top of the river and the time it was fetched.</p>
                  
                  <hr>

                  <h4><a href='https://api.poe.watch/leagues' target='_blank'>leagues</a></h4>
                  <p class="card-text">List of current leagues. Entries are sorted such that event leagues appear first, followed by the challenge leagues and then the permanent leagues. SSF entries are omitted.</p>

                  <hr>

                  <h4><a href='https://api.poe.watch/categories' target='_blank'>categories</a></h4>
                  <p class="card-text">List of parent and child catetgories currently in use.</p>

                  <hr>

                  <h4><a href='https://api.poe.watch/itemdata' target='_blank'>itemdata</a></h4>
                  <p class="card-text">All items found in the stash API and their defining properties.</p>


                  <hr>

                  <h4><a href='https://api.poe.watch/get' target='_blank'>get</a></h4>
                  <p class="card-text">Main interface for the price API. Items are listed in decreasing order from most expensive to least expensive. Updated every minute. Capitalization does not matter for request fields.</p>

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
