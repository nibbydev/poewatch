/*
  There's not much here except for some poorly written JS functions. And since you're 
  already here, it can't hurt to take a look at http://youmightnotneedjquery.com/
*/

var LEAGUE = parseQueryParam('league');
var ITEM = {};
var CHART_HISTORY = null;
var HISTORY_DATASET = 1;

$(document).ready(function() {
  parseQueryParams();
  if (LEAGUE) makeHistoryRequest();
}); 

//------------------------------------------------------------------------------------------------------------
// Expanded row
//------------------------------------------------------------------------------------------------------------

function parseQueryParams() {
  let tmp;
  if (tmp = parseQueryParam('dataset')) {
    if      (tmp ===   'mean') HISTORY_DATASET = 1;
    else if (tmp === 'median') HISTORY_DATASET = 2;
    else if (tmp ===   'mode') HISTORY_DATASET = 3;
    else if (tmp ===  'daily') HISTORY_DATASET = 4;
  }
}

function makeHistoryRequest() {
  let id = parseInt(parseQueryParam('id'));

  let request = $.ajax({
    url: "https://api.poe.watch/item.php",
    data: {id: id},
    type: "GET",
    async: true,
    dataTypes: "json"
  });

  request.done(function(payload) {
    $(".card-header.slim-card-edge > div.content").parent().addClass("p-0");

    let leagues = [];
    for (let i = 0; i < payload.data.length; i++) {
      leagues.push({
        name:    payload.data[i].league.name,
        display: payload.data[i].league.display,
        active:  payload.data[i].league.active
      });
    }

    ITEM = payload;

    createCharts();
    fillData();
    createSelectorFields(leagues);
    createListeners(id);

    $(".buffering").addClass("d-none");
    $(".content").removeClass("d-none");
  });
}

function createCharts() {
  var dataPlugin = {
    beforeUpdate: function(chart) {
      // Don't run if data has not yet been initialized
      if (chart.data.data.length < 1) return;

      var keys = chart.data.data.keys;
      var vals = chart.data.data.vals;

      chart.data.labels = keys;

      switch (HISTORY_DATASET) {
        case 1: chart.data.datasets[0].data = vals.mean;   break;
        case 2: chart.data.datasets[0].data = vals.median; break;
        case 3: chart.data.datasets[0].data = vals.mode;   break;
        case 4: chart.data.datasets[0].data = vals.daily;  break;
      }
    }
  };

  var gradientLinePlugin = {
    beforeDatasetUpdate: function(chart) {
      if (!chart.width) return;

      // Create the linear gradient  chart.scales['x-axis-0'].width
      var gradient = chart.ctx.createLinearGradient(0, 0, 0, 250);

      gradient.addColorStop(0.0, 'rgba(247, 233, 152, 1)');
      gradient.addColorStop(1.0, 'rgba(244, 149, 179, 1)');

      // Assign the gradient to the dataset's border color.
      chart.data.datasets[0].borderColor = gradient;
    }
  };

  let settings = {
    plugins: [dataPlugin, gradientLinePlugin],
    type: "line",
    data: {
      data: [],
      labels: [],
      datasets: [{
        data: [],
        backgroundColor: "rgba(0, 0, 0, 0.2)",
        borderColor: "rgba(255, 255, 255, 0.5)",
        borderWidth: 3,
        lineTension: 0.4,
        pointRadius: 0
      }]
    },
    options: {
      title: {display: false},
      layout: {padding: 0},
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
            let price = data['datasets'][0]['data'][tooltipItem[0]['index']];
            return price ? price : "No data";
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
        yAxes: [{
          ticks: {
            beginAtZero: true,
            padding: 0
          }
        }],
        xAxes: [{
          ticks: {
            callback: function(value, index, values) {
              return (value ? value : '');
            },
            maxRotation: 0,
            padding: 0
          }
        }]
      }
    }
  }

  CHART_HISTORY = new Chart($("#chart-past"), settings);
}

function dynamicColor(multi) {
  if (multi === null) {
    return "rgb(255, 0, 0)";
  }

  let roof = 10, r, g;

  let half = roof * 50 / 100;
  let localPerc = multi / half * 100;

  if (multi / roof * 100 < 50) {
    r = 255;
    g = 255 * localPerc / 100;
  } else {
    g = 255;
    r = 255 - (255 * localPerc / 100 - 255);
  }

  r = r | 0;
  g = g | 0;

  if (r > 255) r = 255;
  if (g > 255) g = 255;
  if (r < 0) r = 0;
  if (g < 0) g = 0;

  return "rgb("+ r +", "+ g +", 0)";
}

