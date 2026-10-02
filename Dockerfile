# --- Build stage: compile and package the application with Maven ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy the POM first so the dependency layer is cached between builds
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q package -DskipTests

# --- Runtime stage: only the JRE and the jar ---
FROM eclipse-temurin:21-jre
WORKDIR /app

RUN groupadd --system app && useradd --system --gid app app
USER app

COPY --from=build /app/target/inventory-api.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
