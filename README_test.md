# Проект модуля №6: "Безопасность в Kafka" (Python)

## Задание "Настройка защищённого соединения и управление доступом"

**Цели задания:** настроить защищённое SSL-соединение для кластера Apache 
Kafka из трёх брокеров с использованием Docker Compose, создать новый топик 
и протестировать отправку и получение зашифрованных сообщений.

**Задание:**
1. Создайте сертификаты для каждого брокера. 
2. Создайте Truststore и Keystore для каждого брокера.
3. Настройте дополнительные брокеры в режиме SSL. Ранее в курсе вы уже 
   работали с кластером Kafka, состоящим из трёх брокеров. Используйте
   имеющийся `docker-compose` кластера и настройте для него SSL. 
4. Создайте топики:
   * **topic-1**
   * **topic-2**
5. Настройте права доступа:
   * **topic-1**: доступен как для продюсеров, так и для консьюмеров.
   * **topic-2**: продюсеры могут отправлять сообщения; консьюмеры не имеют 
     доступа к чтению данных.
6. Реализуйте продюсера и консьюмера.
7. Проверьте права доступа.

## Решение

1. **Создайте сертификаты для каждого брокера.**

   a. Создаем файл конфигурации для корневого сертификата (Root CA) `ca.cnf`:
   
   ```
   [ policy_match ]
   countryName = match
   stateOrProvinceName = match
   organizationName = match
   organizationalUnitName = optional
   commonName = supplied
   emailAddress = optional
   
   [ req ]
   prompt = no
   distinguished_name = dn
   default_md = sha256
   default_bits = 4096
   x509_extensions = v3_ca
   
   [ dn ]
   countryName = RU
   organizationName = Yandex
   organizationalUnitName = Practice
   localityName = Moscow
   commonName = yandex-practice-kafka-ca
   
   [ v3_ca ]
   subjectKeyIdentifier = hash
   basicConstraints = critical,CA:true
   authorityKeyIdentifier = keyid:always,issuer:always
   keyUsage = critical,keyCertSign,cRLSign
   ```
   
   b. Создаем корневой сертификат - Root CA (локальный терминал):
   
   ```bash
   openssl req -new -nodes \
      -x509 \
      -days 365 \
      -newkey rsa:2048 \
      -keyout ca.key \
      -out ca.crt \
      -config ca.cnf
   ```
   
   c. Создаем файл для хранения сертификата безопасности `ca.pem` (локальный 
   терминал):
   
   ```bash
   cat ca.crt ca.key > ca.pem
   ```
   
   d. Создаем файлы конфигурации для каждого брокера:
   
      *  Для `kafka-0` создаем файл `kafka-0-creds/kafka-0.cnf`:
      
      ```bash
      [req]
      prompt = no
      distinguished_name = dn
      default_md = sha256
      default_bits = 4096
      req_extensions = v3_req
      
      [ dn ]
      countryName = RU
      organizationName = Yandex
      organizationalUnitName = Practice
      localityName = Moscow
      commonName = kafka-0
      
      [ v3_ca ]
      subjectKeyIdentifier = hash
      basicConstraints = critical,CA:true
      authorityKeyIdentifier = keyid:always,issuer:always
      keyUsage = critical,keyCertSign,cRLSign
      
      [ v3_req ]
      subjectKeyIdentifier = hash
      basicConstraints = CA:FALSE
      nsComment = "OpenSSL Generated Certificate"
      keyUsage = critical, digitalSignature, keyEncipherment
      extendedKeyUsage = serverAuth, clientAuth
      subjectAltName = @alt_names
      
      [ alt_names ]
      DNS.1 = kafka-0
      DNS.2 = kafka-0-external
      DNS.3 = localhost
      ```
      
      * Для `kafka-1` создаем файл `kafka-1-creds/kafka-1.cnf`:
      
      ```bash
      [req]
      prompt = no
      distinguished_name = dn
      default_md = sha256
      default_bits = 4096
      req_extensions = v3_req
      
      [ dn ]
      countryName = RU
      organizationName = Yandex
      organizationalUnitName = Practice
      localityName = Moscow
      commonName = kafka-1
      
      [ v3_ca ]
      subjectKeyIdentifier = hash
      basicConstraints = critical,CA:true
      authorityKeyIdentifier = keyid:always,issuer:always
      keyUsage = critical,keyCertSign,cRLSign
      
      [ v3_req ]
      subjectKeyIdentifier = hash
      basicConstraints = CA:FALSE
      nsComment = "OpenSSL Generated Certificate"
      keyUsage = critical, digitalSignature, keyEncipherment
      extendedKeyUsage = serverAuth, clientAuth
      subjectAltName = @alt_names
      
      [ alt_names ]
      DNS.1 = kafka-1
      DNS.2 = kafka-1-external
      DNS.3 = localhost
      ```
      
      * Для `kafka-2` создаем файл `kafka-2-creds/kafka-2.cnf`:
      
      ```bash
      [req]
      prompt = no
      distinguished_name = dn
      default_md = sha256
      default_bits = 4096
      req_extensions = v3_req
      
      [ dn ]
      countryName = RU
      organizationName = Yandex
      organizationalUnitName = Practice
      localityName = Moscow
      commonName = kafka-2
      
      [ v3_ca ]
      subjectKeyIdentifier = hash
      basicConstraints = critical,CA:true
      authorityKeyIdentifier = keyid:always,issuer:always
      keyUsage = critical,keyCertSign,cRLSign
      
      [ v3_req ]
      subjectKeyIdentifier = hash
      basicConstraints = CA:FALSE
      nsComment = "OpenSSL Generated Certificate"
      keyUsage = critical, digitalSignature, keyEncipherment
      extendedKeyUsage = serverAuth, clientAuth
      subjectAltName = @alt_names
      
      [ alt_names ]
      DNS.1 = kafka-2
      DNS.2 = kafka-2-external
      DNS.3 = localhost
      ```
   
   e. Создаем приватные ключи и запросы на сертификат - CSR (локальный терминал): 
   
   ```bash
   openssl req -new \
       -newkey rsa:2048 \
       -keyout kafka-0-creds/kafka-0.key \
       -out kafka-0-creds/kafka-0.csr \
       -config kafka-0-creds/kafka-0.cnf \
       -nodes
   
   openssl req -new \
       -newkey rsa:2048 \
       -keyout kafka-1-creds/kafka-1.key \
       -out kafka-1-creds/kafka-1.csr \
       -config kafka-1-creds/kafka-1.cnf \
       -nodes
   
   openssl req -new \
       -newkey rsa:2048 \
       -keyout kafka-2-creds/kafka-2.key \
       -out kafka-2-creds/kafka-2.csr \
       -config kafka-2-creds/kafka-2.cnf \
       -nodes
   ```
   
   f. Создаем сертификаты брокеров, подписанный CA (локальный терминал):
   
   ```bash
   openssl x509 -req \
       -days 3650 \
       -in kafka-0-creds/kafka-0.csr \
       -CA ca.crt \
       -CAkey ca.key \
       -CAcreateserial \
       -out kafka-0-creds/kafka-0.crt \
       -extfile kafka-0-creds/kafka-0.cnf \
       -extensions v3_req
   
   openssl x509 -req \
       -days 3650 \
       -in kafka-1-creds/kafka-1.csr \
       -CA ca.crt \
       -CAkey ca.key \
       -CAcreateserial \
       -out kafka-1-creds/kafka-1.crt \
       -extfile kafka-1-creds/kafka-1.cnf \
       -extensions v3_req
   
   openssl x509 -req \
       -days 3650 \
       -in kafka-2-creds/kafka-2.csr \
       -CA ca.crt \
       -CAkey ca.key \
       -CAcreateserial \
       -out kafka-2-creds/kafka-2.crt \
       -extfile kafka-2-creds/kafka-2.cnf \
       -extensions v3_req
   ```
   
   g. Создаем PKCS12-хранилища (локальный терминал):
   
   ```bash
   openssl pkcs12 -export \
       -in kafka-0-creds/kafka-0.crt \
       -inkey kafka-0-creds/kafka-0.key \
       -chain \
       -CAfile ca.pem \
       -name kafka-0 \
       -out kafka-0-creds/kafka-0.p12 \
       -password pass:your-password
   
   openssl pkcs12 -export \
       -in kafka-1-creds/kafka-1.crt \
       -inkey kafka-1-creds/kafka-1.key \
       -chain \
       -CAfile ca.pem \
       -name kafka-1 \
       -out kafka-1-creds/kafka-1.p12 \
       -password pass:your-password
   
   openssl pkcs12 -export \
       -in kafka-2-creds/kafka-2.crt \
       -inkey kafka-2-creds/kafka-2.key \
       -chain \
       -CAfile ca.pem \
       -name kafka-2 \
       -out kafka-2-creds/kafka-2.p12 \
       -password pass:your-password
   ```


