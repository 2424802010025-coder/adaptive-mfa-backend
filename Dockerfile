# STAGE 1: Build file .jar bằng Maven Wrapper có sẵn
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app
COPY . .
RUN chmod +x mvnw
RUN ./mvnw clean package -DskipTests

# STAGE 2: Chạy ứng dụng Java nhẹ nhàng
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-Xmx350m", "-Xms200m", "-jar", "app.jar"]