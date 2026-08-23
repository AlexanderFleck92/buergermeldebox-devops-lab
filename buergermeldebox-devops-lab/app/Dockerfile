# Stufe 1: Anwendung mit Maven und Java 21 bauen
FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /build

COPY pom.xml .
RUN mvn dependency:go-offline

COPY src ./src
RUN mvn clean package -DskipTests

# Stufe 2: Anwendung in einem schlanken Laufzeit-Image ausführen
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

RUN groupadd --system appgroup \
    && useradd --system --gid appgroup --no-create-home appuser

COPY --from=builder --chown=appuser:appgroup \
    /build/target/buergermeldebox-1.0.0.jar app.jar

USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
