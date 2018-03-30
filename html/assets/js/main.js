/*
  There's not much here except for some poorly written JS functions. And since you're 
  already here, it can't hurt to take a look at http://youmightnotneedjquery.com/
*/

let FILTER;
var ITEMS = [];

const INITIAL_LOAD_AMOUNT = 100;
const PRICE_PERCISION = 100;
const COUNT_HIGH = 25;
const COUNT_MED = 15;

// Load conversion rates on page load
$(document).ready(function() {
  var selectorLeague = $("#search-league");
  var selectorSub = $("#search-sub");
  var buttonLoadmore = $("#button-loadmore");

  FILTER = {
    league: selectorLeague.find(":selected").text().toLowerCase(),
    category: getUrlParameter("category").toLowerCase(),
    sub: selectorSub.find(":selected").text().toLowerCase(),
    hideLowConfidence: true,
    links: "",
    search: "",
    gemLvl: "",
    gemQuality: "",
    gemCorrupted: ""
  };

  // Define league event listener
  selectorLeague.change(function(){
    FILTER.league = $(this).find(":selected").text();
    console.log(FILTER.league);
    // Empty item history
    ITEMS = [];

    makeRequest(0, INITIAL_LOAD_AMOUNT);
  });

  // Define subcategory event listener
  selectorSub.change(function(){
    FILTER.sub = $(this).find(":selected").text();
    console.log(FILTER.sub);
    // Empty item history
    ITEMS = [];

    makeRequest(0, INITIAL_LOAD_AMOUNT);
  });

  // Define load more event listener
  buttonLoadmore.on("click", function(){
    console.log("loadmore");
    makeRequest(INITIAL_LOAD_AMOUNT, 0);
    $(this).hide();
  });

  // Define searchbar event listener
  $("#search-searchbar").on("input", function(){
    FILTER.search = $(this).val().toLowerCase().trim();
    console.log(FILTER.search);
    sortResults();
  });

  // Define low confidence radio button event listener
  $("#radio-confidence").on("change", function(){
    let newValue = $("input[name=confidence]:checked", this).val();
    console.log(newValue);
    if (newValue != FILTER.hideLowConfidence) {
      FILTER.hideLowConfidence = !FILTER.hideLowConfidence;
      sortResults();
    }
  });

  // Define link radio button event listener
  $("#radio-links").on("change", function(){
    FILTER.links = $("input[name=links]:checked", this).val();
    console.log(FILTER.links);
    sortResults();
  });

  // Define gem selector and radio event listeners
  $("#select-level").on("change", function(){
    FILTER.gemLvl = $(":selected", this).val();
    console.log(FILTER.gemLvl);
    sortResults();
  });
  $("#select-quality").on("change", function(){
    FILTER.gemQuality = $(":selected", this).val();
    console.log(FILTER.gemQuality);
    sortResults();
  });
  $("#radio-corrupted").on("change", function(){
    FILTER.gemCorrupted = $(":checked", this).val();
    console.log(FILTER.gemCorrupted);
    sortResults();
  });

  // Define tr click event
  var parentRow;
  var selectedRow;
  $("#searchResults > tbody").delegate("tr", "click", function() {
    var selectedRowTemplate = `<tr><td colspan='100'>
      <div class='row m-1'>
        <div class='col-sm'>
          <h4>Chaos value</h4>
          <span class='sparkline-price'>Loading..</span>
        </div>
        <div class='col-sm'>
          <h4>Quantity</h4>
          <span class='sparkline-quant'>Loading..</span>
        </div>
      </div>
      <div class='row m-1 mt-2'>
        <div class='col-sm'>
          <h4>Some additional data</h4>
          <p>Lorem ipsum dolor sit amet, consectetur adipiscing elit. Integer pharetra, enim eget accumsan finibus, lectus orci molestie enim, ut placerat nisi arcu vel urna. In ac condimentum magna, eu maximus lectus.</p>
        </div>
      </div>
      <div class='row m-1 mb-3'>
        <div class='col-sm'>
          <h4>Past leagues (WIP, not an actual chart)</h4>
          <div class='form-group'><select class='form-control'><option>Legacy</option><option>Breach</option><option>Harbinger</option></select></div>
          <canvas id="testChart" height="120px"></canvas>
        </div>
      </div>
    </td></tr>`;

    // Close row if user clicked on parentRow
    if ($(this).is(parentRow)) {
      console.log("parent");
      selectedRow.remove();
      parentRow.removeAttr("class");
      parentRow = null;
      selectedRow = null;
      return;
    }

    // Don't add a new row if user clicked on selectedRow
    if ($(this).is(selectedRow)) return;

    // If there's an expanded row open somewhere, remove it
    if (selectedRow) {
      selectedRow.remove();
      parentRow.removeAttr("class");
    }

    selectedRow = $(selectedRowTemplate);
    $(this).closest("tr").after(selectedRow);

    var parentIndex = parseInt($(this).attr("value"));
    console.log("clicked on row: " + parentIndex + " (" + ITEMS[parentIndex]["name"] + ")");

    var sparklineOptions = {
      width: "100%",
      height: "80px",
      spotRadius: 3,
      lineColor: "#222",
      fillColor: "#aaa",
      highlightLineColor: "#000",
      highlightSpotColor: "#666",
      minSpotColor: false,
      maxSpotColor: false,
      spotColor: false,
      type: "line",
      lineWidth: 2
    };

    $(".sparkline-price").sparkline(ITEMS[parentIndex]["history"]["mean"], sparklineOptions);
    $(".sparkline-quant").sparkline(ITEMS[parentIndex]["history"]["quantity"], sparklineOptions);

    // Here goes chartJS code
    placeChart();

    $(this).addClass("parent-row");
    selectedRow.addClass("selected-row");
    
    parentRow = $(this);
  });

  checkFields();

  makeRequest(0, INITIAL_LOAD_AMOUNT);
}); 