2. **Создайте Truststore и Keystore для каждого брокера.**

   a. Начнем с создания Keystore (локальный терминал):
   
   ```bash
   keytool -importkeystore \
       -deststorepass your-password \
       -destkeystore kafka-0-creds/kafka.kafka-0.keystore.pkcs12 \
       -srckeystore kafka-0-creds/kafka-0.p12 \
       -deststoretype PKCS12  \
       -srcstoretype PKCS12 \
       -noprompt \
       -srcstorepass your-password
   
   keytool -importkeystore \
       -deststorepass your-password \
       -destkeystore kafka-1-creds/kafka.kafka-1.keystore.pkcs12 \
       -srckeystore kafka-1-creds/kafka-1.p12 \
       -deststoretype PKCS12  \
       -srcstoretype PKCS12 \
       -noprompt \
       -srcstorepass your-password
   
   keytool -importkeystore \
       -deststorepass your-password \
       -destkeystore kafka-2-creds/kafka.kafka-2.keystore.pkcs12 \
       -srckeystore kafka-2-creds/kafka-2.p12 \
       -deststoretype PKCS12  \
       -srcstoretype PKCS12 \
       -noprompt \
       -srcstorepass your-password
   ```
   
   b. Создаем Truststore для Kafka (локальный терминал):
   
   ```bash
   keytool -import \
       -file ca.crt \
       -alias ca \
       -keystore kafka-0-creds/kafka.kafka-0.truststore.jks \
       -storepass your-password \
       -noprompt
   
   keytool -import \
       -file ca.crt \
       -alias ca \
       -keystore kafka-1-creds/kafka.kafka-1.truststore.jks \
       -storepass your-password \
       -noprompt
   
   keytool -import \
       -file ca.crt \
       -alias ca \
       -keystore kafka-2-creds/kafka.kafka-2.truststore.jks \
       -storepass your-password \
       -noprompt
   ```
   
   c. Создаем файлы с паролями, которые указывали в предыдущих командах (локальный терминал):
   
   ```bash
   echo "your-password" > kafka-0-creds/kafka-0_sslkey_creds
   echo "your-password" > kafka-0-creds/kafka-0_keystore_creds
   echo "your-password" > kafka-0-creds/kafka-0_truststore_creds
   
   echo "your-password" > kafka-1-creds/kafka-1_sslkey_creds
   echo "your-password" > kafka-1-creds/kafka-1_keystore_creds
   echo "your-password" > kafka-1-creds/kafka-1_truststore_creds
   
   echo "your-password" > kafka-2-creds/kafka-2_sslkey_creds
   echo "your-password" > kafka-2-creds/kafka-2_keystore_creds
   echo "your-password" > kafka-2-creds/kafka-2_truststore_creds
   ```
   
   d. Импортируем PKCS12 в JKS (локальный терминал):
   
   ```bash
   keytool -importkeystore \
       -srckeystore kafka-0-creds/kafka-0.p12 \
       -srcstoretype PKCS12 \
       -destkeystore kafka-0-creds/kafka-0.keystore.jks \
       -deststoretype JKS \
       -deststorepass your-password
   
   keytool -importkeystore \
       -srckeystore kafka-1-creds/kafka-1.p12 \
       -srcstoretype PKCS12 \
       -destkeystore kafka-1-creds/kafka-1.keystore.jks \
       -deststoretype JKS \
       -deststorepass your-password
   
   keytool -importkeystore \
       -srckeystore kafka-2-creds/kafka-2.p12 \
       -srcstoretype PKCS12 \
       -destkeystore kafka-2-creds/kafka-2.keystore.jks \
       -deststoretype JKS \
       -deststorepass your-password
   ```
   
   e. Импортируем CA в Truststore (локальный терминал)::
   
   ```bash
   keytool -import -trustcacerts -file ca.crt \
       -keystore kafka-0-creds/kafka-0.truststore.jks \
       -storepass your-password -noprompt -alias ca
   
   keytool -import -trustcacerts -file ca.crt \
       -keystore kafka-1-creds/kafka-1.truststore.jks \
       -storepass your-password -noprompt -alias ca
   
   keytool -import -trustcacerts -file ca.crt \
       -keystore kafka-2-creds/kafka-2.truststore.jks \
       -storepass your-password -noprompt -alias ca
   ```
   
   f. Создаем конфигурацию для ZooKeeper (для аутентификации через SASL/PLAIN) в 
   файле `zookeeper.sasl.jaas.conf`:
   
   ```
   Server {
     org.apache.zookeeper.server.auth.DigestLoginModule required
     user_admin="your-password";
   };
   ```
   
   g. Создаем конфигурацию Kafka для авторизации в ZooKeeper в файле 
   `kafka_server_jaas.conf`:
   
   ```
   KafkaServer {
      org.apache.kafka.common.security.plain.PlainLoginModule required
      username="admin"
      password="your-password"
      user_admin="your-password"
      user_kafka="your-password"
      user_producer="your-password"
      user_consumer="your-password";
   };
   
   Client {
      org.apache.kafka.common.security.plain.PlainLoginModule required
      username="admin"
      password="your-password";
   };
   ```
   
   h. Добавим учетные записи клиента, создав файл `admin.properties`:
   
   ```
   security.protocol=SASL_SSL
   ssl.truststore.location=/etc/kafka/secrets/kafka.kafka-0.truststore.jks
   ssl.truststore.password=your-password
   sasl.mechanism=PLAIN
   sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required username="admin" password="your-password";
   ```

