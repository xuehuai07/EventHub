param(
    [Parameter(Mandatory = $true)]
    [string]$AccessToken,
    [Parameter(Mandatory = $true)]
    [long]$SessionId,
    [Parameter(Mandatory = $true)]
    [long]$TicketTypeId,
    [Parameter(Mandatory = $true)]
    [long]$SessionSeatId,
    [string]$BaseUrl = "http://localhost:8080",
    [int]$Requests = 100
)

$results = 1..$Requests | ForEach-Object -Parallel {
    $headers = @{ Authorization = "Bearer $using:AccessToken" }
    $body = @{
        sessionId = $using:SessionId
        ticketTypeId = $using:TicketTypeId
        sessionSeatIds = @($using:SessionSeatId)
        quantity = 1
    } | ConvertTo-Json

    try {
        $response = Invoke-RestMethod `
            -Method Post `
            -Uri "$using:BaseUrl/api/seat-locks" `
            -Headers $headers `
            -ContentType "application/json" `
            -Body $body
        [pscustomobject]@{
            Success = $true
            Code = $response.code
            LockNo = $response.data.lockNo
        }
    }
    catch {
        $code = try {
            ($_.ErrorDetails.Message | ConvertFrom-Json).code
        }
        catch {
            "HTTP_ERROR"
        }
        [pscustomobject]@{ Success = $false; Code = $code; LockNo = $null }
    }
} -ThrottleLimit 20

$successCount = @($results | Where-Object Success).Count
$results | Group-Object Code | Sort-Object Count -Descending | Format-Table Count, Name

if ($successCount -ne 1) {
    throw "并发锁座验收失败：期望成功 1 次，实际成功 $successCount 次。"
}

$successfulLock = $results | Where-Object Success | Select-Object -First 1
Invoke-RestMethod `
    -Method Delete `
    -Uri "$BaseUrl/api/seat-locks/$($successfulLock.LockNo)" `
    -Headers @{ Authorization = "Bearer $AccessToken" } | Out-Null

Write-Host "并发锁座验收通过：$Requests 个请求仅 1 个成功。" -ForegroundColor Green
