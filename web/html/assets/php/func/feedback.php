<?php
function getClientIP(){       
  if (array_key_exists('HTTP_X_FORWARDED_FOR', $_SERVER)) {
    return  $_SERVER["HTTP_X_FORWARDED_FOR"];  
  } else if (array_key_exists('REMOTE_ADDR', $_SERVER)) { 
    return $_SERVER["REMOTE_ADDR"]; 
  } else if (array_key_exists('HTTP_CLIENT_IP', $_SERVER)) {
    return $_SERVER["HTTP_CLIENT_IP"]; 
  } 

  return '';
}

function createStatusMsg($msg, $status) {
  return array(
    'status'  => $status,
    'message' => $msg
  );
}

function GetIpPostLimit($pdo, $clientIP) {
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

function CreateDbEntry($pdo, $contact, $message, $clientIP) {
  $query = "insert into web_feedback (ip_crc, contact, message) values (crc32(?), ?, ?)";
 
  $stmt = $pdo->prepare($query);
  return $stmt->execute([$clientIP, $contact, $message]);
}

function processPOST($pdo) {
  if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    return null;
  }

  if (empty($_POST) || !isset($_POST['message']) || !isset($_POST['contact'])) {
    return createStatusMsg('Invalid fields', 'error');
  }

  $message = trim($_POST['message']);
  $contact = trim($_POST['contact']);
  $clientIP = getClientIP();

  if (strlen($message) < 16) {
    return createStatusMsg('Message too short', 'error');
  } else if (strlen($contact) < 4) {
    return createStatusMsg('Contact address too short', 'error');
  }

  if (strlen($message) > 1024) {
    return createStatusMsg('Message too long', 'error');
  } else if (strlen($contact) > 128) {
    return createStatusMsg('Contact too long', 'error');
  }

  // Check if there have been more than x posts from the same ip in the past hour
  if (GetIpPostLimit($pdo, $clientIP) > 1) {
    return createStatusMsg('Whoa, too many messages', 'error');
  }

  if (CreateDbEntry($pdo, $contact, $message, $clientIP)) {
    return createStatusMsg('Message sent', 'ok');
  } else {
    return createStatusMsg('Something went wrong', 'error');
  }
}
