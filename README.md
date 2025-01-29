# Backpackr_DE_Project

## 설명

- 주어진 과제 해결을 위한 프로젝트입니다.

## 개발 환경

- OpenJDK 11
- Scala 3.6.3
- Docker 27.4

## Pre-requisite

- Kaggle API Token 파일(kaggle.json 파일을 spark 폴더에 넣어주세요.)

## 실행 방법

```sh
$ docker-compose up -d
# spark 컨테이너 내부의 /opt/spark/code위치에서 sbt package 명령어를 실행
$ docker exec -it spark bash -c "cd /opt/spark/code && sbt package"
# spark 컨테이너 내부에서 spark-submit 명령어를 실행
$ docker exec -it spark bash -c "/opt/spark/bin/spark-submit --class backpackr --master local[*] --driver-memory 3g /opt/spark/code/target/scala-2.12/backpackr-project_2.12-1.0.jar"
```

코드 작동이 끝난 뒤 data 폴더에 결과 파일이 생성됩니다.

## 과제 설명

- [과제 설명](./description.md)

## Trouble Shooting

1. 만일 OOM(Out Of Memory) 에러가 발생한다면, 실행 방법 5번째 줄에 있는 `--driver-memory 3g` 옵션을 더 높은 값으로 변경해주세요. ( ex. `--driver-memory 4g`, 사전 테스트 환경에서는 3g로 충분했습니다. )
2. 만일 `docker-compose up` 명령어를 실행했을 때,  
`STATEMENT:  CREATE TABLE "BUCKETING_COLS" ( "SD_ID" bigint NOT NULL, "BUCKET_COL_NAME" character varying(256) DEFAULT NULL::character varying, "INTEGER_IDX" bigint NOT NULL )` 에러가 발생한다면,  
다음 명령어를 실행한 후 다시 `docker-compose up` 명령어를 실행해주세요.

```sh
$ docker-compose down -v
```