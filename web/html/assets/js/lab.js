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
    .replace(/{{mm}}/g,   (current.getMonth() + 1 <= 9 ? "0" : "") + (current.getMonth() + 1))
    .replace(/{{dd}}/g,   (current.getDate() <= 9 ? "0" : "") + current.getDate())
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
      .replace(/{{mm}}/g,   (previous.getMonth() + 1 <= 9 ? "0" : "") + (previous.getMonth() + 1))
      .replace(/{{dd}}/g,   (previous.getDate() <= 9 ? "0" : "") + previous.getDate())
      .replace(/{{lab}}/g,  labs[i]);
    
    console.log("Trying: " + event.target.src);
  });

  img.on('load', function(event) {
    console.log("Loaded " + labs[i] + ": " + event.target.src);
    event.target.parentElement.href = event.target.src;
  });
}