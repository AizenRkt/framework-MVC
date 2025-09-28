@echo off
setlocal enabledelayedexpansion

:: ============================================================================
:: Script pour compiler et déployer le projet Framework et Test-Project
:: SANS MAVEN. (Version finale corrigée 3)
:: ============================================================================
C:\xampp\tomcat\webapps
:: === Variables à adapter ===
set "FRAMEWORK_DIR=%~dp0"
set "TEST_PROJECT_DIR=%FRAMEWORK_DIR%test-project"
set "PROJECT_NAME=test-project-url"
set "TOMCAT_HOME=C:\xampp\tomcat"
set "JAVA_HOME=C:\Program Files\Java\jdk-21"

:: === Variables calculées (ne pas modifier) ===
set "PATH=%JAVA_HOME%\bin;%PATH%"
set "BUILD_DIR=%FRAMEWORK_DIR%build"
set "FRAMEWORK_CLASSES_DIR=%BUILD_DIR%\framework-classes"
set "WEB_INF_LIB_DIR=%TEST_PROJECT_DIR%\src\main\webapp\WEB-INF\lib"
set "FRAMEWORK_JAR=%WEB_INF_LIB_DIR%\framework.jar"
set "TEST_CLASSES_DIR=%TEST_PROJECT_DIR%\src\main\webapp\WEB-INF\classes"
set "WAR_FILE_NAME=%PROJECT_NAME%.war"
set "DEPLOY_DIR=%TOMCAT_HOME%\webapps"

:: Construction manuelle et explicite du Classpath
set "FULL_CLASSPATH="
for %%j in ("%FRAMEWORK_DIR%lib\*.jar") do (
    set "FULL_CLASSPATH=!FULL_CLASSPATH!;%%j"
)
for %%j in ("%TOMCAT_HOME%\lib\*.jar") do (
    set "FULL_CLASSPATH=!FULL_CLASSPATH!;%%j"
)

:: ============================================================================
:: 1. Nettoyage
:: ============================================================================
echo Nettoyage des anciens builds...
if exist "%BUILD_DIR%" ( rd /s /q "%BUILD_DIR%" )
if exist "%WEB_INF_LIB_DIR%" ( rd /s /q "%WEB_INF_LIB_DIR%" )
if exist "%TEST_CLASSES_DIR%" ( rd /s /q "%TEST_CLASSES_DIR%" )
if exist "%DEPLOY_DIR%\%WAR_FILE_NAME%" ( del /f /q "%DEPLOY_DIR%\%WAR_FILE_NAME%" )
if exist "%DEPLOY_DIR%\%PROJECT_NAME%" ( rd /s /q "%DEPLOY_DIR%\%PROJECT_NAME%" )

mkdir "%BUILD_DIR%"
mkdir "%FRAMEWORK_CLASSES_DIR%"
mkdir "%TEST_CLASSES_DIR%"
mkdir "%WEB_INF_LIB_DIR%"
if not exist "%DEPLOY_DIR%" ( mkdir "%DEPLOY_DIR%" )

echo Nettoyage termine.

:: ============================================================================
:: 2. Compilation du Framework
:: ============================================================================
echo Compilation du Framework...
set "FRAMEWORK_SOURCES_FILE=%BUILD_DIR%\framework_sources.txt"
dir /s /b "%FRAMEWORK_DIR%src\main\java\*.java" > "%FRAMEWORK_SOURCES_FILE%"
javac -d "%FRAMEWORK_CLASSES_DIR%" -cp "!FULL_CLASSPATH!" @"%FRAMEWORK_SOURCES_FILE%"
if %errorlevel% neq 0 (
    echo Erreur lors de la compilation du framework.
    pause
    exit /b 1
)
echo Framework compile.

:: ============================================================================
:: 3. Création du JAR du Framework
:: ============================================================================
echo Creation du fichier framework.jar...
jar -c -f "%FRAMEWORK_JAR%" -C "%FRAMEWORK_CLASSES_DIR%" .
if %errorlevel% neq 0 (
    echo Erreur lors de la creation du JAR.
    pause
    exit /b 1
)
echo framework.jar cree dans WEB-INF/lib.

:: ============================================================================
:: 4. Compilation du Projet de Test
:: ============================================================================
echo Compilation du projet de test...
set "TEST_SOURCES_FILE=%BUILD_DIR%\test_sources.txt"
dir /s /b "%TEST_PROJECT_DIR%\src\main\java\*.java" > "%TEST_SOURCES_FILE%"
for %%A in ("%TEST_SOURCES_FILE%") do set "FileSize=%%~zA"
if !FileSize! gtr 0 (
    javac -d "%TEST_CLASSES_DIR%" -cp "%FRAMEWORK_JAR%;!FULL_CLASSPATH!" @"%TEST_SOURCES_FILE%"
    if !errorlevel! neq 0 (
        echo Erreur lors de la compilation du projet de test.
        pause
        exit /b 1
    )
)
echo Projet de test compile.

:: ============================================================================
:: 5. Création du WAR
:: ============================================================================
echo Creation du fichier %WAR_FILE_NAME%...
cd "%TEST_PROJECT_DIR%\src\main\webapp"
jar -c -f "%DEPLOY_DIR%\%WAR_FILE_NAME%" .
cd "%FRAMEWORK_DIR%"
if %errorlevel% neq 0 (
    echo Erreur lors de la creation du WAR.
    pause
    exit /b 1
)
echo Fichier WAR cree et copie dans Tomcat/webapps.

:: ============================================================================
:: 6. Redémarrage de Tomcat
:: ============================================================================
echo Redemarrage de Tomcat...
call "%TOMCAT_HOME%\bin\shutdown.bat"
echo Attente de l'arret de Tomcat...
timeout /t 5 >nul
call "%TOMCAT_HOME%\bin\startup.bat"

echo Tomcat redemarre.
echo Deploiement termine. Accedez a : http://localhost:8080/%PROJECT_NAME%/hello

endlocal
pause
