// Define "global" variables (ie the largest scope here)
const SELECTED;

// Load conversion rates on page load
$(document).ready(function() {
    var selectorLeague = $("#search-league");
    var selectorSub = $("#search-sub");

    SELECTED = {
        league: selectorLeague.find(":selected").text(),
        category: getUrlParameter("category"),
        sub: selectorSub.find(":selected").text()
    };

    // Define event listener
    selectorLeague.on("change", function(){
        SELECTED.league = $(this).find(":selected").text();
        makeRequest();
    });
    // Define event listener
    selectorSub.on("change", function(){
        SELECTED.sub = $(this).find(":selected").text();
        makeRequest();
    });

    makeRequest();
});

function makeRequest() {
    var request = $.ajax({
        url: "http://api.poe.ovh/get",
        data: {
            async: true,
            league: $("#search-league").find(":selected").text(), 
            category: getUrlParameter("category"),
            sub: $("#search-sub").find(":selected").text()
        },
        type: "GET",
        dataTypes: "json"
    });

    request.done(function(json) {
        var tableData = "";
        $("#searchResults tbody tr").empty();

        for (let i = 0; i < json.length; i++) {
            const item = json[i];
            
            var type;
            if (item["count"] > 100) {
                type = "custom-badge-green";
            } else if (item["count"] > 50) {
                type = "custom-badge-orange";
            } else {
                type = "custom-badge-red";
            }

            var icon;
            if (item["icon"].length < 3) {
                icon = "assets/img/missing.png";
            } else {
                icon = item["icon"];
            }

            var name = item["name"];
            if (item["type"]) name += ", " + item["type"];
            if (item["links"]) name += " <span class=\"badge custom-badge-gray\">" + item["links"] + " link</span>";
            if (item["variant"]) name += " <span class=\"badge custom-badge-gray\">" + item["variant"] + "</span>";

            if (item["frameType"] === 4) {
                if (item["lvl"]) name += " <span class=\"badge custom-badge-gray\">level " + item["lvl"] + "</span>";
                if (item["quality"]) name += " <span class=\"badge custom-badge-gray\">quality " + item["quality"] + "</span>";
                if (item["corrupted"]) name += " <span class=\"badge custom-badge-red\">Corrupted</span>";
            }
            
            tableData += "<tr>" +
            "<td>" + 
                "<div class=\"table-img-container\"><img src=\"" + icon + "?scale=1&w=1&h=1\"></div>" + name + 
            "</td>" + 
            "<td>" + Math.round(item["mean"] * 10) / 10 + "</td>" + 
            "<td>" + Math.round(item["median"] * 10) / 10 + "</td>" + 
            "<td>" + Math.round(item["mode"] * 10) / 10 + "</td>" +
            "<td><span class=\"badge "+type+"\">" + item["count"] + "</span></td>" + 
            "</tr>";
        }

        $("#searchResults").append(tableData);
    });
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