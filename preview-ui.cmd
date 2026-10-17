@echo off
setlocal
cd /d "%~dp0"
call mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev,ui-preview" "-Dspring-boot.run.jvmArguments=-Dspring.devtools.restart.enabled=false" "-Dspring-boot.run.arguments=--debug=false"
endlocal
