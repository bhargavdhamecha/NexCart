# ---------- Build stage ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Dependency layer first: only busts the cache when build.gradle/settings.gradle/the wrapper
# itself change, not on every source edit.
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon

COPY src ./src
RUN ./gradlew bootJar --no-daemon -x test

# ---------- Runtime stage ----------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# curl is needed for the HEALTHCHECK below — temurin's alpine runtime images don't ship it.
RUN apk add --no-cache curl

RUN addgroup -S nexcart && adduser -S nexcart -G nexcart
COPY --from=build /app/build/libs/*.jar app.jar
RUN chown nexcart:nexcart app.jar
USER nexcart

# Empty default — actual heap sizing comes from docker-compose.yml's JAVA_OPTS, not baked
# into the image, so it can be re-tuned without a rebuild.
ENV JAVA_OPTS=""
EXPOSE 8081

# Shorter start-period than backend's — no JPA/Hibernate warm-up on this reactive gateway.
HEALTHCHECK --interval=15s --timeout=5s --start-period=45s --retries=6 \
  CMD curl -f http://localhost:8081/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
