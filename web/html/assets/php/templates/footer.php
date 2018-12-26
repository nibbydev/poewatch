<footer class='container-fluid d-flex flex-column justify-content-center align-items-center p-0'>
  <div>PoeWatch © <?php echo date('Y') ?></div>
  <div><a href='http://github.com/siegrest/poewatch' target='_blank'>Available on Github</a></div>
</footer>
<?php foreach($PAGEDATA['jsIncludes'] as $js): ?>
<?php if (strpos($js, 'http://') !== false || strpos($js, 'https://') !== false): ?>
<script type="text/javascript" src="<?php echo $js ?>"></script>
<?php else: ?>
<script type="text/javascript" src="assets/js/<?php echo $js ?>"></script>
<?php endif ?>
<?php endforeach; ?>
</body>
</html>
