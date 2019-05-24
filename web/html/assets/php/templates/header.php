<!DOCTYPE html>
<html lang="en">
<head>
  <title><?php echo $PAGE_DATA["title"] ?></title>
  <meta charset='utf-8'>
  <meta property='og:site_name' content='PoeWatch'>
  <meta property='og:locale' content='en_US'>
  <meta property='og:title' content='<?php echo $PAGE_DATA["title"] ?>'>
  <meta property='og:type' content='website'>
  <meta property='og:image' content='https://poe.watch/assets/img/ico/96.png'>
  <meta property='og:description' content='<?php echo $PAGE_DATA["description"] ?>'>
  <link rel='icon' type='image/png' href='assets/img/ico/192.png' sizes='192x192'>
  <link rel='icon' type='image/png' href='assets/img/ico/96.png' sizes='96x96'>
  <link rel='icon' type='image/png' href='assets/img/ico/32.png' sizes='32x32'>
  <link rel='icon' type='image/png' href='assets/img/ico/16.png' sizes='16x16'>
  <meta name='viewport' content='width=device-width, initial-scale=1'>

<?php foreach($PAGE_DATA['cssIncludes'] as $include): ?>
<?php if (strpos($include, 'http://') !== false || strpos($include, 'https://') !== false): ?>
  <link rel="stylesheet" href="<?php echo $include ?>">
<?php else: ?>
  <link rel="stylesheet" href="assets/css/<?php echo $include ?>">
<?php endif ?>
<?php endforeach ?>

<?php foreach($PAGE_DATA['headerIncludes'] as $include): ?>
  <?php echo $include ?>
<?php endforeach ?>

</head>
