#!/bin/bash

echo "🚀 Starting Digital Procurement Platform Microservices..."
echo ""
echo "🗄️  Using PostgreSQL databases"
echo "ℹ️  Make sure PostgreSQL is running and databases are created!"
echo "   Run: psql -U postgres -f setup-databases.sql"
echo ""

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Check if PostgreSQL is running
if ! command -v psql &> /dev/null; then
    echo -e "${RED}❌ PostgreSQL not found! Please install PostgreSQL first.${NC}"
    echo "   macOS: brew install postgresql@16"
    echo "   Ubuntu: sudo apt install postgresql"
    exit 1
fi

# Function to start a service
start_service() {
    local service_name=$1
    local service_dir=$2
    local port=$3
    
    echo -e "${BLUE}Starting $service_name on port $port...${NC}"
    cd $service_dir
    mvn spring-boot:run > "../logs/${service_name}.log" 2>&1 &
    echo $! > "../logs/${service_name}.pid"
    cd ..
    echo -e "${GREEN}✓ $service_name started${NC}"
    echo ""
}

# Create logs directory
mkdir -p logs

# Start services in order
echo "1️⃣  Starting Procurement Service..."
start_service "procurement-service" "procurement-service" "8084"
sleep 10

echo "2️⃣  Starting Supplier Service..."
start_service "supplier-service" "supplier-service" "8085"
sleep 10

echo "3️⃣  Starting Contract Service..."
start_service "contract-service" "contract-service" "8086"
sleep 10

echo "4️⃣  Starting API Gateway..."
start_service "api-gateway" "api-gateway" "8080"
sleep 10

echo ""
echo -e "${GREEN}✅ All microservices started successfully!${NC}"
echo ""
echo "📊 Service Status:"
echo "  - Procurement Service: http://localhost:8084"
echo "  - Supplier Service: http://localhost:8085"
echo "  - Contract Service: http://localhost:8086"
echo "  - API Gateway: http://localhost:8080"
echo ""
echo "🗄️  PostgreSQL Databases:"
echo "  - procurement_db (localhost:5432)"
echo "  - supplier_db (localhost:5432)"
echo "  - contract_db (localhost:5432)"
echo ""
echo "📝 Logs are available in the 'logs' directory"
echo "🛑 To stop all services, run: ./stop-services.sh"
