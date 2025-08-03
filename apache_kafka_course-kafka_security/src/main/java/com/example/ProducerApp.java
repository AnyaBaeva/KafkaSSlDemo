package com.example;

import java.util.Properties;
import java.util.concurrent.ExecutionException;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.serialization.StringSerializer;

public class ProducerApp{
    private static Properties getProducerConfig() {
        Properties props = new Properties();
        props.put("bootstrap.servers", "localhost:19092");
        props.put("security.protocol", "SASL_SSL");
        props.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, "C:/Users/abaev/IdeaProjects/apache_kafka_course-kafka_security/apache_kafka_course-kafka_security/kafka-0-creds/kafka-0.truststore.jks");
        props.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, "C:/Users/abaev/IdeaProjects/apache_kafka_course-kafka_security/apache_kafka_course-kafka_security/kafka-0-creds/kafka-0.keystore.jks");
        props.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, "your-password");
        props.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, "your-password");
        props.put(SslConfigs.SSL_KEY_PASSWORD_CONFIG, "your-password");
        props.put("sasl.mechanism", "PLAIN");
        props.put("sasl.jaas.config",
            "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"admin\" password=\"your-password\";");
        props.put("key.serializer", StringSerializer.class.getName());
        props.put("value.serializer", StringSerializer.class.getName());
        props.put("acks", "all");
        props.put("retries", 3);
        return props;
    }

    public static void main(String[] args) {
        try (Producer<String, String> producer = new KafkaProducer<>(getProducerConfig())) {
            // Отправка в topic-1 (разрешено)
            producer.send(new ProducerRecord<>("topic-1", "key1", "value1"));

            // Отправка в topic-2 (разрешено)
            producer.send(new ProducerRecord<>("topic-2", "key2", "value2"));
        }
    }
}