3. **Настройте дополнительные брокеры в режиме SSL.**

   Реализуем `docker-compose.yaml` (в нем также реализован запуск будущих 
   producer и consumer, поэтому лучше всего дождаться их реализации). Для 
   запуска используется команда:

   ```powershell
   docker compose up -d
   ```

4. **Создайте топики.**

   a. После запуска контейнера проверяем, что топики еще не созданы (локальный 
   терминал):
   
   ```powershell
   docker exec -it kafka-0 bash -c "kafka-topics --bootstrap-server kafka-0:9092 --command-config /etc/kafka/secrets/admin.properties --list"
   ```
   
   Должны увидеть пустой вывод.
   
   b. Создаем два новых топика:
   
   ```powershell
   docker exec -it kafka-0 kafka-topics --create --bootstrap-server kafka-0:9092 --command-config /etc/kafka/secrets/admin.properties --topic topic-1 --partitions 3 --replication-factor 3
   ```
   Вывод: Created topic topic-1.
   ```powershell
   docker exec -it kafka-0 kafka-topics --create --bootstrap-server kafka-0:9092 --command-config /etc/kafka/secrets/admin.properties --topic topic-2 --partitions 3 --replication-factor 3
   ```
   Вывод: Created topic topic-2.
   c. Проверяем созданные топики:
   
   ```powershell
   docker exec -it kafka-0 bash -c "kafka-topics --bootstrap-server kafka-0:9092 --command-config /etc/kafka/secrets/admin.properties --list"
   ```
   Вывод: 
   topic-1
   topic-2.

