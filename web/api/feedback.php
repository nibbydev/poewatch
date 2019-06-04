<?php
function error($code, $msg) {
  http_response_code($code);
  die(json_encode(["error" => $msg]));
}

function check_errors() {
  if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    error(405, "Invalid request method");
  }

  if (!isset($_POST['message'])) {
    error(400, "Missing message");
  }

  if (!isset($_POST['contact'])) {
    error(400, "Missing contact address");
  }

  if (strlen($_POST["message"]) > 2048) {
    error(400, "Message too long (2048 max)");
  }

  if (strlen($_POST["message"]) < 16) {
    error(400, "Message too short (16 min)");
  }


  if (strlen($_POST["contact"]) > 128) {
    error(400, "Contact address too long (128 max)");
  }

  if (strlen($_POST["contact"]) < 4) {
    error(400, "Contact address too short (4 min)");
  }
}

function getIpPostLimit($pdo, $clientIP) {
  $query = "
    select count(*) as count
    from web_feedback 
    where ip_crc = crc32(?) 
    and time > date_sub(now(), interval 1 hour)
  ";

  $stmt = $pdo->prepare($query);
  $stmt->execute([$clientIP]);

  return $stmt->fetch()['count'];
}

function getIp() {
  $client_ip = '';

  if (array_key_exists('HTTP_X_FORWARDED_FOR', $_SERVER)) {
    $client_ip = $_SERVER["HTTP_X_FORWARDED_FOR"];
  } else if (array_key_exists('REMOTE_ADDR', $_SERVER)) {
    $client_ip = $_SERVER["REMOTE_ADDR"];
  } else if (array_key_exists('HTTP_CLIENT_IP', $_SERVER)) {
    $client_ip = $_SERVER["HTTP_CLIENT_IP"];
  }

  return $client_ip;
}

function createDbEntry($pdo, $clientIP, $contact, $message) {
  $query = "insert into web_feedback (ip_crc, contact, message) values (crc32(?), ?, ?)";

  $stmt = $pdo->prepare($query);
  return $stmt->execute([$clientIP, $contact, $message]);
}

function checkLimit($pdo, $client_ip) {
  $postCount = getIpPostLimit($pdo, $client_ip);

  // Check if there have been more than x posts from the same ip in the past hour
  if ($postCount > 2) {
    error(403, "Too many messages");
  }
}

header("Content-Type: application/json");
check_errors();
include_once("../details/pdo.php");

$client_ip = getIp();
// Check post count for this ip
checkLimit($pdo, $client_ip);
createDbEntry($pdo, $client_ip, $_POST["contact"], $_POST["message"]);

echo json_encode(["error" => null]);