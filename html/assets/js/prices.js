/*
  There's not much here except for some poorly written JS functions. And since you're 
  already here, it can't hurt to take a look at http://youmightnotneedjquery.com/
*/

// Default item search filter options
var FILTER = {
  league: null,
  category: null,
  sub: "all",
  showLowConfidence: false,
  links: null,
  tier: null,
  search: null,
  gemLvl: null,
  gemQuality: null,
  gemCorrupted: null,
  parseAmount: 100
};

var ITEMS = {};
var LEAGUES = null;
var HISTORY_DATA = {};
var CHART_HISTORY = null;
var CHART_MEAN = null;
var CHART_QUANT = null;
var HISTORY_LEAGUE = null;
var INTERVAL;

var ROW_last_id = null;
var ROW_parent = null, ROW_expanded = null, ROW_filler = null;

// Re-used icon urls
const ICON_ENCHANTMENT = "https://web.poecdn.com/image/Art/2DItems/Currency/Enchantment.png?scale=1&w=1&h=1";
const ICON_EXALTED = "https://web.poecdn.com/image/Art/2DItems/Currency/CurrencyAddModToRare.png?scale=1&w=1&h=1";
const ICON_CHAOS = "https://web.poecdn.com/image/Art/2DItems/Currency/CurrencyRerollRare.png?scale=1&w=1&h=1";
const ICON_MISSING = "https://poe.watch/assets/img/missing.png";

var TEMPLATE_imgContainer = `
<span class='table-img-container text-center mr-1'><img src={{img}}></span>`;

