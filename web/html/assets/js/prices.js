/*
  There's not much here except for some poorly written JS functions. And since you're 
  already here, it can't hurt to take a look at http://youmightnotneedjquery.com/
*/

class ItemRow {
  constructor (item) {
    this.item = item;
    this.row = "<tr value={{id}}>{{data}}</tr>";

    this.sparkOptions = {
      pad_y: 2,
      width: 60,
      height: 30,
      radius: 0.2
    }

    // Build HTML elements
    var rowBuilder = [];
    rowBuilder.push(
      this.buildNameField(),
      this.buildGemFields(),
      this.buildBaseFields(),
      this.buildMapFields(),
      this.buildSparkField(),
      this.buildPriceFields(),
      this.buildChangeField(),
      this.buildDailyField(),
      this.buildTotalField()
    );

    this.row = this.row
      .replace("{{id}}", item.id)
      .replace("{{data}}", rowBuilder.join(""));
  }

  buildNameField() {
    let template = `
    <td>
      <div class='d-flex align-items-center'>
        <span class='img-container img-container-sm text-center mr-1'><img src="{{icon}}"></span>
        <span class='cursor-pointer {{color}}'>{{name}}{{type}}</span>{{variation}}{{link}}
      </div>
    </td>
    `.trim();
  
    template = template.replace("{{url}}", "https://poe.watch/item?league=" + FILTER.league.name + "&id=" + this.item.id);
  
    // If item is base
    if (this.item.base) {
      if (this.item.base.shaper) {
        this.item.icon += "&shaper=1";
        template = template.replace("{{color}}", "item-shaper");
      } else if (this.item.base.elder) {
        this.item.icon += "&elder=1";
        template = template.replace("{{color}}", "item-elder");
      }
    }

    // Use TLS for icons for that sweet, sweet secure site badge
    this.item.icon = this.item.icon.replace("http://", "https://");
    template = template.replace("{{icon}}", this.item.icon);

    // Foil item coloring
    template = template.replace("{{color}}", this.item.frame === 9 ? "item-foil" : "");
  
    // If item is enchantment, insert enchant values for display purposes
    if (this.item.enchant) {
      // Min roll
      if (this.item.name.includes("#") && this.item.enchant.min !== null) {
        this.item.name = this.item.name.replace("#", this.item.enchant.min);
      }
      
      // Max roll
      if (this.item.name.includes("#") && this.item.enchant.max !== null) {
        this.item.name = this.item.name.replace("#", this.item.enchant.max); 
      }
    }
    
    template = template.replace("{{name}}", this.item.name);
  
    if (this.item.type) {
      let tmp = "<span class='subtext-1'>, " + this.item.type + "</span>";;
      template = template.replace("{{type}}", tmp);
    } else {
      template = template.replace("{{type}}", "");
    }
  
    if (this.item.links) {
      let tmp = " <span class='badge custom-badge-gray ml-1'>" + this.item.links + " link</span>";
      template = template.replace("{{link}}", tmp);
    } else {
      template = template.replace("{{link}}", "");
    }
  
    if (this.item.variation) {
      let tmp = " <span class='badge custom-badge-gray ml-1'>" + this.item.variation + "</span>";
      template = template.replace("{{variation}}", tmp);
    } else {
      template = template.replace("{{variation}}", "");
    }
  
    return template;
  }
  
  buildGemFields() {
    // Don't run if item is not a gem
    if (!this.item.gem) return "";
  
    let template = `
    <td><span class='badge custom-badge-block custom-badge-gray'>{{level}}</span></td>
    <td><span class='badge custom-badge-block custom-badge-gray'>{{quality}}</span></td>
    <td><span class='badge custom-badge-{{color}}'>{{corrupted}}</span></td>
    `.trim();
  
    template = template.replace("{{level}}",      this.item.gem.level);
    template = template.replace("{{quality}}",  this.item.gem.quality);
    
    if (this.item.gem.corrupted) {
      template = template.replace("{{color}}",     "red");
      template = template.replace("{{corrupted}}", "✓");
    } else {
      template = template.replace("{{color}}",     "green");
      template = template.replace("{{corrupted}}", "✕");
    }
  
    return template;
  }
  
  buildBaseFields() {
    // Don't run if item is not a gem
    if (!this.item.base) return "";

    let template = `
    <td class='nowrap'>
      <span class='badge custom-badge-block custom-badge-gray'>{{itemLevel}}</span>
    </td>
    `.trim();

    template = template.replace("{{itemLevel}}", this.item.base.itemLevel);

    return template;
  }
  
  buildMapFields() {
    // Don't run if item is not a map
    if (!this.item.map) {
      return "";
    }

    let template = `
    <td class='nowrap'>
      <span class='badge custom-badge-block custom-badge-gray'>{{tier}}</span>
    </td>
    `.trim();

    return this.item.map.tier ? template.replace("{{tier}}", this.item.map.tier) : "<td></td>";
  }

  buildSparkField() {
    var template = `
      <td class='d-none d-md-flex'>{{spark}}</td>
    `.trim();

    // Count the number of history elements that are not null
    let count = 0;
    for (let i = 0; i < 7; i++) {
      if (this.item.history[i] !== null) {
        count++;
      }
    }

    // Can't display a sparkline with 1 element
    if (count < 2) return template.replace("{{spark}}", "");

    // Find first price from the left that is not null
    let lastPrice = null;
    for (let i = 0; i < 7; i++) {
      if (this.item.history[i] !== null) {
        lastPrice = this.item.history[i];
        break;
      }
    }

    // Calculate each value's change %-relation to current price
    let changes = [];
    for (let i = 0; i < 7; i++) { 
      if (this.item.history[i] > 0) {
        changes[i] = Math.round((1 - (lastPrice / this.item.history[i])) * 100, 4);
      }
    }

    // Generate sparklike html
    var spark = ItemRow.genSparkSVG(this.sparkOptions, changes);
    return "<td class='d-none d-md-flex'>" + spark + "</td>";
  }
  
