FROM --platform=linux/amd64 eclipse-temurin:21-alpine AS build
ENV GRADLE_OPTS="-Dorg.gradle.daemon=false -Dkotlin.incremental=false"
WORKDIR /app

COPY gradlew settings.gradle.kts ./
COPY gradle ./gradle
COPY gradle.properties ./gradle.properties
RUN ./gradlew --version

COPY build.gradle.kts ./
COPY composeApp ./composeApp
COPY server ./server
COPY shared ./shared

RUN ./gradlew :server:installDist

FROM alpine:3.19.0
LABEL maintainer="Vishnu Rajeevan <github@vishnu.email>"

RUN apk add --no-cache \
      curl \
      openjdk21-jre \
      bash \
 && rm -rf /var/cache/* \
 && mkdir /var/cache/apk

ENV \
    # Fail if cont-init scripts exit with non-zero code.
    LIBRO_FM_USERNAME="" \
    LIBRO_FM_PASSWORD="" \
    DATA_DIR="" \
    MEDIA_DIR=""

WORKDIR /app
COPY scripts/run.sh ./
COPY --from=build /app/server/build/install/server ./

CMD /app/run.sh