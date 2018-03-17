<?php
    $parent = trim(strtolower($_GET["category"]));
    if (!$parent) $parent = "currency";

    $jsonFile = json_decode( file_get_contents(dirname(getcwd(), 2) . "/categories.json"), true );

    // Add option for all
    echo "<option>All</option>";

    if (!array_key_exists($parent, $jsonFile)) return;

    foreach ($jsonFile[$parent] as $item) {
        echo "<option>" . ucwords($item) . "</option>";
    }
?>