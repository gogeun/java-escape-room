$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath (Join-Path $PSScriptRoot '../..')
New-Item -ItemType Directory -Force out/classes, src/room3/dist | Out-Null
$sources = @(Get-ChildItem -LiteralPath src -Filter *.java -Recurse | Where-Object { $_.FullName -notmatch '[\\/]room3[\\/](tests|dist)[\\/]' } | ForEach-Object { $_.FullName })
$ErrorActionPreference = 'Continue'
$javaSettings = & java -XshowSettings:properties -version 2>&1 | Out-String
$ErrorActionPreference = 'Stop'
$jdkRoot = [regex]::Match($javaSettings, 'java.home\s*=\s*([^\r\n]+)').Groups[1].Value.Trim()
$jarTool = Join-Path $jdkRoot 'bin/jar.exe'
if (-not (Test-Path -LiteralPath $jarTool)) { throw 'A full JDK 17+ with bin/jar.exe is required.' }
& javac -encoding UTF-8 --release 17 -d out/classes @sources
if ($LASTEXITCODE -ne 0) { throw 'Compilation failed' }
& $jarTool --create --file src/room3/dist/BloodmoonChapel.jar --main-class room3.Main -C out/classes room3 -C resources room3
if ($LASTEXITCODE -ne 0) { throw 'JAR packaging failed' }
Write-Host 'Built src/room3/dist/BloodmoonChapel.jar'
