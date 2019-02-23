<?php 
  require_once "assets/php/pageData.php";
  require_once "assets/php/templates/body.php"; 
  require_once "../details/pdo.php";

  $PAGEDATA["title"] = "Stats - PoeWatch";
  $PAGEDATA["description"] = "Statistics about the site";
  $PAGEDATA["pageHeader"] = "Statistics (WIP)";

  $PAGEDATA["cssIncludes"][] = "https://cdn.jsdelivr.net/chartist.js/latest/chartist.min.css";
  $PAGEDATA["jsIncludes"][] = "https://cdn.jsdelivr.net/chartist.js/latest/chartist.min.js";
  $PAGEDATA["jsIncludes"][] = "chartist-plugin-tooltip2.js";
  $PAGEDATA["jsIncludes"][] = "stats.js";

  include "assets/php/templates/header.php";
  include "assets/php/templates/navbar.php";
  include "assets/php/templates/priceNav.php";
?>

<style>
/* graph grid */
.ct-grids line {
  stroke: rgba(255,255,255,.2);
}

/* graph labels*/
.ct-labels span {
  color: rgba(255,255,255,.5);
}

/* graph line */ 
.ct-series.ct-series .ct-line {
  stroke: rgb(112, 202, 90);
  stroke-width: .15em;
}

.ct-series.ct-series .ct-bar {
  stroke: rgb(112, 202, 90) !important;
  stroke-width: .3em !important;
}

/* hide points by default */
.ct-series.ct-series .ct-point {
  stroke-width: 0;
  stroke: rgb(112, 202, 90);
}

.chartist-tooltip {
  position: absolute;
  left: 0;
  top: 0;
  z-index: 1100;
  display: inline-block;
  padding: .2em .8em;
  visibility: hidden;
  transform: translateY(3em);
  opacity: 0;
  border-radius: 0.25em;
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 0 0.5em rgba(0, 0, 0, 0.2);
  transition: transform 0.2s ease-in-out;
  color: rgba(0, 0, 0, 0.9);
}

.chartist-tooltip:not([hidden]) {
  margin: 0;
  visibility: visible;;
  transform: none;
  opacity: 1;
}

/* Tooltip arrow */
.chartist-tooltip::before {
  content: '\25BC';
  position: absolute;
  left: calc(50% - .5em);
  top: 100%;
  z-index: -1;
  font-size: 1.3em;
  line-height: .5em;
  font-family: Arial, sans-serif;
  color: rgba(255, 255, 255, 0.9);
  transform: scaleY(0.7);
  text-shadow: 0 0.25em 0.35em rgba(0, 0, 0, 0.1);
}

.chartist-tooltip--left::before {
  left: 0.75em;
}

.chartist-tooltip--right::before {
  left: auto;
  right: 0.75em;
}

/* Adds a small point transition (line charts) when the point is active */
.ct-point {
  transition: all 0.2s ease-in-out;
}

/* Increased specificity intended to overwrite the default chartist style */
.ct-chart-line.ct-chart-line .ct-point--hover {
  stroke-width: .5em;
}
</style>

<?php genBodyHeader() ?>
<div class="col-12 p-0" id="main">

</div>
<?php genBodyFooter() ?>

<?php include "assets/php/templates/footer.php" ?>
