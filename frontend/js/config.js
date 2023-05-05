var USER_ID = getUserId();

var TOKEN = "some-token";

var SERVERS = [
    "http://localhost:8800",
];

var QUESTIONS_SUBJECT = "/admin/gamification/question";
var RESULTS_SUBJECT = "/admin/gamification/result/" + USER_ID;
var ANSWERS_SUBJECT = "/admin/gamification/answer";
var TOP_USERS_SUBJECT = "/admin/gamification/top/" + USER_ID;
var TOP_SERVICE_SUBJECT = "/admin/gamification/gettop"
var LIVE_VIDEO_TIME_SUBJECT = "/admin/gamification/live/time";

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