function fillData() {
  let leaguePayload;
  for (let i = 0; i < ITEM.data.length; i++) {
    if (ITEM.data[i].league.name === LEAGUE) {
      leaguePayload = ITEM.data[i];
      break;
    }
  }

  // Pad history with leading nulls
  let formattedHistory = formatHistory(leaguePayload);

  // Assign history chart datasets
  CHART_HISTORY.data.data = formattedHistory;
  CHART_HISTORY.update();

  // Set data in details table
  $("#details-table-mean")    .html(  formatNum(leaguePayload.mean)   );
  $("#details-table-median")  .html(  formatNum(leaguePayload.median) );
  $("#details-table-mode")    .html(  formatNum(leaguePayload.mode)   );
  $("#details-table-total")   .html(  formatNum(leaguePayload.total)  );
  $("#details-table-1d")      .html(  formatNum(leaguePayload.daily)  );
  $("#details-table-exalted") .html(  formatNum(leaguePayload.exalted));

  fixIcon(ITEM);

  $("#item-icon").attr('src',  ITEM.icon);
  $("#item-name").html( buildNameField(ITEM.name) );

  $("#item-chaos").html(formatNum(leaguePayload.mean));
  $("#item-exalt").html(formatNum(leaguePayload.exalted));
}

function createSelectorFields(leagues) {
  let buffer = "";

  for (let i = 0; i < leagues.length; i++) {
    let display;

    if (leagues[i].display) {
      display = leagues[i].active ? leagues[i].display : "● " + leagues[i].display;
    } else {
      display = leagues[i].active ? leagues[i].name : "● " + leagues[i].name;
    }

    buffer += "<option value='{{value}}' {{selected}}>{{name}}</option>"
      .replace("{{selected}}",  (LEAGUE === leagues[i].name ? "selected" : ""))
      .replace("{{value}}",     leagues[i].name)
      .replace("{{name}}",      display);
  }

  $("#history-league-selector").append(buffer);
}

function createListeners() {
  $("#history-league-selector").change(function(){
    LEAGUE = $(":selected", this).val();
    updateQueryParam('league', LEAGUE);
    fillData();
  });

  $("#history-dataset-radio").change(function(){
    let val = $("input[name=dataset]:checked", this).val();
    updateQueryParam('dataset', val);

    if      (val ===   'mean') HISTORY_DATASET = 1;
    else if (val === 'median') HISTORY_DATASET = 2;
    else if (val ===   'mode') HISTORY_DATASET = 3;
    else if (val ===  'daily') HISTORY_DATASET = 4;

    fillData();
  });
}

//------------------------------------------------------------------------------------------------------------
// Item parsing and table HTML generation
//------------------------------------------------------------------------------------------------------------

