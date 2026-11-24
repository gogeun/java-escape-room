$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath (Join-Path $PSScriptRoot '../..')
& "$PSScriptRoot/build.ps1"
$ErrorActionPreference = 'Continue'
$javaSettings = & java -XshowSettings:properties -version 2>&1 | Out-String
$ErrorActionPreference = 'Stop'
$jdkRoot = [regex]::Match($javaSettings, 'java.home\s*=\s*([^\r\n]+)').Groups[1].Value.Trim()
$packager = Join-Path $jdkRoot 'bin/jpackage.exe'
if (-not (Test-Path -LiteralPath $packager)) { throw 'JDK 17+ jpackage.exe is required.' }
if (Test-Path -LiteralPath src/room3/dist/windows/BloodmoonChapel) { throw 'src/room3/dist/windows/BloodmoonChapel already exists. Move it before packaging again.' }
New-Item -ItemType Directory -Force out/package-input | Out-Null
Copy-Item -LiteralPath src/room3/dist/BloodmoonChapel.jar -Destination out/package-input/BloodmoonChapel.jar
& $packager --type app-image --input out/package-input --dest src/room3/dist/windows --name BloodmoonChapel --main-jar BloodmoonChapel.jar --main-class room3.Main --app-version 1.0.0 --java-options '-Dfile.encoding=UTF-8'
if ($LASTEXITCODE -ne 0) { throw 'Windows packaging failed' }
Write-Host 'Run src/room3/dist/windows/BloodmoonChapel/BloodmoonChapel.exe; distribute the entire BloodmoonChapel directory.'
