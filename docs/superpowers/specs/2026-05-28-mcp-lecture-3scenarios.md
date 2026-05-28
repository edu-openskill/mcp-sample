# MCP 강의자료 — 3 시나리오 재구조화 설계서

**작성일**: 2026-05-28 (재설계 1차)
**이전 설계**: [`2026-05-28-mcp-lecture-design.md`](./2026-05-28-mcp-lecture-design.md) (단일 시나리오)
**대상 강의**: 3차시 (각 90~120분), 주니어 백엔드 개발자 대상 MCP 입문 → 심화
**산출물**: 시나리오별 독립 폴더 — 코드 + 슬라이드 + README 세트 × 3

---

## 0. 재설계 동기

이전 단일 시나리오 설계는 "Client는 LLM이 아니라 통신 라이브러리"를 가르치기 위해 학생이 직접 JSON-RPC over stdio 클라이언트를 구현하는 구조였다. 그러나:

1. **Client는 보통 구현하지 않는다** — Claude Code/Desktop이 내장 구현 제공. 학습용 자체 Client는 교육 목적상 유효하지만 실전 패턴이 아니다.
2. **MCP Server 위치에 대한 mental model 혼란** — 학생들은 "MCP Server는 Resource Server 옆"이라고 직관적으로 생각하지만, stdio 모드에서는 Host 옆(같은 머신)에 있다.
3. **운영 시나리오의 다양성** — Resource가 외부인지 내부인지, 사용자가 1인인지 다수인지에 따라 배포 모델이 다르다. 단일 시나리오로는 못 다룬다.

→ 세 가지 시나리오로 재구조화. 각자 독립적으로 학습할 수 있게.

---

## 1. 학습 목표

**대상**: 주니어 백엔드 개발자. REST API는 만들어봤지만 MCP/LLM 도구 연동은 처음.

**3차시 후 학생이 할 수 있는 것**:

| 시나리오 | 끝났을 때 학생이 할 수 있는 것 |
|---|---|
| **01-personal (B-1)** | 자기가 만든 시스템을 자기 노트북에서 Claude Code에 연결한다. stdio transport의 부모-자식 모델을 이해한다. 90분 안에 한 사이클 완주. |
| **02-external (A)** | 외부 공개 REST API(JSONPlaceholder 등)를 LLM에 연결한다. "MCP Server는 외부 API의 어댑터"라는 핵심을 코드로 이해한다. |
| **03-organization (B-2)** | 자기 시스템을 회사 서버에 띄워서 여러 사용자에게 LLM 통합을 제공한다. HTTP transport + Bearer 인증 + 사용자별 데이터 격리. |

**비목표**:
- OAuth2/SSO (B-2에서는 고정 Bearer token 매핑만)
- prompts, resources (tools만)
- 학습용 JSON-RPC Client (제거 — Claude Code 내장 사용)
- 원격 MCP Server를 외부 호스팅 (로컬 HTTP만)

---

## 2. 폴더 구조

```
mcp-exam/
├── 01-personal/                    # 시나리오 B-1 — 내 것 + 1인 (현재 구현 이동)
│   ├── resource-server/
│   ├── mcp-server/                 # stdio
│   ├── docs/slides/mcp-lecture-personal.md
│   ├── .mcp.json                   # Claude Code 등록
│   └── README.md
│
├── 02-external/                    # 시나리오 A — 외부 Resource
│   ├── mcp-server/                 # 학생이 만드는 유일한 컴포넌트
│   ├── docs/slides/mcp-lecture-external.md
│   ├── .mcp.json
│   └── README.md
│
├── 03-organization/                # 시나리오 B-2 — HTTP + 인증 + multi-tenant
│   ├── resource-server/            # Bearer 검증 + 사용자별 Todo 격리
│   ├── mcp-server/                 # HTTP/SSE transport
│   ├── docs/slides/mcp-lecture-organization.md
│   ├── .mcp.alice.json             # alice 사용자용
│   ├── .mcp.bob.json               # bob 사용자용
│   └── README.md
│
├── docs/superpowers/
│   ├── specs/2026-05-28-mcp-lecture-3scenarios.md  # 이 문서
│   └── plans/                                       # 구현 계획서 (writing-plans 단계)
├── .gitignore
└── README.md                       # 시나리오 라우팅 + 학습 순서 안내
```

