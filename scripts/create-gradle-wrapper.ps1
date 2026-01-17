Param(
    [string]$GradleVersion = "8.6"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Test-Command([string]$Name) {
    $null -ne (Get-Command $Name -ErrorAction SilentlyContinue)
}

if (-not (Test-Command "gradle")) {
    Write-Error "Gradle is not available on PATH. Install Gradle or run this from Android Studio."
}

Write-Host "Creating Gradle wrapper (version $GradleVersion)..." -ForegroundColor Cyan
gradle wrapper --gradle-version $GradleVersion --distribution-type bin

Write-Host "Gradle wrapper created. Files should include:" -ForegroundColor Green
Write-Host " - gradlew"
Write-Host " - gradlew.bat"
Write-Host " - gradle/wrapper/gradle-wrapper.properties"
Write-Host " - gradle/wrapper/gradle-wrapper.jar"
