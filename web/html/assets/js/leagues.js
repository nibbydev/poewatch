$(".element-id").each(function() {
  addCountDownTimer(this);
});

function calcTime(timeString) {
  if (!timeString) {
    return {
      text: "<span class='subtext-0'>Unavailable</span>",
      distance: -1
    };
  }

  const time = new Date(timeString);

  const _second = 1000;
  const _minute = _second * 60;
  const _hour = _minute * 60;
  const _day = _hour * 24;

  var distance = Math.abs(time - new Date());
  
  var days     = Math.round(distance / _day);
  var hours    = Math.round((distance % _day) / _hour);
  var minutes  = Math.round((distance % _hour) / _minute);
  var seconds  = Math.round((distance % _minute) / _second);
  
  let dDisplay, hDisplay, mDisplay, sDisplay;

  if      (days === 0)  dDisplay = "<span class='subtext-1'>"                + days    + "d</span>";
  else if (days === 1)  dDisplay = "<span class='custom-text-orange'>"       + days    + "d</span>";
  else                  dDisplay = "<span class='subtext-0'>"                + days    + "d</span>";
  
  if (days === 0) {
    if      (hours === 0) hDisplay = "<span class='subtext-1'>"              + hours   + "h</span>";
    else if (hours === 1) hDisplay = "<span class='custom-text-orange'>"     + hours   + "h</span>";
    else                  hDisplay = "<span class='custom-text-red'>"        + hours   + "h</span>";
  } else                  hDisplay = "<span class='subtext-0'>"              + hours   + "h</span>";

  if (days === 0 && hours === 0) {
    if      (minutes ===  0) mDisplay = "<span class='subtext-1'>"           + minutes + "m</span>";
    else if (minutes ===  1) mDisplay = "<span class='custom-text-orange'>"  + minutes + "m</span>";
    else                     mDisplay = "<span class='custom-text-red'>"     + minutes + "m</span>";
  } else                     mDisplay = "<span class='subtext-0'>"           + minutes + "m</span>";

  if (days === 0 && hours === 0 && minutes === 0) {
    if      (seconds ===  0) sDisplay = "<span class='subtext-1'>"           + seconds + "s</span>";
    else if (seconds ===  1) sDisplay = "<span class='custom-text-orange'>"  + seconds + "s</span>";
    else                     sDisplay = "<span class='custom-text-red'>"     + seconds + "s</span>";
  } else                     sDisplay = "<span class='subtext-0'>"           + seconds + "s</span>";

  return {
    text: dDisplay + " " + hDisplay + " " + mDisplay + " " + sDisplay,
    distance: distance
  };
}

function calcPercentage(startString, endString) {
  if (!startString || !endString) {
    return 0;
  }

  const now = new Date();
  const startTime = new Date(startString);
  const endTime = new Date(endString);

  return startTime < now ? (now - startTime) / (endTime - startTime) * 100 : 0;
}

function addCountDownTimer(element) {
  const isUpcoming = $(".element-data-upcoming", element).attr("value") == 1;
  const start = $(".element-data-start", element).attr("value");
  const end = $(".element-data-end", element).attr("value");
  var timer;

  const cd1Text = $(".element-cd-id-1-text", element);
  const cd2Text = $(".element-cd-id-2-text", element);
  const cdBar = $(".element-cdbar-id", element);

  function showRemaining() {
    var startData = calcTime(start);
    var endData = calcTime(end);

    if (isUpcoming && start && startData.distance < 1000 || !isUpcoming && end && endData.distance < 1000) {
      clearInterval(timer);
      return;
    }

    cd1Text.html(startData.text);
    cd2Text.html(endData.text);

    if (!isUpcoming) {
      cdBar.css("width", calcPercentage(start, end) + "%");
    }
  }

  showRemaining();
  timer = setInterval(showRemaining, 1000);
}
