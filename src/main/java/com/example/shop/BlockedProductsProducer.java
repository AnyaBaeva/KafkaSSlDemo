package com.example.shop;

import com.example.config.KafkaProperties;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

public class BlockedProductsProducer {

    public static void main(String[] args) {
        String filePath = "blocked_products.txt";

        Properties props = KafkaProperties.getConfig();

        try (KafkaProducer<String, String> producer = new KafkaProducer<>(props);
             BufferedReader reader = new BufferedReader(new FileReader(filePath))) {

            String line;
            while ((line = reader.readLine()) != null) {
                // Очищаем строку от пробелов и запятых
                String productId = line.trim().replace(",", "").replace(" ", "");

                if (!productId.isEmpty()) {
                    // Создаем JSON сообщение
                    String jsonMessage = String.format("{\"product_id\": \"%s\"}", productId);

                    // Отправляем сообщение в Kafka
                    ProducerRecord<String, String> record =
                            new ProducerRecord<>(props.getProperty("topic_blockedProducts"), productId, jsonMessage);

                    producer.send(record, (metadata, exception) -> {
                        if (exception != null) {
                            System.err.println("Error sending product " + productId + ": " + exception.getMessage());
                        } else {
                            System.out.println("Successfully sent product: " + productId +
                                    ", partition: " + metadata.partition() +
                                    ", offset: " + metadata.offset());
                        }
                    });
                }
            }

            producer.flush();
            System.out.println("All products from file sent to Kafka topic: " + props.getProperty("topic_blockedProducts"));

        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        }
    }
}