  buildPriceFields() {
    let template = `
    <td>
      <div class='pricebox'>{{chaos_icon}}{{chaos_price}}</div>
    </td>
    <td class='d-none d-md-flex'>
      <div class='pricebox'>{{ex_icon}}{{ex_price}}</div>
    </td>
    `.trim();

    let chaosContainer  = TEMPLATE_imgContainer.trim().replace("{{img}}", ICON_CHAOS);
    let exContainer     = TEMPLATE_imgContainer.trim().replace("{{img}}", ICON_EXALTED);
  
    template = template.replace("{{chaos_price}}",  ItemRow.roundPrice(this.item.mean));
    template = template.replace("{{chaos_icon}}",   chaosContainer);
  
    if (this.item.exalted >= 1) {
      template = template.replace("{{ex_icon}}",    exContainer);
      template = template.replace("{{ex_price}}",   ItemRow.roundPrice(this.item.exalted));
    } else {
      template = template.replace("{{ex_icon}}",    "");
      template = template.replace("{{ex_price}}",   "");
    }
    
    return template;
  }
  
  buildChangeField() {
    let template = `
    <td>
      <span class='badge custom-badge-block custom-badge-{{color}}'>
        {{percent}}%
      </span>
    </td>
    `.trim();
  
    // Find first price from the left that is not null
    let lastPrice = null;
    for (let i = 0; i < 7; i++) {
      if (this.item.history[i] !== null) {
        lastPrice = this.item.history[i];
        break;
      }
    }
   
    // Calculate change %
    let change = Math.round(100 - lastPrice / this.item.mean * 100);

    // Limit it
    if (change > 999) {
      change = 999;
    } else if (change < -999) {
      change = -999;
    }

    // Pick a color scheme
    if (change >= 100) {
      template = template.replace("{{color}}", "green-ex");
    } else if (change <= -100) {
      template = template.replace("{{color}}", "red-ex");
    } else if (change >= 30) {
      template = template.replace("{{color}}", "green");
    } else if (change <= -30) {
      template = template.replace("{{color}}", "red");
    } else if (change >= 15) {
      template = template.replace("{{color}}", "green-lo");
    } else if (change <= -15) {
      template = template.replace("{{color}}", "red-lo");
    } else {
      template = template.replace("{{color}}", "gray");
    }

    return template.replace("{{percent}}", change);
  }
  
  buildDailyField() {
    let template = `
    <td>
      <span class='badge custom-badge-block custom-badge-{{color}}'>
        {{daily}}
      </span>
    </td>
    `.trim();

    if (FILTER.league.active) {
      if (this.item.daily >= 20) {
        template = template.replace("{{color}}", "gray");
      } else if (this.item.daily >= 10) {
        template = template.replace("{{color}}", "orange-lo");
      } else if (this.item.daily >= 5) {
        template = template.replace("{{color}}", "red-lo");
      } else if (this.item.daily >= 0) {
        template = template.replace("{{color}}", "red");
      }
    } else {
      template = template.replace("{{color}}", "gray");
    }
  
    return template.replace("{{daily}}", this.item.daily);
  }

  buildTotalField() {
    let template = `
    <td>
      <span class='badge custom-badge-block custom-badge-gray'>
        {{total}}
      </span>
    </td>
    `.trim();
  
    return template.replace("{{total}}", this.item.total);
  }

  static roundPrice(price) {
    const numberWithCommas = (x) => {
      var parts = x.toString().split(".");
      parts[0] = parts[0].replace(/\B(?=(\d{3})+(?!\d))/g, ",");
      return parts.join(".");
    }
  
    return numberWithCommas(Math.round(price * 100) / 100);
  }

  static genSparkSVG(options, elements) {
    var maxElement = Math.max(...elements);
    var minElement = Math.min(...elements);
  
    // There has been no change in the past week
    if (maxElement === minElement && minElement === 0) {
      maxElement = 1;
    }
  
    var stepX = options.width / (elements.length - 1);
    var stepY = (options.height - options.pad_y*2) / (maxElement - minElement);
  
    // Create pointarray
    var pointBuilder = ["M "];
    for (var i = 0; i < elements.length; i++) {
      if (elements[i] !== null) {
        var x = stepX * i;
        var y = (options.height - elements[i]*stepY + minElement*stepY - options.pad_y/2).toFixed(3);
  
        pointBuilder.push(x, " ", y, " L ");
      }
    }
  
    // Remove trailing zero
    pointBuilder.pop();

    return `
    <svg width="{{width}}" height="{{height}}" class="ct-chart-line">
      <g class="ct-series ct-series-a">
        <path d="{{points}}" class="ct-line" />
      </g>
    </svg>`
      .trim()
      .replace(/{{width}}/g,  options.width)
      .replace(/{{height}}/g, options.height)
      .replace(/{{points}}/g, ItemRow.roundSVGPathCorners(pointBuilder.join(""), options));
  }

