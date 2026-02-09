#!/bin/bash

echo "🛑 Stopping Digital Procurement Platform Microservices..."

# Kill all Spring Boot processes
pkill -f "spring-boot:run"

# Stop Docker containers
docker-compose down

echo "✅ All services stopped!"
