<!DOCTYPE html>
<html lang="en">
<head>
  <title>Poe-Stats - API</title>
  <meta charset="utf-8">
  <link rel="icon" type="image/png" href="assets/img/favico.png">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/4.0.0/css/bootstrap.min.css">
  <link rel="stylesheet" href="assets/css/main.css">
</head>
<body>
  <nav class="navbar navbar-expand-lg navbar-dark">
    <div class="container-fluid">
      <a href="/" class="navbar-brand">
        <img src="assets/img/favico.png" class="d-inline-block align-top mr-2" alt="">
        Poe-Stats
      </a>
      <button class="navbar-toggler" type="button" data-toggle="collapse" data-target="#navbarNavDropdown" aria-controls="navbarNavDropdown" aria-expanded="false" aria-label="Toggle navigation">
        <span class="navbar-toggler-icon"></span>
      </button>
      <div class="collapse navbar-collapse" id="navbarNavDropdown">
        <ul class="navbar-nav mr-auto">
          <li class="nav-item"><a class="nav-link" href="/">Front</a></li>
          <li class="nav-item"><a class="nav-link" href="prices">Prices</a></li>
          <li class="nav-item"><a class="nav-link active" href="api">API</a></li>
          <li class="nav-item"><a class="nav-link" href="about">About</a></li>
        </ul>
      </div>
    </div>
  </nav>
