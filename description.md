# 과제 설명

## 선택 언어 및 사유

본 과제에서는 Java와 Scala 중 하나를 선택해야 했고, 저는 Scala를 선택했습니다. 저는 주로 Python을 사용하여 개발해 왔으며, Numpy나 Scipy와 같은 라이브러리를 활용하여 데이터 처리 및 분석 업무를 수행한 경험이 있습니다. Java와 Scala의 특징을 비교했을 때, Scala가 제 경험을 최대한 활용할 수 있을 것이라 판단했습니다.

먼저, Python 개발자의 관점에서 Scala는 객체 지향 프로그래밍과 함수형 프로그래밍을 모두 지원하며, 비교적 친숙한 문법과 간결성을 제공합니다.  
예를 들어, Python의 `def` 키워드를 사용한 함수 정의는 Scala에서도 거의 동일하게 `def` 키워드를 사용하여 이루어지며, 리스트 컴프리헨션 역시 유사한 형태로 Scala에서 사용할 수 있습니다.  
> Python: `squares = [x2 for x in range(10)]`  
Scala: `val squares = (0 until 10).map(x => x * x)`

이러한 문법적 유사성은 Python에서 Scala로의 전환을 Java보다 수월하게 만들어 줄 것이라 기대했습니다.

또한, 이번 과제는 Spark를 활용한 대규모 데이터 처리를 포함하고 있습니다. Spark가 Scala로 작성되었다는 점을 고려했을 때, Scala를 사용하면 Spark의 기능을 더 효과적으로 활용할 수 있을 것으로 판단했습니다. 예를 들어, Spark의 Dataset API는 Scala의 정적 타입 지정을 활용하여 컴파일 시점에 타입 오류를 검출하고 런타임 에러를 줄일 수 있습니다. 또한, Scala는 JVM에서 실행되어 Java와 동일한 수준의 성능을 제공하며, Scala의 함수형 프로그래밍 기능과 정적 타입 지정은 병렬 처리 및 분산 처리에 있어서 더욱 효율적인 코드를 작성할 수 있도록 도와줍니다.

PySpark를 사용하면서, 타입 안정성에 대한 아쉬움이 있었습니다. Scala를 사용하면 이러한 부분을 개선하고, 더욱 효율적이고 안정적인 Spark 애플리케이션을 개발할 수 있을 것이라 기대했습니다.

결론적으로, Java보다는 Python 개발자의 관점에서 더 접근하기 쉬운 Scala를 선택함으로써, 기존 Python 개발 경험을 최대한 활용하면서도 JVM 기반 언어의 장점과 Spark와의 긴밀한 통합을 통해 성능 및 타입 안정성 측면에서 이점을 얻을 수 있을 것이라 판단했습니다.


## 과제 단계별 설명

1. KST 기준 Daily Partition 처리

*   원본 데이터의 `event_time` 컬럼은 UTC 기준의 타임스탬프를 저장하고 있습니다. 이를 KST(Korea Standard Time) 기준으로 변환하고, 일자별 파티션을 생성하기 위해 다음과 같은 처리를 수행했습니다.
*   Spark SQL의 `from_utc_timestamp` 함수를 사용하여 `event_time` 컬럼을 UTC에서 KST로 변환하여 `event_time_kst` 컬럼을 생성했습니다.
*   `to_date` 함수를 사용하여 `event_time_kst` 컬럼에서 날짜를 추출하고, `date` 컬럼을 생성했습니다.

---

2. 동일 `user_id` 내에서 `event_time` 간격이 5분 이상인 경우 세션 종료로 간주하고 새로운 세션 ID를 생성

*   사용자의 행동 데이터를 분석하기 위해, 세션을 정의하고 각 사용자에게 고유한 세션 ID를 부여했습니다.
*   동일한 `user_id` 내에서 `event_time` 간격이 5분 이상인 경우, 세션이 종료된 것으로 간주하고 새로운 세션 ID를 생성하도록 구현했습니다.
*   이를 위해 Spark SQL의 윈도우 함수(`lag`, `sum`)를 사용했습니다.
    *   `lag` 함수를 사용하여 동일 `user_id` 내에서 바로 이전 이벤트의 `event_time_kst` 값을 가져와 `prev_event_time` 컬럼을 생성했습니다.
    *   `unix_timestamp` 함수를 사용하여 `event_time_kst`와 `prev_event_time`의 차이를 초 단위로 계산하고, `time_diff` 컬럼에 저장했습니다.
    *   `when` 함수를 사용하여 `time_diff`가 300초(5분) 이상이거나 `prev_event_time`이 null인 경우(첫 번째 이벤트인 경우), `session_start` 컬럼에 1을, 그렇지 않으면 0을 할당했습니다.
    *   `sum` 함수를 윈도우 함수와 함께 사용하여 `session_start` 컬럼의 누적 합계를 계산하고, 이를 `session_id` 컬럼에 저장했습니다. 이로써, 새로운 세션이 시작될 때마다 `session_id` 값이 1씩 증가하게 됩니다.
