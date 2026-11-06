param([ValidateSet(36, 37)][int]$ApiLevel = 36)
$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path $PSScriptRoot -Parent
$adb = Join-Path $env:LOCALAPPDATA 'Android\Sdk\platform-tools\adb.exe'
$serial = if ($ApiLevel -eq 36) { 'emulator-5580' } else { 'emulator-5582' }
$avdName = @(& $adb -s $serial emu avd name)
if ($LASTEXITCODE -ne 0 -or $avdName -notcontains "SoloStudying_QA_API$ApiLevel") {
    throw "Only the dedicated SoloStudying_QA_API$ApiLevel emulator is allowed."
}
$boot = & $adb -s $serial shell getprop sys.boot_completed
if ($boot.Trim() -ne '1') { throw 'The emulator has not finished booting.' }
foreach ($apk in @('app\build\outputs\apk\debug\app-debug.apk', 'app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk')) {
    $apkPath = Join-Path $repoRoot $apk
    if (-not (Test-Path -LiteralPath $apkPath)) { throw "Missing $apk. Build assembleDebug assembleDebugAndroidTest first." }
    & $adb -s $serial install -r $apkPath
    if ($LASTEXITCODE -ne 0) { throw "Installation failed: $apk" }
}
$reportRoot = Join-Path $repoRoot 'app\build\reports\emulator-qa'
New-Item -ItemType Directory -Path $reportRoot -Force | Out-Null
$result = @(& $adb -s $serial shell am instrument -w -r 'com.amkumirab.solostudying.test/androidx.test.runner.AndroidJUnitRunner')
$result | Tee-Object -FilePath (Join-Path $reportRoot "instrumentation-api$ApiLevel.txt")
if ($LASTEXITCODE -ne 0 -or ($result -join "`n") -notmatch 'OK \([1-9]\d* tests?\)') {
    throw "Instrumentation did not report a successful complete run. Inspect instrumentation-api$ApiLevel.txt."
}
