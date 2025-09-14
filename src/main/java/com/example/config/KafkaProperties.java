package com.example.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.streams.StreamsConfig;

import java.util.Properties;

public class KafkaProperties {
    public static Properties getConfig() {
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "product-filter-app");
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:19092,localhost:29092,localhost:39092");
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

        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        props.put("schema.registry.url", "http://localhost:18081");

        // Для надежной доставки
        props.put("enable.idempotence", true);

        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 3);

        props.put("topic_blockedProducts", "blockedProducts");
        props.put("topic_products", "products");


        return props;
    }
}
