<?php
$title = basename($_SERVER["PHP_SELF"], ".php");
$index = ($title === "index" ? "active" : "");
$prices = ($title === "prices" ? "active" : "");
$about = ($title === "about" ? "active" : "");
$api = ($title === "api" ? "active" : "");

echo <<<"HEADER"
<nav class="navbar navbar-expand-lg navbar-dark bg-dark">
  <div class="container-fluid">
    <a href="/" class="navbar-brand">Poe.Ovh</a>

    <button class="navbar-toggler" type="button" data-toggle="collapse" data-target="#navbarNavDropdown" aria-controls="navbarNavDropdown" aria-expanded="false" aria-label="Toggle navigation">
      <span class="navbar-toggler-icon"></span>
    </button>

    <div class="collapse navbar-collapse" id="navbarNavDropdown">
      <ul class="navbar-nav mr-auto">
        <li class="nav-item"><a class="nav-link $index" href="/">Front</a></li>
        <li class="nav-item"><a class="nav-link $prices" href="prices">Prices</a></li>
        <li class="nav-item"><a class="nav-link $api" href="api">API</a></li>
        <li class="nav-item"><a class="nav-link $about" href="about">About</a></li>
      </ul>
    </div>
  </div>
</nav>
HEADER;
?>