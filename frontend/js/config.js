var USER_ID = getUserId();

var TOKEN = "some-token";

var SERVERS = [
    "http://localhost:8800",
];

var KAFKA_QUESTION_TOPIC = "question";
var KAFKA_ANSWER_TOPIC = "answer";
var KAFKA_RESULT_TOPIC = "result";
var KAFKA_TOP_TOPIC = "top";
var KAFKA_GETTOP_TOPIC = "gettop";

var QUESTIONS_SUBJECT = "/" + KAFKA_QUESTION_TOPIC;
var RESULTS_SUBJECT = "/" + KAFKA_RESULT_TOPIC + "/" + USER_ID;
var ANSWERS_SUBJECT = "/" + KAFKA_ANSWER_TOPIC + "/" + USER_ID;
var TOP_USERS_SUBJECT = "/" + KAFKA_TOP_TOPIC + "/" + USER_ID;
var TOP_SERVICE_SUBJECT = "/" + KAFKA_GETTOP_TOPIC;
var LIVE_VIDEO_TIME_SUBJECT = "/live/time";

var SUBJECTS = [
    QUESTIONS_SUBJECT,
    RESULTS_SUBJECT,
    TOP_USERS_SUBJECT,
    LIVE_VIDEO_TIME_SUBJECT
];

var questionTimer;
// global variable for the player
var player;
var videoSeekSeconds = -1;
var videoPlayerReady = false;

var WAITING_TEXT = "Waiting for result.";
var INCORRECT_TEXT = "The answer was incorrect, wait for the next question!";
