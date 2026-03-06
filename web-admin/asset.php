<?php
declare(strict_types=1);

$allowedFiles = [
    'ic_banner.png',
    'ic_plumbing.png',
    'ic_carpentry.png',
    'ic_aircon_repairs.png',
    'ic_appliance_repair.png',
    'ic_electronics_repair.png',
    'ic_internet_technician.png',
    'ic_electrical_repair.png',
];

$file = basename((string) ($_GET['file'] ?? ''));
if (!in_array($file, $allowedFiles, true)) {
    http_response_code(404);
    exit('Not found');
}

$assetDir = realpath(__DIR__ . "/assets");
$fallbackDir = realpath(__DIR__ . "/../app/src/main/res/drawable");
$path = false;

if ($assetDir && is_file($assetDir . "/" . $file)) {
    $path = realpath($assetDir . "/" . $file);
} elseif ($fallbackDir && is_file($fallbackDir . "/" . $file)) {
    $path = realpath($fallbackDir . "/" . $file);
}

if ($path === false || (!empty($assetDir) && strpos($path, $assetDir) !== 0 && !empty($fallbackDir) && strpos($path, $fallbackDir) !== 0)) {
    http_response_code(404);
    exit('Not found');
}

$ext = strtolower(pathinfo($path, PATHINFO_EXTENSION));
$mime = $ext === 'png' ? 'image/png' : 'application/octet-stream';
header("Content-Type: " . $mime);
header("Cache-Control: public, max-age=86400");
readfile($path);