5. **Настройте права доступа. (для задачи по правам)**

   a. Настраиваем права доступа на запись для пользователя `producer` в топик 
   `topic-1` (локальный терминал):
   
   ```powershell
   docker exec -it kafka-0 kafka-acls `
   --bootstrap-server kafka-0:9092 `
   --command-config /etc/kafka/secrets/admin.properties `
   --add --allow-principal User:producer `
   --operation ALL --topic topic-1

   ```
   Вывод:
   Adding ACLs for resource `ResourcePattern(resourceType=TOPIC, name=topic-1, patternType=LITERAL)`:
   (principal=User:producer, host=*, operation=ALL, permissionType=ALLOW)

   Current ACLs for resource `ResourcePattern(resourceType=TOPIC, name=topic-1, patternType=LITERAL)`:
   (principal=User:producer, host=*, operation=ALL, permissionType=ALLOW)

   b. Настраиваем права доступа на чтение для пользователя `consumer` в топик 
   `topic-1` (локальный терминал):
   
   ```powershell
   docker exec -it kafka-0 kafka-acls `
   --bootstrap-server kafka-0:9092 `
   --command-config /etc/kafka/secrets/admin.properties `
   --add --allow-principal User:consumer `
   --operation Read --group consumer-group

   docker exec -it kafka-0 kafka-acls `
   --bootstrap-server kafka-0:9092 `
   --command-config /etc/kafka/secrets/admin.properties `
   --add --allow-principal User:consumer `
   --operation ALL --topic topic-1

   ```
   Вывод:
   Adding ACLs for resource `ResourcePattern(resourceType=GROUP, name=consumer-group, patternType=LITERAL)`:
   (principal=User:consumer, host=*, operation=READ, permissionType=ALLOW)

   Current ACLs for resource `ResourcePattern(resourceType=GROUP, name=consumer-group, patternType=LITERAL)`:
   (principal=User:consumer, host=*, operation=READ, permissionType=ALLOW)
   
   Adding ACLs for resource `ResourcePattern(resourceType=TOPIC, name=topic-1, patternType=LITERAL)`:
   (principal=User:consumer, host=*, operation=ALL, permissionType=ALLOW)

   Current ACLs for resource `ResourcePattern(resourceType=TOPIC, name=topic-1, patternType=LITERAL)`:
   (principal=User:producer, host=*, operation=ALL, permissionType=ALLOW)
   (principal=User:consumer, host=*, operation=ALL, permissionType=ALLOW)

   c. Настраиваем права доступа на запись для пользователя `producer` в топик 
   `topic-2` (локальный терминал):
   
   ```powershell
   docker exec -it kafka-0 kafka-acls `
   --bootstrap-server kafka-0:9092 `
   --command-config /etc/kafka/secrets/admin.properties `
   --add --allow-principal User:producer `
   --operation WRITE --topic topic-2
   ```
   Вывод: Adding ACLs for resource `ResourcePattern(resourceType=TOPIC, name=topic-2, patternType=LITERAL)`:
   (principal=User:producer, host=*, operation=WRITE, permissionType=ALLOW)

   Current ACLs for resource `ResourcePattern(resourceType=TOPIC, name=topic-2, patternType=LITERAL)`:
   (principal=User:producer, host=*, operation=WRITE, permissionType=ALLOW)

   b. Настраиваем права доступа на чтение для пользователя `consumer` в топик
      `topic-2` (локальный терминал):

   ```powershell
   docker exec -it kafka-0 kafka-acls `
   --bootstrap-server kafka-0:9092 `
   --command-config /etc/kafka/secrets/admin.properties `
   --add --deny-principal User:consumer `
   --operation READ --topic topic-2
   ```
   Вывод: Adding ACLs for resource `ResourcePattern(resourceType=TOPIC, name=topic-2, patternType=LITERAL)`:
   (principal=User:consumer, host=*, operation=READ, permissionType=DENY)
   
   Current ACLs for resource `ResourcePattern(resourceType=TOPIC, name=topic-2, patternType=LITERAL)`:
   (principal=User:consumer, host=*, operation=READ, permissionType=DENY)
   (principal=User:producer, host=*, operation=WRITE, permissionType=ALLOW)

