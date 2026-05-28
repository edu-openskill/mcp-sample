# 시나리오 03-organization: 사내 공식 통합 + 멀티유저

> 자기 시스템을 회사 서버에 띄워서 여러 사용자에게 LLM 통합으로 제공한다.
> HTTP transport + Bearer 인증 + 사용자별 데이터 격리.

## 구성

```
03-organization/
├── resource-server/    # Spring Boot REST + 사용자별 Todo 격리 (8080)
├── mcp-server/         # Spring AI MCP starter, HTTP/SSE mode (8081)
├── docs/slides/        # Marp 슬라이드
├── .mcp.alice.json     # alice 사용자용 Claude Code 등록
└── .mcp.bob.json       # bob 사용자용
```

## 인증 모델

강의용 단순 매핑 (in-memory):
- `Bearer alice-token` → user `alice`
- `Bearer bob-token` → user `bob`
- 그 외 → 401 Unauthorized

운영은 JWT/OAuth2로 대체 (`AuthContext.resolveUserId` 자리만 교체).

## 필요 환경

- Java 21, Gradle wrapper, Claude Code CLI
- 포트 8080(Resource), 8081(MCP) 두 개 사용

## 실행 순서

### 1) Resource Server (터미널 1)

```powershell
cd resource-server
./gradlew bootRun
# → http://localhost:8080 (Bearer 검증)
```

확인 (alice로):

```powershell
curl -H "Authorization: Bearer alice-token" http://localhost:8080/todos
# 응답: []

curl -H "Authorization: Bearer wrong-token" http://localhost:8080/todos
# 응답: 401
```

### 2) MCP Server jar 빌드 (한 번)

```powershell
cd ..\mcp-server
./gradlew bootJar
```

### 3) MCP Server 실행 (터미널 2)

```powershell
java -jar build\libs\mcp-server-org.jar
# → http://localhost:8081
# GET /sse  (SSE 연결, sessionId 발급)
# POST /mcp/message?sessionId=...  (JSON-RPC)
```

### 4) Claude Code 두 세션 (alice / bob)

터미널 3 (alice):

```powershell
cd ..
Copy-Item .mcp.alice.json .mcp.json -Force
claude
> 내 할일 보여줘
> "alice의 비밀 task" 추가
> 1번 할일 완료
```

터미널 4 (bob, alice 세션 끝낸 후 또는 별도 폴더에서):

```powershell
cd ..
Copy-Item .mcp.bob.json .mcp.json -Force
claude
> 내 할일 보여줘   # alice 것 안 보임!
> "bob의 task" 추가
```

> **`.mcp.json` 충돌**: Claude Code는 cwd의 `.mcp.json`을 인식. alice/bob 같은 폴더에서 둘 다 띄우면 충돌. 해결책 두 가지:
> - 시간 분리: alice 먼저 끝내고 → `.mcp.json` 교체 → bob
> - 폴더 분리: alice용 / bob용 폴더를 따로 만들어 각자 `.mcp.json` 보유

## 강의 흐름 (105분)

| 분 | 단계 |
|---|---|
| 0~10 | 01/02 복습 + 오늘 목표 |
| 10~35 | Resource Server: userId, Interceptor, 격리 |
| 35~70 | MCP Server HTTP transport + Auth forward |
| 70~95 | 두 Claude Code 세션 시연 |
| 95~105 | 운영 고려사항(JWT/OAuth, HTTPS, rate limit) + Q&A |

## 트러블슈팅

### MCP Server URL이 404

원인: `mcp-spring-webmvc` 의존성 누락. `build.gradle.kts`에:

```kotlin
implementation("io.modelcontextprotocol.sdk:mcp-spring-webmvc:0.10.0")
```

이게 없으면 Tomcat은 뜨지만 SSE/message endpoint가 등록 안 됨 (transitively `optional`). 이미 본 프로젝트는 명시되어 있음.

### 401 Unauthorized

- `.mcp.*.json`의 토큰이 정확한가? (`alice-token` 또는 `bob-token`)
- Resource Server가 Bearer 검증 중인지 확인 (인터셉터 로그)

### alice/bob이 같은 데이터 보임

- Resource Server 사용자별 격리: `TodoRepositoryTest` 6 tests pass 확인
- MCP Server의 `AuthForwardInterceptor`가 동작하나? `RequestAuthHeaderHolder` 로깅 추가해서 확인

### Two Claude Code sessions 충돌

`.mcp.json` 파일 위치 충돌. 시간 분리 또는 폴더 분리 (위 §4 메모 참조).

## 핵심 메시지

1. **MCP Server도 일반 서버다** — HTTP transport, 멀티유저
2. **Bearer가 모든 계층 통과** — Claude Code → MCP → Resource
3. **인증은 plug-in** — 강의는 단순 매핑, 실전은 JWT/OAuth
4. **같은 MCP Server, 다른 자격** — multi-tenant SaaS 패턴

## 다음 단계 (강의 후)

- JWT/OAuth 통합 (Spring Security)
- HTTPS + 토큰 secure storage
- 다중 MCP Server 조합 (한 Claude Code에 N개 서버)
- 다른 SDK (TypeScript, Python) MCP Server와 비교
