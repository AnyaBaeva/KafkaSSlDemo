package com.example;

import org.apache.spark.sql.SparkSession;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.conf.Configuration;

public class HDFSMonitor {

  private final FileSystem hdfs;

  public HDFSMonitor(SparkSession spark) throws Exception {
    Configuration hadoopConf = spark.sparkContext().hadoopConfiguration();
    this.hdfs = FileSystem.get(hadoopConf);
  }

  public void monitorProductsData() throws Exception {
    Path productsPath = new Path("hdfs://hadoop-namenode:9000/data/products");

    if (hdfs.exists(productsPath)) {
      long fileCount = hdfs.listStatus(productsPath).length;
      long totalSize = hdfs.getContentSummary(productsPath).getLength();

      System.out.println("=== HDFS MONITOR ===");
      System.out.println("Products directory: " + productsPath);
      System.out.println("Number of files: " + fileCount);
      System.out.println("Total size: " + (totalSize / (1024 * 1024)) + " MB");
      System.out.println("Last modified: " +
          new java.util.Date(hdfs.getFileStatus(productsPath).getModificationTime()));
    } else {
      System.out.println("Products directory not found in HDFS");
    }
  }

  public boolean waitForData(String path, long timeoutMs) throws Exception {
    long startTime = System.currentTimeMillis();
    Path hdfsPath = new Path(path);

    while (System.currentTimeMillis() - startTime < timeoutMs) {
      if (hdfs.exists(hdfsPath)) {
        long fileCount = hdfs.listStatus(hdfsPath).length;
        if (fileCount > 0) {
          System.out.println("Data found in " + path + ", files: " + fileCount);
          return true;
        }
      }
      Thread.sleep(5000); // Wait 5 seconds
      System.out.println("Waiting for data in " + path + "...");
    }

    return false;
  }
}