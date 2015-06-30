@echo off

java -jar %~dp0\..\dist\checker.jar -processorpath %~dp0\..\dist\checker.jar @argfile %*
