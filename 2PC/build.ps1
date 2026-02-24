Write-Host "Start building 2PC Webshop..."

Start-Process powershell -ArgumentList "cd .\order-service\; ./mvnw clean package; docker build -f src/main/docker/Dockerfile.jvm -t quarkus/order-service-2pc-jvm ."
Start-Process powershell -ArgumentList "cd .\inventory-service\; ./mvnw clean package; docker build -f src/main/docker/Dockerfile.jvm -t quarkus/inventory-service-2pc-jvm ."
Start-Process powershell -ArgumentList "cd .\payment-service\; ./mvnw clean package; docker build -f src/main/docker/Dockerfile.jvm -t quarkus/payment-service-2pc-jvm ."
Start-Process powershell -ArgumentList "cd .\identity-provider\; ./mvnw clean package; docker build -f src/main/docker/Dockerfile.jvm -t quarkus/identity-provider-2pc-jvm ."
Start-Process powershell -ArgumentList "cd .\user-service\; ./mvnw clean package; docker build -f src/main/docker/Dockerfile.jvm -t quarkus/user-service-2pc-jvm ."
Start-Process powershell -ArgumentList "cd .\coordinator\; ./mvnw clean package; docker build -f src/main/docker/Dockerfile.jvm -t quarkus/coordinator-2pc-jvm ."

Write-Host "Finished building 2PC Webshop"