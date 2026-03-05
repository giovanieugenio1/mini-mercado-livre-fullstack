@REM ----------------------------------------------------------------------------
@REM Maven Wrapper startup batch script
@REM ----------------------------------------------------------------------------
@IF "%OS%"=="Windows_NT" @SETLOCAL
@IF "%OS%"=="Windows_NT" @SETLOCAL EnableDelayedExpansion

@SET MAVEN_WRAPPER_DIR=%~dp0.mvn\wrapper
@SET MAVEN_WRAPPER_PROPERTIES=%MAVEN_WRAPPER_DIR%\maven-wrapper.properties
@SET MVNW_VERBOSE=false

@IF NOT EXIST "%MAVEN_WRAPPER_DIR%" MKDIR "%MAVEN_WRAPPER_DIR%"

@REM Read distributionUrl from properties
@FOR /F "tokens=1,* delims==" %%A IN (%MAVEN_WRAPPER_PROPERTIES%) DO (
    @IF "%%A"=="distributionUrl" @SET DISTRIBUTION_URL=%%B
)

@REM Derive Maven home from distributionUrl filename
@FOR %%F IN ("%DISTRIBUTION_URL%") DO @SET MAVEN_ZIP=%%~nxF
@SET MAVEN_HOME_NAME=%MAVEN_ZIP:-bin.zip=%
@SET MAVEN_USER_HOME=%USERPROFILE%\.m2\wrapper\dists\%MAVEN_HOME_NAME%

@IF EXIST "%MAVEN_USER_HOME%\bin\mvn.cmd" GOTO RunMaven
@IF EXIST "%MAVEN_USER_HOME%\bin\mvn" GOTO RunMaven

@ECHO [mvnw] Maven nao encontrado em %MAVEN_USER_HOME%
@ECHO [mvnw] Baixando %DISTRIBUTION_URL% ...

@IF NOT EXIST "%MAVEN_USER_HOME%" MKDIR "%MAVEN_USER_HOME%"
@SET DOWNLOAD_ZIP=%MAVEN_USER_HOME%\%MAVEN_ZIP%

@powershell -Command "Invoke-WebRequest -Uri '%DISTRIBUTION_URL%' -OutFile '%DOWNLOAD_ZIP%'" ^
    || @powershell -Command "(New-Object Net.WebClient).DownloadFile('%DISTRIBUTION_URL%','%DOWNLOAD_ZIP%')"

@ECHO [mvnw] Extraindo ...
@powershell -Command "Expand-Archive -Path '%DOWNLOAD_ZIP%' -DestinationPath '%MAVEN_USER_HOME%' -Force"
@DEL "%DOWNLOAD_ZIP%"

@REM Mover para remover subdiretório extra da extração
@FOR /D %%D IN ("%MAVEN_USER_HOME%\apache-maven-*") DO @SET EXTRACTED=%%D
@IF NOT "%EXTRACTED%"=="" (
    @XCOPY "%EXTRACTED%\*" "%MAVEN_USER_HOME%\" /E /I /Q >NUL 2>&1
    @RMDIR /S /Q "%EXTRACTED%" >NUL 2>&1
)

:RunMaven
@IF EXIST "%MAVEN_USER_HOME%\bin\mvn.cmd" (
    @"%MAVEN_USER_HOME%\bin\mvn.cmd" %*
) ELSE (
    @"%MAVEN_USER_HOME%\bin\mvn" %*
)

@IF "%OS%"=="Windows_NT" @ENDLOCAL
