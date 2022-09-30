This repository contains a demo application for gamification as detailed in the blog post[Real-time Gaming Infrastructure for Millions of Users with Kafka, KSQL, and WebSockets](https://www.confluent.io/blog/real-time-gaming-infrastructure-kafka-ksqldb-websockets/). See this demo live at:

<a href="https://migratorydata.com/blog/realtime-gamification-to-millions-of-users/" target="_blank"><img src="https://migratorydata.com/images/blog/2021/08/migratorydata-live-demo-gamfication.png" width="70%"/></a>


The folder `frontend` contains the source code of the UI. The folder `backend` contains the source code of the Leaderboard Processor, Answers Processor, and Questions Generator.

How to run:

1. Install the MigratoryData server. See the [Installation Guide](https://migratorydata.com/docs/migratorydata/installation/).

2. Install Kafka broker

    Make sure that you have Java version 8 installed. Download the [platform-independent tarball package](https://archive.apache.org/dist/kafka/2.6.0/kafka_2.12-2.6.0.tgz) of Apache Kafka and unzip it to any folder, change to that folder, and run the following two commands in two different terminals on Linux or MacOS (run the similar commands with the bat extension instead of the sh extension on Windows):
```bash
    ./bin/zookeeper-server-start.sh config/zookeeper.properties
    ./bin/kafka-server-start.sh config/server.properties
```

**Note:** This will deploy a cluster of one instance of Apache Kafka which will listen for Kafka producers and consumers on localhost, on the default port 9092.

3. Enable the Kafka Native Add-on. Edit the main configuration file `migratorydata.conf` of the MigratoryDat server and add the following line:

`ClusterEngine = kafka`

4. Configure the Kafka Native Add-on by editing the parameters `bootstrap.servers` and `topics` of the configuration file `addons/kafka/consumer.properties` as follows:

`bootstrap.servers=localhost:9092`

`topics=question,result,top,live`

**Note:** Because this demo uses a youtube video instead of a live video stream, we use an additional topic `live` to 
synchronize the feed of the live questions with the moment when the player loads the video (when loaded, the youtube video 
will seek ahead a number of seconds corresponding to the current live question).

5. Configure and start the backend, see backend/README (gradle run)

6. Configure and start the frontend, see frontend/README (npm start)