*   마지막으로 `user_id`와 `session_id`를 조합하여 고유한 세션 ID를 생성하였습니다.

---


3. 재처리 후 parquet, snappy 처리

*   Spark DataFrame의 `write` 메서드를 사용하여 데이터를 Parquet 포맷으로 저장하고, `option("compression", "snappy")`을 통해 Snappy 압축을 적용했습니다.

---


4. External Table 방식으로 설계 하고, 추가 기간 처리에 대응가능하도록 구현

*   Hive 테이블을 External Table로 설계하여, 데이터 파일은 HDFS 또는 로컬 파일 시스템에 저장하고, 테이블의 메타데이터만 Hive Metastore에 저장하도록 했습니다.
*   External Table을 사용하면, 데이터 파일의 위치를 변경하거나, 다른 애플리케이션에서 데이터 파일에 직접 접근하는 것이 용이합니다.
*   `CREATE EXTERNAL TABLE` 문을 사용하여 Hive 외부 테이블을 생성하고, `LOCATION` 절을 통해 데이터 파일의 경로를 지정했습니다.
*   `ALTER TABLE ... RECOVER PARTITIONS` 명령을 사용하여, 새로운 파티션(새로운 날짜의 데이터)이 추가되었을 때, Metastore에 파티션 정보를 등록하도록 구현했습니다. 이를 통해, 추가 기간의 데이터 처리에 유연하게 대응할 수 있습니다.

---

5. 배치 장애시 복구를 위한 장치 구현

*   배치 처리 중 장애가 발생했을 때, 처음부터 다시 시작하지 않고 마지막으로 성공한 지점부터 복구할 수 있도록 체크포인팅(checkpointing)을 적용했습니다.
*   Spark의 체크포인팅 기능을 사용하여, 데이터 전처리 완료 후 중간 결과를 HDFS 또는 로컬 파일 시스템에 저장하도록 했습니다.

---

6. 설계한 Hive external 테이블을 이용하여 WAU(Weekly Active Users) 계산
    1. user_id 를 기준으로 WAU를 계산
    2. 2에서 생성된 세션 ID를 기준으로 WAU를 계산
    3. 6-a, 6-b의 결과 값과 계산에 사용한 쿼리도 함께 전달

*   설계한 Hive External Table을 사용하여 주간 활성 사용자 수(WAU)를 계산했습니다.
*   `user_id`와 세션 ID(`session_id`)를 기준으로 각각 WAU를 계산했습니다.
*   `date_trunc('week', date)` 함수를 사용하여 주차를 계산했습니다. `date_trunc`는 주의 시작을 월요일로 간주합니다.
*   `count(DISTINCT ...)` 함수를 사용하여 고유 사용자 수 또는 고유 세션 수를 계산했습니다.  

**user_id를 기준으로 WAU를 계산하는 쿼리**
```sql
SELECT
    date_format(date_trunc('week', date), 'yyyy-MM-dd') AS week_start,
    count(DISTINCT user_id) AS wau_user_id
FROM
    $tableName
GROUP BY
    date_trunc('week', date)
ORDER BY
    week_start
```

**결과**

```bash
+----------+-----------+
|week_start|wau_user_id|
+----------+-----------+
|2019-09-30|     818388|
|2019-10-07|    1057958|
|2019-10-14|    1090898|
|2019-10-21|    1093146|
|2019-10-28|    1054722|
+----------+-----------+
```

**session_id를 기준으로 WAU를 계산하는 쿼리**
```sql
SELECT
    date_format(date_trunc('week', date), 'yyyy-MM-dd') AS week_start,
    count(DISTINCT concat(user_id, '_', session_id)) AS wau_session_id
FROM
    $tableName
GROUP BY
    date_trunc('week', date)
ORDER BY
    week_start
```

**결과**

```bash
+----------+--------------+
|week_start|wau_session_id|
+----------+--------------+
|2019-09-30|       1573080|
|2019-10-07|       2155922|
|2019-10-14|       2263210|
|2019-10-21|       2159382|
|2019-10-28|       2116987|
+----------+--------------+
```
