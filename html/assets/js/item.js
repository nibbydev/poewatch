var LEAGUE = "Incursion";
var itemData = null;
var HISTORY_CHART = null;

var TEMPLATE_leagueBtn = `
<label class="btn btn-sm btn-outline-dark p-0 px-1 {{active}}">
  <input type="radio" name="league" value="{{value}}">{{name}}
</label>`;

$(document).ready(function() {
  if (index) makeRequest(index);
});


function makeRequest(index) {
  var data = {
    index: index
  };

  var request = $.ajax({
    url: "http://api.poe.watch/priceData",
    data: data,
    type: "GET",
    async: true,
    dataTypes: "json"
  });

  request.done(function(json) {
    if ("error" in json) return;
    console.log("we did it");
    itemData = json;

    fillData();
    buildSparkLine();
    placeCharts();
    displayNewHistory();
    displayOldHistory();
  });
}

function buildSparkLine() {
  var svgColorClass = itemData["data"][LEAGUE]["history"]["change"] > 0 ? "green" : "orange";
  
  let svg = document.createElement("svg");
  
  svg.setAttribute("class", "sparkline sparkline-" + svgColorClass);
  svg.setAttribute("width", 100);
  svg.setAttribute("height", 40);
  svg.setAttribute("stroke-width", 3);

  sparkline.sparkline(svg, itemData["data"][LEAGUE]["history"]["spark"]);

  $(".mega-sparkline").html(svg.outerHTML);
}

//------------------------------------------------------------------------------------------------------------
// Data displaying
//------------------------------------------------------------------------------------------------------------

function fillData() {
  $("#item-icon").prop("src", itemData["data"][LEAGUE]["icon"]);

  $("#item-name").html( buildNameField(itemData) );
  $("#item-change").html(itemData["data"][LEAGUE]["history"]["change"] + "%");
  
  $("#item-chaos").html( roundPrice(itemData["data"][LEAGUE]["mean"]) );
  $("#item-exalt").html( roundPrice(itemData["data"][LEAGUE]["exalted"]) );

  $("#item-mean").html(itemData["data"][LEAGUE]["mean"]);
  $("#item-median").html(itemData["data"][LEAGUE]["median"]);
  $("#item-mode").html(itemData["data"][LEAGUE]["mode"]);

  $("#item-count").html(itemData["data"][LEAGUE]["count"]);
  $("#item-quantity").html(itemData["data"][LEAGUE]["quantity"]);
  //$("#item-1w")  .html(itemData["data"][LEAGUE]["quantity"]);
}

function buildNameField(itemData) {
  let template = "<span {{foil}}>{{name}}{{type}}</span>{{var_or_tier}}{{links}}";
  let item = itemData["data"][LEAGUE];

  if (item["frame"] === 9) {
    template = template.replace("{{foil}}", "class='item-foil'");
  } else {
    template = template.replace("{{foil}}", "");
  }

  template = template.replace("{{name}}", item["name"]);

  if ("type" in item) {
    let tmp = "<span class='subtext-1'>, " + item["type"] + "</span>";;
    template = template.replace("{{type}}", tmp);
  } else {
    template = template.replace("{{type}}", "");
  }

  if ("var" in item && item["frame"] !== -1) {
    let tmp = " <span class='badge custom-badge-gray'>" + item["var"] + "</span>";
    template = template.replace("{{var_or_tier}}", tmp);
  } else if ("tier" in item) {
    let tmp = " <span class='badge custom-badge-gray'>" + item["tier"] + "</span>";
    template = template.replace("{{var_or_tier}}", tmp);
  } else {
    template = template.replace("{{var_or_tier}}", "");
  }

  if ("links" in item) {
    let tmp = " <span class='badge custom-badge-gray'>" + item["links"] + " link</span>";
    template = template.replace("{{links}}", tmp);
  } else {
    template = template.replace("{{links}}", "");
  }

  if (item["history"]["mean"].length < 7) {
    let tmp = "<span class='badge badge-light'>New</span>";
    template = template.replace("{{new}}", tmp);
  } else {
    template = template.replace("{{new}}", "");
  }
  
  return template;
}

