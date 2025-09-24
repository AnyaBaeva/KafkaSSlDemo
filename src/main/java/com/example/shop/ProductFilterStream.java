package com.example.shop;

import com.example.config.KafkaProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Produced;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

public class ProductFilterStream {
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final Set<String> blockedProductIds = new HashSet<>();

    public static void main(String[] args) {
        // Сначала читаем и отправляем данные из файла в Kafka
        for (int i = 0; i <= 100; i++) {
            sendJsonFileToKafka("products.json", "inputJsonStream");
        }

        // Затем запускаем Streams обработку
        startStreamsProcessing();
    }

    private static void sendJsonFileToKafka(String filename, String topic) {
        Properties props = KafkaProperties.getConfig();

        try (KafkaProducer<String, String> producer = new KafkaProducer<>(props)) {
            String content = new String(Files.readAllBytes(Paths.get(filename)));

            // Парсим JSON массив
            JsonNode productsArray = mapper.readTree(content);

            if (productsArray.isArray()) {
                for (JsonNode product : productsArray) {
                    String productJson = product.toString();
                    String productId = product.get("product_id").asText();

                    ProducerRecord<String, String> record =
                            new ProducerRecord<>(topic, productId, productJson);

                    producer.send(record, (metadata, exception) -> {
                        if (exception != null) {
                            System.err.println("Error sending product " + productId + ": " + exception.getMessage());
                        } else {
                            System.out.println("Product sent to Kafka: " + productId +
                                    ", offset: " + metadata.offset());
                        }
                    });
                }
            }
            producer.flush();
            System.out.println("All products from file sent to Kafka topic: " + topic);

        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        }
    }

    private static void startStreamsProcessing() {
        Properties props = KafkaProperties.getConfig();
        StreamsBuilder builder = new StreamsBuilder();

        // Подписываемся на топик с заблокированными продуктами
        builder.stream("blockedProducts", Consumed.with(Serdes.String(), Serdes.String()))
                .foreach((key, value) -> {
                    try {
                        if (value != null) {
                            JsonNode blockedProduct = mapper.readTree(value);
                            String productId = blockedProduct.get("product_id").asText();
                            blockedProductIds.add(productId);
                            System.out.println("Blocked product added: " + productId);
                        }
                    } catch (Exception e) {
                        System.err.println("Error parsing blocked product JSON: " + e.getMessage());
                    }
                });

        // Обрабатываем основной поток продуктов
        builder.stream("inputJsonStream", Consumed.with(Serdes.String(), Serdes.String()))
                .filter((key, value) -> {
                    try {
                        if (value == null) return false;

                        JsonNode product = mapper.readTree(value);
                        String productId = product.get("product_id").asText();
                        boolean isBlocked = blockedProductIds.contains(productId);

                        if (!isBlocked) {
                            System.out.println("Product allowed: " + productId);
                            return true;
                        } else {
                            System.out.println("Product blocked: " + productId);
                            return false;
                        }
                    } catch (Exception e) {
                        System.err.println("Error parsing product JSON: " + e.getMessage());
                        return false;
                    }
                })
                .to("products", Produced.with(Serdes.String(), Serdes.String()));

        KafkaStreams streams = new KafkaStreams(builder.build(), props);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            streams.close();
            System.out.println("Streams application closed");
        }));

        streams.start();
        System.out.println("Streams application started");
    }
}