var TEMPLATE = `
<div class="row mb-3">
  <div class="col-lg">
    <div class="card custom-card">
      <div class="card-body">
        <h2 class="text-center">{{title}}</h2>
        <hr>
        <h4 id="counter-{{id}}" class="text-center mb-3"></h4>
        <div class="progress" style="height: 20px;">
          <div class="progress-bar bg-secondary" role="progressbar" id="bar-{{id}}"></div>
        </div>
      </div>
    </div>
  </div>
</div>`;

function AddCountDownTimer(st, nd, index) {
  var start = new Date(st);
  var end = new Date(nd);

  var idE = $("#counter-" + index);
  var pbE = $("#bar-" + index);

  var _second = 1000;
  var _minute = _second * 60;
  var _hour = _minute * 60;
  var _day = _hour * 24;
  var timer;

  function showRemaining() {
    var now = new Date();
    var distance = end - now;
    var percentage = Math.round((now - start) / (end - start) * 1000) / 10;

    if (distance < 0) {

      clearInterval(timer);
      idE.text("Expired");

      return;
    }
    var days = Math.floor(distance / _day);
    var hours = Math.floor((distance % _day) / _hour);
    var minutes = Math.floor((distance % _hour) / _minute);
    var seconds = Math.floor((distance % _minute) / _second);

    var tmp = "";
    if (days > 0) tmp += days + " days, ";
    if (hours > 0) tmp += hours + " hours, ";
    if (minutes > 0) tmp += minutes + " minutes, ";
    tmp += seconds + " seconds";

    idE.text(tmp);
    pbE.css("width", percentage+"%");
  }

  showRemaining();
  timer = setInterval(showRemaining, 1000);
}

$(document).ready(function() {
  var main = $("#main");
  var payload = $("#service-data").data("payload");
  var elements = JSON.parse( payload.replace(/'/g, '"') );

  $.each(elements, function(index, element) {
    if (element["id"].indexOf("SSF") !== -1 || element["id"] === "Standard" || element["id"].indexOf("Hardcore") !== -1) return;

    let template = TEMPLATE.trim();

    template = template.replace("{{title}}", "League progress: " + element["id"]);
    template = template.replace("{{id}}", index);
    template = template.replace("{{id}}", index); // I am lazy

    main.append(template);

    AddCountDownTimer(element["start"], element["end"], index);
    return;
  });
});




