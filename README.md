# 오늘의 식단 기록
> **AI 기반 식단 이미지 분석 및 영양 정보 관리 시스템**
> 
> 단순한 AI 모델 구현을 넘어, **BFF(Backend For Frontend) 아키텍처**를 도입하고 시스템 간 **통신 프로토콜 최적화**를 통해 엔드투엔드 서비스를 구축한 프로젝트입니다.

---

## System Architecture
현대적인 마이크로서비스 구조를 지향하며, 클라이언트와 AI 서버 간의 결합도를 낮추기 위해 Spring Boot를 API Gateway 겸 BFF로 배치했습니다.

**Vue.js (Frontend) <-> Spring Boot (BFF/Auth) <-> Django (AI Inference) <-> Oracle DB**

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
- **Result**: 학습 속도를 기존 대비 **300% 이상 향상** (1일 100GB -> 300GB 처리).

### 3. BFF 아키텍처 및 보안 강화
- **Design**: AI 서버(Django)를 비즈니스 로직과 분리하여 추론 전용 워커로 구성하고, Spring Boot를 통해 통합 인증 및 라우팅을 수행.
- **Security**: Spring Security 6를 활용하여 세션 기반 인증 구현. 클라이언트의 민감한 정보 없이 서버 간 통신(Server-to-Server)을 통해 보안성 확보.

---

## Engineering Insight
> "프레임워크가 제공하는 편리함 뒤에 숨겨진 HTTP 프로토콜의 동작 원리를 깊게 이해하는 계기가 되었습니다. 특히 WSGI와 ASGI의 스펙 차이로 발생하는 통신 이슈를 해결하며, 인프라 환경에 최적화된 전송 전략을 설계하는 능력을 길렀습니다."

---

## Tech Stack

### Backend & AI
- **Java 17, Spring Boot 3.5.12, Spring Security 6**
- **Spring WebFlux (WebClient)**
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
- **AI Server (Django):** 가상환경에서 pip install -r requirements.txt 후 python manage.py runserver 0.0.0.0:9000 실행
- **Frontend (Vue.js):** npm install -> npm run serve (8081 포트)
- **Database Setup:** 원활한 프로젝트 구동 및 AI 추론 결과 매핑을 위해, 최상위 database/ 폴더 내의 init_database.sql 스크립트를 실행하여 4개의 테이블 스키마와 400개의 기본 영양소 데이터 및 테스트용 User 계정을 생성해 주세요.