// Define "global" variable (ie the largest scope here)
let SELECTED;
var ITEMS = [];
const INITIAL_LOAD_AMOUNT = 100;
const PRICE_PERCISION = 100;
var currentlyLoaded = 0;

const COUNT_HIGH = 25;
const COUNT_MED = 15;

// User-modifiable variables
var HIDE_LOW_CONFIDENCE = true;
var LINK_FILTER = 0;
var SEARCH_INPUT = "";
var GEM_LEVEL = "-1";
var GEM_QUALITY = "-1";
var GEM_CORRUPTED = "-1";

// Load conversion rates on page load
$(document).ready(function() {
    var selectorLeague = $("#search-league");
    var selectorSub = $("#search-sub");
    var buttonLoadmore = $("#button-loadmore");

    SELECTED = {
        league: selectorLeague.find(":selected").text(),
        category: getUrlParameter("category"),
        sub: selectorSub.find(":selected").text()
    };

    // Define league event listener
    selectorLeague.change(function(){
        // Get field that changed
        SELECTED.league = $(this).find(":selected").text();
        // Empty item history
        ITEMS = [];

        makeRequest(0, INITIAL_LOAD_AMOUNT);
    });

    // Define subcategory event listener
    selectorSub.change(function(){
        // Get field that changed
        SELECTED.sub = $(this).find(":selected").text();
        // Empty item history
        ITEMS = [];

        makeRequest(0, INITIAL_LOAD_AMOUNT);
    });

    // Define load more event listener
    buttonLoadmore.on("click", function(){
        makeRequest(INITIAL_LOAD_AMOUNT, 0);
        $(this).hide();
    });

    // Define searchbar event listener
    $("#search-searchbar").on("input", function(){
        SEARCH_INPUT = $(this).val();
        sortResults();
    });

    // Define low confidence radio button event listener
    $("#radio-confidence").on("change", function(){
        let newValue = $("input[name=confidence]:checked", this).val();
        if (newValue != HIDE_LOW_CONFIDENCE) {
            HIDE_LOW_CONFIDENCE = !HIDE_LOW_CONFIDENCE;
            sortResults();
        }
    });

    // Define link radio button event listener
    $("#radio-links").on("change", function(){
        LINK_FILTER = parseInt($("input[name=links]:checked", this).val());
        sortResults();
    });

    // Define gem selector and radio event listeners
    $("#select-level").on("change", function(){
        GEM_LEVEL = $(":selected", this).val();
        sortResults();
    });
    $("#select-quality").on("change", function(){
        GEM_QUALITY = $(":selected", this).val();
        sortResults();
    });
    $("#radio-corrupted").on("change", function(){
        GEM_CORRUPTED = $(":checked", this).val();
        sortResults();
    });

    makeRequest(0, INITIAL_LOAD_AMOUNT);
}); 


function makeRequest(from, to) {
    var request = $.ajax({
        url: "http://api.poe.ovh/get",
        data: {
            league: SELECTED.league, 
            category: SELECTED.category,
            sub: SELECTED.sub,
            from: from,
            to: to
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


function parseItem(item) {
    // Format count badge
    var countBadge;
    if (item["count"] >= COUNT_HIGH) countBadge = "<span class=\"badge custom-badge-green\">" + item["count"] + "</span>";
    else if (item["count"] >= COUNT_MED) countBadge = "<span class=\"badge custom-badge-orange\">" + item["count"] + "</span>";
    else countBadge = "<span class=\"badge custom-badge-red\">" + item["count"] + "</span>";

    // Format icon
    var iconDiv = "<div class=\"table-img-container\"><img src=\"" + 
    (item["icon"] ? item["icon"] : "http://poe.ovh/assets/img/missing.png") + "\"></div>";

    // Format variant/links badge
    var name = item["name"];
    if (item["type"]) name += ", " + item["type"];
    if (item["variant"]) name += " <span class=\"badge custom-badge-gray\">" + item["variant"] + "</span>";

    // Add gem/map extra data
    var extraFields = "";
    if (item["frame"] === 4) {
        if ("lvl" in item) extraFields += "<td>" + item["lvl"] + "</td>";
        else extraFields += "<td>0</td>";
        
        if ("quality" in item) extraFields += "<td>" + item["quality"] + "</td>";
        else extraFields += "<td>0</td>";

        if ("corrupted" in item) extraFields += "<td><span class=\"badge custom-badge-red\">Yes</span></td>";
        else extraFields += "<td>No</td>";
    }

    // Add it all together
    var returnString = "<tr>" +
    "<td>" +  iconDiv + name + ("tier" in item ? " <span class=\"badge custom-badge-gray\">" + item["tier"] + "</span>" : "") + "</td>" + 
    extraFields +
    //"<td>" + roundPrice(item["mean"]) + "</td>" + 
    "<td>" + roundPrice(item["mean"]) + "</td>" + 
    //"<td>" + roundPrice(item["mode"]) + "</td>" +
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
    var splitInput = SEARCH_INPUT.toLowerCase().trim().split(" ");

    var tableData = "";
    ITEMS.forEach(item => {
        let matchCount = 0;

        // Hide low confidence items
        if (HIDE_LOW_CONFIDENCE && item["count"] < COUNT_MED) return;
        // Hide items with different links
        if (LINK_FILTER) {
            if (!("links" in item) || item["links"] !== LINK_FILTER) return;
        } else if ("links" in item) return;
        // Sort gems, I guess
        if (item["frame"] === 4) {
            if (GEM_LEVEL !== "-1") {
                if (item["lvl"] != GEM_LEVEL) return;
            }
            if (GEM_QUALITY !== "-1") {
                if (GEM_QUALITY) {
                    if (!("quality" in item)) return;
                    if (item["quality"] != GEM_QUALITY) return;
                } else {
                    if ("quality" in item && item["quality"] > 0) return;
                }
            }
            if (GEM_CORRUPTED !== "-1") {
                if (GEM_CORRUPTED == true) {
                    if (!("corrupted" in item)) return;
                    if (!item["corrupted"]) return;
                } else {
                    if ("corrupted" in item) return;
                    if (item["corrupted"]) return;
                }
            }
        }
        
        for (let i = 0; i < splitInput.length; i++) {
            if ("name" in item && item["name"].toLowerCase().indexOf(splitInput[i]) !== -1) matchCount++;
            else if ("category" in item && item["category"].toLowerCase().indexOf(splitInput[i]) !== -1) matchCount++;
            else if ("type" in item && item["type"].toLowerCase().indexOf(splitInput[i]) !== -1) matchCount++;
        }

        if (matchCount === splitInput.length) tableData += parseItem(item);
    });

    // Fill the table
    $("#searchResults").append(tableData);
}