#ПРАВА
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
# СХЕМА 
## (если не создан топик _SCHEMAS)
```
# Создаем топик для схем
docker exec kafka-0 kafka-topics `
--bootstrap-server kafka-0:9092 `
--create `
--topic _schemas `
--partitions 1 `
--replication-factor 3 `
--config cleanup.policy=compact `
--command-config /etc/kafka/secrets/admin.properties

# Проверим, что топик создан
docker exec kafka-0 kafka-topics `
--bootstrap-server kafka-0:9092 `
--list `
--command-config /etc/kafka/secrets/admin.properties | findstr "_schemas"
```
## создаем схему (тест)
```
# URL вашего Schema Registry
$schemaRegistryUrl = "http://localhost:18081"

# Имя subject (обычно: <topic-name>-value)
$subject = "topic-1-value"

# Схема Avro
$schema = @'
{
"type": "record",
"name": "User",
"namespace": "com.example.avro",
"fields": [
{"name": "id", "type": "int"},
{"name": "name", "type": "string"},
{"name": "email", "type": ["null", "string"], "default": null},
{"name": "age", "type": ["null", "int"], "default": null}
]
}
'@

# Подготовка данных для отправки
$schemaData = @{
schema = $schema
} | ConvertTo-Json

# Заголовки
$headers = @{
"Content-Type" = "application/vnd.schemaregistry.v1+json"
}

# Регистрируем схему
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
```
$schemaRegistryUrl = "http://localhost:18081"
$subject = "topic-1-value"

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


