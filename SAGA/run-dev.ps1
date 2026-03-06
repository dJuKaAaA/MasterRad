Write-Host "Starting SAGA Webshop in Dev mode..."

Start-Process powershell -ArgumentList "cd .\order-service\; ./mvnw quarkus:dev;"
Start-Process powershell -ArgumentList "cd .\inventory-service\; ./mvnw quarkus:dev;"
Start-Process powershell -ArgumentList "cd .\payment-service\; ./mvnw quarkus:dev;"
Start-Process powershell -ArgumentList "cd .\user-service\; ./mvnw quarkus:dev;"
Start-Process powershell -ArgumentList "cd .\identity-provider\; ./mvnw quarkus:dev;"
Start-Process powershell -ArgumentList "cd .\saga-orchestrator\; ./mvnw quarkus:dev;"

Write-Host "All services started"
