# STAGE 1: Build file .jar bằng Maven
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy trực tiếp file jar đã build từ máy bạn lên container
COPY target/adaptive-mfa-backend-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

# Chạy ứng dụng và ép giới hạn RAM vừa vặn cho Render (tối đa 350MB)
ENTRYPOINT ["java", "-Xmx350m", "-Xms200m", "-jar", "app.jar"]