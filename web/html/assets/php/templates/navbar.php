<?php
  $currentPage = explode('?', $_SERVER['REQUEST_URI'])[0];

  $navbarElements = array(
    array(
      'name' => 'Front',
      'href' => '/'
    ),
    array(
      'name' => 'Prices',
      'href' => '/prices'
    ),
    array(
      'name' => 'API',
      'href' => '/api'
    ),
    array(
      'name' => 'Leagues',
      'href' => '/leagues'
    ),
    array(
      'name' => 'Characters',
      'href' => '/characters'
    ),
    array(
      'name' => 'Feedback',
      'href' => '/feedback'
    ),
    array(
      'name' => 'About',
      'href' => '/about'
    )
  );
?>
<nav class='navbar navbar-expand-md navbar-dark'>
  <div class='container-fluid'>
    <a href='/' class='navbar-brand'>
      <img src='assets/img/favico.png' class='d-inline-block align-top mr-2'>
      <span>PoeWatch</span>
    </a>
    <button class='navbar-toggler' type='button' data-toggle='collapse' data-target='#navbarNavDropdown' aria-controls='navbarNavDropdown' aria-expanded='false' aria-label='Toggle navigation'>
      <span class='navbar-toggler-icon'></span>
    </button>
    <div class='collapse navbar-collapse' id='navbarNavDropdown'>
      <ul class='navbar-nav mr-auto'>
<?php foreach($navbarElements as $element): ?>
        <li class='nav-item'>
          <a class='nav-link <?php if ($currentPage === $element['href']) {?>active<?php }?>' href='<?php echo $element["href"] ?>'><?php echo $element["name"] ?></a>
        </li>
<?php endforeach ?>
      </ul>
    </div>
  </div>
</nav>
