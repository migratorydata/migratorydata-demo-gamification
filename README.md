This repository contains a demo application for gamification as detailed in the article [Building a Realtime Gamification Feature that Scales to Millions of Devices using MigratoryData, Kafka, and WebSockets: A look at Watch’NPlay Interactive Game](https://migratorydata.com/2021/08/30/gamification-millions-devices/). The folder `frontend` contains the source code of the UI. The folder `backend` contains the source code of the Leaderboard Processor, Answers Processor, and Questions Generator.

How to run:

1. Install the MigratoryData server. See the [Installation Guide](https://migratorydata.com/docs/migratorydata/installation/).

2. Enable the Kafka Native Add-on. Edit the main configuration file `migratorydata.conf` of the MigratoryDat server and add the following line:

`ClusterEngine = kafka`

3. Configure the Kafka Native Add-on by editing the parameter `topics` of the configuration file `addons/kafka/consumer.properties` as follows:
   
`topics=question,result,top,live`
   
**Note:** Because this demo uses a youtube video instead of a live video stream, we use an additional topic `live` to 
synchronize the feed of the live questions with the moment when the player loads the video (when loaded, the youtube video 
will seek ahead a number of seconds corresponding to the current live question).

3. Configure and start the backend, see backend/README (gradle run)

4. Configure and start the frontend, see frontend/README (npm start)
