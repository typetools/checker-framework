@echo off

java -jar %~dp0\..\dist\checker.jar -processorpath %~dp0\..\dist\checker.jar @argfile %*

echo.
echo.
echo NOTE: javac_maven.bat is deprecated and will be removed in December 2015.
echo.
