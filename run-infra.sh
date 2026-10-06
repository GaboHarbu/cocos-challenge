#!/usr/bin/env bash
# ===================================================
#   Cocos Capital - Backend Challenge Infrastructure
#   macOS & Linux helper script
# ===================================================

set -e

ACTION="${1:-all}"

GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

print_header() {
    echo -e "${BLUE}===================================================${NC}"
    echo -e "${BLUE}  Cocos Capital - Backend Challenge Infrastructure ${NC}"
    echo -e "${BLUE}===================================================${NC}"
}

check_docker() {
    if ! docker info > /dev/null 2>&1; then
        echo -e "${RED}[ERROR] Docker is not running or not installed in PATH.${NC}"
        echo "Please start Docker daemon / Docker Desktop and try again."
        exit 1
    fi
}

print_links() {
    echo ""
    echo -e "${GREEN}===================================================${NC}"
    echo -e "${GREEN}                 SERVICES AND LINKS                ${NC}"
    echo -e "${GREEN}===================================================${NC}"
    echo -e "  ${YELLOW}API and Documentation:${NC}"
    echo -e "    - REST API:            http://localhost:8080"
    echo -e "    - Swagger / OpenAPI:   http://localhost:8080/swagger-ui.html"
    echo ""
    echo -e "  ${YELLOW}Database and Web Viewer:${NC}"
    echo -e "    - pgweb (DB Web GUI):  http://localhost:8081"
    echo -e "    - PostgreSQL Direct:   localhost:5432 (DB: cocos_local, User: postgres, Pass: postgres)"
    echo ""
    echo -e "  ${YELLOW}Observability and Monitoring:${NC}"
    echo -e "    - Grafana Dashboards:  http://localhost:3000 (User: admin / Pass: admin)"
    echo -e "    - Prometheus Server:   http://localhost:9090"
    echo -e "    - Loki (Logs Engine):  http://localhost:3100"
    echo -e "${GREEN}===================================================${NC}"
}

show_help() {
    echo ""
    echo "Usage: ./run-infra.sh [command]"
    echo ""
    echo "Commands:"
    echo "  (no args) / all : Start ALL services (API + DB + pgweb + Prometheus + Grafana + Loki)"
    echo "  infra            : Start infrastructure only (DB + pgweb + Prometheus + Grafana + Loki)"
    echo "  db               : Start PostgreSQL and pgweb only"
    echo "  build            : Rebuild Docker images and start services"
    echo "  logs             : Tail container logs in real time"
    echo "  status / ps      : Show status of running containers"
    echo "  stop / down      : Stop and remove containers"
    echo "  restart          : Restart all services"
    echo "  help             : Display this help message"
    echo ""
}

print_header
check_docker

case "$ACTION" in
    all|up)
        echo -e "${YELLOW}[INFO] Starting all services (API, DB, pgweb, Prometheus, Grafana, Loki)...${NC}"
        docker compose up -d
        print_links
        ;;
    build)
        echo -e "${YELLOW}[INFO] Rebuilding images and starting all services...${NC}"
        docker compose up -d --build
        print_links
        ;;
    infra)
        echo -e "${YELLOW}[INFO] Starting infrastructure only (DB, pgweb, Prometheus, Grafana, Loki, Promtail)...${NC}"
        docker compose up -d postgres pgweb prometheus grafana loki promtail
        echo ""
        echo -e "${GREEN}[SUCCESS] Infrastructure is ready! You can run the API locally:${NC}"
        echo "  mvn spring-boot:run -Dspring-boot.run.profiles=local"
        print_links
        ;;
    db|database)
        echo -e "${YELLOW}[INFO] Starting PostgreSQL and pgweb database viewer...${NC}"
        docker compose up -d postgres pgweb
        echo ""
        echo -e "${GREEN}[SUCCESS] Database and web viewer are ready!${NC}"
        echo "  - pgweb (Web GUI):      http://localhost:8081"
        echo "  - PostgreSQL Direct:    localhost:5432 (DB: cocos_local, User: postgres, Pass: postgres)"
        echo ""
        echo "You can run the API locally:"
        echo "  mvn spring-boot:run -Dspring-boot.run.profiles=local"
        ;;
    stop|down)
        echo -e "${YELLOW}[INFO] Stopping all containers...${NC}"
        docker compose down
        echo -e "${GREEN}[SUCCESS] All containers stopped.${NC}"
        ;;
    restart)
        echo -e "${YELLOW}[INFO] Restarting services...${NC}"
        docker compose down
        docker compose up -d
        print_links
        ;;
    logs)
        echo -e "${YELLOW}[INFO] Tailing logs (Press Ctrl+C to exit)...${NC}"
        docker compose logs -f
        ;;
    status|ps)
        echo -e "${YELLOW}[INFO] Container status:${NC}"
        docker compose ps
        ;;
    help|--help|-h)
        show_help
        ;;
    *)
        echo -e "${RED}[WARN] Unknown parameter: $ACTION${NC}"
        show_help
        exit 1
        ;;
esac