  static roundSVGPathCorners(pathString, options) {
    function moveTowardsFractional(movingPoint, targetPoint, fraction) {
      return {
        x: parseFloat(movingPoint.x + (targetPoint.x - movingPoint.x)*fraction).toFixed(3),
        y: parseFloat(movingPoint.y + (targetPoint.y - movingPoint.y)*fraction).toFixed(3)
      };
    }
    
    // Adjusts the ending position of a command
    function adjustCommand(cmd, newPoint) {
      if (cmd.length > 2) {
        cmd[cmd.length - 2] = newPoint.x;
        cmd[cmd.length - 1] = newPoint.y;
      }
    }
    
    // Gives an {x, y} object for a command's ending position
    function pointForCommand(cmd) {
      return {
        x: parseFloat(cmd[cmd.length - 2]),
        y: parseFloat(cmd[cmd.length - 1]),
      };
    }
    
    // Split apart the path, handing concatonated letters and numbers
    var pathParts = pathString
      .split(/[,\s]/)
      .reduce(function(parts, part){
        var match = part.match("([a-zA-Z])(.+)");
        if (match) {
          parts.push(match[1]);
          parts.push(match[2]);
        } else {
          parts.push(part);
        }
        
        return parts;
      }, []);
    
    // Group the commands with their arguments for easier handling
    var commands = pathParts.reduce(function(commands, part) {
      if (parseFloat(part) == part && commands.length) {
        commands[commands.length - 1].push(part);
      } else {
        commands.push([part]);
      }
      
      return commands;
    }, []);
    
    // The resulting commands, also grouped
    var resultCommands = [];
    
    if (commands.length > 1) {
      var startPoint = pointForCommand(commands[0]);
      
      // Handle the close path case with a "virtual" closing line
      var virtualCloseLine = null;
      if (commands[commands.length - 1][0] == "Z" && commands[0].length > 2) {
        virtualCloseLine = ["L", startPoint.x, startPoint.y];
        commands[commands.length - 1] = virtualCloseLine;
      }
      
      // We always use the first command (but it may be mutated)
      resultCommands.push(commands[0]);
      
      for (var cmdIndex=1; cmdIndex < commands.length; cmdIndex++) {
        var prevCmd = resultCommands[resultCommands.length - 1];
        
        var curCmd = commands[cmdIndex];
        
        // Handle closing case
        var nextCmd = (curCmd == virtualCloseLine)
          ? commands[1]
          : commands[cmdIndex + 1];
        
        // Nasty logic to decide if this path is a candidite.
        if (nextCmd && prevCmd && (prevCmd.length > 2) && curCmd[0] == "L" && nextCmd.length > 2 && nextCmd[0] == "L") {
          // Calc the points we're dealing with
          var prevPoint = pointForCommand(prevCmd);
          var curPoint = pointForCommand(curCmd);
          var nextPoint = pointForCommand(nextCmd);
          
          // The start and end of the cuve are just our point moved towards the previous and next points, respectivly
          var curveStart = moveTowardsFractional(curPoint, prevCmd.origPoint || prevPoint, options.radius);
          var curveEnd = moveTowardsFractional(curPoint, nextCmd.origPoint || nextPoint, options.radius);
          
          // Adjust the current command and add it
          adjustCommand(curCmd, curveStart);
          curCmd.origPoint = curPoint;
          resultCommands.push(curCmd);
          
          // The curve control points are halfway between the start/end of the curve and
          // the original point
          var startControl = moveTowardsFractional(curveStart, curPoint, .5);
          var endControl = moveTowardsFractional(curPoint, curveEnd, .5);
    
          // Create the curve 
          var curveCmd = ["C", startControl.x, startControl.y, endControl.x, endControl.y, curveEnd.x, curveEnd.y];
          // Save the original point for fractional calculations
          curveCmd.origPoint = curPoint;
          resultCommands.push(curveCmd);
        } else {
          // Pass through commands that don't qualify
          resultCommands.push(curCmd);
        }
      }
      
      // Fix up the starting point and restore the close path if the path was orignally closed
      if (virtualCloseLine) {
        var newStartPoint = pointForCommand(resultCommands[resultCommands.length-1]);
        resultCommands.push(["Z"]);
        adjustCommand(resultCommands[0], newStartPoint);
      }
    } else {
      resultCommands = commands;
    }
    
    return resultCommands.reduce(function(str, c){ return str + c.join(" ") + " "; }, "");
  }
}

class DetailsModal {
  constructor() {
    this.dataSets = {};
    this.modal = $("#modal-details");
    this.current = {
      id: null,
      league: null,
      chart: null,
      dataset: 1
    }

    this.chartOptions = {
      height: 250,
      showPoint: true,
      lineSmooth: true,
      axisX: {
        showGrid: true,
        showLabel: true,
        labelInterpolationFnc: function skipLabels(value, index) {
          return index % 7  === 0 ? value : null;
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
          hideDelay: 500,
          valueTransformFunction: formatNum
        })
      ]
    };

    // Create league select event listener
    $("#modal-leagues", this.modal).change(function(){
      DETMODAL.current.league = $(":selected", this).val();
      DETMODAL.updateContent();
    });
  
