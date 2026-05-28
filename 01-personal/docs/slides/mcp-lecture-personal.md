---
marp: true
theme: default
paginate: true
header: 'MCP 입문 (01-personal) — 내 시스템 + 1인 사용'
footer: '2026-05-28'
---

# MCP 입문
## Resource → MCP Server → Client → Host
### 90분에 끝내는 첫 MCP 연결

---

## 오늘의 목표

90분 후 여러분은:

1. MCP **4컴포넌트**를 자기 말로 설명할 수 있다
2. Client가 **LLM이 아니라는 사실**을 이해한다 (Claude Code 내장)
3. 자기가 만든 MCP Server를 **Claude Code에 등록**해서 자연어로 동작시킬 수 있다
4. stdio transport의 **부모-자식 프로세스 모델**을 이해한다

---

## 왜 MCP인가?

**Function calling의 한계**:
- 모델마다 함수 정의 포맷이 다름
- LLM이 직접 외부 시스템 호출하면 보안·격리 어려움

**MCP의 가치**:
- **표준 프로토콜** (JSON-RPC) — 한 번 만든 서버를 여러 LLM/Host가 공유
- **명확한 책임 분리** — LLM은 Host에만, Server는 결정론적

---

## 4 컴포넌트 그림

```
┌────────────────────────────┐
│ Host (LLM 앱: Claude Code)  │  ← LLM이 여기 산다
│  └ MCP Client (내장)        │
└──────────┬─────────────────┘
           │ stdio (JSON-RPC)
           ↓
┌────────────────────────────┐
│ MCP Server                  │  ← 우리가 만든다 (Spring AI)
└──────────┬─────────────────┘
           │ HTTP
           ↓
┌────────────────────────────┐
│ Resource Server             │  ← 우리가 만든다 (Spring REST)
└────────────────────────────┘
```

---

## ⚠️ Client는 LLM이 아닙니다

**가장 흔한 오해:**
- "Client가 자연어 처리하는 거 아니야?"
- "Client에 Claude API 키 박아야 하는 거 아니야?"

**진실:**
- **Client = JSON-RPC 통신 라이브러리**
- 자연어 처리는 **Host 안의 LLM** 책임
- Claude Code의 **내장 MCP Client** 코드에 LLM 호출 **0줄**

> 이걸 코드로 직접 확인합니다.

---

## JSON-RPC 메시지 두 가지만 보면 됩니다

**tools/list — "어떤 tool 있어?"**

```json
{"jsonrpc":"2.0","id":1,"method":"tools/list","params":{}}
```

응답:

```json
{"jsonrpc":"2.0","id":1,"result":{"tools":[
  {"name":"add_todo","description":"...","inputSchema":{...}},
  ...
]}}
```

---

## tools/call — "이거 실행해줘"

요청:

```json
{"jsonrpc":"2.0","id":2,"method":"tools/call","params":{
  "name":"add_todo",
  "arguments":{"title":"MCP 공부","memo":"오늘"}
}}
```

응답:

```json
{"jsonrpc":"2.0","id":2,"result":{
  "structuredContent":{"id":1,"title":"MCP 공부",...}
}}
```

> **이게 MCP의 전부입니다.**

---

## 오늘 만들 것

| # | 컴포넌트 | 기술 | 역할 |
|---|---|---|---|
| ① | Resource Server | Spring Boot REST | Todo 저장소 (내 시스템) |
| ② | MCP Server | Spring Boot + Spring AI | `@Tool` 5개 노출 (stdio) |
| ③ | Host | Claude Code | (외부 도구, ②를 자식 프로세스로 spawn) |

도메인: **Todo/메모 관리** (단일 사용자, 내 노트북)

> Spring AI는 일반 tool 어노테이션을 그대로 MCP tool로 노출합니다.

---

## 5개 tool

| tool | 인자 | REST |
|---|---|---|
| `list_todos` | — | GET /todos |
| `get_todo` | id | GET /todos/{id} |
| `add_todo` | title, memo? | POST /todos → **201 Created** |
| `complete_todo` | id | PATCH /todos/{id}/complete |
| `search_todos` | query | GET /todos/search |

---

## @Tool 어노테이션 (Spring AI 1.0.3)

```java
@Component
public class TodoTools {

    @Tool(name = "add_todo",
          description = "새 할일을 추가합니다. memo는 선택입니다.")
    public TodoView addTodo(
            @ToolParam(description = "제목", required = true) String title,
            @ToolParam(description = "메모(선택)", required = false) String memo) {
        return restClient.add(title, memo);
    }

    // list_todos / get_todo / complete_todo / search_todos 동일 패턴
}
```

`MethodToolCallbackProvider` 빈 등록 → MCP starter가 자동으로 tool 목록 노출

---

## 강의 흐름 (105분)

| 분 | 단계 |
|---|---|
| 0~15 | 도입 + 이 슬라이드 |
| 15~35 | ① Resource Server 둘러보기 + curl |
| 35~70 | ② MCP Server: `@Tool` 5개 + stdio 깊게 |
| 70~95 | ③ Claude Code 연결 + 자연어 시연 (클라이맥스) |
| 95~105 | 다음 시나리오 예고 + Q&A |

---

## (라이브 코딩 전환)

> 이후는 IDE와 터미널로 전환합니다.
> 슬라이드는 마지막 챕터에서 다시 돌아옵니다.

---

## 다음 시나리오 — `02-external`, `03-organization`

**02-external**: 외부 공개 API (JSONPlaceholder)를 LLM에 연결
- "MCP Server는 어댑터"라는 사실을 외부 API로 확인
- 오늘 만든 MCP Server와 **거의 같은 코드** — base-url만 다름

**03-organization**: 사내 공식 통합 (HTTP transport + 인증)
- 여러 사용자에게 LLM 통합 제공
- Bearer 토큰 → 사용자별 Todo 격리
- MCP Server도 멀티유저 서비스가 될 수 있음

오늘 배운 패턴이 그대로 확장됩니다.

---

## 핵심 메시지 4가지

1. **Client = 통신 라이브러리, LLM 아님**
2. **MCP Server는 결정론적 서버** — JSON-RPC만 처리
3. **자연어 처리는 Host 책임** — 우리 코드에 LLM 호출 0줄
4. **같은 MCP Server에 여러 Host가 붙는다** — 표준화의 힘

---

## Q & A

코드 저장소:
`github.com/<your-org>/mcp-exam`

설계 문서:
`docs/superpowers/specs/2026-05-28-mcp-lecture-design.md`
