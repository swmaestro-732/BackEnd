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

# healthcheck용 curl + non-root 사용자
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/* && \
    useradd -r -u 1001 appuser
# bootJar 산출물(단일). 버전 문자열에 결합하지 않도록 *.jar 사용.
COPY --from=build /workspace/build/libs/*.jar app.jar
USER appuser

EXPOSE 8080
# actuator health 로 컨테이너 상태 점검 (start-period 로 부팅 시간 확보)
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD curl -fsS http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
