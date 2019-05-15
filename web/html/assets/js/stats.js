/*
 * Handles displaying/loading statistical data
 */

const statData = {};
const chartOptions = {
  height: 250,
  showPoint: true,
  lineSmooth: Chartist.Interpolation.cardinal({
    fillHoles: true,
  }),
  axisX: {
    showGrid: true,
    showLabel: true,
    labelInterpolationFnc: function skipLabels(value, index) {
      return index % 16 === 0 ? value + "h" : null;
    }
  },
  fullWidth: true,
  plugins: [
    Chartist.plugins.tooltip2({
      cssClass: 'chartist-tooltip',
      offset: {
        x: 0,
        y: -20,
      },
      template: '{{key}}h ago: {{value}}',
      hideDelay: 500
    })
  ]
};
const API_URL = "https://api.poe.watch/";

$(document).ready(function() {
  let tmp;

  if ((tmp = parseQueryParam("type"))) {
    request(tmp);
  } else {
    request("count");
  }

  $("button.statSelect").on("click", function(){
    let $this = $(this);
    let val = $this.val();

    console.log("Button press: " + val);
    updateQueryParam("type", val);

    $("button.statSelect").removeClass("active");
    $this.addClass("active");

    request(val);
  });
}); 

function request(type) {
  if (!["count", "error", "time"].includes(type)) {
    return;
  } 
  
  if (type in statData) {
    parseStats(statData[type]);
    return;
  }

  $.ajax({
    url: API_URL + "stats?type=" + type,
    type: "GET",
    async: true,
    dataTypes: "json"
  }).done(function (json) {
    statData[type] = json;
    parseStats(json);
  });
}

function formatTime(time) {
  const diff = Math.abs(new Date(time) - new Date());
  const val = Math.floor(diff / 1000 / 60 / 60);

  return val.toString();
}

function parseStats(json) {
  const main = $("#main");
  main.empty();

  const labels = [];
  for (let i = 0; i < json.labels.length; i++) {
    labels.push(formatTime(json.labels[i]));
  }

  for (let i = 0; i < json.types.length; i++) {
    const type = json.types[i];

    const series = [];
    for (let j = 0; j < json.series[i].length; j++) {
      series.push(json.series[i][j] === null ? 0 : json.series[i][j]);
    }

    const data = {
      labels: labels,
      series: [series]
    };

    const cardTemplate = `
    <div class="card custom-card w-100 mb-3">
      <div class="card-header">
        <h3 class="m-0">${type}</h3>
      </div>

      <div class="card-body">
        <div class='ct-chart' id='CHART-${type}'></div>
      </div>
    
      <div class="card-footer slim-card-edge"></div>
    </div>
    `.trim();

    main.append(cardTemplate);

    switch (type) {
      case "COUNT_API_ERRORS_READ_TIMEOUT":
      case "COUNT_API_ERRORS_CONNECT_TIMEOUT":
      case "COUNT_API_ERRORS_CONNECTION_RESET":
      case "COUNT_API_ERRORS_5XX":
      case "COUNT_API_ERRORS_429":
      case "COUNT_API_ERRORS_DUPLICATE":
        new Chartist.Bar("#CHART-" + type, data, chartOptions);
        break;
      default:
        new Chartist.Line("#CHART-" + type, data, chartOptions);
        break;
    }
  }
}

function parseQueryParam(key) {
  let url = window.location.href;
  key = key.replace(/[\[\]]/g, "\\$&");
  
  var regex = new RegExp("[?&]" + key + "(=([^&#]*)|&|#|$)"),
      results = regex.exec(url);
      
  if (!results   ) return null;
  if (!results[2]) return "";

  return decodeURIComponent(results[2].replace(/\+/g, " "));
}

function updateQueryParam(key, value) {
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
