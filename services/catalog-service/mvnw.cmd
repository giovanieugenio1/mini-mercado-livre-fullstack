@ECHO OFF
@REM Delega para o Maven Wrapper do monorepo raiz
@SET MONOREPO_ROOT=%~dp0..\..\
@CALL "%MONOREPO_ROOT%mvnw.cmd" %*
