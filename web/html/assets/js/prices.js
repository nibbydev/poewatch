/*
  There's not much here except for some poorly written JS functions. And since you're 
  already here, it can't hurt to take a look at http://youmightnotneedjquery.com/
*/

class ItemRow {
  constructor (item) {
    this.item = item;
    this.row = "<tr value={{id}}>{{fill}}</tr>";

    let rowBuilder = "";

    rowBuilder += this.buildNameField();
    rowBuilder += this.buildGemFields();
    rowBuilder += this.buildBaseFields();
    rowBuilder += this.buildMapFields();
    rowBuilder += this.buildPriceFields();
    rowBuilder += this.buildChangeField();
    rowBuilder += this.buildQuantField();

    this.row = this.row
      .replace("{{id}}",    item.id)
      .replace("{{fill}}",  rowBuilder);
  }

  buildNameField() {
    let template = `
    <td>
      <div class='d-flex align-items-center'>
        <span class='img-container img-container-sm text-center {{influence}} mr-1'><img src="{{icon}}"></span>
        <a href='{{url}}' target="_blank" {{foil}}>{{name}}{{type}}</a>{{var}}{{link}}
      </div>
    </td>
    `.trim();
  
    template = template.replace("{{url}}", "https://poe.watch/item?league=" + FILTER.league + "&id=" + this.item.id);
  
    if (this.item.icon) {
      // Use SSL for icons for that sweet, sweet secure site badge
      this.item.icon = this.item.icon.replace("http://", "https://");
      template = template.replace("{{icon}}", this.item.icon);
    } else {
      template = template.replace("{{icon}}", ICON_MISSING);
    }
  
    if (this.item.frame === 9) {
      template = template.replace("{{foil}}", "class='item-foil'");
    } else {
      template = template.replace("{{foil}}", "");
    }
  
    if (FILTER.category === "base") {
      if (this.item.var === "shaper") {
        template = template.replace("{{influence}}", "influence influence-shaper-1x1");
      } else if (this.item.var === "elder") {
        template = template.replace("{{influence}}", "influence influence-elder-1x1");
      } else {
        template = template.replace("{{influence}}", "");
      }
    } else {
      template = template.replace("{{influence}}", "");
    }
  
    if (FILTER.category === "enchantment") {
      if (this.item.var !== null) {
        let splitVar = this.item.var.split('-');
        for (var num in splitVar) {
          this.item.name = this.item.name.replace("#", splitVar[num]);
        }
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
  
    if (this.item.var && FILTER.category !== "enchantment") {
      let tmp = " <span class='badge custom-badge-gray ml-1'>" + this.item.var + "</span>";
      template = template.replace("{{var}}", tmp);
    } else {
      template = template.replace("{{var}}", "");
    }
  
    return template;
  }
  
  buildGemFields() {
    // Don't run if item is not a gem
    if (this.item.frame !== 4) return "";
  
    let template = `
    <td><span class='badge custom-badge-block custom-badge-gray'>{{lvl}}</span></td>
    <td><span class='badge custom-badge-block custom-badge-gray'>{{quality}}</span></td>
    <td><span class='badge custom-badge-{{color}}'>{{corr}}</span></td>
    `.trim();
  
    template = template.replace("{{lvl}}",      this.item.lvl);
    template = template.replace("{{quality}}",  this.item.quality);
    
    if (this.item.corrupted) {
      template = template.replace("{{color}}",  "red");
      template = template.replace("{{corr}}",   "✓");
    } else {
      template = template.replace("{{color}}",  "green");
      template = template.replace("{{corr}}",   "✕");
    }
  
    return template;
  }
  
  buildBaseFields() {
    // Don't run if item is not a gem
    if (FILTER.category !== "base") return "";
  
    let displayLvl;
  
    if (this.item.var === "elder" || this.item.var === "shaper") {
      switch (this.item.ilvl) {
        case 68: displayLvl = "68 - 74";  break;
        case 75: displayLvl = "75 - 82";  break;
        case 84: displayLvl = "83 - 84";  break;
        case 85: displayLvl = "85 - 100"; break;
      }
    } else {
      displayLvl = "84";
    }
  
    return "<td class='nowrap'><span class='badge custom-badge-block custom-badge-gray'>" + displayLvl + "</span></td>";
  }
  
  buildMapFields() {
    // Don't run if item is not a map
    if (FILTER.category !== "map") {
      return "";
    }

    let template = `
    <td class='nowrap'>
      <span class='badge custom-badge-block custom-badge-gray'>{{tier}}</span>
    </td>
    `.trim();

    return template.replace("{{tier}}", this.item.tier ? this.item.tier : "-");
  }
  
  buildPriceFields() {
    let template = `
    <td>
      <div class='pricebox'>{{sparkline}}{{chaos_icon}}{{chaos_price}}</div>
    </td>
    <td>
      <div class='pricebox'>{{ex_icon}}{{ex_price}}</div>
    </td>
    `.trim();

    let chaosContainer  = TEMPLATE_imgContainer.trim().replace("{{img}}", ICON_CHAOS);
    let exContainer     = TEMPLATE_imgContainer.trim().replace("{{img}}", ICON_EXALTED);
    let sparkLine       = this.buildSparkLine(this.item);
  
    template = template.replace("{{sparkline}}",    sparkLine);
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
  
  buildSparkLine() {
    if (!this.item.spark) return "";
  
    let svgColorClass = this.item.change > 0 ? "sparkline-green" : "sparkline-orange";
    let svg = document.createElement("svg");
    
    svg.setAttribute("class", "sparkline " + svgColorClass);
    svg.setAttribute("width", 60);
    svg.setAttribute("height", 30);
    svg.setAttribute("stroke-width", 3);
  
    sparkline(svg, this.item.spark);
    
    return svg.outerHTML;
  }
  
  buildChangeField() {
    let template = `
    <td>
      <span class='badge custom-badge-block custom-badge-{{color}}'>
        {{percent}}%
      </span>
    </td>
    `.trim();
  
    let change = 0;
  
    if (this.item.change > 999) {
      change = 999;
    } else if (this.item.change < -999) {
      change = -999;
    } else {
      change = Math.round(this.item.change); 
    }
  
    if (change > 100) {
      template = template.replace("{{color}}", "green");
    } else if (change < -100) {
      template = template.replace("{{color}}", "orange");
    } else if (change > 50) {
      template = template.replace("{{color}}", "green-lo");
    } else if (change < -50) {
      template = template.replace("{{color}}", "orange-lo");
    } else {
      template = template.replace("{{color}}", "gray");
    }
  
    return template.replace("{{percent}}", change);
  }
  
  buildQuantField() {
    let template = `
    <td>
      <span class='badge custom-badge-block custom-badge-{{color}}'>
        {{quant}}
      </span>
    </td>
    `.trim();
  
    if (this.item.quantity >= 10) {
      template = template.replace("{{color}}", "gray");
    } else if (this.item.quantity >= 5) {
      template = template.replace("{{color}}", "orange");
    } else {
      template = template.replace("{{color}}", "red");
    }
  
    return template.replace("{{quant}}", this.item.quantity);
  }

  static roundPrice(price) {
    const numberWithCommas = (x) => {
      var parts = x.toString().split(".");
      parts[0] = parts[0].replace(/\B(?=(\d{3})+(?!\d))/g, ",");
      return parts.join(".");
    }
  
    return numberWithCommas(Math.round(price * 100) / 100);
  }
}

class ExpandedRow {
  constructor() {
    this.dataSets     = {};

    this.rowExpanded  = null;
    this.rowParent    = null;
    this.rowFiller    = null;

    this.id           = null;
    this.league       = null;
    this.chart        = null;
    this.dataset      = 1;

    // A bit more "static" variables
    this.template_exaltedContainer = null;
    this.template_chaosContainer = null;
    this.template_expandedRow = null;
    this.chart_settings = null;

    this.setStaticData();
  }

  // Set some more or less static data
  setStaticData() {
    this.template_exaltedContainer = `
    <span class='img-container img-container-xs text-center mr-1'>
      <img src='https://web.poecdn.com/image/Art/2DItems/Currency/CurrencyAddModToRare.png?scale=1&w=1&h=1'>
    </span>
    `.trim();

    this.template_chaosContainer = `
    <span class='img-container img-container-xs text-center mr-1'>
      <img src='https://web.poecdn.com/image/Art/2DItems/Currency/CurrencyRerollRare.png?scale=1&w=1&h=1'>
    </span>
    `.trim();

    this.template_expandedRow = `
    <tr class='selected-row'><td colspan='100'>
      <div class='row m-1'>
        <div class='col-sm d-flex mt-2'>
          <h4 class='m-0 mr-2'>League</h4>
          <select class="form-control form-control-sm w-auto mr-2" id="history-league-selector"></select>
        </div>
      </div>
      <hr>
      <div class='row m-1 mt-2'>
        <div class='col d-flex'>
          <table class="table table-sm details-table mw-item-dTable mr-4">
            <tbody>
              <tr>
                <td class='nowrap w-100'>Mean</td>
                <td class='nowrap'>{{chaosContainter}}<span id='details-table-mean'></span></td>
              </tr>
              <tr>
                <td class='nowrap w-100'>Median</td>
                <td class='nowrap'>{{chaosContainter}}<span id='details-table-median'></span></td>
              </tr>
              <tr>
                <td class='nowrap w-100'>Mode</td>
                <td class='nowrap'>{{chaosContainter}}<span id='details-table-mode'></span></td>
              </tr>
            </tbody>
          </table>
  
          <table class="table table-sm details-table mw-item-dTable">
            <tbody>
              <tr>
                <td class='nowra pw-100'>Total amount listed</td>
                <td class='nowrap'><span id='details-table-count'></span></td>
              </tr>
              <tr>
                <td class='nowrap w-100'>Listed every 24h</td>
                <td class='nowrap'><span id='details-table-quantity'></span></td>
              </tr>
              <tr>
                <td class='nowrap w-100'>Price in exalted</td>
                <td class='nowrap'>{{exaltedContainter}}<span id='details-table-exalted'></span></td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
      <hr>
      <div class='row m-1 mb-3'>
        <div class='col-sm'>
          <h4>Past data</h4>
          <div class="btn-group btn-group-toggle mt-1 mb-3" data-toggle="buttons" id="history-dataset-radio">
            <label class="btn btn-sm btn-outline-dark p-0 px-1 active"><input type="radio" name="dataset" value=1>Mean</label>
            <label class="btn btn-sm btn-outline-dark p-0 px-1"><input type="radio" name="dataset" value=2>Median</label>
            <label class="btn btn-sm btn-outline-dark p-0 px-1"><input type="radio" name="dataset" value=3>Mode</label>
            <label class="btn btn-sm btn-outline-dark p-0 px-1"><input type="radio" name="dataset" value=4>Quantity</label>
          </div>
          <div class='chart-large'><canvas id="chart"></canvas></div>
        </div>
      </div>
    </td></tr>
    `.trim();
  
    let gradientPlugin = {
      beforeDatasetUpdate: function(chart) {
        if (!chart.width) return;
  
        // Create the linear gradient  chart.scales['x-axis-0'].width
        var gradient = chart.ctx.createLinearGradient(0, 0, 0, 250);
  
        gradient.addColorStop(0.0, 'rgba(247, 233, 152, 1)');
        gradient.addColorStop(1.0, 'rgba(244, 149, 179, 1)');
  
        // Assign the gradient to the dataset's border color.
        chart.data.datasets[0].borderColor = gradient;
      }
    };

    let dataPlugin = {
      beforeUpdate: function(chart) {
        // Don't run if data has not yet been initialized
        if (chart.data.data.length < 1) return;
  
        var keys = chart.data.data.keys;
        var vals = chart.data.data.vals;
  
        chart.data.labels = keys;
  
        switch (EXPROW.dataset) {
          case 1: chart.data.datasets[0].data = vals.mean;      break;
          case 2: chart.data.datasets[0].data = vals.median;    break;
          case 3: chart.data.datasets[0].data = vals.mode;      break;
          case 4: chart.data.datasets[0].data = vals.quantity;  break;
        }
      }
    };
  
    this.chart_settings = {
      plugins: [dataPlugin, gradientPlugin],
      type: "line",
      data: {
        data: [],
        labels: [],
        datasets: [{
          data: [],
          backgroundColor: "rgba(0, 0, 0, 0.2)",
          borderColor: "rgba(255, 255, 255, 0.5)",
          borderWidth: 3,
          lineTension: 0.4,
          pointRadius: 0
        }]
      },
      options: {
        title: {display: false},
        layout: {padding: 0},
        legend: {display: false},
        responsive: true,
        maintainAspectRatio: false,
        animation: {duration: 0},
        hover: {animationDuration: 0},
        responsiveAnimationDuration: 0,
        tooltips: {
          intersect: false,
          mode: "index",
          callbacks: {
            title: function(tooltipItem, data) {
              let price = data['datasets'][0]['data'][tooltipItem[0]['index']];
              return price ? price : "No data";
            },
            label: function(tooltipItem, data) {
              return data['labels'][tooltipItem['index']];
            }
          },
          backgroundColor: '#fff',
          titleFontSize: 16,
          titleFontColor: '#222',
          bodyFontColor: '#444',
          bodyFontSize: 14,
          displayColors: false,
          borderWidth: 1,
          borderColor: '#aaa'
        },
        scales: {
          yAxes: [{
            ticks: {
              beginAtZero: true,
              padding: 0
            }
          }],
          xAxes: [{
            ticks: {
              callback: function(value, index, values) {
                return (value ? value : '');
              },
              maxRotation: 0,
              padding: 0
            }
          }]
        }
      }
    }
  }

  onRowClick(event) {
    let target = $(event.currentTarget);
    let id = parseInt(target.attr('value'));
  
    // If user clicked on a table that does not contain an id
    if (isNaN(id)) return;
    // If user clicked on item name
    if (event.target.href) return;
    // If user clicked on item type
    if (event.target.parentElement.href) return;
  
    // Get rid of any filler rows
    this.removeFillerRow();
  
    // User clicked on open parent-row
    if (target.is(this.rowParent)) {
      console.log("Closed open row");
  
      $(".parent-row").removeAttr("class");
      this.rowParent = null;
  
      $(".selected-row").remove();
      this.rowExpanded = null;
      return;
    }
  
    // There's an open row somewhere
    if (this.rowParent !== null || this.rowExpanded !== null) {
      $(".selected-row").remove();
      $(".parent-row").removeAttr("class");
  
      console.log("Closed row: " + this.id);
  
      this.rowParent = null;
      this.rowExpanded = null;
    }
  
    console.log("Clicked on row id: " + id);
  
    // Define current row as parent target row
    target.addClass("parent-row");
    this.rowParent = target;
    this.league = FILTER.league;
    this.id = id;
  
    // Load history data
    if (id in this.dataSets) {
      console.log("History source: local");
      this.buildExpandedRow();
    } else {
      console.log("History source: remote");
      this.displayFillerRow();
      this.makeHistoryRequest();
    }
  }

  displayFillerRow() {
    let template = `
    <tr class='filler-row'><td colspan='100'>
      <div class="d-flex justify-content-center">
        <div class="buffering m-2"></div>
      </div>
    </td></tr>
    `.trim();
  
    this.rowFiller = $(template);
    this.rowParent.after(this.rowFiller);
  }

  makeHistoryRequest() {
    let request = $.ajax({
      url: "https://api.poe.watch/item.php",
      data: {id: this.id},
      type: "GET",
      async: true,
      dataTypes: "json"
    });
  
    request.done(function(payload) {
      // Get rid of any filler rows
      EXPROW.removeFillerRow();
            
      EXPROW.dataSets[EXPROW.id] = payload;
      EXPROW.buildExpandedRow();
    });
  }

  // Removes all fillers rows
  removeFillerRow() {
    if (this.rowFiller) {
      $(".filler-row").remove();
      this.rowFiller = null;
    }
  }

  buildExpandedRow() {
    // Get list of past leagues available for the row
    let leagues = ExpandedRow.getItemHistoryLeagues(this.dataSets, this.id);
  
    // Stop if no leagues available
    if (leagues.length < 1) {
      return;
    }
  
    // Create expandedRow jQuery object
    let template = this.template_expandedRow
      .replace("{{chaosContainter}}",   this.template_chaosContainer)
      .replace("{{chaosContainter}}",   this.template_chaosContainer)
      .replace("{{chaosContainter}}",   this.template_chaosContainer)
      .replace("{{exaltedContainter}}", this.template_exaltedContainer);
    this.rowExpanded = $(template);

    // Instantiate chart
    let chartCanvas = $("#chart", this.rowExpanded);
    this.chart = new Chart(chartCanvas, this.chart_settings);

    // Format and display data
    this.fillData();
    // Create league selectors
    this.createSelectors(leagues);
  
    // Place expanded row in table
    this.rowParent.after(this.rowExpanded);
  
    // Create league select event listener
    $("#history-league-selector", this.rowExpanded).change(function(){
      EXPROW.league = $(":selected", this).val();
      EXPROW.fillData();
    });
  
    // Create dataset radio event listener
    $("#history-dataset-radio", this.rowExpanded).change(function(){
      EXPROW.dataset = parseInt($("input[name=dataset]:checked", this).val());
      EXPROW.fillData();
    });
  }

  // Format and display data
  fillData() {
    // Get league-specific data pack
    let leaguePayload = ExpandedRow.getLeaguePayload(this.dataSets, this.league, this.id);

    // Format history
    let formattedHistory = ExpandedRow.formatHistory(leaguePayload);

    // Assign chart datasets
    this.chart.data.data = formattedHistory;
    this.chart.update();
    
    // Set data in details table
    $("#details-table-mean",     this.rowExpanded).html( formatNum(leaguePayload.mean)     );
    $("#details-table-median",   this.rowExpanded).html( formatNum(leaguePayload.median)   );
    $("#details-table-mode",     this.rowExpanded).html( formatNum(leaguePayload.mode)     );
    $("#details-table-count",    this.rowExpanded).html( formatNum(leaguePayload.count)    );
    $("#details-table-quantity", this.rowExpanded).html( formatNum(leaguePayload.quantity) );
    $("#details-table-exalted",  this.rowExpanded).html( formatNum(leaguePayload.exalted)  );
  }

  // Create league selectors
  createSelectors(leagues) {
    let builder = "";
  
    for (let i = 0; i < leagues.length; i++) {
      let display = leagues[i].active ? leagues[i].display : "● " + leagues[i].display;
      let selected = FILTER.league === leagues[i].name ? "selected" : "";
  
      builder += "<option value='{{value}}' {{selected}}>{{name}}</option>"
        .replace("{{selected}}",  selected)
        .replace("{{value}}",     leagues[i].name)
        .replace("{{name}}",      display);
    }
  
    $("#history-league-selector", this.rowExpanded).append(builder);
  }

  // Return the matching payload from dataSets
  static getLeaguePayload(dataSets, league, id) {
    for (let i = 0; i < dataSets[id].data.length; i++) {
      if (dataSets[id].data[i].league.name === league) {
        return dataSets[id].data[i];
      }
    }
  
    return null;
  }

  // Get list of past leagues available for the row
  static getItemHistoryLeagues(dataSets, id) {
    let leagues = [];
  
    for (let i = 0; i < dataSets[id].data.length; i++) {
      leagues.push({
        name:    dataSets[id].data[i].league.name,
        display: dataSets[id].data[i].league.display,
        active:  dataSets[id].data[i].league.active
      });
    }
  
    return leagues;
  }

  // Format history
  static formatHistory(leaguePayload) {
    let keys = [];
    let vals = {
      mean:     [],
      median:   [],
      mode:     [],
      quantity: []
    };

    // Convert date strings into objects
    let oldestDate = new Date(leaguePayload.history[0].time);
    let startDate  = new Date(leaguePayload.league.start);
    let endDate    = new Date(leaguePayload.league.end);
  
    // Increment startdate to balance timezone differences
    startDate.setTime(startDate.getTime() + 24 * 60 * 60 * 1000);
  
    // Nr of days league data is missing since league start until first entry
    let timeDiffMissing = Math.abs(startDate.getTime() - oldestDate.getTime());
    let daysMissing     = Math.ceil(timeDiffMissing / (1000 * 60 * 60 * 24));
  
    // Nr of days in a league
    let timeDiffLeague = Math.abs(endDate.getTime() - startDate.getTime());
    let daysLeague     = Math.ceil(timeDiffLeague / (1000 * 60 * 60 * 24));
  
    // Hardcore (id 1) and Standard (id 2) don't have an end date
    if (leaguePayload.league.id <= 2) {
      daysLeague = 120;
      daysMissing = 0;
    }
  
    // Bloat using 'null's the amount of days that should not have a tooltip
    for (let i = 0; i < daysLeague - daysMissing - leaguePayload.history.length; i++) {
      vals.mean     .push(null);
      vals.median   .push(null);
      vals.mode     .push(null);
      vals.quantity .push(null);
      keys          .push(null);
    }
  
    // Bloat using '0's the amount of days that should show "no data"
    // Skip Hardcore (id 1) and Standard (id 2)
    if (leaguePayload.league.id > 2) {
      let tmpDate = new Date(startDate);
  
      for (let i = 0; i < daysMissing; i++) {
        vals.mean     .push(0);
        vals.median   .push(0);
        vals.mode     .push(0);
        vals.quantity .push(0);
  
        keys.push(ExpandedRow.formatDate(tmpDate));
        tmpDate.setDate(tmpDate.getDate() + 1);
      }
    }

    // Grab values
    for (let i = 0; i < leaguePayload.history.length; i++) {
      let element = leaguePayload.history[i];
      vals.mean    .push(Math.round(element.mean   * 100) / 100);
      vals.median  .push(Math.round(element.median * 100) / 100);
      vals.mode    .push(Math.round(element.mode   * 100) / 100);
      vals.quantity.push(element.quantity);

      keys.push(ExpandedRow.formatDate(element.time));
    }

    // Add current values
    vals.mean    .push(Math.round(leaguePayload.mean   * 100) / 100);
    vals.median  .push(Math.round(leaguePayload.median * 100) / 100);
    vals.mode    .push(Math.round(leaguePayload.mode   * 100) / 100);
    vals.quantity.push(leaguePayload.quantity);
    keys.push("Now");
  
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
}

// Default item search filter options
var FILTER = {
  league: null,
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
  baseIlvlMin: null,
  baseIlvlMax: null,
  baseInfluence: null,
  parseAmount: 150
};

var ITEMS = {};
var LEAGUES = null;
var INTERVAL;
var EXPROW = new ExpandedRow();

// Re-used icon urls
const ICON_ENCHANTMENT = "https://web.poecdn.com/image/Art/2DItems/Currency/Enchantment.png?scale=1&w=1&h=1";
const ICON_EXALTED = "https://web.poecdn.com/image/Art/2DItems/Currency/CurrencyAddModToRare.png?scale=1&w=1&h=1";
const ICON_CHAOS = "https://web.poecdn.com/image/Art/2DItems/Currency/CurrencyRerollRare.png?scale=1&w=1&h=1";
const ICON_MISSING = "https://poe.watch/assets/img/missing.png";

var TEMPLATE_imgContainer = "<span class='img-container img-container-sm text-center mr-1'><img src={{img}}></span>";

$(document).ready(function() {
  if (!SERVICE_category) return;

  FILTER.league = SERVICE_leagues[0].name;
  FILTER.category = SERVICE_category;

  readLeagueFromCookies(FILTER, SERVICE_leagues);
  parseQueryParams();
  makeGetRequest(FILTER.league, FILTER.category);
  defineListeners();
}); 

//------------------------------------------------------------------------------------------------------------
// Data prep
//------------------------------------------------------------------------------------------------------------

function readLeagueFromCookies(FILTER, leagues) {
  let league = getCookie("league");

  if (league) {
    console.log("Got league from cookie: " + league);

    // Check if league from cookie is still active
    for (let i = 0; i < leagues.length; i++) {
      const entry = leagues[i];
      
      if (league === entry.name) {
        FILTER.league = league;
        // Point league dropdown to that league
        $("#search-league").val(league);
        return;
      }
    }

    console.log("League cookie did not match any active leagues");
  }
}

function parseQueryParams() {
  let tmp;

  if (tmp = parseQueryParam('group')) {
    FILTER.group = tmp;
    $('#search-group').val(tmp);
  }

  if (tmp = parseQueryParam('search')) FILTER.search = tmp;
  if (tmp = parseQueryParam('confidence')) FILTER.showLowConfidence = true;

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

    let splitRange = tmp.split("-");
    FILTER.baseIlvlMin = parseInt(splitRange[0]);
    FILTER.baseIlvlMax = parseInt(splitRange[1]);
  }

  if (tmp = parseQueryParam('influence')) {
    $('#select-influence').val(tmp);
    FILTER.influence = tmp;
  }
}

function defineListeners() {
  // League
  $("#search-league").on("change", function(){
    FILTER.league = $(":selected", this).val();
    console.log("Selected league: " + FILTER.league);
    document.cookie = "league="+FILTER.league;
    makeGetRequest(FILTER.league, FILTER.category);
  });

  // Subcategory
  $("#search-group").change(function(){
    FILTER.group = $(this).find(":selected").val();
    console.log("Selected group: " + FILTER.group);
    updateQueryParam("group", FILTER.group);
    sortResults(ITEMS);
  });

  // Load all button
  $("#button-showAll").on("click", function(){
    console.log("Button press: show all");
    $(this).hide();
    FILTER.parseAmount = -1;
    sortResults(ITEMS);
  });

  // Searchbar
  $("#search-searchbar").on("input", function(){
    FILTER.search = $(this).val().toLowerCase().trim();
    console.log("Search: " + FILTER.search);
    updateQueryParam("search", FILTER.search);
    sortResults(ITEMS);
  });

  // Low confidence
  $("#radio-confidence").on("change", function(){
    let option = $("input:checked", this).val() === "true";
    console.log("Show low count: " + option);
    FILTER.showLowConfidence = option;
    updateQueryParam("confidence", option);
    sortResults(ITEMS);
  });

  // Rarity
  $("#radio-rarity").on("change", function(){
    FILTER.rarity = $(":checked", this).val();
    console.log("Rarity filter: " + FILTER.rarity);
    updateQueryParam("rarity", FILTER.rarity);

    if      (FILTER.rarity ===    "all") FILTER.rarity = null;
    else if (FILTER.rarity === "unique") FILTER.rarity =    3;
    else if (FILTER.rarity ===  "relic") FILTER.rarity =    9;
    
    sortResults(ITEMS);
  });
  
  // Item links
  $("#radio-links").on("change", function(){
    FILTER.links = $(":checked", this).val();
    console.log("Link filter: " + FILTER.links);
    updateQueryParam("links", FILTER.links);
    if      (FILTER.links === "none") FILTER.links = null;
    else if (FILTER.links ===  "all") FILTER.links = -1;
    else FILTER.links = parseInt(FILTER.links);
    sortResults(ITEMS);
  });

  // Map tier
  $("#select-tier").on("change", function(){
    FILTER.tier = $(":selected", this).val();
    console.log("Map tier filter: " + FILTER.tier);
    updateQueryParam("tier", FILTER.tier);
    if (FILTER.tier === "all") FILTER.tier = null;
    else if (FILTER.tier === "none") FILTER.tier = 0;
    else FILTER.tier = parseInt(FILTER.tier);
    sortResults(ITEMS);
  });

  // Gem level
  $("#select-level").on("change", function(){
    FILTER.gemLvl = $(":selected", this).val();
    console.log("Gem lvl filter: " + FILTER.gemLvl);
    if (FILTER.gemLvl === "all") FILTER.gemLvl = null;
    else FILTER.gemLvl = parseInt(FILTER.gemLvl);
    updateQueryParam("lvl", FILTER.gemLvl);
    sortResults(ITEMS);
  });

  // Gem quality
  $("#select-quality").on("change", function(){
    FILTER.gemQuality = $(":selected", this).val();
    console.log("Gem quality filter: " + FILTER.gemQuality);
    if (FILTER.gemQuality === "all") FILTER.gemQuality = null;
    else FILTER.gemQuality = parseInt(FILTER.gemQuality);
    updateQueryParam("quality", FILTER.gemQuality);
    sortResults(ITEMS);
  });

  // Gem corrupted
  $("#radio-corrupted").on("change", function(){
    FILTER.gemCorrupted = $(":checked", this).val();
    console.log("Gem corruption filter: " + FILTER.gemCorrupted);
    if (FILTER.gemCorrupted === "all") FILTER.gemCorrupted = null;
    else FILTER.gemCorrupted = FILTER.gemCorrupted === "true";
    updateQueryParam("corrupted", FILTER.gemCorrupted);
    sortResults(ITEMS);
  });

  // Base iLvl
  $("#select-ilvl").on("change", function(){
    let ilvlRange = $(":selected", this).val();
    console.log("Base iLvl filter: " + ilvlRange);
    if (ilvlRange === "all") {
      FILTER.baseIlvlMin = null;
      FILTER.baseIlvlMax = null;
      updateQueryParam("ilvl", null);
    } else {
      let splitRange = ilvlRange.split("-");
      FILTER.baseIlvlMin = parseInt(splitRange[0]);
      FILTER.baseIlvlMax = parseInt(splitRange[1]);
      updateQueryParam("ilvl", ilvlRange);
    }
    
    sortResults(ITEMS);
  });

  // Base influence
  $("#select-influence").on("change", function(){
    FILTER.baseInfluence = $(":selected", this).val();
    console.log("Base influence filter: " + FILTER.baseInfluence);
    if (FILTER.baseInfluence === "all") {
      FILTER.baseInfluence = null;
    }
    updateQueryParam("influence", FILTER.baseInfluence);
    sortResults(ITEMS);
  });


  // Expand row
  $("#searchResults > tbody").delegate("tr", "click", function(event) {
    EXPROW.onRowClick(event);
  });

  // Live search toggle
  $("#live-updates").on("change", function(){
    let live = $("input[name=live]:checked", this).val() === "true";
    console.log("Live updates: " + live);
    document.cookie = "live="+live;

    if (live) {
      $("#progressbar-live").css("animation-name", "progressbar-live");
      INTERVAL = setInterval(timedRequestCallback, 60 * 1000);
    } else {
      $("#progressbar-live").css("animation-name", "");
      clearInterval(INTERVAL);
    }
  });
}

//------------------------------------------------------------------------------------------------------------
// Requests
//------------------------------------------------------------------------------------------------------------

function makeGetRequest(league, category) {
  $("#searchResults tbody").empty();
  $(".buffering").show();
  $("#button-showAll").hide();
  $(".buffering-msg").remove();

  let request = $.ajax({
    url: "https://api.poe.watch/get.php",
    data: {
      league: league, 
      category: category
    },
    type: "GET",
    async: true,
    dataTypes: "json"
  });

  request.done(function(json) {
    console.log("Got " + json.length + " items from request");
    $(".buffering").hide();
    $(".buffering-msg").remove();

    let items = parseRequest(json);
    sortResults(items);
    ITEMS = items;
  });

  request.fail(function(response) {
    ITEMS = {};

    $(".buffering-msg").remove();

    let buffering = $(".buffering");
    buffering.hide();
    buffering.after("<div class='buffering-msg align-self-center mb-2'>" + response.responseJSON.error + "</div>");
  });
}

function parseRequest(json) {
  let items = new Map();

  // Loop though array, creating an associative array based on IDs
  for (let i = 0; i < json.length; i++) {
    const item = json[i];
    items['_' + item.id] = item;
  }

  return items;
}

function timedRequestCallback() {
  console.log("Automatic update");

  var request = $.ajax({
    url: "https://api.poe.watch/get.php",
    data: {
      league: FILTER.league, 
      category: FILTER.category
    },
    type: "GET",
    async: true,
    dataTypes: "json"
  });

  request.done(function(json) {
    console.log("Got " + json.length + " items from request");

    let items = parseRequest(json);
    sortResults(items);
    ITEMS = items;
  });

  request.fail(function(response) {
    $("#searchResults tbody").empty();
    buffering.after("<div class='buffering-msg align-self-center mb-2'>" + response.responseJSON.error + "</div>");
  });
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

function getCookie(cname) {
  var name = cname + "=";
  var decodedCookie = decodeURIComponent(document.cookie);
  var ca = decodedCookie.split(';');

  for(var i = 0; i <ca.length; i++) {
    var c = ca[i];

    while (c.charAt(0) == ' ') {
      c = c.substring(1);
    }

    if (c.indexOf(name) == 0) {
      return c.substring(name.length, c.length);
    }
  }

  return "";
}

function updateQueryParam(key, value) {
  switch (key) {
    case "confidence": value = value === false  ? null : value;   break;
    case "search":     value = value === ""     ? null : value;   break;
    case "rarity":     value = value === "all"  ? null : value;   break;
    case "corrupted":  value = value === "all"  ? null : value;   break;
    case "quality":    value = value === "all"  ? null : value;   break;
    case "lvl":        value = value === "all"  ? null : value;   break;
    case "links":      value = value === "none" ? null : value;   break;
    case "group":      value = value === "all"  ? null : value;   break;
    case "tier":       value = value === "all"  ? null : value;   break;
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

//------------------------------------------------------------------------------------------------------------
// Itetm sorting and searching
//------------------------------------------------------------------------------------------------------------

function sortResults(items) {
  // Empty the table
  let table = $("#searchResults");
  $("tbody", table).empty();

  let count = 0, matches = 0;
  let buffer = "";

  // Loop through every item provided
  for (var key in items) {
    if (items.hasOwnProperty(key)) {
      // Skip parsing if item should be hidden according to filters
      if ( checkHideItem(items[key]) ) {
        continue;
      }

      matches++;

      // Stop if specified item limit has been reached
      if ( FILTER.parseAmount < 0 || count < FILTER.parseAmount ) {
        // If item has not been parsed, parse it 
        if ( !('tableData' in items[key]) ) {
          items[key].tableData = new ItemRow(items[key]);
        }

        // Append generated table data to buffer
        buffer += items[key].tableData.row;
        count++;
      }
    }
  }

  $(".buffering-msg").remove();

  if (count < 1) {
    let msg = "<div class='buffering-msg align-self-center mb-2'>No results</div>";
    $(".buffering").after(msg);
  }

  let loadAllBtn = $("#button-showAll");
  if (FILTER.parseAmount > 0 && matches > FILTER.parseAmount) {
    loadAllBtn.text("Show all (" + (matches - FILTER.parseAmount) + " items)");
    loadAllBtn.show();
  } else {
    loadAllBtn.hide();
  }
  
  // Add the generated HTML table data to the table
  table.append(buffer);
}

function checkHideItem(item) {
  // Hide low confidence items
  if (!FILTER.showLowConfidence) {
    if (item.quantity < 5) return true;
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
  if (FILTER.group !== "all" && FILTER.group !== item.category) {
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
  if (FILTER.category === "gem") {
    if (FILTER.gemLvl !== null && item.lvl != FILTER.gemLvl) return true;
    if (FILTER.gemQuality !== null && item.quality != FILTER.gemQuality) return true;
    if (FILTER.gemCorrupted !== null && item.corrupted != FILTER.gemCorrupted) return true;

  } else if (FILTER.category === "map") {
    if (FILTER.tier !== null) {
      if (FILTER.tier === 0) {
        if (item.tier !== null) return true;
      } else if (item.tier !== FILTER.tier) return true;
    }

  } else if (FILTER.category === "base") {
    // Check base influence
    if (FILTER.baseInfluence !== null) {
      if (FILTER.baseInfluence === "none") {
        if (item.var !== null) return true;
      } else if (FILTER.baseInfluence === "either") {
        if (item.var === null) return true;
      } else if (item.var !== FILTER.baseInfluence) {
        return true;
      }
    }

    // Check base ilvl
    if (item.ilvl !== null && FILTER.baseIlvlMin !== null && FILTER.baseIlvlMax !== null) {
      if (item.ilvl < FILTER.baseIlvlMin || item.ilvl > FILTER.baseIlvlMax) {
        return true;
      }
    }
  }

  return false;
}
