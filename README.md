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
$ docker-compose up
```

## 과제 설명

- [과제 설명](./description.md)

## Trouble Shooting

만일 `docker-compose up` 명령어를 실행했을 때,  
`STATEMENT:  CREATE TABLE "BUCKETING_COLS" ( "SD_ID" bigint NOT NULL, "BUCKET_COL_NAME" character varying(256) DEFAULT NULL::character varying, "INTEGER_IDX" bigint NOT NULL )` 에러가 발생한다면,  
다음 명령어를 실행한 후 다시 `docker-compose up` 명령어를 실행해주세요.

```sh
$ docker-compose down -v
```