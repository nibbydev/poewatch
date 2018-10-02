<?php
function GenFooter() {
  echo "<footer class='container-fluid d-flex flex-column justify-content-center align-items-center p-0'>
    <div>PoeWatch © " . date('Y') . "</div>
  </footer>";
}

function GenNavbar($pdo) {
  $stmt = $pdo->query("SELECT * FROM web_navbar_items WHERE enabled = 1");

  echo "
  <nav class='navbar navbar-expand-md navbar-dark'>
    <div class='container-fluid'>
      <a href='/' class='navbar-brand'>
        <img src='assets/img/favico.png' class='d-inline-block align-top mr-2'>
        PoeWatch
      </a>
      <button class='navbar-toggler' type='button' data-toggle='collapse' data-target='#navbarNavDropdown' aria-controls='navbarNavDropdown' aria-expanded='false' aria-label='Toggle navigation'>
        <span class='navbar-toggler-icon'></span>
      </button>
      <div class='collapse navbar-collapse' id='navbarNavDropdown'>
        <ul class='navbar-nav mr-auto'>";

  while ($row = $stmt->fetch()) {
    $active = explode('?', $_SERVER['REQUEST_URI'])[0] === $row['href'] ? 'active' : '';
    echo "<li class='nav-item'><a class='nav-link $active' href='{$row['href']}'>{$row['display']}</a></li>";
  }

  echo  "
        </ul>
      </div>
    </div>
  </nav>";

}

function GenCatMenuHTML($pdo) {
  $stmt = $pdo->query("SELECT * FROM web_menu_items WHERE enabled = 1");

  echo "<div class='custom-sidebar-column mr-3'>
          <div class='card custom-card'>
            <div class='card-header slim-card-edge'></div>
              <div class='card-body p-0'>";

  while ($row = $stmt->fetch()) {
    // If category param matches current category, mark it as active
    $active = "";
    if (isset($_GET["category"])) {
      if ($_GET["category"] === explode('=', $row["href"])[1]) {
        $active = "active";
      }
    }

    echo "
    <a class='custom-menu-item d-flex p-2 $active' href='{$row['href']}'>
      <div class='img-container img-container-sm'>
        <img src='{$row['icon']}'>
      </div>
      <div class='custom-menu-name align-self-center mx-2'>
        <span>{$row['display']}</span>
      </div>
    </a>";
  }

  echo "  </div>
        <div class='card-footer slim-card-edge'>
      </div>
    </div>
  </div>";
}

function GenMotDBox() {
  echo "
  <div class='row d-block mb-3'>
    <div class='col'> 
      <div class='card custom-card'>
        <div class='card-header slim-card-edge'></div>
        <div class='card-body p-1'>
          <p class='mb-0 text-center subtext-1'>
            [ allan please add advertisement ]
          </p>
        </div>
        <div class='card-footer slim-card-edge'></div>
      </div>
    </div>
  </div>";
}

function GenHeaderMetaTags($title, $description) {
  echo "
  <title>$title</title>
  <meta charset='utf-8'>
  <meta property='og:site_name' content='Poe Watch'>
  <meta property='og:locale' content='en_US'>
  <meta property='og:title' content='$title'>
  <meta property='og:type' content='website'>
  <meta property='og:image' content='https://poe.watch/assets/img/ico/96.png'>
  <meta property='og:description' content='$description'>
  <link rel='icon' type='image/png' href='assets/img/ico/192.png' sizes='192x192'>
  <link rel='icon' type='image/png' href='assets/img/ico/96.png' sizes='96x96'>
  <link rel='icon' type='image/png' href='assets/img/ico/32.png' sizes='32x32'>
  <link rel='icon' type='image/png' href='assets/img/ico/16.png' sizes='16x16'>
  <meta name='viewport' content='width=device-width, initial-scale=1'>";
}
