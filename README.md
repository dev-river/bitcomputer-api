# bitcomputer-api

비트컴퓨터 사내 직원 관리 시스템 백엔드 (Spring Boot 3 + MyBatis + PostgreSQL).

## 로컬 실행

1. PostgreSQL을 로컬에 띄우고 `bitcomputer` 데이터베이스를 생성합니다.
2. 아래 환경변수를 설정합니다 (`.env` 또는 IDE run config):

| 변수 | 설명 | 예시 |
|---|---|---|
| `DB_URL` | JDBC URL | `jdbc:postgresql://localhost:5432/bitcomputer` |
| `DB_USERNAME` | DB 사용자 | `postgres` |
| `DB_PASSWORD` | DB 비밀번호 | `postgres` |
| `AES_SECRET_KEY` | Base64로 인코딩된 32바이트(AES-256) 키 | `openssl rand -base64 32` 로 생성 |
| `JWT_SECRET` | Base64로 인코딩된 32바이트 이상 키 | `openssl rand -base64 32` 로 생성 |
| `BGC_API_BASE_URL` | Background Check API 주소 | `https://54capvm12g.execute-api.ap-northeast-2.amazonaws.com` |
| `FRONTEND_ORIGIN` | CORS 허용 프론트 도메인 | `http://localhost:5173` |

3. `./gradlew bootRun`

## 시드 계정

| 로그인ID | 초기 비밀번호 | 역할 |
|---|---|---|
| `ADMIN-001` | `ChangeMe123!` | 관리자 |
| `EMP-001` | `ChangeMe123!` | 직원 (최초 로그인 시 비밀번호 변경 필수) |

## 배포 (Railway)

1. Railway 프로젝트 생성 → PostgreSQL 플러그인 추가 (Railway가 `DATABASE_URL` 등을 자동 주입하므로 `DB_URL`/`DB_USERNAME`/`DB_PASSWORD`는 해당 값에 맞게 재매핑해서 설정)
2. 이 저장소를 연결하면 `Dockerfile`로 자동 빌드됨
3. 위 표의 환경변수를 Railway 서비스 Variables에 등록
4. 배포 후 헬스체크: `GET /auth/login`에 시드 계정으로 로그인 요청을 보내 200이 오는지 확인

## 측정값 플레이스홀더

`application.yml`의 `bgc.timeout-ms`, `bgc.retry-count`, `bgc.retry-interval-ms`, `bgc.poll-interval-ms`, `bgc.max-poll-retry-count`는 실제 Background Check API를 실측하기 전 임시값입니다. `MEASUREMENTS.md` 작성 후 이 값들을 실측 근거로 교체하세요.
