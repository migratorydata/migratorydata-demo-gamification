This repository contains a demo application for gamification as detailed in the article [Building a Realtime Gamification Feature that Scales to Millions of Devices using MigratoryData, Kafka, and WebSockets: A look at Watch’NPlay Interactive Game](#). The folder `frontend` contains the source code of the UI. The folder `backend-deployment` contains the docker-compose file for running the Kafka, Ksqldb and MigratoryData-KE deployment.

How to run:

1. Start Kafka, Ksqldb and Migratorydata-ke using docker-compose.yml file from backend-deployment running command:

$ docker-compose up

2. Connect to ksqldb console using command:

$ docker exec -it ksqldb-cli ksql http://ksqldb-server:8088

3. Add ksql streams from file KSQLDB-STREAM.sql

4. Open a browser and go to url http://localhost:8800 to open realtime game app.

5. Publish a question using ksqldb console:
INSERT INTO INPUT_QUESTION (id, question, answers, answer, points) VALUES ('id-1', 'How many spectators are there altogether?', ARRAY['less than 1000', 'between 1000 and 5000', 'between 5000 and 10000', 'more than 10000'], 'between 1000 and 5000', 100);

INSERT INTO INPUT_QUESTION (id, question, answers, answer, points) VALUES ('id-2', 'What is the number of strokes with which this game will be won?', ARRAY['less than 10', 'between 10 and 20', 'between 20 and 30', 'more than 30'], 'more than 30', 100);

INSERT INTO INPUT_QUESTION (id, question, answers, answer, points) VALUES ('id-3', 'Who will win this game?', ARRAY['Simona Halep', 'Sloane Stephens'], 'Sloane Stephens', 100);
