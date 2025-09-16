package com.example.hdfs;

import com.example.config.KafkaProperties;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.ProducerConfig;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.UUID;


public class KafkaHdfsConsumer {
    public static void main(String[] args) {


        Properties props = new Properties();
// Указываем все брокеры-реплики
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:19092,localhost:29092,localhost:39092");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "hadoop-consumer-group");
// Указываем стойку (rack) потребителя - должна соответствовать стойке одной из реплик!
//        props.put(ConsumerConfig.CLIENT_RACK_CONFIG, "us-east-1a"); // Замените на актуальную стойку вашего потребителя
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");

// Прочие рекомендуемые настройки
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false"); // Рекомендуется отключить авто-коммит для большей надежности
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, "30000");
        props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, "3000");
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "500"); // Настройте под вашу нагрузку
        props.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, "1");
        props.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, "500");

        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singletonList("products"));


        String hdfsUri = "hdfs://localhost:9000";
        Configuration conf = new Configuration();
        conf.set("fs.defaultFS", hdfsUri);
//        conf.set("dfs.client.socket-timeout", "300000"); // 5 минут вместо стандартных 60 секунд
//        conf.set("dfs.client.block.write.timeout", "300000");



        try (FileSystem hdfs = FileSystem.get(new URI(hdfsUri), conf, "root")) {
            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
                for (ConsumerRecord<String, String> record : records) {
                    String value = record.value();


                    System.out.println("Получено сообщение: " + value + ", partition=" + record.partition() + ", offset=" + record.offset());


                    String hdfsFilePath = "/data/message_" + UUID.randomUUID();
                    Path path = new Path(hdfsFilePath);




                    // Запись файла в HDFS
                    try (FSDataOutputStream outputStream = hdfs.create(path, true)) {
                        outputStream.writeUTF(value);
                    }


                    System.out.println("Сообщение записано в HDFS по пути: " + hdfsFilePath);




                    //  Чтение файла из HDFS
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(hdfs.open(path), StandardCharsets.UTF_8))) {
                        StringBuilder content = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            content.append(line).append("\n");
                        }
                        System.out.println("Чтение файла '" + hdfsFilePath + "' из HDFS. Содержимое: '" + content.toString().trim() + "'");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            consumer.close();
        }
    }
}