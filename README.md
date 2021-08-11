The repository contains the demo application for watch and play. 

In the directory `backend` the java application is found. The app simulates players, send trivia question, aggregates answers received from players and responds to players with the results. Also the app creates a leaderboard with top 10 players.

The frontend app for the human player is found in directory `frontend`. When the app is open in browser the MigratoryData Javascript API connects to MigratoryData KE server and waits for questions. The user selects an answer and waits for the result. On tab `Top` the player can see the leaderboard and current score.

How to run:

1. Start a kafka broker

2. Configure and start migratorydata-ke

update topic configuration for consumer in file `migratorydata-ke/kafka/consumer.properties`:
    topics=question,result,top

3. Configure and start backend, see backend/README (gradle run)
4. Configure and start frontend, see frontend/README (npm start)