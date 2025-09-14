# создать топики
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

docker exec -it kafka-1 kafka-acls `
   --bootstrap-server kafka-1:9092 `
   --command-config /etc/kafka/secrets/admin.properties `
   --add --allow-principal User:admin `
   --operation All --group file-sink-group

docker exec -it kafka-2 kafka-acls `
   --bootstrap-server kafka-2:9092 `
   --command-config /etc/kafka/secrets/admin.properties `
   --add --allow-principal User:admin `
   --operation All --group file-sink-group

# # Создайте СХЕМУ для основных кластеров
# $schemaRegistryUrl = "http://localhost:18081"
# $subject = "products-value"

# $schema = @'
# {
# "type": "record",
# "name": "Product",
# "namespace": "com.example.avro",
# "fields": [
# {"name": "product_id", "type": "string"},
# {"name": "name", "type": "string"},
# {"name": "description", "type": "string"},
# {
# "name": "price",
# "type": {
# "type": "record",
# "name": "Price",
# "fields": [
# {"name": "amount", "type": "double"},
# {"name": "currency", "type": "string"}
# ]
# }
# },
# {"name": "category", "type": "string"},
# {"name": "brand", "type": "string"},
# {
# "name": "stock",
# "type": {
# "type": "record",
# "name": "Stock",
# "fields": [
# {"name": "available", "type": "int"},
# {"name": "reserved", "type": "int"}
# ]
# }
# },
# {"name": "sku", "type": "string"},
# {
# "name": "tags",
# "type": {
# "type": "array",
# "items": "string"
# }
# },
# {
# "name": "images",
# "type": {
# "type": "array",
# "items": {
# "type": "record",
# "name": "Image",
# "fields": [
# {"name": "url", "type": "string"},
# {"name": "alt", "type": "string"}
# ]
# }
# }
# },
# {
# "name": "specifications",
# "type": {
# "type": "record",
# "name": "Specifications",
# "fields": [
# {"name": "weight", "type": "string"},
# {"name": "dimensions", "type": "string"},
# {"name": "battery_life", "type": "string"},
# {"name": "water_resistance", "type": "string"}
# ]
# }
# },
# {"name": "created_at", "type": "string"},
# {"name": "updated_at", "type": "string"},
# {"name": "index", "type": "string"},
# {"name": "store_id", "type": "string"}
# ]
# }
# '@
#
# $schemaData = @{
# schema = $schema
# } | ConvertTo-Json
#
# $headers = @{
# "Content-Type" = "application/vnd.schemaregistry.v1+json"
# }
#
# try {
# $response = Invoke-RestMethod `
# -Uri "$schemaRegistryUrl/subjects/$subject/versions" `
# -Method Post `
# -Body $schemaData `
# -Headers $headers
# Write-Output "Схема зарегистрирована. ID: $response"
# }
# catch {
# Write-Output "Ошибка регистрации схемы: $($_.Exception.Message)"
# }

# #Создайте СХЕМУ для реплики
# $schemaRegistryUrl = "http://localhost:18082"
# $subject = "products-value"
#
# $schema = @'
# {
# "type": "record",
# "name": "Product",
# "namespace": "com.example.avro",
# "fields": [
# {"name": "product_id", "type": "string"},
# {"name": "name", "type": "string"},
# {"name": "description", "type": "string"},
# {
# "name": "price",
# "type": {
# "type": "record",
# "name": "Price",
# "fields": [
# {"name": "amount", "type": "double"},
# {"name": "currency", "type": "string"}
# ]
# }
# },
# {"name": "category", "type": "string"},
# {"name": "brand", "type": "string"},
# {
# "name": "stock",
# "type": {
# "type": "record",
# "name": "Stock",
# "fields": [
# {"name": "available", "type": "int"},
# {"name": "reserved", "type": "int"}
# ]
# }
# },
# {"name": "sku", "type": "string"},
# {
# "name": "tags",
# "type": {
# "type": "array",
# "items": "string"
# }
# },
# {
# "name": "images",
# "type": {
# "type": "array",
# "items": {
# "type": "record",
# "name": "Image",
# "fields": [
# {"name": "url", "type": "string"},
# {"name": "alt", "type": "string"}
# ]
# }
# }
# },
# {
# "name": "specifications",
# "type": {
# "type": "record",
# "name": "Specifications",
# "fields": [
# {"name": "weight", "type": "string"},
# {"name": "dimensions", "type": "string"},
# {"name": "battery_life", "type": "string"},
# {"name": "water_resistance", "type": "string"}
# ]
# }
# },
# {"name": "created_at", "type": "string"},
# {"name": "updated_at", "type": "string"},
# {"name": "index", "type": "string"},
# {"name": "store_id", "type": "string"}
# ]
# }
# '@
#
# $schemaData = @{
# schema = $schema
# } | ConvertTo-Json
#
# $headers = @{
# "Content-Type" = "application/vnd.schemaregistry.v1+json"
# }
#
# try {
# $response = Invoke-RestMethod `
# -Uri "$schemaRegistryUrl/subjects/$subject/versions" `
# -Method Post `
# -Body $schemaData `
# -Headers $headers
# Write-Output "Схема зарегистрирована для реплики. ID: $response"
# }
# catch {
# Write-Output "Ошибка регистрации схемы: $($_.Exception.Message)"
# }

