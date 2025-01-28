import org.apache.spark.sql.SparkSession
import sys.process._
import java.nio.file.{Files, Paths}

object backpackr {
  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder()
      .appName("backpackr")
      .getOrCreate()

    // Kaggle 데이터셋 다운로드
    val dataset = "mkechinov/ecommerce-behavior-data-from-multi-category-store"
    val downloadPath = "/data"
    val zipFile = s"$downloadPath/ecommerce-behavior-data-from-multi-category-store.zip"
    val novCsvFile = s"$downloadPath/2019-Nov.csv"
    val octCsvFile = s"$downloadPath/2019-Oct.csv"

    // CSV 파일 존재 여부
    val csvFilesExist = Files.exists(Paths.get(novCsvFile)) && Files.exists(Paths.get(octCsvFile))
    // ZIP 파일 존재 여부
    val zipFileExists = Files.exists(Paths.get(zipFile))

    // CSV 파일이 존재하는지 확인
    if (!csvFilesExist) {
      // ZIP 파일이 존재하는지 확인
      if (!zipFileExists) {
      println("Downloading Kaggle dataset...")
      s"/root/.local/bin/kaggle datasets download -d $dataset -p $downloadPath".!
      } else {
      println("Kaggle dataset already downloaded.")
      }

      // ZIP 파일 압축 해제
      println("Unzipping Kaggle dataset...")
      s"unzip -o $zipFile -d $downloadPath".!
    } else {
      println("Kaggle dataset already unzipped.")
    }

    // CSV 파일 읽기
    val dfNov = spark.read.option("header", "true").csv(novCsvFile)
    val dfOct = spark.read.option("header", "true").csv(octCsvFile)

    // 데이터 요약
    println("2019-Nov.csv 요약:")
    dfNov.describe().show()

    println("2019-Oct.csv 요약:")
    dfOct.describe().show()

    spark.stop()
  }
}