// Define "global" variable (ie the largest scope here)
let SELECTED;
var ITEMS = [];
const INITIAL_LOAD_AMOUNT = 50;
const PRICE_PERCISION = 100;
var currentlyLoaded = 0;

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
        searchTheResults($(this).val());
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
        // Empty the table
        $("#searchResults > tbody").empty();
        // Loop through all items, parse them and append to displaystring
        var tableData = "";
        ITEMS.forEach(item => {
            // Add it all together
            tableData += parseItem(item);
            // Inc loaded item counter
            currentlyLoaded++;
        });

        // Add parsed items to the table (table might be cleared in previous function)
        $("#searchResults").append(tableData);
        // Enable "show more" button
        if (ITEMS.length === INITIAL_LOAD_AMOUNT) $("#button-loadmore").show();
    });
}


function parseItem(item) {
    // Check if the item has already been displayed
    //if ("parsed" in item && item["parsed"] === true) return;
    // Mark the item as "parsed"
    //item["parsed"] = true;

    // Format count badge
    var countBadge;
    if (item["count"] > 50) countBadge = "<span class=\"badge custom-badge-green\">" + item["count"] + "</span>";
    else if (item["count"] > 20) countBadge = "<span class=\"badge custom-badge-orange\">" + item["count"] + "</span>";
    else countBadge = "<span class=\"badge custom-badge-red\">" + item["count"] + "</span>";

    // Format icon
    var iconDiv = "<div class=\"table-img-container\"><img src=\"" + 
    (item["icon"] ? item["icon"] : "http://poe.ovh/assets/img/missing.png") + "\"></div>";

    // Format variant/links badge
    var name = item["name"];
    if (item["type"]) name += ", " + item["type"];
    if (item["links"]) name += " <span class=\"badge custom-badge-gray\">" + item["links"] + " link</span>";
    if (item["variant"]) name += " <span class=\"badge custom-badge-gray\">" + item["variant"] + "</span>";

    // Add gem badge
    if (item["frameType"] === 4) {
        if (item["lvl"]) name += " <span class=\"badge custom-badge-gray\">level " + item["lvl"] + "</span>";
        if (item["quality"]) name += " <span class=\"badge custom-badge-gray\">quality " + item["quality"] + "</span>";
        if (item["corrupted"]) name += " <span class=\"badge custom-badge-red\">Corrupted</span>";
    }

    // Add it all together
    var returnString = "<tr>" +
    "<td>" +  iconDiv + name + "</td>" + 
    "<td>" + roundPrice(item["mean"]) + "</td>" + 
    "<td>" + roundPrice(item["median"]) + "</td>" + 
    "<td>" + roundPrice(item["mode"]) + "</td>" +
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


function searchTheResults(input) {
    // Empty the table
    $("#searchResults > tbody").empty();

    // Format input
    var splitInput = input.toLowerCase().trim().split(" ");

    var tableData = "";
    ITEMS.forEach(item => {
        let matchCount = 0;
        
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