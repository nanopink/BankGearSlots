param(
	[Parameter(ValueFromRemainingArguments = $true)]
	[string[]] $GradleArgs
)

$ErrorActionPreference = "Stop"

if (-not $GradleArgs -or $GradleArgs.Count -eq 0)
{
	$GradleArgs = @("run")
}

$jdkHome = "C:\Program Files\Eclipse Adoptium\jdk-21.0.3.9-hotspot"
$javaExe = Join-Path $jdkHome "bin\java.exe"

if (-not (Test-Path -LiteralPath $javaExe))
{
	throw "Expected JDK not found at $jdkHome. Install JDK 21 or update run-plugin.ps1."
}

$env:JAVA_HOME = $jdkHome
$env:Path = "$jdkHome\bin;$env:Path"

& "$PSScriptRoot\gradlew.bat" @GradleArgs
exit $LASTEXITCODE