**폴더 네이밍 원칙**:
- 번호 prefix (`01`, `02`, `03`) = **권장 학습 순서**. 학생이 처음 들어왔을 때 어디서 시작할지 명확.
- 의미 명사 (`personal`, `external`, `organization`) = 시나리오의 본질. Resource 소유와 사용자 모델을 한 단어로 요약.
- 각 폴더는 **완전 독립** — 강사가 한 시나리오만 골라서 수업해도 OK. 코드 중복 허용.

---

## 3. 세 시나리오 한눈에 비교

| | **01-personal** | **02-external** | **03-organization** |
|---|---|---|---|
| Resource 소유 | 내 것 | 외부 (JSONPlaceholder) | 내 것 |
| Resource 위치 | 학생 노트북 (localhost:8080) | 인터넷 (jsonplaceholder.typicode.com) | 학생 노트북 또는 회사 서버 |
| 학생 작성 | Resource + MCP Server | MCP Server만 | Resource + MCP Server |
| Transport | stdio | stdio | HTTP/SSE |
| Host 위치 | 학생 노트북 | 학생 노트북 | 학생 노트북 (여러 명) |
| 인증 | 없음 | 없음 | Bearer (alice/bob 두 명) |
| 데이터 격리 | 한 명 분량 | 200건 fake (전체 공유) | 사용자별 격리 |
| 자연어 시연 핵심 | "내 Todo 추가/완료" | "fake Todo 검색/조회" | "alice/bob 각자 자기 Todo만" |
| 핵심 깨달음 | "Host와 같은 머신, 부모-자식" | "MCP Server = 외부 API 어댑터" | "MCP Server도 멀티유저 SaaS가 될 수 있음" |
| 강의 권장 순서 | 1번째 | 2번째 | 3번째 |

---

## 4. 시나리오 01-personal (90~120분, 기존 구현 이동)

현재 16 commits에 있는 결과물을 `01-personal/` 폴더로 이동. `03-client` 삭제. 슬라이드/README에서 "학습용 Client" 챕터 제거.

### 학생이 만드는 컴포넌트
- `01-personal/resource-server/` — Spring Boot REST API, `ConcurrentHashMap<Long, Todo>` in-memory 저장소. Todo CRUD 5개 endpoint.
- `01-personal/mcp-server/` — Spring Boot + Spring AI 1.0.3 MCP starter, stdio mode. `@Tool` 5개. Resource Server를 RestClient로 호출.

### 도메인: Todo (단일 사용자)
- `Todo = { id, title, memo, completed }`
- tool 5개: `list_todos`, `get_todo`, `add_todo`, `complete_todo`, `search_todos`

### 강의 흐름 (105분 기준)
| 분 | 섹션 | 내용 |
|---|---|---|
| 0~15 | 도입 + MCP 개념 | 4컴포넌트 그림, "Client는 LLM이 아님", JSON-RPC 메시지 두 가지 |
| 15~30 | Resource Server | Spring Boot REST + 인메모리, curl/HTTP 확인 |
| 30~60 | MCP Server | Spring AI starter 의존성, `@Tool` 5개, stdio 설정 (`web-application-type: none`, `banner-mode: off`, `stdio: true`) |
| 60~85 | Claude Code 연결 | `.mcp.json` 등록 → 자연어 시연. 클라이맥스. |
| 85~95 | 다음 단계 안내 | 02-external, 03-organization 예고 |
| 95~105 | Q&A | |

