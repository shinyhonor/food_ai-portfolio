# 오늘의 식단 기록
> **AI 기반 식단 이미지 분석 및 영양 정보 관리 시스템(기여도: 100%)**
> 
> 단순한 AI 모델 구현을 넘어, **BFF(Backend For Frontend) 아키텍처**를 도입하고 시스템 간 **통신 프로토콜 최적화**를 통해 엔드투엔드 서비스를 구축한 프로젝트입니다.

---

## System Architecture
현대적인 마이크로서비스 구조를 지향하며, 클라이언트와 AI 서버 간의 결합도를 낮추기 위해 Spring Boot를 API Gateway 겸 BFF로 배치했습니다. 또한 대규모 트래픽 상황에서 서버 생존성을 보장하기 위해 비동기 메시지 큐(Kafka)를 도입했습니다.

**Vue.js (Frontend) <-> Spring Boot (BFF/Auth) <-> Apache Kafka <-> Django (AI Inference) <-> Oracle DB**

---

## Key Achievements & Technical Challenges

### 1. 네트워크 프로토콜 불일치 해결(Manual Serialization)
- **Problem**: Spring WebClient로 대용량 이미지 전송 시, Django(WSGI) 서버가 데이터를 수신하지 못하고 바디가 유실되는 현상 발생.
- **Analysis**: WebClient의 기본 전송 방식인 **Transfer-Encoding: chunked**가 수신측인 WSGI 표준 스펙과 호환되지 않아 발생한 문제임을 파악.
- **Solution**: 라이브러리의 추상화 계층에 의존하지 않고, **RFC 7578 스펙에 맞춰 Multipart 데이터를 직접 바이트 스트림으로 직렬화**하여 전송.
- **Result**: Content-Length를 명시적으로 제어함으로써 시스템 간 통신 무결성을 100% 확보하고 호환성 문제 해결.

### 2. 1.6TB 대규모 데이터 처리 최적화
- **Challenge**: 845,000장(1.6TB) 규모의 AI 허브 음식 데이터셋 처리 시 HDD I/O 병목으로 인한 학습 속도 저하.
- **Optimization**: 물리 저장소 분산 처리 및 병렬 전처리 파이프라인 구축.
- **Result**: 학습 속도를 기존 대비 **300% 이상 향상**.

### 3. BFF 아키텍처 및 보안 강화
- **Design**: AI 서버(Django)를 비즈니스 로직과 분리하여 추론 전용 워커로 구성하고, Spring Boot를 통해 통합 인증 및 라우팅을 수행.
- **Security**: Spring Security 6를 활용하여 세션 기반 인증 구현. 클라이언트의 민감한 정보 없이 서버 간 통신(Server-to-Server)을 통해 보안성 확보.

### 4. 대용량 트래픽 환경에 대비한 아키텍처 개선 ([Java 17]WebClient -> Java 21 Virtual Threads)
- **Challenge**: VUser 10,000명 규모의 k6 부하 테스트 시, 기존 동기(Blocking) 방식에서는 Tomcat의 OS 스레드 풀 고갈 및 포트 고갈(TCP RST)로 인한 시스템 장애가 발생함을 확인했습니다.
- **Action (Phase 1)**: 이를 개선하고자 Netty 기반의 WebClient(Non-blocking I/O)를 도입하여 적은 스레드로 트래픽 병목을 해소할 수 있음을 데이터로 검증했습니다.
- **Action (Phase 2 - Refactoring)**: 하지만 비동기 코드 특유의 복잡성과 디버깅의 어려움이 장기적인 유지보수에 부담이 될 수 있음을 인지했습니다. 이에 대안을 모색하던 중, **Java 21 (Eclipse Temurin)로 환경을 업그레이드하고 Spring Boot의 가상 스레드를 도입**하는 방안을 적용했습니다.
- **Result**: 동기식(RestClient)의 직관적인 코드 구조를 유지하면서도, I/O 대기 시 OS 스레드가 아닌 수만 개의 가벼운 가상 스레드(tomcat-handler)가 동작하도록 아키텍처를 개선했습니다. 결과적으로 **비동기 방식에 준하는 트래픽 처리량(Throughput)을 확보함과 동시에, 팀 단위 협업에 유리한 코드의 유지보수성을 함께 고려하는 엔지니어링 경험**을 쌓을 수 있었습니다.

### 5. 분산 환경에서의 동시성 제어
- **Challenge**: 다수의 사용자가 동시에 자원(포인트/잔액)에 접근할 때 발생하는 갱신 손실(Lost Update) 버그 방어.
- **Action**: 100개의 쓰레드가 동시에 출금 API를 호출하는 테스트 환경을 구축하여 동시성 버그를 로컬에서 재현. 이후, 분산 환경에서도 안전한 Redis 기반의 분산 락(Redisson)을 도입.
- **Result**: 임계 영역을 완벽히 보호하여 데이터 정합성 100%를 보장함과 동시에, 시스템 혼잡 시 무한 대기를 방지하기 위한 Lock의 Wait Time 튜닝을 통해 트래픽 유량 제어 최적화 달성.

