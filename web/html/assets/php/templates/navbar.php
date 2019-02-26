<?php $currentPage = explode('?', $_SERVER['REQUEST_URI'])[0] ?>
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
<?php foreach($PAGEDATA["navs"] as $element): ?>
        <li class='nav-item'>
          <a class='nav-link <?php if ($element['href'] === $currentPage) echo "active"?>' href='<?php echo $element["href"] ?>'><?php echo $element["name"] ?></a>
        </li>
<?php endforeach ?>
      </ul>
    </div>
  </div>
</nav>
