$(".league-element").each(function() {
  addCountDownTimer(this);
});

function addCountDownTimer(element) {
  let start = new Date($(".league-start", element).attr("value"));
  let end   = new Date($(".league-end",   element).attr("value"));

  var cdText = $(".league-countdown", element);
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
      cdText.remove();
      return;
    }

    if (isNaN(eDist)) eDist = 0;
    if (isNaN(rDist)) rDist = 0;

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
    let rDd, rHd, rMd, rSd;

    if      (rDays === 0)  rDd = "<span class='custom-text-gray'>"         + rDays    + "d</span>";
    else if (rDays === 1)  rDd = "<span class='custom-text-orange'>"       + rDays    + "d</span>";
    else                   rDd = "<span class='subtext-0'>"                + rDays    + "d</span>";
    
    if (rDays === 0) {
      if      (rHours === 0) rHd = "<span class='custom-text-gray'>"       + rHours   + "h</span>";
      else if (rHours === 1) rHd = "<span class='custom-text-orange'>"     + rHours   + "h</span>";
      else                   rHd = "<span class='custom-text-red'>"        + rHours   + "h</span>";
    } else                   rHd = "<span class='subtext-0'>"              + rHours   + "h</span>";

    if (rDays === 0 && rHours === 0) {
      if      (rMinutes ===  0) rMd = "<span class='custom-text-gray'>"    + rMinutes + "m</span>";
      else if (rMinutes ===  1) rMd = "<span class='custom-text-orange'>"  + rMinutes + "m</span>";
      else                      rMd = "<span class='custom-text-red'>"     + rMinutes + "m</span>";
    } else                      rMd = "<span class='subtext-0'>"           + rMinutes + "m</span>";

    if (rDays === 0 && rHours === 0 && rMinutes === 0) {
      if      (rSeconds ===  0) rSd = "<span class='custom-text-gray'>"    + rSeconds + "s</span>";
      else if (rSeconds ===  1) rSd = "<span class='custom-text-orange'>"  + rSeconds + "s</span>";
      else                      rSd = "<span class='custom-text-red'>"     + rSeconds + "s</span>";
    } else                      rSd = "<span class='subtext-0'>"           + rSeconds + "s</span>";

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
            .replace("{{r-d}}", rDd)
            .replace("{{r-h}}", rHd)
            .replace("{{r-m}}", rMd)
            .replace("{{r-s}}", rSd);

    cdText.html(template);
    cdBar.css("width", percentage+"%");
  }

  showRemaining();
  timer = setInterval(showRemaining, 1000);
}