от команды PowerShell для перезапуска destination-кластера:

1. Остановка только destination-сервисов:
   powershell
# Остановка отдельных сервисов
docker-compose stop mirror-maker
docker-compose stop kafka-ui-destination
docker-compose stop kafka-0-destination
docker-compose stop kafka-1-destination
docker-compose stop kafka-2-destination
docker-compose stop zookeeper-destination

# Или остановка всех destination-сервисов одной командой
docker-compose stop mirror-maker kafka-ui-destination kafka-0-destination kafka-1-destination kafka-2-destination zookeeper-destination
2. Удаление контейнеров (опционально, для полного сброса):
   powershell
# Удаление контейнеров
docker-compose rm -f mirror-maker
docker-compose rm -f kafka-ui-destination
docker-compose rm -f kafka-0-destination
docker-compose rm -f kafka-1-destination
docker-compose rm -f kafka-2-destination
docker-compose rm -f zookeeper-destination

# Или удаление всех destination-контейнеров
docker-compose rm -f mirror-maker kafka-ui-destination kafka-0-destination kafka-1-destination kafka-2-destination zookeeper-destination
3. Запуск только destination-сервисов:
   powershell
# Запуск отдельных сервисов
docker-compose up -d zookeeper-destination
docker-compose up -d kafka-0-destination
docker-compose up -d kafka-1-destination
docker-compose up -d kafka-2-destination
docker-compose up -d kafka-ui-destination
docker-compose up -d mirror-maker

# Или запуск всех destination-сервисов
docker-compose up -d zookeeper-destination kafka-0-destination kafka-1-destination kafka-2-destination kafka-ui-destination mirror-maker
4. Команды для быстрого перезапуска:
   powershell
