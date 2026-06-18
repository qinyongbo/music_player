@echo off
REM ========================================================
REM 音乐播放器 - 启动脚本
REM ========================================================

setlocal enabledelayedexpansion

echo.
echo ========================================================
echo      音乐播放器 Pro - 启动脚本
echo ========================================================
echo.

REM 检查 Java 版本
echo [步骤 1] 检查 Java 环境...
java -version >nul 2>&1
if !errorlevel! neq 0 (
    echo ❌ 错误: 未找到 Java，请先安装 JDK 18 或更高版本
    echo 下载地址: https://www.oracle.com/java/technologies/downloads/
    pause
    exit /b 1
)
echo ✓ Java 环境已就位

REM 检查 VLC 安装
echo.
echo [步骤 2] 检查 VLC 安装...
if not exist "D:\Program Files\VideoLAN\VLC" (
    echo ❌ 错误: 找不到 VLC，请确保 VLC 安装在: D:\Program Files\VideoLAN\VLC
    echo 下载地址: https://www.videolan.org/vlc/
    pause
    exit /b 1
)
echo ✓ VLC 已安装

REM 检查是否需要编译
echo.
echo [步骤 3] 检查编译状态...
if not exist "target\music-player-2.0.0-shaded.jar" (
    echo ⚠️  未找到编译产物，正在编译项目...
    call mvn clean package -DskipTests
    if !errorlevel! neq 0 (
        echo ❌ 编译失败
        pause
        exit /b 1
    )
    echo ✓ 编译成功
) else (
    echo ✓ 编译产物已存在
)

REM 启动应用
echo.
echo [步骤 4] 启动音乐播放器...
echo.
start "" javaw -Dfile.encoding=UTF-8 -Dvlc.path="D:\Program Files\VideoLAN\VLC" -jar target\music-player-2.0.0-shaded.jar

echo ✓ 应用已启动！
echo.
echo ========================================================
echo 提示: 如需查看日志，请在命令行中运行:
echo java -Dfile.encoding=UTF-8 -Dvlc.path="D:\Program Files\VideoLAN\VLC" -jar target\music-player-2.0.0-shaded.jar
echo ========================================================
echo.
