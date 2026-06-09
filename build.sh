#!/bin/bash

# Build all 4 apps in parallel
echo "========================================="
echo " Building all 4 apps in parallel..."
echo "========================================="

build_app() {
    local name="$1"
    local dir="$2"
    local cmd="$3"

    output=$(cd "$dir" && eval "$cmd" 2>&1)
    if [ $? -eq 0 ]; then
        echo "✅ $name — BUILD SUCCESS"
    else
        echo "❌ $name — BUILD FAILED"
        echo "$output" | grep -E "ERROR|error:" | head -20
    fi
}

# doctor-office uses mvn clean package (Spring Boot, no assembly)
build_app "doctor-office"          "/workspace/MCP/doctor-office"                              "mvn clean package -DskipTests -q" &
build_app "statistics-app"         "/workspace/Statistics/statistics-app"                      "mvn clean package -q" &
build_app "purchase-orders"        "/workspace/Purchase_Orders/purchase-orders"                 "mvn clean package -q" &
build_app "appointment-processor"  "/workspace/Costumer/appointment-processor"                  "mvn clean package -q" &

# Wait for all background builds to finish
wait

echo "========================================="
echo " End of Build."
echo "========================================="
