@rem Gradle start up script for Windows
@echo off
set APP_HOME=%~dp0
set DEFAULT_JVM_OPTS=-Dfile.encoding=UTF-8 -Xmx64m -Xms64m
set JAVA_HOME=C:\Program Files\Java\jdk-17

if defined JAVA_HOME goto findJavaFromJavaHome

set JAVACMD=java
goto execute

:findJavaFromJavaHome
set JAVACMD=%JAVA_HOME%\bin\java.exe

:execute
"%JAVACMD%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %GRADLE_OPTS% "-Dorg.gradle.appname=%~nx0" -classpath "%APP_HOME%gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain %*

:end
@endlocal