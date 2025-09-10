package com.example;

import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

  public class ProductConsumer {

    private static Properties getConsumerConfig() {
      Properties props = new Properties();
      props.put("bootstrap.servers", "localhost:19092");
      props.put("group.id", "consumer-group");
      props.put("enable.auto.commit", "true");
      props.put("auto.commit.interval.ms", "1000");
      props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, "45000"); // Увеличено с 30000
      props.put(ConsumerConfig.REQUEST_TIMEOUT_MS_CONFIG, "50000");  // Увеличено с 30000
      props.put("security.protocol", "SASL_SSL");

      // SASL configuration
      props.put("sasl.mechanism", "PLAIN");
      // Альтернативный вариант с проверкой:
      String jaasConfig = "org.apache.kafka.common.security.plain.PlainLoginModule required "
          + "username=\"consumer\" "
          + "password=\"your-password\";";
      props.put("sasl.jaas.config", jaasConfig);

      // SSL configuration
      props.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG,
          "./kafka-0-creds/kafka-0.truststore.jks");
      props.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, "your-password");
      props.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG,
          "./kafka-0-creds/kafka-0.keystore.jks");
      props.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, "your-password");
      props.put(SslConfigs.SSL_KEY_PASSWORD_CONFIG, "your-password");

      // Deserializers
      props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
      props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class.getName());
      props.put("schema.registry.url", "http://localhost:18081");

      props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
      return props;
    }


    public static void main(String[] args) {
      try (KafkaConsumer<String, GenericRecord> consumer = new KafkaConsumer<>(getConsumerConfig())) {
        // Подписка на топик
        consumer.subscribe(Collections.singletonList("products"));

        System.out.println("Начинаем чтение сообщений из топика...");

        // Бесконечный цикл для чтения сообщений
        while (true) {
          ConsumerRecords<String, GenericRecord> records = consumer.poll(Duration.ofMillis(100));

          for (ConsumerRecord<String, GenericRecord> record : records) {
            System.out.println("\n=== Получено новое сообщение ===");
            System.out.println("Ключ: " + record.key());
            System.out.println("Оффсет: " + record.offset());
            System.out.println("Партиция: " + record.partition());

            // Извлечение и вывод данных из Avro записи
            GenericRecord product = record.value();

            System.out.println("Данные продукта:");
            System.out.println("  ID: " + product.get("product_id"));
            System.out.println("  Название: " + product.get("name"));
            System.out.println("  Описание: " + product.get("description"));
            System.out.println("  Категория: " + product.get("category"));
            System.out.println("  Бренд: " + product.get("brand"));
            System.out.println("  SKU: " + product.get("sku"));

            // Извлечение вложенных объектов
            GenericRecord price = (GenericRecord) product.get("price");
            System.out.println("  Цена: " + price.get("amount") + " " + price.get("currency"));

            GenericRecord stock = (GenericRecord) product.get("stock");
            System.out.println("  В наличии: " + stock.get("available") +
                ", зарезервировано: " + stock.get("reserved"));

            // Извлечение массива тегов
            Object tags = product.get("tags");
            if (tags != null) {
              System.out.println("  Теги: " + tags);
            }

            // Извлечение массива изображений
            Object images = product.get("images");
            if (images != null) {
              System.out.println("  Изображения: " + images);
            }

            // Извлечение спецификаций
            GenericRecord specs = (GenericRecord) product.get("specifications");
            if (specs != null) {
              System.out.println("  Вес: " + specs.get("weight"));
              System.out.println("  Размеры: " + specs.get("dimensions"));
              System.out.println("  Время работы батареи: " + specs.get("battery_life"));
              System.out.println("  Водонепроницаемость: " + specs.get("water_resistance"));
            }

            System.out.println("  Создан: " + product.get("created_at"));
            System.out.println("  Обновлен: " + product.get("updated_at"));
            System.out.println("  Индекс: " + product.get("index"));
            System.out.println("  ID магазина: " + product.get("store_id"));
          }

          // Подтверждение обработки сообщений
          consumer.commitSync();
        }
      } catch (Exception e) {
        System.err.println("Ошибка при чтении сообщений: " + e.getMessage());
        e.printStackTrace();
      }
    }
  }
