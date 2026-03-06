Write-Host "Start building TCC Webshop..."

Start-Process powershell -ArgumentList "cd .\coordinator\; ./mvnw clean package; docker build -f src/main/docker/Dockerfile.jvm -t quarkus/coordinator-tcc-jvm ."
Start-Process powershell -ArgumentList "cd .\order-service\; ./mvnw clean package; docker build -f src/main/docker/Dockerfile.jvm -t quarkus/order-service-tcc-jvm ."
Start-Process powershell -ArgumentList "cd .\inventory-service\; ./mvnw clean package; docker build -f src/main/docker/Dockerfile.jvm -t quarkus/inventory-service-tcc-jvm ."
Start-Process powershell -ArgumentList "cd .\payment-service\; ./mvnw clean package; docker build -f src/main/docker/Dockerfile.jvm -t quarkus/payment-service-tcc-jvm ."
Start-Process powershell -ArgumentList "cd .\identity-provider\; ./mvnw clean package; docker build -f src/main/docker/Dockerfile.jvm -t quarkus/identity-provider-tcc-jvm ."
Start-Process powershell -ArgumentList "cd .\user-service\; ./mvnw clean package; docker build -f src/main/docker/Dockerfile.jvm -t quarkus/user-service-tcc-jvm ."

Write-Host "Finished building TCC Webshop"