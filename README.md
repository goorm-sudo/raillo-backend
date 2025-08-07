# 🚅 Raillo

## 🙌🏻 멤버
<table>
  <tbody>
    <tr>
      <td align="center">
        <a href="https://github.com/Ogu1208"><img src="https://avatars.githubusercontent.com/u/76902448?v=4" width="100px;" alt="김민아"/><br /></a>
      </td>
      <td align="center">
        <a href="https://github.com/EndlessMilkyway"><img src="https://avatars.githubusercontent.com/u/26517746?v=4" width="100px;" alt="김영렬"/><br /></a>
      </td>
      <td align="center">
        <a href="https://github.com/karlislepark"><img src="https://avatars.githubusercontent.com/u/64067168?v=4" width="100px;" alt="박범진"/><br /></a>
      </td>
      <td align="center">
        <a href="https://github.com/Jimin730"><img src="https://avatars.githubusercontent.com/u/108002997?v=4" width="100px;" alt="신지민"/><br /></a>
      </td>
      <td align="center">
        <a href="https://github.com/Friox"><img src="https://avatars.githubusercontent.com/u/10986386?v=4" width="100px;" alt="이승훈"/><br /></a>
      </td>
      <td align="center">
        <a href="https://github.com/chanwonlee"><img src="https://avatars.githubusercontent.com/u/116537544?v=4" width="100px;" alt="이찬원"/><br /></a>
      </td>
      <td align="center">
        <a href="https://github.com/Yunsung-Jo"><img src="https://avatars.githubusercontent.com/u/135187534?v=4" width="100px;" alt="조윤성"/><br /></a>
      </td>
    </tr>
    <tr>
      <td align="center"><a href="https://github.com/Ogu1208">김민아</a></td>
      <td align="center"><a href="https://github.com/EndlessMilkyway">김영렬</a></td>
      <td align="center"><a href="https://github.com/karlislepark">박범진</a></td>
      <td align="center"><a href="https://github.com/Jimin730">신지민</a></td>
      <td align="center"><a href="https://github.com/Friox">이승훈</a></td>
      <td align="center"><a href="https://github.com/chanwonlee">이찬원</a></td>
      <td align="center"><a href="https://github.com/Yunsung-Jo">조윤성</a></td>
    </tr>
  </tbody>
</table>

