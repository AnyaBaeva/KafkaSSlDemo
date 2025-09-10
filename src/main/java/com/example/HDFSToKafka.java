package com.example;

import java.util.Properties;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

public class HDFSToKafka {

  public static void main(String[] args) {
    SparkSession spark = SparkSession.builder()
        .appName("Simple-HDFS-to-Kafka-Test")
        .config("spark.hadoop.fs.defaultFS", "hdfs://hadoop-namenode:9000")
        .config("spark.executor.memory", "2g")
        .config("spark.driver.memory", "2g")
        .config("spark.memory.fraction", "0.8")
        .master("local[*]")
        .getOrCreate();


    try {
      // Чтение небольшой порции данных для теста
      Dataset<Row> testDF = spark.read()
          .format("avro")
          .load("hdfs://hadoop-namenode:9000/data/products/*.avro")
          .limit(10);

      System.out.println("Test data schema:");
      testDF.printSchema();

      System.out.println("Sample data:");
      testDF.show(5, false);

      // Простая отправка в Kafka
      Properties props = new Properties();
      props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:19092");
      props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
      props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

      try (KafkaProducer<String, String> producer = new KafkaProducer<>(props)) {
        Row[] rows = (Row[]) testDF.collect();

        for (Row row : rows) {
          String productId = row.getAs("product_id");
          String productName = row.getAs("name");

          String message = String.format("{\"product_id\":\"%s\",\"name\":\"%s\"}",
              productId, productName.replace("\"", "\\\""));

          ProducerRecord<String, String> record = new ProducerRecord<>(
              "test_products", productId, message);

          producer.send(record);
          System.out.println("Sent: " + productId + " - " + productName);
        }

        producer.flush();
      }

      System.out.println("Test completed successfully!");

    } catch (Exception e) {
      System.err.println("Test failed: " + e.getMessage());
      e.printStackTrace();
    } finally {
      spark.stop();
    }
  }
}
