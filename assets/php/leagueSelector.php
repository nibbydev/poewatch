<?php
    $jsonFile = json_decode( file_get_contents( dirname(getcwd(), 2) . "/leagues.json"), true );

    foreach ($jsonFile as $leagueName) {
        echo "<option>" . $leagueName . "</option>";
    }
?>