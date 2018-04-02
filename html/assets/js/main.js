/*
  There's not much here except for some poorly written JS functions. And since you're 
  already here, it can't hurt to take a look at http://youmightnotneedjquery.com/
*/

let FILTER;
var ITEMS = [];

const INITIAL_LOAD_AMOUNT = 100;
const PRICE_PERCISION = 100;
const COUNT_HIGH = 25;
const COUNT_MED = 15;
const MINOR_CHANGE = 50;
const MAJOR_CHANGE = 100;

var parentRow, expandedRow;
var expandedRowTemplate = `<tr><td colspan='100'>
  <div class='row m-1'>
    <div class='col-sm'>
      <h4>Chaos value</h4>
      <div class='chart-small'><canvas id="chart-price"></canvas></div>
    </div>
    <div class='col-sm'>
      <h4>Quantity</h4>
      <div class='chart-small'><canvas id="chart-quantity"></canvas></div>
    </div>
  </div>
  <div class='row m-1 mt-2'>
    <div class='col-sm'>
      <h4>Some additional data</h4>
      <p>Lorem ipsum dolor sit amet, consectetur adipiscing elit. Integer pharetra, enim eget accumsan finibus, lectus orci molestie enim, ut placerat nisi arcu vel urna. In ac condimentum magna, eu maximus lectus.</p>
    </div>
  </div>
  <div class='row m-1 mb-3'>
    <div class='col-sm'>
      <h4>Past leagues (WIP, not an actual chart)</h4>
      <div class='form-group'><select class='form-control'><option>Legacy</option><option>Breach</option><option>Harbinger</option></select></div>
      <div class='chart-large'><canvas id="chart-past"></canvas></div>
    </div>
  </div>
</td></tr>`;

// Load conversion rates on page load
$(document).ready(function() {
  var selectorLeague = $("#search-league");
  var selectorSub = $("#search-sub");
  var buttonLoadAll = $("#button-loadall");

  FILTER = {
    league: selectorLeague.find(":selected").text().toLowerCase(),
    category: getUrlParameter("category").toLowerCase(),
    sub: selectorSub.find(":selected").text().toLowerCase(),
    hideLowConfidence: true,
    links: "",
    search: "",
    gemLvl: "",
    gemQuality: "",
    gemCorrupted: ""
  };

  // Define league event listener
  selectorLeague.change(function(){
    FILTER.league = $(this).find(":selected").text();
    console.log(FILTER.league);
    // Empty item history
    ITEMS = [];

    makeRequest(0, INITIAL_LOAD_AMOUNT);
  });

  // Define subcategory event listener
  selectorSub.change(function(){
    FILTER.sub = $(this).find(":selected").text();
    console.log(FILTER.sub);
    // Empty item history
    ITEMS = [];

    makeRequest(0, INITIAL_LOAD_AMOUNT);
  });

  // Define load more event listener
  buttonLoadAll.on("click", function(){
    console.log("loadmore");
    makeRequest(INITIAL_LOAD_AMOUNT, 0);
    $(this).hide();
  });

  // Define searchbar event listener
  $("#search-searchbar").on("input", function(){
    FILTER.search = $(this).val().toLowerCase().trim();
    console.log("search: '" + FILTER.search + "'");
    sortResults();
  });

  // Define low confidence radio button event listener
  $("#radio-confidence").on("change", function(){
    let newValue = $("input[name=confidence]:checked", this).val();
    console.log(newValue);
    if (newValue != FILTER.hideLowConfidence) {
      FILTER.hideLowConfidence = !FILTER.hideLowConfidence;
      sortResults();
    }
  });

  // Define link radio button event listener
  $("#radio-links").on("change", function(){
    FILTER.links = $("input[name=links]:checked", this).val();
    console.log(FILTER.links);
    sortResults();
  });

  // Define gem selector and radio event listeners
  $("#select-level").on("change", function(){
    FILTER.gemLvl = $(":selected", this).val();
    console.log(FILTER.gemLvl);
    sortResults();
  });
  $("#select-quality").on("change", function(){
    FILTER.gemQuality = $(":selected", this).val();
    console.log(FILTER.gemQuality);
    sortResults();
  });
  $("#radio-corrupted").on("change", function(){
    FILTER.gemCorrupted = $(":checked", this).val();
    console.log(FILTER.gemCorrupted);
    sortResults();
  });

  // Define tr click event
  $("#searchResults > tbody").delegate("tr", "click", function(event) {
    onRowClick(event);
  });

  checkFields();

  makeRequest(0, INITIAL_LOAD_AMOUNT);
}); 


