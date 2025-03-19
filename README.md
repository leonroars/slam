# 💃 SLAM! : 콘서트 예약 서비스

## 1. Description
- **`콘서트 예약 서비스`** 를 구현해 봅니다.
- 대기열 시스템을 구축하고, 예약 서비스는 작업가능한 유저만 수행할 수 있도록 해야합니다.
- 사용자는 좌석예약 시에 미리 충전한 잔액을 이용합니다.
- 공평한 좌석 선점 기회 제공을 위해 한 사용자가 좌석 선택 시, 해당 시점으로부터 5분 간만 다른 사용자의 좌석 선점을 방지합니다. 5분 지날 경우 해당 좌석은 다시 선점 가능 상태가 됩니다!


## 2. Requirements
- 아래 5가지 API 를 구현합니다.
  - 유저 토큰 발급 API
  - 예약 가능 날짜 / 좌석 API
  - 좌석 예약 요청 API
  - 잔액 충전 / 조회 API
  - 결제 API
- 다수의 인스턴스로 어플리케이션이 동작하더라도 기능에 문제가 없도록 작성하도록 합니다.
- 동시성 이슈를 고려하여 구현합니다.
- 대규모 트래픽 상황에서의 유량 제어 방법으로 대기열을 선택하여 구현합니다.

## 3. 설계

### 3.1 프로젝트 마일스톤
<img width="1665" alt="Screenshot 2025-01-03 at 6 24 29 AM" src="https://github.com/user-attachments/assets/ef289e50-9090-4f0a-8de5-60e8a89af361" />

### 3.2 [요구사항 분석](https://github.com/leonroars/hhp7-concert-reservation/wiki/요구사항-분석)
연결된 문서는 요구사항을 분석하며 개략적인 설계 방향 과정에 대한 고민을 담은 문서입니다.
프로젝트 개발 주기에서의 _요구 사항 분석_ 결과는 아래의 UML 입니다.

### 3.3 [UML : Flow Chart & Sequence Diagrams](https://github.com/leonroars/hhp7-concert-reservation/wiki/UML-:-Flow-Chart-&-Sequence-Diagrams)
요구사항 분석의 결과물로서 산출된 UML입니다.
- 사용자 관점에서 서비스 진입 시부터 사용 종료 시까지의 흐름을 개략적으로 파악할 수 있도록 플로우 차트를 작성하였습니다.
- 시퀀스 다이어그램 작성을 통해 개별적인 비즈니스 요구 사항들이 내부적으로 어떤 상호작용을 통해 이루어지는지 표현하였습니다.

### 3.4 API Specification
현 프로젝트의 API 명세 및 문서화는 Swagger를 활용하여 진행하였습니다.
프로젝트 클론 및 실행 로컬 환경 내 Docker 실행 후 다음의 주소로 접속 시 명세를 확인하실 수 있습니다.
> Swagger API UI : http://localhost:8080/swagger-ui.html
