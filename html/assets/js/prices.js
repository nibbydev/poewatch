/*
  There's not much here except for some poorly written JS functions. And since you're 
  already here, it can't hurt to take a look at http://youmightnotneedjquery.com/
*/

// Default item search filter options
var FILTER = {
  league: null,
  category: null,
  sub: "all",
  hideLowConfidence: true,
  links: null,
  search: null,
  gemLvl: null,
  gemQuality: null,
  gemCorrupted: null
};

var ITEMS = [];
var LEAGUES;
var CATEGORIES;
var HISTORY_DATA = {};
var HISTORY_CHART;
var HISTORY_LEAGUE;
var INTERVAL;

var ROW_parent, ROW_expanded;
var PARSE_AMOUNT = 100;
var COUNTER = {
  lowCount: 0,
  categories: {}
};

const PRICE_PERCISION = 100;
const ENCH_QUANT_HIGH = 10;
const ENCH_QUANT_MED = 5;
const QUANT_HIGH = 7;
const QUANT_MED = 3;
const MINOR_CHANGE = 50;
const MAJOR_CHANGE = 100;

// Re-used icon urls
const ICON_ENCHANTMENT = "http://web.poecdn.com/image/Art/2DItems/Currency/Enchantment.png?scale=1&w=1&h=1";
const ICON_EXALTED = "http://web.poecdn.com/image/Art/2DItems/Currency/CurrencyAddModToRare.png?scale=1&w=1&h=1";
const ICON_CHAOS = "http://web.poecdn.com/image/Art/2DItems/Currency/CurrencyRerollRare.png?scale=1&w=1&h=1";
const ICON_MISSING = "http://poe-stats.com/assets/img/missing.png";

var TEMPLATE_expandedRow = `
<tr class='selected-row'><td colspan='100'>
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
          <tr>
            <td>Current mean</td>
            <td>{{mean}}</td>
          </tr>
          <tr>
            <td>Current median</td>
            <td>{{median}}</td>
          </tr>
          <tr>
            <td>Current mode</td>
            <td>{{mode}}</td>
          </tr>
        </tbody>
      </table>
    </div>
    <div class='col-md'>
      <table class="table table-sm details-table">
        <tbody>
          <tr>
            <td>Total amount listed</td>
            <td>{{count}}</td>
          </tr>
          <tr>
            <td>Price since yesterday</td>
            <td>{{1d}}</td>
          </tr>
          <tr>
            <td>Price since 1 week</td>
            <td>{{1w}}</td>
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

var TEMPLATE_prices = `
<td>
  <div class='pricebox'>{{sparkline}}{{chaos_icon}}{{chaos_price}}</div>
</td>
<td>
  <div class='pricebox'>{{ex_icon}}{{ex_price}}</div>
</td>`;

var TEMPLATE_gemFields = `
<td>{{lvl}}</td>
<td>{{quality}}</td>
<td><span class='badge custom-badge-{{color}}'>{{corr}}</span></td>`;
  
var TEMPLATE_changeField = `
<td><span class='badge custom-badge-block custom-badge-{{color}}'>{{percent}}%</span></td>`;

var TEMPLATE_quantField = `
<td><span class='badge custom-badge-block custom-badge-{{color}}'>{{quant}}</span></td>`;

var TEMPLATE_row = `
<tr value={{id}}>{{name}}{{gem}}{{price}}{{change}}{{quant}}</tr>`;

var TEMPLATE_th = `<th scope='col'>{{name}}</th>`;

var TEMPLATE_option = `<option value="{{value}}">{{name}}</option>`;

var TEMPLATE_leagueBtn = `
<label class="btn btn-sm btn-outline-dark p-0 px-1 {{active}}">
  <input type="radio" name="league" value="{{value}}">{{name}}
