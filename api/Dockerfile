# ---- build stage: Maven + JDK live only here, nothing to install on your machine ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B -q dependency:go-offline || true
COPY src ./src
RUN mvn -B -q -DskipTests package

# ---- runtime stage: JRE only, runs as an unprivileged user ----
FROM eclipse-temurin:21-jre
RUN useradd --system --create-home --shell /usr/sbin/nologin app
WORKDIR /app
COPY --from=build /build/target/battleship-api.jar app.jar
USER app
EXPOSE 8081
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
