FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -B -q dependency:copy-dependencies package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/classes /app/classes
COPY --from=build /app/target/dependency /app/dependency
EXPOSE 50051
CMD ["java", "-cp", "/app/classes:/app/dependency/*", "Main"]
