---
marp: true
theme: default
paginate: true
header: 'MCP 입문 (03-organization) — 사내 공식 통합 + 멀티유저'
footer: '2026-05-28'
---

# MCP 입문 — 03-organization
## HTTP transport + Bearer 인증 + 사용자별 격리
### "MCP Server도 멀티유저 SaaS가 될 수 있다"

---

## 지난 두 시간 복습

| 시나리오 | Transport | 사용자 | 인증 |
|---|---|---|---|
| 01-personal | stdio | 1명 (나) | 없음 |
| 02-external | stdio | 1명 (나) | (외부 API에 따라) |
| **03-organization** (오늘) | **HTTP/SSE** | **여러 명 (alice/bob)** | **Bearer** |

---

## 오늘의 목표

90분 후 여러분은:

1. **HTTP transport** MCP Server를 띄울 수 있다
2. **Bearer 토큰**으로 사용자를 식별하고 Todo를 격리할 수 있다
3. **두 Claude Code 세션**(alice/bob)이 같은 MCP Server에 다른 자격으로 접속하는 것을 본다
4. "MCP Server도 일반 서버다 — 사용자 多인 SaaS형 운영 가능"을 이해한다

---

## 아키텍처

```
[Resource Server :8080]              ← 회사 백엔드 (사용자별 격리)
   ↑ HTTP + Bearer
   │
[MCP Server :8081 HTTP/SSE]          ← 회사 공식 MCP (HTTP transport)
   ↑ HTTP/SSE + Bearer header
   │  ┌─────────────────────────────┐
   ├──┤ alice의 Claude Code          │
   │  │ (.mcp.alice.json)            │
   │  └─────────────────────────────┘
   │
   │  ┌─────────────────────────────┐
   └──┤ bob의 Claude Code            │
      │ (.mcp.bob.json)              │
      └─────────────────────────────┘
```

같은 MCP Server에 두 사용자가 각자 자격으로 접속 → 자기 Todo만.

---

## 인증 모델 (단순화)

토큰 매핑 (in-memory 하드코딩):

```java
public static String resolveUserId(String bearerToken) {
    return switch (bearerToken) {
        case "alice-token" -> "alice";
        case "bob-token"   -> "bob";
        default            -> null;  // 401
    };
}
```

운영은 JWT/OAuth로 대체. 본 강의는 **패턴**에 집중.

---

## Resource Server 변경점 (01-personal 대비)

| 변경 | 코드 |
|---|---|
| Todo에 `userId` 필드 | `record Todo(Long id, String userId, ...)` |
| Repository 사용자별 격리 | `Map<UserId, Map<TodoId, Todo>>` |
| 모든 endpoint 인증 | `HandlerInterceptor` + Bearer 검증 |
| Controller에서 userId 사용 | `AuthContext.getOrThrow()` (ThreadLocal) |

`afterCompletion`에서 `AuthContext.clear()` 필수 — 요청 끝나면 ThreadLocal 정리.

---

## MCP Server HTTP transport 설정

`application.yml`:

```yaml
server:
  port: 8081

spring:
  main:
    web-application-type: servlet   # Tomcat 활성 (vs 01-personal: none)
    banner-mode: console            # stdout JSON-RPC X, 배너 OK
  ai:
    mcp:
      server:
        type: SYNC
        # stdio 키 없음 → HTTP/SSE 활성
```

추가 의존성 필수 (transitive 안 됨):
```kotlin
implementation("io.modelcontextprotocol.sdk:mcp-spring-webmvc:0.10.0")
```

---

## HTTP/SSE 엔드포인트

Spring AI 1.0.3 MCP starter가 자동 노출:

- `GET /sse` — long-lived SSE 연결. sessionId 발급.
- `POST /mcp/message?sessionId=<uuid>` — 실제 JSON-RPC 메시지.

Claude Code는 `url: "http://localhost:8081/sse"` 만 명시. sessionId는 자동.

---

## Bearer 헤더 forward 패턴

```java
// MCP Server의 AuthForwardInterceptor가 incoming Auth를 ThreadLocal에 저장
RequestAuthHeaderHolder.set(request.getHeader("Authorization"));

// TodoRestClient의 RestClient interceptor가 downstream에 그대로 전달
.requestInterceptor((req, body, exec) -> {
    String auth = RequestAuthHeaderHolder.get();
    if (auth != null) req.getHeaders().set("Authorization", auth);
    return exec.execute(req, body);
})

// afterCompletion에서 clear
```

→ Resource Server는 평소처럼 Bearer 받아 검증.

---

## Claude Code 연결 — `.mcp.alice.json`

```json
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

- `command/args` 대신 `url` 사용 → HTTP transport
- `headers`에 Bearer 토큰 → MCP Server가 받아서 downstream forward

bob도 똑같은 구조, 토큰만 `bob-token`.

---

## 강의 흐름 (105분)

| 분 | 단계 |
|---|---|
| 0~10 | 01/02 복습 + 오늘 목표 |
| 10~35 | Resource Server 변경 (userId, Interceptor, 격리) |
| 35~70 | MCP Server HTTP transport + Auth forward |
| 70~95 | 두 Claude Code 세션 alice/bob 동시 시연 |
| 95~105 | 운영 고려사항 + Q&A |

---

## Multi-tenant 시연

터미널 1: Resource Server (8080)
터미널 2: MCP Server (8081)

터미널 3 (alice):
```
cd 03-organization
cp .mcp.alice.json .mcp.json
claude
> 내 할일 보여줘
> "alice의 비밀 task" 추가
```

터미널 4 (bob):
```
cd 03-organization
cp .mcp.bob.json .mcp.json
claude
> 내 할일 보여줘   # alice 것 안 보임!
```

같은 MCP Server, 다른 토큰 → 다른 결과.

---

## 운영 고려사항

- **JWT/OAuth2**: `AuthContext.resolveUserId(token)` 자리를 JWT decoder/Spring Security로 교체
- **토큰 저장**: `.mcp.json`에 평문은 학습용. 실전은 OS keychain/secret manager.
- **HTTPS**: 로컬은 HTTP, 운영은 HTTPS 필수 (Bearer 평문 전송)
- **Rate limiting**: 토큰별 호출량 제한
- **세션 격리**: SSE sessionId별 헤더 보관 — 멀티 세션 동시 처리 시 주의

---

## 핵심 메시지 4가지

1. **MCP Server도 서버다** — HTTP transport로 다중 사용자 처리 가능
2. **Bearer 헤더가 모든 계층 통과** — Claude Code → MCP Server → Resource Server
3. **인증 모델은 plug-in** — 강의 단순 매핑 → 실전 OAuth2/JWT
4. **같은 MCP Server에 여러 Host** — 표준화의 진짜 가치

---

## 3 시나리오 마무리

| 시나리오 | 한 줄 요약 |
|---|---|
| 01-personal | "내 시스템을 내 노트북에서 LLM에" |
| 02-external | "남의 API를 LLM에 (어댑터)" |
| 03-organization | "내 시스템을 우리 팀에 LLM 통합으로 제공" |

세 가지 운영 시나리오의 멘탈 모델이 머리에 박혔다면, 어떤 실전 케이스도 분류할 수 있다.
