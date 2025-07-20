@echo off
echo 正在修复Maven缓存问题...

echo.
echo 1. 清理Maven本地仓库中的失败缓存...
for /r "%USERPROFILE%\.m2\repository" %%f in (*.lastUpdated) do (
    echo 删除: %%f
    del "%%f" 2>nul
)

echo.
echo 2. 清理Spring Boot AMQP相关缓存...
rmdir /s /q "%USERPROFILE%\.m2\repository\org\springframework\boot\spring-boot-starter-amqp" 2>nul
rmdir /s /q "%USERPROFILE%\.m2\repository\org\springframework\amqp" 2>nul
rmdir /s /q "%USERPROFILE%\.m2\repository\com\rabbitmq" 2>nul

echo.
echo 3. 清理Spring Cloud相关缓存...
rmdir /s /q "%USERPROFILE%\.m2\repository\org\springframework\cloud\spring-cloud-stream" 2>nul
rmdir /s /q "%USERPROFILE%\.m2\repository\org\springframework\cloud\spring-cloud-stream-binder-rabbit" 2>nul

echo.
echo 4. 测试网络连接...
ping -n 1 maven.aliyun.com >nul 2>&1
if %errorlevel% equ 0 (
    echo ✓ 阿里云Maven镜像连接正常
) else (
    echo ✗ 阿里云Maven镜像连接失败，请检查网络
)

echo.
echo 缓存清理完成！
echo 现在可以重新运行Maven构建命令了。
echo.
echo 建议使用以下命令重新构建：
echo mvn clean compile -U -pl message-service
echo.
pause