---
marp: true
theme: default
paginate: true
header: 'MCP 입문 (02-external) — 외부 API + 1인 사용'
footer: '2026-05-28'
---

# MCP 입문 — 02-external
## 외부 공개 API를 LLM에 연결한다
### "MCP Server는 외부 API의 어댑터"

---

## 01-personal 복습

지난 시간:
- 내가 만든 Resource Server (Todo)
- 내가 만든 MCP Server (`@Tool` 5개)
- Claude Code가 spawn한 stdio MCP Server

**핵심 깨달음**: Client는 LLM이 아니다. Resource Server는 MCP를 모른다.

---

## 오늘의 목표

90분 후 여러분은:

1. **외부 공개 API**(GitHub, Notion, JSONPlaceholder 등)를 MCP Server로 감쌀 수 있다
2. 01-personal의 MCP Server 코드와 **거의 똑같다**는 사실을 두 눈으로 확인했다
3. "Resource API에 접근 권한만 있으면 누구든 MCP Server 만들 수 있다"는 mental model을 이해한다
4. Fake API의 한계(저장 안 됨)를 LLM 응답에 어떻게 알려주는지 안다

---

## 오늘 만들 것

| # | 컴포넌트 | 기술 | 역할 |
|---|---|---|---|
| ① | MCP Server | Spring Boot + Spring AI | `@Tool` 5개, RestClient → JSONPlaceholder |
| ② | Host | Claude Code | (외부 도구) |

**Resource Server는 학생이 만들지 않는다.** 외부 공개 API 사용.

JSONPlaceholder: `https://jsonplaceholder.typicode.com/todos`
- 200건 fake Todo, 인증 불필요
- POST/PATCH는 **fake** (응답만, 실제 저장 X)

---

## 핵심 — 01-personal과 비교

```
01-personal                   02-external
─────────────                 ────────────
@Tool list_todos              @Tool list_todos     ← 같음
@Tool get_todo                @Tool get_todo       ← 같음
@Tool add_todo                @Tool add_todo       ← 같음
...                           ...

RestClient                    RestClient            ← 같음
  baseUrl: localhost:8080       baseUrl: jsonplaceholder.typicode.com   ← 다름

application.yml stdio         application.yml stdio  ← 같음
```

**99% 같은 코드**. base-url과 데이터 모델(`memo` vs `userId`)만 다름.

---

## tool 5개 + 보조 1개

| tool | JSONPlaceholder 호출 |
|---|---|
| `list_todos` | GET /todos?_limit=20 |
| `list_todos_by_user` | GET /todos?userId={id} |
| `get_todo` | GET /todos/{id} |
| `add_todo` | POST /todos (**fake** — 응답만) |
| `complete_todo` | PATCH /todos/{id} (**fake**) |
| `search_todos` | GET /todos 후 클라이언트 필터링 |

---

## Fake API 응답 처리

`add_todo`와 `complete_todo`는 응답에 **note**를 함께 반환:

```java
return new AddResult(
    created,
    "참고: JSONPlaceholder는 데모 API라 POST/PATCH가 실제 저장되지 않습니다. 응답만 돌아옵니다."
);
```

→ Claude Code가 사용자에게 자연어로 "추가됐지만 데모 API라 실제 저장은 안 돼요"라고 안내.

학생 교훈: **MCP tool 응답에 LLM이 사용자에게 전달할 메타 정보 포함 가능**.

---

## 강의 흐름 (105분)

| 분 | 단계 |
|---|---|
| 0~10 | 01-personal 복습 + 오늘 목표 |
| 10~25 | JSONPlaceholder 둘러보기 (curl) + 도메인 매핑 설계 |
| 25~70 | MCP Server 코드 (01-personal 코드와 diff로 비교) |
| 70~95 | Claude Code 연결 + 자연어 시연 |
| 95~105 | 03-organization 예고 + Q&A |

---

## 자연어 시연 예시

```
> 사용자 3의 할일 보여줘
[Claude → list_todos_by_user(3) → JSONPlaceholder /todos?userId=3]

> 1번 할일 뭐였지?
[Claude → get_todo(1) → /todos/1]

> "MCP 공부하기" 추가해줘 (userId=1)
[Claude → add_todo(...) → POST /todos]
[응답에 fake note 포함 → Claude가 "추가됐지만 데모 API라 저장 안 됨" 안내]

> milk 관련 할일 검색해줘
[Claude → search_todos("milk") → /todos 전체 + 클라이언트 필터]
```

---

## 다음 시나리오 — `03-organization`

지금까지: stdio + 1인 사용자.

다음:
- **HTTP transport** — MCP Server가 독립 서버
- **Bearer 인증** — 여러 사용자가 자기 데이터만 봄
- **alice, bob 두 계정으로 multi-tenant 시연**
- Resource Server를 회사 서버에 띄우는 운영 시나리오

---

## 핵심 메시지 4가지

1. **외부 API + 학생 권한만 있으면 누구든 MCP Server 만들 수 있다**
2. **01-personal MCP Server 코드와 거의 똑같다** — base-url과 모델만 다름
3. **Fake API note를 tool 응답에 포함**해서 LLM이 사용자에게 메타 정보 전달
4. **Resource Server를 못 건드려도 MCP로 LLM 노출 가능** — 어댑터 패턴

---

## Q & A

코드:
`02-external/mcp-server/src/main/java/com/example/external/`

비교:
01-personal과 diff: `diff -r 01-personal/mcp-server/src 02-external/mcp-server/src`
