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
</head>
<body>

<nav class="navbar navbar-expand-lg navbar-dark bg-dark">
  <div class="container-fluid">
    <a href="/" class="navbar-brand">Poe.Ovh</a>

    <button class="navbar-toggler" type="button" data-toggle="collapse" data-target="#navbarNavDropdown" aria-controls="navbarNavDropdown" aria-expanded="false" aria-label="Toggle navigation">
      <span class="navbar-toggler-icon"></span>
    </button>

    <div class="collapse navbar-collapse" id="navbarNavDropdown">
      <ul class="navbar-nav mr-auto">
        <li class="nav-item"><a class="nav-link" href="/">Front</a></li>
        <li class="nav-item"><a class="nav-link" href="prices">Prices</a></li>
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
              <p class="card-text">Main interface for the price API. Items are listed in decreasing order from most expensive to least expensive</p>
              <h5 class="card-title">Fields</h5>
              <div class="custom-table">
                <table class="table table-hover">
                  <thead>
                    <tr>
                      <th>Param</th>
                      <th>Required</th>
                      <th>Example</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr>
                      <td>league</td>
                      <td>Yes</td>
                      <td>Hardcore Bestiary</td>
                    </tr>
                    <tr>
                      <td>category</td>
                      <td>Yes</td>
                      <td>Armour</td>
                    </tr>
                    <tr>
                      <td>sub</td>
                      <td>No</td>
                      <td>Boots</td>
                    </tr>
                    <tr>
                      <td>from</td>
                      <td>No</td>
                      <td>5</td>
                    </tr>
                    <tr>
                      <td>to</td>
                      <td>No</td>
                      <td>64</td>
                    </tr>
                  </tbody>
                </table>
              </div>

              <h5 class="card-title">Examples</h5>
              <a class="btn btn-dark" href="http://api.poe.ovh/get?league=bestiary&category=armour&sub=boots&from=5&to=20">Example 1</a>
              <a class="btn btn-dark" href="http://api.poe.ovh/get?league=bestiary&category=armour&sub=boots&from=5">Example 2</a>
              <a class="btn btn-dark" href="http://api.poe.ovh/get?league=bestiary&category=armour&sub=boots&to=20">Example 3</a>
              <a class="btn btn-dark" href="http://api.poe.ovh/get?league=bestiary&category=armour&sub=boots">Example 3</a>
              <a class="btn btn-dark" href="http://api.poe.ovh/get?league=bestiary&category=armour">Example 4</a>
              <a class="btn btn-dark" href="http://api.poe.ovh/get?league=bestiary&category=all">Example 5</a>
            </div>
          </div>
        </div>
      </div>

      <div class="row nested-row">
        <div class="col-lg">
          <div class="card custom-card">
            <div class="card-header">
              <h2>poe.ovh/icons</h2>
            </div>
            <div class="card-body">
              <h5 class="card-title">Description</h5>
              <p class="card-text">Main interface for the icon API</p>
              <h5 class="card-title">Fields</h5>
              <div class="custom-table">
                <table class="table table-hover">
                  <thead>
                    <tr>
                      <th>Param</th>
                      <th>Required</th>
                      <th>Example</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr>
                      <td>index</td>
                      <td>No</td>
                      <td>512,8,78</td>
                    </tr>
                  </tbody>
                </table>
              </div>
              <h5 class="card-title">Examples</h5>
              <a class="btn btn-dark" href="http://api.poe.ovh/icons?index=451,4,5,6,1,88,0">Example 1</a>
              <a class="btn btn-dark" href="http://api.poe.ovh/icons?index=55">Example 2</a>
              <a class="btn btn-dark" href="http://api.poe.ovh/icons">Example 3</a>
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
              <p class="card-text">Provides a list of catetgories currently in use</p>
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
              <p class="card-text">Provides a list of leagues</p>
              <h5 class="card-title">Examples</h5>
              <a class="btn btn-dark" href="http://api.poe.ovh/leagues">Example</a>
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
