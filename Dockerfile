FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY target/adaptive-mfa-backend-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT java -Xmx350m -Xms200m -Dserver.port=${PORT:-8080} -jar app.jar