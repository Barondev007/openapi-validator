@echo off
REM BNPPF OpenAPI Validator CLI - Windows Launcher
REM
REM This script launches the BNPPF OpenAPI Validator command-line tool.
REM
REM Usage: bnppf-validator.bat <command> [options]
REM
REM Commands:
REM   validate-spec      Validate an OpenAPI specification
REM   validate-request   Validate an HTTP request
REM   validate-response  Validate an HTTP response
REM
REM Examples:
REM   bnppf-validator.bat validate-spec --spec openapi.yaml
REM   bnppf-validator.bat validate-request --spec openapi.yaml --method GET --path /pets
REM   bnppf-validator.bat --help

setlocal enabledelayedexpansion

REM Determine the directory where this script is located
set "SCRIPT_DIR=%~dp0"

REM Remove trailing backslash
if "%SCRIPT_DIR:~-1%"=="\" set "SCRIPT_DIR=%SCRIPT_DIR:~0,-1%"

REM Set the lib directory
set "LIB_DIR=%SCRIPT_DIR%\lib"

REM Check if lib directory exists
if not exist "%LIB_DIR%" (
    echo Error: Library directory not found: %LIB_DIR%
    echo Please ensure the distribution was extracted correctly.
    exit /b 1
)

REM Build the classpath from all JARs in the lib directory
set "CLASSPATH="
for %%f in ("%LIB_DIR%\*.jar") do (
    if "!CLASSPATH!"=="" (
        set "CLASSPATH=%%f"
    ) else (
        set "CLASSPATH=!CLASSPATH!;%%f"
    )
)

REM Check if any JARs were found
if "%CLASSPATH%"=="" (
    echo Error: No JAR files found in %LIB_DIR%
    exit /b 1
)

REM Set Java options (can be overridden by JAVA_OPTS environment variable)
if "%JAVA_OPTS%"=="" (
    set "JAVA_OPTS=-Xmx256m"
)

REM Find Java executable
if defined JAVA_HOME (
    set "JAVA_CMD=%JAVA_HOME%\bin\java.exe"
) else (
    set "JAVA_CMD=java"
)

REM Check if Java is available
"%JAVA_CMD%" -version >nul 2>&1
if errorlevel 1 (
    echo Error: Java not found. Please install Java 11 or higher.
    echo You can set JAVA_HOME to point to your Java installation.
    exit /b 1
)

REM Run the CLI
"%JAVA_CMD%" %JAVA_OPTS% -cp "%CLASSPATH%" be.bnppf.openvalidator.cli.OpenAPIValidatorCLI %*

exit /b %ERRORLEVEL%