function buildNameField() {
  // Fix name if item is enchantment
  if (ITEM.category === "enchantment" && ITEM.variation !== null) {
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
  } else if (ITEM.variation === "shaper") {
    builder = "<span class='item-shaper'>" + builder + "</span>";
  } else if (ITEM.variation === "elder") {
    builder = "<span class='item-elder'>" + builder + "</span>";
  }

  if (ITEM.variation && ITEM.category !== "enchantment") { 
    builder += " <span class='badge custom-badge-gray ml-1'>" + ITEM.variation + "</span>";
  } 
  
  if (ITEM.tier) {
    builder += " <span class='badge custom-badge-gray ml-1'>Tier " + ITEM.tier + "</span>";
  } 

  if (ITEM.ilvl) {
    builder += " <span class='badge custom-badge-gray ml-1'>iLvl " + ITEM.ilvl + "</span>";
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

function fixIcon(ITEM) {
  ITEM.icon = ITEM.icon.replace("http://", "https://");

  if (ITEM.variation === "shaper") {
    ITEM.icon += "&shaper=1";
  } else if (ITEM.variation === "elder") {
    ITEM.icon += "&elder=1";
  }

  // Flaks have no params
  if (!ITEM.icon.includes("?")) {
    return;
  }

  let splitIcon = ITEM.icon.split("?");
  let splitParams = splitIcon[1].split("&");
  let newParams = "";

  for (let i = 0; i < splitParams.length; i++) {
    switch (splitParams[i].split("=")[0]) {
      case "scale": 
        break;
      default:
        newParams += "&" + splitParams[i];
        break;
    }
  }

  if (newParams) {
    ITEM.icon = splitIcon[0] + "?" + newParams.substr(1);
  } else {
    ITEM.icon = splitIcon[0];
  }
}

//------------------------------------------------------------------------------------------------------------
// Utility functions
//------------------------------------------------------------------------------------------------------------

function convertDateToUTC(date) {
  return date ? new Date(
    date.getUTCFullYear(), 
    date.getUTCMonth(), 
    date.getUTCDate(), 
    date.getUTCHours(), 
    date.getUTCMinutes(), 
    date.getUTCSeconds()) : null;
}

Date.prototype.addDays = function(days) {
  var date = new Date(this.valueOf());
  date.setDate(date.getDate() + days);
  return date;
}

function formatHistory(leaguePayload) {
  let keys = [];
  let vals = {
    mean:   [],
    median: [],
    mode:   [],
    daily:  []
  };

  const msInDay = 86400000;
  let firstDate = null, lastDate = null;
  let totalDays = null, elapDays = null;
  let startDate = null, endDate  = null;
  let daysMissingStart = 0, daysMissingEnd = 0;
  let startEmptyPadding = 0;

  // If there are any history entries for this league, find the first and last date
  if (leaguePayload.history.length) {
    firstDate = new Date(leaguePayload.history[0].time);
    lastDate = new Date(leaguePayload.history[leaguePayload.history.length - 1].time);
  }

  // League should always have a start date
  if (leaguePayload.league.start) {
    startDate = new Date(leaguePayload.league.start);
  }

  // Permanent leagues don't have an end date
  if (leaguePayload.league.end) {
    endDate = new Date(leaguePayload.league.end);
  }

  // Find duration for non-permanent leagues
  if (startDate && endDate) {
    let diff = Math.abs(endDate.getTime() - startDate.getTime());
    totalDays = Math.floor(diff / msInDay);
    
    if (leaguePayload.league.active) {
      let diff = Math.abs(new Date().getTime() - startDate.getTime());
      elapDays = Math.floor(diff / msInDay);
    } else {
      elapDays = totalDays;
    }
  }

  // Find how many days worth of data is missing from the league start
  if (leaguePayload.league.id > 2) {
    if (firstDate && startDate) {
      let diff = Math.abs(firstDate.getTime() - startDate.getTime());
      daysMissingStart = Math.floor(diff / msInDay);
    }
  } 

  // Find how many days worth of data is missing from the league end, if league has ended
  if (leaguePayload.league.active) {
    // League is active, compare time of last entry to right now
    if (lastDate) {
      let diff = Math.abs(new Date().getTime() - lastDate.getTime());
      daysMissingEnd = Math.floor(diff / msInDay);
    }
  } else {
    // League has ended, compare time of last entry to time of league end
    if (lastDate && endDate) {
      let diff = Math.abs(lastDate.getTime() - endDate.getTime());
      daysMissingEnd = Math.floor(diff / msInDay);
    }
  }

  // Find number of ticks the graph should be padded with empty entries on the left
  if (leaguePayload.league.id > 2) {
    if (totalDays !== null && elapDays !== null) {
      startEmptyPadding = totalDays - elapDays;
    }
  } else {
    startEmptyPadding = 120 - leaguePayload.history.length;
  }


  // Right, now that we have all the dates, durations and counts we can start 
  // building the actual payload


  // Bloat using 'null's the amount of days that should not have a tooltip
  for (let i = 0; i < startEmptyPadding; i++) {
    vals.mean.push(null);
    vals.median.push(null);
    vals.mode.push(null);
    vals.daily.push(null);
    keys.push(null);
  }

  // If entries are missing before the first entry, fill with "No data"
  if (daysMissingStart) {
    let date = new Date(startDate);

    for (let i = 0; i < daysMissingStart; i++) {
      vals.mean.push(0);
      vals.median.push(0);
      vals.mode.push(0);
      vals.daily.push(0);
      keys.push(formatDate(date.addDays(i)));
    }
  }

  // Add actual history data
  for (let i = 0; i < leaguePayload.history.length; i++) {
    const entry = leaguePayload.history[i];

    // Add current entry values
    vals.mean.push(Math.round(entry.mean * 100) / 100);
    vals.median.push(Math.round(entry.median * 100) / 100);
    vals.mode.push(Math.round(entry.mode * 100) / 100);
    vals.daily.push(entry.daily);
    keys.push(formatDate(entry.time));

    // Check if there are any missing entries between the current one and the next one
    if (i + 1 < leaguePayload.history.length) {
      const nextEntry = leaguePayload.history[i + 1];

      // Get dates
      let currentDate = new Date(entry.time);
      let nextDate = new Date(nextEntry.time);

      // Get difference in days between entries
      let timeDiff = Math.abs(nextDate.getTime() - currentDate.getTime());
      let diffDays = Math.floor(timeDiff / (1000 * 3600 * 24)) - 1; 

      // Fill missing days with "No data" (if any)
      for (let i = 0; i < diffDays; i++) {
        vals.mean.push(0);
        vals.median.push(0);
        vals.mode.push(0);
        vals.daily.push(0);
        keys.push(formatDate(currentDate.addDays(i + 1)));
      }
    }
  }

  // If entries are missing after the first entry, fill with "No data"
  if (daysMissingEnd && lastDate) {
    let date = new Date(lastDate);
    date.setDate(date.getDate() + 1);

    for (let i = 0; i < daysMissingEnd; i++) {
      vals.mean.push(0);
      vals.median.push(0);
      vals.mode.push(0);
      vals.daily.push(0);
      keys.push(formatDate(date.addDays(i)));
    }
  }

  // Add current values
  if (leaguePayload.league.active) {
    vals.mean.push(Math.round(leaguePayload.mean * 100) / 100);
    vals.median.push(Math.round(leaguePayload.median * 100) / 100);
    vals.mode.push(Math.round(leaguePayload.mode * 100) / 100);
    vals.daily.push(leaguePayload.daily);
    keys.push("Now");
  }

  // Return generated data
  return {
    'keys': keys,
    'vals': vals
  }
}

function formatNum(num) {
  const numberWithCommas = (x) => {
    var parts = x.toString().split(".");
    parts[0] = parts[0].replace(/\B(?=(\d{3})+(?!\d))/g, ",");
    return parts.join(".");
  }

  if (num === null) {
    return 'Unavailable';
  } else return numberWithCommas(Math.round(num * 100) / 100);
}

function formatDate(date) {
  const MONTH_NAMES = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", 
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
  ];

  let s = convertDateToUTC(new Date(date));
  return s.getDate() + " " + MONTH_NAMES[s.getMonth()];
}

function updateQueryParam(key, value) {
  switch (key) {
    case "confidence": value = value === false  ? null : value;   break;
    case "search":     value = value === ""     ? null : value;   break;
    case "rarity":     value = value === "all"  ? null : value;   break;
    case "corrupted":  value = value === "all"  ? null : value;   break;
    case "quality":    value = value === "all"  ? null : value;   break;
    case "lvl":        value = value === "all"  ? null : value;   break;
    case "links":      value = value === "none" ? null : value;   break;
    case "group":      value = value === "all"  ? null : value;   break;
    case "tier":       value = value === "all"  ? null : value;   break;
    default:           break;
  }

  var url = document.location.href;
  var re = new RegExp("([?&])" + key + "=.*?(&|#|$)(.*)", "gi");
  var hash;

  if (re.test(url)) {
    if (typeof value !== 'undefined' && value !== null) {
      url = url.replace(re, '$1' + key + "=" + value + '$2$3');
    } else {
      hash = url.split('#');
      url = hash[0].replace(re, '$1$3').replace(/(&|\?)$/, '');
      
      if (typeof hash[1] !== 'undefined' && hash[1] !== null) {
        url += '#' + hash[1];
      }
    }
  } else if (typeof value !== 'undefined' && value !== null) {
    var separator = url.indexOf('?') !== -1 ? '&' : '?';
    hash = url.split('#');
    url = hash[0] + separator + key + '=' + value;

    if (typeof hash[1] !== 'undefined' && hash[1] !== null) {
      url += '#' + hash[1];
    }
  }

  history.replaceState({}, "foo", url);
}

function parseQueryParam(key) {
  let url = window.location.href;
  key = key.replace(/[\[\]]/g, '\\$&');
  
  var regex = new RegExp('[?&]' + key + '(=([^&#]*)|&|#|$)'),
      results = regex.exec(url);
      
  if (!results   ) return null;
  if (!results[2]) return   '';

  return decodeURIComponent(results[2].replace(/\+/g, ' '));
}
