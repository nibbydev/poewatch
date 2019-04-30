/*
  There's not much here except for some poorly written JS functions. And since you're 
  already here, it can't hurt to take a look at http://youmightnotneedjquery.com/
*/

// Eh for development, i guess?
const API_URL = "https://api.poe.watch/";
const SPARK_LINE_OPTIONS = {
  pad_y: 2,
  width: 60,
  height: 30,
  radius: 0.2
};
const MODAL_CHART_OPTIONS = {
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

/**
 * One item row on the page
 */
class ItemRow {
  constructor (item) {
    this.item = item;

    // Build row elements
    let rowData = [
      this.buildNameField(),
      this.buildGemFields(),
      this.buildBaseFields(),
      this.buildMapFields(),
      this.buildSparkField(),
      this.buildPriceFields(),
      this.buildChangeField(),
      this.buildDailyField(),
      this.buildTotalField()
    ].join("");

    this.row = `<tr value=${item.id}>${rowData}</tr>`;
  }

  buildNameField() {
    let color, type, variation, links, icon, name;

    // Use TLS for icons for that sweet, sweet secure site badge
    icon = this.item.icon.replace("http://", "https://");

    // If item is base
    if (this.item.category === 'base') {
      // Shaper or elder
      if (this.item.baseIsShaper) {
        icon += "&shaper=1";
        color = "item-shaper";
      } else if (this.item.baseIsElder) {
        icon += "&elder=1";
        color= "item-elder";
      }
    }

    // If color was not set and item is foil
    if (!color && this.item.frame === 9) {
      color = "item-foil";
    }

    // If item is enchantment, insert enchant values to name
    if (this.item.category === 'enchantment') {
      // Min roll
      if (this.item.name.includes("#") && this.item.enchantMin !== undefined) {
        name = this.item.name.replace("#", this.item.enchantMin);
      }
      
      // Max roll
      if (this.item.name.includes("#") && this.item.enchantMax !== undefined) {
        name = this.item.name.replace("#", this.item.enchantMax);
      }
    }

    // If item has a base type
    if (this.item.type) {
      type = ` <span class='subtext-1'>${this.item.type}</span>`;
    } else type = '';

    // If item has links
    if (this.item.linkCount) {
      links = ` <span class='badge custom-badge-gray ml-1'>${this.item.linkCount} link</span>`;
    } else links = '';

    // If item has a variation
    if (this.item.variation) {
      variation = ` <span class='badge custom-badge-gray ml-1'>${this.item.variation}</span>`;
    } else variation = '';

    // Create the name container
    return `
    <td>
      <div class='d-flex align-items-center'>
        <span class='img-container img-container-sm text-center mr-1'><img src="${this.item.icon}"></span>
        <span class='cursor-pointer ${color}'>${name || this.item.name}${type}</span>${variation}${links}
      </div>
    </td>`.trim();
  }
  
  buildGemFields() {
    // Don't run if item is not a gem
    if (this.item.category !== 'gem') {
      return '';
    }

    let color, corrupted;

    if (this.item.gemIsCorrupted) {
      color = 'red';
      corrupted = '✓';
    } else {
      color = 'green';
      corrupted = '✕';
    }
  
    return `
    <td>
        <span class='badge custom-badge-block custom-badge-gray'>${this.item.gemLevel}</span>
    </td>
    <td>
        <span class='badge custom-badge-block custom-badge-gray'>${this.item.gemQuality}</span>
    </td>
    <td>
        <span class='badge custom-badge-${color}'>${corrupted}</span>
    </td>`.trim();
  }
  
  buildBaseFields() {
    // Don't run if item is not a base
    if (this.item.category !== 'base') {
      return "";
    }

    return `
    <td class='nowrap'>
      <span class='badge custom-badge-block custom-badge-gray'>${this.item.baseItemLevel}</span>
    </td>`.trim();
  }
  
  buildMapFields() {
    // Don't run if item is not a map
    if (this.item.category !== 'map') {
      return '';
    }

    let tier;
    if (this.item.mapTier !== null) {
      tier = `<span class='badge custom-badge-block custom-badge-gray'>${this.item.mapTier}</span>`;
    }

    return `<td class='nowrap'>${tier || ''}</td>`;
  }

  buildSparkField() {
    /**
     * Inner-function to stop execution halfway
     *
     * @param history Valid history array
     * @returns {null}
     */
    function buildSpark(history) {
      // If there is no history (eg old leagues)
      if (!history){
        return null;
      }

      // Count the number of elements that are not null
      let count = 0;
      for (let i = 0; i < 7; i++) {
        if (history[i] !== null) {
          count++;
        }
      }

      // Can't display a sparkline with 1 value
      if (count < 2) {
        return null;
      }

      // Find first price from the left that is not null
      let lastPrice = null;
      for (let i = 0; i < 7; i++) {
        if (history[i] !== null) {
          lastPrice = history[i];
          break;
        }
      }

      // Calculate each value's change %-relation to current price
      let changes = [];
      for (let i = 0; i < 7; i++) {
        if (history[i] > 0) {
          changes[i] = Math.round((1 - (lastPrice / history[i])) * 100);
        }
      }

      // Generate sparkline html
      return ItemRow.genSparkSVG(SPARK_LINE_OPTIONS, changes);
    }

    let spark = buildSpark(this.item.history);

    // Return as template
    return `<td class='d-none d-md-flex'>${spark || ''}</td>`;
  }
  
  buildPriceFields() {
    const chaos = ItemRow.roundPrice(this.item.mean);
    const exalt = ItemRow.roundPrice(this.item.exalted);
    const hideExalted = this.item.exalted < 1 ? 'd-none' : '';

    return `
    <td>
      <div class='pricebox'>
        <span class='img-container img-container-sm text-center mr-1'>
          <img src="https://web.poecdn.com/image/Art/2DItems/Currency/CurrencyRerollRare.png?scale=1&w=1&h=1">
        </span>
        ${chaos}
      </div>
    </td>
    <td class='d-none d-md-flex'>
      <div class='pricebox ${hideExalted}'>
        <span class='img-container img-container-sm text-center mr-1'>
          <img src="https://web.poecdn.com/image/Art/2DItems/Currency/CurrencyAddModToRare.png?scale=1&w=1&h=1">
        </span>
        ${exalt}
      </div>
    </td>`.trim();
  }
  
  buildChangeField() {
    let change = Math.round(this.item.change);
    let color;

    // Limit it
    if (change > 999) change = 999;
    else if (change < -999) change = -999;

    // Pick a color scheme
    if (change >= 100) {
      color = "green-ex";
    } else if (change <= -100) {
      color = "red-ex";
    } else if (change >= 30) {
      color = "green";
    } else if (change <= -30) {
      color = "red";
    } else if (change >= 15) {
      color = "green-lo";
    } else if (change <= -15) {
      color = "red-lo";
    } else {
      color = "gray";
    }

    return `
    <td>
        <span class='badge custom-badge-block custom-badge-${color}'>${change}%</span>
    </td>`.trim();
  }
  
  buildDailyField() {
    let color;

    if (FILTER.league.active) {
      if (this.item.daily >= 20) {
        color = "gray";
      } else if (this.item.daily >= 10) {
        color = "orange-lo";
      } else if (this.item.daily >= 5) {
        color = "red-lo";
      } else if (this.item.daily >= 0) {
        color = "red";
      }
    } else {
      color = "gray";
    }
  
    return `
    <td>
      <span class='badge custom-badge-block custom-badge-${color}'>
        ${this.item.daily}
      </span>
    </td>`.trim();
  }

  buildTotalField() {
    return `
    <td>
      <span class='badge custom-badge-block custom-badge-gray'>
        ${this.item.total}
      </span>
    </td>`.trim();
  }

  static roundPrice(price) {
    const numberWithCommas = (x) => {
      let parts = x.toString().split(".");
      parts[0] = parts[0].replace(/\B(?=(\d{3})+(?!\d))/g, ",");
      return parts.join(".");
    };
  
    return numberWithCommas(Math.round(price * 100) / 100);
  }

  static genSparkSVG(options, elements) {
    let maxElement = Math.max(...elements);
    let minElement = Math.min(...elements);
  
    // If there has been no change in the past week
    if (maxElement === minElement && minElement === 0) {
      maxElement = 1;
    }

    // Find step sizes in pixels
    let stepX = options.width / (elements.length - 1);
    let stepY = (options.height - options.pad_y*2) / (maxElement - minElement);
  
    // Create point array
    let pointBuilder = ["M "];
    for (let i = 0; i < elements.length; i++) {
      if (elements[i] !== null) {
        let x = stepX * i;
        let y = (options.height - elements[i]*stepY + minElement*stepY - options.pad_y/2).toFixed(3);
  
        pointBuilder.push(x, " ", y, " L ");
      }
    }
  
    // Remove trailing zero
    pointBuilder.pop();
    const points = ItemRow.roundSVGPathCorners(pointBuilder.join(""), options);

    return `
    <svg width="${options.width}" height="${options.height}" class="ct-chart-line">
      <g class="ct-series ct-series-a">
        <path d="${points}" class="ct-line" />
      </g>
    </svg>`.trim();
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

/**
 * Specific item modal
 */
class DetailsModal {
  constructor() {
    this.modal = $("#modal-details");

    // Contains all requested item data & history on current page
    this.dataSets = {};

    // Contains up to date league and item information
    this.current = {
      id: null,
      league: null,
      chart: null,
      dataset: 1
    };

    // League select listener
    $("#modal-leagues", this.modal).change(function(){
      MODAL.current.league = $(":selected", this).val();
      MODAL.getHistory();
    });
  
    // Dataset radio listener
    $("#modal-radio", this.modal).change(function(){
      const val = $("input[name=dataset]:checked", this).val();
      MODAL.current.dataset = parseInt(val);
      MODAL.updateContent();
    });
  }

  resetData() {
    // Clear leagues from selector
    $("#modal-leagues", this.modal).find('option').remove();

    // Dataset selection
    let $radios = $('#modal-radio').children();
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
    if (!id) {
      return;
    }

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
        url: API_URL + "item",
        data: {id: this.current.id},
        type: "GET",
        async: true,
        dataTypes: "json"
      });
    
      request.done(function(payload) {
        MODAL.dataSets[MODAL.current.id] = payload;
        MODAL.setContent();
      });
    }

    // Find item entry from initial get request
    let item = DetailsModal.findItem(this.current.id);

    // Set modal's icon and name while request might still be processing
    $("#modal-icon", this.modal).attr("src", item.icon);
    $("#modal-name", this.modal).html(DetailsModal.buildNameField(item));

    // Open the modal
    this.modal.modal("show");
  }

  setContent() {
    // Get item user clicked on
    let item = this.dataSets[this.current.id];

    // Get list of leagues for the item
    let leagues = DetailsModal.getLeagues(item);
    this.current.league = leagues[0].name;

    // Add leagues as selector options
    this.createLeagueSelector(leagues);

    // Get history data for the current league
    this.getHistory();

    // Hide buffer and show content
    this.setBufferVisibility(false);
  }

  /**
   * Requests history data for current league or gets it from memory
   */
  getHistory() {
    // Check if the data already exists
    if (this.checkLeagueHistoryExists()) {
      console.log('History from local');
      MODAL.updateContent();
      return;
    }

    // Prep request
    let request = $.ajax({
      url: API_URL + "itemhistory",
      data: {
        league: this.current.league,
        id: this.current.id
      },
      type: "GET",
      async: true,
      dataTypes: "json"
    });

    request.done(function(payload) {
      // Find associated league
      let league = MODAL.getCurrentItemLeague();

      league.history = payload;
      MODAL.updateContent();
    });
  }

  updateContent() {
    let league = this.getCurrentItemLeague();
    let currentFormatHistory = DetailsModal.formatHistory(league);

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

    this.current.chart = new Chartist.Line('.ct-chart', data, MODAL_CHART_OPTIONS);

    // Update modal table
    $("#modal-mean",     this.modal).html( formatNum(league.mean)   );
    $("#modal-median",   this.modal).html( formatNum(league.median) );
    $("#modal-mode",     this.modal).html( formatNum(league.mode)   );
    $("#modal-total",    this.modal).html( formatNum(league.total)  );
    $("#modal-daily",    this.modal).html( formatNum(league.daily)  );
    $("#modal-current",  this.modal).html( formatNum(league.current)  );
    $("#modal-exalted",  this.modal).html( formatNum(league.exalted));
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

  createLeagueSelector(leagues) {
    let builder = "";
  
    for (let i = 0; i < leagues.length; i++) {
      let display = leagues[i].active ? leagues[i].display : "● " + leagues[i].display;
      builder += `<option value='${leagues[i].name}'>${display}</option>`;
    }
  
    $("#modal-leagues", this.modal).html(builder);
  }

  static buildNameField(item) {
    // If item is enchantment, insert enchant values for display purposes
    if (item.category === 'enchant') {
      // Min roll
      if (item.name.includes("#") && item.enchantMin !== null) {
        item.name = item.name.replace("#", item.enchantMin);
      }

      // Max roll
      if (item.name.includes("#") && item.enchantMax !== null) {
        item.name = item.name.replace("#", item.enchantMax);
      }
    }

    // Begin builder
    let builder = item.name;

    if (item.type) {
      builder += "<span class='subtext-1'>, " + item.type + "</span>";;
    }

    if (item.frame === 9) {
      builder = "<span class='item-foil'>" + builder + "</span>";
    } else if (item.category === 'base') {
      if (item.baseIsShaper) {
        builder = "<span class='item-shaper'>" + builder + "</span>";
      } else if (item.baseIsElder) {
        builder = "<span class='item-elder'>" + builder + "</span>";
      }
    }

    if (item.variation) {
      builder += " <span class='badge custom-badge-gray ml-1'>" + item.variation + "</span>";
    }

    if (item.category === 'map' && item.mapTier) {
      builder += " <span class='badge custom-badge-gray ml-1'>Tier " + item.mapTier + "</span>";
    }

    if (item.baseItemLevel) {
      builder += " <span class='badge custom-badge-gray ml-1'>iLvl " + item.itemLevel + "</span>";
    }

    if (item.linkCount) {
      builder += " <span class='badge custom-badge-gray ml-1'>" + item.linkCount + " Link</span>";
    }

    if (item.category === 'gem') {
      builder += `<span class='badge custom-badge-gray ml-1'>Lvl ${item.gemLevel}</span>`;
      builder += `<span class='badge custom-badge-gray ml-1'>${item.gemQuality} quality</span>`;

      if (item.gemIsCorrupted) {
        builder += "<span class='badge custom-badge-red ml-1'>Corrupted</span>";
      }
    }

    return builder;
  }

  /**
   * Given the complete item api json, returns list of leagues for that item
   *
   * @param item
   * @returns {Array}
   */
  static getLeagues(item) {
    let leagues = [];

    for (let i = 0; i < item.leagues.length; i++) {
      leagues.push({
        name: item.leagues[i].name,
        display: item.leagues[i].display,
        active: item.leagues[i].active
      });
    }
  
    return leagues;
  }

  static formatHistory(league) {
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
    if (league.history.length) {
      firstDate = new Date(league.history[0].time);
      lastDate = new Date(league.history[league.history.length - 1].time);
    }
  
    // League should always have a start date
    if (league.start) {
      startDate = new Date(league.start);
    }
  
    // Permanent leagues don't have an end date
    if (league.end) {
      endDate = new Date(league.end);
    }
  
    // Find duration for non-permanent leagues
    if (startDate && endDate) {
      let diff = Math.abs(endDate.getTime() - startDate.getTime());
      totalDays = Math.floor(diff / msInDay);
      
      if (league.active) {
        let diff = Math.abs(new Date().getTime() - startDate.getTime());
        elapDays = Math.floor(diff / msInDay);
      } else {
        elapDays = totalDays;
      }
    }
  
    // Find how many days worth of data is missing from the league start
    if (league.id > 2) {
      if (firstDate && startDate) {
        let diff = Math.abs(firstDate.getTime() - startDate.getTime());
        daysMissingStart = Math.floor(diff / msInDay);
      }
    } 
  
    // Find how many days worth of data is missing from the league end, if league has ended
    if (league.active) {
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
    if (league.id > 2) {
      if (totalDays !== null && elapDays !== null) {
        startEmptyPadding = totalDays - elapDays;
      }
    } else {
      startEmptyPadding = 120 - league.history.length;
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
        keys.push(DetailsModal.formatDate(date.addDays(i)));
      }
    }
  
    // Add actual history data
    for (let i = 0; i < league.history.length; i++) {
      const entry = league.history[i];
  
      // Add current entry values
      vals.mean.push(Math.round(entry.mean * 100) / 100);
      vals.median.push(Math.round(entry.median * 100) / 100);
      vals.mode.push(Math.round(entry.mode * 100) / 100);
      vals.daily.push(entry.daily);
      vals.current.push(entry.current);
      keys.push(DetailsModal.formatDate(entry.time));
  
      // Check if there are any missing entries between the current one and the next one
      if (i + 1 < league.history.length) {
        const nextEntry = league.history[i + 1];
  
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
          keys.push(DetailsModal.formatDate(currentDate.addDays(i + 1)));
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
        keys.push(DetailsModal.formatDate(date.addDays(i)));
      }
    }
  
    // Add current values
    if (league.active) {
      vals.mean.push(Math.round(league.mean * 100) / 100);
      vals.median.push(Math.round(league.median * 100) / 100);
      vals.mode.push(Math.round(league.mode * 100) / 100);
      vals.daily.push(league.daily);
      vals.current.push(league.current);
      keys.push("Now");
    }
  
    // Return generated data
    return {
      'keys': keys,
      'vals': vals
    }
  }

  static formatDate(date) {
    const MONTH_NAMES = [
      "Jan", "Feb", "Mar", "Apr", "May", "Jun", 
      "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    ];
  
    let s = new Date(date);

    return s.getDate() + " " + MONTH_NAMES[s.getMonth()];
  }

  static findItem(id) {
    for (let i = 0; i < ITEMS.length; i++) {
      if (ITEMS[i].id === id) {
        return ITEMS[i];
      }
    }

    return null;
  }

  /**
   * Returns the current league object of the current item
   *
   * @returns {null|*} League object or null if does not exist
   */
  getCurrentItemLeague() {
    let item = this.dataSets[this.current.id];

    for (let i = 0; i < item.leagues.length; i++) {
      if (item.leagues[i].name === this.current.league) {
        return item.leagues[i];
      }
    }

    return null;
  }

  /**
   * Check whether a specific league's data has been downloaded for the current item
   *
   * @returns {boolean} True if exists, false if not
   */
  checkLeagueHistoryExists() {
    const leagues = this.dataSets[this.current.id].leagues;

    for (let i = 0; i < leagues.length; i++) {
      if (leagues[i].name === this.current.league && leagues[i].history !== undefined) {
        return true;
      }
    }

    return false;
  }
}

// Default item search filter options
let FILTER = {
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
// List of items displayed on the current page
let ITEMS = [];
// Singular modal to display item specifics on
let MODAL = new DetailsModal();

$(document).ready(function() {
  parseQueryParams();

  makeGetRequest();
  defineListeners();
}); 

//------------------------------------------------------------------------------------------------------------
// Data prep
//------------------------------------------------------------------------------------------------------------

function parseQueryParams() {
  // Reusable variable to hold query param values
  let tmp;

  // If there is a league param
  if ((tmp = parseQueryParam('league'))) {
    // Get data associated with the league
    let leagueData = getServiceLeague(tmp);

    // If it's valid
    if (leagueData) {
      FILTER.league = leagueData;
      $("#search-league").val(leagueData.name);
    }
  }

  // Overwrite league query param with a correct value
  updateQueryParam("league", FILTER.league.name);

  if ((tmp = parseQueryParam('category'))) {
    FILTER.category = tmp;
  } else {
    FILTER.category = "currency";
    updateQueryParam("category", FILTER.category);
  }

  if ((tmp = parseQueryParam('group'))) {
    FILTER.group = tmp;
    $('#search-group').val(tmp);
  }

  if ((tmp = parseQueryParam('search'))) {
    FILTER.search = tmp;
  }

  if (parseQueryParam('confidence')) {
    FILTER.showLowConfidence = true;
  }

  if ((tmp = parseQueryParam('rarity'))) {
    if      (tmp === "unique") FILTER.rarity =    3;
    else if (tmp ===  "relic") FILTER.rarity =    9;
  }

  if ((tmp = parseQueryParam('links'))) {
    if (tmp ===  "all") FILTER.links = -1;
    else FILTER.links = parseInt(tmp);
  }

  if ((tmp = parseQueryParam('tier'))) {
    $('#select-tier').val(tmp);
    if (tmp === "none") FILTER.tier = 0;
    else FILTER.tier = parseInt(tmp);
  }

  if ((tmp = parseQueryParam('corrupted'))) {
    FILTER.gemCorrupted = tmp === "true";
  }

  if ((tmp = parseQueryParam('lvl'))) {
    $('#select-level').val(tmp);
    FILTER.gemLvl = parseInt(tmp);
  }

  if ((tmp = parseQueryParam('quality'))) {
    $('#select-quality').val(tmp);
    FILTER.gemQuality = parseInt(tmp);
  }

  if ((tmp = parseQueryParam('ilvl'))) {
    $('#select-ilvl').val(tmp);
    FILTER.ilvl = parseInt(tmp);
  }

  if ((tmp = parseQueryParam('influence'))) {
    $('#select-influence').val(tmp);
    FILTER.influence = tmp;
  }

  let tmpCol, tmpOrder;

  if ((tmpCol = parseQueryParam('sortby'))) {
    let element = null;

    // Find column that matches the provided param
    $(".sort-column").each(function() {
      if (this.innerHTML.toLowerCase() === tmpCol) {
        element = this;
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

    // If there was a sort order query param as well
    if ((tmpOrder = parseQueryParam('sortorder'))) {
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
    MODAL.onRowClick(event);
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

/**
 * Get api request
 */
function makeGetRequest() {
  // Empty previous data
  $("#searchResults tbody").empty();
  // Show buffering symbol
  $("#buffering-main").show();
  // Hide 'show all' button
  $("#button-showAll").hide();
  // Hide status message
  $(".buffering-msg").remove();
  // Clear current items
  ITEMS = [];

  let request = $.ajax({
    url: API_URL + "get",
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
    $(".buffering-msg").remove();

    let buffering = $("#buffering-main");
    buffering.hide();

    let msg;
    if (response.status) {
      msg = "<div class='buffering-msg align-self-center mb-2'>" + response.responseJSON.error + "</div>";
    } else {
      msg = "<div class='buffering-msg align-self-center mb-2'>Too many requests, please wait a bit</div>";
    }

    buffering.after(msg);
  });
}

/**
 * Sorting by column
 */
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
    let parts = x.toString().split(".");
    parts[0] = parts[0].replace(/\B(?=(\d{3})+(?!\d))/g, ",");
    return parts.join(".");
  };

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
  
  let url = document.location.href;
  let re = new RegExp("([?&])" + key + "=.*?(&|#|$)(.*)", "gi");
  let hash;

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
    let separator = url.indexOf('?') !== -1 ? '&' : '?';

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

  let regex = new RegExp('[?&]' + key + '(=([^&#]*)|&|#|$)');
  let results = regex.exec(url);
      
  if (!results) return null;
  if (!results[2]) return '';

  return decodeURIComponent(results[2].replace(/\+/g, ' '));
}

/**
 * Extension method for Date to add days
 */
Date.prototype.addDays = function(days) {
  let date = new Date(this.valueOf());
  date.setDate(date.getDate() + days);
  return date;
};

/**
 * Provided a league name, returns league data from before
 *
 * @param league Valid league name
 * @returns {null|*} League data if exists, null otherwise
 */
function getServiceLeague(league) {
  for (let i = 0; i < SERVICE_leagues.length; i++) {
    if (SERVICE_leagues[i].name === league) {
      return SERVICE_leagues[i];
    }
  }

  return null;
}

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

/**
 * Check whether or not to hide items when searching
 *
 * @param item Get api item entry
 * @returns {boolean} True if hidden, false if not
 */
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
    if (item.linkCount !== undefined) {
      return true;
    }
  } else if (FILTER.links > 0) {
    if (item.linkCount !== FILTER.links) {
      return true;
    }
  }

  // Sort gems, I guess
  if (FILTER.category === 'gem' && item.category === 'gem') {
    if (FILTER.gemLvl !== null && item.gemLevel !== FILTER.gemLvl) return true;
    if (FILTER.gemQuality !== null && item.gemQuality !== FILTER.gemQuality) return true;
    if (FILTER.gemCorrupted !== null && item.gemIsCorrupted !== FILTER.gemCorrupted) return true;

  } else if (FILTER.category === 'map' && item.category === 'map') {
    if (FILTER.tier !== null) {
      if (FILTER.tier === 0) {
        if (item.mapTier !== null) return true;
      } else if (item.mapTier !== FILTER.tier) return true;
    }

  } else if (FILTER.category === 'base' && item.category === 'base') {
    // Check base influence
    if (FILTER.influence !== null) {
      if (FILTER.influence === "none") {
        if (item.baseIsShaper || item.baseIsElder) return true;
      } else if (FILTER.influence === "either") {
        if (!item.baseIsShaper && !item.baseIsElder) return true;
      } else if (FILTER.influence === "shaper" && !item.baseIsShaper) {
        return true;
      } else if (FILTER.influence === "elder" && !item.baseIsElder) {
        return true;
      }
    }

    // Check base ilvl
    if (item.baseItemLevel !== null && FILTER.ilvl !== null) {
      if (item.baseItemLevel !== FILTER.ilvl) {
        return true;
      }
    }
  }

  return false;
}