function placeChart() {
  var sparklineValues = [40,40,38,38,37,37,36,37,30,30,30,30,29,29,29,27,27,27,28,27,28,26,25,25,25,26,25,25,25,24,24,24,24,23,23,24,24,24,24,23,24,24,24,23,24,23,24,22,19,20,22,24,23,22,22,22,22,23,22,21,19,18,17,19,18,17,17,17,16,16,16,16,17,18,17,17,16,15,15,15,15,14,14,13,13,15,18,26,28,32];
  var sparklineLabels = ['','','','','','','','','','','','','','','','','','','','','','','','','','','','','','','','','','','','','','','','','','','','','','','','','','','','','','','','','','','','','','','','','','','','','','','','','','','','','','','','','','','','','','','','','',''];
  var ctx = document.getElementById("testChart");

  var myChart = new Chart(ctx, {
      type: "line",
      data: {
          labels: sparklineLabels,
          datasets: [{
              label: "Price in chaos",
              data: sparklineValues,
              backgroundColor: ["rgba(0, 0, 0, 0.2)"],
              borderColor: ["#222"],
              borderWidth: 1,
              lineTension: 0
          }]
      },
      options: {
        animation: {duration: 0},
        hover: {animationDuration: 0},
        responsiveAnimationDuration: 0
      }
  });
}


function makeRequest(from, to) {
  var request = $.ajax({
    url: "http://api.poe.ovh/get",
    data: {
      league: FILTER.league, 
      parent: FILTER.category,
      child: FILTER.sub,
      from: from,
      to: to,
      exclude: "median,mode,index"
    },
    type: "GET",
    async: true,
    dataTypes: "json"
  });

  request.done(function(json) {
    // Add downloaded items to global variable ITEMS
    ITEMS = ITEMS.concat(json);
    // Sort the current results
    sortResults();
    // Enable "show more" button
    if (ITEMS.length === INITIAL_LOAD_AMOUNT) $("#button-loadmore").show();
  });
}


function checkFields() {
  if (FILTER.category === "armour" || FILTER.category === "weapons") {
    $(".link-fields").removeClass("link-fields");
  } else if (FILTER.category === "gems") {
    $(".gem-fields").removeClass("gem-fields");
  }
}


