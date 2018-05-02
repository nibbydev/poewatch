var TEMPLATE = `
<h4>{{title}}</h4>
<p class="mb-1">{{start}}</p>
<p class="mb-1">{{end}}</p>
<p id="counter-{{id}}" class="mb-2 mt-3"></p>
<div class="progress" style="height: 20px;">
  <div class="progress-bar bg-secondary" role="progressbar" id="bar-{{id}}"></div>
</div>
<hr>`;

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
    if (element["id"].indexOf("SSF") !== -1 || element["id"] === "Standard" || element["id"] === "Hardcore") return;

    let template = TEMPLATE.trim();

    template = template.replace("{{title}}", element["id"]);

    template = template.replace("{{start}}", "Started on: " + formatDate(element["start"]));
    template = template.replace("{{end}}", "Ends on: " + formatDate(element["end"]));

    template = template.replace("{{id}}", index);
    template = template.replace("{{id}}", index); // I am lazy

    main.append(template);

    AddCountDownTimer(element["start"], element["end"], index);
    return;
  });

  $("hr:last-child", main).remove();
});

function formatDate(dt) {
  var date = new Date(dt);

  var monthNames = [
    "Jan", "Feb", "Mar",
    "Apr", "May", "Jun", 
    "Jul", "Aug", "Sep",
    "Oct", "Nov", "Dec"
  ];

  var day = date.getDate();
  var monthIndex = date.getMonth();
  var year = date.getFullYear();

  return day + " " + monthNames[monthIndex] + " " + year;
}