### 6. 비동기 이벤트 드리븐 아키텍처 및 장애 내성 구축
- **Challenge**: 식단 저장 요청이 폭주하거나 AI 서버(Django) 장애 발생 시, 데이터가 유실되고 시스템 전체의 장애로 전파되는 위험 존재.
- **Action**: Spring Boot와 Django 사이에 **Apache Kafka**를 도입하여 결합도를 낮추고 비동기 처리 환경 구축. 클라이언트 UI에 Kafka 토글 스위치를 구현하여, 트래픽 상황에 따라 동기(UX 중심)/비동기(서버 생존 중심) 모드를 전환할 수 있도록 설계.
- **Result**: Django 서버의 일시적 다운타임 시에도 데이터가 유실되지 않도록 DefaultErrorHandler를 통한 재시도(Back-off: 2초 간격 10회) 로직을 구현하여 시스템의 **장애 내성**을 테스트했습니다. 이를 통해 메시지 큐의 안정성을 확인했으며, 장기 장애에 대비한 DLQ(Dead Letter Queue) 도입의 필요성을 도출했습니다.

---

## Tech Stack

### Backend & AI
- **Eclipse Temurin 21, Spring Boot 3.5.12**
- **Spring MVC + Virtual Threads (Project Loom)**
- **Spring WebFlux (WebClient)**
- **Spring Security 6, RestClient**
- **Apache Kafka**
- **Python 3.9, Django**
- **YOLOv8 (Object Detection)**

### Frontend
- **Vue.js 3, Axios, SweetAlert2**

### Database & DevOps
- **Oracle 11g, JPA (Hibernate)**
- **Git / GitHub**

---

## Preview
- **로그인**: 미리 정의된 DB 계정을 통한 세션 기반 인증.
- **이미지 분석**: 웹캠 촬영 및 파일 업로드 이미지를 통한 음식 탐지 및 판독.
- **결과 저장**: 분석된 영양 정보와 식단 이미지를 사용자별 매핑하여 Oracle DB 저장.

---

## Prerequisites & How to Run
- **Backend (Spring Boot):** application.yml에 Oracle DB 계정 정보 설정 후 8082 포트로 실행
- **AI Server (Django):** 가상환경에서 pip install -r requirements.txt 후 python manage.py runserver 0.0.0.0:9000 실행(오라클 계정 오류시 views.py의 connection_str변수에 오라클 계정 정보 설정 필요)
- **Frontend (Vue.js):** npm install -> npm run serve (8081 포트)
- **Database Setup:** 원활한 프로젝트 구동 및 AI 추론 결과 매핑을 위해, 최상위 database/ 폴더 내의 init_database.sql 스크립트를 실행하여 4개의 테이블 스키마와 400개의 기본 영양소 데이터 및 테스트용 User 계정을 생성해 주세요.
- **Docker Desktop 설치**
- **Kafka 서버 실행(비동기 메시지 큐 용도):** 최상위 폴더에서 다음 명령을 실행
> docker-compose -f docker/kafka/docker-compose.yml up -d

### How to Run Performance & Concurrency Tests (k6)
본 프로젝트는 대용량 트래픽 방어와 데이터 정합성 보장을 직접 검증할 수 있는 k6 부하 테스트 스크립트를 제공합니다.

**1. 준비 사항**
- k6 설치(https://k6.io/docs/get-started/installation/)
- Redis 서버 실행(분산 락 테스트 용도): docker run -d -p 6379:6379 redis
- Spring Boot 서버 실행 (Port: 8082)

**2. 트래픽 방어 테스트(Blocking vs Non-blocking)**
- Tomcat 쓰레드 고갈 및 Netty(WebClient)의 비동기 처리 성능 차이를 확인합니다.

- VUser 10,000명 동시 접속 테스트 실행(에러 발생시 load-test.js에 localhost, 127.0.0.1 대신 현재 사용 중인 내부 IP나 외부 IP로 설정 필요)
> k6 run k6-test/load-test.js

**3. Redis 분산 락 동시성 제어 테스트 (Lost Update 방어)**
- 100명의 유저가 동시에 출금 API를 호출할 때, Redis 분산 락(Redisson)이 갱신 손실 버그를 막고 잔액 무결성을 보장하는지 확인합니다.

- 초기 잔액 10,000원 세팅
> curl http://localhost:8082/api/bank/reset

- 100명 동시 100원 출금 테스트 실행(에러 발생시 bank-test.js에 localhost, 127.0.0.1 대신 현재 사용 중인 내부 IP나 외부 IP로 설정 필요)
> k6 run k6-test/bank-test.js
