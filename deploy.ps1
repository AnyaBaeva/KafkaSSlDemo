# создать топики
docker exec -it kafka-0 kafka-topics --create --bootstrap-server kafka-0:9092 --command-config /etc/kafka/secrets/admin.properties --topic products --partitions 3 --replication-factor 3
docker exec -it kafka-0-destination kafka-topics --create --bootstrap-server kafka-0-destination:9092 --topic products --partitions 3 --replication-factor 1

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

# 5. Права consumer
docker exec -it kafka-0 kafka-acls `
   --bootstrap-server kafka-0:9092 `
   --command-config /etc/kafka/secrets/admin.properties `
   --add --allow-principal User:consumer `
   --operation Read --group consumer-group

docker exec -it kafka-0 kafka-acls `
   --bootstrap-server kafka-0:9092 `
   --command-config /etc/kafka/secrets/admin.properties `
   --add --allow-principal User:consumer `
   --operation ALL --topic products

# Создайте СХЕМУ для основных кластеров
$schemaRegistryUrl = "http://localhost:18081"
$subject = "products-value"

$schema = @'
{
"type": "record",
"name": "Product",
"namespace": "com.example.avro",
"fields": [
{"name": "product_id", "type": "string"},
{"name": "name", "type": "string"},
{"name": "description", "type": "string"},
{
"name": "price",
"type": {
"type": "record",
"name": "Price",
"fields": [
{"name": "amount", "type": "double"},
{"name": "currency", "type": "string"}
]
}
},
{"name": "category", "type": "string"},
{"name": "brand", "type": "string"},
{
"name": "stock",
"type": {
"type": "record",
"name": "Stock",
"fields": [
{"name": "available", "type": "int"},
{"name": "reserved", "type": "int"}
]
}
},
{"name": "sku", "type": "string"},
{
"name": "tags",
"type": {
"type": "array",
"items": "string"
}
},
{
"name": "images",
"type": {
"type": "array",
"items": {
"type": "record",
"name": "Image",
"fields": [
{"name": "url", "type": "string"},
{"name": "alt", "type": "string"}
]
}
}
},
{
"name": "specifications",
"type": {
"type": "record",
"name": "Specifications",
"fields": [
{"name": "weight", "type": "string"},
{"name": "dimensions", "type": "string"},
{"name": "battery_life", "type": "string"},
{"name": "water_resistance", "type": "string"}
]
}
},
{"name": "created_at", "type": "string"},
{"name": "updated_at", "type": "string"},
{"name": "index", "type": "string"},
{"name": "store_id", "type": "string"}
]
}
'@

$schemaData = @{
schema = $schema
} | ConvertTo-Json

$headers = @{
"Content-Type" = "application/vnd.schemaregistry.v1+json"
}

try {
$response = Invoke-RestMethod `
-Uri "$schemaRegistryUrl/subjects/$subject/versions" `
-Method Post `
-Body $schemaData `
-Headers $headers
Write-Output "Схема зарегистрирована. ID: $response"
}
catch {
Write-Output "Ошибка регистрации схемы: $($_.Exception.Message)"
}

#Создайте СХЕМУ для реплики
$schemaRegistryUrl = "http://localhost:18082"
$subject = "products-value"

$schema = @'
{
"type": "record",
"name": "Product",
"namespace": "com.example.avro",
"fields": [
{"name": "product_id", "type": "string"},
{"name": "name", "type": "string"},
{"name": "description", "type": "string"},
{
"name": "price",
"type": {
"type": "record",
"name": "Price",
"fields": [
{"name": "amount", "type": "double"},
{"name": "currency", "type": "string"}
]
}
},
{"name": "category", "type": "string"},
{"name": "brand", "type": "string"},
{
"name": "stock",
"type": {
"type": "record",
"name": "Stock",
"fields": [
{"name": "available", "type": "int"},
{"name": "reserved", "type": "int"}
]
}
},
{"name": "sku", "type": "string"},
{
"name": "tags",
"type": {
"type": "array",
"items": "string"
}
},
{
"name": "images",
"type": {
"type": "array",
"items": {
"type": "record",
"name": "Image",
"fields": [
{"name": "url", "type": "string"},
{"name": "alt", "type": "string"}
]
}
}
},
{
"name": "specifications",
"type": {
"type": "record",
"name": "Specifications",
"fields": [
{"name": "weight", "type": "string"},
{"name": "dimensions", "type": "string"},
{"name": "battery_life", "type": "string"},
{"name": "water_resistance", "type": "string"}
]
}
},
{"name": "created_at", "type": "string"},
{"name": "updated_at", "type": "string"},
{"name": "index", "type": "string"},
{"name": "store_id", "type": "string"}
]
}
'@

$schemaData = @{
schema = $schema
} | ConvertTo-Json

$headers = @{
"Content-Type" = "application/vnd.schemaregistry.v1+json"
}

try {
$response = Invoke-RestMethod `
-Uri "$schemaRegistryUrl/subjects/$subject/versions" `
-Method Post `
-Body $schemaData `
-Headers $headers
Write-Output "Схема зарегистрирована для реплики. ID: $response"
}
catch {
Write-Output "Ошибка регистрации схемы: $($_.Exception.Message)"
}

# Зарегистрируйте HDFS Sink Connector

# Удалим старый коннектор
#Invoke-RestMethod -Uri "http://localhost:18083/connectors/hdfs-sink-avro-connector" -Method Delete

# Подождем
#Start-Sleep -Seconds 5

# Создадим новый коннектор с правильными настройками
$connectorConfig = @{
    "name" = "hdfs-sink-avro-connector"
    "config" = @{
        "connector.class" = "io.confluent.connect.hdfs.HdfsSinkConnector"
        "tasks.max" = "1"
        "topics" = "products"
        "hdfs.url" = "hdfs://hadoop-namenode:9000"
        "hadoop.conf.dir" = "/etc/hadoop/conf"
        "hadoop.home" = "/opt/hadoop"
        "flush.size" = "3"
        "format.class" = "io.confluent.connect.hdfs.avro.AvroFormat"
        "key.converter" = "org.apache.kafka.connect.storage.StringConverter"
        "key.converter.schemas.enable" = "false"
        "value.converter" = "io.confluent.connect.avro.AvroConverter"
        "value.converter.schema.registry.url" = "http://schema-registry-destination:8081"
        "schema.compatibility" = "BACKWARD"
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

