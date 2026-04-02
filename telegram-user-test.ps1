$ErrorActionPreference = "Stop"

$BaseUrl = if ($env:BASE_URL) { $env:BASE_URL } else { "http://localhost:8080" }
$Username = if ($env:APP_USERNAME) { $env:APP_USERNAME } else { "" }
$Password = if ($env:APP_PASSWORD) { $env:APP_PASSWORD } else { "" }
$PhoneNumber = if ($env:TELEGRAM_TEST_PHONE) { $env:TELEGRAM_TEST_PHONE } else { "" }

function Write-Step($message) {
    Write-Host ""
    Write-Host "== $message ==" -ForegroundColor Cyan
}

function Get-JwtToken {
    if (-not $Username) {
        $script:Username = Read-Host "Enter application username with ADMIN role"
    }
    if (-not $Password) {
        $securePassword = Read-Host "Enter application password" -AsSecureString
        $script:Password = [System.Net.NetworkCredential]::new("", $securePassword).Password
    }

    $body = @{
        username = $Username
        password = $Password
    } | ConvertTo-Json

    try {
        $response = Invoke-RestMethod `
            -Method Post `
            -Uri "$BaseUrl/auth/login" `
            -ContentType "application/json" `
            -Body $body
        if (-not $response.token) {
            throw "JWT token is missing in /auth/login response"
        }
        return $response.token
    }
    catch {
        Write-Host "Login failed for user '$Username'." -ForegroundColor Red
        throw
    }
}

