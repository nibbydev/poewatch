$(document).ready(function() {
  $(".league-element").each(function(index) {
    addCountDownTimer(this);
  });
});

function addCountDownTimer(element) {
  let start = new Date($(".league-start", element).attr("value"));
  let end   = new Date($(".league-end",   element).attr("value"));

  var cdText = $(".league-description", element);
  var cdBar = $(".progressbar-bar", element);

  var _second = 1000;
  var _minute = _second * 60;
  var _hour = _minute * 60;
  var _day = _hour * 24;
  var timer;

  function showRemaining() {
    var now = new Date();

    if (start < now) {
      var distance = end - now;
      var percentage = (now - start) / (end - start) * 100;
    } else {
      var distance = start - now;
      var percentage = 0;
    }

    if (distance < 1000) {
      clearInterval(timer);
      cdText.html(start < now ? " " : "<span class='badge badge-danger mb-2'>Started</span>");
      return;
    }

    var days = Math.floor(distance / _day);
    var hours = Math.floor((distance % _day) / _hour);
    var minutes = Math.floor((distance % _hour) / _minute);
    var seconds = Math.floor((distance % _minute) / _second);
    var remainString = "Remaining: "; 

    if      (days === 0)  remainString += "<span class='custom-text-gray'>"   + days + " days, </span>";
    else if (days === 1)  remainString += "<span class='custom-text-orange'>" + days + " day, </span>";
    else                  remainString += "<span class='subtext-0'>"          + days + " days, </span>";
    
    if (days === 0) {
      if      (hours === 0) remainString += "<span class='custom-text-gray'>"   + hours + " hours, </span>";
      else if (hours === 1) remainString += "<span class='custom-text-orange'>" + hours + " hour, </span>";
      else                  remainString += "<span class='custom-text-red'>"    + hours + " hours, </span>";
    } else                  remainString += "<span class='subtext-0'>"          + hours + (hours === 1 ?  " hour, " : " hours, ") + "</span>";

    if (days === 0 && hours === 0) {
      if      (minutes ===  0) remainString += "<span class='custom-text-gray'>"   + minutes + " minutes, </span>";
      else if (minutes ===  1) remainString += "<span class='custom-text-orange'>" + minutes + " minute, </span>";
      else                     remainString += "<span class='custom-text-red'>"    + minutes + " minutes, </span>";
    } else                     remainString += "<span class='subtext-0'>"          + minutes + (minutes === 1 ?  " minute, " : " minutes, ") + "</span>";

    if (days === 0 && hours === 0 && minutes === 0) {
      if      (seconds ===  0) remainString += "<span class='custom-text-gray'>"   + seconds + " seconds</span>";
      else if (seconds ===  1) remainString += "<span class='custom-text-orange'>" + seconds + " second</span>";
      else                     remainString += "<span class='custom-text-red'>"    + seconds + " seconds</span>";
    } else                     remainString += "<span class='subtext-0'>"          + seconds + (seconds === 1 ?  " second" : " seconds") + "</span>";

    cdText.html(remainString);
    cdBar.css("width", percentage+"%");
  }

  showRemaining();
  timer = setInterval(showRemaining, 1000);
}
