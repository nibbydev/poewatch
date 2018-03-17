<?php
    $league = $_GET["league"];
    $category = $_GET["category"];
    $sub = $_GET["sub"];

    if (is_null($league)) $league = "Standard";
    if (is_null($category)) {
        $category = "accessories";
        $sub = "amulet";
    }

    $jsonFile = json_decode( file_get_contents("data/" . $league . ".json"), true );
    $counter = 0;
    
    foreach ($jsonFile[$category . ":" . $sub] as $key=>$value) {
        $row_data = "<tr><td><div class=\"table-img-container\"><img src=\"http://web.poecdn.com/image/Art/2DItems/Armours/Helmets/AlphasHowlAlt.png?scale=1&scaleIndex=0&w=2&h=4\"></div></td>";
        $row_data .= "<td>" . str_replace("|", ", ", $key) . "</td>";
        $row_data .= "<td>" . $value["mean"] . "</td>";
        $row_data .= "<td>" . $value["median"] . "</td>";
        $row_data .= "<td>" . $value["mode"] . "</td>";
        $row_data .= "<td>" . $value["inc"] . "</td>";
        $row_data .= "<td>" . $value["count"] . "</td>";
        $row_data .= "</tr>";

        echo $row_data;

        //if ($counter > 25) break;
        $counter++;
    }

    
?>