# Итоговый проект
## Создайте топики

   a. После запуска контейнера проверяем, что топики еще не созданы (локальный 
   терминал):
   
   ```powershell
   docker exec -it kafka-0 bash -c "kafka-topics --bootstrap-server kafka-0:9092 --command-config /etc/kafka/secrets/admin.properties --list"
   ```
   
   Должны увидеть пустой вывод.
   
   b.1 Создаем топик:
   
   ```powershell
   docker exec -it kafka-0 kafka-topics --create --bootstrap-server kafka-0:9092 --command-config /etc/kafka/secrets/admin.properties --topic products --partitions 3 --replication-factor 3
   ```
   Вывод: Created topic products.

   b.2 Создаем топик в destination (команда не рабочая): если руками, то 3 партиции и одна реплика
```powershell
   docker exec -it kafka-0-destination kafka-topics --create --bootstrap-server kafka-0-destination:9092 --command-config /etc/kafka/secrets/admin.properties --topic products --partitions 3 --replication-factor 1 
   ```
   Вывод: Created topic products.
   
   c. Проверяем созданные топики:
   
   ```powershell
   docker exec -it kafka-0 bash -c "kafka-topics --bootstrap-server kafka-0:9092 --command-config /etc/kafka/secrets/admin.properties --list"
   ```
   Вывод:
   products.

## Создайте топики для destination аналогично source 

## ПРАВА
```
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
```

### consumer
```
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
```
## Создайте СХЕМУ для основных кластеров

```
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
```

## Создайте СХЕМУ для реплики

```
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
Write-Output "Схема зарегистрирована. ID: $response"
}
catch {
Write-Output "Ошибка регистрации схемы: $($_.Exception.Message)"
}
```


## Зеркалирование

### проверка
```
docker exec -it kafka-0 kafka-console-consumer --bootstrap-server localhost:9092 --topic topic-to-mirror --from-beginning
```

# Проверьте логи основных компонентов:
``` powershell
# Проверьте ZooKeeper
docker logs zookeeper-source
docker logs zookeeper-destination

# Проверьте Kafka брокеры
docker logs kafka-source
docker logs kafka-destination

# Проверьте MirrorMaker
docker logs mirror-maker
```

📊 Мониторинг:
Spark UI: http://localhost:8081

HDFS UI: http://localhost:9870

Проверка данных: hdfs dfs -ls /test_data

# HDFS - > Spark - > HDFS
запустите класс HDFSProcessing

# HDFS - > Kafka-destination
запустите класс HDFSToKafka


#SPARK ВРЕМЕННО ЗАКОММЕНТИРОВАН, НЕТ МЕСТА, КОНФИГУРАЦИЯ ВЕРНАЯ, НА КОД НЕДОСТАТОЧНО РАЗРЕШЕНИЙ


```
curl -X POST -H "Content-Type: application/json" --data @- http://localhost:8083/connectors << EOF
{
"name": "postgres-source-connector",
"config": {
"connector.class": "io.debezium.connector.postgresql.PostgresConnector",
"database.hostname": "postgres",
"database.port": "5432",
"database.user": "postgres-user",
"database.password": "postgres-pw",
"database.dbname": "customers",
"database.server.name": "postgres-server",
"plugin.name": "pgoutput",
"slot.name": "debezium",
"publication.name": "dbz_publication",
"table.include.list": "public.(.*)",
"tombstones.on.delete": "true",
"transforms": "unwrap",
"transforms.unwrap.type": "io.debezium.transforms.ExtractNewRecordState",
"transforms.unwrap.drop.tombstones": "false",
"transforms.unwrap.delete.handling.mode": "rewrite",
"key.converter": "io.confluent.connect.avro.AvroConverter",
"value.converter": "io.confluent.connect.avro.AvroConverter",
"key.converter.schema.registry.url": "http://schema-registry:8081",
"value.converter.schema.registry.url": "http://schema-registry:8081"
}
}
EOF
```

```
curl -X POST -H "Content-Type: application/json" --data @- http://localhost:8083/connectors << EOF
{
  "name": "postgres-sink-connector",
  "config": {
    "connector.class": "io.confluent.connect.jdbc.JdbcSinkConnector",
    "tasks.max": "1",
    "topics": "your-target-topic",
    "connection.url": "jdbc:postgresql://postgres:5432/customers",
    "connection.user": "postgres-user",
    "connection.password": "postgres-pw",
    "insert.mode": "upsert",
    "pk.mode": "record_key",
    "pk.fields": "id",
    "auto.create": "true",
    "auto.evolve": "true",
    "key.converter": "io.confluent.connect.avro.AvroConverter",
    "value.converter": "io.confluent.connect.avro.AvroConverter",
    "key.converter.schema.registry.url": "http://schema-registry:8081",
    "value.converter.schema.registry.url": "http://schema-registry:8081"
  }
}
EOF
```