/*
  There's not much here except for some poorly written JS functions. And since you're 
  already here, it can't hurt to take a look at http://youmightnotneedjquery.com/
*/

var ITEM = {};
var CHART_HISTORY = null;
var HISTORY_DATASET = 1;

$(document).ready(function() {
  if (ID && LEAGUE) makeHistoryRequest(ID);
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
    $(".card-header.slim-card-edge > div.content").parent().addClass("p-0");

    // Create deep clone of the payload
    let tmp = $.extend(true, {}, payload);
    let leagues = [];

    // Make league data accessible through league name
    tmp.leagues = {};
    for (let i = 0; i < payload.leagues.length; i++) {
      let leagueData = payload.leagues[i];
      tmp.leagues[leagueData.leagueName] = leagueData;
      
      leagues.push({
        name: leagueData.leagueName,
        display: leagueData.leagueDisplay,
        active: leagueData.leagueActive
      });
    }
    ITEM = tmp;

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
        case 1: chart.data.datasets[0].data = vals.mean;      break;
        case 2: chart.data.datasets[0].data = vals.median;    break;
        case 3: chart.data.datasets[0].data = vals.mode;      break;
        case 4: chart.data.datasets[0].data = vals.quantity;  break;
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

      /*
      for (let i = 0; i < chart.data.data.keys.length; i++) {
        let dynColor = dynamicColor(chart.data.data.vals.quantity[i]);
        gradient.addColorStop(1.0 / chart.data.data.keys.length * i, dynColor);
      }
      */

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
  let leaguePayload = ITEM.leagues[LEAGUE];

  // Pad history with leading nulls
  let formattedHistory = formatHistory(leaguePayload);

  // Assign history chart datasets
  CHART_HISTORY.data.data = formattedHistory;
  CHART_HISTORY.update();

  // Set data in details table
  $("#details-table-mean")    .html(  formatNum(leaguePayload.mean)      );
  $("#details-table-median")  .html(  formatNum(leaguePayload.median)    );
  $("#details-table-mode")    .html(  formatNum(leaguePayload.mode)      );
  $("#details-table-count")   .html(  formatNum(leaguePayload.count)     );
  $("#details-table-1d")      .html(  formatNum(leaguePayload.quantity)  );
  $("#details-table-exalted") .html(  formatNum(leaguePayload.exalted)   );

  if (ITEM.categoryParent === "bases") {
    if (ITEM.variation === "shaped") {
      $("#item-icon").parent().addClass("influence influence-shaper-2x3");
    } else if (ITEM.variation === "elder") {
      $("#item-icon").parent().addClass("influence influence-elder-2x3");
    }
  }
  
  $("#item-icon").attr('src', fixIcon(ITEM.icon) );
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
    fillData( ITEM.leagues[LEAGUE] );
  });

  $("#history-dataset-radio").change(function(){
    HISTORY_DATASET = parseInt($("input[name=dataset]:checked", this).val());
    fillData( ITEM.leagues[LEAGUE] );
  });
}

//------------------------------------------------------------------------------------------------------------
// Item parsing and table HTML generation
//------------------------------------------------------------------------------------------------------------

function buildNameField() {
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

function fixIcon(icon) {
  if (!icon) {
    return "https://poe.watch/assets/img/missing.png";
  }

  // Use SSL
  icon = icon.replace("http://", "https://");

  if (!icon.includes("?")) {
    return icon;
  }

  let splitIcon = icon.split("?");
  let splitParams = splitIcon[1].split("&");
  let newParams = "";

  for (let i = 0; i < splitParams.length; i++) {
    switch (splitParams[i].split("=")[0]) {
      case "scale": 
      case "w":
      case "h":
        break;
      default:
        newParams += "&" + splitParams[i];
        break;
    }
  }

  if (newParams) {
    return splitIcon[0] + "?" + newParams.substr(1);
  } else {
    return splitIcon[0];
  }
}

//------------------------------------------------------------------------------------------------------------
// Utility functions
//------------------------------------------------------------------------------------------------------------

function formatHistory(leaguePayload) {
  let keys = [];
  let vals = {
    mean:     [],
    median:   [],
    mode:     [],
    quantity: []
  };

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
      vals.mean     .push(null);
      vals.median   .push(null);
      vals.mode     .push(null);
      vals.quantity .push(null);

      keys.push(null);
    }

    // Grab values
    for (var key in leaguePayload.history) {
      if (leaguePayload.history.hasOwnProperty(key)) {
        keys.push(formatDate(key));

        if (leaguePayload.history[key] === null) {
          vals.mean     .push(0);
          vals.median   .push(0);
          vals.mode     .push(0);
          vals.quantity .push(0);
        } else {
          vals.mean     .push(leaguePayload.history[key].mean     );
          vals.median   .push(leaguePayload.history[key].median   );
          vals.mode     .push(leaguePayload.history[key].mode     );
          vals.quantity .push(leaguePayload.history[key].quantity );
        }
      }
    }
  } else {
    let oldestDate = new Date();
    oldestDate.setDate(oldestDate.getDate() - 120);
    let oldDate = new Date(Object.keys(leaguePayload.history)[0]);

    let timeDiff = Math.abs(oldDate.getTime() - oldestDate.getTime());
    let diffDays = Math.ceil(timeDiff / (1000 * 3600 * 24)); 

    // For development
    if (diffDays > 120) diffDays = 120;

    for (let i = 0; i < diffDays; i++) {
      vals.mean     .push(null);
      vals.median   .push(null);
      vals.mode     .push(null);
      vals.quantity .push(null);

      keys.push(null);
    }

    // Grab values
    for (var key in leaguePayload.history) {
      if (leaguePayload.history.hasOwnProperty(key)) {
        if (leaguePayload.history[key] === null) {
          vals.mean     .push(null);
          vals.median   .push(null);
          vals.mode     .push(null);
          vals.quantity .push(null);

          keys.push(null);
        } else {
          vals.mean     .push(leaguePayload.history[key].mean     );
          vals.median   .push(leaguePayload.history[key].median   );
          vals.mode     .push(leaguePayload.history[key].mode     );
          vals.quantity .push(leaguePayload.history[key].quantity );
          
          keys.push(formatDate(key));
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

  let s = new Date(date);
  return s.getDate() + " " + MONTH_NAMES[s.getMonth()];
}
