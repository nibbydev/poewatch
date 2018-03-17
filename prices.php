<!DOCTYPE html>
<html lang="en">
<head>
  <title>Poe.ovh</title>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <link rel="stylesheet" href="assets/css/bootstrap.min.css">
  <link rel="stylesheet" href="assets/css/main.css">
  <script src="assets/js/jquery.min.js"></script>
  <script src="assets/js/bootstrap.min.js"></script>
  <script src="assets/js/main.js"></script>
</head>
<body>

<nav class="navbar navbar-expand-md navbar-dark bg-dark">
  <div class="container-fluid">
    <a href="/" class="navbar-brand">Poe.Ovh</a>

    <button class="navbar-toggler" type="button" data-toggle="collapse" data-target="#navbarNavDropdown" aria-controls="navbarNavDropdown" aria-expanded="false" aria-label="Toggle navigation">
      <span class="navbar-toggler-icon"></span>
    </button>

    <div class="collapse navbar-collapse" id="navbarNavDropdown">
      <ul class="navbar-nav mr-auto">
        <li class="nav-item"><a class="nav-link" href="/">Front</a></li>
        <li class="nav-item active"><a class="nav-link" href="#">Prices</a></li>
        <li class="nav-item"><a class="nav-link" href="#">About</a></li>
      </ul>
      <div class="navbar-nav ml-auto">
        <a href="api"><span class="badge badge-secondary">API</span></a>
      </div>
    </div>
  </div>
</nav>
  
<div class="container-fluid">    
  <div class="row">
    <div class="col-md-3"> 
      <div class="list-group sidebar-left" id="sidebar-link-container">
        <?php include "assets/php/menu.php" ?>
      </div>
    </div>

    <div class="col-md-8 main-content"> 
      <div class="row nested-row">
        <div class="col-md-4"> 
          <h1>Accessories</h1>
        </div>
      </div>

      <div class="row nested-row">
        <div class="col-md-4"> 
          <h4>League</h4>
          <select class="form-control" id="search-league" onchange="makeRequest()">
            <?php include "assets/php/leagueSelector.php" ?>
          </select>
        </div>

        <div class="col-md-6">
          <h4>Sub-category</h4>
          <select class="form-control" id="search-sub" onchange="makeRequest()">
            <?php include "assets/php/categorySelector.php" ?>
          </select>
        </div>

        <div class="col-md-2">
          <h4>Links</h4>
          <select class="form-control">
            <option selected>(WIP)</option>
            <option value=0>None</option>
            <option value=5>5</option>
            <option value=6>6</option>
          </select>
        </div>
      </div>

      <div class="row nested-row">
        <div class="col-md">
          <h4>Search</h4>
          <input type="text" class="form-control" placeholder="Search (WIP)">
        </div>
      </div>

      <div class="row nested-row">
        <div class="col-md">
          <div class="card custom-card">
            <div class="card-body">
              <div class="custom-table">
                <table class="table table-hover table-sm" id="searchResults">
                  <thead>
                    <tr>
                      <th scope="col">Item</th>
                      <th scope="col">Mean</th>
                      <th scope="col">Median</th>
                      <th scope="col">Mode</th>
                      <th scope="col">Count</th>
                    </tr>
                  </thead>
                  <tbody></tbody>
                </table>
              </div>
            </div>
          </div>
        </div>
      </div>


      
    </div>
  </div>
</div>

<footer class="container-fluid bg-dark text-center">
  <p>Footer Text</p>
</footer>

</body>
</html>
