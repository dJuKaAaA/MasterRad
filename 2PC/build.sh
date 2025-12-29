currDir=pwd

cd ./coordinator
./mvnw clean package
docker build -f src/main/docker/Dockerfile.jvm -t quarkus/coordinator-jvm .

cd ../order-service
./mvnw clean package
docker build -f src/main/docker/Dockerfile.jvm -t quarkus/order-service-2pc-jvm .

cd ../inventory-service
./mvnw clean package
docker build -f src/main/docker/Dockerfile.jvm -t quarkus/inventory-service-2pc-jvm .

cd ../payment-service
./mvnw clean package
docker build -f src/main/docker/Dockerfile.jvm -t quarkus/payment-service-2pc-jvm .

cd ../identity-provider
./mvnw clean package
docker build -f src/main/docker/Dockerfile.jvm -t quarkus/identity-provider-2pc-jvm .

cd $currDir
