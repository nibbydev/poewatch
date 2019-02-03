
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
    case "d":
      coeff = 1000 * 60 * 60 * 24;
      suffix = "d";
      break;
    default:
      coeff = 1000 * 60 * 60;
      suffix = "h";
      break;
  }

  var diff = Math.abs(new Date(time) - new Date());
  var val = Math.floor(diff / coeff);

  return val + suffix + " ago";
}

function parseStats(json) {
  var labelFreq;

  switch (TYPE) {
    case "m":
      labelFreq = 5;
      break;
    case "h":
    case "d":
      labelFreq = 1;
      break;
    default:
      labelFreq = Math.ceil(json.labels.length / 16);
      break;
  }

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
        return index % labelFreq === 0 ? value : null;
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

    var cardTemplate = `
    <div class="card custom-card w-100 mb-3">
      <div class="card-header">
        <h3 class="m-0">{{title}}</h3>
      </div>

      <div class="card-body">
        <div class='ct-chart' id='CHART-{{type}}'></div>
      </div>
    
      <div class="card-footer slim-card-edge"></div>
    </div>
    `.trim().replace("{{title}}", type).replace("{{type}}", type);

    $("#main").append(cardTemplate);
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
