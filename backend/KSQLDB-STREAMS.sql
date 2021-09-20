CREATE STREAM input_question (ID VARCHAR, question VARCHAR, answers ARRAY<VARCHAR>, answer VARCHAR, points INT)
    WITH (KAFKA_TOPIC='input_question',
          VALUE_FORMAT='JSON',
          PARTITIONS=8);

(broadcast clients)
CREATE STREAM question
    WITH (KAFKA_TOPIC='question',
          VALUE_FORMAT='JSON',
          PARTITIONS=8) AS
	SELECT
		input_question.id as `id`,
		input_question.question as `question`,
		input_question.answers as `answers`,
		input_question.points as `points`
	FROM input_question;


(receive from each client)
CREATE STREAM answer (ID VARCHAR KEY, answer VARCHAR, question_id VARCHAR)
    WITH (KAFKA_TOPIC='answer',
          VALUE_FORMAT='JSON',
          PARTITIONS=8);



(send to each client with 0 or number of points won)
CREATE STREAM result
    WITH (KAFKA_TOPIC='result',
          VALUE_FORMAT='JSON',
          PARTITIONS=8) AS
SELECT answer.id AS `user_id`,
	   input_question.id AS `question_id`,
	   input_question.answer AS `answer`,
	   case
			WHEN input_question.answer = answer.answer THEN input_question.points
			ELSE 0
	   END AS `points`
FROM input_question
INNER JOIN answer
WITHIN 20 SECONDS
ON input_question.id = answer.question_id
WHERE ((answer.rowtime - input_question.rowtime) / 1000 / 60) < 20
PARTITION BY answer.id;