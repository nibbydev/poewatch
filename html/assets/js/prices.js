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
var HISTORY_DATASET = 1;
var INTERVAL;

var ROW_last_id = null;
var ROW_parent = null, ROW_expanded = null, ROW_filler = null;

// Re-used icon urls
const ICON_ENCHANTMENT = "https://web.poecdn.com/image/Art/2DItems/Currency/Enchantment.png?scale=1&w=1&h=1";
const ICON_EXALTED = "https://web.poecdn.com/image/Art/2DItems/Currency/CurrencyAddModToRare.png?scale=1&w=1&h=1";
const ICON_CHAOS = "https://web.poecdn.com/image/Art/2DItems/Currency/CurrencyRerollRare.png?scale=1&w=1&h=1";
const ICON_MISSING = "https://poe.watch/assets/img/missing.png";

var TEMPLATE_imgContainer = "<span class='table-img-container text-center mr-1'><img src={{img}}></span>";

$(document).ready(function() {
  if (!SERVICE_category) return;

  FILTER.league = SERVICE_leagues[0][0];
  FILTER.category = SERVICE_category;

  readLeagueFromCookies(FILTER, SERVICE_leagues);
  makeGetRequest(FILTER.league, FILTER.category);
  defineListeners();
}); 

//------------------------------------------------------------------------------------------------------------
// Data prep
//------------------------------------------------------------------------------------------------------------

function readLeagueFromCookies(FILTER, leagues) {
  let league = getCookie("league");

  if (league) {
    console.log("Got league from cookie: " + league);

    // Check if league from cookie is still active
    for (let i = 0; i < leagues.length; i++) {
      const entry = leagues[i];
      
      if (league === entry[0]) {
        FILTER.league = league;
        // Point league dropdown to that league
        $("#search-league").val(league);
        return;
      }
    }

    console.log("League cookie did not match any active leagues");
  }
}

