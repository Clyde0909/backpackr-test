import sys.process._
import java.nio.file.{Files, Paths}
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._
import org.apache.spark.sql.expressions.Window

object backpackr {
  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder()
      .appName("backpackr")
      .enableHiveSupport() // Hive 지원 활성화
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

    // 장애 복구를 위한 체크포인트 디렉토리 설정
    val checkpointDir = "/data/checkpoints"
    spark.sparkContext.setCheckpointDir(checkpointDir)

    // CSV 파일 읽기
    val dfNov = spark.read.option("header", "true").csv(novCsvFile)
    val dfOct = spark.read.option("header", "true").csv(octCsvFile)

    // 데이터 합치기
    val df = dfNov.union(dfOct)

    // KST 기준 일별 파티션 처리 (event_time은 원래 UTC 기준)
    val dfWithDate = df
      .withColumn("event_time_kst", from_utc_timestamp(col("event_time"), "Asia/Seoul"))
      .withColumn("date", to_date(col("event_time_kst")))

     // 세션 ID 생성
    val windowSpec = Window.partitionBy("user_id").orderBy("event_time_kst")
    val dfWithSession = dfWithDate
      .withColumn("prev_event_time", lag("event_time_kst", 1).over(windowSpec))
      .withColumn("time_diff", coalesce(unix_timestamp(col("event_time_kst")) - unix_timestamp(col("prev_event_time")), lit(0)))
      .withColumn("session_start", when(col("time_diff") > 300 || col("time_diff") === 0, 1).otherwise(0))
      .withColumn("session_id", sum("session_start").over(windowSpec))
      .drop("prev_event_time", "time_diff", "session_start")
      .repartition(col("date")) // 파티션 수 조정 (메모리 부족시 별도 조정)

    // 체크포인트 적용
    dfWithSession.checkpoint()

    // Hive External Table 생성
    val tableName = "user_activity"
    val tableLocation = "/data/sessionized_data" // 데이터 위치
    
    spark.sql(s"DROP TABLE IF EXISTS $tableName") // 테이블이 존재하면 삭제
    spark.sql(
        s"""
          CREATE EXTERNAL TABLE $tableName (
            event_time STRING,
            event_type STRING,
            product_id STRING,
            category_id STRING,
            category_code STRING,
            brand STRING,
            price STRING,
            user_id STRING,
            user_session STRING,
            event_time_kst TIMESTAMP,
            session_id INT
          )
          PARTITIONED BY (date DATE)
          STORED AS PARQUET
          LOCATION '$tableLocation'
          """
    )
    
    // 데이터 저장
    dfWithSession
    .write
    .mode("overwrite")
    .partitionBy("date")
    .option("compression", "snappy")
    .option("path", tableLocation)
    .saveAsTable(tableName)
    
    // 새로운 파티션만 복구 ( Metastore 업데이트를 통해 파티션 정보를 업데이트)
    spark.sql(s"ALTER TABLE $tableName RECOVER PARTITIONS")
    
    // WAU 계산을 위한 쿼리 (user_id 기준, date_trunc 함수 사용 - 월요일 기준)
    val wauUserIdQuery =
      s"""
        SELECT
          date_format(date_trunc('week', date), 'yyyy-MM-dd') AS week_start,
          count(DISTINCT user_id) AS wau_user_id
        FROM
          $tableName
        GROUP BY
          date_trunc('week', date)
        ORDER BY
          week_start
      """

    // WAU 계산 (user_id 기준)
    val wauUserId = spark.sql(wauUserIdQuery)
    println("WAU (user_id 기준):")
    wauUserId.show(5)

    // WAU 계산을 위한 쿼리 (session_id 기준, date_trunc 함수 사용 - 월요일 기준)
    val wauSessionIdQuery =
      s"""
        SELECT
          date_format(date_trunc('week', date), 'yyyy-MM-dd') AS week_start,
          count(DISTINCT concat(user_id, '_', session_id)) AS wau_session_id
        FROM
          $tableName
        GROUP BY
          date_trunc('week', date)
        ORDER BY
          week_start
      """

    // WAU 계산 (session_id 기준)
    val wauSessionId = spark.sql(wauSessionIdQuery)
    println("WAU (session_id 기준):")
    wauSessionId.show(5)

    // 쿼리 결과 저장
    wauUserId.write.mode("overwrite").option("header", "true").csv("/data/wau_user_id")
    wauSessionId.write.mode("overwrite").option("header", "true").csv("/data/wau_session_id")

    // 6-3: 결과 값과 계산에 사용한 쿼리 출력
    println("6-1. WAU (user_id 기준) 계산 쿼리:")
    println(wauUserIdQuery)
    println("6-2. WAU (session_id 기준) 계산 쿼리:")
    println(wauSessionIdQuery)

    println("6-1. WAU (user_id 기준) 결과 (최대 5주):")
    wauUserId.show(5)
    println("6-2. WAU (session_id 기준) 결과 (최대 5주):")
    wauSessionId.show(5)

    spark.stop()
  }
}