    // Create dataset radio event listener
    $("#modal-radio", this.modal).change(function(){
      DETMODAL.current.dataset = parseInt($("input[name=dataset]:checked", this).val());
      DETMODAL.updateContent();
    });
  }

  resetData() {
    // Clear leagues from selector
    $("#modal-leagues", this.modal).find('option').remove();

    // Dataset selection
    var $radios = $('#modal-radio').children();
    $radios.prop('checked', false).removeClass('active');
    $radios.first().prop('checked', true).addClass('active');

    this.current = {
      id: null,
      league: null,
      chart: null,
      dataset: 1
    }
  }

  onRowClick(event) {
    let target = $(event.currentTarget);
    let id = parseInt(target.attr('value'));

    // If user clicked on a different row
    if (isNaN(id)) return;

    // Reset anything left by previous modal
    this.resetData();

    this.current.id = id;
    console.log("Clicked on row id: " + this.current.id);

    // Show buffer and hide content
    this.setBufferVisibility(true);

    // Load history data
    if (this.current.id in this.dataSets) {
      console.log("History source: local");
      this.setContent();
    } else {
      console.log("History source: remote");

      let request = $.ajax({
        url: "https://api.poe.watch/item",
        data: {id: this.current.id},
        type: "GET",
        async: true,
        dataTypes: "json"
      });
    
      request.done(function(payload) {
        DETMODAL.dataSets[DETMODAL.current.id] = payload;
        DETMODAL.setContent();
      });
    }

    // Find item entry from current price category
    let item = DetailsModal.findItem(this.current.id);

    // Set modal's icon and name while request might still be processing
    $("#modal-icon", this.modal).attr("src", item.icon);
    $("#modal-name", this.modal).html(this.buildNameField(item));

    // Open the modal
    this.modal.modal("show");
  }

  setContent() {
    // Get item user clicked on
    let item = this.dataSets[this.current.id];

    // Get list of leagues with history data for the item
    let leagues = this.getLeagues(item);
    this.current.league = leagues[0].name;

    // Add leagues as selector options
    this.createLeagueSelector(leagues);

    this.updateContent();

    // Hide buffer and show content
    this.setBufferVisibility(false);
  }

  updateContent() {
    // Format league data
    let currentHistory = this.getPayload();
    let currentFormatHistory = this.formatHistory(currentHistory);

    let data = {
      labels: currentFormatHistory.keys,
      series: []
    };

    switch (this.current.dataset) {
      case 1: data.series[0] = currentFormatHistory.vals.mean;   break;
      case 2: data.series[0] = currentFormatHistory.vals.median; break;
      case 3: data.series[0] = currentFormatHistory.vals.mode;   break;
      case 4: data.series[0] = currentFormatHistory.vals.daily;  break;
      case 5: data.series[0] = currentFormatHistory.vals.current;break;
    }

    this.current.chart = new Chartist.Line('.ct-chart', data, this.chartOptions);

    // Update modal table
    $("#modal-mean",     this.modal).html( formatNum(currentHistory.mean)   );
    $("#modal-median",   this.modal).html( formatNum(currentHistory.median) );
    $("#modal-mode",     this.modal).html( formatNum(currentHistory.mode)   );
    $("#modal-total",    this.modal).html( formatNum(currentHistory.total)  );
    $("#modal-daily",    this.modal).html( formatNum(currentHistory.daily)  );
    $("#modal-current",  this.modal).html( formatNum(currentHistory.current)  );
    $("#modal-exalted",  this.modal).html( formatNum(currentHistory.exalted));
  }

  static findItem(id) {
    for (let i = 0; i < ITEMS.length; i++) {
      if (ITEMS[i].id === id) {
         return ITEMS[i];
      }
    }

    return null;
  }

  setBufferVisibility(visible) { 
    if (visible) {
      $("#modal-body-buffer", this.modal).removeClass("d-none").addClass("d-flex");
      $("#modal-body-content", this.modal).addClass("d-none").removeClass("d-flex");
    } else {
      $("#modal-body-buffer", this.modal).addClass("d-none").removeClass("d-flex");
      $("#modal-body-content", this.modal).removeClass("d-none").addClass("d-flex");
    }
  }

  buildNameField(item) {
    // If item is enchantment, insert enchant values for display purposes
    if (item.enchant) {
      // Min roll
      if (item.name.includes("#") && item.enchant.min !== null) {
        item.name = item.name.replace("#", item.enchant.min);
      }
      
      // Max roll
      if (item.name.includes("#") && item.enchant.max !== null) {
        item.name = item.name.replace("#", item.enchant.max); 
      }
    }

    // Begin builder
    let builder = item.name;
  
    if (item.type) {
      builder += "<span class='subtext-1'>, " + item.type + "</span>";;
    }
  
    if (item.frame === 9) {
      builder = "<span class='item-foil'>" + builder + "</span>";
    } else if (item.base) {
      if (item.base.shaper) {
        builder = "<span class='item-shaper'>" + builder + "</span>";
      } else if (item.base.elder) {
        builder = "<span class='item-elder'>" + builder + "</span>";
      }
    }
    

    if (item.variation) { 
      builder += " <span class='badge custom-badge-gray ml-1'>" + item.variation + "</span>";
    } 
    
    if (item.map && item.map.tier) {
      builder += " <span class='badge custom-badge-gray ml-1'>Tier " + item.map.tier + "</span>";
    } 
  
    if (item.itemLevel) {
      builder += " <span class='badge custom-badge-gray ml-1'>iLvl " + item.itemLevel + "</span>";
    } 
    
    if (item.links) {
      builder += " <span class='badge custom-badge-gray ml-1'>" + item.links + " Link</span>";
    }
  
    if (item.gem) {
      builder += "<span class='badge custom-badge-gray ml-1'>Lvl " + item.gem.level + "</span>";
      builder += "<span class='badge custom-badge-gray ml-1'>" + item.gem.quality + " quality</span>";
  
      if (item.gem.corrupted) {
        builder += "<span class='badge custom-badge-red ml-1'>Corrupted</span>";
      }
    }
  
    return builder;
  }

  formatIcon(item) {
    let icon = item.icon.replace("http://", "https://");
  
    if (item.base) {
      if (item.base.shaper) {
        icon += "&shaper=1";
      } else if (item.base.elder) {
        icon += "&elder=1";
      }
    }
  
    // Flaks have no params
    if (!icon.includes("?")) {
      return icon;
    }
  
    let splitIcon = icon.split("?");
    let splitParams = splitIcon[1].split("&");
    let newParams = "";
  
    for (let i = 0; i < splitParams.length; i++) {
      switch (splitParams[i].split("=")[0]) {
        case "scale": 
          break;
        default:
          newParams += "&" + splitParams[i];
          break;
      }
    }
  
    if (newParams) {
      icon = splitIcon[0] + "?" + newParams.substr(1);
    } else {
      icon = splitIcon[0];
    }

    return icon;
  }

  createLeagueSelector(leagues) {
    let builder = "";
  
    for (let i = 0; i < leagues.length; i++) {
      let display = leagues[i].active ? leagues[i].display : "● " + leagues[i].display;
  
      builder += "<option value='{{value}}'>{{name}}</option>"
        .replace("{{value}}", leagues[i].name)
        .replace("{{name}}", display);
    }
  
    $("#modal-leagues", this.modal).html(builder);
  }

  getPayload() {
    for (let i = 0; i < this.dataSets[this.current.id].data.length; i++) {
      if (this.dataSets[this.current.id].data[i].league.name === this.current.league) {
        return this.dataSets[this.current.id].data[i];
      }
    }
  
    return null;
  }

  getLeagues(item) {
    let leagues = [];

    for (let i = 0; i < item.data.length; i++) {
      leagues.push({
        name: item.data[i].league.name,
        display: item.data[i].league.display,
        active: item.data[i].league.active
      });
    }
  
    return leagues;
  }

  formatHistory(leaguePayload) {
    let keys = [];
    let vals = {
      mean:   [],
      median: [],
      mode:   [],
      daily:  [],
      current: [],
    };
  
    const msInDay = 86400000;
    let firstDate = null, lastDate = null;
    let totalDays = null, elapDays = null;
    let startDate = null, endDate  = null;
    let daysMissingStart = 0, daysMissingEnd = 0;
    let startEmptyPadding = 0;
  
    // If there are any history entries for this league, find the first and last date
    if (leaguePayload.history.length) {
      firstDate = new Date(leaguePayload.history[0].time);
      lastDate = new Date(leaguePayload.history[leaguePayload.history.length - 1].time);
    }
  
    // League should always have a start date
    if (leaguePayload.league.start) {
      startDate = new Date(leaguePayload.league.start);
    }
  
    // Permanent leagues don't have an end date
    if (leaguePayload.league.end) {
      endDate = new Date(leaguePayload.league.end);
    }
  
    // Find duration for non-permanent leagues
    if (startDate && endDate) {
      let diff = Math.abs(endDate.getTime() - startDate.getTime());
      totalDays = Math.floor(diff / msInDay);
      
      if (leaguePayload.league.active) {
        let diff = Math.abs(new Date().getTime() - startDate.getTime());
        elapDays = Math.floor(diff / msInDay);
      } else {
        elapDays = totalDays;
      }
    }
  
    // Find how many days worth of data is missing from the league start
    if (leaguePayload.league.id > 2) {
      if (firstDate && startDate) {
        let diff = Math.abs(firstDate.getTime() - startDate.getTime());
        daysMissingStart = Math.floor(diff / msInDay);
      }
    } 
  
    // Find how many days worth of data is missing from the league end, if league has ended
    if (leaguePayload.league.active) {
      // League is active, compare time of last entry to right now
      if (lastDate) {
        let diff = Math.abs(new Date().getTime() - lastDate.getTime());
        daysMissingEnd = Math.floor(diff / msInDay);
      }
    } else {
      // League has ended, compare time of last entry to time of league end
      if (lastDate && endDate) {
        let diff = Math.abs(lastDate.getTime() - endDate.getTime());
        daysMissingEnd = Math.floor(diff / msInDay);
      }
    }
  
    // Find number of ticks the graph should be padded with empty entries on the left
    if (leaguePayload.league.id > 2) {
      if (totalDays !== null && elapDays !== null) {
        startEmptyPadding = totalDays - elapDays;
      }
    } else {
      startEmptyPadding = 120 - leaguePayload.history.length;
    }
  
  
    // Right, now that we have all the dates, durations and counts we can start 
    // building the actual payload
  
  
    // Bloat using 'null's the amount of days that should not have a tooltip
    if (startEmptyPadding) {
      for (let i = 0; i < startEmptyPadding; i++) {
        vals.mean.push(null);
        vals.median.push(null);
        vals.mode.push(null);
        vals.daily.push(null);
        vals.current.push(null);
        keys.push("");
      }
    }
    
    // If entries are missing before the first entry, fill with "No data"
    if (daysMissingStart) {
      let date = new Date(startDate);
  
      for (let i = 0; i < daysMissingStart; i++) {
        vals.mean.push(0);
        vals.median.push(0);
        vals.mode.push(0);
        vals.daily.push(0);
        vals.current.push(0);
        keys.push(this.formatDate(date.addDays(i)));
      }
    }
  
    // Add actual history data
    for (let i = 0; i < leaguePayload.history.length; i++) {
      const entry = leaguePayload.history[i];
  
      // Add current entry values
      vals.mean.push(Math.round(entry.mean * 100) / 100);
      vals.median.push(Math.round(entry.median * 100) / 100);
      vals.mode.push(Math.round(entry.mode * 100) / 100);
      vals.daily.push(entry.daily);
      vals.current.push(entry.current);
      keys.push(this.formatDate(entry.time));
  
      // Check if there are any missing entries between the current one and the next one
      if (i + 1 < leaguePayload.history.length) {
        const nextEntry = leaguePayload.history[i + 1];
  
        // Get dates
        let currentDate = new Date(entry.time);
        let nextDate = new Date(nextEntry.time);
  
        // Get difference in days between entries
        let timeDiff = Math.abs(nextDate.getTime() - currentDate.getTime());
        let diffDays = Math.floor(timeDiff / (1000 * 3600 * 24)) - 1; 
  
        // Fill missing days with "No data" (if any)
        for (let i = 0; i < diffDays; i++) {
          vals.mean.push(0);
          vals.median.push(0);
          vals.mode.push(0);
          vals.daily.push(0);
          vals.current.push(0);
          keys.push(this.formatDate(currentDate.addDays(i + 1)));
        }
      }
    }
  
    // If entries are missing after the first entry, fill with "No data"
    if (daysMissingEnd && lastDate) {
      let date = new Date(lastDate);
      date.setDate(date.getDate() + 1);
  
      for (let i = 0; i < daysMissingEnd; i++) {
        vals.mean.push(0);
        vals.median.push(0);
        vals.mode.push(0);
        vals.daily.push(0);
        vals.current.push(0);
        keys.push(this.formatDate(date.addDays(i)));
      }
    }
  
    // Add current values
    if (leaguePayload.league.active) {
      vals.mean.push(Math.round(leaguePayload.mean * 100) / 100);
      vals.median.push(Math.round(leaguePayload.median * 100) / 100);
      vals.mode.push(Math.round(leaguePayload.mode * 100) / 100);
      vals.daily.push(leaguePayload.daily);
      vals.current.push(leaguePayload.current);
      keys.push("Now");
    }
  
    // Return generated data
    return {
      'keys': keys,
      'vals': vals
    }
  }

  formatDate(date) {
    const MONTH_NAMES = [
      "Jan", "Feb", "Mar", "Apr", "May", "Jun", 
      "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    ];
  
    let s = new Date(date);

    return s.getDate() + " " + MONTH_NAMES[s.getMonth()];
  }
}

