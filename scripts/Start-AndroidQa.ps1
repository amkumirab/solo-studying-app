param([switch]$ShowWindow, [ValidateSet(36, 37)][int]$ApiLevel = 36)
$ErrorActionPreference = 'Stop'
$sdkRoot = Join-Path $env:LOCALAPPDATA 'Android\Sdk'
$adb = Join-Path $sdkRoot 'platform-tools\adb.exe'
$emulator = Join-Path $sdkRoot 'emulator\emulator.exe'
$avdName = "SoloStudying_QA_API$ApiLevel"
$port = if ($ApiLevel -eq 36) { 5580 } else { 5582 }
$serial = "emulator-$port"
if (-not (Test-Path -LiteralPath $emulator)) { throw 'Android Emulator is not installed.' }
if (@(& $emulator -list-avds) -notcontains $avdName) { throw "Create $avdName first; see docs/ANDROID_EMULATOR_QA.md." }
$devices = & $adb devices
if ($devices -match "^$serial\s") {
    $runningAvd = @(& $adb -s $serial emu avd name)
    if ($LASTEXITCODE -ne 0 -or $runningAvd -notcontains $avdName) {
        throw "$serial does not belong to $avdName; no emulator was changed."
    }
    Write-Output "$serial is already running. Close it first if you want to change window mode."
    exit 0
}
$reportRoot = Join-Path $PSScriptRoot '..\app\build\reports\emulator-qa'
New-Item -ItemType Directory -Path $reportRoot -Force | Out-Null
$emulatorArgs = @('-avd', $avdName, '-port', "$port", '-memory', '4096', '-cores', '4', '-gpu', 'software', '-no-boot-anim', '-no-snapshot')
if (-not $ShowWindow) { $emulatorArgs += '-no-window' }
$launchOptions = @{
    FilePath = $emulator
    ArgumentList = $emulatorArgs
    RedirectStandardOutput = Join-Path $reportRoot "emulator-$ApiLevel-stdout.log"
    RedirectStandardError = Join-Path $reportRoot "emulator-$ApiLevel-stderr.log"
    PassThru = $true
}
if (-not $ShowWindow) { $launchOptions.WindowStyle = 'Hidden' }
$process = Start-Process @launchOptions
Write-Output "Started $avdName ($serial), PID $($process.Id). Wait for Android to finish booting."
