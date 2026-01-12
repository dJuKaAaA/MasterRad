currDir=pwd

cd ./saga-orchestrator
./mvnw clean package
docker build -f src/main/docker/Dockerfile.jvm -t quarkus/saga-orchestrator-jvm .

cd ../order-service
./mvnw clean package
docker build -f src/main/docker/Dockerfile.jvm -t quarkus/order-service-saga-jvm .

cd ../inventory-service
./mvnw clean package
docker build -f src/main/docker/Dockerfile.jvm -t quarkus/inventory-service-saga-jvm .

cd ../payment-service
./mvnw clean package
docker build -f src/main/docker/Dockerfile.jvm -t quarkus/payment-service-saga-jvm .

cd ../identity-provider
./mvnw clean package
docker build -f src/main/docker/Dockerfile.jvm -t quarkus/identity-provider-saga-jvm .

cd ../user-service
./mvnw clean package
docker build -f src/main/docker/Dockerfile.jvm -t quarkus/user-service-saga-jvm .

cd $currDir