# Быстрый перезапуск (остановка + запуск)
docker-compose restart zookeeper-destination
docker-compose restart kafka-0-destination
docker-compose restart kafka-1-destination
docker-compose restart kafka-2-destination
docker-compose restart kafka-ui-destination
docker-compose restart mirror-maker

# Или перезапуск всех destination-сервисов
docker-compose restart zookeeper-destination kafka-0-destination kafka-1-destination kafka-2-destination kafka-ui-destination mirror-maker
5. Просмотр логов для проверки:
   powershell
# Просмотр логов отдельных сервисов
docker-compose logs -f kafka-0-destination
docker-compose logs -f zookeeper-destination
docker-compose logs -f mirror-maker

# Просмотр логов всех destination-сервисов
docker-compose logs -f zookeeper-destination kafka-0-destination kafka-1-destination kafka-2-destination mirror-maker
6. Проверка статуса:
   powershell
# Проверка статуса контейнеров
docker-compose ps | Select-String "destination"

# Или
docker ps --filter "name=destination"
7. Полный сброс и перезапуск:
   powershell
# Полная пересборка destination-сервисов
docker-compose up -d --force-recreate --build zookeeper-destination kafka-0-destination kafka-1-destination kafka-2-destination kafka-ui-destination mirror-maker

Команды для перезапуска и диагностики:
powershell
# Остановить и удалить mirror-maker
docker-compose stop mirror-maker
docker-compose rm -f mirror-maker

# Проверить доступность исходного кластера
docker exec kafka-0 kafka-topics --list --bootstrap-server kafka-0:9092 --command-config /etc/kafka/secrets/admin.properties

# Проверить доступность целевого кластера
docker exec kafka-0-destination kafka-topics --list --bootstrap-server localhost:9092

# Запустить только mirror-maker
docker-compose up -d mirror-maker

# Смотреть логи в реальном времени
docker-compose logs -f mirror-maker

Команды для перезапуска:
powershell
# Остановить и пересоздать mirror-maker
docker-compose stop mirror-maker
docker-compose rm -f mirror-maker
docker-compose up -d mirror-maker

# Проверить логи
docker-compose logs -f mirror-maker




#Вот команды для проверки работы HDFS в PowerShell:

1. Проверка статуса Hadoop сервисов
   powershell
# Проверить статус namenode
docker exec -it hadoop-namenode hdfs dfsadmin -report

# Проверить статус datanodes
docker exec -it hadoop-namenode hdfs dfsadmin -report | Select-String "Datanodes available:"

# Проверить общее состояние HDFS
docker exec -it hadoop-namenode hdfs fsck / -files -blocks
2. Проверка файловой системы HDFS
   powershell
# Посмотреть корневую директорию HDFS
docker exec -it hadoop-namenode hdfs dfs -ls /

# Посмотреть директорию topics (где Kafka Connect сохраняет данные)
docker exec -it hadoop-namenode hdfs dfs -ls /topics

# Посмотреть конкретную тему (например, products)
docker exec -it hadoop-namenode hdfs dfs -ls /topics/products

# Рекурсивный просмотр всех файлов в теме
docker exec -it hadoop-namenode hdfs dfs -ls -R /topics/products
3. Проверка содержимого файлов в HDFS
   powershell
# Посмотреть содержимое первого файла в теме products
$firstFile = docker exec -it hadoop-namenode hdfs dfs -ls /topics/products | Select-Object -First 1 | ForEach-Object { $_.Split()[-1] }
docker exec -it hadoop-namenode hdfs dfs -cat $firstFile

