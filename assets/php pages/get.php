<?php
    // Set header to json
    header('Content-Type: application/json');

    // Get url params and allow only alphanumeric inputs
    $paramLeague = preg_replace("/[^A-Za-z0-9] /", '', $_GET["league"]);
    $paramCategory = preg_replace("/[^A-Za-z0-9]/", '', $_GET["category"]);
    $paramSub = preg_replace("/[^A-Za-z0-9]/", '', $_GET["sub"]);
    $praramFrom = preg_replace("/[^A-Za-z0-9]/", '', $_GET["from"]);
    $paramTo = preg_replace("/[^A-Za-z0-9]/", '', $_GET["to"]);

    // Trim and format whatever's left
    $paramLeague = ucwords(strtolower(trim($paramLeague)));
    $paramCategory = strtolower(trim($paramCategory));
    $paramSub = strtolower(trim($paramSub));
    $praramFrom = (int)trim($praramFrom);
    $paramTo = (int)trim($paramTo);

    // Don't run with missing params
    if (!$paramLeague || !$paramCategory) {
        echo "{\"error\": \"Missing params\"}";
        return;
    }

    // Get league file
    $dataFile = file_get_contents(getcwd() . "/data/" . $paramLeague . ".json");

    // If user requested whole file
    if ($paramCategory === "all") $paramCategory = null;
    // If user requested whole category
    if ($paramSub === "all") $paramSub = null;

    // Get JSON objects from files
    $jsonFile = json_decode( $dataFile, true );
    $payload = [];
    $counter = 0;

    // Loop through all categories
    foreach ($jsonFile as $parentCategoryKey => $itemList) {
        if ($paramCategory && $paramCategory !== $parentCategoryKey) continue;

        // Loop through the JSON file
        foreach ($itemList as $itemKey => $item) {
            if ($paramSub && $item["category"] && $paramSub !== $item["category"]) continue;
            
            $counter++;
            // Set starting position
            if ($praramFrom > 0 && $counter <= $praramFrom) continue;
            // Set ending position
            if ($paramTo > 0 && $counter > $paramTo) break;

            $splitKey = explode("|", $itemKey);

            if ($paramCategory === "gems") {
                $name = $splitKey[0];
                $frameType = (int)$splitKey[1];

                $lvl = substr($splitKey[2], 2);
                $quality = substr($splitKey[3], 2);

                $corrupted = ($splitKey[4] === "c:1" ? true : false);
            } else {
                if (strpos($splitKey[0], ":") != false) {
                    $name = explode(":", $splitKey[0])[0];
                    $type = explode(":", $splitKey[0])[1];
                } else {
                    $name = $splitKey[0];
                }
                
                $frameType = (int)$splitKey[1];

                if (sizeof($splitKey) > 2) {
                    // Can be either "5L" or "var:ar/es/li"
                    if (strpos($splitKey[2], "var:") !== false) {
                        $variant = substr($splitKey[2], 4);
                    } else {
                        $links = (int)substr($splitKey[2], -1);
                        // Check if it has variant info aswell
                        if (sizeof($splitKey) > 3) $variant = substr($splitKey[3], 4);
                    }
                }
            }

            $tempPayload = [
                "mean" => $item["mean"],
                "median" => $item["median"],
                "mode" => $item["mode"],
                "count" => $item["count"],
                "icon" => $item["icon"],
                "index" => $item["index"]
            ];

            if (!$paramCategory) $tempPayload["parent"] = $parentCategoryKey;
            if (!$paramSub && $item["category"]) $tempPayload["child"] = $item["category"];

            // $name, $type, $variant, $frameType, $links, $lvl, $quality, $corrupted;
            if ($name) $tempPayload["name"] = $name;
            if ($type) $tempPayload["type"] = $type;
            if ($variant) $tempPayload["variant"] = $variant;
            if ($links) $tempPayload["links"] = $links;
            if ($frameType) $tempPayload["frameType"] = $frameType;
            if ($lvl) $tempPayload["lvl"] = $lvl;
            if ($quality) $tempPayload["quality"] = $quality;
            if ($corrupted) $tempPayload["corrupted"] = $corrupted;

            array_push($payload, $tempPayload);

            $name = null; 
            $type = null; 
            $variant = null; 
            $frameType = null; 
            $links = null; 
            $lvl = null; 
            $quality = null; 
            $corrupted = null; 
        }
    }

    echo json_encode( $payload, true );
?>