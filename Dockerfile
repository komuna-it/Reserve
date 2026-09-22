FROM eclipse-temurin:25-jdk AS builder

WORKDIR /app

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

RUN chmod +x mvnw && ./mvnw -B dependency:go-offline

COPY src ./src

RUN ./mvnw -B clean package -DskipTests

FROM eclipse-temurin:25-jdk

WORKDIR /app

COPY --from=builder /app/target/reserve-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 6902

ENTRYPOINT ["java","-jar","app.jar", "--spring.profiles.active=prod"]