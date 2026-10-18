@echo off
setlocal enabledelayedexpansion
chcp 65001 >nul
echo ============================================================
echo   BackroomsComplex 模组 - 一键打包脚本  (backrooms_complex)
echo ============================================================
echo.

rem ---------- 1. 定位 Java ----------
set "JAVA_CMD=java"
if defined JAVA_HOME (
    if exist "%JAVA_HOME%\bin\java.exe" (
        set "JAVA_CMD=%JAVA_HOME%\bin\java.exe"
    )
)

"%JAVA_CMD%" -version >nul 2>&1
if errorlevel 1 (
    echo [错误] 未找到 Java。请先安装 JDK 21 并设置 JAVA_HOME。
    echo        下载地址: https://adoptium.net/temurin/releases/?version=21
    echo.
    pause
    exit /b 1
)

rem 检查 Java 主版本（Gradle 运行需要 17+，编译 1.21.1 需要 21 工具链，Gradle 会自动下载）
for /f "tokens=3" %%v in ('"%JAVA_CMD%" -version 2^>^&1 ^| findstr /i "version"') do set "JVER=%%v"
echo [信息] 当前 Java 版本: %JVER%
echo.

rem ---------- 2. 生成 gradle wrapper（首次） ----------
if not exist "gradlew.bat" (
    echo [信息] 未找到 gradlew，正在生成 wrapper ...
    where gradle >nul 2>&1
    if errorlevel 1 (
        echo [错误] 本机未安装 Gradle，无法生成 wrapper。
        echo        两种解决办法任选其一：
        echo          (A) 安装 Gradle 8.10+ 并加入 PATH 后重新运行本脚本；
        echo          (B) 用 IntelliJ IDEA 打开本项目目录（会自动导入并生成 wrapper），
        echo              然后在 IDEA 的 Gradle 面板双击 build 任务。
        echo.
        pause
        exit /b 1
    )
    gradle wrapper --gradle-version 8.10.2
    if errorlevel 1 (
        echo [错误] 生成 wrapper 失败。
        pause
        exit /b 1
    )
)

rem ---------- 3. 构建（首次需联网下载依赖，约 10-30 分钟） ----------
echo [信息] 开始构建（首次构建需联网下载 NeoForge/Minecraft 依赖，请耐心等待）...
echo.
call gradlew.bat build
if errorlevel 1 (
    echo.
    echo [错误] 构建失败，请查看上方日志。
    pause
    exit /b 1
)

rem ---------- 4. 输出产物 ----------
rem 产物名由 build.gradle 的 base.archivesName = mod_id 决定，即 backrooms_complex-<版本>.jar
set "JAR=build\libs\backrooms_complex-1.0.0.jar"
if exist "%JAR%" (
    copy /y "%JAR%" "backrooms_complex-1.0.0.jar" >nul
    echo.
    echo ============================================================
    echo   [成功] 已生成可直接使用的 JAR：
    echo     %~dp0backrooms_complex-1.0.0.jar
    echo.
    echo   使用方法：将其复制到游戏目录的 mods 文件夹即可
    echo   （需要先安装 NeoForge 21.1.x for Minecraft 1.21.1）。
    echo ============================================================
) else (
    echo [错误] 未找到构建产物 %JAR%
)
echo.
pause
