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

  $("#searchResults").click(function(){ 
    //console.log("It ran");
    //$("tr", this).css("background-color", "red");
  });

  checkFields();

  makeRequest(0, INITIAL_LOAD_AMOUNT);
}); 


function makeRequest(from, to) {
  var request = $.ajax({
    url: "http://api.poe.ovh/get",
    data: {
      league: FILTER.league, 
      parent: FILTER.category,
      child: FILTER.sub,
      from: from,
      to: to,
      exclude: "median,mode,history,index"
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
  var returnString = "<tr>" +
  "<td>" +  iconDiv + name + "</td>" + 
  extraFields +
  //"<td>" + roundPrice(item["mean"]) + "</td>" + 
  "<td>" + roundPrice(item["mean"]) + "</td>" + 
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
  // Empty the table
  $("#searchResults > tbody").empty();

  // Format input
  var splitInput = FILTER.search.split(" ");

  var tableData = "";
  ITEMS.forEach(item => {
    let matchCount = 0;

    // Hide harbinger pieces of shit. This is temporary
    if (item["child"] === "piece") return;

    // Hide low confidence items
    if (FILTER.hideLowConfidence && item["count"] < COUNT_MED) return;

    // Hide items with different links
    if (FILTER.links) {
      if (!("links" in item) || item["links"] !== FILTER.links) return;
    } else if ("links" in item) return;

    // Sort gems, I guess
    if (item["frame"] === 4) {
      if (FILTER.gemLvl !== "") {
        if (item["lvl"] != FILTER.gemLvl) return;
      }
      if (FILTER.gemQuality !== "") {
        if (FILTER.gemQuality) {
          if (!("quality" in item)) return;
          if (item["quality"] != FILTER.gemQuality) return;
        } else {
          if ("quality" in item && item["quality"] > 0) return;
        }
      }
      if (FILTER.gemCorrupted === "1") {
        if (!("corrupted" in item)) return;
        if (!item["corrupted"]) return;
      } else if (FILTER.gemCorrupted === "0") {
        if ("corrupted" in item) return;
        if (item["corrupted"]) return;
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
  });

  // Fill the table
  $("#searchResults").append(tableData);
}