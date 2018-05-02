<!DOCTYPE html>
<html lang="en">
<head>
  <title>Poe-Stats - Progress</title>
  <meta charset="utf-8">
  <link rel="icon" type="image/png" href="assets/img/favico.png">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/4.0.0/css/bootstrap.min.css">
  <link rel="stylesheet" href="assets/css/main.css">
</head>
<body>
<!-- Primary navbar -->
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
        <li class="nav-item"><a class="nav-link" href="api">API</a></li>
        <li class="nav-item"><a class="nav-link active" href="progress">Progress</a></li>
        <li class="nav-item"><a class="nav-link" href="about">About</a></li>

      </ul>
    </div>
  </div>
</nav>
<!--/Primary navbar/-->
<!-- Page body -->
<div class="container-fluid">    
  <div class="row">
    <!-- Menu -->
    <div class="col-xl-3"> 
      <div class="row mt-4 mb-xl-4 custom-sidebar-column">

          <?php include ( "assets/php/menu.php" ) ?>

      </div>
    </div>
    <!--/Menu/-->
    <!-- Main content -->
    <div class="col-xl-8 col-lg-10 offset-xl-0 offset-lg-1 offset-md-0 mt-4">
      <div class="row mb-3">
        <div class="col-lg">
          <div class="card custom-card">
            <div class="card-body">
              <h2 id="title" class="text-center">League progress</h2>
              <hr>
              <h4 id="counter" class="text-center mb-3"></h4>
              <div class="progress" style="height: 20px;">
                <div class="progress-bar bg-secondary" role="progressbar" id="leagueProgressBar"></div>
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
<script src="https://ajax.googleapis.com/ajax/libs/jquery/3.3.1/jquery.min.js"></script>
<script src="https://maxcdn.bootstrapcdn.com/bootstrap/4.0.0/js/bootstrap.min.js"></script>
<link rel="stylesheet" href="assets/css/responsive.css">

<script>
function CountDownTimer(st, nd, id, pb) {
  var start = new Date(st);
  var end = new Date(nd);

  var idE = $(id);
  var pbE = $(pb);

  var _second = 1000;
  var _minute = _second * 60;
  var _hour = _minute * 60;
  var _day = _hour * 24;
  var timer;

  function showRemaining() {
    var now = new Date();
    var distance = end - now;
    var percentage = Math.round((now - start) / (end - start) * 1000) / 10;

    if (distance < 0) {

      clearInterval(timer);
      idE.text("Expired");

      return;
    }
    var days = Math.floor(distance / _day);
    var hours = Math.floor((distance % _day) / _hour);
    var minutes = Math.floor((distance % _hour) / _minute);
    var seconds = Math.floor((distance % _minute) / _second);

    var tmp = "";
    if (days > 0) tmp += days + " days, ";
    if (hours > 0) tmp += hours + " hours, ";
    if (minutes > 0) tmp += minutes + " minutes, ";
    tmp += seconds + " seconds";

    idE.text(tmp);
    pbE.css("width", percentage+"%");
  }

  showRemaining();
  timer = setInterval(showRemaining, 1000);
}

$(document).ready(function() {
  var elements = JSON.parse( "<?php echo str_replace('"', "'", file_get_contents( dirname( getcwd(), 2) . "/data/length.json" ) ); ?>".replace(/'/g, '"') );
  $.each(elements, function(index, element) {
    if (element["id"].indexOf("SSF") !== -1 || element["id"] === "Standard" || element["id"].indexOf("Hardcore") !== -1) return;
    $("#title").text("League progress: " + element["id"]);
    CountDownTimer(element["start"], element["end"], "#counter", "#leagueProgressBar");
    return;
  });
})
</script>
</body>
</html>
