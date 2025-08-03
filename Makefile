docker exec -it kafka-0 kafka-topics --create --bootstrap-server kafka-0:9092 --command-config /etc/kafka/secrets/admin.properties --topic topic-1 --partitions 3 --replication-factor 3
docker exec -it kafka-0 kafka-topics --create --bootstrap-server kafka-0:9092 --command-config /etc/kafka/secrets/admin.properties --topic topic-2 --partitions 3 --replication-factor 3
# Настройка ACL
# Для topic-1 (разрешить всё)
docker exec -it kafka-0 kafka-acls `
  --bootstrap-server kafka-0:9092 `
  --command-config /etc/kafka/secrets/admin.properties `
  --add --allow-principal User:consumer `
  --operation ALL --topic topic-1

# Для topic-2 (только запись)
docker exec -it kafka-0 kafka-acls `
  --bootstrap-server kafka-0:9092 `
  --command-config /etc/kafka/secrets/admin.properties `
  --add --allow-principal User:consumer `
  --operation WRITE --topic topic-2

# Явный запрет чтения
docker exec -it kafka-0 kafka-acls `
  --bootstrap-server kafka-0:9092 `
  --command-config /etc/kafka/secrets/admin.properties `
  --add --deny-principal User:consumer `
  --operation READ --topic topic-2