// Default item search filter options
var FILTER = {
  league: SERVICE_leagues[0],
  category: null,
  group: "all",
  showLowConfidence: false,
  links: null,
  rarity: null,
  tier: null,
  search: null,
  gemLvl: null,
  gemQuality: null,
  gemCorrupted: null,
  ilvl: null,
  influence: null,
  parseAmount: 150,
  sortFunction: null
};

var ITEMS = [];
var LEAGUES = null;
var INTERVAL;
var DETMODAL = new DetailsModal();

// Re-used icon urls
const ICON_ENCHANTMENT = "https://web.poecdn.com/image/Art/2DItems/Currency/Enchantment.png?scale=1&w=1&h=1";
const ICON_EXALTED = "https://web.poecdn.com/image/Art/2DItems/Currency/CurrencyAddModToRare.png?scale=1&w=1&h=1";
const ICON_CHAOS = "https://web.poecdn.com/image/Art/2DItems/Currency/CurrencyRerollRare.png?scale=1&w=1&h=1";

var TEMPLATE_imgContainer = "<span class='img-container img-container-sm text-center mr-1'><img src={{img}}></span>";

$(document).ready(function() {
  parseQueryParams();

  makeGetRequest();
  defineListeners();
}); 

//------------------------------------------------------------------------------------------------------------
// Data prep
//------------------------------------------------------------------------------------------------------------