# Зарегистрируйте HDFS Sink Connector AvroFormat
# $connectorConfig = @{
#     "name" = "hdfs-sink-avro-connector"
#     "config" = @{
#         "connector.class" = "io.confluent.connect.hdfs3.Hdfs3SinkConnector"
#         "tasks.max" = "1"
#         "topics" = "products"
#         "hdfs.url" = "hdfs://hadoop-namenode:9000"
#         "hadoop.conf.dir" = "/etc/hadoop/conf"
#         "flush.size" = "3"
#
#         "format.class" = "io.confluent.connect.hdfs3.avro.AvroFormat"
#
#         "key.converter" = "org.apache.kafka.connect.storage.StringConverter"
#         "key.converter.schemas.enable" = "false"
#         "value.converter" = "io.confluent.connect.avro.AvroConverter"
#         "value.converter.schema.registry.url" = "http://schema-registry-destination:8081"
#         "confluent.topic.bootstrap.servers" = "PLAINTEXT://kafka-0-destination:9092,PLAINTEXT://kafka-1-destination:9092,PLAINTEXT://kafka-2-destination:9092"
#         "schema.compatibility" = "BACKWARD"
#         "errors.tolerance" = "all"
#         "errors.log.enable" = "true"
#         "errors.log.include.messages" = "true"
#         "hdfs.authentication.kerberos" = "false"
#         "topics.dir" = "/data"
#         "logs.dir" = "/logs"
#     }
# } | ConvertTo-Json -Depth 10
#
# Invoke-RestMethod -Uri "http://localhost:18083/connectors/" -Method Post -ContentType "application/json" -Body $connectorConfig

# Зарегистрируйте HDFS Sink Connector JsonFormat
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
        "hdfs.url" = "hdfs://hadoop-namenode:9000"
        "hadoop.conf.dir" = "/etc/hadoop/conf"
        "flush.size" = "3"

        "format.class" = "io.confluent.connect.hdfs3.json.JsonFormat"
        "key.converter" = "org.apache.kafka.connect.storage.StringConverter"
        "key.converter.schemas.enable" = "false"
        "value.converter" = "org.apache.kafka.connect.json.JsonConverter"
        "value.converter.schemas.enable" = "false"

        "confluent.topic.bootstrap.servers" = "PLAINTEXT://kafka-0-destination:9092,PLAINTEXT://kafka-1-destination:9092,PLAINTEXT://kafka-2-destination:9092"
        "schema.compatibility" = "NONE"  # Для JSON обычно не используется
        "errors.tolerance" = "all"
        "errors.log.enable" = "true"
        "errors.log.include.messages" = "true"
        "hdfs.authentication.kerberos" = "false"
        "topics.dir" = "/data"
        "logs.dir" = "/logs"
    }
} | ConvertTo-Json -Depth 10

Invoke-RestMethod -Uri "http://localhost:18083/connectors/" -Method Post -ContentType "application/json" -Body $connectorConfig

# Создадим директории с правильными правами
docker exec hadoop-namenode hdfs dfs -mkdir -p /data
docker exec hadoop-namenode hdfs dfs -mkdir -p /logs
docker exec hadoop-namenode hdfs dfs -chmod -R 777 /data /logs

# Проверим
docker exec hadoop-namenode hdfs dfs -ls -R /

# Зарегистрируйте JdbcSinkConnector
#
# $jsonConfig = @'
# {
#   "name": "jdbc-postgres-products-sink",
#   "config": {
#     "connector.class": "io.confluent.connect.jdbc.JdbcSinkConnector",
#     "tasks.max": "1",
#     "topics": "products",
#     "connection.url": "jdbc:postgresql://postgres:5432/shop",
#     "connection.user": "postgres-user",
#     "connection.password": "postgres-pw",
#     "auto.create": "true",
#     "insert.mode": "upsert",
#     "pk.mode": "record_value",
#     "pk.fields": "id",
#     "table.name.format": "products",
#
#     # Конвертеры для Avro данных основного кластера
#     "key.converter": "org.apache.kafka.connect.storage.StringConverter",
#     "value.converter": "io.confluent.connect.avro.AvroConverter",
#     "value.converter.schema.registry.url": "http://schema-registry:8081",
#     "value.converter.schema.registry.basic.auth.credentials.source": "USER_INFO",
#     "value.converter.schema.registry.basic.auth.user.info": "admin:your-password",
#
#     # Настройки безопасности основного кластера (SASL_SSL)
#     "producer.security.protocol": "SASL_SSL",
#     "producer.sasl.mechanism": "PLAIN",
#     "producer.sasl.jaas.config": "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"admin\" password=\"your-password\";",
#     "producer.ssl.truststore.location": "/etc/kafka/secrets/kafka-0.truststore.jks",
#     "producer.ssl.truststore.password": "your-password",
#
#     # Дополнительные настройки
#     "errors.tolerance": "all",
#     "errors.log.enable": "true",
#     "errors.log.include.messages": "true"
#   }
# }
# '@
#
# # Регистрируем на kafka-connect-files (который работает с основным кластером)
# Invoke-RestMethod -Uri "http://localhost:28083/connectors" `
#   -Method Post `
#   -ContentType "application/json" `
#   -Body $jsonConfig
# # Проверка статуса
# Invoke-RestMethod -Uri "http://localhost:18083/connectors/jdbc-postgres-products-sink/status"

# Создадим коннектор FileStreamSink
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