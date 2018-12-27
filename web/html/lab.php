<?php 
  require_once "assets/php/pageData.php";

  $PAGEDATA["title"] = "Lab - PoeWatch";
  $PAGEDATA["description"] = "Just the images from poelab.com";
  $PAGEDATA["headerIncludes"][] = "<meta name='referrer' content='no-referrer'/>";

  include "assets/php/templates/header.php";
  include "assets/php/templates/navbar.php";
  include "assets/php/templates/priceNav.php";
?>

<div class='container-fluid d-flex justify-content-center p-0'>
  <div class='row body-boundaries w-100 py-3'>
<?php foreach(array("uber", "merciless", "cruel", "normal") as $lab): ?>
      <div class="col-12 mb-3">
        <div class="card custom-card w-100">
          <div class="card-header">
            <h4 class="card-title mb-0"><?php echo ucfirst($lab) ?> (<span id="pw-lab-<?php echo $lab ?>-status"></span>)</h4>
          </div>
          <div class="card-body p-0">
            <a target="_blank" title="Open <?php echo $lab ?> lab layout in new tab" id="pw-lab-<?php echo $lab ?>">
              <img class="img-fluid">
            </a>
          </div>
        </div>
      </div>
<?php endforeach ?>
  </div>
</div>
<?php include "assets/php/templates/footer.php" ?>

<script>
  var url_template = "https://www.poelab.com/wp-content/uploads/{{yyyy}}/{{mm}}/{{yyyy}}-{{mm}}-{{dd}}_{{lab}}.jpg";
  var labs = ["uber","merciless","cruel","normal"];
  var tryCount = {};

  var current = new Date();
  var previous = new Date();
  previous.setDate(previous.getDate() - 1);

  for (let i = 0; i < labs.length; i++) {
    var img = $("#pw-lab-" + labs[i] + " img");

    var statusDiv = $("#pw-lab-" + labs[i] + "-status");
    var date = current.getDate() + "/" + (current.getMonth() + 1) + "/" + current.getFullYear();
    statusDiv.html("<span class='custom-text-green'>" + date + "</span>");
    
    img.attr("src", url_template
      .replace(/{{yyyy}}/g, current.getFullYear())
      .replace(/{{mm}}/g,   current.getMonth() + 1)
      .replace(/{{dd}}/g,   current.getDate())
      .replace(/{{lab}}/g,  labs[i])
    );

    img.on('error', function(event) {
      console.log("Error loading " + labs[i] + ": " + event.target.src);
      var statusDiv = $("#pw-lab-" + labs[i] + "-status");
      var date = previous.getDate() + "/" + (previous.getMonth() + 1) + "/" + previous.getFullYear();

      if (tryCount[labs[i]] === undefined) {
        tryCount[labs[i]] = true;
        statusDiv.html("<span class='custom-text-red'>" + date + "</span>");
      } else {
        console.log("Exceeded maximum retry count for: " + labs[i]);
        return;
      }

      event.target.src = url_template
        .replace(/{{yyyy}}/g, previous.getFullYear())
        .replace(/{{mm}}/g,   previous.getMonth() + 1)
        .replace(/{{dd}}/g,   previous.getDate())
        .replace(/{{lab}}/g,  labs[i]);
      
      console.log("Trying: " + event.target.src);
    });

    img.on('load', function(event) {
      console.log("Loaded " + labs[i] + ": " + event.target.src);
      event.target.parentElement.href = event.target.src;
    });
  }
</script>