$(document).ready(function() {
  if (!SERVICE_category) return;

  FILTER.league = SERVICE_leagues[0][0];
  FILTER.category = SERVICE_category;

  readLeagueFromCookies(FILTER);
  makeGetRequest(FILTER.league, FILTER.category);
  defineListeners();
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

function defineListeners() {
  // League
  $("#search-league").on("change", function(){
    FILTER.league = $("input[name=league]:checked", this).val();
    console.log("Selected league: " + FILTER.league);
    document.cookie = "league="+FILTER.league;
    makeGetRequest(FILTER.league, FILTER.category);
  });

  // Subcategory
  $("#search-sub").change(function(){
    FILTER.sub = $(this).find(":selected").val();
    console.log("Selected sub-category: " + FILTER.sub);
    sortResults(ITEMS);
  });

  // Load all button
  var loadall = $(".loadall");
  $(loadall, "button").on("click", function(){
    console.log("Button press: loadall");
    loadall.hide();
    FILTER.parseAmount = 0;
    sortResults(ITEMS);
  });

  // Searchbar
  $("#search-searchbar").on("input", function(){
    FILTER.search = $(this).val().toLowerCase().trim();
    console.log("Search: " + FILTER.search);
    sortResults(ITEMS);
  });

  // Low confidence
  $("#radio-confidence").on("change", function(){
    let option = $("input:checked", this).val() === "1";
    console.log("Show low count: " + option);
    FILTER.showLowConfidence = option;
    sortResults(ITEMS);
  });

  // Item links/sockets
  $("#radio-links").on("change", function(){
    FILTER.links = $("input[name=links]:checked", this).val();
    console.log("Link filter: " + FILTER.links);
    if (FILTER.links === "none") FILTER.links = null;
    sortResults(ITEMS);
  });

  // Map tier
  $("#select-tier").on("change", function(){
    FILTER.tier = $(":selected", this).val();
    console.log("Map tier filter: " + FILTER.tier);
    if (FILTER.tier === "all") FILTER.tier = null;
    else FILTER.tier = parseInt(FILTER.tier);
    sortResults(ITEMS);
  });

  // Gem level
  $("#select-level").on("change", function(){
    FILTER.gemLvl = $(":selected", this).val();
    console.log("Gem lvl filter: " + FILTER.gemLvl);
    if (FILTER.gemLvl === "none") FILTER.gemLvl = null;
    else FILTER.gemLvl = parseInt(FILTER.gemLvl);
    sortResults(ITEMS);
  });

  // Gem quality
  $("#select-quality").on("change", function(){
    FILTER.gemQuality = $(":selected", this).val();
    console.log("Gem quality filter: " + FILTER.gemQuality);
    if (FILTER.gemQuality === "all") FILTER.gemQuality = null;
    else FILTER.gemQuality = parseInt(FILTER.gemQuality);
    sortResults(ITEMS);
  });

  // Gem corrupted
  $("#radio-corrupted").on("change", function(){
    FILTER.gemCorrupted = $(":checked", this).val();
    console.log("Gem corruption filter: " + FILTER.gemCorrupted);
    if (FILTER.gemCorrupted === "all") FILTER.gemCorrupted = null;
    else FILTER.gemCorrupted = parseInt(FILTER.gemCorrupted);
    sortResults(ITEMS);
  });

  // Expand row
  $("#searchResults > tbody").delegate("tr", "click", function(event) {
    onRowClick(event);
  });

  // Live search toggle
  $("#live-updates").on("change", function(){
    let live = $("input[name=live]:checked", this).val() === "true";
    console.log("Live updates: " + live);
    document.cookie = "live="+live;

    if (live) {
      $("#progressbar-live").css("animation-name", "progressbar-live");
      INTERVAL = setInterval(timedRequestCallback, 60 * 1000);
    } else {
      $("#progressbar-live").css("animation-name", "");
      clearInterval(INTERVAL);
    }
  });
}

//------------------------------------------------------------------------------------------------------------
// Expanded row
//------------------------------------------------------------------------------------------------------------

function onRowClick(event) {
  let target = $(event.currentTarget);
  let id = parseInt(target.attr("value"));

  // If user clicked on a table that does not contain an id
  if (isNaN(id)) {
    return;
  }

  // Get rid of any filler rows
  if (ROW_filler) {
    $(".filler-row").remove();
    ROW_filler = null;
  }

  // User clicked on open parent-row
  if (target.is(ROW_parent)) {
    console.log("Closed open row");

    $(".parent-row").removeAttr("class");
    ROW_parent = null;

    $(".selected-row").remove();
    ROW_expanded = null;
    return;
  }

  // There's an open row somewhere
  if (ROW_parent !== null || ROW_expanded !== null) {
    $(".selected-row").remove();
    $(".parent-row").removeAttr("class");

    console.log("Closed row: " + ROW_last_id);

    ROW_parent = null;
    ROW_expanded = null;
  }

  console.log("Clicked on row id: " + id);

  // Define current row as parent target row
  target.addClass("parent-row");
  ROW_parent = target;
  ROW_last_id = id;

  // Load history data
  if (id in HISTORY_DATA) {
    console.log("History source: local");
    buildExpandedRow(id);
  } else {
    console.log("History source: remote");

    // Display a filler row
    displayFillerRow();

    makeHistoryRequest(id);
  }
}

function displayFillerRow() {
  let template = `
  <tr class='filler-row'><td colspan='100'>
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
              <td>Mean</td>
            </tr>
            <tr>
              <td>Median</td>
            </tr>
            <tr>
              <td>Mode</td>
            </tr>
          </tbody>
        </table>
      </div>
      <div class='col-md'>
        <table class="table table-sm details-table">
          <tbody>
            <tr>
              <td>Total amount listed</td>
            </tr>
            <tr>
              <td>Listed every 24h</td>
            </tr>
            <tr>
              <td>Price in exalted</td>
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
  </td></tr>
  `.trim();

  ROW_filler = $(template);
  ROW_parent.after(ROW_filler);
}

function makeHistoryRequest(id) {
  let request = $.ajax({
    url: "https://api.poe.watch/item.php",
    data: {id: id},
    type: "GET",
    async: true,
    dataTypes: "json"
  });

  request.done(function(payload) {
    // Get rid of any filler rows
    if (ROW_filler) {
      $(".filler-row").remove();
      ROW_filler = null;
    }
    
    let tmp = {};

    // Make league data accessible through league name
    for (let i = 0; i < payload.leagues.length; i++) {
      let leagueData = payload.leagues[i];
      tmp[leagueData.leagueName] = leagueData;
    }

    HISTORY_DATA[id] = tmp;

    buildExpandedRow(id);
  });
}

function formatHistory(leaguePayload) {
  let vals = [], keys = [];

  // Skip Hardcore (id 1) and Standard (id 2)
  if (leaguePayload.leagueId > 2) {
    // Because javascript is "special"
    let size = Object.keys(leaguePayload.history).length;

    // Convert date strings into dates
    let endDate = new Date(leaguePayload.leagueEnd);
    let startDate = new Date(leaguePayload.leagueStart);

    // Get difference in days between the two dates
    let timeDiff = Math.abs(endDate.getTime() - startDate.getTime());
    let dateDiff = Math.ceil(timeDiff / (1000 * 60 * 60 * 24));
    
    // Bloat if less entries than league duration
    for (let i = 0; i < dateDiff - size; i++) {
      vals.push(null);
      keys.push(null);
    }

    // Grab values
    for (var key in leaguePayload.history) {
      if (leaguePayload.history.hasOwnProperty(key)) {
        keys.push(formatDate(key));

        if (leaguePayload.history[key] === null) {
          vals.push(0);
        } else {
          vals.push(leaguePayload.history[key].mean);
        }
      }
    }
  } else {
    // Grab values
    for (var key in leaguePayload.history) {
      if (leaguePayload.history.hasOwnProperty(key)) {
        if (leaguePayload.history[key] === null) {
          keys.push(null);
          vals.push(null);
        } else {
          keys.push(formatDate(key));
          vals.push(leaguePayload.history[key].mean);
        }
      }
    }
  }

  // Return generated data
  return {
    'keys': keys,
    'vals': vals
  }
}

function formatWeek(leaguePayload) {
  // Because javascript is "special"
  let size = Object.keys(leaguePayload.history).length;
  let means = [], quants = [], count = 0;

  // If less than 7 entries, need to bloat
  for (let i = 0; i < 7 - size; i++) {
    means.push(null);
    quants.push(null);
  }

  // Grab latest 7 values
  for (var key in leaguePayload.history) {
    if (leaguePayload.history.hasOwnProperty(key)) {
      if (size - count++ <= 7) {
        if (leaguePayload.history[key] === null) {
          means.push(null);
          quants.push(null);
        } else {
          means.push(leaguePayload.history[key].mean);
          quants.push(leaguePayload.history[key].quantity);
        }
      }
    }
  }

  // Return generated data
  return {
    'keys':  [7, 6, 5, 4, 3, 2, 1],
    'means': means,
    'quants': quants
  }
}

function buildExpandedRow(id) {
  // Get list of past leagues available for the item
  let leagues = getItemHistoryLeagues(id);

  // Get league-specific data pack
  let selectedLeague = getSelectedLeague(leagues);
  let leaguePayload = HISTORY_DATA[id][selectedLeague];

  // Create jQuery object based on data from request and set gvar
  ROW_expanded = createExpandedRow(leaguePayload);
  placeCharts(ROW_expanded);
  fillChartData(leaguePayload);
  createHistoryRadio(ROW_expanded, leagues, selectedLeague);

  // Place jQuery object in table
  ROW_parent.after(ROW_expanded);

  // Create event listener for league selector
  createExpandedRowListener(id, ROW_expanded);
}

function placeCharts(expandedRow) {
  var priceData = {
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
            return "Price: " + data['datasets'][0]['data'][tooltipItem[0]['index']] + "c";
          },
          label: function(tooltipItem, data) {
            let day = data['labels'][tooltipItem['index']];
            return day === 1 ? '1 day ago' : day + ' days ago';
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
      // remove x-axes labels (but not grids)
      scales: {xAxes: [{ticks: {display: false}}]}
      }
    }
  
  var quantData = {
    type: "line",
    data: {
      labels: [],
      datasets: [{
        label: "Quantity",
        data: [],
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
            return data['labels'][tooltipItem['index']] + ' days ago';
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
      // remove x-axes labels (but not grids)
      scales: {xAxes: [{ticks: {display: false}}]}
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
            let price = data['datasets'][0]['data'][tooltipItem[0]['index']];
            return price ? price + "c" : "No data";
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
      },
      scales: {
        yAxes: [{ticks: {beginAtZero:true}}],
        xAxes: [{
          ticks: {
            //autoSkip: false,
            callback: function(value, index, values) {
              return (value ? value : '');
            }
          }
        }]
      }
    }
  }

  CHART_MEAN    = new Chart($("#chart-price",     expandedRow),  priceData);
  CHART_QUANT   = new Chart($("#chart-quantity",  expandedRow),  quantData);
  CHART_HISTORY = new Chart($("#chart-past",      expandedRow),   pastData);
}

function fillChartData(leaguePayload) {
  // Pad history with leading nulls
  let formattedHistory = formatHistory(leaguePayload);

  // Assign history chart datasets
  CHART_HISTORY.data.labels = formattedHistory.keys;
  CHART_HISTORY.data.datasets[0].data = formattedHistory.vals;
  CHART_HISTORY.update();

  // Get a fixed size of 7 latest history entries
  let formattedWeek = formatWeek(leaguePayload);

  CHART_MEAN.data.labels = formattedWeek.keys;
  CHART_MEAN.data.datasets[0].data = formattedWeek.means;
  CHART_MEAN.update();

  CHART_QUANT.data.labels = formattedWeek.keys;
  CHART_QUANT.data.datasets[0].data = formattedWeek.quants;
  CHART_QUANT.update();
}

function createHistoryRadio(expandedRow, leagues, selectedLeague) {
  let template = `
  <label class="btn btn-sm btn-outline-dark p-0 px-1 {{active}}">
    <input type="radio" name="league" value="{{value}}">{{name}}
  </label>
  `.trim();

  let buffer = "";
  for (let i = 0; i < leagues.length; i++) {
    buffer += template
      .replace("{{active}}",  (selectedLeague === leagues[i] ? "active" : ""))
      .replace("{{value}}",   leagues[i])
      .replace("{{name}}",    leagues[i]);
  }

  $("#history-league-radio", expandedRow).append(buffer);
}

function createExpandedRow(leaguePayload) {
  // Define the base template
  let template = `
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
              <td>Mean</td>
              <td>{{mean}}</td>
            </tr>
            <tr>
              <td>Median</td>
              <td>{{median}}</td>
            </tr>
            <tr>
              <td>Mode</td>
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
              <td>Listed every 24h</td>
              <td>{{1d}}</td>
            </tr>
            <tr>
              <td>Price in exalted</td>
              <td>{{exalted}}</td>
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
  </td></tr>
  `.trim();

  // Create base chaos icon container
  let chaosContainer   = TEMPLATE_imgContainer.trim().replace("{{img}}", ICON_CHAOS);
  let exaltedContainer = TEMPLATE_imgContainer.trim().replace("{{img}}", ICON_EXALTED);

  // Fill basic data
  template = template
    .replace("{{mean}}",    formatNum(leaguePayload.mean)    +  ' c')
    .replace("{{median}}",  formatNum(leaguePayload.median)  +  ' c')
    .replace("{{mode}}",    formatNum(leaguePayload.mode)    +  ' c')
    .replace("{{count}}",   formatNum(leaguePayload.count)          )
    .replace("{{1d}}",      formatNum(leaguePayload.quantity)       )
    .replace("{{exalted}}", formatNum(leaguePayload.exalted) + ' ex');
  
  // Convert into jQuery object and return
  return $(template);
}

function createExpandedRowListener(id, expandedRow) {
  $("#history-league-radio", expandedRow).change(function(){
    HISTORY_LEAGUE = $("input[name=league]:checked", this).val();

    // Get the payload associated with the selected league
    let leaguePayload = HISTORY_DATA[id][HISTORY_LEAGUE];
    fillChartData(leaguePayload);
  });
}

function getSelectedLeague(leagues) {
  // If user has not selected a league in the history menu before, use the first one
  if (!HISTORY_LEAGUE) HISTORY_LEAGUE = leagues[0];

  // If user had selected a league before in the history menu, check if that league
  // is present for this item. If yes, select it; if no, use the first one
  return leagues.indexOf(HISTORY_LEAGUE) > -1 ? HISTORY_LEAGUE : leagues[0];
}

function getItemHistoryLeagues(id) {
  // Get list of past leagues available for the item
  let leagues = [];

  for (var key in HISTORY_DATA[id]) {
    if (HISTORY_DATA[id].hasOwnProperty(key)) {
      leagues.push(key);
    }
  }

  return leagues;
}

//------------------------------------------------------------------------------------------------------------
// Requests
//------------------------------------------------------------------------------------------------------------

function makeGetRequest(league, category) {
  let request = $.ajax({
    url: "https://api.poe.watch/get.php",
    data: {
      league: league, 
      category: category
    },
    type: "GET",
    async: true,
    dataTypes: "json"
  });

  request.done(function(json) {
    console.log("Got " + json.length + " items from request");

    let items = parseRequest(json);
    sortResults(items);
    ITEMS = items;
    
    // Enable "show more" button
    if (json.length > FILTER.parseAmount) {
      let loadAllDiv = $(".loadall");
      $("button", loadAllDiv).text("Load more (" + (json.length - FILTER.parseAmount) + ")");
      loadAllDiv.show();
    }
  });
}

function parseRequest(json) {
  let items = new Map();

  // Loop though array, creating an associative array based on IDs
  for (let i = 0; i < json.length; i++) {
    const item = json[i];
    items['_' + item.id] = item;
  }

  return items;
}

function timedRequestCallback() {
  console.log("Automatic update");

  var request = $.ajax({
    url: "https://api.poe.watch/get.php",
    data: {
      league: FILTER.league, 
      category: FILTER.category
    },
    type: "GET",
    async: true,
    dataTypes: "json"
  });

  request.done(function(json) {
    console.log("Got " + json.length + " items from request");

    let items = parseRequest(json);
    sortResults(items);
    ITEMS = items;
  });
}

//------------------------------------------------------------------------------------------------------------
// Item parsing and table HTML generation
//------------------------------------------------------------------------------------------------------------

function parseItem(item) {
  // Format name and variant/links badge
  let nameField = buildNameField(item);

  // Format gem fields
  let gemFields = buildGemFields(item);
  
  // Format price and sparkline field
  let priceFields = buildPriceFields(item);

  // Format change field
  let changeField = buildChangeField(item);

  // Format count badge
  let quantField = buildQuantField(item);

  let template = `
    <tr value={{id}}>{{name}}{{gem}}{{price}}{{change}}{{quant}}</tr>
  `.trim();

  item.tableData = template
    .replace("{{id}}",      item.id)
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

  if (item.icon) {
    // Use SSL for icons for that sweet, sweet secure site badge
    item.icon = item.icon.replace("http://", "https://");
    template = template.replace("{{icon}}", item.icon);
  } else {
    template = template.replace("{{icon}}", ICON_MISSING);
  }

  if (item.frame === 9) {
    template = template.replace("{{foil}}", "class='item-foil'");
  } else {
    template = template.replace("{{foil}}", "");
  }

  if (FILTER.category === "enchantments") {
    if (item.var !== null) {
      let splitVar = item.var.split('-');
      for (var num in splitVar) {
        item.name = item.name.replace("#", splitVar[num]);
      }
    }

    template = template.replace("{{name}}", item.name);
    item.name = null;
  } else {
    template = template.replace("{{name}}", item.name);
  }

  if (item.type) {
    let tmp = "<span class='subtext-1'>, " + item.type + "</span>";;
    template = template.replace("{{type}}", tmp);
  } else {
    template = template.replace("{{type}}", "");
  }

  if (item.var && FILTER.category !== "enchantments") {
    let tmp = " <span class='badge custom-badge-gray'>" + item.var + "</span>";
    template = template.replace("{{var_or_tier}}", tmp);
  } else if (item.tier) {
    let tmp = " <span class='badge custom-badge-gray'>" + item.tier + "</span>";
    template = template.replace("{{var_or_tier}}", tmp);
  } else {
    template = template.replace("{{var_or_tier}}", "");
  }

  if (item.history.spark.length < 7) {
    template = template.replace("{{new}}", "<span class='badge badge-light'>New</span>");
  } else {
    template = template.replace("{{new}}", "");
  }
  
  return template;
}

function buildGemFields(item) {
  // Don't run if item is not a gem
  if (item.frame !== 4) return "";

  let template = `
  <td>{{lvl}}</td>
  <td>{{quality}}</td>
  <td><span class='badge custom-badge-{{color}}'>{{corr}}</span></td>
  `.trim();

  template = template.replace("{{lvl}}",      item.lvl);
  template = template.replace("{{quality}}",  item.quality);
  
  if (item.corrupted === 1) {
    template = template.replace("{{color}}",  "red");
    template = template.replace("{{corr}}",   "✓");
  } else {
    template = template.replace("{{color}}",  "green");
    template = template.replace("{{corr}}",   "✕");
  }

  return template;
}

function buildPriceFields(item) {
  let template = `
  <td>
    <div class='pricebox'>{{sparkline}}{{chaos_icon}}{{chaos_price}}</div>
  </td>
  <td>
    <div class='pricebox'>{{ex_icon}}{{ex_price}}</div>
  </td>
  `.trim();

  let chaosContainer  = TEMPLATE_imgContainer.trim().replace("{{img}}", ICON_CHAOS);
  let exContainer     = TEMPLATE_imgContainer.trim().replace("{{img}}", ICON_EXALTED);
  let sparkLine       = buildSparkLine(item);

  template = template.replace("{{sparkline}}",    sparkLine);
  template = template.replace("{{chaos_price}}",  roundPrice(item.mean));
  template = template.replace("{{chaos_icon}}",   chaosContainer);

  if (item.exalted >= 1) {
    template = template.replace("{{ex_icon}}",    exContainer);
    template = template.replace("{{ex_price}}",   roundPrice(item.exalted));
  } else {
    template = template.replace("{{ex_icon}}",    "");
    template = template.replace("{{ex_price}}",   "");
  }
  
  return template;
}

function buildSparkLine(item) {
  let svgColorClass = item.history.change > 0 ? "sparkline-green" : "sparkline-orange";
  let svg = document.createElement("svg");
  
  svg.setAttribute("class", "sparkline " + svgColorClass);
  svg.setAttribute("width", 60);
  svg.setAttribute("height", 25);
  svg.setAttribute("stroke-width", 3);

  sparkline(svg, item.history.spark);

  return svg.outerHTML;
}

function buildChangeField(item) {
  let template = `
  <td>
    <span class='badge custom-badge-block custom-badge-{{color}}'>
      {{percent}}%
    </span>
  </td>
  `.trim();

  let change = 0;

  if (item.history.change > 999) {
    change = 999;
  } else if (item.history.change < -999) {
    change = -999;
  } else {
    change = Math.round(item.history.change); 
  }

  if (change > 100) {
    template = template.replace("{{color}}", "green");
  } else if (change < -100) {
    template = template.replace("{{color}}", "orange");
  } else if (change > 50) {
    template = template.replace("{{color}}", "green-lo");
  } else if (change < -50) {
    template = template.replace("{{color}}", "orange-lo");
  } else {
    template = template.replace("{{color}}", "gray");
  }

  return template.replace("{{percent}}", change);
}

function buildQuantField(item) {
  let template = `
  <td>
    <span class='badge custom-badge-block custom-badge-{{color}}'>
      {{quant}}
    </span>
  </td>
  `.trim();

  if (item.quantity >= 10) {
    template = template.replace("{{color}}", "gray");
  } else if (item.quantity >= 5) {
    template = template.replace("{{color}}", "orange");
  } else {
    template = template.replace("{{color}}", "red");
  }

  return template.replace("{{quant}}", item.quantity);
}

//------------------------------------------------------------------------------------------------------------
// Utility functions
//------------------------------------------------------------------------------------------------------------

function formatNum(num) {
  const numberWithCommas = (x) => {
    var parts = x.toString().split(".");
    parts[0] = parts[0].replace(/\B(?=(\d{3})+(?!\d))/g, ",");
    return parts.join(".");
  }

  return numberWithCommas(num);
}

function roundPrice(price) {
  const numberWithCommas = (x) => {
    var parts = x.toString().split(".");
    parts[0] = parts[0].replace(/\B(?=(\d{3})+(?!\d))/g, ",");
    return parts.join(".");
  }

  return numberWithCommas(Math.round(price * 100) / 100);
}

function formatDate(date) {
  const MONTH_NAMES = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", 
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
  ];

  let s = new Date(date);
  return s.getDate() + " " + MONTH_NAMES[s.getMonth()];
}

function getAllDays(length) {
  const MONTH_NAMES = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", 
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
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
  return str.replace(/\w\S*/g, function(txt){
    return txt.charAt(0).toUpperCase() + txt.substr(1).toLowerCase();
  });
}

//------------------------------------------------------------------------------------------------------------
// Itetm sorting and searching
//------------------------------------------------------------------------------------------------------------

function sortResults(items) {
  // Empty the table
  let table = $("#searchResults");
  $("tbody", table).empty();

  let count = 0;
  let buffer = "";

  // Loop through every item provided
  for (var key in items) {
    if (items.hasOwnProperty(key)) {
      // Stop if specified item limit has been reached
      if ( count > FILTER.parseAmount ) {
        break;
      }

      // Skip parsing if item should be hidden according to filters
      if ( checkHideItem(items[key]) ) {
        continue;
      }

      // If item has not been parsed, parse it 
      if ( !('tableData' in items[key]) ) {
        parseItem(items[key]);
      }

      // Append generated table data to buffer
      buffer += items[key].tableData;
      count++;
    }
  }

  // Add the generated HTML table data to the table
  table.append(buffer);
}

function checkHideItem(item) {
  // Hide low confidence items
  if (!FILTER.showLowConfidence) {
    if (item.quantity < 5) return true;
  }

  // String search
  if (FILTER.search) {
    if (item.name && item.name.toLowerCase().indexOf(FILTER.search) === -1) return true;
    if (item.type && item.type.toLowerCase().indexOf(FILTER.search) === -1) return true;
  }

  // Hide sub-categories
  if (FILTER.sub !== "all" && FILTER.sub !== item.child) return true;

  // Hide items with different links
  if (item.links != FILTER.links) return true;

  // Sort gems, I guess
  if (FILTER.category === "gems") {
    if (FILTER.gemLvl !== null && item.lvl != FILTER.gemLvl) return true;
    if (FILTER.gemQuality !== null && item.quality != FILTER.gemQuality) return true;
    if (FILTER.gemCorrupted !== null && item.corrupted !== FILTER.gemCorrupted) return true;

  } else if (FILTER.category === "currency") {
    if (item.frame === 3 && FILTER.sub === "all") {
      // Hide harbinger pieces under category 'all'
      return true;
    }
  } else if (FILTER.category === "maps") {
    if (FILTER.tier != null && item.tier !== FILTER.tier) return true;
  }

  return false;
}
