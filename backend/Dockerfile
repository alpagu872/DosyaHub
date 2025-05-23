FROM maven:3.8.6-openjdk-17-slim AS build
WORKDIR /app
COPY pom.xml .
# Bağımlılıkları indirme
RUN mvn dependency:go-offline -B
COPY src ./src
# Uygulamayı derleme
RUN mvn package -DskipTests

FROM openjdk:17-jdk-slim
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
# Dosya yükleme dizini
RUN mkdir -p /app/uploads
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"] 