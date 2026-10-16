# ── build stage ──
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# 의존성 캐시 레이어
COPY gradlew settings.gradle.kts ./
COPY gradle gradle
COPY buildSrc buildSrc
COPY app-api/build.gradle.kts app-api/
COPY common/build.gradle.kts common/
COPY domain-rds/build.gradle.kts domain-rds/
COPY domain-search/build.gradle.kts domain-search/
COPY core-web/build.gradle.kts core-web/
COPY client-tmap/build.gradle.kts client-tmap/
COPY client-kakao/build.gradle.kts client-kakao/
COPY client-oauth/build.gradle.kts client-oauth/
COPY client-s3/build.gradle.kts client-s3/
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon > /dev/null 2>&1 || true

COPY app-api/src app-api/src
COPY common/src common/src
COPY domain-rds/src domain-rds/src
COPY domain-search/src domain-search/src
COPY core-web/src core-web/src
COPY client-tmap/src client-tmap/src
COPY client-kakao/src client-kakao/src
COPY client-oauth/src client-oauth/src
COPY client-s3/src client-s3/src
RUN ./gradlew :app-api:bootJar --no-daemon

# ── runtime stage ──
FROM eclipse-temurin:21-jre
WORKDIR /app

# healthcheck용 curl + non-root 사용자
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/* && \
    # 베이스 이미지에 딸려오는 미사용 Go 바이너리(Canonical pebble) 제거 — trivy HIGH 8건(Go stdlib CVE)의 출처. 우리는 java -jar 로 직접 기동한다(SCRUM-520).
    rm -f /usr/bin/pebble && \
    useradd -r -u 1001 appuser
# bootJar 산출물(단일). 버전 문자열에 결합하지 않도록 *.jar 사용.
COPY --from=build /workspace/app-api/build/libs/*.jar app.jar
USER appuser

EXPOSE 8080
# actuator health 로 컨테이너 상태 점검 (start-period 로 부팅 시간 확보)
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD curl -fsS http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