function onRowClick(event) {
  var target = $(event.currentTarget);

  // Close row if user clicked on parentRow
  if (target.is(parentRow)) {
    console.log("Closed row");
    expandedRow.remove();
    parentRow.removeAttr("class");
    parentRow = null;
    expandedRow = null;
    return;
  }

  // Don't add a new row if user clicked on expandedRow
  if (target.is(expandedRow)) return;

  // If there's an expanded row open somewhere, remove it
  if (expandedRow) {
    expandedRow.remove();
    parentRow.removeAttr("class");
  }

  expandedRow = $(expandedRowTemplate);
  target.closest("tr").after(expandedRow);

  var parentIndex = parseInt(target.attr("value"));
  console.log("Clicked on row: " + parentIndex + " (" + ITEMS[parentIndex]["name"] + ")");

  // Here goes chartJS code
  placeCharts(parentIndex);

  target.addClass("parent-row");
  expandedRow.addClass("selected-row");
  
  parentRow = target;
}


function placeCharts(index) {
  var priceData = {
    type: "line",
    data: {
      labels: getAllDays(ITEMS[index]["history"]["mean"].length),
      datasets: [{
        label: "Price in chaos",
        data: ITEMS[index]["history"]["mean"],
        backgroundColor: "rgba(0, 0, 0, 0.2)",
        borderColor: "#222",
        borderWidth: 1,
        lineTension: 0,
        pointRadius: 0
      }]
    },
    options: {
      legend: {display: false},
      responsive: true,
      maintainAspectRatio: false,
      animation: {duration: 0},
      hover: {animationDuration: 0},
      responsiveAnimationDuration: 0,
      tooltips: {
        intersect: false,
        mode: "index"
      }
    }
  }
  
  var quantData = {
    type: "line",
    data: {
      labels: getAllDays(ITEMS[index]["history"]["quantity"].length),
      datasets: [{
        label: "Quantity",
        data: ITEMS[index]["history"]["quantity"],
        backgroundColor: "rgba(0, 0, 0, 0.2)",
        borderColor: "#222",
        borderWidth: 1,
        lineTension: 0,
        pointRadius: 0
      }]
    },
    options: {
      legend: {display: false},
      responsive: true,
      maintainAspectRatio: false,
      animation: {duration: 0},
      hover: {animationDuration: 0},
      responsiveAnimationDuration: 0,
      tooltips: {
        intersect: false,
        mode: "index"
      }
    }
  }

  var randData = randNumbers(90, 50, 10);
  var pastData = {
    type: "line",
    data: {
      labels: getAllDays(randData.length),
      datasets: [{
        label: "Price in chaos",
        data: randData,
        backgroundColor: "rgba(0, 0, 0, 0.2)",
        borderColor: "#222",
        borderWidth: 1,
        lineTension: 0,
        pointRadius: 0
      }]
    },
    options: {
      legend: {display: false},
      responsive: true,
      maintainAspectRatio: false,
      animation: {duration: 0},
      hover: {animationDuration: 0},
      responsiveAnimationDuration: 0,
      tooltips: {
        intersect: false,
        mode: "index"
      },
      scales: {
        yAxes: [{ticks: {beginAtZero:true}}]
      }
    }
  }

  new Chart(document.getElementById("chart-price"), priceData);
  new Chart(document.getElementById("chart-quantity"), quantData);
  new Chart(document.getElementById("chart-past"), pastData);
}


