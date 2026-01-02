# ============================
# Stage 1: Build với Maven
# ============================
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build

# Copy pom.xml trước để cache dependencies
COPY pom.xml .

# Download dependencies
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build application (skip tests)
RUN mvn clean package -DskipTests

# ============================
# Stage 2: Runtime - ĐỔI SANG DEBIAN thay vì Alpine
# ============================
FROM eclipse-temurin:17-jre

WORKDIR /app

# Cài đặt fonts tiếng Việt cho Debian/Ubuntu
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    fonts-dejavu \
    fonts-dejavu-extra \
    fonts-liberation \
    fonts-noto \
    fontconfig \
    && fc-cache -f -v \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

# Set locale UTF-8
ENV LANG=C.UTF-8
ENV LC_ALL=C.UTF-8

# Copy JAR từ build stage
COPY --from=builder /build/target/LunchBot_BE-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

# Force UTF-8 encoding
ENTRYPOINT ["java", "-Dfile.encoding=UTF-8", "-Dsun.jnu.encoding=UTF-8", "-jar", "app.jar"]