function parseQueryParams() {
  let tmp;

  if (tmp = parseQueryParam('league')) {
    tmp = getServiceLeague(tmp);

    if (tmp) {
      FILTER.league = tmp;
      $("#search-league").val(FILTER.league.name);
    }
  }

  updateQueryParam("league", FILTER.league.name);

  if (tmp = parseQueryParam('category')) {
    FILTER.category = tmp;
  } else {
    FILTER.category = "currency";
    updateQueryParam("category", FILTER.category);
  }

  if (tmp = parseQueryParam('group')) {
    FILTER.group = tmp;
    $('#search-group').val(tmp);
  }

  if (tmp = parseQueryParam('search')) {
    FILTER.search = tmp;
  }

  if (tmp = parseQueryParam('confidence')) {
    FILTER.showLowConfidence = true;
  }

  if (tmp = parseQueryParam('rarity')) {
    if      (tmp === "unique") FILTER.rarity =    3;
    else if (tmp ===  "relic") FILTER.rarity =    9;
  }

  if (tmp = parseQueryParam('links')) {
    if (tmp ===  "all") FILTER.links = -1;
    else FILTER.links = parseInt(tmp);
  }

  if (tmp = parseQueryParam('tier')) {
    $('#select-tier').val(tmp);
    if (tmp === "none") FILTER.tier = 0;
    else FILTER.tier = parseInt(tmp);
  }

  if (tmp = parseQueryParam('corrupted')) {
    FILTER.gemCorrupted = tmp === "true";
  }

  if (tmp = parseQueryParam('lvl')) {
    $('#select-level').val(tmp);
    FILTER.gemLvl = parseInt(tmp);
  }

  if (tmp = parseQueryParam('quality')) {
    $('#select-quality').val(tmp);
    FILTER.gemQuality = parseInt(tmp);
  }

  if (tmp = parseQueryParam('ilvl')) {
    $('#select-ilvl').val(tmp);
    FILTER.ilvl = parseInt(tmp);
  }

  if (tmp = parseQueryParam('influence')) {
    $('#select-influence').val(tmp);
    FILTER.influence = tmp;
  }

  if (tmpCol = parseQueryParam('sortby')) {
    let element;

    // Find column that matches the provided param
    $(".sort-column").each(function( index ) {
      if (this.innerHTML.toLowerCase() === tmpCol) {
        element = this;
        return;
      }
    });

    // If there was no match then clear the browser's query params
    if (!element) {
      updateQueryParam("sortby", null);
      updateQueryParam("sortorder", null);
      return;
    }

    // Get column name
    let col = element.innerHTML.toLowerCase();

    // If there was a sortorder query param as well
    if (tmpOrder = parseQueryParam('sortorder')) {
      let order = null;
      let color;

      // Only two options
      if (tmpOrder === "descending") {
        order = "descending";
        color = "custom-text-green";
      } else if (tmpOrder === "ascending") {
        order = "ascending";
        color = "custom-text-red";
      }

      // If user provided a third option, count that as invalid and
      // clear the browser's query params
      if (!order) {
        updateQueryParam("sortby", null);
        updateQueryParam("sortorder", null);
        return;
      }

      // User-provided params were a-ok, set the sort function and
      // add indication which col is being sorted
      console.log("Sorting: " + col + " " + order);
      FILTER.sortFunction = getSortFunc(col, order);
      $(element).addClass(color);
    }
  }
}

