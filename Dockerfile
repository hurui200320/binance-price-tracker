FROM gradle:7.3-jdk11 AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle installDist --no-daemon

FROM azul/zulu-openjdk-alpine:11-jre-headless AS runtime
RUN mkdir /opt/app
COPY --from=build /home/gradle/src/build/install/binance-price-tracker /opt/app
WORKDIR /opt/app
CMD ["sh", "./bin/binance-price-tracker"]