//------------------------------------------------------------------------------------------------------------
// Chart displaying
//------------------------------------------------------------------------------------------------------------

function placeCharts() {
  var priceData = {
    type: "line",
    data: {
      labels: getAllDays(itemData["data"][LEAGUE]["history"]["mean"].length),
      datasets: [{
        label: "Price in chaos",
        data: itemData["data"][LEAGUE]["history"]["mean"],
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
      labels: getAllDays(itemData["data"][LEAGUE]["history"]["quantity"].length),
      datasets: [{
        label: "Quantity",
        data: itemData["data"][LEAGUE]["history"]["quantity"],
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

  new Chart($("#chart-price"), priceData);
  new Chart($("#chart-quantity"), quantData);
  HISTORY_CHART = new Chart($("#chart-past"), pastData);
}

function displayNewHistory() {
  var newLeagues = Object.keys(itemData["new"]);

  HISTORY_CHART.data.labels = itemData["new"][newLeagues[0]];
  HISTORY_CHART.data.datasets[0].data = itemData["new"][newLeagues[0]];
  HISTORY_CHART.update();

  let tmp_leagueBtnString = "";

  $.each(newLeagues, function(index, league) {
    tmp_leagueBtnString += TEMPLATE_leagueBtn.trim()
      .replace("{{active}}", (league === newLeagues[0] ? "active" : ""))
      .replace("{{value}}", league)
      .replace("{{name}}", formatLeague(league));
  });

  let radio = $("#history-league-radio-new");
  radio.append(tmp_leagueBtnString);

  radio.change(function(){

    $("#history-league-radio-old[data-toggle='buttons'] :radio").prop("checked", false);
    $("#history-league-radio-old[data-toggle='buttons'] label").removeClass("active");

    selectedLeague = $("input[name=league]:checked", this).val();
    console.log(selectedLeague);

    HISTORY_CHART.data.labels = itemData["new"][selectedLeague];
    HISTORY_CHART.data.datasets[0].data = itemData["new"][selectedLeague];
    HISTORY_CHART.update();
  });
}

function displayOldHistory() {
  var oldLeagues = Object.keys(itemData["old"]);
  let tmp_leagueBtnString = "";

  if (oldLeagues.length > 0) {
    $.each(oldLeagues, function(index, league) {
      tmp_leagueBtnString += TEMPLATE_leagueBtn.trim()
        .replace("{{active}}", "")
        .replace("{{value}}", league)
        .replace("{{name}}", formatLeague(league));
    });
  } else {
    tmp_leagueBtnString = "<span class='text-muted'>No results</span>"
  }

  let radio = $("#history-league-radio-old");
  radio.append(tmp_leagueBtnString);

  radio.change(function(){
    $("#history-league-radio-new[data-toggle='buttons'] :radio").prop("checked", false);
    $("#history-league-radio-new[data-toggle='buttons'] label").removeClass("active");

    selectedLeague = $("input[name=league]:checked", this).val();

    HISTORY_CHART.data.labels = itemData["old"][selectedLeague];
    HISTORY_CHART.data.datasets[0].data = itemData["old"][selectedLeague];
    HISTORY_CHART.update();
  });
}

//------------------------------------------------------------------------------------------------------------
// Utility functions
//------------------------------------------------------------------------------------------------------------

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

function roundPrice(price) {
  const numberWithCommas = (x) => {
    var parts = x.toString().split(".");
    parts[0] = parts[0].replace(/\B(?=(\d{3})+(?!\d))/g, ",");
    return parts.join(".");
  }

  return numberWithCommas(Math.round(price * 100) / 100);
}

function formatLeague(name) {
  if (~name.indexOf(" Event")) return name.substring(0, name.indexOf(" Event"));
  else if (~name.indexOf("Hardcore ")) return "HC " + name.substring(9);
  else return name;
}