function randNumbers(size, add, mult) {
  var numbers = [];
  
  for (var i = 0; i < size; i += 1) {
    numbers.push(add + Math.random() * mult);
  }
  
  return numbers;
}


function getAllDays(length) {
  const MONTH_NAMES = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", 
    "Jul", "Augt", "Sep", "Oct", "Nov", "Dec"
  ];
  var a = [];
  
  for (let index = length; index > 0; index--) {
    var s = new Date();
    var n = new Date(s.setDate(s.getDate() - index))
    a.push(s.getDate() + " " + MONTH_NAMES[s.getMonth()]);
  }

  return a;
};


function makeRequest(from, to) {
  var request = $.ajax({
    url: "http://api.poe.ovh/get",
    data: {
      league: FILTER.league, 
      parent: FILTER.category,
      child: FILTER.sub,
      from: from,
      to: to,
      exclude: "mode,index"
    },
    type: "GET",
    async: true,
    dataTypes: "json"
  });

  request.done(function(json) {
    ITEMS = ITEMS.concat(json);
    sortResults();
    
    // Enable "show more" button
    if (ITEMS.length === INITIAL_LOAD_AMOUNT) $(".loadall").show();
  });
}


function checkFields() {
  if (FILTER.category === "armour" || FILTER.category === "weapons") {
    $(".link-fields").removeClass("link-fields");
  } else if (FILTER.category === "gems") {
    $(".gem-fields").removeClass("gem-fields");
  }
}


function parseItem(item, index) {
  // Format icon
  var tmpIcon = item["icon"] ? item["icon"] : "http://poe.ovh/assets/img/missing.png";
  var iconField = "<span class='table-img-container text-center mr-2'><img src='" + tmpIcon + "'></span>";

  // Format name and variant/links badge
  var nameField = item["name"];
  if ("type" in item) nameField += ", " + item["type"];
  if ("var" in item) nameField += " <span class='badge custom-badge-gray'>" + item["var"] + "</span>";
  if ("tier" in item) nameField += " <span class='badge custom-badge-gray'>" + item["tier"] + "</span>";

  // Format gem fields
  var gemFields = "";
  if (item["frame"] === "4") {
    gemFields += "lvl" in item ? "<td>" + item["lvl"] + "</td>" : "<td>0</td>";
    gemFields += "quality" in item ? "<td>" + item["quality"] + "</td>" : "<td>0</td>";

    if ("corrupted" in item) {
      if (item["corrupted"] === "1") gemFields += "<td><span class='badge custom-badge-red'>Yes</span></td>";
      else gemFields += "<td><span class='badge custom-badge-green'>No</span></td>";
    }
  }

  // Format price and sparkline field
  var priceField = "<div class='sparklinebox'>";
  var sparkColorClass = item["history"]["change"] > 0 ? "sparkline-green" : "sparkline-orange";
  priceField += "<svg class='sparkline "+sparkColorClass+"' width='60' height='25' stroke-width='3' id='sparkline-"+index+"'></svg>";
  priceField += "<img src='http://web.poecdn.com/image/Art/2DItems/Currency/CurrencyRerollRare.png?scale=1&scaleIndex=1&w=1&h=1'>";
  priceField += roundPrice(item["mean"]);
  priceField += "</div>";

  // Format change field
  var tmpChange;
  if (item["history"]["change"] > MAJOR_CHANGE) tmpChange = "custom-badge-green";
  else if (item["history"]["change"] < -1*MAJOR_CHANGE) tmpChange = "custom-badge-orange";
  else if (item["history"]["change"] > MINOR_CHANGE) tmpChange = "custom-badge-green-lo";
  else if (item["history"]["change"] < -1*MINOR_CHANGE) tmpChange = "custom-badge-orange-lo";
  else tmpChange = "custom-badge-gray";
  var changeField = "<span class='badge "+tmpChange+"'>"+item["history"]["change"]+"%</span>";

  // Format count badge
  var countField;
  if (item["count"] >= COUNT_HIGH) countField = "<span class='badge custom-badge-gray'>" + item["count"] + "</span>";
  else if (item["count"] >= COUNT_MED) countField = "<span class='badge custom-badge-orange'>" + item["count"] + "</span>";
  else countField = "<span class='badge custom-badge-red'>" + item["count"] + "</span>";

  // Add it all together
  var returnString = "<tr value=" + ITEMS.indexOf(item) + ">" +
  "<td>" +  iconField + nameField + "</td>" + 
  gemFields +
  "<td>" + priceField + "</td>" + 
  "<td>" + changeField + "</td>" + 
  "<td>" + countField + "</td>" + 
  "</tr>";

  return returnString;
}


