<?php
declare(strict_types=1);

require_once __DIR__ . "/config.php";

function firebase_project_id(): string
{
    $projectId = getenv('FIREBASE_PROJECT_ID');
    if (!is_string($projectId) || trim($projectId) === '') {
        throw new RuntimeException("FIREBASE_PROJECT_ID is not set.");
    }
    return trim($projectId);
}

function firebase_api_key(): string
{
    $apiKey = getenv('FIREBASE_API_KEY');
    if (!is_string($apiKey) || trim($apiKey) === '') {
        throw new RuntimeException("FIREBASE_API_KEY is not set.");
    }
    return trim($apiKey);
}

function firebase_credentials_path(): string
{
    $path = getenv('FIREBASE_CREDENTIALS');
    if (!is_string($path) || trim($path) === '' || !file_exists($path)) {
        throw new RuntimeException("FIREBASE_CREDENTIALS file is missing.");
    }
    return $path;
}

function firebase_service_account(): array
{
    $json = file_get_contents(firebase_credentials_path());
    $data = json_decode((string) $json, true);
    if (!is_array($data) || empty($data['client_email']) || empty($data['private_key']) || empty($data['token_uri'])) {
        throw new RuntimeException("Invalid Firebase service account JSON.");
    }
    return $data;
}

function base64url(string $data): string
{
    return rtrim(strtr(base64_encode($data), '+/', '-_'), '=');
}

function firebase_access_token(): string
{
    static $cachedToken = null;
    static $cachedExp = 0;

    if ($cachedToken && $cachedExp > time() + 60) {
        return $cachedToken;
    }

    $sa = firebase_service_account();
    $now = time();
    $header = base64url(json_encode(['alg' => 'RS256', 'typ' => 'JWT']));
    $payload = base64url(json_encode([
        'iss' => $sa['client_email'],
        'scope' => 'https://www.googleapis.com/auth/datastore',
        'aud' => $sa['token_uri'],
        'iat' => $now,
        'exp' => $now + 3600,
    ]));
    $signatureInput = $header . '.' . $payload;
    $signature = '';
    $ok = openssl_sign($signatureInput, $signature, $sa['private_key'], 'sha256');
    if (!$ok) {
        throw new RuntimeException("Failed to sign JWT.");
    }
    $jwt = $signatureInput . '.' . base64url($signature);

    $ch = curl_init($sa['token_uri']);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_HTTPHEADER, ['Content-Type: application/x-www-form-urlencoded']);
    curl_setopt($ch, CURLOPT_POST, true);
    curl_setopt($ch, CURLOPT_POSTFIELDS, http_build_query([
        'grant_type' => 'urn:ietf:params:oauth:grant-type:jwt-bearer',
        'assertion' => $jwt,
    ]));
    $response = curl_exec($ch);
    $error = curl_error($ch);
    curl_close($ch);

    if ($response === false) {
        throw new RuntimeException("Token request failed: " . $error);
    }

    $decoded = json_decode($response, true);
    if (!is_array($decoded) || empty($decoded['access_token'])) {
        throw new RuntimeException("Failed to obtain access token.");
    }

    $cachedToken = $decoded['access_token'];
    $cachedExp = $now + (int) ($decoded['expires_in'] ?? 3600);
    return $cachedToken;
}

function firebase_request(string $url, array $payload): array
{
    $ch = curl_init($url);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_HTTPHEADER, ['Content-Type: application/json']);
    curl_setopt($ch, CURLOPT_POST, true);
    curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($payload));
    $response = curl_exec($ch);
    $error = curl_error($ch);
    curl_close($ch);

    if ($response === false) {
        throw new RuntimeException("Firebase request failed: " . $error);
    }

    $decoded = json_decode($response, true);
    if (isset($decoded['error'])) {
        $message = is_array($decoded['error']) ? ($decoded['error']['message'] ?? 'Unknown error') : $decoded['error'];
        throw new RuntimeException("Firebase error: " . $message);
    }
    return $decoded ?? [];
}