</label>`;

var TEMPLATE_imgContainer = `
<span class='table-img-container text-center mr-1'><img src={{img}}></span>`;

$(document).ready(function() {
  if (!SERVICE_category) return;

  LEAGUES = SERVICE_leagues;
  CATEGORIES = SERVICE_categories;

  //FILTER.league = LEAGUES[0];
  FILTER.league = "Incursion";
  FILTER.category = SERVICE_category;

  readLeagueFromCookies(FILTER);
  makeRequest(FILTER, ITEMS, PARSE_AMOUNT); 

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
    console.log("Selected sub-category: " + FILTER.sub);
    sortResults();
  });

  // Define load all button listener
  var loadall = $(".loadall");
  $(loadall, "button").on("click", function(){
    console.log("Button press: loadall");
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
    let option = $("input:checked", this).val() === "1";
    console.log("Show low count: " + option);
    
    if (FILTER.hideLowConfidence != option) {
      FILTER.hideLowConfidence = option;
      sortResults();
    }
  });

  // Define link radio button event listener
  $("#radio-links").on("change", function(){
    FILTER.links = $("input[name=links]:checked", this).val();
    console.log("Link filter: " + FILTER.links);
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

  // Define live search toggle listener
  $("#live-updates").on("change", function(){
    var live = $("input[name=live]:checked", this).val() == "true";
    console.log("Live updates: " + live);
    document.cookie = "live="+live;

    var bar = $("#progressbar-live");
    if (live) {
      bar.css("animation-name", "progressbar-live");
      INTERVAL = setInterval(timedRequestCallback, 60 * 1000);
    } else {
      bar.css("animation-name", "");
      clearInterval(INTERVAL);
    }
  });
}); 

//------------------------------------------------------------------------------------------------------------
// Data prep
//------------------------------------------------------------------------------------------------------------

function readLeagueFromCookies(FILTER) {
  let league = getCookie("league");

  if (league) {
    console.log("Got league from cookie: " + league);
    FILTER.league = league;
  }

  $("#search-league input").filter(function() { 
    return ($(this).val() === FILTER.league);
  }).prop("active", true).trigger("click");
}

//------------------------------------------------------------------------------------------------------------
// Expanded row
//------------------------------------------------------------------------------------------------------------

function onRowClick(event) {
  var target = $(event.currentTarget);
  var index = parseInt(target.attr("value"));

  // If user clicked on the smaller embedded table
  if ( isNaN(index) ) return;

  var item = ITEMS[index];

  console.log("Clicked on row: " + index + " (" + item["name"] + ")");

  // Close expanded row if user clicked on a parentRow
  if (target.is(ROW_parent)) {
    console.log("Closed row");
    ROW_expanded.remove();
    ROW_parent.removeAttr("class");
    ROW_parent = null;
    ROW_expanded = null;
    return;
  }

  // Don't add a new row if user clicked on expandedRow
  if ( target.is(ROW_expanded) ) return;

  // If there's an expanded row open somewhere, remove it
  if (ROW_expanded) {
    ROW_expanded.remove();
    ROW_parent.removeAttr("class");
  }

  let chaosContainer = TEMPLATE_imgContainer.trim().replace("{{img}}", ICON_CHAOS);
  let history = item["history"]["mean"];
  let chaosChangeDay = roundPrice(item["mean"] - history[history.length - 1]);
  let chaosChangeWeek = roundPrice(item["mean"] - history[0]);

  let template = TEMPLATE_expandedRow.trim()
    .replace("{{mean}}",    chaosContainer + roundPrice(item["mean"]))
    .replace("{{median}}",  chaosContainer + roundPrice(item["median"]))
    .replace("{{mode}}",    chaosContainer + roundPrice(item["mode"]))
    .replace("{{count}}",                    roundPrice(item["count"]))
    .replace("{{1d}}",      chaosContainer + (chaosChangeDay   > 0 ? '+' : '') + chaosChangeDay)
    .replace("{{1w}}",      chaosContainer + (chaosChangeWeek  > 0 ? '+' : '') + chaosChangeWeek);

  // Set gvar
  ROW_expanded = $(template);

  // Load history data
  if (item["index"] in HISTORY_DATA) {
    console.log("history from: memory");
    placeCharts(index, ROW_expanded);
    displayHistory(item["index"], ROW_expanded);
  } else {
    console.log("history from: source");
    makeHistoryRequest(item["parent"], index, ROW_expanded);
  }

  target.addClass("parent-row");
  target.after(ROW_expanded);
  ROW_parent = target;

  // Create event listener for league selector
  $("#history-league-radio", ROW_expanded).change(function(){
    HISTORY_LEAGUE = $("input[name=league]:checked", this).val();;
  
    if (HISTORY_LEAGUE in HISTORY_DATA[item["index"]]) {
      HISTORY_CHART.data.labels = HISTORY_DATA[item["index"]][HISTORY_LEAGUE];
      HISTORY_CHART.data.datasets[0].data = HISTORY_DATA[item["index"]][HISTORY_LEAGUE];
      HISTORY_CHART.update();
    }
  });
}

function placeCharts(index, expandedRow) {
  var priceData = {
    type: "line",
    data: {
      labels: getAllDays(ITEMS[index]["history"]["mean"].length),
      datasets: [{
        label: "Price in chaos",
        data: ITEMS[index]["history"]["mean"],
        backgroundColor: "rgba(255, 255, 255, 0.2)",
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
        backgroundColor: "rgba(255, 255, 255, 0.2)",
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
        backgroundColor: "rgba(255, 255, 255, 0.2)",
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
            return "Day " + tooltipItem['index'];
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

  new Chart($("#chart-price", expandedRow), priceData);
  new Chart($("#chart-quantity", expandedRow), quantData);
  HISTORY_CHART = new Chart($("#chart-past", expandedRow), pastData);
}

function displayHistory(index, expandedRow) {
  if ("error" in HISTORY_DATA[index]) {
    console.log("History data: no results");

    var chartArea = $(".chart-large", expandedRow);
    chartArea.append("<h5 class='text-center my-3'>No results</h5>");
    $("#chart-past", expandedRow).remove();
    $("#history-league-radio", expandedRow).remove();

    return;
  }

  var leagues = Object.keys(HISTORY_DATA[index]);

  if (!HISTORY_LEAGUE) HISTORY_LEAGUE = leagues[0];

  let selectedLeague;
  if (HISTORY_LEAGUE in HISTORY_DATA[index]) selectedLeague = HISTORY_LEAGUE;
  else selectedLeague = leagues[0];

  HISTORY_CHART.data.labels = HISTORY_DATA[index][selectedLeague];
  HISTORY_CHART.data.datasets[0].data = HISTORY_DATA[index][selectedLeague];
  HISTORY_CHART.update();

  let tmp_leagueBtnString = "";

  $.each(leagues, function(index, league) {
    if (!~LEAGUES.indexOf(league)) return;

    tmp_leagueBtnString += TEMPLATE_leagueBtn.trim()
      .replace("{{active}}", (selectedLeague === league ? "active" : ""))
      .replace("{{value}}", league)
      .replace("{{name}}", formatLeague(league));
  });

  $.each(leagues, function(index, league) {
    if (~LEAGUES.indexOf(league)) return;

    tmp_leagueBtnString += TEMPLATE_leagueBtn.trim()
      .replace("{{active}}", (selectedLeague === league ? "active" : ""))
      .replace("{{value}}", league)
      .replace("{{name}}", formatLeague(league));
  });

  $("#history-league-radio", expandedRow).append(tmp_leagueBtnString);
}

//------------------------------------------------------------------------------------------------------------
// Requests
//------------------------------------------------------------------------------------------------------------

function makeRequest() {
  var data = {
    league: FILTER.league, 
    category: FILTER.category
  };

  var request = $.ajax({
    url: "http://159.69.15.81/api/get.php",
    data: data,
    type: "GET",
    async: true,
    dataTypes: "json"
  });

  request.done(function(json) {
    console.log("got " + json.length + " items");

    ITEMS = json;

    sortResults(json);
    
    // Enable "show more" button
    if (json.length > PARSE_AMOUNT) {
      var loadAllDiv = $(".loadall");
      $("button", loadAllDiv).text("Load more (" + (json.length - PARSE_AMOUNT) + ")");
      loadAllDiv.show();
    }
  });
}

function makeHistoryRequest(category, parentIndex, expandedRow) {
  var index = ITEMS[parentIndex]["index"];

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
    HISTORY_DATA[index] = payload;
    placeCharts(parentIndex, expandedRow);
    displayHistory(index, expandedRow);
  });
}

function timedRequestCallback() {
  console.log("Automatic update");

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
    ITEMS = json;
    sortResults();

    console.log("got " + ITEMS.length + " items");
  });
}

//------------------------------------------------------------------------------------------------------------
// Item data parsing and displaying
//------------------------------------------------------------------------------------------------------------

function parseItem(item, index) {
  // Format name and variant/links badge
  var nameField = buildNameField(item);

  // Format gem fields
  var gemFields = buildGemFields(item);
  
  // Format price and sparkline field
  var priceFields = buildPriceFields(item);

  // Format change field
  var changeField = buildChangeField(item);

  // Format count badge
  var quantField = buildQuantField(item);

  return TEMPLATE_row.trim().replace("{{id}}", index)
    .replace("{{name}}",    nameField)
    .replace("{{gem}}",     gemFields)
    .replace("{{price}}",   priceFields)
    .replace("{{change}}",  changeField)
    .replace("{{quant}}",   quantField);
}

function buildNameField(item) {
  let template = `
  <td>
    <span class='table-img-container text-center mr-1'><img src='{{icon}}'></span>
    <span {{foil}}>{{name}}{{type}}</span>{{var_or_tier}}
  </td>
  `.trim();

  if ( item["icon"] ) {
    template = template.replace("{{icon}}", item["icon"]);
  } else {
    template = template.replace("{{icon}}", ICON_MISSING);
  }

  if (item["frame"] === 9) {
    template = template.replace("{{foil}}", "class='item-foil'");
  } else {
    template = template.replace("{{foil}}", "");
  }

  if (FILTER.category === "enchantments") {
    template = template.replace("{{name}}", item["name"].replace("#", item["var"]));
    item["var"] = null;
  } else {
    template = template.replace("{{name}}", item["name"]);
  }

  if (item["type"]) {
    let tmp = "<span class='subtext-1'>, " + item["type"] + "</span>";;
    template = template.replace("{{type}}", tmp);
  } else {
    template = template.replace("{{type}}", "");
  }

  if (item["var"]) {
    let tmp = " <span class='badge custom-badge-gray'>" + item["var"] + "</span>";
    template = template.replace("{{var_or_tier}}", tmp);
  } else if (item["tier"]) {
    let tmp = " <span class='badge custom-badge-gray'>" + item["tier"] + "</span>";
    template = template.replace("{{var_or_tier}}", tmp);
  } else {
    template = template.replace("{{var_or_tier}}", "");
  }

  if (item["history"]["mean"].length < 7) {
    let tmp = "<span class='badge badge-light'>New</span>";
    template = template.replace("{{new}}", tmp);
  } else {
    template = template.replace("{{new}}", "");
  }
  
  return template;
}

function buildGemFields(item) {
  if (item["frame"] !== 4) return "";

  template = TEMPLATE_gemFields.trim();

  template = template.replace("{{lvl}}",      item["lvl"]);
  template = template.replace("{{quality}}",  item["quality"]);
  
  if (item["corrupted"] === "1") {
    template = template.replace("{{color}}",  "red");
    template = template.replace("{{corr}}",   "✓");
  } else {
    template = template.replace("{{color}}",  "green");
    template = template.replace("{{corr}}",   "✕");
  }

  return template;
}

function buildSparkLine(item) {
  var svgColorClass = item["history"]["change"] > 0 ? "sparkline-green" : "sparkline-orange";
  
  let svg = document.createElement("svg");
  
  svg.setAttribute("class", "sparkline " + svgColorClass);
  svg.setAttribute("width", 60);
  svg.setAttribute("height", 25);
  svg.setAttribute("stroke-width", 3);

  sparkline.sparkline(svg, item["history"]["spark"]);

  return svg.outerHTML;
}

function buildPriceFields(item) {
  var template = TEMPLATE_prices.trim();
  var chaosContainer = TEMPLATE_imgContainer.trim().replace("{{img}}", ICON_CHAOS);
  var exContainer = TEMPLATE_imgContainer.trim().replace("{{img}}", ICON_EXALTED);

  let sparkLine = buildSparkLine( item );
  template = template.replace("{{sparkline}}", sparkLine);
  template = template.replace("{{chaos_price}}", roundPrice( item["mean"] ));
  template = template.replace("{{chaos_icon}}", chaosContainer);

  if ("exalted" in item && item["exalted"] >= 1) {
    template = template.replace("{{ex_icon}}", exContainer);
    template = template.replace("{{ex_price}}", roundPrice( item["exalted"] ));
  } else {
    template = template.replace("{{ex_icon}}", "");
    template = template.replace("{{ex_price}}", "");
  }
  
  return template;
}

function buildChangeField(item) {
  let template = TEMPLATE_changeField.trim();

  if (item["history"]["change"] > MAJOR_CHANGE) {
    template = template.replace("{{color}}", "green");
  } else if (item["history"]["change"] < -1*MAJOR_CHANGE) {
    template = template.replace("{{color}}", "orange");
  } else if (item["history"]["change"] > MINOR_CHANGE) {
    template = template.replace("{{color}}", "green-lo");
  } else if (item["history"]["change"] < -1*MINOR_CHANGE) {
    template = template.replace("{{color}}", "orange-lo");
  } else {
    template = template.replace("{{color}}", "gray");
  }

  template = template.replace("{{percent}}", Math.round(item["history"]["change"]));

  return template;
}

function buildQuantField(item) {
  let template = TEMPLATE_quantField.trim();

  if (item["frame"] === -1) {
    if (item["quantity"] >= ENCH_QUANT_HIGH) {
      template = template.replace("{{color}}", "gray");
    } else if (item["quantity"] >= ENCH_QUANT_MED) {
      template = template.replace("{{color}}", "orange");
    } else {
      template = template.replace("{{color}}", "red");
    }
  } else {
    if (item["quantity"] >= QUANT_HIGH) {
      template = template.replace("{{color}}", "gray");
    } else if (item["quantity"] >= QUANT_MED) {
      template = template.replace("{{color}}", "orange");
    } else {
      template = template.replace("{{color}}", "red");
    }
  }

  template = template.replace("{{quant}}", item["quantity"]);

  return template;
}

//------------------------------------------------------------------------------------------------------------
// Utility functions
//------------------------------------------------------------------------------------------------------------

function roundPrice(price) {
  const numberWithCommas = (x) => {
    var parts = x.toString().split(".");
    parts[0] = parts[0].replace(/\B(?=(\d{3})+(?!\d))/g, ",");
    return parts.join(".");
  }

  return numberWithCommas(Math.round(price * PRICE_PERCISION) / PRICE_PERCISION);
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
  
  for (let index = length; index > 1; index--) {
    var s = new Date();
    var n = new Date(s.setDate(s.getDate() - index))
    a.push(s.getDate() + " " + MONTH_NAMES[s.getMonth()]);
  }
  
  a.push("Atm");

  return a;
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

function toTitleCase(str) {
  return str.replace(/\w\S*/g, function(txt){return txt.charAt(0).toUpperCase() + txt.substr(1).toLowerCase();});
}

function formatLeague(name) {
  if (~name.indexOf(" Event")) return name.substring(0, name.indexOf(" Event"));
  else if (~name.indexOf("Hardcore ")) return "HC " + name.substring(9);
  else return name;
}

function formatCategory(name) {
  switch (name) {
    case "activegem":
      return "Skill Gems";
    case "supportgem":
      return "Support Gems";
    case "vaalgem":
      return "Vaal gems";

    case "twomace":
      return "2H Maces";
    case "onemace":
      return "1H Maces";

    case "twosword":
      return "2H Swords";
    case "onesword":
      return "1H Swords";

    case "twoaxe":
      return "2H Axes";
    case "oneaxe":
      return "1H Axes";

    case "wand":
      return "Wands";
    case "bow":
      return "Bows";
    case "rod":
      return "Rods";
    case "dagger":
      return "Daggers";
    case "claw":
      return "Claws";
    case "staff":
      return "Staves";

    case "boots":
      return "Boots";
    case "helmet":
      return "Helmets";
    case "chest":
      return "Body Armours";
    case "gloves":
      return "Gloves";
    case "shield":
      return "Shields";
    case "quiver":
      return "Quivers";

    case "ring":
      return "Rings";
    case "amulet":
      return "Amulets";
    case "belt":
      return "Belts";

    default:
      return toTitleCase(name);
  }
}

//------------------------------------------------------------------------------------------------------------
// Sorting and searching
//------------------------------------------------------------------------------------------------------------

function sortResults() {
  // Empty the table
  var table = $("#searchResults");
  $("tbody", table).empty();

  var parsed_count = 0;
  var tableDataBuffer = "";

  for (let index = 0; index < ITEMS.length; index++) {
    const item = ITEMS[index];

    if (parsed_count > PARSE_AMOUNT) break;
    if ( checkHideItem(item) ) continue;

    // If item has not been parsed, parse it 
    if ( !("tableData" in item) ) item["tableData"] = parseItem(item, index);

    tableDataBuffer += item["tableData"];
    parsed_count++;
  }

  table.append(tableDataBuffer);
}

function checkHideItem(item) {
  // Hide low confidence items
  if (FILTER.hideLowConfidence) {
    if (item["quantity"] < QUANT_MED) return true;
  }

  // Hide sub-categories
  if (FILTER.sub !== "all" && FILTER.sub !== item["child"]) return true;

  // Hide items with different links
  if (FILTER.links && item["links"] !== FILTER.links) return true;

  // Sort gems, I guess
  if (item["frame"] === 4) {
    if (FILTER.gemLvl !== "") {
      if (item["lvl"] != FILTER.gemLvl) return true;
    }
    if (FILTER.gemQuality !== "") {
      if (FILTER.gemQuality) {
        if (!("quality" in item)) return true;
        if (item["quality"] != FILTER.gemQuality) return true;
      } else {
        if ("quality" in item && item["quality"] > 0) return true;
      }
    }
    // Sort based on corruption selector
    if (FILTER.gemCorrupted === "1" && item["corrupted"] !== "1") return true;
    else if (FILTER.gemCorrupted === "0" && item["corrupted"] !== "0") return true;
  
  } else if (FILTER.category === "currency") {
    if (item["frame"] === 3 && FILTER.sub === "all") {
      // Hide harbinger pieces under category 'all'
      return true;
    }
  } else if (item["frame"] === -1) {
    // Let enchants have a bit different quantity
    if (item["quantity"] < ENCH_QUANT_MED) return true;
  }

  // String search
  if (FILTER.search) {
    var nameBool = ("name" in item && item["name"].toLowerCase().indexOf(FILTER.search) !== -1);
    var typeBool = ("type" in item && item["type"].toLowerCase().indexOf(FILTER.search) !== -1);
    if (!nameBool && !typeBool) return true;
  }
}
