FROM maven:3.9.11-eclipse-temurin-21 AS build

WORKDIR /workspace
COPY backend/pom.xml pom.xml
COPY backend/src src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre

WORKDIR /app
COPY --from=build /workspace/target/resume-parser-api-1.0.0.jar app.jar

ENV BACKEND_HOST=0.0.0.0
ENV BACKEND_PORT=10000
ENV DATABASE_PATH=/var/data/resume_history.sqlite3
EXPOSE 10000

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