### 핵심 메시지
1. "Client = 통신 라이브러리, LLM 아님"
2. "MCP Server는 결정론적 서버 — JSON-RPC만 응답"
3. "자연어 처리는 Host 책임 — 우리 코드에 LLM 호출 0줄"
4. "stdio = 부모-자식 프로세스. Host가 spawn"

---

## 5. 시나리오 02-external (90~120분, 신규)

### 학생이 만드는 컴포넌트
- `02-external/mcp-server/` — Spring Boot + Spring AI MCP starter, stdio mode. `@Tool` 5개. **Resource Server를 학생이 만들지 않음** — JSONPlaceholder를 RestClient로 호출.

### Resource (학생이 만들지 않음)
- `https://jsonplaceholder.typicode.com/todos`
- 200건의 fake Todo (`{userId, id, title, completed}`)
- `GET /todos`, `GET /todos/{id}`, `POST /todos` (fake), `PATCH /todos/{id}` (fake), `DELETE /todos/{id}` (fake)
- 토큰 불필요, 공개 API, 인증 없음

### 도메인 매핑 (JSONPlaceholder → 학생의 tool)
| 학생의 tool | JSONPlaceholder 호출 | 비고 |
|---|---|---|
| `list_todos` | `GET /todos?_limit=20` | 200건 fake 중 20건만 |
| `list_todos_by_user` | `GET /todos?userId={id}` | 사용자별 필터 (JSONPlaceholder 특성) |
| `get_todo` | `GET /todos/{id}` | |
| `add_todo` | `POST /todos` | **fake — 응답은 오지만 서버 저장 안 됨**. 슬라이드에 명시. |
| `complete_todo` | `PATCH /todos/{id}` | **fake — 응답만** |
| `search_todos` | `GET /todos` 후 client-side title 필터 | JSONPlaceholder가 검색 미지원 → MCP Server 자체 필터링 |

### 데이터 모델
- `TodoView = { userId, id, title, completed }` — JSONPlaceholder 모양 그대로 (memo 없음, userId 있음).

### 강의 흐름 (105분 기준)
| 분 | 섹션 | 내용 |
|---|---|---|
| 0~10 | 도입 (01-personal 복습) | "이번엔 외부 API. MCP Server는 어댑터" |
| 10~25 | JSONPlaceholder 둘러보기 | curl로 endpoint 5개 확인. fake POST/PATCH 동작 학생이 직접 봄 |
| 25~30 | 도메인 매핑 설계 | 어떤 endpoint를 어떤 tool로 노출할지 |
| 30~70 | MCP Server 코드 | 01-personal과 같은 구조, RestClient base-url만 jsonplaceholder.typicode.com으로 |
| 70~95 | Claude Code 연결 + 시연 | "사용자 3의 할일 보여줘", "Todo 1번 뭐였지" |
| 95~105 | 다음 단계 + Q&A | 03-organization 예고 ("자기 시스템 + 다중 사용자") |

### 핵심 메시지
1. "**01-personal 코드와 MCP Server는 거의 똑같다** — base-url만 다르다"
2. "JSONPlaceholder는 LLM의 존재를 모른다. 그냥 REST 클라이언트가 한 명 더 생긴 셈"
3. "Resource Server를 못 건드려도 (외부 소유) MCP로 LLM 노출 가능"
4. "Fake POST/PATCH는 실제 저장 안 되는 게 JSONPlaceholder 특성. Resource Server의 한계가 아니라 데모 API의 특성"

### 학습 전이 효과 (Transfer)
강사가 강조해야 할 포인트: "01-personal MCP Server 코드와 02-external MCP Server 코드를 나란히 띄워놓고 비교하라." 99% 같다. `RestClient` base-url, tool 설명, 데이터 모델만 다르다. **MCP Server 패턴이 외부 API에도 동일하게 적용된다는 사실**이 02 시나리오의 가장 큰 가치.

---

## 6. 시나리오 03-organization (90~120분, 신규)

