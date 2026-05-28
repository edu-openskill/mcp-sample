# 시나리오 01-personal: 내 시스템 + 1인 사용

> 자기가 만든 백엔드를 자기 노트북에서 Claude Code에 연결한다.
> MCP 입문의 첫 시나리오. stdio transport의 부모-자식 모델을 이해한다.

## 구성

```
01-personal/
├── resource-server/    # Spring Boot REST (Todo CRUD, 인메모리)
├── mcp-server/         # Spring AI MCP starter, stdio mode
├── docs/slides/        # Marp 슬라이드
└── .mcp.json           # Claude Code 등록 설정
```

## 필요 환경

- **Java 21** (JDK)
- **Gradle 8.x** (각 폴더에 wrapper 포함)
- **Claude Code CLI** (Pro 요금제 권장)

## 실행 순서

### 1) Resource Server (터미널 1)

```powershell
cd resource-server
./gradlew bootRun
# → http://localhost:8080
```

확인:

```powershell
curl http://localhost:8080/todos
# 응답: []
```

### 2) MCP Server JAR 빌드 (한 번만)

```powershell
cd ..\mcp-server
./gradlew bootJar
# → build/libs/mcp-server.jar (~27MB)
```

### 3) Claude Code 연결

`.mcp.json`의 jar 절대경로를 자기 환경에 맞게 수정 (자기 작업 디렉토리 절대경로). 그 후 `01-personal/` 디렉토리에서:

```powershell
claude
```

세션 안에서:

```
할일 목록 보여줘
"MCP 공부하기" 추가해줘
1번 할일 완료 처리해줘
MCP 관련 할일 검색해줘
```

## 강의 흐름 (105분)

| 분 | 단계 |
|---|---|
| 0~15 | 도입 + 슬라이드 |
| 15~35 | Resource Server 둘러보기 + curl |
| 35~70 | MCP Server: `@Tool` 5개 + stdio |
| 70~95 | Claude Code 연결 + 자연어 시연 |
| 95~105 | 다음 시나리오(02-external, 03-organization) 예고 + Q&A |

## 트러블슈팅

### MCP Server가 응답을 안 보냄

`mcp-server/logs/mcp-server.log` 확인. 가능한 원인:

- `application.yml`의 `web-application-type: none` 누락 → Tomcat이 stdout 가로챔
- `banner-mode: off` 누락 → Spring 배너가 stdout에 섞임
- `logging.pattern.console: ""` 누락 → 로그가 stdout으로 빠짐
- `spring.ai.mcp.server.stdio: true` 누락 → stdio transport 비활성화 (Spring AI 1.0.3 기본 false)

### `tools/call`에서 "Connection refused"

Resource Server(8080)가 안 떠 있음. 터미널 1 확인.

### Claude Code가 tool을 못 찾음

- `.mcp.json` jar 경로가 절대경로이고 정확한가?
- `claude` 실행 후 `/mcp` 명령으로 등록된 서버 목록 확인
- jar가 빌드되어 있나? `./gradlew bootJar` 다시.

### Windows PowerShell 따옴표 이슈

`curl -d` JSON에서 따옴표가 꼬이면 `resource-server/requests.http`를 VS Code REST Client로 실행.

## 핵심 메시지

1. **Client = 통신 라이브러리, LLM 아님** — Claude Code가 자기 안에 내장
2. **MCP Server는 결정론적 서버** — JSON-RPC만 응답, LLM 호출 없음
3. **자연어 처리는 Host 책임** — 우리 코드 어디에도 LLM 호출 없음
4. **stdio = 부모-자식 프로세스** — Claude Code가 jar를 spawn

## 다음 시나리오

- [`../02-external/`](../02-external/) — 외부 공개 API를 LLM에 연결
- [`../03-organization/`](../03-organization/) — HTTP transport + 인증 + 멀티유저
