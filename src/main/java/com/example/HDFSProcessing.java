package com.example;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.functions;

public class HDFSProcessing {

  public static void main(String[] args) {
    SparkSession spark = SparkSession.builder()
        .appName("Simple-HDFS-Processing")
        .config("spark.hadoop.fs.defaultFS", "hdfs://hadoop-namenode:9000")
        .config("spark.executor.memory", "2g")
        .config("spark.driver.memory", "2g")
        .config("spark.memory.fraction", "0.8")
        .master("spark://spark-master:7077")
        .getOrCreate();

    try {
      // Автоматическое определение схемы из Avro файлов
      Dataset<Row> productsDF = spark.read()
          .format("avro")
          .load("hdfs://hadoop-namenode:9000/data/products/*.avro");

      System.out.println("Schema automatically inferred from Avro:");
      productsDF.printSchema();

      // Простая обработка с извлечением вложенных полей
      Dataset<Row> processedDF = productsDF
          .withColumn("price_amount", functions.col("price.amount"))
          .withColumn("price_currency", functions.col("price.currency"))
          .withColumn("available_stock", functions.col("stock.available"))
          .withColumn("tags_count", functions.size(functions.col("tags")))
          .withColumn("processing_time", functions.current_timestamp());

      // Базовая агрегация
      Dataset<Row> stats = processedDF
          .groupBy("category")
          .agg(
              functions.count("product_id").as("product_count"),
              functions.sum("available_stock").as("total_stock"),
              functions.avg("price_amount").as("avg_price")
          )
          .orderBy("product_count");

      // Запись результатов
      processedDF.write()
          .mode("overwrite")
          .parquet("hdfs://hadoop-namenode:9000/processed_data/simple_processed");

      stats.write()
          .mode("overwrite")
          .parquet("hdfs://hadoop-namenode:9000/analytics/simple_stats");

      System.out.println("Processing completed successfully!");
      System.out.println("Total records: " + processedDF.count());
      stats.show(10, false);

    } catch (Exception e) {
      System.err.println("Error: " + e.getMessage());
      e.printStackTrace();
    } finally {
      spark.stop();
    }
  }
}