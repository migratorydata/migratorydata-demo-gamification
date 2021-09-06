document.addEventListener('DOMContentLoaded', function() {
    // add event to Tabs
    document.querySelectorAll('input[type="radio"]').forEach(el => {
        el.addEventListener('click', function (event) {
              switch (event.target.id) {
                case "tab-play": {
                    break;
                }
                case "tab-leaderboard": {
                    // send the request to get the top players
                    MigratoryDataClient.publish({
                        subject: TOP_SERVICE_SUBJECT,
                        content: JSON.stringify({"user_id" :USER_ID})
                    });
                    break;
                }
            }
        });
    });


    // init the MigratoryData client
    MigratoryDataClient.setEntitlementToken(TOKEN);
    MigratoryDataClient.setServers(SERVERS);
    MigratoryDataClient.setStatusHandler(function (event) {
        console.log("Status : " + event.type + " : " + event.info);
    });
    MigratoryDataClient.setMessageHandler(function (message) {
        console.log(message);

        if (message.subject == LIVE_VIDEO_TIME_SUBJECT) {
            var seekObject = JSON.parse(message.content);
            videoSeekSeconds = seekObject.seek;

            if (videoPlayerReady) {
                player.seekTo(videoSeekSeconds, true);
                player.playVideo();
            }

            MigratoryDataClient.unsubscribe([LIVE_VIDEO_TIME_SUBJECT]);
            return;
        }

        if (message.type != MigratoryDataClient.MESSAGE_TYPE_UPDATE) {
            return;
        }

        var subject = message.subject;

        // display the newly received question
        if (subject == QUESTIONS_SUBJECT) {
            var questionObject = JSON.parse(message.content);

            if (questionObject.reset) {
                if (videoPlayerReady) {
                    player.seekTo(0, true);
                    player.playVideo();
                }
                document.querySelector('#show-question').textContent = "";
                document.querySelector('#show-result').textContent = questionObject.message;
                var divEl = document.querySelector('#show-answers');
                divEl.innerHTML = "";            
            } else {
                showQuestion(questionObject);

                stopQuestionTimeoutTimer();
                startQuestionTimeoutTimer();    
            }
        }

        // show the result 
        if (subject == RESULTS_SUBJECT) {
            var result = JSON.parse(message.content);

            if (parseInt(result.points) > 0) {
                showInfoAboutResult("You won " + result.points + " points, wait for the next question!");
            } else {
                showInfoAboutResult(INCORRECT_TEXT);
            }
            document.querySelectorAll('a[questionId]').forEach(el => {
                if (el.textContent == result.answer) {
                    el.classList.remove("btn-danger");
                    el.classList.add("btn-success");
                }
            });
        }

        // display the top users
        if (subject == TOP_USERS_SUBJECT) {
            var topPlayers = JSON.parse(message.content);

            var score = document.getElementById("total-score");
            score.innerHTML = "Your total score: " + topPlayers.score;

            var topScores = document.getElementById("top-scores");
            topScores.innerHTML = "";
            for (var i = 0; i < topPlayers.top.length; i++) {
                if (topPlayers.top[i].name == USER_ID) {
                    topScores.innerHTML += "<li class=\"list-group-item active\">" + "You" + " - " + topPlayers.top[i].score + "</li>";
                } else {
                    topScores.innerHTML += "<li class=\"list-group-item\">" + topPlayers.top[i].name + " - " + topPlayers.top[i].score + "</li>";
                }
            }
        }
    });
    MigratoryDataClient.connect();
    MigratoryDataClient.subscribe(SUBJECTS);
});