param([string]$Jdk = 'C:\Program Files\Java\jdk-21.0.11')
$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$output = Join-Path $root '../src/main/resources/assets/raven/opai/native/windows-x86_64/raven-nanovg.dll'
if (!(Test-Path (Join-Path $Jdk 'include/jni.h'))) { throw 'Pass -Jdk with a JDK containing JNI headers.' }
& gcc -std=c99 -O2 -shared -static-libgcc -DGLEW_STATIC -I "$Jdk/include" -I "$Jdk/include/win32" -I "$root/vendor/glew/include" -I "$root/vendor/nanovg" "$root/raven_nanovg.c" "$root/vendor/nanovg/nanovg.c" "$root/vendor/glew/src/glew.c" -o $output -lopengl32 -lgdi32 -luser32 -lm
if ($LASTEXITCODE -ne 0) { throw "Native build failed: $LASTEXITCODE" }
Get-FileHash -LiteralPath $output -Algorithm SHA256
