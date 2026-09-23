FROM eclipse-temurin:25-jdk AS builder

WORKDIR /app

ARG REVISION=0.0.1-SNAPSHOT

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

RUN chmod +x mvnw && ./mvnw -B dependency:go-offline

COPY src ./src
COPY .git .git

RUN ./mvnw -B clean package -DskipTests -Drevision=${REVISION}

FROM eclipse-temurin:25-jdk

WORKDIR /app

COPY --from=builder /app/target/reserve-*.jar app.jar

EXPOSE 6902

ENTRYPOINT ["java","-jar","app.jar", "--spring.profiles.active=prod"]