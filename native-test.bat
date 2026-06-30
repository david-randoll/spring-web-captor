@echo off
setlocal
REM Local GraalVM nativeTest runner for spring-web-captor. Pass the module via -pl, e.g.:
REM   native-test.bat -pl spring-web-captor
REM   native-test.bat -pl spring-web-captor-xml
REM   native-test.bat -pl spring-web-captor-storage
REM Run modules ONE AT A TIME (each native-image build uses ~6-9GB RAM).
REM
REM No machine-specific paths are hardcoded. Requirements:
REM   - GRAALVM_HOME : a GraalVM 21 JDK (must contain bin\native-image). If unset, JAVA_HOME is used
REM                    only when it is itself a GraalVM.
REM   - Visual Studio / Build Tools with the C++ workload (located automatically via vswhere).
REM   - Optional NATIVE_M2 : local Maven repo for native builds (defaults to a path without spaces,
REM                          since spaces in the repo path break native-image).

cd /d "%~dp0"

REM --- Resolve GraalVM (prefer GRAALVM_HOME; else JAVA_HOME if it has native-image) ---
set "GVM=%GRAALVM_HOME%"
if not defined GVM if exist "%JAVA_HOME%\bin\native-image.cmd" set "GVM=%JAVA_HOME%"
if not defined GVM (
  echo ERROR: Set GRAALVM_HOME to a GraalVM 21 JDK ^(must contain bin\native-image^).
  exit /b 1
)
if not exist "%GVM%\bin\native-image.cmd" (
  echo ERROR: "%GVM%" is not a GraalVM with native-image ^(bin\native-image.cmd missing^).
  exit /b 1
)
set "JAVA_HOME=%GVM%"
set "PATH=%JAVA_HOME%\bin;%PATH%"

REM --- Load MSVC x64 build env (native-image needs cl.exe). Locate VS via vswhere (any edition/year). ---
set "VSWHERE=%ProgramFiles(x86)%\Microsoft Visual Studio\Installer\vswhere.exe"
if not exist "%VSWHERE%" (
  echo ERROR: vswhere not found. Install Visual Studio / Build Tools with the C++ workload.
  exit /b 1
)
set "VSINSTALL="
for /f "usebackq tokens=*" %%i in (`"%VSWHERE%" -latest -products * -requires Microsoft.VisualStudio.Component.VC.Tools.x86.x64 -property installationPath`) do set "VSINSTALL=%%i"
if not defined VSINSTALL (
  echo ERROR: No Visual Studio with the C++ ^(VC.Tools.x86.x64^) component was found.
  exit /b 1
)
call "%VSINSTALL%\VC\Auxiliary\Build\vcvars64.bat" >nul

REM --- Native-friendly local repo (spaces in the path break native-image). Override via NATIVE_M2. ---
if not defined NATIVE_M2 set "NATIVE_M2=%HOMEDRIVE%\mvnrepo"

REM -Dgroups=native: native-compile + run only the @Tag("native") boot subset.
REM -Djacoco.check.phase=none: the single-context native run scopes coverage low; the JVM build owns the 80% gate.
call "%~dp0mvnw.cmd" ^
  -ntp -PnativeTest -Dgroups=native -Djacoco.check.phase=none ^
  -Dmaven.repo.local="%NATIVE_M2%" ^
  %* clean test
