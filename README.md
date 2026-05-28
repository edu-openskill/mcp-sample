# MCP 입문 강의자료 — 3 시나리오

주니어 백엔드 개발자 대상 MCP(Model Context Protocol) 강의자료.
세 가지 운영 시나리오를 독립 폴더로 구성. 각각 90~120분 1차시 분량.

> Spring Boot + Spring AI MCP starter 1.0.3 + Claude Code

---

## 시나리오 한눈에

| 시나리오 | Resource | Transport | 사용자 | 핵심 깨달음 |
|---|---|---|---|---|
| [`01-personal/`](./01-personal/) | 내 것 | stdio | 1명 (나) | Host와 같은 머신, 부모-자식 프로세스 |
| [`02-external/`](./02-external/) | 외부 (JSONPlaceholder) | stdio | 1명 (나) | MCP Server는 외부 API의 어댑터 |
| [`03-organization/`](./03-organization/) | 내 것 + 격리 | HTTP/SSE | 여러 명 (alice/bob) | MCP Server도 멀티유저 SaaS가 될 수 있음 |

---

## 권장 학습 순서

**01-personal → 02-external → 03-organization** (폴더 번호 순)

각 시나리오는 자기 폴더 안에 자체 슬라이드·README·실행 가이드가 있다. 강사가 하나만 골라서 수업해도 OK.

---

## 빠른 시작 (Quick Start)

### 사전 준비

```bash
git clone https://github.com/edu-openskill/mcp-sample.git
cd mcp-sample
```

### 환경 확인

- **Java 21** (JDK) — `java -version`이 `21`로 시작해야 합니다
- **Claude Code CLI** — `claude --version` (Pro 요금제 권장)
- **Gradle**: 각 시나리오 폴더에 wrapper 포함, 별도 설치 불필요

### `.mcp.json` 경로 수정 (필수)

각 시나리오의 `.mcp.json`에는 jar 경로 placeholder가 있어요. 자기 환경 절대경로로 교체:

```
"/ABSOLUTE/PATH/TO/mcp-sample/01-personal/mcp-server/build/libs/mcp-server.jar"
                ↑ 여기를 자기 clone 위치로
```

- macOS/Linux 예: `/Users/you/code/mcp-sample/01-personal/mcp-server/build/libs/mcp-server.jar`
- Windows 예: `C:\\Users\\you\\code\\mcp-sample\\01-personal\\mcp-server\\build\\libs\\mcp-server.jar` (백슬래시 두 개, JSON 이스케이프)

### 첫 시나리오 실행 (01-personal)

```bash
# 터미널 1 — Resource Server
cd 01-personal/resource-server
./gradlew bootRun

# 터미널 2 — MCP Server jar 빌드 (한 번만)
cd 01-personal/mcp-server
./gradlew bootJar

# 터미널 2 — Claude Code 연결
cd 01-personal
claude
> 할일 목록 보여줘
> "MCP 공부하기" 추가해줘
```

다음 시나리오는 각 폴더 README 참조.

---

## 강의 시나리오 (각 90~120분)

### 01-personal — 내 시스템 + 1인 사용 (입문)

**핵심 메시지**: "Client는 LLM이 아니다", "stdio = 부모-자식 프로세스"

| 분 | 단계 |
|---|---|
| 0~15 | 도입 + MCP 4컴포넌트 |
| 15~35 | Resource Server (Spring REST + 인메모리) |
| 35~70 | MCP Server (`@Tool` 5개, stdio 설정) |
| 70~95 | Claude Code 연결 + 자연어 시연 ⭐ |
| 95~105 | 다음 시나리오 예고 + Q&A |

학생이 만드는 것: **Resource Server + MCP Server (2개)**.

### 02-external — 외부 공개 API 어댑터

**핵심 메시지**: "01-personal과 99% 같은 코드 — base-url만 다름"

| 분 | 단계 |
|---|---|
| 0~10 | 01 복습 + 오늘 목표 |
| 10~25 | JSONPlaceholder 둘러보기 (curl) |
| 25~70 | MCP Server (01-personal과 diff로 비교) |
| 70~95 | Claude Code 연결 + 자연어 시연 |
| 95~105 | 다음 시나리오 예고 + Q&A |

학생이 만드는 것: **MCP Server만 (1개)**. Resource는 외부 (JSONPlaceholder).

### 03-organization — 사내 공식 통합 + 멀티유저

**핵심 메시지**: "MCP Server도 일반 서버다", "Bearer가 모든 계층 통과"

| 분 | 단계 |
|---|---|
| 0~10 | 01/02 복습 + 오늘 목표 |
| 10~35 | Resource Server (userId, Interceptor, 격리) |
| 35~70 | MCP Server HTTP transport + Auth forward |
| 70~95 | alice/bob 두 세션 multi-tenant 시연 ⭐ |
| 95~105 | 운영 고려사항 (JWT/OAuth, HTTPS) + Q&A |

학생이 만드는 것: **Resource Server (인증 추가) + MCP Server (HTTP transport)**.

---

## 강사용 진행 팁

- **사전 환경 점검 30분**: Java 21, Claude Code 설치, repo clone, jar 빌드까지 미리 확인. 강의 중 환경 트러블슈팅은 시간 잡아먹음.
- **라이브 코딩 vs 코드 리딩**: 1차시 분량이라 학생이 다 따라치기 어려움. 핵심 부분만 라이브, 나머지는 repo로 보여주기.
- **클라이맥스는 Claude Code 자연어 시연**: 70~95분 구간을 충분히 확보. 시간 모자라면 다른 섹션을 줄이지 말고 학습용 Client 코드 리딩 같은 부수적 부분을 줄여라.
- **01 → 02 비교**: `diff -r 01-personal/mcp-server/src 02-external/mcp-server/src`를 강의 중 실제로 띄워서 "거의 같다"는 사실을 눈으로 보여줘라. 학습 전이 효과 큼.
- **03 multi-tenant 시연 시간 분리**: alice/bob 두 세션 동시 운영이 어려우면 시간 분리 (alice 끝내고 → bob). 격리되는 사실 보여주는 게 핵심.

---

## 학생용 사전 안내

강의 24시간 전 학생에게:

1. `java -version`이 `21`인지 확인 (없으면 [Eclipse Temurin](https://adoptium.net/) 또는 [Oracle JDK 21](https://www.oracle.com/java/technologies/downloads/#java21))
2. `claude --version` 동작 확인 ([Claude Code 설치](https://docs.claude.com/en/docs/claude-code))
3. `git clone https://github.com/edu-openskill/mcp-sample.git` 미리 받기
4. 각 시나리오 폴더의 `mcp-server`에서 `./gradlew bootJar` 미리 한 번 실행 (의존성 다운로드 시간 절약)

---

## 필요 환경 (전 시나리오 공통)

- Java 21 (JDK)
- Gradle 8.x (각 폴더 wrapper 포함, 별도 설치 불필요)
- Claude Code CLI (Pro 요금제 권장)
- 선택: Marp (슬라이드 미리보기), VS Code REST Client (`.http` 파일)

---

## 참고

- 설계서: [`docs/superpowers/specs/2026-05-28-mcp-lecture-3scenarios.md`](docs/superpowers/specs/2026-05-28-mcp-lecture-3scenarios.md)
- 구현 계획서: `docs/superpowers/plans/2026-05-28-*.md`
- Spring AI MCP 문서: https://docs.spring.io/spring-ai/reference/api/mcp/
- MCP 명세: https://modelcontextprotocol.io/

---

## 라이선스

[MIT License](./LICENSE) — 자유롭게 강의·교육 용도로 사용·변형 가능.
