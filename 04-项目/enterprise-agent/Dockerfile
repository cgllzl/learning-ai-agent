FROM maven:3.8.4-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/enterprise-agent-0.0.1-SNAPSHOT.jar app.jar
ENV DEEPSEEK_API_KEY=""
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
