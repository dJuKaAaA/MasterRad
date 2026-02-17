currDir=pwd

cd ./coordinator
./mvnw clean package
docker build -f src/main/docker/Dockerfile.jvm -t quarkus/coordinator-tcc-jvm .

cd ../order-service
./mvnw clean package
docker build -f src/main/docker/Dockerfile.jvm -t quarkus/order-service-tcc-jvm .

cd ../inventory-service
./mvnw clean package
docker build -f src/main/docker/Dockerfile.jvm -t quarkus/inventory-service-tcc-jvm .

cd ../payment-service
./mvnw clean package
docker build -f src/main/docker/Dockerfile.jvm -t quarkus/payment-service-tcc-jvm .

cd ../identity-provider
./mvnw clean package
docker build -f src/main/docker/Dockerfile.jvm -t quarkus/identity-provider-tcc-jvm .

cd ../user-service
./mvnw clean package
docker build -f src/main/docker/Dockerfile.jvm -t quarkus/user-service-tcc-jvm .

cd $currDir

read -p "Press Enter to continue..."
