FROM gradle:6.9.4-jdk8 AS build-backend
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle build

FROM amazoncorretto:8-alpine-jre
WORKDIR /backend
COPY --from=build-backend /home/gradle/src/build/libs/backend.jar ./backend.jar
COPY --from=build-backend /home/gradle/src/config/quiz.txt ./config/quiz.txt

ENV SERVER=127.0.0.1:8800
ENV TOKEN=some-token
ENV SUBJECTS_PREFIX=/admin/gamification

ENTRYPOINT exec java -jar backend.jar $SERVER $TOKEN $SUBJECTS_PREFIX