<?php
    // Set header to json
    header('Content-Type: application/json');

    // Get url params and allow only numeric inputs
    $paramIndex = preg_replace("/[^0-9,]/", '', $_GET["index"]);

    // Get JSON file
    $jsonFile = json_decode( file_get_contents(dirname(getcwd(), 2) . "/data/itemData.json"), true );

    // If the user didn't specify the indexes, echo the whole file
    if (!$paramIndex && $paramIndex !== "0") {
        echo json_encode( $jsonFile, true );
        return;
    }

    $payload = new stdClass;
    $indexList = explode(",", trim($paramIndex));

    if (sizeof($indexList) > 100) {
        echo "{\"error\": \"Too many params\"}";
        return;
    }
    
    // Loop through the JSON file
    foreach ($indexList as $inputIndex) {
        // There was no value
        if (!$inputIndex && $inputIndex !== "0") continue;

        // Make sure it's a legit index
        if ((int)$inputIndex >= 0) $payload->{$inputIndex} = $jsonFile[$inputIndex]["url"];
    }

    // echo constructed payload
    echo json_encode( (array) $payload, true );
?>