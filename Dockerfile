FROM gradle:8.5-jdk17 AS builder
WORKDIR /build
COPY build.gradle settings.gradle ./
ENV JAVA_OPTS="-Dhttps.protocols=TLSv1.2,TLSv1.3"
RUN gradle --no-daemon dependencies 2>/dev/null || true
COPY src src
RUN gradle --no-daemon build -x test

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /build/build/libs/template-1.0-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
