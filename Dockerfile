# Step 1: Build stage
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY . .
RUN mvn -e -X clean package -DskipTests

# Step 2: Run stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy only the built jar from the build stage
COPY --from=build /app/target/demo-chess-0.0.1-SNAPSHOT.jar app.jar

# Standard port for Spring Boot
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]