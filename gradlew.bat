@echo off
set DIR=%~dp0
set GRADLE_WRAPPER_JAR=%DIR%gradle\wrapper\gradle-wrapper.jar
if not exist "%GRADLE_WRAPPER_JAR%" (
  echo gradle-wrapper.jar not found. Open this project in Android Studio to generate it automatically.
  exit /b 1
)
java -jar "%GRADLE_WRAPPER_JAR%" %*
