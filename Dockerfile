# Multi-stage Dockerfile for Cocos Backend Challenge API
FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /app

# Cache Maven dependencies layer
COPY pom.xml .
RUN mvn dependency:resolve -B

COPY src ./src
RUN mvn package -DskipTests -B

# Minimal runtime image
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S cocos && adduser -S cocos -G cocos
USER cocos:cocos

COPY --from=builder /app/target/cocos-challenge-*.jar app.jar

ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["java", "-XX:+UseG1GC", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