function parseItem(item) {
  // Format count badge
  var countBadge;
  if (item["count"] >= COUNT_HIGH) countBadge = "<span class=\"badge custom-badge-green\">" + item["count"] + "</span>";
  else if (item["count"] >= COUNT_MED) countBadge = "<span class=\"badge custom-badge-orange\">" + item["count"] + "</span>";
  else countBadge = "<span class=\"badge custom-badge-red\">" + item["count"] + "</span>";

  // Format icon
  var iconDiv = "";
  if (item["icon"]) {
    //item["icon"] = item["icon"].split("?")[0] + "?scale=1&w=1&h=1";
    iconDiv = "<div class=\"table-img-container text-center\"><img src=\"" + item["icon"] + "\"></div>";
  } else {
    iconDiv = "<div class=\"table-img-container text-center\"><img src=\"http://poe.ovh/assets/img/missing.png\"></div>";
  }
  // Format variant/links badge
  var name = item["name"];
  if ("type" in item) name += ", " + item["type"];
  if ("var" in item) name += " <span class=\"badge custom-badge-gray\">" + item["var"] + "</span>";
  if ("tier" in item) name += " <span class=\"badge custom-badge-gray\">" + item["tier"] + "</span>";

  // Add gem/map extra data
  var extraFields = "";
  if (item["frame"] === 4) {
    if ("lvl" in item) extraFields += "<td>" + item["lvl"] + "</td>";
    else extraFields += "<td>0</td>";
    
    if ("quality" in item) extraFields += "<td>" + item["quality"] + "</td>";
    else extraFields += "<td>0</td>";

    if ("corrupted" in item) {
      if (item["corrupted"] === "1") extraFields += "<td><span class=\"badge custom-badge-red\">Yes</span></td>";
      else extraFields += "<td><span class=\"badge custom-badge-green\">No</span></td>";
    }
  }

  // Add it all together
  var returnString = "<tr value=" + ITEMS.indexOf(item) + ">" +
  "<td>" +  iconDiv + name + "</td>" + 
  extraFields +
  //"<td>" + roundPrice(item["mean"]) + "</td>" + 
  "<td><span class='sparkline-small mr-2'>Loading</span>" + roundPrice(item["mean"]) + "</td>" + 
  "<td>" + roundPrice(item["quantity"]) + "</td>" +
  "<td>" + countBadge + "</td>" + 
  "</tr>";

  return returnString;
}


function getUrlParameter(sParam) {
  var sPageURL = decodeURIComponent(window.location.search.substring(1)),
    sURLVariables = sPageURL.split('&'),
    sParameterName,
    i;

  for (i = 0; i < sURLVariables.length; i++) {
    sParameterName = sURLVariables[i].split('=');

    if (sParameterName[0] === sParam) {
      return sParameterName[1] === undefined ? true : sParameterName[1];
    }
  }
};


function roundPrice(price) {
  return Math.round(price * PRICE_PERCISION) / PRICE_PERCISION;
}


function sortResults() {
  const sparklineOptions = {
    width: "50px",
    height: "20px",
    spotRadius: 0,
    lineColor: "#222",
    fillColor: "#aaa",
    highlightLineColor: "#000",
    highlightSpotColor: "#666",
    minSpotColor: false,
    maxSpotColor: false,
    spotColor: false,
    type: "line",
    lineWidth: 2,
    disableInteraction: true,
    disableTooltips: true,
    disableHighlight: true
  };

  // Empty the table
  $("#searchResults > tbody").empty();

  // Format input
  var splitInput = FILTER.search.split(" ");

  var tableData = "";
  for (let index = 0; index < ITEMS.length; index++) {
    const item = ITEMS[index];
    let matchCount = 0;

    // Hide harbinger pieces of shit. This is temporary
    if (item["child"] === "piece") continue;

    // Hide low confidence items
    if (FILTER.hideLowConfidence && item["count"] < COUNT_MED) continue;

    // Hide items with different links
    if (FILTER.links) {
      if (!("links" in item) || item["links"] !== FILTER.links) continue;
    } else if ("links" in item) continue;

    // Sort gems, I guess
    if (item["frame"] === 4) {
      if (FILTER.gemLvl !== "") {
        if (item["lvl"] != FILTER.gemLvl) continue;
      }
      if (FILTER.gemQuality !== "") {
        if (FILTER.gemQuality) {
          if (!("quality" in item)) continue;
          if (item["quality"] != FILTER.gemQuality) continue;
        } else {
          if ("quality" in item && item["quality"] > 0) continue;
        }
      }
      if (FILTER.gemCorrupted === "1") {
        if (!("corrupted" in item)) continue;
        if (!item["corrupted"]) continue;
      } else if (FILTER.gemCorrupted === "0") {
        if ("corrupted" in item) continue;
        if (item["corrupted"]) continue;
      }
    }
    
    // Search string matching
    for (let i = 0; i < splitInput.length; i++) {
      if ("name" in item && item["name"].toLowerCase().indexOf(splitInput[i]) !== -1) matchCount++;
      else if ("parent" in item && item["parent"].toLowerCase().indexOf(splitInput[i]) !== -1) matchCount++;
      else if ("child" in item && item["child"].toLowerCase().indexOf(splitInput[i]) !== -1) matchCount++;
      else if ("type" in item && item["type"].toLowerCase().indexOf(splitInput[i]) !== -1) matchCount++;
    }

    if (matchCount === splitInput.length) {
      if ("tableData" in item) {
        tableData += item["tableData"]
      } else {
        item["tableData"] = parseItem(item);
        tableData += item["tableData"];
      }
    }

    var el = $(item["tableData"]);
    $("#searchResults").append(el);
    $(".sparkline-small", el).sparkline(item["history"]["mean"], sparklineOptions);
  }
}