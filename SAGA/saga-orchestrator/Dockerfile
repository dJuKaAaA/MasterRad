FROM registry.access.redhat.com/ubi9/openjdk-21:1.23

WORKDIR /workspace

# Copy only Maven wrapper + pom for dependency caching
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Pre-download dependencies (cached layer)
RUN ./mvnw -B dependency:go-offline

EXPOSE 8080

# Run Quarkus in dev mode
CMD ["./mvnw", "quarkus:dev", "-Dquarkus.http.host=0.0.0.0"]