function defineListeners() {
  // League
  $("#search-league").on("change", function(){
    FILTER.league = $(":selected", this).val();
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
  $(".loadall button").on("click", function(){
    console.log("Button press: loadall");
    $(this).hide();
    FILTER.parseAmount = -1;
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
      <div class='col-sm'>
        <h4>League</h4>
        <select class="form-control form-control-sm small-selector mr-2" id="history-league-selector"></select>
      </div>
    </div>
    <hr>
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
        <h4>Past data</h4>
        <div class="btn-group btn-group-toggle mt-1 mb-3" data-toggle="buttons" id="history-dataset-radio">
          <label class="btn btn-sm btn-outline-dark p-0 px-1 active"><input type="radio" name="dataset" value=1>Mean</label>
          <label class="btn btn-sm btn-outline-dark p-0 px-1"><input type="radio" name="dataset" value=2>Median</label>
          <label class="btn btn-sm btn-outline-dark p-0 px-1"><input type="radio" name="dataset" value=3>Mode</label>
          <label class="btn btn-sm btn-outline-dark p-0 px-1"><input type="radio" name="dataset" value=4>Quantity</label>
        </div>
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
          switch (HISTORY_DATASET) {
            case 1: vals.push(leaguePayload.history[key].mean);     break;
            case 2: vals.push(leaguePayload.history[key].median);   break;
            case 3: vals.push(leaguePayload.history[key].mode);     break;
            case 4: vals.push(leaguePayload.history[key].quantity); break;
            default:                                                break;
          }
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

          switch (HISTORY_DATASET) {
            case 1: vals.push(leaguePayload.history[key].mean);     break;
            case 2: vals.push(leaguePayload.history[key].median);   break;
            case 3: vals.push(leaguePayload.history[key].mode);     break;
            case 4: vals.push(leaguePayload.history[key].quantity); break;
            default:                                                break;
          }
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

  // Add today's values
  means.push(leaguePayload.mean);
  quants.push(leaguePayload.quantity);

  // Return generated data
  return {
    'meanKeys':  ["7 days ago", "6 days ago", "5 days ago", "4 days ago", "3 days ago", "2 days ago", "1 day ago", "Right now"],
    'quantKeys':  ["7 days ago", "6 days ago", "5 days ago", "4 days ago", "3 days ago", "2 days ago", "1 day ago", "Last 24 hours"],
    'means': means,
    'quants': quants
  }
}

function buildExpandedRow(id) {
  // Get list of past leagues available for the item
  let leagues = getItemHistoryLeagues(id);

  // Get league-specific data pack
  let leaguePayload = HISTORY_DATA[id][FILTER.league];
  HISTORY_LEAGUE = FILTER.league;

  // Create jQuery object based on data from request and set gvar
  ROW_expanded = createExpandedRow();
  placeCharts(ROW_expanded);
  fillChartData(leaguePayload);
  createHistoryLeagueSelectorFields(ROW_expanded, leagues, FILTER.league);

  // Place jQuery object in table
  ROW_parent.after(ROW_expanded);

  // Create event listener for league selector
  createExpandedRowListeners(id, ROW_expanded);
}

function setDetailsTableValues(expandedRow, leaguePayload) {
  $("#details-table-mean",    expandedRow).html(  formatNum(leaguePayload.mean)      );
  $("#details-table-median",  expandedRow).html(  formatNum(leaguePayload.median)    );
  $("#details-table-mode",    expandedRow).html(  formatNum(leaguePayload.mode)      );
  $("#details-table-count",   expandedRow).html(  formatNum(leaguePayload.count)     );
  $("#details-table-1d",      expandedRow).html(  formatNum(leaguePayload.quantity)  );
  $("#details-table-exalted", expandedRow).html(  formatNum(leaguePayload.exalted)   );
}

function placeCharts(expandedRow) {
  let ctx = $("#chart-price", expandedRow)[0].getContext('2d');
  let gradient = ctx.createLinearGradient(0, 0, 1000, 0);

  gradient.addColorStop(0.0, 'rgba(247, 233, 152, 1)');
  gradient.addColorStop(0.3, 'rgba(244, 188, 172, 1)');
  gradient.addColorStop(0.7, 'rgba(244, 149, 179, 1)');

  let baseSettings = {
    type: "line",
    data: {
      labels: [],
      datasets: [{
        data: [],
        backgroundColor: "rgba(0, 0, 0, 0.2)",
        borderColor: gradient,
        borderWidth: 3,
        lineTension: 0.2,
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
        callbacks: {},
        backgroundColor: '#fff',
        titleFontSize: 16,
        titleFontColor: '#222',
        bodyFontColor: '#444',
        bodyFontSize: 14,
        displayColors: false,
        borderWidth: 1,
        borderColor: '#aaa'
      },
      scales: {}
    }
  }

  // Create deep clones of the base settings
  let priceSettings   = $.extend(true, {}, baseSettings);
  let quantSettings   = $.extend(true, {}, baseSettings);
  let historySettings = $.extend(true, {}, baseSettings);

  // Set price chart unique options
  priceSettings.options.scales.xAxes = [{ticks: {display: false}}];
  priceSettings.options.tooltips.callbacks = {
    title: function(tooltipItem, data) {
      return "Price: " + tooltipItem[0]['yLabel'];
    },
    label: function(tooltipItem, data) {
      return data['labels'][tooltipItem['index']];
    }
  };

  // Set quantity chart unique options
  quantSettings.options.scales.xAxes = [{ticks: {display: false}}];
  quantSettings.options.tooltips.callbacks = {
    title: function(tooltipItem, data) {
      return "Amount: " + tooltipItem[0]['yLabel'];
    },
    label: function(tooltipItem, data) {
      return data['labels'][tooltipItem['index']];
    }
  };

  // Set history chart unique options 
  historySettings.options.scales.yAxes = [{ticks: {beginAtZero:true}}];
  historySettings.options.scales.xAxes = [{
    ticks: {
      callback: function(value, index, values) {
        return (value ? value : '');
      }
    }
  }];
  historySettings.options.tooltips.callbacks = {
    title: function(tooltipItem, data) {
      let price = data['datasets'][0]['data'][tooltipItem[0]['index']];
      return price ? price : "No data";
    },
    label: function(tooltipItem, data) {
      return data['labels'][tooltipItem['index']];
    }
  };

  // Create charts
  CHART_MEAN    = new Chart($("#chart-price",    expandedRow), priceSettings);
  CHART_QUANT   = new Chart($("#chart-quantity", expandedRow), quantSettings);
  CHART_HISTORY = new Chart($("#chart-past",     expandedRow), historySettings);
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

  CHART_MEAN.data.labels = formattedWeek.meanKeys;
  CHART_MEAN.data.datasets[0].data = formattedWeek.means;
  CHART_MEAN.update();

  CHART_QUANT.data.labels = formattedWeek.quantKeys;
  CHART_QUANT.data.datasets[0].data = formattedWeek.quants;
  CHART_QUANT.update();
  
  // Set data in details table
  setDetailsTableValues(ROW_expanded, leaguePayload);
}

function createHistoryLeagueSelectorFields(expandedRow, leagues, selectedLeague) {
  let buffer = "";

  for (let i = 0; i < leagues.length; i++) {
    buffer += "<option value='{{value}}' {{selected}}>{{name}}</option>"
      .replace("{{selected}}",  (selectedLeague === leagues[i].name ? "selected" : ""))
      .replace("{{value}}",     leagues[i].name)
      .replace("{{name}}",      leagues[i].display);
  }

  $("#history-league-selector", expandedRow).append(buffer);
}

function createExpandedRow() {
  // Define the base template
  let template = `
  <tr class='selected-row'><td colspan='100'>
    <div class='row m-1'>
      <div class='col-sm'>
        <h4>League</h4>
        <select class="form-control form-control-sm small-selector mr-2" id="history-league-selector"></select>
      </div>
    </div>
    <hr>
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
              <td>{{chaosContainter}}<span id='details-table-mean'></span></td>
            </tr>
            <tr>
              <td>Median</td>
              <td>{{chaosContainter}}<span id='details-table-median'></span></td>
            </tr>
            <tr>
              <td>Mode</td>
              <td>{{chaosContainter}}<span id='details-table-mode'></span></td>
            </tr>
          </tbody>
        </table>
      </div>
      <div class='col-md'>
        <table class="table table-sm details-table">
          <tbody>
            <tr>
              <td>Total amount listed</td>
              <td><span id='details-table-count'></span></td>
            </tr>
            <tr>
              <td>Listed every 24h</td>
              <td><span id='details-table-1d'></span></td>
            </tr>
            <tr>
              <td>Price in exalted</td>
              <td>{{exaltedContainter}}<span id='details-table-exalted'></span></td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
    <hr>
    <div class='row m-1 mb-3'>
      <div class='col-sm'>
        <h4>Past data</h4>
        <div class="btn-group btn-group-toggle mt-1 mb-3" data-toggle="buttons" id="history-dataset-radio">
          <label class="btn btn-sm btn-outline-dark p-0 px-1 active"><input type="radio" name="dataset" value=1>Mean</label>
          <label class="btn btn-sm btn-outline-dark p-0 px-1"><input type="radio" name="dataset" value=2>Median</label>
          <label class="btn btn-sm btn-outline-dark p-0 px-1"><input type="radio" name="dataset" value=3>Mode</label>
          <label class="btn btn-sm btn-outline-dark p-0 px-1"><input type="radio" name="dataset" value=4>Quantity</label>
        </div>
        <div class='chart-large'><canvas id="chart-past"></canvas></div>
      </div>
    </div>
  </td></tr>
  `.trim();

  let containterTemplate = "<span class='table-img-container-sm text-center mr-1'><img src={{img}}></span>";
  let chaosContainer = containterTemplate.replace("{{img}}", ICON_CHAOS);
  let exaltedContainer = containterTemplate.replace("{{img}}", ICON_EXALTED);

  // :thinking:
  template = template
    .replace("{{chaosContainter}}",   chaosContainer)
    .replace("{{chaosContainter}}",   chaosContainer)
    .replace("{{chaosContainter}}",   chaosContainer)
    .replace("{{exaltedContainter}}", exaltedContainer);
  
  // Convert into jQuery object and return
  return $(template);
}

function createExpandedRowListeners(id, expandedRow) {
  $("#history-league-selector", expandedRow).change(function(){
    HISTORY_LEAGUE = $(":selected", this).val();

    // Get the payload associated with the selected league
    let leaguePayload = HISTORY_DATA[id][HISTORY_LEAGUE];
    fillChartData(leaguePayload);
  });

  $("#history-dataset-radio", expandedRow).change(function(){
    HISTORY_DATASET = parseInt($("input[name=dataset]:checked", this).val());

    // Get the payload associated with the selected league
    let leaguePayload = HISTORY_DATA[id][HISTORY_LEAGUE];
    fillChartData(leaguePayload);
  });
}

function getItemHistoryLeagues(id) {
  // Get list of past leagues available for the item
  let leagues = [];

  for (var key in HISTORY_DATA[id]) {
    if (HISTORY_DATA[id].hasOwnProperty(key)) {
      leagues.push({
        name: key,
        display: HISTORY_DATA[id][key].leagueDisplay
      });
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
    <div class='namebox'>
      <span class='table-img-container text-center mr-1'><img src='{{icon}}'></span>
      <span {{foil}}>{{name}}{{type}}</span>{{var_or_tier}}
    </div>
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
    let tmp = " <span class='badge custom-badge-gray ml-1'>" + item.var + "</span>";
    template = template.replace("{{var_or_tier}}", tmp);
  } else if (item.tier) {
    let tmp = " <span class='badge custom-badge-gray ml-1'>" + item.tier + "</span>";
    template = template.replace("{{var_or_tier}}", tmp);
  } else {
    template = template.replace("{{var_or_tier}}", "");
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
      if ( FILTER.parseAmount > 0 && count > FILTER.parseAmount ) {
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
