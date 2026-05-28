# MCP 강의자료 설계서

**작성일**: 2026-05-28
**대상 강의**: 1차시(90~120분), 주니어 백엔드 개발자 대상 MCP 입문
**산출물**: 슬라이드 + GitHub 코드 저장소 (4컴포넌트 동작 코드)

---

## 1. 목표와 학습 결과

### 대상

주니어 백엔드 개발자. REST API는 만들어봤지만 MCP/LLM 도구 연동은 처음.

### 90~120분 후 학생이 할 수 있어야 하는 것

1. MCP의 4가지 컴포넌트(Host / Client / MCP Server / Resource Server)와 각자의 역할을 자기 말로 설명할 수 있다.
2. **"Client는 LLM이 아니다, JSON-RPC 통신 라이브러리다"** 라는 사실을 코드로 본 후 이해한다.
3. 자신이 만든 Resource Server를 MCP Server로 감싸서 Claude Code에 노출시킬 수 있다.
4. JSON-RPC `tools/list`, `tools/call` 메시지가 실제로 어떻게 흐르는지 시각화한다.

### 비목표 (이번 1차시에서 다루지 않는 것)

- HTTP/SSE transport (stdio만 다룸. 마지막 5분에 "다음 단계"로 언급만)
- prompts, resources (tools만 다룸)
- 원격 MCP, 인증/보안
- Claude API 직접 호출 (Host는 Claude Code만 사용, Pro 요금제로 커버)

---

## 2. 시스템 아키텍처

```
┌────────────────────────────────┐
│  Host = Claude Code CLI        │  ← Claude Pro $20 요금제로 동작
│  (LLM + 내장 MCP Client)        │     별도 API 결제 X
└──────────────┬─────────────────┘
               │ stdio (JSON-RPC)
               ↓
┌────────────────────────────────┐     ┌─────────────────────────┐
│  ② MCP Server                  │     │  ① 학습용 Client        │
│  Spring Boot + MCP Java SDK    │ ←───┤  Java (직접 구현)       │
│  @Tool 어노테이션 5개           │     │  JSON-RPC 손으로 짜기   │
└──────────────┬─────────────────┘     └─────────────────────────┘
               │ HTTP REST
               ↓
┌────────────────────────────────┐
│  ③ Resource Server             │
│  Spring Boot REST API          │
│  Todo/메모 CRUD                │
│  ConcurrentHashMap 저장소      │
└────────────────────────────────┘
```

### 핵심 설계 의도

- **①과 Host(Claude Code)는 같은 MCP Server(②)에 둘 다 붙는다.** 이게 MCP 표준화의 강력함을 보여주는 그림.
- **①은 실전 도구가 아니라 학습 도구**다. "Postman을 직접 짜는" 셈. stdin/stdout으로 JSON-RPC 메시지를 보내고 응답을 출력함으로써 학생이 "MCP 프로토콜의 실체"를 눈으로 본다.
- Host는 Claude Code. 자연어 처리는 Claude가 한다. 학생 코드에는 **LLM 호출이 한 줄도 없다**.

---

## 3. 도메인: Todo / 메모

### Resource Server REST API

| Method | Path | 설명 | 요청/응답 |
|---|---|---|---|
| GET | `/todos` | 전체 조회 | `[{id, title, memo, completed}]` |
| GET | `/todos/{id}` | 단건 조회 | `{id, title, memo, completed}` |
| POST | `/todos` | 추가 | 요청 `{title, memo}` → 응답 `{id, ...}` |
| PATCH | `/todos/{id}/complete` | 완료 처리 | 응답 `{id, ..., completed: true}` |
| GET | `/todos/search?q=...` | 검색 (title/memo) | `[{...}]` |

### MCP Server tool (5개)

| tool 이름 | 입력 스키마 | 호출하는 REST | 자연어 예시 |
|---|---|---|---|
| `list_todos` | (없음) | GET /todos | "할일 목록 보여줘" |
| `get_todo` | `{id: number}` | GET /todos/{id} | "1번 할일 뭐였지" |
| `add_todo` | `{title: string, memo?: string}` | POST /todos | "MCP 공부하기 추가해줘" |
| `complete_todo` | `{id: number}` | PATCH /todos/{id}/complete | "1번 끝냈어" |
| `search_todos` | `{query: string}` | GET /todos/search | "MCP 관련 할일 찾아줘" |