### 학생이 만드는 컴포넌트
- `03-organization/resource-server/` — Spring Boot REST. **Bearer 토큰 검증 + 사용자별 Todo 격리** 추가. Todo 모델에 `userId` 필드 추가.
- `03-organization/mcp-server/` — Spring Boot + Spring AI MCP starter. **HTTP transport (SSE 또는 streamable HTTP)**. Bearer 토큰을 Resource Server로 forward.

### 인증 모델 (단순화)
**토큰 매핑 (in-memory)**:
- `alice-token` → user `alice`
- `bob-token` → user `bob`
- 그 외 토큰 → 401 Unauthorized

**Resource Server**: Spring `HandlerInterceptor`로 모든 요청 가로채서 `Authorization: Bearer <token>` 검증 → `userId` 추출 → request scope에 저장. `TodoRepository`는 `userId`별로 분리된 `Map<UserId, Map<TodoId, Todo>>` 저장.

**MCP Server**: HTTP transport. incoming 요청의 `Authorization` 헤더 추출 → downstream Resource Server 호출 시 동일 헤더로 forward.

### 데이터 모델
- `Todo = { id, userId, title, memo, completed }` (userId 추가)
- 응답에서는 `userId` 제거 가능 (현재 사용자 것만 보여주니까 redundant). 학습 목적상 노출 OK.

### Transport: HTTP/SSE
Spring AI 1.0.3 MCP starter의 HTTP transport 모드:
- `application.yml`에서 `spring.main.web-application-type: servlet` (Tomcat 활성)
- MCP transport 설정 (SSE 또는 streamable HTTP — plan 단계에서 Spring AI 정확한 키 확인)
- 기본 path `/sse` 또는 `/mcp` 등

### Claude Code 등록
```json
// .mcp.alice.json
{
  "mcpServers": {
    "todo-org-alice": {
      "url": "http://localhost:8081/sse",
      "headers": {
        "Authorization": "Bearer alice-token"
      }
    }
  }
}
```

```json
// .mcp.bob.json
{
  "mcpServers": {
    "todo-org-bob": {
      "url": "http://localhost:8081/sse",
      "headers": {
        "Authorization": "Bearer bob-token"
      }
    }
  }
}
```

학생이 두 Claude Code 세션을 띄워서 alice/bob 각자의 view를 시연.

### 강의 흐름 (105분 기준)
| 분 | 섹션 | 내용 |
|---|---|---|
| 0~10 | 도입 (B-1, A 복습) | "이번엔 다중 사용자. HTTP transport + 인증" |
| 10~35 | Resource Server 변경 | userId 추가, HandlerInterceptor로 Bearer 검증, 사용자별 저장소 |
| 35~70 | MCP Server HTTP transport | `web-application-type: servlet`, MCP SSE/HTTP 설정, Authorization forward |
| 70~95 | Claude Code 두 세션 시연 | alice 터미널과 bob 터미널 동시 실행 → 같은 명령 → 다른 결과 |
| 95~105 | 운영 고려사항 + Q&A | 실제 OAuth/SSO로 확장하려면? Spring Security 통합 |

### 핵심 메시지
1. "**MCP Server도 서버다** — 사용자가 여러 명이면 인증 필요"
2. "HTTP transport에서는 MCP Server가 독립 서버 — Resource Server 옆에 둘 수 있음 (운영 관점)"
3. "토큰은 Claude Code의 `.mcp.json`에서 헤더로 보냄 → MCP Server → Resource Server로 forward"
4. "고정 Bearer는 학습용 단순화. 실전은 OAuth2/JWT — 패턴은 같음"

### 보안 경고 (학생에게)
- 토큰을 `.mcp.json`에 평문 저장 → 학습용. 실전은 secret manager 사용.
- 모든 사용자가 같은 머신에서 두 세션을 띄우는 시연 → 단일 머신 multi-tenant. 진짜 멀티 머신은 운영 관점.

