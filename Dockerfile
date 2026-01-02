# Giai đoạn 1: Build file JAR bằng Maven
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app
# Copy toàn bộ code vào trong container
COPY . .
# Chạy lệnh build ngay trong Docker
RUN mvn clean package -DskipTests

# Giai đoạn 2: Chạy ứng dụng với JRE nhẹ
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
# Copy file JAR từ giai đoạn build sang
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]