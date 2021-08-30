function sendAnswer(answer, questionId) {
    var content = JSON.stringify({
        "answer": answer,
        "question_id": questionId
    });
    
    MigratoryDataClient.publish({
        "subject": ANSWERS_SUBJECT,
        "content": content,
        "closure": "closure"
    });

    showInfoAboutResult(WAITING_TEXT);
};

function createAnswerButton(answer, questionId) {
    var buttonElement = document.createElement("a");
    buttonElement.setAttribute("class", "button btn btn-primary btn-lg");
    buttonElement.setAttribute('style', 'cursor:pointer');
    buttonElement.setAttribute("questionId", questionId);
    buttonElement.textContent = answer;
    buttonElement.addEventListener('click', function() {
        sendAnswer(this.textContent, this.getAttribute('questionId'));
        document.querySelectorAll('a[questionId]').forEach(el => {
            el.classList.add("disabled");
        });
        this.classList.add("btn-danger");
    });

    return buttonElement;
}

function startQuestionTimeoutTimer() {
    var timeleft = 10;

    questionTimer = setInterval(function () {
        if (timeleft <= 0) {
            clearInterval(questionTimer);
            disableQuestionAnswers();
        }
        var procent = (10 - timeleft) * 10 + 10;
        var elQuestionTimeout = document.querySelector("#questionTimeout");
        elQuestionTimeout.setAttribute("aria-valuenow", procent);
        elQuestionTimeout.style.width = (procent + "%");
        elQuestionTimeout.innerHTML = timeleft;
        timeleft -= 1;
    }, 1000);
}

function stopQuestionTimeoutTimer() {
    var elQuestionTimeout = document.querySelector("#questionTimeout");
    elQuestionTimeout.setAttribute("aria-valuenow", 0);
    elQuestionTimeout.style.width = "0%";
    elQuestionTimeout.innerHTML = 10;

    if (questionTimer !== null) {
        clearInterval(questionTimer);
    }
}

function showQuestion(question) {
    var questionText = question.question;
    var questionPoints = question.points;

    document.querySelector('#show-question').textContent = questionText + " (" + questionPoints + " points)"; 
    document.querySelector('#show-result').textContent = "";


    var divEl = document.querySelector('#show-answers');
    divEl.innerHTML = "";

    for (var i = 0; i < question.answers.length; i++) {
        divEl.appendChild(createAnswerButton(question.answers[i], question.id));
    }
}

function disableQuestionAnswers() {
    document.querySelector('#show-result').textContent = "Waiting for the next question.";

    document.querySelectorAll('a[questionId]').forEach(el => {
        el.classList.add("disabled");
    });
}

function showInfoAboutResult(info) {

    stopQuestionTimeoutTimer();

    if (info !== null) {
        document.querySelector('#show-result').textContent = info;
    }
}

function getUserId() {
    var userId = localStorage.getItem('watch-and-play-user-id');
    if (userId == null) {
        userId = "Player-human-" + generateRandomId(3);
        localStorage.setItem('watch-and-play-user-id', userId);
    }
    return userId;
};

function generateRandomId(length) {
    var result = '';
    var characters = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
    var charactersLength = characters.length;
    for (var i = 0; i < length; i++) {
      result += characters.charAt(Math.floor(Math.random() * charactersLength));
    }
    return result;
}

// this function gets called when API is ready to use
function onYouTubePlayerAPIReady() {
    // create the global player from the specific iframe (#video)
    player = new YT.Player('video', {
        events: {
            // call this function when player is ready to use
            'onReady': onPlayerReady
        }
    });
}

function onPlayerReady(event) {
    videoPlayerReady = true;
    if (videoSeekSeconds != -1) {
        player.seekTo(videoSeekSeconds, true);
        player.playVideo();
    }
}