function defineListeners() {
  // League
  $("#search-league").on("change", function(){
    let tmp = getServiceLeague($(":selected", this).val());
    if (!tmp) return;
    
    FILTER.league = tmp;
    console.log("Selected league: " + FILTER.league.name);
    updateQueryParam("league", FILTER.league.name);
    makeGetRequest();
  });

  // Group
  $("#search-group").change(function(){
    FILTER.group = $(this).find(":selected").val();
    console.log("Selected group: " + FILTER.group);
    updateQueryParam("group", FILTER.group);
    sortResults();
  });

  // Load all button
  $("#button-showAll").on("click", function(){
    console.log("Button press: show all");
    $(this).hide();
    FILTER.parseAmount = -1;
    sortResults();
  });

  // Searchbar
  $("#search-searchbar").on("input", function(){
    FILTER.search = $(this).val().toLowerCase().trim();
    console.log("Search: " + FILTER.search);
    updateQueryParam("search", FILTER.search);
    sortResults();
  });

  // Low confidence
  $("#radio-confidence").on("change", function(){
    let option = $("input:checked", this).val() === "true";
    console.log("Show low daily: " + option);
    FILTER.showLowConfidence = option;
    updateQueryParam("confidence", option);
    sortResults();
  });

  // Rarity
  $("#radio-rarity").on("change", function(){
    FILTER.rarity = $(":checked", this).val();
    console.log("Rarity filter: " + FILTER.rarity);
    updateQueryParam("rarity", FILTER.rarity);

    if      (FILTER.rarity ===    "all") FILTER.rarity = null;
    else if (FILTER.rarity === "unique") FILTER.rarity =    3;
    else if (FILTER.rarity ===  "relic") FILTER.rarity =    9;
    
    sortResults();
  });
  
  // Item links
  $("#radio-links").on("change", function(){
    FILTER.links = $(":checked", this).val();
    console.log("Link filter: " + FILTER.links);
    updateQueryParam("links", FILTER.links);
    if      (FILTER.links === "none") FILTER.links = null;
    else if (FILTER.links ===  "all") FILTER.links = -1;
    else FILTER.links = parseInt(FILTER.links);
    sortResults();
  });

  // Map tier
  $("#select-tier").on("change", function(){
    FILTER.tier = $(":selected", this).val();
    console.log("Map tier filter: " + FILTER.tier);
    updateQueryParam("tier", FILTER.tier);
    if (FILTER.tier === "all") FILTER.tier = null;
    else if (FILTER.tier === "none") FILTER.tier = 0;
    else FILTER.tier = parseInt(FILTER.tier);
    sortResults();
  });

  // Gem level
  $("#select-level").on("change", function(){
    FILTER.gemLvl = $(":selected", this).val();
    console.log("Gem lvl filter: " + FILTER.gemLvl);
    if (FILTER.gemLvl === "all") FILTER.gemLvl = null;
    else FILTER.gemLvl = parseInt(FILTER.gemLvl);
    updateQueryParam("lvl", FILTER.gemLvl);
    sortResults();
  });

  // Gem quality
  $("#select-quality").on("change", function(){
    FILTER.gemQuality = $(":selected", this).val();
    console.log("Gem quality filter: " + FILTER.gemQuality);
    if (FILTER.gemQuality === "all") FILTER.gemQuality = null;
    else FILTER.gemQuality = parseInt(FILTER.gemQuality);
    updateQueryParam("quality", FILTER.gemQuality);
    sortResults();
  });

  // Gem corrupted
  $("#radio-corrupted").on("change", function(){
    FILTER.gemCorrupted = $(":checked", this).val();
    console.log("Gem corruption filter: " + FILTER.gemCorrupted);
    if (FILTER.gemCorrupted === "all") FILTER.gemCorrupted = null;
    else FILTER.gemCorrupted = FILTER.gemCorrupted === "true";
    updateQueryParam("corrupted", FILTER.gemCorrupted);
    sortResults();
  });

  // Base iLvl
  $("#select-ilvl").on("change", function(){
    let ilvl = $(":selected", this).val();
    console.log("Base iLvl filter: " + ilvl);
    FILTER.ilvl = ilvl === "all" ? null : parseInt(ilvl);
    updateQueryParam("ilvl", ilvl);
    sortResults();
  });

  // Base influence
  $("#select-influence").on("change", function(){
    FILTER.influence = $(":selected", this).val();
    console.log("Influence filter: " + FILTER.influence);
    if (FILTER.influence == "all") FILTER.influence = null; 
    updateQueryParam("influence", FILTER.influence);
    sortResults();
  });

  // Expand row
  $("#searchResults > tbody").delegate("tr", "click", function(event) {
    DETMODAL.onRowClick(event);
  });

  // Live search toggle
  $("#live-updates").on("change", function(){
    let live = $("input[name=live]:checked", this).val() === "true";
    console.log("Live updates: " + live);

    if (live) {
      $("#progressbar-live").css("animation-name", "progressbar-live").show();
      INTERVAL = setInterval(timedRequestCallback, 60 * 1000);
    } else {
      $("#progressbar-live").css("animation-name", "").hide();
      clearInterval(INTERVAL);
    }
  });

  // Sort
  $(".sort-column").on("click", function(){
    // Get col name
    let col = $(this)[0].innerHTML.toLowerCase();
    // Get order tag, if present
    let order = $(this).attr("order");
    let color = null;

    // Remove all data from all sort columns
    $(".sort-column")
      .attr("class", "sort-column")
      .attr("order", null);

    // Toggle descriptions and orders
    if (!order) {
      order = "descending";
      color = "custom-text-green";
    } else if (order === "descending") {
      order = "ascending";
      color = "custom-text-red";
    } else if (order === "ascending") {
      updateQueryParam("sortby", null);
      updateQueryParam("sortorder", null);
      console.log("Sorting: default");
      FILTER.sortFunction = getSortFunc(null, "descending");
      sortResults();
      return;
    }

    updateQueryParam("sortby", col);
    updateQueryParam("sortorder", order);

    // Set clicked col's data
    $(this).attr("order", order);
    $(this).addClass(color);

    console.log("Sorting: " + col + " " + order);
    FILTER.sortFunction = getSortFunc(col, order);

    sortResults();
  });
}

//------------------------------------------------------------------------------------------------------------
// Requests
//------------------------------------------------------------------------------------------------------------

function makeGetRequest() {
  $("#searchResults tbody").empty();
  $("#buffering-main").show();
  $("#button-showAll").hide();
  $(".buffering-msg").remove();

  let request = $.ajax({
    url: "https://api.poe.watch/get",
    data: {
      league: FILTER.league.name, 
      category: FILTER.category
    },
    type: "GET",
    async: true,
    dataTypes: "json"
  });

  request.done(function(json) {
    console.log("Got " + json.length + " items from request");
    $("#buffering-main").hide();
    $(".buffering-msg").remove();

    ITEMS = json;
    sortResults();
  });

  request.fail(function(response) {
    ITEMS = [];

    $(".buffering-msg").remove();

    let buffering = $("#buffering-main");
    buffering.hide();

    let msg;
    if (response.status) {
      msg = "<div class='buffering-msg align-self-center mb-2'>" + response.responseJSON.error + "</div>";
    } else {
      msg = "<div class='buffering-msg align-self-center mb-2'>Too many requests, please wait 60 seconds.</div>";
    }

    buffering.after(msg);
  });
}

function timedRequestCallback() {
  console.log("Automatic update");

  var request = $.ajax({
    url: "https://api.poe.watch/get",
    data: {
      league: FILTER.league.name, 
      category: FILTER.category
    },
    type: "GET",
    async: true,
    dataTypes: "json"
  });

  request.done(function(json) {
    console.log("Got " + json.length + " items from request");

    ITEMS = json;
    sortResults();
  });

  request.fail(function(response) {
    $("#searchResults tbody").empty();
    buffering.after("<div class='buffering-msg align-self-center mb-2'>" + response.responseJSON.error + "</div>");
  });
}

//------------------------------------------------------------------------------------------------------------
// Sorting. This can probably be done better. If you know how, let me know.
//------------------------------------------------------------------------------------------------------------

function getSortFunc(col, order) {
  switch (col) {
    case "change":
      return order === "descending" 
      ? (a, b) => {
        if (a.change > b.change) return -1;
        if (a.change < b.change) return 1;
        return 0;
      } 
      : (a, b) => {
        if (a.change < b.change) return -1;
        if (a.change > b.change) return 1;
        return 0;
      };
    case "daily":
      return order === "descending" 
      ? (a, b) => {
        if (a.daily > b.daily) return -1;
        if (a.daily < b.daily) return 1;
        return 0;  
      }
      : (a, b) => {
        if (a.daily < b.daily) return -1;
        if (a.daily > b.daily) return 1;
        return 0;
      };
    case "total":
      return order === "descending" 
      ? (a, b) => {
        if (a.total > b.total) return -1;
        if (a.total < b.total) return 1;
        return 0;
      }  
      : (a, b) => {
        if (a.total < b.total) return -1;
        if (a.total > b.total) return 1;
        return 0;
      };
    case "item":
      return order === "descending" 
      ? (a, b) => {
        if (a.name > b.name) return -1;
        if (a.name < b.name) return 1;
        return 0;
      }   
      : (a, b) => {
        if (a.name < b.name) return -1;
        if (a.name > b.name) return 1;
        return 0;
      };
    default:
      return order === "descending" 
      ? (a, b) => {
        if (a.mean > b.mean) return -1;
        if (a.mean < b.mean) return 1;
        return 0;
      }  
      : (a, b) => {
        if (a.mean < b.mean) return -1;
        if (a.mean > b.mean) return 1;
        return 0;
      }
  }
}

