Write-Host "Start building SAGA Webshop..."

Start-Process powershell -ArgumentList "cd .\order-service\; ./mvnw clean package; docker build -f src/main/docker/Dockerfile.jvm -t quarkus/order-service-saga-jvm .; Read-Host 'Press Enter to continue'"
Start-Process powershell -ArgumentList "cd .\inventory-service\; ./mvnw clean package; docker build -f src/main/docker/Dockerfile.jvm -t quarkus/inventory-service-saga-jvm .; Read-Host 'Press Enter to continue'"
Start-Process powershell -ArgumentList "cd .\payment-service\; ./mvnw clean package; docker build -f src/main/docker/Dockerfile.jvm -t quarkus/payment-service-saga-jvm .; Read-Host 'Press Enter to continue'"
Start-Process powershell -ArgumentList "cd .\identity-provider\; ./mvnw clean package; docker build -f src/main/docker/Dockerfile.jvm -t quarkus/identity-provider-saga-jvm .; Read-Host 'Press Enter to continue'"
Start-Process powershell -ArgumentList "cd .\user-service\; ./mvnw clean package; docker build -f src/main/docker/Dockerfile.jvm -t quarkus/user-service-saga-jvm .; Read-Host 'Press Enter to continue'"
Start-Process powershell -ArgumentList "cd .\saga-orchestrator\; ./mvnw clean package; docker build -f src/main/docker/Dockerfile.jvm -t quarkus/saga-orchestrator-jvm .; Read-Host 'Press Enter to continue'"

Write-Host "Finished building SAGA Webshop"