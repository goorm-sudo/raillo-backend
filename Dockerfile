# 🔧 Stage 1: 빌드용 (Gradle로 JAR 생성)
# 필요프로그램 설치
FROM openjdk:17-jdk-alpine AS stage1
# 파일 복사
WORKDIR /app
COPY gradle gradle
COPY src src
COPY build.gradle .
COPY settings.gradle .
COPY gradlew .
# 빌드
RUN chmod +x gradlew
RUN ./gradlew bootJar

# Stage 2: 실행용 (경량 이미지에 결과물만 포함)
FROM openjdk:17-jdk-alpine

# 1. 타임존 데이터 설치
RUN apk add --no-cache tzdata
# 2. 시스템 타임존을 Asia/Seoul로 설정
ENV TZ=Asia/Seoul
# 3. JVM도 명시적으로 Asia/Seoul로 고정
ENV JAVA_TOOL_OPTIONS="-Duser.timezone=Asia/Seoul"

WORKDIR /app
COPY --from=stage1 /app/build/libs/*.jar app.jar
# 파일이 변경되지 않으면 Docker 빌드 캐시로 인해 재사용됨
COPY files ./files

# 실행 : CMD 또는 ENTRYPOINT를 통해 컨테이너를 배열 형태의 명령어로 실행
ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-jar", "app.jar"]
