FROM openjdk:26-ea-jdk AS build
WORKDIR /app

RUN microdnf install -y maven && microdnf clean all

COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn dependency:go-offline -DskipTests

COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn clean package -DskipTests

FROM openjdk:26-ea-jdk
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
