
var TYPE = null;

$(document).ready(function() {
  if (TYPE = parseQueryParam('type')) {
    let request = $.ajax({
      url: "https://api.poe.watch/stats?type=" + TYPE,
      type: "GET",
      async: true,
      dataTypes: "json"
    });
  
    request.done(parseStats);
  }
}); 

function formatTime(time) {
  var coeff, suffix;

  switch (TYPE) {
    case "m":
      coeff = 1000 * 60;
      suffix = "m";
      break;
    case "0":
    case "h":
      coeff = 1000 * 60 * 60;
      suffix = "h";
      break;
    case "d":
      coeff = 1000 * 60 * 60 * 24;
      suffix = "d";
      break;
    default:
      break;
  }

  var diff = Math.abs(new Date(time) - new Date());
  var val = Math.ceil(diff / coeff);

  return val + suffix + " ago";
}

function parseStats(json) {
  var chartOptions = {
    height: 250,
    showPoint: true,
    lineSmooth: Chartist.Interpolation.cardinal({
      fillHoles: true,
    }),
    axisX: {
      showGrid: true,
      showLabel: true,
      labelInterpolationFnc: function skipLabels(value, index) {
        return index % (TYPE === "m" ? 5 : 1) === 0 ? value : null;
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
        template: '{{key}}: {{value}}',
        hideDelay: 500
      })
    ]
  };

  var labels = [];
  for (let i = 0; i < json.labels.length; i++) {
    labels.push(formatTime(json.labels[i]));
  }

  for (let i = 0; i < json.types.length; i++) {
    const type = json.types[i];
    
    var data = {
      labels: labels,
      series: [json.series[i]]
    }

    var html = "<h3>"+type+"</h3>" + "<hr>"+ "<div class='ct-chart' id='CHART-"+type+"'></div>";
    $("#main").append(html);
    new Chartist.Line('#CHART-'+type, data, chartOptions);
  }
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