# Посмотреть первые 10 строк любого файла
docker exec -it hadoop-namenode hdfs dfs -cat /topics/products/* | Select-Object -First 10

# Посмотреть количество файлов в директории
docker exec -it hadoop-namenode hdfs dfs -count /topics/products
4. Мониторинг в реальном времени
   powershell
# Мониторить появление новых файлов (запустить в отдельном окне)
while ($true) {
$fileCount = docker exec -it hadoop-namenode hdfs dfs -count /topics/products | ForEach-Object { $_.Split()[1] }
Write-Host "[$(Get-Date -Format 'HH:mm:ss')] Files in HDFS: $fileCount"
Start-Sleep -Seconds 5
}



# Проверка доступности портов брокеров
Test-NetConnection -ComputerName localhost -Port 19092
Test-NetConnection -ComputerName localhost -Port 29092
Test-NetConnection -ComputerName localhost -Port 39092

# Альтернатива с telnet (если установлен)
telnet localhost 19092







1. Проверьте, занят ли порт
   Сначала убедитесь, что порт действительно свободен или узнайте, какой процесс его использует.

Команда:

powershell
netstat -ano | findstr :59093
Что это даст:

Если порт занят, вы увидите строку с состоянием LISTENING и PID (Process Identifier) процесса в последнем столбце.

Если вывод пустой, порт не занят обычным процессом, но может быть заблокирован системой или Hyper-V.

🚫 2. Завершите процесс, занимающий порт
Если netstat показал PID, можно попробовать завершить процесс.

Команда:

powershell
# Завершить процесс по PID (замените YOUR_PID на фактический номер)
taskkill /PID YOUR_PID /F

# Пример: taskkill /PID 1234 /F
Флаг /F означает принудительное завершение.

⚠️ Внимание: Будьте осторожны с завершением системных процессов. Если PID равен 4, это часто системный процесс System, и его завершение может привести к нестабильности системы.

🔧 3. Перезапустите службу Windows NAT Driver (winnat)
Очень часто в Windows порты блокируются или резервируются службой сетевого адреса (NAT), особенно после сна или гибернации. Это может помочь, даже если порт не отображается как занятый в netstat.

Команды (запускайте PowerShell от имени администратора):

powershell
net stop winnat
net start winnat
Что это делает: Эта команда останавливает и сразу перезапускает службу NAT, что может освободить незаконно занятые порты.

🎯 Важно: Это часто бывает самым эффективным решением такой проблемы на Windows.

📝 4. Проверьте и измените исключённые диапазоны портов
Иногда Windows или Hyper-V резервируют (исключают) целые диапазоны портов для своих нужд. Ваш порт может попасть в такой диапазон.

Команда для просмотра исключённых диапазонов:

powershell
netsh int ipv4 show excludedportrange protocol=tcp
Что делать: Если порт 59093 попадает в один из исключённых диапазонов, вы можете попробовать изменить динамический диапазон портов.

Команда для изменения динамического диапазона портов (требует перезагрузки):

powershell
netsh int ipv4 set dynamic tcp start=49152 num=16384
netsh int ipv6 set dynamic tcp start=49152 num=16384
Это установит стандартный динамический диапазон портов (49152-65535). После выполнения команд перезагрузите компьютер.

🔄 5. Простые альтернативы
Если контейнер нужно запустить быстро, а возиться с поиском причины нет времени:

Перезагрузите компьютер: Это может очистить временные блокировки портов и перезапустить сетевые службы.

Измените порт в команде Docker: Просто маппируйте другой порт хоста на порт 59093 в контейнере.

bash
docker run -p 59094:59093 your_image_name
💎 Краткий алгоритм решения
Запустите PowerShell от имени Администратора.

Проверьте порт: netstat -ano | findstr :59093

Если есть PID (и это не системный процесс) -> завершите его taskkill /PID <YOUR_PID> /F.

Если PID нет или это системный процесс -> перезапустите службу winnat (net stop winnat + net start winnat).

Если не помогло -> проверьте excludedportrange и, при необходимости, измените dynamicportrange и перезагрузитесь.

Если всё ещё не работает -> используйте другой порт для маппирования в Docker.


порт хадуп

PS C:\projects\KafkaShop> docker inspect hadoop-namenode | Select-String "IPAddress"

            "SecondaryIPAddresses": null,
            "IPAddress": "",
                    "IPAddress": "172.19.0.12",
