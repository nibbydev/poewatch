<footer class='container-fluid d-flex flex-column justify-content-center align-items-center p-0'>
  <div>PoeWatch © <?php echo date('Y') ?></div>
  <div><a href='http://github.com/siegrest/poewatch' target='_blank'>Available on Github</a></div>
</footer>
<?php foreach($PAGE_DATA['jsIncludes'] as $include): ?>
<?php if (strpos($include, 'http://') !== false || strpos($include, 'https://') !== false): ?>
<script type="text/javascript" src="<?php echo $include ?>"></script>
<?php else: ?>
<script type="text/javascript" src="assets/js/<?php echo $include ?>"></script>
<?php endif ?>
<?php endforeach ?>
<?php foreach($PAGE_DATA['footerIncludes'] as $include): ?>
  <?php echo $include ?>
<?php endforeach ?>
</body>
</html>
