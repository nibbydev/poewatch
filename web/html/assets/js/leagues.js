$(".league-element").each(function(index) {
  addCountDownTimer(this);
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
    var rDist = end - now;
    var eDist = now - start;
    var percentage = start < now ? (now - start) / (end - start) * 100 : 0;

    if (rDist < 1000) {
      clearInterval(timer);
      cdText.html(start < now ? "" : "<span class='badge badge-danger mb-2'>Started</span>");
      return;
    }

    //
    // Software gore
    //

    var eDays     = Math.ceil(eDist / _day);
    var eHours    = Math.ceil((eDist % _day) / _hour);
    var eMinutes  = Math.ceil((eDist % _hour) / _minute);
    var eSeconds  = Math.ceil((eDist % _minute) / _second);

    var rDays     = Math.floor(rDist / _day);
    var rHours    = Math.floor((rDist % _day) / _hour);
    var rMinutes  = Math.floor((rDist % _hour) / _minute);
    var rSeconds  = Math.floor((rDist % _minute) / _second);

    if      (rDays === 0)  rDays = "<span class='custom-text-gray'>"         + rDays    + "d</span>";
    else if (rDays === 1)  rDays = "<span class='custom-text-orange'>"       + rDays    + "d</span>";
    else                   rDays = "<span class='subtext-0'>"                + rDays    + "d</span>";
    
    if (rDays === 0) {
      if      (rHours === 0) rHours = "<span class='custom-text-gray'>"       + rHours   + "h</span>";
      else if (rHours === 1) rHours = "<span class='custom-text-orange'>"     + rHours   + "h</span>";
      else                   rHours = "<span class='custom-text-red'>"        + rHours   + "h</span>";
    } else                   rHours = "<span class='subtext-0'>"              + rHours   + "h</span>";

    if (rDays === 0 && rHours === 0) {
      if      (rMinutes ===  0) rMinutes = "<span class='custom-text-gray'>"    + rMinutes + "m</span>";
      else if (rMinutes ===  1) rMinutes = "<span class='custom-text-orange'>"  + rMinutes + "m</span>";
      else                      rMinutes = "<span class='custom-text-red'>"     + rMinutes + "m</span>";
    } else                      rMinutes = "<span class='subtext-0'>"           + rMinutes + "m</span>";

    if (rDays === 0 && rHours === 0 && rMinutes === 0) {
      if      (rSeconds ===  0) rSeconds = "<span class='custom-text-gray'>"    + rSeconds + "s</span>";
      else if (rSeconds ===  1) rSeconds = "<span class='custom-text-orange'>"  + rSeconds + "s</span>";
      else                      rSeconds = "<span class='custom-text-red'>"     + rSeconds + "s</span>";
    } else                      rSeconds = "<span class='subtext-0'>"           + rSeconds + "s</span>";

    var template = `
    <table>
      <tr>
        <td class='pr-2'>Elapsed:</td>
        <td class='text-right pr-1 subtext-0'>{{e-d}}d</td>
        <td class='text-right pr-1 subtext-0'>{{e-h}}h</td>
        <td class='text-right pr-1 subtext-0'>{{e-m}}m</td>
        <td class='text-right pr-1 subtext-0'>{{e-s}}s</td>
      </tr>
      <tr>
        <td class='pr-2'>Remaining:</td>
        <td class='text-right pr-1'>{{r-d}}</td>
        <td class='text-right pr-1'>{{r-h}}</td>
        <td class='text-right pr-1'>{{r-m}}</td>
        <td class='text-right pr-1'>{{r-s}}</td>
      </tr>
    </table>
    `.trim().replace("{{e-d}}", eDays)
            .replace("{{e-h}}", eHours)
            .replace("{{e-m}}", eMinutes)
            .replace("{{e-s}}", eSeconds)
            .replace("{{r-d}}", rDays)
            .replace("{{r-h}}", rHours)
            .replace("{{r-m}}", rMinutes)
            .replace("{{r-s}}", rSeconds);

    cdText.html(template);
    cdBar.css("width", percentage+"%");
  }

  showRemaining();
  timer = setInterval(showRemaining, 1000);
}
