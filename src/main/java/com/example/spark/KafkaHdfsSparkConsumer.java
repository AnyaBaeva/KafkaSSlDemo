package com.example.spark;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.functions;

import java.util.regex.Pattern;

public class KafkaHdfsSparkConsumer {

    private static final Pattern JSON_PATTERN = Pattern.compile("\"brand\"\\s*:\\s*\"([^\"]+)\"");

    public static void main(String[] args) {
        SparkConf sparkConf = new SparkConf()
                .setAppName("HDFS Brand Analysis")
                .setMaster("local[*]")
                .set("spark.sql.warehouse.dir", "hdfs://172.19.0.12:9000/user/hive/warehouse")
                .set("spark.driver.extraJavaOptions",
                        "--add-opens=java.base/sun.nio.ch=ALL-UNNAMED " +
                                "--add-opens=java.base/java.lang=ALL-UNNAMED");

        System.setProperty("hadoop.home.dir", "C:\\hadoop");

        try (JavaSparkContext sc = new JavaSparkContext(sparkConf)) {
            SparkSession spark = SparkSession.builder()
                    .config(sparkConf)
                    .getOrCreate();

            spark.sparkContext().setLogLevel("ERROR");

            String hdfsPath = "hdfs://172.19.0.12:9000/data/products";

            System.out.println("Анализ данных из HDFS: " + hdfsPath);
            System.out.println("============================================================");

            analyzeBrand(spark, hdfsPath);

            System.out.println("\n============================================================");

        } catch (Exception e) {
            System.err.println("Ошибка при анализе данных: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void analyzeBrand(SparkSession spark, String hdfsPath) {
        try {
            // Чтение текстовых данных из HDFS (каждая строка - отдельная запись)
            Dataset<String> textData = spark.read().textFile(hdfsPath);

            System.out.println("Анализ текстовых данных из HDFS");
            System.out.println("=" .repeat(50));

            System.out.println("Пример данных (первые 5 строк):");
            textData.show(5, false);

            // Преобразуем в DataFrame с извлечением бренда из текста
            Dataset<Row> df = textData
                    .filter(functions.col("value").isNotNull())
                    .withColumn("brand", extractBrandFromText(functions.col("value")));

            // Показываем данные с извлеченными брендами
            System.out.println("Данные с извлеченными брендами (первые 5 записей):");
            df.select("value", "brand").show(5, false);

            // Анализ: какой бренд встречается чаще всего
            System.out.println("ТОП-10 самых популярных брендов:");

            Dataset<Row> brandStats = df
                    .filter(functions.col("brand").isNotNull())
                    .groupBy("brand")
                    .count()
                    .orderBy(functions.desc("count"))
                    .limit(10);

            brandStats.show(false);

            // Дополнительная статистика
            System.out.println("Общая статистика:");
            long totalLines = textData.count();
            long linesWithBrand = df.filter(functions.col("brand").isNotNull()).count();
            long uniqueBrands = df.select("brand").distinct().count();

            System.out.println("Всего строк: " + totalLines);
            System.out.println("Строк с извлеченным брендом: " + linesWithBrand);
            System.out.println("Уникальных брендов: " + uniqueBrands);

            // Анализ самых частых слов (альтернативный подход)
            System.out.println("ТОП-20 самых частых слов:");

            Dataset<Row> wordStats = textData
                    .filter(functions.col("value").isNotNull())
                    .select(functions.explode(functions.split(functions.col("value"), "\\s+")).alias("word"))
                    .filter(functions.length(functions.col("word")).gt(3)) // слова длиннее 3 символов
                    .groupBy("word")
                    .count()
                    .orderBy(functions.desc("count"))
                    .limit(20);

            wordStats.show(false);

            // Сохраняем результаты анализа
            brandStats.write()
                    .mode("overwrite")
                    .csv("hdfs://172.19.0.12:9000/results/brand_analysis_csv");

            System.out.println("Результаты сохранены в HDFS: /results/brand_analysis_csv");

        } catch (Exception e) {
            System.err.println("Ошибка при анализе текстовых данных: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static org.apache.spark.sql.Column extractBrandFromText(org.apache.spark.sql.Column textColumn) {
        return functions.regexp_extract(textColumn, "(?i)(samsung|apple|xiaomi|huawei|sony|lg|nokia|google|oneplus|motorola)", 0);
    }

}