### 데이터 모델

```java
public record Todo(
    Long id,
    String title,
    String memo,
    boolean completed
) {}
```

저장: `ConcurrentHashMap<Long, Todo>` (in-memory, DB 설치 부담 없음). ID는 `AtomicLong`으로 발급.

---

## 4. 강의 흐름 (105분 기준)

| 시간 | 섹션 | 내용 | 화면에 띄울 것 |
|---|---|---|---|
| **0~15분** | 도입 + MCP 개념 | "왜 MCP인가" — Function calling vs MCP. 4컴포넌트 그림. Client의 진짜 역할(LLM 아님). | 슬라이드 |
| **15~30분** | ③ Resource Server | Spring Boot 프로젝트 둘러보기. REST API 동작 확인 (curl/HTTPie). | IDE + 터미널 |
| **30~55분** | ② MCP Server | Spring AI MCP starter 의존성, `@Tool` 어노테이션, stdio transport 설정. 5개 tool 등록. 빌드 후 실행. | IDE + 터미널 |
| **55~80분** | ① 학습용 Client | **하이라이트**. `ProcessBuilder`로 ②를 자식 프로세스로 띄우고 stdin/stdout으로 JSON-RPC 직접 송수신하는 Java 코드. `tools/list`, `tools/call` 메시지를 학생이 눈으로 본다. | IDE + 콘솔 출력 |
| **80~100분** | ④ Host 연결 (Claude Code) | Claude Code 설정파일(`.mcp.json`)에 ② 등록 → 터미널에서 "오늘 할일 보여줘" 자연어 시연. **"내가 만든 게 진짜 AI랑 붙었다" 감동 포인트.** | Claude Code 세션 |
| **100~105분** | 마무리 + Q&A | 다음 단계 안내 (HTTP transport, prompts/resources, 원격 MCP). | 슬라이드 |

### 분량 조절 안전장치

- 60~80분 클라이언트 섹션에서 시간이 모자라면 → `tools/list`만 보여주고 `tools/call`은 코드 리딩으로 대체.
- 시간이 남으면 → 학생이 직접 6번째 tool(`delete_todo`)을 추가하는 실습.

---

## 5. 기술 스택 세부

| 항목 | 선택 | 이유 |
|---|---|---|
| Java | 21 (LTS) | record, pattern matching으로 코드 짧게 |
| 빌드 | Gradle (Kotlin DSL) | Spring Boot 친화, 한국 환경 표준 |
| Spring Boot | 3.3.x | 최신 LTS |
| MCP SDK | `spring-ai-starter-mcp-server` (Spring AI 1.0+) | `@Tool` 어노테이션 기반, 1차시 분량 적합 |
| Transport | stdio | Claude Code 연동 표준, HTTP보다 단순 |
| 저장소 | `ConcurrentHashMap` | DB 설치 부담 0 |
| HTTP 클라이언트 (②→③) | Spring `RestClient` | Spring Boot 3.2+ 표준 |
| JSON | Jackson (Spring Boot 내장) | 표준 |
| Client 구현 (①) | 표준 라이브러리 + Jackson | "SDK 없이 손으로 짜는" 학습 효과 |

### 실행 방법

```bash
# 터미널 1: Resource Server
cd 01-resource-server && ./gradlew bootRun
# → http://localhost:8080

# 터미널 2: 학습용 Client가 MCP Server를 자식 프로세스로 띄움
cd 03-client && ./gradlew run
# → "tools/list" 메시지가 콘솔에 보임

# Claude Code 연동
# Claude Code의 MCP 설정에 02-mcp-server 등록 (정확한 경로는
# OS/Claude Code 버전마다 다름 — README에 환경별로 안내)
```

---

## 6. 산출물 디렉토리 구조

