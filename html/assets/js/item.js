/*
  There's not much here except for some poorly written JS functions. And since you're 
  already here, it can't hurt to take a look at http://youmightnotneedjquery.com/
*/

var LEAGUES = null;
var ITEM = {};
var CHART_HISTORY = null;
var CHART_MEAN = null;
var CHART_QUANT = null;
var HISTORY_LEAGUE = null;
var HISTORY_DATASET = 1;

// Re-used icon urls
const ICON_ENCHANTMENT = "https://web.poecdn.com/image/Art/2DItems/Currency/Enchantment.png?scale=1&w=1&h=1";
const ICON_EXALTED = "https://web.poecdn.com/image/Art/2DItems/Currency/CurrencyAddModToRare.png?scale=1&w=1&h=1";
const ICON_CHAOS = "https://web.poecdn.com/image/Art/2DItems/Currency/CurrencyRerollRare.png?scale=1&w=1&h=1";
const ICON_MISSING = "https://poe.watch/assets/img/missing.png";

var TEMPLATE_imgContainer = "<span class='img-container img-container-sm text-center mr-1'><img src={{img}}></span>";

$(document).ready(function() {
  if (!ID || !LEAGUE) return;
  makeHistoryRequest(ID);
}); 

//------------------------------------------------------------------------------------------------------------
// Expanded row
//------------------------------------------------------------------------------------------------------------


function makeHistoryRequest(id) {
  let request = $.ajax({
    url: "https://api.poe.watch/item.php",
    data: {id: id},
    type: "GET",
    async: true,
    dataTypes: "json"
  });

  request.done(function(payload) {
    // Create deep clone of the payload
    let tmp = $.extend(true, {}, payload);

    // Make league data accessible through league name
    tmp.leagues = {};
    for (let i = 0; i < payload.leagues.length; i++) {
      let leagueData = payload.leagues[i];
      tmp.leagues[leagueData.leagueName] = leagueData;
    }

    ITEM = tmp;
    buildExpandedRow(id);
  });
}

function buildExpandedRow(id) {
  // Get list of past leagues available for the item
  let leagues = getItemHistoryLeagues(id);

  createCharts();
  fillData();
  createHistoryLeagueSelectorFields(leagues);

  // Create event listener for league selector
  createExpandedRowListeners(id);
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

function createCharts() {
  let ctx = $("#chart-price")[0].getContext('2d');
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
  CHART_MEAN    = new Chart($("#chart-price"),    priceSettings);
  CHART_QUANT   = new Chart($("#chart-quantity"), quantSettings);
  CHART_HISTORY = new Chart($("#chart-past"),     historySettings);
}

function fillData() {
  let leaguePayload = ITEM.leagues[LEAGUE];

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
  $("#details-table-mean")    .html(  formatNum(leaguePayload.mean)      );
  $("#details-table-median")  .html(  formatNum(leaguePayload.median)    );
  $("#details-table-mode")    .html(  formatNum(leaguePayload.mode)      );
  $("#details-table-count")   .html(  formatNum(leaguePayload.count)     );
  $("#details-table-1d")      .html(  formatNum(leaguePayload.quantity)  );
  $("#details-table-exalted") .html(  formatNum(leaguePayload.exalted)   );
  
  $("#item-icon").attr('src', ITEM.icon.replace('http://', 'https://'));
  $("#item-name").html( buildNameField(ITEM.name) );

  $("#item-chaos").html(formatNum(leaguePayload.mean));
  $("#item-exalt").html(formatNum(leaguePayload.exalted));
}

function createHistoryLeagueSelectorFields(leagues) {
  let buffer = "";

  for (let i = 0; i < leagues.length; i++) {
    buffer += "<option value='{{value}}' {{selected}}>{{name}}</option>"
      .replace("{{selected}}",  (LEAGUE === leagues[i].name ? "selected" : ""))
      .replace("{{value}}",     leagues[i].name)
      .replace("{{name}}",      leagues[i].display);
  }

  $("#history-league-selector").append(buffer);
}

function createExpandedRowListeners() {
  $("#history-league-selector").change(function(){
    LEAGUE = $(":selected", this).val();

    // Get the payload associated with the selected league
    fillData( ITEM.leagues[LEAGUE] );
  });

  $("#history-dataset-radio").change(function(){
    HISTORY_DATASET = parseInt($("input[name=dataset]:checked", this).val());

    // Get the payload associated with the selected league
    fillData( ITEM.leagues[LEAGUE] );
  });
}

function getItemHistoryLeagues() {
  // Get list of past leagues available for the item
  let leagues = [];

  for (var key in ITEM.leagues) {
    if (ITEM.leagues.hasOwnProperty(key)) {
      leagues.push({
        name: key,
        display: ITEM.leagues[key].leagueDisplay
      });
    }
  }

  return leagues;
}

//------------------------------------------------------------------------------------------------------------
// Item parsing and table HTML generation
//------------------------------------------------------------------------------------------------------------

function buildNameField() {
  // Fix icon
  if (ITEM.icon) {
    ITEM.icon = ITEM.icon.replace("http://", "https://");
  } else {
    ITEM.icon = ICON_MISSING;
  }

  // Fix name if item is enchantment
  if (ITEM.categoryParent === "enchantments" && ITEM.variation !== null) {
    let splitVar = ITEM.variation.split('-');
    
    for (var num in splitVar) {
      ITEM.name = ITEM.name.replace("#", splitVar[num]);
    }
  }

  // Begin builder
  let builder = ITEM.name;

  if (ITEM.type) {
    builder += "<span class='subtext-1'>, " + ITEM.type + "</span>";;
  }

  if (ITEM.frame === 9) {
    builder = "<span class='item-foil'>" + builder + "</span>";
  }

  if (ITEM.variation && ITEM.categoryParent !== "enchantments") {
    builder += " <span class='badge custom-badge-gray ml-1'>" + ITEM.variation + "</span>";
  } 
  
  if (ITEM.tier) {
    builder += " <span class='badge custom-badge-gray ml-1'>Tier " + ITEM.tier + "</span>";
  } 
  
  if (ITEM.links) {
    builder += " <span class='badge custom-badge-gray ml-1'>" + ITEM.links + " Link</span>";
  }

  if (ITEM.frame === 4) {
    builder += "<span class='badge custom-badge-gray ml-1'>Lvl " + ITEM.lvl + "</span>";
    builder += "<span class='badge custom-badge-gray ml-1'>Quality " + ITEM.quality + "</span>";

    if (ITEM.corrupted) {
      builder += "<span class='badge custom-badge-red ml-1'>Corrupted</span>";
    }
  }

  return builder;
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

  return numberWithCommas(Math.round(num * 100) / 100);
}


function formatDate(date) {
  const MONTH_NAMES = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", 
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
  ];

  let s = new Date(date);
  return s.getDate() + " " + MONTH_NAMES[s.getMonth()];
}