<div class="container-fluid">    
  <div class="row">
    <div class="col-lg-3"> 
      <div class="list-group sidebar-left" id="sidebar-link-container">
        
        <?php include ( "assets/php/menu.php" ) ?>

      </div>
    </div>
    <div class="col-lg-8 main-content"> 
      <!-- API: id -->
      <div class="row mb-3">
        <div class="col-lg">
          <div class="card custom-card">
            <div class="card-header">
              <h2>api.poe-stats.com/id</h2>
            </div>
            <div class="card-body">
              <h5 class="card-title">Description</h5>
              <p class="card-text">Provides some basic data about the serice, such as: the latest change ID from the top of the river, time in MS the change ID was fetched, current status of the service.</p>
              <h5 class="card-title">Examples</h5>
              <a class="btn btn-outline-dark btn-outline-dark-alt mt-1" href="http://api.poe-stats.com/id">Id</a>
            </div>
          </div>
        </div>
      </div>
      <!--/API: id/-->

      <!-- API: leagues -->
      <div class="row mb-3">
        <div class="col-lg">
          <div class="card custom-card">
            <div class="card-header">
              <h2>api.poe-stats.com/leagues</h2>
            </div>
            <div class="card-body">
              <h5 class="card-title">Description</h5>
              <p class="card-text">Provides a list of current active leagues. Will be sorted so that challenge league is first, followed by the hardcore version of the challenge league. SSF leagues are omitted. Updated dynamically and also every 30 minutes from the official api.</p>
              <h5 class="card-title">Examples</h5>
              <a class="btn btn-outline-dark btn-outline-dark-alt mt-1" href="http://api.poe-stats.com/leagues">Leagues</a>
            </div>
          </div>
        </div>
      </div>
      <!--/API: leagues/-->

      <!-- API: categories -->
      <div class="row mb-3">
        <div class="col-lg">
          <div class="card custom-card">
            <div class="card-header">
              <h2>api.poe-stats.com/categories</h2>
            </div>
            <div class="card-body">
              <h5 class="card-title">Description</h5>
              <p class="card-text">Provides a list of catetgories currently in use. Updated dynamically.</p>
              <h5 class="card-title">Categories</h5>
              <a class="btn btn-outline-dark btn-outline-dark-alt mt-1" href="http://api.poe-stats.com/categories">Example</a>
            </div>
          </div>
        </div>
      </div>
      <!--/API: categories/-->

      <!-- API: itemdata -->
      <div class="row mb-3">
        <div class="col-lg">
          <div class="card custom-card">
            <div class="card-header">
              <h2>api.poe-stats.com/itemdata</h2>
            </div>
            <div class="card-body">
              <h5 class="card-title">Description</h5>
              <p class="card-text">For all your poe-stats-index-to-item-data-mappings needs. Indexes are not, however, permanent and can change (but only on manual intervention). Updated dynamically.</p>
              <h5 class="card-title">Examples</h5>
              <a class="btn btn-outline-dark btn-outline-dark-alt mt-1" href="http://api.poe-stats.com/itemdata">Item data</a>
            </div>
          </div>
        </div>
      </div>
      <!--/API: itemdata/-->

      <!-- API: get -->
      <div class="row mb-3">
        <div class="col-lg">
          <div class="card custom-card">
            <div class="card-header">
              <h2>api.poe-stats.com/get</h2>
            </div>
            <div class="card-body">
              <h5 class="card-title">Description</h5>
              <p class="card-text">Main interface for the price API. Items are listed in decreasing order from most expensive to least expensive. Updated every minute.</p>
              <!-- Request fields -->
              <h5 class="card-title">Request fields</h5>
              <div class="card api-data-table px-2 pt-1 mb-2">
                <table class="table table-sm">
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
                      <td>One of the active primary leagues</td>
                      <td>hardcore bestiary</td>
                    </tr>
                    <tr>
                      <td>category</td>
                      <td>Yes</td>
                      <td>Parent cateogry (see category API)</td>
                      <td>armour</td>
                    </tr>
                  </tbody>
                </table>
              </div>
              <!--/Request fields/-->
              <!-- Response fields -->
              <h5 class="card-title">Response fields - generic</h5>
              <div class="card api-data-table px-2 pt-1 mb-2">
                <table class="table table-sm">
                  <thead>
                    <tr>
                      <th>Param</th>
                      <th>Persistent</th>
                      <th>Description</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr>
                      <td>mean</td>
                      <td>Yes</td>
                      <td>Price of item in chaos calculated as mean</td>
                    </tr>
                    <tr>
                      <td>median</td>
                      <td>Yes</td>
                      <td>Price of item in chaos calculated as median</td>
                    </tr>
                    <tr>
                      <td>mode</td>
                      <td>Yes</td>
                      <td>Price of item in chaos calculated as mode</td>
                    </tr>
                    <tr>
                      <td>exalted</td>
                      <td>Yes</td>
                      <td>Price of item in exalted calculated as mode</td>
                    </tr>
                    <tr>
                      <td>count</td>
                      <td>Yes</td>
                      <td>Total amount of items listed during league</td>
                    </tr>
                    <tr>
                      <td>quantity</td>
                      <td>Yes</td>
                      <td>Avg amount of items listed in 24h (past the last 7d, as mean)</td>
                    </tr>
                    <tr>
                      <td>frame</td>
                      <td>Yes</td>
                      <td>Frametype of item (-1 for enchantments)</td>
                    </tr>
                    <tr>
                      <td>index</td>
                      <td>Yes</td>
                      <td>Hexadecimal index in two parts used to keep track of item. First 4 digits specify the base item. Followed by a dash, two more digits mark item's variant.</td>
                    </tr>
                    <tr>
                      <td>specificKey</td>
                      <td>Yes</td>
                      <td>Unique item key that consists of all item's possible variables (e.g see gems)</td>
                    </tr>
                    <tr>
                      <td>genericKey</td>
                      <td>Yes</td>
                      <td>Non-unique item key that consists of some of item's variables (e.g see gems)</td>
                    </tr>
                    <tr>
                      <td>parent</td>
                      <td>Yes</td>
                      <td>Parent category of item</td>
                    </tr>
                    <tr>
                      <td>child</td>
                      <td>Yes</td>
                      <td>Child category of item</td>
                    </tr>
                    <tr>
                      <td>name</td>
                      <td>Yes</td>
                      <td>Name of item</td>
                    </tr>
                    <tr>
                      <td>icon</td>
                      <td>Yes</td>
                      <td>Item's icon</td>
                    </tr>
                    <tr>
                      <td>history</td>
                      <td>Yes</td>
                      <td>Data from past 7 days, excluding current day. `change` corresponds to `spark` and marks difference between past week.</td>
                    </tr>
                    <tr>
                      <td>type</td>
                      <td>No</td>
                      <td>If present, the item's typeline</td>
                    </tr>
                    <tr>
                      <td>var</td>
                      <td>No</td>
                      <td>If present, the item's variant (e.g "spells"/"attacks" for Vessel of Vinktar)</td>
                    </tr>
                    <tr>
                      <td>corrupted</td>
                      <td>No</td>
                      <td>Gems only. Corruption flag (i.e "1" for true, "0" for false)</td>
                    </tr>
                    <tr>
                      <td>lvl</td>
                      <td>No</td>
                      <td>Gems only. Gem's level</td>
                    </tr>
                    <tr>
                      <td>quality</td>
                      <td>No</td>
                      <td>Gems only. Gem's quality</td>
                    </tr>
                  </tbody>
                </table>
              </div>
              <h5 class="card-title">Response fields - enchantments</h5>
              <div class="card api-data-table px-2 pt-1 mb-2">
                <table class="table table-sm">
                  <thead>
                    <tr>
                      <th>Param</th>
                      <th>Persistent</th>
                      <th>Description</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr>
                      <td>frame</td>
                      <td>Yes</td>
                      <td>As enchantments don't explicitly have a frametype, this is set to -1 to be compatible with the rest of the API.</td>
                    </tr>
                    <tr>
                      <td>var</td>
                      <td>No</td>
                      <td>The value of the enchantment. E.g "90" from "90% of Glacial Cascade Physical..." or "88-132" from "Adds 88 to 132 Chaos Damage if..."</td>
                    </tr>
                  </tbody>
                </table>
              </div>
              <!--/Response fields/-->
              <h5 class="card-title">Examples</h5>
              <a class="btn btn-outline-dark btn-outline-dark-alt mt-1" href="http://api.poe-stats.com/get?league=standard&category=armour">Armour</a>
            </div>
          </div>
        </div>
      </div>
      <!--/API: get/-->
    </div>
  </div>
</div>
<footer class="container-fluid text-center">
  <p>Poe-Stats © 2018</p>
</footer>
<script src="https://ajax.googleapis.com/ajax/libs/jquery/3.3.1/jquery.min.js"></script>
</body>
</html>
