# BeforeYouGo runtime image.
# The Jenkins pipeline already runs `mvn package`, so this image only copies the
# finished jar onto a JRE — no second Maven build, no JDK in the runtime image.
FROM eclipse-temurin:21-jre

ARG APP_VERSION=local-dev
ARG GIT_COMMIT=unknown

LABEL org.opencontainers.image.title="beforeyougo" \
      org.opencontainers.image.version="${APP_VERSION}" \
      org.opencontainers.image.revision="${GIT_COMMIT}"

WORKDIR /app

# Run as an unprivileged user, never root.
RUN groupadd --system --gid 10001 app \
 && useradd --system --uid 10001 --gid app --no-create-home app

COPY --chown=app:app target/app.jar /app/app.jar

# APP_VERSION is shown in the page footer; Kubernetes also sets it at deploy time.
# MaxRAMPercentage makes the JVM respect the container memory limit.
ENV APP_VERSION=${APP_VERSION} \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"

USER 10001
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
