# syntax=docker/dockerfile:1

# ---- build stage ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# Copy the reactor pom and every module pom first so dependency resolution can be cached
# independently of source changes.
COPY pom.xml ./
COPY proto/pom.xml proto/
COPY core-service/pom.xml core-service/
COPY agent-service/pom.xml agent-service/
COPY case-data-service/pom.xml case-data-service/

# Now the sources, and build only what core-service needs (proto + core-service, plus upstream).
COPY . .
RUN mvn -pl proto,core-service -am clean package -DskipTests

# ---- runtime stage ----
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/core-service/target/core-service.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
