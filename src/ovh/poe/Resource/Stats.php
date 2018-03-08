<?php
    header('Content-Type: application/json');
    echo file_get_contents( "./data/" . $_GET["league"] . ".json" );
?>
