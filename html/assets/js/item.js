LEAGUE = "Bestiary";

$(document).ready(function() {
  if (!category || !index) return;

  console.log(category);
  console.log(index);

  makeRequest(category, index);
});


function makeRequest(category, index) {
  var data = {
    category: category,
    index: index
  };

  var request = $.ajax({
    url: "http://api.poe-stats.com/priceData",
    data: data,
    type: "GET",
    async: true,
    dataTypes: "json"
  });

  request.done(function(json) {
    console.log("we did it");

    fillData(json);
    buildSparkLine(json);

    
  });
}


function buildSparkLine(item) {
  var svgColorClass = item["data"][LEAGUE]["history"]["change"] > 0 ? "sparkline-green" : "sparkline-orange";
  
  let svg = document.createElement("svg");
  
  svg.setAttribute("class", "sparkline " + svgColorClass);
  svg.setAttribute("width", 100);
  svg.setAttribute("height", 40);
  svg.setAttribute("stroke-width", 3);

  sparkline.sparkline(svg, item["data"][LEAGUE]["history"]["spark"]);

  $(".mega-sparkline").html(svg.outerHTML);
}

function fillData(item) {
  $("#item-icon").prop("src", item["data"][LEAGUE]["icon"]);

  $("#item-name").html(item["data"][LEAGUE]["name"]);
  $("#item-change").html(item["data"][LEAGUE]["history"]["change"] + "%");
  
  $("#item-chaos").html( roundPrice(item["data"][LEAGUE]["mean"]) );
  $("#item-exalt").html( roundPrice(item["data"][LEAGUE]["exalted"]) );

  $("#item-mean").html(item["data"][LEAGUE]["mean"]);
  $("#item-median").html(item["data"][LEAGUE]["median"]);
  $("#item-mode").html(item["data"][LEAGUE]["mode"]);

  $("#item-count").html(item["data"][LEAGUE]["count"]);
  $("#item-quantity").html(item["data"][LEAGUE]["quantity"]);
  //$("#item-1w")  .html(item["data"][LEAGUE]["quantity"]);
}

function roundPrice(price) {
  const numberWithCommas = (x) => {
    var parts = x.toString().split(".");
    parts[0] = parts[0].replace(/\B(?=(\d{3})+(?!\d))/g, ",");
    return parts.join(".");
  }

  return numberWithCommas(Math.round(price * 100) / 100);
}