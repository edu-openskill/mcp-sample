# 시나리오 02-external: 외부 공개 API + 1인 사용

> 자기 권한으로 접근 가능한 외부 REST API(JSONPlaceholder)를 LLM에 연결한다.
> "MCP Server는 외부 API의 어댑터"라는 mental model을 코드로 확인한다.

## 구성

```
02-external/
├── mcp-server/         # Spring AI MCP starter, stdio mode
│                       # Resource Server는 학생이 만들지 않음 — 외부 사용
├── docs/slides/        # Marp 슬라이드
└── .mcp.json           # Claude Code 등록 설정
```

**참고 — 01-personal과의 비교 가치**

이 시나리오의 핵심은 "01-personal MCP Server 코드와 거의 똑같다"는 사실을 학생이 직접 보는 것. 강의에서:

```powershell
diff -r ..\01-personal\mcp-server\src ..\02-external\mcp-server\src
```

99%가 같다. 차이는 (1) `RestClient` base-url, (2) `TodoView` 모델 (`memo` 대신 `userId`), (3) `add`/`complete`에 fake note 추가.

## 필요 환경

- Java 21 (JDK)
- Gradle 8.x (wrapper 포함)
- Claude Code CLI
- **인터넷 접근** (JSONPlaceholder 호출용)

## 실행 순서

01-personal과 달리 Resource Server를 띄울 필요 **없음**. JSONPlaceholder는 외부.

### 1) MCP Server JAR 빌드 (한 번만)

```powershell
cd mcp-server
./gradlew bootJar
# → build/libs/mcp-server-external.jar
```

### 2) Claude Code 연결

`.mcp.json`의 jar 절대경로를 자기 환경에 맞게 수정 후, `02-external/` 디렉토리에서:

```powershell
claude
```

세션 안에서:

```
사용자 3의 할일 보여줘
1번 할일 뭐였지
"MCP 공부하기" 추가해줘 (사용자 1로)
milk 관련 할일 검색해줘
```

`add_todo`와 `complete_todo` 응답에 **"JSONPlaceholder는 데모 API라 실제 저장되지 않습니다"** note가 함께 옴 → Claude가 이를 사용자에게 자연어로 전달.

## 강의 흐름 (105분)

| 분 | 단계 |
|---|---|
| 0~10 | 01-personal 복습 + 오늘 목표 |
| 10~25 | JSONPlaceholder 둘러보기 (curl) + 도메인 매핑 설계 |
| 25~70 | MCP Server 코드 (01-personal과 비교) |
| 70~95 | Claude Code 연결 + 자연어 시연 |
| 95~105 | 03-organization 예고 + Q&A |

## 트러블슈팅

### "JSONPlaceholder에 연결 실패"

- 인터넷 접근 확인: `curl https://jsonplaceholder.typicode.com/todos/1`
- 회사 방화벽이 차단하는 경우 → 대안: `https://dummyjson.com` 등 다른 fake API로 base-url 교체

### "POST/PATCH 응답은 오는데 다시 조회하면 변경 없음"

**JSONPlaceholder의 정상 동작** (fake API). MCP Server의 버그 아님. tool 응답의 `note` 필드를 강의에서 강조.

### 그 외 (Spring Boot stdio 일반 문제)

01-personal/README.md의 트러블슈팅 섹션 참조 — 거의 동일.

## 핵심 메시지

1. **외부 API + 접근 권한 있으면 누구나 MCP Server 만들 수 있다**
2. **01-personal과 99% 같은 코드** — 패턴 전이 효과
3. **Fake API 응답을 tool note로 LLM에 전달** — 메타 정보 통신
4. **Resource Server는 LLM의 존재를 모른다** — 진정한 어댑터 패턴

## 다음 시나리오

- [`../03-organization/`](../03-organization/) — HTTP transport + Bearer 인증 + 멀티유저
