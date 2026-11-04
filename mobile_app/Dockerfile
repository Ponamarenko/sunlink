FROM eclipse-temurin:11-jdk

# Установка Gradle
ENV GRADLE_VERSION=7.2
ENV GRADLE_HOME /opt/gradle
ENV PATH $PATH:/opt/gradle/bin

RUN apt-get update && apt-get install -y wget unzip && \
    wget -q https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip && \
    unzip -d /opt gradle-${GRADLE_VERSION}-bin.zip && \
    ln -s /opt/gradle-${GRADLE_VERSION} /opt/gradle && \
    rm gradle-${GRADLE_VERSION}-bin.zip

# Копирование проекта
COPY . /app
WORKDIR /app

# Сборка APK
RUN ./gradlew assembleDebug

# Копирование результата
FROM alpine
COPY --from=0 /app/app/build/outputs/apk/debug/app-debug.apk /app/sunlink-mobile-v1.0.8.apk
CMD ["ls", "/app"]