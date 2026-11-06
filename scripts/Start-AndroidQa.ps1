param([switch]$ShowWindow)
$ErrorActionPreference = 'Stop'
$sdkRoot = Join-Path $env:LOCALAPPDATA 'Android\Sdk'
$adb = Join-Path $sdkRoot 'platform-tools\adb.exe'
$emulator = Join-Path $sdkRoot 'emulator\emulator.exe'
$avdName = 'SoloStudying_QA_API36'
$serial = 'emulator-5580'
if (-not (Test-Path -LiteralPath $emulator)) { throw 'Android Emulator is not installed.' }
if (@(& $emulator -list-avds) -notcontains $avdName) { throw "Create $avdName first; see docs/ANDROID_EMULATOR_QA.md." }
$devices = & $adb devices
if ($devices -match "^$serial\s") {
    Write-Output "$serial is already running. Close it first if you want to change window mode."
    exit 0
}
$reportRoot = Join-Path $PSScriptRoot '..\app\build\reports\emulator-qa'
New-Item -ItemType Directory -Path $reportRoot -Force | Out-Null
$emulatorArgs = @('-avd', $avdName, '-port', '5580', '-memory', '4096', '-cores', '4', '-gpu', 'software', '-no-boot-anim', '-no-snapshot')
if (-not $ShowWindow) { $emulatorArgs += '-no-window' }
$launchOptions = @{
    FilePath = $emulator
    ArgumentList = $emulatorArgs
    RedirectStandardOutput = Join-Path $reportRoot 'emulator-stdout.log'
    RedirectStandardError = Join-Path $reportRoot 'emulator-stderr.log'
    PassThru = $true
}
if (-not $ShowWindow) { $launchOptions.WindowStyle = 'Hidden' }
$process = Start-Process @launchOptions
Write-Output "Started $avdName ($serial), PID $($process.Id). Wait for Android to finish booting."
