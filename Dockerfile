# ── build stage ──
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# 의존성 캐시 레이어
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle gradle
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon > /dev/null 2>&1 || true

COPY src src
RUN ./gradlew bootJar --no-daemon

# ── runtime stage ──
FROM eclipse-temurin:21-jre
WORKDIR /app

# non-root 실행 (보안)
RUN useradd -r -u 1001 appuser
# bootJar 산출물(단일). 버전 문자열에 결합하지 않도록 *.jar 사용.
COPY --from=build /workspace/build/libs/*.jar app.jar
USER appuser

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
