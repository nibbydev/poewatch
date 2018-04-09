/*
  There's not much here except for some poorly written JS functions. And since you're 
  already here, it can't hurt to take a look at http://youmightnotneedjquery.com/
*/

let FILTER;
var ITEMS = [];
var LEAGUES = [];
var CATEGORIES = {};
const HISTORY_LEAGUES = ["Abyss", "Harbinger", "Legacy", "Breach"];
let HISTORY_CHART;

const INITIAL_LOAD_AMOUNT = 150;
const PRICE_PERCISION = 100;
const COUNT_HIGH = 25;
const COUNT_MED = 15;
const MINOR_CHANGE = 50;
const MAJOR_CHANGE = 100;

var parentRow, expandedRow;
var expandedRowTemplate = `<tr><td colspan='100'>
  <div class='row m-1'>
    <div class='col-md'>
      <h4>Chaos value</h4>
      <div class='chart-small'><canvas id="chart-price"></canvas></div>
    </div>
    <div class='col-md'>
      <h4>Listed per 24h</h4>
      <div class='chart-small'><canvas id="chart-quantity"></canvas></div>
    </div>
  </div>
  <hr>
  <div class='row m-1 mt-2'>
    <div class='col-sm'>
      <table class="table table-bordered table-sm details-table">
        <tbody>
          <tr id='details-row-mean'>
            <td>Current mean</td>
          </tr>
          <tr id='details-row-median'>
            <td>Current median</td>
          </tr>
          <tr id='details-row-mode'>
            <td>Current mode</td>
          </tr>
        </tbody>
      </table>
    </div>
    <div class='col-sm'>
      <table class="table table-bordered table-sm details-table">
        <tbody>
          <tr id='details-row-quantity'>
            <td>Average listed per 24h</td>
          </tr>
          <tr id='details-row-1d'>
            <td>Price change since yesterday</td>
          </tr>
          <tr id='details-row-1w'>
            <td>Price change since a week</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
  <hr>
  <div class='row m-1 mb-3'>
    <div class='col-sm'>
      <h4>Past leagues (WIP)</h4>
      <div class='form-group'>
        <select class='form-control' id='history-league-select'>
        </select>
      </div>
      <div class='chart-large'><canvas id="chart-past"></canvas></div>
    </div>
  </div>
</td></tr>`;

