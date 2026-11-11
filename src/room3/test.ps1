$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath (Join-Path $PSScriptRoot '../..')
& "$PSScriptRoot/build.ps1"
& javac -encoding UTF-8 --release 17 -cp src/room3/dist/BloodmoonChapel.jar -d out/test src/room3/tests/room3/GameTest.java src/room3/tests/room3/SoundTest.java
if ($LASTEXITCODE -ne 0) { throw 'Test compilation failed' }
& java '-Djava.awt.headless=true' -cp 'src/room3/dist/BloodmoonChapel.jar;out/test' room3.GameTest
if ($LASTEXITCODE -ne 0) { throw 'Tests failed' }
& java '-Djava.awt.headless=true' -cp 'src/room3/dist/BloodmoonChapel.jar;out/test' room3.SoundTest
if ($LASTEXITCODE -ne 0) { throw 'Sound tests failed' }
