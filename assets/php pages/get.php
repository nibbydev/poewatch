<?php
    // Set header to json
    header('Content-Type: application/json');

    // Get url params and allow only alphanumeric inputs
    $PARAM_league = preg_replace("/[^A-Za-z ]/", '', $_GET["league"]);
    $PARAM_fields = preg_replace("/[^A-Za-z,]/", '', $_GET["fields"]);
    $PARAM_parent = preg_replace("/[^A-Za-z]/", '', $_GET["category"]);
    $PARAM_child = preg_replace("/[^A-Za-z]/", '', $_GET["sub"]);
    $PARAM_from = preg_replace("/[^0-9]/", '', $_GET["from"]);
    $PARAM_to = preg_replace("/[^0-9]/", '', $_GET["to"]);

    // Trim and format whatever's left
    $PARAM_league = ucwords(strtolower(trim($PARAM_league)));
    $PARAM_fields = explode(',', trim($PARAM_fields));
    $PARAM_parent = strtolower(trim($PARAM_parent));
    $PARAM_child = strtolower(trim($PARAM_child));
    $PARAM_from = (int)trim($PARAM_from);
    $PARAM_to = (int)trim($PARAM_to);

    // Don't run with missing params
    if (!$PARAM_league || !$PARAM_parent) {
        echo "{\"error\": \"Missing params\"}";
        return;
    }
    // Don't run with too many params
    if (sizeof($PARAM_fields) > 20) {
        echo "{\"error\": \"Too many params\"}";
        return;
    }

    // If user requested whole file
    if ($PARAM_parent === "all") $PARAM_parent = null;
    // If user requested whole category
    if ($PARAM_child === "all") $PARAM_child = null;

    // Get JSON object from file
    $jsonFile = json_decode( file_get_contents(getcwd() . "/data/" . $PARAM_league . ".json") , true );
    $payload = [];
    $counter = 0;

    // Loop through all categories
    foreach ($jsonFile as $parentCategoryKey => $itemList) {
        if ($PARAM_parent && $PARAM_parent !== $parentCategoryKey) continue;

        // Loop through the JSON file
        foreach ($itemList as $itemKey => $item) {
            if ($PARAM_child && $item["category"] && $PARAM_child !== $item["category"]) continue;
            
            $counter++;
            // Set starting position
            if ($PARAM_from > 0 && $counter <= $PARAM_from) continue;
            // Set ending position
            if ($PARAM_to > 0 && $counter > $PARAM_to) break;

            // Null the variables
            $name = null; 
            $type = null; 
            $variant = null; 
            $frameType = null; 
            $links = null; 
            $lvl = null; 
            $quality = null; 
            $corrupted = null; 

            $splitKey = explode("|", $itemKey);

            $frameType = (int)$splitKey[1];

            if ($frameType === 4) {
                $name = $splitKey[0];

                $lvl = (int)substr($splitKey[2], 2);
                $quality = (int)substr($splitKey[3], 2);

                $corrupted = ($splitKey[4] === "c:1" ? true : false);
            } else {
                if (strpos($splitKey[0], ":") != false) {
                    $name = explode(":", $splitKey[0])[0];
                    $type = explode(":", $splitKey[0])[1];
                } else {
                    $name = $splitKey[0];
                }
                
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
                "index" => $item["index"],
                "name" => $name,
                "frameType" => $frameType
            ];

            if (!$PARAM_parent) $tempPayload["parent"] = $parentCategoryKey;
            if (!$PARAM_child && $item["category"]) $tempPayload["child"] = $item["category"];

            // $name, $type, $variant, $frameType, $links, $lvl, $quality, $corrupted;
            if (!is_null($type)) $tempPayload["type"] = $type;
            if (!is_null($variant)) $tempPayload["variant"] = $variant;
            if (!is_null($links)) $tempPayload["links"] = $links;
            if (!is_null($lvl)) $tempPayload["lvl"] = $lvl;
            if (!is_null($quality)) $tempPayload["quality"] = $quality;
            if (!is_null($corrupted)) $tempPayload["corrupted"] = $corrupted;

            array_push($payload, $tempPayload);
        }
    }

    echo json_encode( $payload, true );
?>