$(document).ready(function() {
  var category = getUrlParameter("category").toLowerCase();
  if (!category) return;

  readServiceContainers();

  FILTER = {
    league: LEAGUES[0],
    category: category,
    sub: "all",
    hideLowConfidence: true,
    links: '0',
    search: '',
    gemLvl: '',
    gemQuality: '',
    gemCorrupted: ''
  };

  fillSelectors(category);
  readCookies();

  makeRequest(0, INITIAL_LOAD_AMOUNT);

  // Define league event listener
  $("#search-league").change(function(){
    FILTER.league = $(this).find(":selected").text();
    console.log(FILTER.league);
    document.cookie = "league="+FILTER.league;
    ITEMS = [];
    makeRequest(0, INITIAL_LOAD_AMOUNT);
  });

  // Define subcategory event listener
  $("#search-sub").change(function(){
    FILTER.sub = $(this).find(":selected").text();
    console.log(FILTER.sub);
    ITEMS = [];
    makeRequest(0, INITIAL_LOAD_AMOUNT);
  });

  // Define load more event listener
  $("#button-loadall").on("click", function(){
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
    ITEMS = [];
    makeRequest(0, INITIAL_LOAD_AMOUNT);
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
}); 


function readServiceContainers() {
  $(".service-container").each(function() {
    var container = $(this);
    var id = container.attr("id");
    var payload = container.data("payload");
    payload = payload.replace(/'/g, '"');

    switch (id) {
      case "service-leagues":
        LEAGUES = JSON.parse(payload);
        break;
      case "service-categories":
        CATEGORIES = JSON.parse(payload);
        break;
    }
  });
}


function fillSelectors(category) {
  $("#page-title").text(toTitleCase(category));

  var leagueSelector = $("#search-league");
  $.each(LEAGUES, function(index, league) {   
    leagueSelector.append($("<option></option>").attr("value", index).text(league)); 
  });

  var categorySelector = $("#search-sub");
  categorySelector.append($("<option></option>").attr("value", 0).text("All")); 

  $.each(CATEGORIES[category], function(index, child) {   
    categorySelector.append($("<option></option>").attr("value", index + 1).text(toTitleCase(child))); 
  });

  // Add price table headers
  var tableHeaderContent = "<th scope='col'>Item</th>";
  if (category === "gems") {
    tableHeaderContent += "<th scope='col'>Lvl</th>";
    tableHeaderContent += "<th scope='col'>Qual</th>";
    tableHeaderContent += "<th scope='col'>Corr</th>";
  }
  tableHeaderContent += "<th scope='col'>Price</th>";
  tableHeaderContent += "<th scope='col'>Change</th>";
  tableHeaderContent += "<th scope='col'>Count</th>";
  $("#searchResults > thead > tr").append(tableHeaderContent);
}


function toTitleCase(str) {
  return str.replace(/\w\S*/g, function(txt){return txt.charAt(0).toUpperCase() + txt.substr(1).toLowerCase();});
}


function readCookies() {
  var league = getCookie("league");
  if (league) console.log("Got league from cookie: " + league);

  FILTER.league = league.toLowerCase();
  
  $("#search-league option").filter(function() { 
    return ($(this).text() == league);
  }).prop("selected", true);
}


function getCookie(cname) {
  var name = cname + "=";
  var decodedCookie = decodeURIComponent(document.cookie);
  var ca = decodedCookie.split(';');
  for(var i = 0; i <ca.length; i++) {
      var c = ca[i];
      while (c.charAt(0) == ' ') {
          c = c.substring(1);
      }
      if (c.indexOf(name) == 0) {
          return c.substring(name.length, c.length);
      }
  }
  return "";
}


function onRowClick(event) {
  var target = $(event.currentTarget);
  var parentIndex = parseInt(target.attr("value"));

  // If user clicked on the smaller embedded table
  if (isNaN(parentIndex)) return;

  console.log("Clicked on row: " + parentIndex + " (" + ITEMS[parentIndex]["name"] + ")");

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

  // Add league options to template
  var historyLeagueSelector = $("#history-league-select");
  $.each(HISTORY_LEAGUES, function(index, league) {   
    historyLeagueSelector.append($("<option></option>").attr("value", index).text(toTitleCase(league))); 
  });

  // Create event listener for league selector
  historyLeagueSelector.change(function(){
    var selectedLeauge = HISTORY_LEAGUES[historyLeagueSelector.find(":selected").val()];
    makeHistoryRequest(selectedLeauge, ITEMS[parentIndex]["specificKey"]);
  });

  // Get currently selected league and make request
  var selectedLeauge = HISTORY_LEAGUES[historyLeagueSelector.find(":selected").val()];
  makeHistoryRequest(selectedLeauge, ITEMS[parentIndex]["specificKey"]);

  // Place ChartJS charts inside the expanded row
  placeCharts(parentIndex);

  // Fill expanded row with item data
  var chaosIcon = "<img src='http://web.poecdn.com/image/Art/2DItems/Currency/CurrencyRerollRare.png?scale=1&scaleIndex=1&w=1&h=1'>";
  $("#details-row-quantity",  expandedRow).append("<td>"+ITEMS[parentIndex]["quantity"]+"</td>");
  $("#details-row-mean",      expandedRow).append("<td>"+chaosIcon+ITEMS[parentIndex]["mean"]+"</td>");
  $("#details-row-median",    expandedRow).append("<td>"+chaosIcon+ITEMS[parentIndex]["median"]+"</td>");
  $("#details-row-mode",      expandedRow).append("<td>"+chaosIcon+ITEMS[parentIndex]["mode"]+"</td>");

  let history = ITEMS[parentIndex]["history"]["mean"];
  var chaosChangeDay = roundPrice(ITEMS[parentIndex]["mean"] - history[history.length - 1]);
  var chaosChangeWeek = roundPrice(ITEMS[parentIndex]["mean"] - history[0]);

  $("#details-row-1d",        expandedRow).append("<td>"+chaosIcon+(chaosChangeDay > 0 ? '+' : '')+chaosChangeDay+"</td>");
  $("#details-row-1w",        expandedRow).append("<td>"+chaosIcon+(chaosChangeWeek > 0 ? '+' : '')+chaosChangeWeek+"</td>");

  target.addClass("parent-row");
  expandedRow.addClass("selected-row");
  
  parentRow = target;
}


function makeHistoryRequest(league, key) {
  var request = $.ajax({
    url: "http://api.poe.ovh/history",
    data: {
      league: league, 
      key: key
    },
    type: "GET",
    async: true,
    dataTypes: "json"
  });

  request.done(function(payload) {
    HISTORY_CHART.data.labels = payload["tags"];
    HISTORY_CHART.data.datasets[0].data = payload["prices"];
    HISTORY_CHART.update();
  });
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

  var pastData = {
    type: "line",
    data: {
      labels: [],
      datasets: [{
        label: "Price in chaos",
        data: [],
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
  HISTORY_CHART = new Chart(document.getElementById("chart-past"), pastData);
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
  var data = {
    league: FILTER.league, 
    parent: FILTER.category,
    child: FILTER.sub,
    from: from,
    to: to
  };

  if (FILTER.category === "weapons" || FILTER.category === "armour") {
    data["links"] = FILTER.links;
  }

  var request = $.ajax({
    url: "http://api.poe.ovh/get",
    data: data,
    type: "GET",
    async: true,
    dataTypes: "json"
  });

  request.done(function(json) {
    ITEMS = ITEMS.concat(json);
    sortResults();

    console.log("size: " + ITEMS.length);
    
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
  var nameField = "<span"+(item["frame"] === 9 ? " class='item-foil'" : "")+">";
  nameField += item["name"];
  if ("type" in item) nameField += ", " + item["type"];
  if ("var" in item) nameField += " <span class='badge custom-badge-gray'>" + item["var"] + "</span>";
  if ("tier" in item) nameField += " <span class='badge custom-badge-gray'>" + item["tier"] + "</span>";
  if (item["history"]["mean"].length < 7) nameField += " <span class='badge badge-dark'>New</span>";
  nameField += "</span>";

  // Format gem fields
  var gemFields = "";
  if (item["frame"] === 4) {
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
    //if (FILTER.links) {
    //  if (!("links" in item) || item["links"] !== FILTER.links) continue;
    //} else if ("links" in item) continue;

    // Sort gems, I guess
    if (item["frame"] === 4) {
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
