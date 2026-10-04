# ── build stage ──
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# 의존성 캐시 레이어 — 빌드 스크립트만 먼저 복사(소스만 바뀌면 이 레이어 캐시 재사용)
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle gradle
COPY common/build.gradle.kts common/
COPY area/build.gradle.kts area/
COPY media/build.gradle.kts media/
COPY direction/build.gradle.kts direction/
COPY place/build.gradle.kts place/
COPY course/build.gradle.kts course/
COPY user/build.gradle.kts user/
COPY mobile/build.gradle.kts mobile/
COPY bootstrap/build.gradle.kts bootstrap/
RUN chmod +x gradlew && ./gradlew :bootstrap:dependencies --no-daemon > /dev/null 2>&1 || true

# 모듈 소스 복사 후 실행 산출물(bootJar) 빌드 (도메인 단위 모듈)
COPY common/src common/src
COPY area/src area/src
COPY media/src media/src
COPY direction/src direction/src
COPY place/src place/src
COPY course/src course/src
COPY user/src user/src
COPY mobile/src mobile/src
COPY bootstrap/src bootstrap/src
RUN ./gradlew :bootstrap:bootJar --no-daemon

# ── runtime stage ──
FROM eclipse-temurin:21-jre
WORKDIR /app

# healthcheck용 curl + non-root 사용자
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/* && \
    # 베이스 이미지에 딸려오는 미사용 Go 바이너리(Canonical pebble) 제거 — trivy HIGH 8건(Go stdlib CVE)의 출처. 우리는 java -jar 로 직접 기동한다(SCRUM-520).
    rm -f /usr/bin/pebble && \
    useradd -r -u 1001 appuser
# bootJar 산출물(단일, plain jar 는 비활성). 버전 문자열에 결합하지 않도록 *.jar 사용.
COPY --from=build /workspace/bootstrap/build/libs/*.jar app.jar
USER appuser

EXPOSE 8080
# actuator health 로 컨테이너 상태 점검 (start-period 로 부팅 시간 확보)
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD curl -fsS http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