```
mcp-exam/
├── README.md                          # 실행 순서, Claude Code 연동 가이드, 트러블슈팅
├── docs/
│   ├── slides/
│   │   └── mcp-lecture.md             # Marp 마크다운 슬라이드
│   └── superpowers/specs/
│       └── 2026-05-28-mcp-lecture-design.md  # 이 문서
├── 01-resource-server/                # Spring Boot REST
│   ├── build.gradle.kts
│   └── src/main/java/com/example/resource/
│       ├── ResourceServerApplication.java
│       ├── Todo.java
│       ├── TodoController.java
│       └── TodoRepository.java        # ConcurrentHashMap
├── 02-mcp-server/                     # Spring Boot + MCP SDK
│   ├── build.gradle.kts
│   └── src/main/java/com/example/mcp/
│       ├── McpServerApplication.java
│       ├── TodoTools.java             # @Tool 5개 메서드
│       └── TodoRestClient.java        # → 01-resource-server 호출
├── 03-client/                         # Java CLI (학습용)
│   ├── build.gradle.kts
│   └── src/main/java/com/example/client/
│       ├── LearningClient.java        # main, REPL 루프
│       ├── McpProcess.java            # ProcessBuilder로 ② 띄우기
│       └── JsonRpc.java               # 메시지 빌더 + 파서
└── .gitignore
```

### 슬라이드 (Marp 마크다운) 챕터 구성

1. MCP가 뭐고 왜 필요한가 (Function calling 한계, 표준화의 가치)
2. 4컴포넌트 그림 + 각 역할
3. **"Client = 통신 라이브러리, LLM 아님"** (가장 강조할 슬라이드)
4. JSON-RPC 메시지 예시 (tools/list, tools/call)
5. 우리가 만들 것 (3개 컴포넌트 + Claude Code 연결)
6. 도메인 소개 (Todo, 5개 tool)
7. (이후는 라이브 코딩으로 전환)
8. (마지막) 다음 단계 — HTTP transport, prompts, resources, 원격 MCP

---

## 7. 핵심 교육 포인트 (강의 중 반복할 메시지)

1. **"Client = 통신 라이브러리, LLM 아님"** — 학생들이 가장 헷갈리는 부분. 우리 학습용 Client 코드에 LLM 호출이 한 줄도 없다는 사실을 강조.
2. **"MCP Server는 결정론적 서버"** — JSON-RPC 들어오면 JSON-RPC로 응답. LLM 호출 없음.
3. **"자연어 처리는 Host의 책임"** — 우리 코드 어디에도 Claude API 호출 없음. Claude Code가 자기 안에서 처리.
4. **"같은 MCP Server에 학습용 Client와 Claude Code가 둘 다 붙는다"** — 표준화의 힘. tool 한번 만들면 어떤 MCP Host든 쓸 수 있음.

---

## 8. 위험 요소와 대응

| 위험 | 대응 |
|---|---|
| Spring AI 1.0 GA 안정성 (2026-05 시점 확인 필요) | 구현 단계에서 실제 의존성 버전 검증. 문제 시 `modelcontextprotocol/java-sdk` 직접 사용으로 fallback. |
| 학생 환경의 Java 21 미설치 | 사전 안내문에 명시. 백업으로 Java 17 호환 옵션 준비. |
| Claude Code 미설치/로그인 안 됨 | 강의 시작 30분 전 환경 점검 시간 안내. |
| stdio transport 디버깅 어려움 (눈에 안 보임) | ② 실행 시 별도 로그 파일에 들어오는 JSON-RPC 메시지를 기록하도록 설정 (학생이 tail로 따라가게). |
| 105분 안에 다 못 끝남 | §4의 "분량 조절 안전장치" 적용. ① 섹션을 가장 먼저 줄임. |

---

## 9. 다음 단계

1. 이 설계서 사용자 리뷰 및 승인
2. `superpowers:writing-plans` 스킬로 구현 계획서 작성 (어떤 순서로 4개 컴포넌트 코드 + 슬라이드를 만들지)
3. `superpowers:executing-plans` 또는 직접 구현으로 코드 작성
4. 실제로 90분 안에 다 돌아가는지 dry-run