function getUrlParameter(sParam) {
  var sPageURL = decodeURIComponent(window.location.search.substring(1)),
    sURLVariables = sPageURL.split('&'),
    sParameterName,
    i;

  for (i = 0; i < sURLVariables.length; i++) {
    sParameterName = sURLVariables[i].split('=');

    if (sParameterName[0] === sParam) {
      return sParameterName[1] === undefined ? true : sParameterName[1];
    }
  }
};


function roundPrice(price) {
  const numberWithCommas = (x) => {
    var parts = x.toString().split(".");
    parts[0] = parts[0].replace(/\B(?=(\d{3})+(?!\d))/g, ",");
    return parts.join(".");
  }

  return numberWithCommas(Math.round(price * PRICE_PERCISION) / PRICE_PERCISION);
}


function sortResults() {
  // Empty the table
  $("#searchResults > tbody").empty();

  for (let index = 0; index < ITEMS.length; index++) {
    const item = ITEMS[index];

    // Hide harbinger pieces of shit. This is temporary
    if (item["child"] === "piece") continue;

    // Hide low confidence items
    if (FILTER.hideLowConfidence && item["count"] < COUNT_MED) continue;

    // Hide items with different links
    if (FILTER.links) {
      if (!("links" in item) || item["links"] !== FILTER.links) continue;
    } else if ("links" in item) continue;

    // Sort gems, I guess
    if (item["frame"] === "4") {
      if (FILTER.gemLvl !== "") {
        if (item["lvl"] != FILTER.gemLvl) continue;
      }
      if (FILTER.gemQuality !== "") {
        if (FILTER.gemQuality) {
          if (!("quality" in item)) continue;
          if (item["quality"] != FILTER.gemQuality) continue;
        } else {
          if ("quality" in item && item["quality"] > 0) continue;
        }
      }
      // Sort based on corruption selector
      if (FILTER.gemCorrupted === "1" && item["corrupted"] !== "1") continue;
      else if (FILTER.gemCorrupted === "0" && item["corrupted"] !== "0") continue;
    }

    if (FILTER.search) {
      var nameBool = ("name" in item && item["name"].toLowerCase().indexOf(FILTER.search) !== -1);
      var parentBool = ("parent" in item && item["parent"].toLowerCase().indexOf(FILTER.search) !== -1);
      var childBool = ("child" in item && item["child"].toLowerCase().indexOf(FILTER.search) !== -1);
      var typeBool = ("type" in item && item["type"].toLowerCase().indexOf(FILTER.search) !== -1);

      if (!nameBool && !parentBool && !childBool && !typeBool) continue;
    }

    // If item has not been parsed, parse it 
    var temp = false;
    if (!("tableData" in item)) {
      item["tableData"] = $(parseItem(item, index));
      temp = true;
    }

    $("#searchResults").append(item["tableData"]);

    if (temp) sparkline.sparkline(document.querySelector("#sparkline-" + index), item["history"]["spark"]);
  }
}
