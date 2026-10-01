@echo off
setlocal enabledelayedexpansion

title Cocos Challenge - Infrastructure

echo ===================================================
echo   Cocos Capital - Backend Challenge Infrastructure
echo ===================================================

set "CLI_ARG=%~1"
set "ACTION=%~1"
if "%ACTION%"=="" set "ACTION=all"

set "SHOULD_PAUSE=0"
if "%CLI_ARG%"=="" set "SHOULD_PAUSE=1"

if /i "%ACTION%"=="help" goto :show_help
if /i "%ACTION%"=="--help" goto :show_help
if /i "%ACTION%"=="-h" goto :show_help

docker info >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo.
    echo [ERROR] Docker is not running or not installed in PATH.
    echo Please start Docker Desktop and try again.
    echo.
    pause
    exit /b 1
)

if /i "%ACTION%"=="all" goto :start_all
if /i "%ACTION%"=="up" goto :start_all
if /i "%ACTION%"=="build" goto :start_build
if /i "%ACTION%"=="infra" goto :start_infra
if /i "%ACTION%"=="db" goto :start_db
if /i "%ACTION%"=="database" goto :start_db
if /i "%ACTION%"=="down" goto :stop_all
if /i "%ACTION%"=="stop" goto :stop_all
if /i "%ACTION%"=="restart" goto :restart_all
if /i "%ACTION%"=="logs" goto :show_logs
if /i "%ACTION%"=="status" goto :show_status
if /i "%ACTION%"=="ps" goto :show_status

echo.
echo [WARN] Unknown parameter: %ACTION%
echo Run 'run-infra.bat help' to see usage instructions.
goto :end

:start_all
echo.
echo [INFO] Starting all services (API, DB, pgweb, Prometheus, Grafana)...
echo ---------------------------------------------------
docker compose up -d
echo.
echo [SUCCESS] All services started successfully!
goto :print_links

:start_build
echo.
echo [INFO] Rebuilding images and starting all services...
echo ---------------------------------------------------
docker compose up -d --build
echo.
echo [SUCCESS] Services rebuilt and started!
goto :print_links

:start_infra
echo.
echo [INFO] Starting infrastructure only (DB, pgweb, Prometheus, Grafana, Loki, Promtail)...
echo ---------------------------------------------------
docker compose up -d postgres pgweb prometheus grafana loki promtail
echo.
echo [SUCCESS] Infrastructure is ready! You can run the API locally in your IDE:
echo   mvn spring-boot:run -Dspring-boot.run.profiles=local
goto :print_links

:start_db
echo.
echo [INFO] Starting PostgreSQL and pgweb database viewer...
echo ---------------------------------------------------
docker compose up -d postgres pgweb
echo.
echo [SUCCESS] Database and web viewer are ready!
echo.
echo   - pgweb (Web GUI):      http://localhost:8081
echo   - PostgreSQL Direct:    localhost:5432 (DB: cocos_local, User: postgres, Pass: postgres)
echo.
echo You can run the API locally in your IDE:
echo   mvn spring-boot:run -Dspring-boot.run.profiles=local
echo.
goto :end

:stop_all
echo.
echo [INFO] Stopping all containers...
docker compose down
echo [SUCCESS] All containers stopped.
goto :end

:restart_all
echo.
echo [INFO] Restarting services...
docker compose down
docker compose up -d
echo [SUCCESS] Services restarted successfully.
goto :print_links

:show_logs
echo.
echo [INFO] Showing Docker logs (Press Ctrl+C to exit)...
docker compose logs -f
goto :end

:show_status
echo.
echo [INFO] Container status:
docker compose ps
goto :end

:show_help
echo.
echo Usage: run-infra.bat [command]
echo.
echo Commands:
echo   (no args) / all : Start ALL services (API + DB + pgweb + Prometheus + Grafana)
echo   infra            : Start infrastructure only (DB + pgweb + Prometheus + Grafana)
echo   db               : Start PostgreSQL and pgweb only
echo   build            : Rebuild images from scratch and start
echo   logs             : Tail logs in real time
echo   status / ps      : Show status of running containers
echo   stop / down      : Stop and remove containers
echo   restart          : Restart all services
echo   help             : Display this help message
echo.
goto :end

:print_links
echo.
echo ===================================================
echo                 SERVICES AND LINKS                 
echo ===================================================
echo   API and Documentation:
echo     - REST API:            http://localhost:8080
echo     - Swagger / OpenAPI:   http://localhost:8080/swagger-ui.html
echo.
echo   Database and Web Viewer:
echo     - pgweb (DB Web GUI):  http://localhost:8081
echo     - PostgreSQL Direct:   localhost:5432 (DB: cocos_local, User: postgres, Pass: postgres)
echo.
echo   Observability and Monitoring:
echo     - Grafana Dashboards:  http://localhost:3000 (User: admin / Pass: admin)
echo     - Prometheus Server:   http://localhost:9090
echo     - Loki (Logs Engine):  http://localhost:3100
echo ===================================================
echo   Tip: Hold [Ctrl + Click] on any URL above to open it in your browser.
echo ===================================================
goto :end

:end
if "!SHOULD_PAUSE!"=="1" (
    echo.
    echo ===================================================
    echo Press any key to close this window...
    echo ===================================================
    pause >nul
)
