# MCP 강의자료 — Todo/메모 도메인

1차시(90~120분) MCP 입문 강의용 코드 + 슬라이드.

학생은 90분 후 자신이 만든 MCP Server를 Claude Code에 등록해서
"오늘 할 일 보여줘" 같은 자연어 명령으로 동작시키는 경험을 한다.

## 디렉토리

```
mcp-exam/
├── 01-resource-server/   # Spring Boot REST API (Todo CRUD)
├── 02-mcp-server/        # Spring AI MCP Server (@Tool x5)
├── 03-client/            # 학습용 JSON-RPC Client (Java)
├── docs/slides/          # Marp 슬라이드
└── .mcp.json             # Claude Code 연결 설정 (HEAD 85ebc86 포함)
```

## 필요 환경

- **Java 21** (JDK)
- **Gradle 8.x** (각 폴더에 wrapper 포함, 별도 설치 불요)
- **Claude Code CLI** (Pro 요금제 권장; `claude` 명령으로 실행 가능)
- 선택: **Marp** (슬라이드 미리보기), **VS Code REST Client** (`.http` 파일)

## 실행 순서

### 1) Resource Server (터미널 1)

```powershell
cd 01-resource-server
./gradlew bootRun
# → http://localhost:8080
```

확인:

```powershell
curl http://localhost:8080/todos
# 응답: []
```

### 2) MCP Server JAR 빌드 (한번만)

```powershell
cd 02-mcp-server
./gradlew bootJar
# → build/libs/mcp-server.jar
```

### 3) 학습용 Client 실행 (터미널 2)

```powershell
cd 03-client
./gradlew run
```

콘솔에 `initialize → tools/list → tools/call` JSON-RPC 메시지가 흐르는 것을 확인.

### 4) Claude Code 연결 (실전 시연)

`.mcp.json` 파일의 jar 절대경로를 자기 환경에 맞게 수정 후:

```powershell
claude
```

세션 안에서:

```
할일 목록 보여줘
"MCP 공부하기" 추가해줘
1번 할일 완료 처리해줘
```

## 트러블슈팅

### MCP Server가 응답을 안 보냄

`02-mcp-server/logs/mcp-server.log` 확인. 가능한 원인:

- `application.yml`의 `web-application-type: none` 누락 — HTTP 서버가 뜨면서 stdio가 막힘
- `banner-mode: off` 누락 — Spring 배너가 stdout에 섞여 JSON-RPC 파싱 실패
- `logging.pattern.console: ""` 누락 — 로그 출력이 stdout에 섞임
- `spring.ai.mcp.server.stdio: true` 미설정 — Spring AI 1.0.3 기본값은 `false`이므로 반드시 명시

### `tools/call`에서 "Connection refused"

Resource Server(8080)가 안 떠 있음. 터미널 1에서 `./gradlew bootRun`을 먼저 실행.

### Claude Code가 tool을 못 찾음

- `.mcp.json` 경로가 맞나? (절대경로로 기재되어 있음 — 자기 환경에 맞게 수정 필요)
- `claude` 실행 후 `/mcp` 명령으로 등록된 서버 목록 확인
- jar가 빌드되어 있나? `./gradlew bootJar` 다시 실행

### Windows에서 PowerShell 따옴표 이슈

`curl -d` JSON에서 작은따옴표/큰따옴표가 꼬이면 `01-resource-server/requests.http` 파일을 VS Code REST Client로 실행하는 것이 안전.

## 강의자 노트

- 코드는 학생이 따라 치지 않아도 됨 (1차시 분량). repo를 clone 후 핵심 부분만 라이브로 짚으며 설명.
- **80~100분 구간(Claude Code 연결)이 클라이맥스**. 시간이 모자라면 ① 학습용 Client의 코드 리딩을 줄여 시간을 확보.
- 학생이 자기 노트북에 Java 21이 없을 가능성 — 시작 30분 전 환경 점검 시간 권장.
- `.mcp.json`의 jar 경로는 절대경로로 기재되어 있어 학생 각자 환경에 맞게 수정 필요함을 안내.

## 참고

- 설계: [`docs/superpowers/specs/2026-05-28-mcp-lecture-design.md`](docs/superpowers/specs/2026-05-28-mcp-lecture-design.md)
- 구현 계획: [`docs/superpowers/plans/2026-05-28-mcp-lecture.md`](docs/superpowers/plans/2026-05-28-mcp-lecture.md)
- Spring AI MCP 문서: https://docs.spring.io/spring-ai/reference/api/mcp/
- MCP 명세: https://modelcontextprotocol.io/
