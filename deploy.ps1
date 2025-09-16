# # Перейти в папку, где лежит файл docker-compose
# cd C:\projects\KafkaSSlDemo
# # Запустить установку
# docker-compose up -d
# # Подождать запуска всех сервисов
# Start-Sleep -Seconds 120
# Создать топики
docker exec -it kafka-0 kafka-topics --create --bootstrap-server kafka-0:9092 --command-config /etc/kafka/secrets/admin.properties --topic inputJsonStream --partitions 3 --replication-factor 3
docker exec -it kafka-0 kafka-topics --create --bootstrap-server kafka-0:9092 --command-config /etc/kafka/secrets/admin.properties --topic products --partitions 3 --replication-factor 3
docker exec -it kafka-0 kafka-topics --create --bootstrap-server kafka-0:9092 --command-config /etc/kafka/secrets/admin.properties --topic blockedProducts --partitions 3 --replication-factor 3
docker exec -it kafka-0 kafka-topics --create --bootstrap-server kafka-0:9092 --command-config /etc/kafka/secrets/admin.properties --topic response --partitions 3 --replication-factor 3
docker exec -it kafka-0 kafka-topics --create --bootstrap-server kafka-0:9092 --command-config /etc/kafka/secrets/admin.properties --topic userQuery --partitions 3 --replication-factor 3
docker exec -it kafka-0-destination kafka-topics --create --bootstrap-server kafka-0-destination:9092 --topic inputJsonStream --partitions 3 --replication-factor 1
docker exec -it kafka-0-destination kafka-topics --create --bootstrap-server kafka-0-destination:9092 --topic products --partitions 3 --replication-factor 1
docker exec -it kafka-0-destination kafka-topics --create --bootstrap-server kafka-0-destination:9092 --topic blockedProducts --partitions 3 --replication-factor 1
docker exec -it kafka-0-destination kafka-topics --create --bootstrap-server kafka-0-destination:9092 --topic response --partitions 3 --replication-factor 1
docker exec -it kafka-0-destination kafka-topics --create --bootstrap-server kafka-0-destination:9092 --topic userQuery --partitions 3 --replication-factor 1
# Выдать права
# 1. Права на создание и управление всеми топиками
docker exec kafka-0 kafka-acls `
--bootstrap-server kafka-0:9092 `
--add `
--allow-principal User:admin `
--operation All `
--topic '*' `
--command-config /etc/kafka/secrets/admin.properties

# 2. Права на работу с группами потребителей
docker exec kafka-0 kafka-acls `
--bootstrap-server kafka-0:9092 `
--add `
--allow-principal User:admin `
--operation All `
--group '*' `
--command-config /etc/kafka/secrets/admin.properties

# 3. Права на управление кластером
docker exec kafka-0 kafka-acls `
--bootstrap-server kafka-0:9092 `
--add `
--allow-principal User:admin `
--operation All `
--cluster `
--command-config /etc/kafka/secrets/admin.properties

# 4. Права на доступ к транзакциям
docker exec kafka-0 kafka-acls `
--bootstrap-server kafka-0:9092 `
--add `
--allow-principal User:admin `
--operation All `
--transactional-id '*' `
--command-config /etc/kafka/secrets/admin.properties

# Права для consumer
docker exec -it kafka-0 kafka-acls `
   --bootstrap-server kafka-0:9092 `
   --command-config /etc/kafka/secrets/admin.properties `
   --add --allow-principal User:consumer `
   --operation Read --topic products

docker exec -it kafka-0 kafka-acls `
   --bootstrap-server kafka-0:9092 `
   --command-config /etc/kafka/secrets/admin.properties `
   --add --allow-principal User:consumer `
   --operation Read --group consumer-group

docker exec -it kafka-0 kafka-acls `
   --bootstrap-server kafka-0:9092 `
   --command-config /etc/kafka/secrets/admin.properties `
   --add --allow-principal User:consumer `
   --operation Describe --topic products

docker exec -it kafka-0 kafka-acls `
   --bootstrap-server kafka-0:9092 `
   --command-config /etc/kafka/secrets/admin.properties `
   --add --allow-principal User:consumer `
   --operation Describe --group consumer-group

docker exec -it kafka-0 kafka-acls `
   --bootstrap-server kafka-0:9092 `
   --command-config /etc/kafka/secrets/admin.properties `
   --add --allow-principal User:consumer `
   --operation Read --transactional-id "*"

# Права для admin
docker exec -it kafka-0 kafka-acls `
   --bootstrap-server kafka-0:9092 `
   --command-config /etc/kafka/secrets/admin.properties `
   --add --allow-principal User:admin `
   --operation All --group connect-file-sink-products

docker exec -it kafka-0 kafka-acls `
   --bootstrap-server kafka-0:9092 `
   --command-config /etc/kafka/secrets/admin.properties `
   --add --allow-principal User:admin `
   --operation All --topic products

