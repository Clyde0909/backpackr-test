# 과제 설명

## 선택 언어 및 사유

설명

## 과제 단계별 설명

1. KST 기준 Daily Partition 처리

설명

2. 동일 `user_id` 내에서 `event_time` 간격이 5분 이상인 경우 세션 종료로 간주하고 새로운 세션 ID를 생성

설명

3. 재처리 후 parquet, snappy 처리

설명

4. External Table 방식으로 설계 하고, 추가 기간 처리에 대응가능하도록 구현

설명

5. 배치 장애시 복구를 위한 장치 구현

설명

6. 설계한 Hive external 테이블을 이용하여 WAU(Weekly Active Users) 계산
    1. user_id 를 기준으로 WAU를 계산
    2. 2에서 생성된 세션 ID를 기준으로 WAU를 계산
    3. 6-a, 6-b의 결과 값과 계산에 사용한 쿼리도 함께 전달

설명