SHELL:=/bin/bash
DC_CMD := docker-compose exec
ZOOKEEPER_PORT := 2181
KAFKA_CONTAINER_NAME := kafka
KAFKA_PORT := 9092
KAFKA_BIN_DIR := /opt/kafka/bin
MYSQL_CMD := /usr/bin/mysql -usql-demo -pdemo-sql -Dsql-demo

args = `arg="$(filter-out $@,$(MAKECMDGOALS))" && echo $${arg:-${1}}`

.DEFAULT_GOAL := help
.PHONY: help up ps build exec down-all show-hostnames log

help:
	@grep -E '^[a-zA-Z/_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?##"}; {printf "\033[36m%-20s\033[0m %s\n", $$1, $$2}'

show-hostnames: ## You can see docker-compose hostnames
	@yq r docker-compose.yml --printMode p 'services.*'

up: ## docker-compose up [HOSTNAME]
	@docker-compose up -d $(call args, )

ps: ## docker-compose ps
	@docker-compose ps

build: ## docker-compose build
	docker-compose build

exec: ## exec [HOSTNAME] //docker-compose exec [HOSTNAME] /bin/bash
	@docker-compose exec $(call args, ) /bin/bash

down-all: ## docker-compose down -v
	@docker-compose down -v

log: ## log [HOSTNAME]
	@docker-compose logs $(call args, )

.PHONY: kafka/topic-list kafka/topic-describe kafka/topic-create kafka/topic-delete kafka/read kafka/write

#refs: https://kafka.apache.org/quickstart
kafka/topic-list: ## list kafka topics
	@$(DC_CMD) $(KAFKA_CONTAINER_NAME) $(KAFKA_BIN_DIR)/kafka-topics.sh --list --zookeeper zookeeper:$(ZOOKEEPER_PORT)

kafka/topic-describe: ## kafka/topic-describe TOPIC=[TOPIC_NAME]
	@$(DC_CMD) $(KAFKA_CONTAINER_NAME) $(KAFKA_BIN_DIR)/kafka-topics.sh --describe --zookeeper zookeeper:$(ZOOKEEPER_PORT) --topic $(call args, )

kafka/topic-create: ## kafka/topic-create [TOPIC_NAME]
	$(DC_CMD) $(KAFKA_CONTAINER_NAME) $(KAFKA_BIN_DIR)/kafka-topics.sh --create --zookeeper zookeeper:$(ZOOKEEPER_PORT) --topic $(call args, ) --partitions 1 --replication-factor 1

kafka/topic-delete: ## kafka/topic-delete [TOPIC_NAME]
	$(DC_CMD) $(KAFKA_CONTAINER_NAME) $(KAFKA_BIN_DIR)/kafka-topics.sh --delete --zookeeper zookeeper:$(ZOOKEEPER_PORT) --topic $(call args, )

kafka/read: ## kafka/read [TOPIC_NAME] //read events from specified topic
	@$(DC_CMD) $(KAFKA_CONTAINER_NAME) $(KAFKA_BIN_DIR)/kafka-console-consumer.sh --topic $(call args, ) --from-beginning --bootstrap-server $(KAFKA_CONTAINER_NAME):$(KAFKA_PORT)

kafka/write: ## kafka/write [TOPIC_NAME]
	$(DC_CMD) $(KAFKA_CONTAINER_NAME) $(KAFKA_BIN_DIR)/kafka-console-producer.sh --topic $(call args, ) --broker-list $(KAFKA_CONTAINER_NAME):$(KAFKA_PORT)

.PHONY: mysql/prompt mysql/show-tables

mysql/show-tables: ## show db tables
	@$(DC_CMD) mysql $(MYSQL_CMD) -e 'show tables'

mysql/prompt: ## start mysql prompt
	@$(DC_CMD) mysql $(MYSQL_CMD)

.PHONY: flink/job-list flink/sql-cli

flink/job-list: ## job list
	@$(DC_CMD) jobmanager flink list

flink/sql-cli: ## sql-cli
	@$(DC_CMD) sql-client ./sql-client.sh

.PHONY: zookeeper/broker-list

zookeeper/broker-list: ## broker list
	$(DC_CMD) zookeeper ./bin/zkCli.sh localhost:$(ZOOKEEPER_PORT) ls /brokers/ids

DYNAMO_ENDPOINT := http://localhost:8000
dynamodb/create-table: ## dynamodb/create-tables
	aws --endpoint-url $(DYNAMO_ENDPOINT) dynamodb create-table --cli-input-json file://dynamodb/scripts/create-tables.json

dynamodb/show-tables: ## dynamodb/show-tables
	aws --endpoint-url $(DYNAMO_ENDPOINT) dynamodb list-tables

dynamodb/scan-table: ## dynamodb/scan-table [TABLE_NAME]
	aws --endpoint-url $(DYNAMO_ENDPOINT) dynamodb scan --table-name $(call args, )

dynamodb/setup: ## dynamodb/setup
	aws --endpoint-url=$(DYNAMO_ENDPOINT) dynamodb create-table \
        --table-name journal \
        --attribute-definitions \
            AttributeName=par,AttributeType=S \
            AttributeName=num,AttributeType=N \
        --key-schema AttributeName=par,KeyType=HASH AttributeName=num,KeyType=RANGE \
        --provisioned-throughput ReadCapacityUnits=1000,WriteCapacityUnits=1000
	aws --endpoint-url=$(DYNAMO_ENDPOINT) dynamodb create-table \
        --table-name snapshot \
        --attribute-definitions \
            AttributeName=par,AttributeType=S \
            AttributeName=ts,AttributeType=N \
        --key-schema \
         	AttributeName=par,KeyType=HASH \
         	AttributeName=ts,KeyType=RANGE \
        --provisioned-throughput ReadCapacityUnits=1000,WriteCapacityUnits=1000 \
		--local-secondary-indexes \
        	IndexName=ts-idx,KeySchema=['{AttributeName=par,KeyType=HASH}','{AttributeName=ts,KeyType=RANGE}'],Projection={ProjectionType=ALL}

POST_CONTENT := -X POST -H "Content-Type: application/json"
BASE_URL := http://127.0.0.1:9000
GET_HTTP_STATUS := -o /dev/null -w '%{http_code}\n' -s

roomName = "default"
post/room: ## post/room
	curl $(POST_CONTENT) -d '{"name":"$(roomName)"}' $(BASE_URL)/room $(GET_HTTP_STATUS)

roomId =
post/message: ## post/message roomId=[ROOM_ID]
	curl $(POST_CONTENT) -d '{"roomId":$(roomId),"sender":1,"body":"hogehoge"}' $(BASE_URL)/message $(GET_HTTP_STATUS)

post/account: ## post/account
	curl $(POST_CONTENT) -d '{"name": "shimomu"}' $(BASE_URL)/account $(GET_HTTP_STATUS)
