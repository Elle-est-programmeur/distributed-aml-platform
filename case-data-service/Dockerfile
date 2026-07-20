# syntax=docker/dockerfile:1

# ---- build stage ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml ./
COPY proto/pom.xml proto/
COPY core-service/pom.xml core-service/
COPY agent-service/pom.xml agent-service/
COPY case-data-service/pom.xml case-data-service/

COPY . .
RUN mvn -pl proto,case-data-service -am clean package -DskipTests

# ---- runtime stage ----
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/case-data-service/target/case-data-service.jar app.jar
EXPOSE 9090
ENTRYPOINT ["java","-jar","app.jar"]