## 📋 목차
- [프로젝트 개요](#-프로젝트-개요)
- [기술 스택](#-기술-스택)
- [아키텍처](#-아키텍처)
- [주요 기능](#-주요-기능)
- [모니터링 & 운영](#-모니터링--운영)
- [테스트](#-테스트)
- [주요 엔드포인트](#-주요-엔드포인트)

## 📖 프로젝트 개요
**Raillo**는 코레일(KORAIL) 예매 시스템을 클론코딩한 기차 예약 플랫폼으로,  
실제 서비스의 핵심 기능들을 최대한 유사하게 구현하여 현업에서 사용되는 기술 스택과 설계 패턴을 학습하고 적용한 프로젝트입니다.

### 📅 진행 기간
- 1차 (기획 및 개발) : 2025. 05. 28. ~ 2025. 07. 01.
- 2차 (클라이언트 요구사항 반영) : 2025. 07. 02 ~ 2025. 07. 15.
- 3차 (카카오 API 통합) : 2025. 07. 16. ~ 2025. 08. 08.
- 지속 개선 : 2025. 08. 09. ~ endless

### 🎯 핵심 목표
- **실제 서비스와 유사한 핵심 기능 구현** : 실제 코레일에서 제공하는 회원 인증 및 주요 예매 흐름을 최대한 비슷하게 구현
- **활발한 협업과 역할 분담 경험** : 팀원이 적절하게 역할을 분담하고, 협업툴을 이용한 버전관리, 이슈 트래킹, 코드 리뷰 등 협업 방식을 적용
- **실무에서 사용되는 기술 스택 학습 및 경험** : 실무에서 사용되는 다양한 기술 스택을 학습, 경험하고 관련 패턴을 적용

## 🔧 기술 스택
### Backend
[![backend](https://skillicons.dev/icons?i=java,spring,redis,mysql)](https://skillicons.dev)
- **Language** : Java
- **Framework** : Spring Boot, Spring Security, Spring Batch
- **ORM** : Spring Data JPA, QueryDSL
- **DB** : MySQL (Production), H2 (Test)
- **Cache** : Redis
- **Authentication** : JWT
- **File Processing** : Apache POI
- **Build Tool** : Gradle

### Infrastructure & DevOps
[![infra,devops](https://skillicons.dev/icons?i=git,github,docker,aws,prometheus,grafana,githubactions)](https://skillicons.dev)
- **Cloud Platform** : AWS (EKS, RDS, Route53, Load Balancer)
- **Container** : Docker, Kubernetes
- **CI/CD** : GitHub Actions, ArgoCD (GitOps)
- **Monitoring** : Prometheus, Grafana
- **VCS** : Git, GitHub

### Testing
- **Framework** : JUnit, Spring Boot Test
- **DB** : H2 (in-memory)
- **Cache** : Embedded Redis
- **Test Utils** : AssertJ
- **Email Testing** : GreenMail
- **Coverage** : JaCoCo

## 🏗️ 아키텍처
<img width="1920" alt="Raillo-Server-Architecture" src="https://github.com/user-attachments/assets/d5ca3e3d-c5bc-497a-aa31-83c1ba7fb65e" />

### 도메인 주도 설계 (DDD)
```
src/main/java/com/sudo/railo/
├── auth/       # 인증 도메인
├── booking/    # 예약 도메인  
├── global/     # 공통 기능
├── member/     # 회원 도메인
├── payment/    # 결제 도메인
└── train/      # 열차 도메인
```

## 🚀 주요 기능
### 🔑 Auth 도메인
- **JWT 기반 인증 시스템** : Access Token과 Refresh Token을 활용한 Stateless 인증 및 인가
- **이메일 인증** : Redis를 활용한 인증 코드 발송 및 검증 시스템
- **보안 강화** : 로그아웃된 토큰 Redis 관리, 쿠키 기반 Refresh Token 관리

### 👤 Member 도메인
- **고유 회원번호 시스템** : Redis 기반 일일 증분 카운터를 활용한 회원번호 자동 생성 (YYYYMMDDCCCC 형식)
- **Soft-Delete** : 실제 회원 삭제가 아닌 비활성화 처리
- **만료 회원 일괄 삭제** : 만료된 회원 데이터 정리를 위한 Spring Batch 활용

### 🎫 Booking 도메인
- **장바구니 시스템** : 예약 후 결제 전 임시 저장 및 관리 기능
- **좌석 예약 관리** : 승객 유형별 좌석 배정 및 예약 상태 관리
- **요금 계산** : 거리별, 승객 유형별, 차량 등급별 요금 자동 계산

### 💵 Payment 도메인
- **결제 수단 확장을 위한 유연한 구조** : 절차를 분리함으로써 추후 결제 수단 확장 용이함
- **결제 키 생성** : 고유한 결제 식별자 자동 생성
- **결제 검증** : 금액 검증 및 중복 결제 방지
- **자동 티켓 발급** : 결제 완료 시 티켓 생성
- **취소 및 환불** : 결제 취소 및 환불 처리 시스템

### 🚅 Train 도메인
- **실제 데이터 활용** : 코레일의 실제 운영 스케줄 Excel 파일을 파싱하여 데이터 구축
- **엑셀 데이터 파싱** : Apache POI를 활용한 복잡한 스케줄 데이터 자동 파싱
- **열차 검색 최적화** : 배치 쿼리를 활용한 대용량 스케줄 검색 성능 최적화
- **좌석 현황 관리** : 실시간 좌석 예약 현황 및 여유석 정보 제공
- **역간 요금 시스템** : 구간별 세분화된 요금 체계 구현

## 📊 모니터링 & 운영
- `GitHub Actions`와 `ArgoCD`를 활용해 코드 변경 시 자동 빌드, 배포, 클러스터 적용
- `AWS EKS` 기반에서 모든 구성요소를 컨테이너로 관리
- `Prometheus`, `Grafana`등을 도입하여 노드 별 서비스 상태, 리소스 사용량을 실시간으로 수집 및 가시화
- 여러 `Node Group`과 분산된 백엔드 및 Redis로 구성되어 고가용성 확보
- `RDS`, `Route53`, `LB` 등 `AWS 서비스`를 사용하여 데이터 관리와 트래픽 분산 및 도메인 운영 지원

## 🧪 테스트
테스트 방법은~

## 📌 주요 엔드포인트
```http
# Auth
<METHOD> <URL>

# Booking
<METHOD> <URL>

# Member
<METHOD> <URL>

# Payment
<METHOD> <URL>

# Train
<METHOD> <URL>
```