docker exec -it kafka-0 kafka-acls `
   --bootstrap-server kafka-0:9092 `
   --command-config /etc/kafka/secrets/admin.properties `
   --add --allow-principal User:admin `
   --operation All --transactional-id "*"

# Права для connect
docker exec -it kafka-0 kafka-acls `
   --bootstrap-server kafka-0:9092 `
   --command-config /etc/kafka/secrets/admin.properties `
   --add --allow-principal User:connect `
   --operation All --group connect-file-sink-products

docker exec -it kafka-0 kafka-acls `
   --bootstrap-server kafka-0:9092 `
   --command-config /etc/kafka/secrets/admin.properties `
   --add --allow-principal User:connect `
   --operation All --topic products

# Права для группы на всех брокерах
docker exec -it kafka-0 kafka-acls `
   --bootstrap-server kafka-0:9092 `
   --command-config /etc/kafka/secrets/admin.properties `
   --add --allow-principal User:admin `
   --operation All --group file-sink-group

# Регистрация HDFS Sink Connector JsonFormat
try {
    $response = Invoke-RestMethod -Uri "http://localhost:18083/connectors/hdfs-sink-json-connector" -Method Delete
    Write-Host "Старый коннектор удален"
} catch {
    Write-Host "Ошибка удаления коннектора (возможно его нет): $($_.Exception.Message)"
}

$connectorConfig = @{
    "name" = "hdfs-sink-json-connector"
    "config" = @{
        "connector.class" = "io.confluent.connect.hdfs3.Hdfs3SinkConnector"
        "tasks.max" = "1"
        "topics" = "products"
        "hdfs.url" = "hdfs://namenode:9000"
        "flush.size" = "1000"

        "format.class" = "io.confluent.connect.hdfs3.json.JsonFormat"
        "key.converter" = "org.apache.kafka.connect.storage.StringConverter"
        "key.converter.schemas.enable" = "false"
        "value.converter" = "org.apache.kafka.connect.json.JsonConverter"
        "value.converter.schemas.enable" = "false"

        "confluent.topic.bootstrap.servers" = "PLAINTEXT://kafka-0-destination:9092,PLAINTEXT://kafka-1-destination:9092,PLAINTEXT://kafka-2-destination:9092"
        "schema.compatibility" = "NONE"
        "errors.tolerance" = "all"
        "errors.log.enable" = "true"
        "errors.log.include.messages" = "true"
        "hdfs.authentication.kerberos" = "false"
        "topics.dir" = "/data"
        "logs.dir" = "/logs"

        "rotate.interval.ms" = "3600000"
    }
} | ConvertTo-Json -Depth 10

# Отправка конфигурации в Kafka Connect REST API
Invoke-RestMethod -Uri "http://localhost:18083/connectors/" -Method Post -ContentType "application/json" -Body $connectorConfig

# # Проверка существующих директорий
# docker exec hadoop-namenode hdfs dfs -ls /
# #Если вы видите директории /data и /logs, то ничего создавать не нужно!
#
# # Правильные команды (если действительно нужно)
# docker exec hadoop-namenode hdfs dfs -mkdir -p /data
# docker exec hadoop-namenode hdfs dfs -mkdir -p /logs
# docker exec hadoop-namenode hdfs dfs -chmod 755 /data
# docker exec hadoop-namenode hdfs dfs -chmod 755 /logs

# права, если надо
# docker exec hadoop-namenode hdfs dfs -chmod -R 777 /

# Создать коннектор FileStreamSink
try {
    $response = Invoke-RestMethod -Uri "http://localhost:18083/connectors/file-sink-products-final" -Method Delete
    Write-Host "Старый коннектор удален"
} catch {
    Write-Host "Ошибка удаления коннектора (возможно его нет): $($_.Exception.Message)"
}


$body = @{
    "name" = "file-sink-products-final"
    "config" = @{
        "connector.class" = "FileStreamSink"
        "tasks.max" = "1"
        "topics" = "products"
        "file" = "/data/output/products-final.json"

        "key.converter" = "org.apache.kafka.connect.storage.StringConverter"
        "value.converter" = "org.apache.kafka.connect.json.JsonConverter"
        "value.converter.schemas.enable" = "false"

        "consumer.override.bootstrap.servers" = "kafka-0:9092,kafka-1:9092,kafka-2:9092"
        "consumer.override.security.protocol" = "SASL_SSL"
        "consumer.override.sasl.mechanism" = "PLAIN"
        "consumer.override.sasl.jaas.config" = "org.apache.kafka.common.security.plain.PlainLoginModule required username=`"admin`" password=`"your-password`";"
        "consumer.override.ssl.truststore.location" = "/etc/kafka/secrets/kafka-0.truststore.jks"
        "consumer.override.ssl.truststore.password" = "your-password"
        "consumer.override.ssl.endpoint.identification.algorithm" = "HTTPS"

        "consumer.override.auto.offset.reset" = "earliest"
    }
} | ConvertTo-Json -Depth 10

Invoke-RestMethod -Uri "http://localhost:18083/connectors" -Method Post `
    -ContentType "application/json" `
    -Body $body

