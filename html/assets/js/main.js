/*
  There's not much here except for some poorly written JS functions. And since you're 
  already here, it can't hurt to take a look at http://youmightnotneedjquery.com/
*/

let FILTER;
var ITEMS = [];
var LEAGUES = [];
var CATEGORIES = {};
var HISTORY_DATA = {};
let HISTORY_CHART;
let HISTORY_LEAGUE;

var PARSE_AMOUNT = 100;

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
    <div class='col-md'>
      <table class="table table-sm details-table">
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
    <div class='col-md'>
      <table class="table table-sm details-table">
        <tbody>
          <tr id='details-row-quantity'>
            <td>Average listed per 24h</td>
          </tr>
          <tr id='details-row-1d'>
            <td>Price since yesterday</td>
          </tr>
          <tr id='details-row-1w'>
            <td>Price since 1 week</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
  <hr>
  <div class='row m-1 mb-3'>
    <div class='col-sm'>
      <h4>Past leagues</h4>
      <div class="btn-group btn-group-toggle my-3" data-toggle="buttons" id="history-league-radio"></div>
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
    links: "",
    search: "",
    gemLvl: "",
    gemQuality: "",
    gemCorrupted: ""
  };

  fillSelectors(category);
  readCookies();

  makeRequest();

  // Define league event listener
  $("#search-league").on("change", function(){
    FILTER.league = $("input[name=league]:checked", this).val();
    console.log(FILTER.league);
    document.cookie = "league="+FILTER.league;
    ITEMS = [];
    makeRequest();
  });

  // Define subcategory event listener
  $("#search-sub").change(function(){
    FILTER.sub = $(this).find(":selected").val();
    console.log("sub-category: '" + FILTER.sub + "'");
    sortResults();
  });

  // Define load all button listener
  var loadall = $(".loadall");
  $(loadall, "button").on("click", function(){
    console.log("loadall");
    loadall.hide();

    PARSE_AMOUNT = -1;
    sortResults();
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
    var button = "<label class='btn btn-sm btn-outline-secondary p-0 px-1'>";
    button += "<input type='radio' name='league' value='"+league+"'>"+league+"</label>";
    leagueSelector.append(button); 
  });

  var categorySelector = $("#search-sub");
  categorySelector.append($("<option></option>").attr("value", "all").text("All")); 

  $.each(CATEGORIES[category], function(index, child) {   
    categorySelector.append($("<option></option>").attr("value", child).text(toTitleCase(child)));
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
  if (!league) {

    $("#search-league input").filter(function() { 
      return ($(this).val() === FILTER.league);
    }).prop("active", true).trigger("click");

    return; 
  }

  console.log("Got league from cookie: " + league);
  FILTER.league = league.toLowerCase();
  
  $("#search-league input").filter(function() { 
    return ($(this).val() === league);
  }).prop("active", true).trigger("click");
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

  // Create event listener for league selector
  var historyLeagueRadio = $("#history-league-radio");
  historyLeagueRadio.change(function(){
    HISTORY_LEAGUE = $("input[name=league]:checked", this).val();;

    if (HISTORY_LEAGUE in HISTORY_DATA) {
      HISTORY_CHART.data.labels = HISTORY_DATA[HISTORY_LEAGUE]["labels"];
      HISTORY_CHART.data.datasets[0].data = HISTORY_DATA[HISTORY_LEAGUE]["values"];
      HISTORY_CHART.update();
    }
  });

  // Make request
  var fullCategory = ITEMS[parentIndex].parent;
  if (ITEMS[parentIndex].child) fullCategory += "-" + ITEMS[parentIndex].child;
  makeHistoryRequest(fullCategory, ITEMS[parentIndex]["index"]);

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


function makeHistoryRequest(category, index) {
  var request = $.ajax({
    url: "http://api.poe-stats.com/history",
    data: {
      category: category, 
      index: index
    },
    type: "GET",
    async: true,
    dataTypes: "json"
  });

  request.done(function(payload) {
    HISTORY_DATA = payload;

    if ("error" in payload) {
      console.log(payload);
      
      var chartArea = $(".chart-large");
      chartArea.after("<h5 class='text-center my-3'>No results</h5>");
      chartArea.remove();
      $("#history-league-radio").remove();

      return;
    }

    var leagues = Object.keys(HISTORY_DATA);

    if (!HISTORY_LEAGUE) HISTORY_LEAGUE = leagues[0];

    let selectedLeague;
    if (HISTORY_LEAGUE in HISTORY_DATA) selectedLeague = HISTORY_LEAGUE;
    else selectedLeague = leagues[0];

    HISTORY_CHART.data.labels = payload[selectedLeague]["labels"];
    HISTORY_CHART.data.datasets[0].data = payload[selectedLeague]["values"];
    HISTORY_CHART.update();

    var historyLeagueRadio = $("#history-league-radio");
    $.each(leagues, function(index, league) {
      var selected = (selectedLeague === league ? " active" : "");

      var button = "<label class='btn btn-outline-secondary"+selected+"'>";
      button += "<input type='radio' name='league' value='"+league+"'>"+league+"</label>";

      historyLeagueRadio.append(button); 
    });
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
        backgroundColor: "rgba(255, 255, 255, 0.5)",
        borderColor: "#fff",
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
        mode: "index",
        callbacks: {
          title: function(tooltipItem, data) {
            return data['datasets'][0]['data'][tooltipItem[0]['index']] + "c";
          },
          label: function(tooltipItem, data) {
            return data['labels'][tooltipItem['index']];
          }
        },
        backgroundColor: '#fff',
        titleFontSize: 16,
        titleFontColor: '#222',
        bodyFontColor: '#444',
        bodyFontSize: 14,
        displayColors: false,
        borderWidth: 1,
        borderColor: '#aaa'
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
        backgroundColor: "rgba(255, 255, 255, 0.5)",
        borderColor: "#fff",
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
        mode: "index",
        callbacks: {
          title: function(tooltipItem, data) {
            return "Quantity: " + data['datasets'][0]['data'][tooltipItem[0]['index']];
          },
          label: function(tooltipItem, data) {
            return data['labels'][tooltipItem['index']];
          }
        },
        backgroundColor: '#fff',
        titleFontSize: 16,
        titleFontColor: '#222',
        bodyFontColor: '#444',
        bodyFontSize: 14,
        displayColors: false,
        borderWidth: 1,
        borderColor: '#aaa'
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
        backgroundColor: "rgba(255, 255, 255, 0.5)",
        borderColor: "#fff",
        borderWidth: 1,
        lineTension: 0,
        pointRadius: 0
      }]
    },
    options: {
      legend: {
        display: false
      },
      responsive: true,
      maintainAspectRatio: false,
      animation: {duration: 0},
      hover: {animationDuration: 0},
      responsiveAnimationDuration: 0,
      tooltips: {
        intersect: false,
        mode: "index",
        callbacks: {
          title: function(tooltipItem, data) {
            return data['datasets'][0]['data'][tooltipItem[0]['index']] + "c";
          },
          label: function(tooltipItem, data) {
            return "Day " + data['labels'][tooltipItem['index']];
          }
        },
        backgroundColor: '#fff',
        titleFontSize: 16,
        titleFontColor: '#222',
        bodyFontColor: '#444',
        bodyFontSize: 14,
        displayColors: false,
        borderWidth: 1,
        borderColor: '#aaa'
      },
      scales: {
        yAxes: [{ticks: {beginAtZero:true}}],
        xAxes: [{
          ticks: {
            autoSkip: false,
            callback: function(value, index, values) {
              return (index % 7 === 0) ? "Week " + (~~(index / 7) + 1) : null;
            }
          }
        }]
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
}


function makeRequest() {
  var data = {
    league: FILTER.league, 
    category: FILTER.category
  };

  var request = $.ajax({
    url: "http://api.poe-stats.com/get",
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
    if (ITEMS.length > PARSE_AMOUNT) $(".loadall").show();
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
  var tmpIcon = item["icon"] ? item["icon"] : "http://poe-stats.com/assets/img/missing.png";
  var iconField = "<span class='table-img-container text-center mr-2'><img src='" + tmpIcon + "'></span>";

  // Format name and variant/links badge
  var nameField = "<span"+(item["frame"] === 9 ? " class='item-foil'" : "")+">";
  nameField += item["name"];
  if ("type" in item) nameField += "<span class='item-type'>, " + item["type"] + "</span>";
  if ("var" in item) nameField += " <span class='badge custom-badge-gray'>" + item["var"] + "</span>";
  else if ("tier" in item) nameField += " <span class='badge custom-badge-gray'>" + item["tier"] + "</span>";
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
}


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

  var parsed_count = 0;

  for (let index = 0; index < ITEMS.length; index++) {
    const item = ITEMS[index];

    if (PARSE_AMOUNT > 0 && parsed_count > PARSE_AMOUNT) break;

    // Hide harbinger pieces of shit. This is temporary
    if (item["child"] === "piece") continue;
    // Hide low confidence items
    if (FILTER.hideLowConfidence && item["count"] < COUNT_MED) continue;
    // Hide sub-categories
    if (FILTER.sub !== "all" && FILTER.sub !== item["child"]) continue;

    // Hide items with different links
    if (FILTER.links) {
      if (!("links" in item)) continue;
      else if (item["links"] !== FILTER.links) continue;
    } else if ("links" in item) continue;

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
    var attachSparkLine = false;
    if (!("tableData" in item)) {
      item["tableData"] = $(parseItem(item, index));
      attachSparkLine = true;
    }

    $("#searchResults").append(item["tableData"]);
    parsed_count++;

    if (attachSparkLine) sparkline.sparkline(document.querySelector("#sparkline-" + index), item["history"]["spark"]);
  }
}
