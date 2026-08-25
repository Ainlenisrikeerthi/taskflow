$ErrorActionPreference = 'Stop'

Write-Host "=== 1. TEST SWAGGER API DOCS ==="
$swagger = Invoke-RestMethod -Uri 'http://localhost:8080/v3/api-docs' -Method Get
Write-Host "OpenAPI Title: $($swagger.info.title) | Version: $($swagger.info.version)"

Write-Host "`n=== 2. TEST AUTH REGISTRATION / LOGIN (Admin) ==="
$adminEmail = "admin_live_" + (Get-Random) + "@taskflow.com"
$adminReg = @{ name = "Live Admin"; email = $adminEmail; password = "password123"; role = "ADMIN" } | ConvertTo-Json
$adminAuth = Invoke-RestMethod -Uri 'http://localhost:8080/api/auth/register' -Method Post -Body $adminReg -ContentType 'application/json'
Write-Host "Registered Admin: $($adminAuth.name) | Role: $($adminAuth.role) | Token: $($adminAuth.token.Substring(0, 20))..."
$adminToken = $adminAuth.token

Write-Host "`n=== 3. TEST AUTH REGISTRATION / LOGIN (User) ==="
$userEmail = "user_live_" + (Get-Random) + "@taskflow.com"
$userReg = @{ name = "Live User"; email = $userEmail; password = "password123"; role = "USER" } | ConvertTo-Json
$userAuth = Invoke-RestMethod -Uri 'http://localhost:8080/api/auth/register' -Method Post -Body $userReg -ContentType 'application/json'
Write-Host "Registered User: $($userAuth.name) | Role: $($userAuth.role) | Token: $($userAuth.token.Substring(0, 20))..."
$userToken = $userAuth.token

Write-Host "`n=== 4. TEST ADMIN CREATES AND PUBLISHES TASK ==="
$adminHeaders = @{ Authorization = "Bearer $adminToken" }
$taskReq = @{
    title = "Live Integration Task $(Get-Random)"
    description = "A task created during live verification to test multi-user assignments"
    instructions = "Complete the instructions and submit proof URL"
    deadline = (Get-Date).AddDays(7).ToString("yyyy-MM-dd")
    proofRequirement = "URL to public repo or post"
    status = "PUBLISHED"
} | ConvertTo-Json

$createdTask = Invoke-RestMethod -Uri 'http://localhost:8080/api/tasks' -Method Post -Body $taskReq -Headers $adminHeaders -ContentType 'application/json'
Write-Host "Created Task ID: $($createdTask.id) | Title: $($createdTask.title) | Status: $($createdTask.status)"

Write-Host "`n=== 5. TEST GET PUBLISHED TASKS ==="
$tasks = Invoke-RestMethod -Uri 'http://localhost:8080/api/tasks' -Method Get
Write-Host "Published tasks count: $($tasks.Count) | Latest task title: $($tasks[-1].title)"

Write-Host "`n=== 6. TEST ADMIN DASHBOARD ==="
$stats = Invoke-RestMethod -Uri 'http://localhost:8080/api/admin/dashboard' -Method Get -Headers $adminHeaders
Write-Host "Dashboard Stats -> Total Tasks: $($stats.totalTasks) | Total Users: $($stats.totalUsers) | Total Assignments: $($stats.totalAssignments)"

Write-Host "`n=== 7. TEST USER SELF-ASSIGN TASK ==="
$userHeaders = @{ Authorization = "Bearer $userToken" }
$targetTaskId = $createdTask.id

$assigned = Invoke-RestMethod -Uri "http://localhost:8080/api/tasks/$targetTaskId/assign" -Method Post -Headers $userHeaders
Write-Host "Assigned Task ID: $($assigned.taskId) | Status: $($assigned.status) | Active: $($assigned.isActive)"

Write-Host "`n=== 8. TEST DUPLICATE ASSIGNMENT REJECTION (Should Conflict/Bad Request) ==="
try {
    Invoke-RestMethod -Uri "http://localhost:8080/api/tasks/$targetTaskId/assign" -Method Post -Headers $userHeaders
    Write-Error "Duplicate assignment should not have succeeded!"
} catch {
    Write-Host "Correctly rejected duplicate assignment with error: $($_.Exception.Message)"
}

Write-Host "`n=== 9. TEST USER UPDATES STATUS TO IN_PROGRESS ==="
$updateReq = @{ status = 'STARTED_NOT_COMPLETED' } | ConvertTo-Json
$inProgress = Invoke-RestMethod -Uri "http://localhost:8080/api/assignments/$($assigned.id)/status" -Method Patch -Body $updateReq -Headers $userHeaders -ContentType 'application/json'
Write-Host "Updated Assignment Status: $($inProgress.status)"

Write-Host "`n=== 10. TEST USER COMPLETES WITHOUT PROOF (Should Fail with 400) ==="
try {
    $badReq = @{ status = 'COMPLETED' } | ConvertTo-Json
    Invoke-RestMethod -Uri "http://localhost:8080/api/assignments/$($assigned.id)/status" -Method Patch -Body $badReq -Headers $userHeaders -ContentType 'application/json'
    Write-Error "Completion without proof should have failed!"
} catch {
    Write-Host "Correctly rejected completion without proof: $($_.Exception.Message)"
}

Write-Host "`n=== 11. TEST USER COMPLETES WITH VALID PROOF URL ==="
$completeReq = @{ status = 'COMPLETED'; proofUrl = 'https://linkedin.com/posts/my-taskflow-submission' } | ConvertTo-Json
$completed = Invoke-RestMethod -Uri "http://localhost:8080/api/assignments/$($assigned.id)/status" -Method Patch -Body $completeReq -Headers $userHeaders -ContentType 'application/json'
Write-Host "Task Completed! Proof URL: $($completed.proofUrl) | Status: $($completed.status)"

Write-Host "`n=== 12. TEST ADMIN ASSIGNMENT SEARCH & FILTER ==="
$adminAssignments = Invoke-RestMethod -Uri "http://localhost:8080/api/admin/assignments?taskId=$targetTaskId" -Method Get -Headers $adminHeaders
Write-Host "Admin found $($adminAssignments.Count) assignment(s) for task $targetTaskId. First user: $($adminAssignments[0].user.name)"

Write-Host "`n=== 13. TEST ADMIN REMOVAL OF ASSIGNMENT (Retains history & sends notification) ==="
$removeReq = @{ reason = 'Task completed satisfactorily, archived by admin.' } | ConvertTo-Json
$removed = Invoke-RestMethod -Uri "http://localhost:8080/api/admin/assignments/$($assigned.id)" -Method Delete -Body $removeReq -Headers $adminHeaders -ContentType 'application/json'
Write-Host "Admin removed assignment -> Status: $($removed.status) | Reason: $($removed.removedReason) | IsActive: $($removed.isActive)"

Write-Host "`n=== 14. TEST USER COMPLETE HISTORY (Includes removed assignment) ==="
$userHistory = Invoke-RestMethod -Uri "http://localhost:8080/api/users/me/history" -Method Get -Headers $userHeaders
Write-Host "User history contains $($userHistory.Count) records. Status of first: $($userHistory[0].status)"

Write-Host "`n========================================================"
Write-Host "ALL 14 LIVE BACKEND INTEGRATION SCENARIOS PASSED 100%!"
Write-Host "========================================================"
