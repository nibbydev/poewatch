<!DOCTYPE html>
<html lang="en">
<head>
  <title>Poe.ovh - API</title>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <link rel="stylesheet" href="assets/css/bootstrap.min.css">
  <link rel="stylesheet" href="assets/css/main.css">
  <script src="assets/js/jquery.min.js"></script>
  <script src="assets/js/bootstrap.min.js"></script>
</head>
<body>

<?php
  include "assets/php/header.php";
?>
  
<div class="container-fluid">    
  <div class="row">
    <div class="col-lg-3"> 
      <div class="list-group sidebar-left" id="sidebar-link-container">
        <?php include "assets/php/menu.php" ?>
      </div>
    </div>

    <div class="col-lg-8 main-content"> 
      <div class="row nested-row">
        <div class="col-lg">
          <div class="card custom-card">
            <div class="card-header">
              <h2>poe.ovh/get</h2>
            </div>
            <div class="card-body">
              <h5 class="card-title">Description</h5>
              <p class="card-text">Main interface for the price API. Items are listed in decreasing order from most expensive to least expensive. Updated every minute.</p>
              <h5 class="card-title">Fields</h5>
              <div class="custom-table">
                <table class="table table-hover">
                  <thead>
                    <tr>
                      <th>Param</th>
                      <th>Required</th>
                      <th>Description</th>
                      <th>Example</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr>
                      <td>league</td>
                      <td>Yes</td>
                      <td>One of the acitve primary leagues</td>
                      <td>Hardcore Bestiary</td>
                    </tr>
                    <tr>
                      <td>parent</td>
                      <td>Yes</td>
                      <td>Parent cateogry</td>
                      <td>Armour</td>
                    </tr>
                    <tr>
                      <td>child</td>
                      <td>No</td>
                      <td>Child category</td>
                      <td>Boots</td>
                    </tr>
                    <tr>
                      <td>from</td>
                      <td>No</td>
                      <td>Limit entries</td>
                      <td>5</td>
                    </tr>
                    <tr>
                      <td>to</td>
                      <td>No</td>
                      <td>Limit entries</td>
                      <td>64</td>
                    </tr>
                  </tbody>
                </table>
              </div>

              <h5 class="card-title">Examples</h5>
              <a class="btn btn-dark" href="http://api.poe.ovh/get?league=bestiary&parent=armour&child=boots&from=5&to=20">Example 1</a>
              <a class="btn btn-dark" href="http://api.poe.ovh/get?league=bestiary&parent=armour&child=boots">Example 2</a>
              <a class="btn btn-dark" href="http://api.poe.ovh/get?league=bestiary&parent=armour">Example 3</a>
              <a class="btn btn-dark" href="http://api.poe.ovh/get?league=bestiary&parent=all">Example 4</a>
            </div>
          </div>
        </div>
      </div>

      <div class="row nested-row">
        <div class="col-lg">
          <div class="card custom-card">
            <div class="card-header">
              <h2>poe.ovh/itemdata</h2>
            </div>
            <div class="card-body">
              <h5 class="card-title">Description</h5>
              <p class="card-text">Allows retrieving item data based on indexes. Indexes are not, however, permanent and can change (but only on manual intervention). Updated dynamically.</p>
              <h5 class="card-title">Fields</h5>
              <div class="custom-table">
                <table class="table table-hover">
                  <thead>
                    <tr>
                      <th>Param</th>
                      <th>Required</th>
                      <th>Description</th>
                      <th>Example</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr>
                      <td>index</td>
                      <td>No</td>
                      <td>Get specific indexes</td>
                      <td>0000002,0000302,0002572</td>
                    </tr>
                  </tbody>
                </table>
              </div>
              <h5 class="card-title">Examples</h5>
              <a class="btn btn-dark" href="http://api.poe.ovh/itemdata?index=0002572,0000302,0000002">Example 1</a>
              <a class="btn btn-dark" href="http://api.poe.ovh/itemdata">Example 2</a>
            </div>
          </div>
        </div>
      </div>

      <div class="row nested-row">
        <div class="col-lg">
          <div class="card custom-card">
            <div class="card-header">
              <h2>poe.ovh/categories</h2>
            </div>
            <div class="card-body">
              <h5 class="card-title">Description</h5>
              <p class="card-text">Provides a list of catetgories currently in use. Updated dynamically.</p>
              <h5 class="card-title">Examples</h5>
              <a class="btn btn-dark" href="http://api.poe.ovh/categories">Example</a>
            </div>
          </div>
        </div>
      </div>

      <div class="row nested-row">
        <div class="col-lg">
          <div class="card custom-card">
            <div class="card-header">
              <h2>poe.ovh/leagues</h2>
            </div>
            <div class="card-body">
              <h5 class="card-title">Description</h5>
              <p class="card-text">Provides a list of current active leagues. Will be sorted so that challenge league is first, followed by the hardcore version of the challenge league. SSF leagues are omitted. Updated every 30 minutes.</p>
              <h5 class="card-title">Examples</h5>
              <a class="btn btn-dark" href="http://api.poe.ovh/leagues">Example</a>
            </div>
          </div>
        </div>
      </div>

    </div>
  </div>
</div>

<?php
  include "assets/php/footer.php";
?>

</body>
</html>
