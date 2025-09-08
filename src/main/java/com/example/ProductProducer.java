package com.example;

import io.confluent.kafka.serializers.KafkaAvroSerializer;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.serialization.StringSerializer;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class ProductProducer {
  private static final String SCHEMA_STRING = "{\"type\":\"record\",\"name\":\"Product\",\"namespace\":\"com.example.avro\",\"fields\":[{\"name\":\"product_id\",\"type\":\"string\"},{\"name\":\"name\",\"type\":\"string\"},{\"name\":\"description\",\"type\":\"string\"},{\"name\":\"price\",\"type\":{\"type\":\"record\",\"name\":\"Price\",\"fields\":[{\"name\":\"amount\",\"type\":\"double\"},{\"name\":\"currency\",\"type\":\"string\"}]}},{\"name\":\"category\",\"type\":\"string\"},{\"name\":\"brand\",\"type\":\"string\"},{\"name\":\"stock\",\"type\":{\"type\":\"record\",\"name\":\"Stock\",\"fields\":[{\"name\":\"available\",\"type\":\"int\"},{\"name\":\"reserved\",\"type\":\"int\"}]}},{\"name\":\"sku\",\"type\":\"string\"},{\"name\":\"tags\",\"type\":{\"type\":\"array\",\"items\":\"string\"}},{\"name\":\"images\",\"type\":{\"type\":\"array\",\"items\":{\"type\":\"record\",\"name\":\"Image\",\"fields\":[{\"name\":\"url\",\"type\":\"string\"},{\"name\":\"alt\",\"type\":\"string\"}]}}},{\"name\":\"specifications\",\"type\":{\"type\":\"record\",\"name\":\"Specifications\",\"fields\":[{\"name\":\"weight\",\"type\":\"string\"},{\"name\":\"dimensions\",\"type\":\"string\"},{\"name\":\"battery_life\",\"type\":\"string\"},{\"name\":\"water_resistance\",\"type\":\"string\"}]}},{\"name\":\"created_at\",\"type\":\"string\"},{\"name\":\"updated_at\",\"type\":\"string\"},{\"name\":\"index\",\"type\":\"string\"},{\"name\":\"store_id\",\"type\":\"string\"}]}";

  private static Properties getProducerConfig() {
    Properties props = new Properties();
    props.put("bootstrap.servers", "localhost:19092,localhost:29092,localhost:39092");
    props.put("security.protocol", "SASL_SSL");
    props.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, "./kafka-0-creds/kafka-0.truststore.jks");
    props.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, "your-password");
    props.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, "./kafka-0-creds/kafka-0.keystore.jks");
    props.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, "your-password");
    props.put(SslConfigs.SSL_KEY_PASSWORD_CONFIG, "your-password");

    props.put("sasl.mechanism", "PLAIN");
    props.put("sasl.jaas.config",
        "org.apache.kafka.common.security.plain.PlainLoginModule required " +
            "username=\"admin\" password=\"your-password\";");

    props.put("key.serializer", StringSerializer.class.getName());
    props.put("value.serializer", KafkaAvroSerializer.class.getName());
    props.put("schema.registry.url", "http://localhost:18081");
    // Для надежной доставки
    props.put("acks", "all");
    props.put("retries", 3);
    props.put("enable.idempotence", true);

    return props;
  }

  public static void main(String[] args) {
    sendData();
  }

  public static void sendData() {
    try (KafkaProducer<String, GenericRecord> producer = new KafkaProducer<>(getProducerConfig())) {
      Schema schema = new Schema.Parser().parse(SCHEMA_STRING);

      // Чтение и парсинг JSON файла
      String jsonContent = new String(Files.readAllBytes(Paths.get("products.json")));
      JSONArray products = new JSONArray(jsonContent);

      // Обработка каждого продукта
      for (int i = 0; i < products.length(); i++) {
        JSONObject productJson = products.getJSONObject(i);

        // Создание Avro записи
        GenericRecord productRecord = new GenericData.Record(schema);

        // Заполнение основных полей
        productRecord.put("product_id", productJson.getString("product_id"));
        productRecord.put("name", productJson.getString("name"));
        productRecord.put("description", productJson.getString("description"));
        productRecord.put("category", productJson.getString("category"));
        productRecord.put("brand", productJson.getString("brand"));
        productRecord.put("sku", productJson.getString("sku"));
        productRecord.put("created_at", productJson.getString("created_at"));
        productRecord.put("updated_at", productJson.getString("updated_at"));
        productRecord.put("index", productJson.getString("index"));
        productRecord.put("store_id", productJson.getString("store_id"));

        // Заполнение вложенной структуры price
        JSONObject priceJson = productJson.getJSONObject("price");
        GenericRecord priceRecord = new GenericData.Record(schema.getField("price").schema());
        priceRecord.put("amount", priceJson.getDouble("amount"));
        priceRecord.put("currency", priceJson.getString("currency"));
        productRecord.put("price", priceRecord);

        // Заполнение вложенной структуры stock
        JSONObject stockJson = productJson.getJSONObject("stock");
        GenericRecord stockRecord = new GenericData.Record(schema.getField("stock").schema());
        stockRecord.put("available", stockJson.getInt("available"));
        stockRecord.put("reserved", stockJson.getInt("reserved"));
        productRecord.put("stock", stockRecord);

        // Заполнение массива tags
        JSONArray tagsJson = productJson.getJSONArray("tags");
        List<String> tags = new ArrayList<>();
        for (int j = 0; j < tagsJson.length(); j++) {
          tags.add(tagsJson.getString(j));
        }
        productRecord.put("tags", tags);

        // Заполнение массива images
        JSONArray imagesJson = productJson.getJSONArray("images");
        List<GenericRecord> images = new ArrayList<>();
        Schema imageSchema = schema.getField("images").schema().getElementType();
        for (int j = 0; j < imagesJson.length(); j++) {
          JSONObject imageJson = imagesJson.getJSONObject(j);
          GenericRecord imageRecord = new GenericData.Record(imageSchema);
          imageRecord.put("url", imageJson.getString("url"));
          imageRecord.put("alt", imageJson.getString("alt"));
          images.add(imageRecord);
        }
        productRecord.put("images", images);

        // Заполнение вложенной структуры specifications
        JSONObject specsJson = productJson.getJSONObject("specifications");
        GenericRecord specsRecord = new GenericData.Record(schema.getField("specifications").schema());
        specsRecord.put("weight", specsJson.getString("weight"));
        specsRecord.put("dimensions", specsJson.getString("dimensions"));
        specsRecord.put("battery_life", specsJson.getString("battery_life"));
        specsRecord.put("water_resistance", specsJson.getString("water_resistance"));
        productRecord.put("specifications", specsRecord);

        // Отправка сообщения в Kafka
        ProducerRecord<String, GenericRecord> record =
                new ProducerRecord<>("products", productJson.getString("product_id"), productRecord);

        producer.send(record, (metadata, exception) -> {
          if (exception == null) {
            System.out.println("Сообщение отправлено: " + metadata.toString());
          } else {
            System.err.println("Ошибка отправки: " + exception.getMessage());
          }
        });
      }

      producer.flush();
      System.out.println("Все сообщения отправлены");

    } catch (IOException e) {
      System.err.println("Ошибка чтения файла: " + e.getMessage());
    }
  }
}