function Invoke-ApiJson {
    param(
        [string]$Method,
        [string]$Uri,
        [object]$Body = $null,
        [hashtable]$Headers = @{}
    )

    if ($null -ne $Body) {
        return Invoke-RestMethod `
            -Method $Method `
            -Uri $Uri `
            -Headers $Headers `
            -ContentType "application/json" `
            -Body ($Body | ConvertTo-Json -Depth 10)
    }

    return Invoke-RestMethod `
        -Method $Method `
        -Uri $Uri `
        -Headers $Headers
}

function Read-ErrorResponse {
    param([System.Management.Automation.ErrorRecord]$ErrorRecord)

    try {
        $responseStream = $ErrorRecord.Exception.Response.GetResponseStream()
        if ($null -eq $responseStream) {
            return $null
        }

        $reader = New-Object System.IO.StreamReader($responseStream)
        $body = $reader.ReadToEnd()
        if (-not $body) {
            return $null
        }

        try {
            return $body | ConvertFrom-Json
        }
        catch {
            return $body
        }
    }
    catch {
        return $null
    }
}

function Show-SessionStatus {
    param(
        [long]$SessionId,
        [hashtable]$Headers
    )

    $status = Invoke-ApiJson -Method Get -Uri "$BaseUrl/api/admin/integrations/telegram-user/$SessionId/status" -Headers $Headers
    [pscustomobject]@{
        id = $status.id
        phoneNumber = $status.phoneNumber
        telegramUserId = $status.telegramUserId
        state = $status.state
        authorized = $status.authorized
        errorMessage = $status.errorMessage
        lastSyncAt = $status.lastSyncAt
    } | Format-List

    return $status
}

Write-Step "JWT login"
$token = Get-JwtToken
if (-not $token) {
    throw "JWT token was not returned"
}

$Headers = @{
    Authorization = "Bearer $token"
}

Write-Step "Validate admin access"
try {
    Invoke-ApiJson -Method Get -Uri "$BaseUrl/api/integrations" -Headers $Headers | Out-Null
}
catch {
    Write-Host "JWT was received, but authenticated API call failed." -ForegroundColor Red
    Write-Host "Make sure you are using a valid application user and that the token is accepted by the service."
    throw
}

if (-not $PhoneNumber) {
    $PhoneNumber = Read-Host "Enter Telegram phone number in international format"
}

Write-Step "Start Telegram user session"
$startResponse = $null
try {
    $startResponse = Invoke-ApiJson `
        -Method Post `
        -Uri "$BaseUrl/api/admin/integrations/telegram-user/start" `
        -Headers $Headers `
        -Body @{
            phoneNumber = $PhoneNumber
        }
}
catch {
    if ($_.Exception.Response.StatusCode.value__ -eq 409) {
        Write-Host "Active Telegram session already exists. Reusing current session." -ForegroundColor Yellow
        $startResponse = Invoke-ApiJson `
            -Method Get `
            -Uri "$BaseUrl/api/admin/integrations/telegram-user/current" `
            -Headers $Headers
    }
    else {
        $errorBody = Read-ErrorResponse -ErrorRecord $_
        if ($errorBody) {
            Write-Host "Start session error: $($errorBody | ConvertTo-Json -Compress)" -ForegroundColor Red
        }
        throw
    }
}

if (-not $startResponse) {
    throw "Telegram user session was not created and no active session was returned"
}

$sessionId = [long]$startResponse.id
Write-Host "Created session: $sessionId" -ForegroundColor Green
$status = Show-SessionStatus -SessionId $sessionId -Headers $Headers

if (-not $status.authorized -and $status.state -eq "WAIT_CODE") {
    $code = Read-Host "Enter Telegram code from SMS/app"
    if (-not $code) {
        throw "Telegram code is required to continue"
    }

    Write-Step "Submit Telegram code"
    Invoke-ApiJson `
        -Method Post `
        -Uri "$BaseUrl/api/admin/integrations/telegram-user/$sessionId/code" `
        -Headers $Headers `
        -Body @{
            code = $code
        } | Out-Null

    $status = Show-SessionStatus -SessionId $sessionId -Headers $Headers
}

if ($status.state -eq "WAIT_PASSWORD") {
    $securePassword = Read-Host "Enter Telegram 2FA password" -AsSecureString
    $plainPassword = [System.Net.NetworkCredential]::new("", $securePassword).Password

    Write-Step "Submit Telegram password"
    Invoke-ApiJson `
        -Method Post `
        -Uri "$BaseUrl/api/admin/integrations/telegram-user/$sessionId/password" `
        -Headers $Headers `
        -Body @{
            password = $plainPassword
        } | Out-Null

    $status = Show-SessionStatus -SessionId $sessionId -Headers $Headers
}

Write-Step "Wait for authorization"
$attempt = 0
do {
    Start-Sleep -Seconds 2
    $status = Invoke-ApiJson -Method Get -Uri "$BaseUrl/api/admin/integrations/telegram-user/$sessionId/status" -Headers $Headers
    Write-Host "State: $($status.state), Authorized: $($status.authorized)"
    $attempt++
} while ($attempt -lt 15 -and -not $status.authorized -and $status.state -ne "FAILED")

if (-not $status.authorized) {
    Write-Host "Session is not authorized yet." -ForegroundColor Yellow
    Write-Host "Status:"
    Show-SessionStatus -SessionId $sessionId -Headers $Headers | Out-Null
    exit 1
}

Write-Step "Read API smoke check"
$integrations = Invoke-ApiJson -Method Get -Uri "$BaseUrl/api/integrations" -Headers $Headers
$conversations = Invoke-ApiJson -Method Get -Uri "$BaseUrl/api/conversations?platform=TELEGRAM" -Headers $Headers
$users = Invoke-ApiJson -Method Get -Uri "$BaseUrl/api/users?platform=TELEGRAM" -Headers $Headers
$messages = Invoke-ApiJson -Method Get -Uri "$BaseUrl/api/messages?platform=TELEGRAM" -Headers $Headers

[pscustomobject]@{
    SessionId = $sessionId
    Authorized = $status.authorized
    SessionState = $status.state
    TelegramUserId = $status.telegramUserId
    IntegrationCount = @($integrations).Count
    ConversationCount = @($conversations).Count
    UserCount = @($users).Count
    MessageCount = @($messages).Count
} | Format-List

$conversationMap = @{}
foreach ($conversation in @($conversations)) {
    $conversationMap["$($conversation.id)"] = $conversation.title
}

$userMap = @{}
foreach ($user in @($users)) {
    $userMap["$($user.id)"] = if ($user.displayName) { $user.displayName } elseif ($user.username) { $user.username } else { $user.externalUserId }
}

$latestMessages = @($messages) | Select-Object -First 10
if ($latestMessages.Count -gt 0) {
    Write-Step "Latest Telegram messages"
    $latestMessages |
        ForEach-Object {
            [pscustomobject]@{
                sentAt = $_.sentAt
                chat = $conversationMap["$($_.conversationId)"]
                author = $userMap["$($_.authorId)"]
                text = $_.text
                messageType = $_.messageType
            }
        } |
        Format-Table -AutoSize
}

Write-Host ""
Write-Host "Telegram user-mode smoke test finished." -ForegroundColor Green
Write-Host "Now you can ask another participant to send a fresh message in a shared chat and rerun this script."
