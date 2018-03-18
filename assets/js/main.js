// Define "global" variable (ie the largest scope here)
let SELECTED;
var ITEMS = [];
const initialLoadAmount = 50;
const PRICE_PERCISION = 100;

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

    // Define event listener
    selectorLeague.change(function(){
        SELECTED.league = $(this).find(":selected").text();
        ITEMS = [];
        $("#searchResults > tbody").empty();
        makeRequest(0, initialLoadAmount);
    });
    // Define event listener
    selectorSub.change(function(){
        SELECTED.sub = $(this).find(":selected").text();
        ITEMS = [];
        $("#searchResults > tbody").empty();
        makeRequest(0, initialLoadAmount);
    });
    // Define event listener
    buttonLoadmore.on("click", function(){
        makeRequest(initialLoadAmount, 0);
        $(this).hide();
    });

    makeRequest(0, initialLoadAmount);
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
        // Parse the downloaded items without parsing already displayed items
        displayData = parseItems(false);
        // Add parsed items to the table (table might be cleared in previous function)
        $("#searchResults").append(displayData);
    });
}

function parseItems(parseAgain) {
    // This is the displaystring that will be returned
    var tableData = "";

    // Loop through all items, parse them and append to displaystring
    ITEMS.forEach(item => {
        // Check if the item has already been displayed
        if ("parsed" in item && item["parsed"] === true) {
            // Check if the item should be displayed again
            if (parseAgain) return;
        }
        // Mark the item as "parsed"
        item["parsed"] = true;

        // Format count badge
        var countBadge;
        if (item["count"] > 100) countBadge = "<span class=\"badge custom-badge-green\">" + item["count"] + "</span>";
        else if (item["count"] > 50) countBadge = "<span class=\"badge custom-badge-orange\">" + item["count"] + "</span>";
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
        tableData += "<tr>" +
        "<td>" +  iconDiv + name + "</td>" + 
        "<td>" + roundPrice(item["mean"]) + "</td>" + 
        "<td>" + roundPrice(item["median"]) + "</td>" + 
        "<td>" + roundPrice(item["mode"]) + "</td>" +
        "<td>" + countBadge + "</td>" + 
        "</tr>";
    });

    // Return the filled tableData
    return tableData;
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