//------------------------------------------------------------------------------------------------------------
// Utility functions
//------------------------------------------------------------------------------------------------------------

function formatNum(num) {
  const numberWithCommas = (x) => {
    var parts = x.toString().split(".");
    parts[0] = parts[0].replace(/\B(?=(\d{3})+(?!\d))/g, ",");
    return parts.join(".");
  }

  if (num === null) {
    return 'Unavailable';
  } else return numberWithCommas(Math.round(num * 100) / 100);
}

function updateQueryParam(key, value) {
  switch (key) {
    case "confidence": value = value === false        ? null : value;   break;
    case "search":     value = value === ""           ? null : value;   break;
    case "rarity":     value = value === "all"        ? null : value;   break;
    case "corrupted":  value = value === "all"        ? null : value;   break;
    case "quality":    value = value === "all"        ? null : value;   break;
    case "lvl":        value = value === "all"        ? null : value;   break;
    case "links":      value = value === "none"       ? null : value;   break;
    case "group":      value = value === "all"        ? null : value;   break;
    case "tier":       value = value === "all"        ? null : value;   break;
    case "influence":  value = value === "all"        ? null : value;   break;
    default:           break;
  }
  
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

function parseQueryParam(key) {
  let url = window.location.href;
  key = key.replace(/[\[\]]/g, '\\$&');
  
  var regex = new RegExp('[?&]' + key + '(=([^&#]*)|&|#|$)'),
      results = regex.exec(url);
      
  if (!results   ) return null;
  if (!results[2]) return   '';

  return decodeURIComponent(results[2].replace(/\+/g, ' '));
}

Date.prototype.addDays = function(days) {
  var date = new Date(this.valueOf());
  date.setDate(date.getDate() + days);
  return date;
}

function getServiceLeague(league) {
  for (let i = 0; i < SERVICE_leagues.length; i++) {
    if (SERVICE_leagues[i].name === league) {
      return SERVICE_leagues[i];
    }
  }

  return null;
}

function getItem(id) {
  for (let i = 0; i < ITEMS.length; i++) {
    if (ITEMS[i].id === id) {
      return ITEMS[i];
    }
  }

  return null;
}

//------------------------------------------------------------------------------------------------------------
// Itetm sorting and searching
//------------------------------------------------------------------------------------------------------------

function sortResults() {
  // Empty the table
  let table = $("#searchResults");
  $("tbody", table).empty();

  let count = 0, matches = 0;

  if (FILTER.sortFunction) {
    ITEMS.sort(FILTER.sortFunction);
  }

  for (let i = 0; i < ITEMS.length; i++) {
    const item = ITEMS[i];

    // Skip parsing if item should be hidden according to filters
    if (checkHideItem(item)) {
      continue;
    }

    matches++;

    // Stop if specified item limit has been reached
    if ( FILTER.parseAmount < 0 || count < FILTER.parseAmount ) {
      // If item has not been parsed, parse it 
      if ( !('tableData' in item) ) {
        item.tableData = new ItemRow(item);
      }

      // Append generated table data to buffer
      table.append(item.tableData.row);
      count++;
    }
  }

  $(".buffering-msg").remove();

  if (count < 1) {
    let msg = "<div class='buffering-msg align-self-center mb-2'>No results</div>";
    $("#buffering-main").after(msg);
  }

  let loadAllBtn = $("#button-showAll");
  if (FILTER.parseAmount > 0 && matches > FILTER.parseAmount) {
    loadAllBtn.text("Show all (" + (matches - FILTER.parseAmount) + " items)");
    loadAllBtn.show();
  } else {
    loadAllBtn.hide();
  }
}

function checkHideItem(item) {
  // Hide low confidence items
  if (!FILTER.showLowConfidence && FILTER.league.active && item.daily < 5) {
    return true;
  }

  // String search
  if (FILTER.search) {
    if (item.name.toLowerCase().indexOf(FILTER.search) === -1) {
      if (item.type) {
        if (item.type.toLowerCase().indexOf(FILTER.search) === -1) {
          return true;
        }
      } else {
        return true;
      }
    }
  }

  // Hide groups
  if (FILTER.group !== "all" && FILTER.group !== item.group) {
    return true;
  }

  // Hide mismatching rarities
  if (FILTER.rarity) {
    if (FILTER.rarity !== item.frame) {
      return true;
    }
  }

  // Hide items with different links
  if (FILTER.links === null) {
    if (item.links !== null) {
      return true;
    }
  } else if (FILTER.links > 0) {
    if (item.links !== FILTER.links) {
      return true;
    }
  }

  // Sort gems, I guess
  if (FILTER.category === "gem" && item.gem) {
    if (FILTER.gemLvl !== null && item.gem.level != FILTER.gemLvl) return true;
    if (FILTER.gemQuality !== null && item.gem.quality != FILTER.gemQuality) return true;
    if (FILTER.gemCorrupted !== null && item.gem.corrupted != FILTER.gemCorrupted) return true;

  } else if (FILTER.category === "map") {
    if (FILTER.tier !== null) {
      if (FILTER.tier === 0) {
        if (item.map && item.map.tier !== null) return true;
      } else if (item.map.tier !== FILTER.tier) return true;
    }

  } else if (FILTER.category === "base" && item.base) {
    // Check base influence
    if (FILTER.influence !== null) {
      if (FILTER.influence === "none") {
        if (item.base.shaper || item.base.elder) return true;
      } else if (FILTER.influence === "either") {
        if (!item.base.shaper && !item.base.elder) return true;
      } else if (FILTER.influence === "shaper" && !item.base.shaper) {
        return true;
      } else if (FILTER.influence === "elder" && !item.base.elder) {
        return true;
      }
    }

    // Check base ilvl
    if (item.base.itemLevel !== null && FILTER.ilvl !== null) {
      if (item.base.itemLevel != FILTER.ilvl) {
        return true;
      }
    }
  }

  return false;
}

