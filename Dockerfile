# syntax=docker/dockerfile:1.7

FROM eclipse-temurin:21-jdk-jammy AS build

WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

RUN --mount=type=cache,target=/root/.m2 chmod +x mvnw \
    && ./mvnw --batch-mode dependency:go-offline

COPY src/ src/

RUN --mount=type=cache,target=/root/.m2 ./mvnw --batch-mode -DskipTests package \
    && cp target/financial-control-project-*.jar application.jar

FROM eclipse-temurin:21-jre-jammy AS runtime

RUN groupadd --system --gid 10001 cointrol \
    && useradd --system --uid 10001 --gid cointrol --home-dir /app --shell /usr/sbin/nologin cointrol

WORKDIR /app

COPY --from=build --chown=cointrol:cointrol /workspace/application.jar application.jar

USER 10001:10001

EXPOSE 8080

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

ENTRYPOINT ["java", "-jar", "/app/application.jar"]