function firebase_create_user(string $email, string $password, string $displayName, string $phone): array
{
    $url = "https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=" . firebase_api_key();
    $result = firebase_request($url, [
        'email' => $email,
        'password' => $password,
        'returnSecureToken' => true,
    ]);

    if (!empty($displayName) || !empty($phone)) {
        $updateUrl = "https://identitytoolkit.googleapis.com/v1/accounts:update?key=" . firebase_api_key();
        $payload = [
            'idToken' => $result['idToken'] ?? '',
            'returnSecureToken' => true,
        ];
        if (!empty($displayName)) {
            $payload['displayName'] = $displayName;
        }
        if (!empty($phone)) {
            $payload['phoneNumber'] = $phone;
        }
        try {
            firebase_request($updateUrl, $payload);
        } catch (RuntimeException $e) {
            // Ignore phone update errors (e.g. invalid E.164 format) but continue.
        }
    }

    return $result;
}

function firebase_sign_in(string $email, string $password): array
{
    $url = "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=" . firebase_api_key();
    return firebase_request($url, [
        'email' => $email,
        'password' => $password,
        'returnSecureToken' => true,
    ]);
}

function firestore_field_value(mixed $value): array
{
    if ($value === null) {
        return ['nullValue' => null];
    }
    if (is_bool($value)) {
        return ['booleanValue' => $value];
    }
    if (is_int($value)) {
        return ['integerValue' => (string) $value];
    }
    if (is_float($value)) {
        return ['doubleValue' => $value];
    }
    return ['stringValue' => (string) $value];
}

function firestore_encode_fields(array $profile): array
{
    $fields = [];
    foreach ($profile as $key => $value) {
        $fields[$key] = firestore_field_value($value);
    }
    return $fields;
}

function firestore_decode_fields(array $fields): array
{
    $result = [];
    foreach ($fields as $key => $value) {
        if (isset($value['stringValue'])) {
            $result[$key] = $value['stringValue'];
        } elseif (isset($value['integerValue'])) {
            $result[$key] = (int) $value['integerValue'];
        } elseif (isset($value['doubleValue'])) {
            $result[$key] = (float) $value['doubleValue'];
        } elseif (isset($value['booleanValue'])) {
            $result[$key] = (bool) $value['booleanValue'];
        } else {
            $result[$key] = null;
        }
    }
    return $result;
}

function firestore_save_admin(string $uid, array $profile): void
{
    $projectId = firebase_project_id();
    $url = "https://firestore.googleapis.com/v1/projects/{$projectId}/databases/(default)/documents/admins/{$uid}";
    $payload = ['fields' => firestore_encode_fields($profile)];

    $ch = curl_init($url);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_CUSTOMREQUEST, 'PATCH');
    curl_setopt($ch, CURLOPT_HTTPHEADER, [
        'Content-Type: application/json',
        'Authorization: Bearer ' . firebase_access_token(),
    ]);
    curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($payload));
    $response = curl_exec($ch);
    $error = curl_error($ch);
    $status = (int) curl_getinfo($ch, CURLINFO_RESPONSE_CODE);
    curl_close($ch);

    if ($response === false) {
        throw new RuntimeException("Firestore request failed: " . $error);
    }
    if ($status >= 300) {
        throw new RuntimeException("Firestore save failed.");
    }
}

function firestore_get_admin(string $uid): ?array
{
    $projectId = firebase_project_id();
    $url = "https://firestore.googleapis.com/v1/projects/{$projectId}/databases/(default)/documents/admins/{$uid}";

    $ch = curl_init($url);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_HTTPHEADER, [
        'Authorization: Bearer ' . firebase_access_token(),
    ]);
    $response = curl_exec($ch);
    $error = curl_error($ch);
    $status = (int) curl_getinfo($ch, CURLINFO_RESPONSE_CODE);
    curl_close($ch);

    if ($response === false) {
        throw new RuntimeException("Firestore request failed: " . $error);
    }
    if ($status === 404) {
        return null;
    }
    if ($status >= 300) {
        throw new RuntimeException("Firestore get failed.");
    }

    $decoded = json_decode($response, true);
    if (!is_array($decoded) || empty($decoded['fields'])) {
        return null;
    }
    return firestore_decode_fields($decoded['fields']);
}
