$ErrorActionPreference = "Stop"

$BaseUrl = "http://localhost:8080"
$Username = "tester"
$Password = "tester123"
$VkGroupId = if ($env:VK_GROUP_ID) { [long]$env:VK_GROUP_ID } else { 0 }
$VkSecret = if ($env:VK_SECRET) { $env:VK_SECRET } else { "" }
$TelegramWebhookSecret = if ($env:TELEGRAM_WEBHOOK_SECRET) { $env:TELEGRAM_WEBHOOK_SECRET } else { "" }

function Get-JwtToken {
    try {
        $registerBody = @{
            username = $Username
            password = $Password
        } | ConvertTo-Json

        $registerResponse = Invoke-RestMethod `
            -Method Post `
            -Uri "$BaseUrl/auth/register" `
            -ContentType "application/json" `
            -Body $registerBody

        return $registerResponse.token
    }
    catch {
        try {
            $loginBody = @{
                username = $Username
                password = $Password
            } | ConvertTo-Json

            $loginResponse = Invoke-RestMethod `
                -Method Post `
                -Uri "$BaseUrl/auth/login" `
                -ContentType "application/json" `
                -Body $loginBody

            return $loginResponse.token
        }
        catch {
            Write-Host "Auth failed" -ForegroundColor Red
            throw
        }
    }
}

$token = Get-JwtToken
if (-not $token) {
    throw "JWT token was not returned"
}

$authHeaders = @{
    Authorization = "Bearer $token"
}

$telegramHeaders = @{}
if ($TelegramWebhookSecret) {
    $telegramHeaders["X-Telegram-Bot-Api-Secret-Token"] = $TelegramWebhookSecret
}

$telegramPayload = @{
    update_id = 100001
    message = @{
        message_id = 501
        date = 1775077200
        text = "Test telegram message"
        chat = @{
            id = -1001234567890
            title = "QA Telegram Group"
            type = "supergroup"
        }
        from = @{
            id = 777001
            is_bot = $false
            first_name = "Ivan"
            last_name = "Petrov"
            username = "ivan_petrov"
        }
    }
} | ConvertTo-Json -Depth 10

$vkPayload = @{
    type = "message_new"
    event_id = "vk-test-1001"
    group_id = $VkGroupId
    secret = $VkSecret
    object = @{
        group_name = "QA VK Group"
        message = @{
            id = 901
            date = 1775077200
            peer_id = 2000000001
            from_id = 555001
            text = "Test vk message"
            attachments = @()
        }
    }
} | ConvertTo-Json -Depth 10

Write-Host "Sending Telegram webhook..."
$telegramResponse = Invoke-RestMethod `
    -Method Post `
    -Uri "$BaseUrl/api/integrations/webhooks/telegram" `
    -ContentType "application/json" `
    -Headers $telegramHeaders `
    -Body $telegramPayload

Write-Host "Sending VK webhook..."
$vkResponse = Invoke-RestMethod `
    -Method Post `
    -Uri "$BaseUrl/api/integrations/webhooks/vk" `
    -ContentType "application/json" `
    -Body $vkPayload

Write-Host "Reading API..."
$messages = Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/messages" -Headers $authHeaders
$telegramMessages = Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/messages?platform=TELEGRAM" -Headers $authHeaders
$vkMessages = Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/messages?platform=VK" -Headers $authHeaders
$users = Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/users" -Headers $authHeaders
$conversations = Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/conversations" -Headers $authHeaders
$integrations = Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/integrations" -Headers $authHeaders

Write-Host ""
Write-Host "Smoke test result" -ForegroundColor Green
[pscustomobject]@{
    TelegramWebhookStatus = $telegramResponse.status
    VkWebhookStatus = $vkResponse
    TotalMessages = @($messages).Count
    TelegramMessages = @($telegramMessages).Count
    VkMessages = @($vkMessages).Count
    TotalUsers = @($users).Count
    TotalConversations = @($conversations).Count
    TotalIntegrations = @($integrations).Count
    LatestTelegramText = (@($telegramMessages) | Select-Object -Last 1).text
    LatestVkText = (@($vkMessages) | Select-Object -Last 1).text
} | Format-List