---

## 7. 기술 스택 (전 시나리오 공통)

| 항목 | 값 |
|---|---|
| Java | 21 |
| Build | Gradle (Kotlin DSL) |
| Spring Boot | 3.3.x |
| Spring AI MCP | 1.0.3 (`spring-ai-starter-mcp-server`) |
| Tool 어노테이션 | `@Tool` / `@ToolParam` (`org.springframework.ai.tool.annotation`) |
| Tool 등록 | `MethodToolCallbackProvider` bean |
| HTTP 클라이언트 | Spring `RestClient` |
| JSON | Jackson (Spring Boot 내장) |
| 03-organization 인증 | Spring `HandlerInterceptor` + in-memory token map |
| 03-organization Transport | Spring AI MCP HTTP/SSE (정확한 키는 plan 단계 확인) |

---

## 8. 기존 코드(16 commits) 처리

| 현재 위치 | 작업 |
|---|---|
| `01-resource-server/` | → `01-personal/resource-server/` 이동 |
| `02-mcp-server/` | → `01-personal/mcp-server/` 이동 |
| `03-client/` | **삭제** |
| `.mcp.json` (루트) | → `01-personal/.mcp.json` 이동 + jar 경로 갱신 |
| `docs/slides/mcp-lecture.md` | → `01-personal/docs/slides/mcp-lecture-personal.md` 이동, "③ 학습용 Client" 챕터 제거 |
| `README.md` (루트) | 새로 작성: 세 시나리오 라우팅 + 학습 순서 안내 |
| `docs/superpowers/specs/2026-05-28-mcp-lecture-design.md` | 보존 (이전 단일 시나리오 설계 기록) |
| `docs/superpowers/plans/2026-05-28-mcp-lecture.md` | 보존 (이전 plan, 참고용) |

이동 후 `01-personal/` 안의 각 컴포넌트는 Gradle wrapper 포함된 독립 프로젝트.

---

## 9. 위험 요소

| 위험 | 대응 |
|---|---|
| Spring AI 1.0.3의 HTTP/SSE transport 안정성 미확인 | plan 단계에서 실제 빌드 검증. SSE 우선, 실패 시 streamable HTTP fallback. 둘 다 실패하면 사용자에게 보고. |
| 3 시나리오 × 2 컴포넌트(외부 시나리오는 1) — 분량 큼 | 각 시나리오 폴더 독립이라 동시 작업 가능. 우선순위: **01-personal (이미 90% 있음, 이동만)** → 02-external (01에서 base-url만 변경) → 03-organization (신규, 가장 복잡) |
| JSONPlaceholder fake POST/PATCH 학생 혼동 | 02-external 슬라이드와 README에 명시. "이건 데모 API 특성이지 MCP/우리 코드 한계가 아님" |
| 두 Claude Code 세션 동시 운영 (03 시연) | Windows에서 두 터미널 OK. 또는 시간 분리 (alice 먼저 → bob 다음) |
| 학생이 폴더 순서를 학습 순서로 오해 안 함 — 02-external 보고 "왜 외부 먼저?" | 루트 README가 권장 순서 (01-personal → 02-external → 03-organization) 명시. 폴더 번호 prefix가 순서 보강 |

---

## 10. 다음 단계

1. 이 설계서 사용자 리뷰 및 승인
2. `superpowers:writing-plans` 스킬로 구현 계획서 작성
   - **세 시나리오를 한 plan에 담지 말고 시나리오별 plan 권장** (각 90~120분 강의 단위 독립)
   - 권장 plan 분할:
     - `2026-05-28-01-personal-migration.md` — 기존 코드 이동 + Client 제거 + 슬라이드 정리
     - `2026-05-28-02-external-new.md` — 02-external 신규 구현
     - `2026-05-28-03-organization-new.md` — 03-organization 신규 구현 (HTTP transport, 인증)
3. 각 plan을 subagent-driven-development로 실행
