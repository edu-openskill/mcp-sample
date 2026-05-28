# 02-external 시나리오 신규 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 외부 공개 REST API(JSONPlaceholder)를 LLM에 노출하는 MCP Server를 신규 구현하고, 시나리오 02-external의 강의 자료(슬라이드 + README + .mcp.json)를 작성한다.

**Architecture:** 01-personal/mcp-server를 템플릿으로 복제. RestClient base-url을 JSONPlaceholder로 변경. tool 5개의 응답 모델을 JSONPlaceholder 스키마(`{userId, id, title, completed}`)에 맞춤. Resource Server는 학생이 만들지 않음.

**Tech Stack:** Java 21, Gradle Kotlin DSL, Spring Boot 3.3.x, Spring AI 1.0.3 (`spring-ai-starter-mcp-server`), Spring `RestClient`, Jackson.

**참고:**
- 설계서: `docs/superpowers/specs/2026-05-28-mcp-lecture-3scenarios.md` §5
- 모델 코드: `01-personal/mcp-server/` (구조 100% 재사용)

---

## File Structure

```
02-external/
├── mcp-server/                    # Spring AI MCP starter, stdio mode
│   ├── build.gradle.kts           # 01-personal과 동일
│   ├── settings.gradle.kts        # rootProject.name = "mcp-server-external"
│   ├── gradle/wrapper/*           # 01-personal에서 복사
│   ├── gradlew, gradlew.bat       # 01-personal에서 복사
│   └── src/
│       ├── main/java/com/example/external/
│       │   ├── McpServerApplication.java  # MethodToolCallbackProvider bean
│       │   ├── TodoView.java              # {userId, id, title, completed}
│       │   ├── JsonPlaceholderClient.java # RestClient → jsonplaceholder.typicode.com
│       │   └── TodoTools.java             # @Tool 5개
│       ├── main/resources/application.yml # stdio 설정
│       └── test/java/com/example/external/
│           └── TodoToolsTest.java         # mock JsonPlaceholderClient
├── docs/slides/mcp-lecture-external.md
├── .mcp.json
└── README.md
```

JSONPlaceholder 측 데이터:
- Endpoint: `https://jsonplaceholder.typicode.com/todos`
- 200건 fake Todo, `{userId, id, title, completed}` 형식
- POST/PATCH/DELETE 는 가짜 응답 (실제 저장 안 됨) — 학생에게 명시

---

## 공통 규칙

- Windows 11, PowerShell.
- 사용자 글로벌 CLAUDE.md: 항상 `git add -A`, hook skip X, 새 commit (amend X).
- 한 태스크 = 한 commit. 커밋 메시지 prefix: `external:`, `docs:` 등.

---

## Task 1: 02-external 디렉토리 + Gradle wrapper 복제

**Files:**
- Create: `02-external/mcp-server/` (디렉토리)
- Copy: `01-personal/mcp-server/gradle/`, `gradlew`, `gradlew.bat` → 같은 위치 02-external에

- [ ] **Step 1: 디렉토리 생성 + wrapper 복사**

Run in `C:\Users\G\workspace\mcp-exam`:

```powershell
New-Item -ItemType Directory -Force -Path "02-external/mcp-server" | Out-Null
Copy-Item -Recurse 01-personal/mcp-server/gradle 02-external/mcp-server/gradle
Copy-Item 01-personal/mcp-server/gradlew 02-external/mcp-server/
Copy-Item 01-personal/mcp-server/gradlew.bat 02-external/mcp-server/
```

- [ ] **Step 2: settings.gradle.kts 작성**

Create `02-external/mcp-server/settings.gradle.kts`:

```kotlin
rootProject.name = "mcp-server-external"
```

- [ ] **Step 3: build.gradle.kts 작성**

Create `02-external/mcp-server/build.gradle.kts` — 01-personal/mcp-server/build.gradle.kts와 거의 동일:

```kotlin
plugins {
    java
    id("org.springframework.boot") version "3.3.5"
    id("io.spring.dependency-management") version "1.1.6"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.ai:spring-ai-bom:1.0.3")
    }
}

dependencies {
    implementation("org.springframework.ai:spring-ai-starter-mcp-server")
    implementation("org.springframework.boot:spring-boot-starter-web")  // RestClient용
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveFileName.set("mcp-server-external.jar")
}
```

- [ ] **Step 4: 빌드 검증 (의존성 다운로드)**

```powershell
Push-Location 02-external\mcp-server; .\gradlew tasks --console=plain 2>&1 | Select-Object -Last 5; Pop-Location
```

Expected: `BUILD SUCCESSFUL`. Gradle wrapper가 작동하고 의존성 해소.

- [ ] **Step 5: Commit**

```powershell
git add -A
git commit -m "external: scaffold 02-external/mcp-server with gradle and spring-ai dependencies"
```

---

## Task 2: McpServerApplication + application.yml

**Files:**
- Create: `02-external/mcp-server/src/main/java/com/example/external/McpServerApplication.java`
- Create: `02-external/mcp-server/src/main/resources/application.yml`

- [ ] **Step 1: McpServerApplication 작성**

Create `02-external/mcp-server/src/main/java/com/example/external/McpServerApplication.java`:

```java
package com.example.external;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class McpServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(McpServerApplication.class, args);
    }

    @Bean
    public ToolCallbackProvider todoToolCallbacks(TodoTools todoTools) {
        return MethodToolCallbackProvider.builder().toolObjects(todoTools).build();
    }
}
```

- [ ] **Step 2: application.yml 작성**

Create `02-external/mcp-server/src/main/resources/application.yml`:

```yaml
spring:
  main:
    web-application-type: none      # stdio 모드
    banner-mode: off
  ai:
    mcp:
      server:
        name: todo-mcp-server-external
        version: 0.0.1
        type: SYNC
        stdio: true

logging:
  file:
    name: ./logs/mcp-server-external.log
  pattern:
    console: ""
  level:
    root: INFO
    com.example.external: DEBUG

jsonplaceholder:
  base-url: https://jsonplaceholder.typicode.com
```

- [ ] **Step 3: 빌드 검증**

```powershell
Push-Location 02-external\mcp-server; .\gradlew build -x test --console=plain 2>&1 | Select-Object -Last 5; Pop-Location
```

Expected: `BUILD SUCCESSFUL` (`TodoTools` 클래스 없어서 application 실행은 fail이지만, 컴파일은 통과 못 함 — application 클래스가 TodoTools를 참조. 그래서 이 단계는 Task 4 이후로 미루는 게 안전. 아래 옵션.)

**대안 (권장)**: Step 1의 application 클래스에서 `todoToolCallbacks` bean을 일단 주석 처리하고 다음 task에서 추가. 또는 빌드 검증을 다음 task로 미룸.

여기서는 **빌드 검증을 다음 task로 미룬다**. 이 step에서는 클래스/yaml 파일만 생성하고 진행.

- [ ] **Step 4: Commit**

```powershell
git add -A
git commit -m "external: add application class and stdio-mode application.yml"
```

---

## Task 3: TodoView record

**Files:**
- Create: `02-external/mcp-server/src/main/java/com/example/external/TodoView.java`

- [ ] **Step 1: TodoView record 작성**

JSONPlaceholder의 `/todos` 응답 모양은 `{userId, id, title, completed}`. 01-personal과 달리 `memo` 없고 `userId` 있음.

Create `02-external/mcp-server/src/main/java/com/example/external/TodoView.java`:

```java
package com.example.external;

public record TodoView(
        Long userId,
        Long id,
        String title,
        boolean completed
) {}
```

- [ ] **Step 2: Commit**

```powershell
git add -A
git commit -m "external: add TodoView record matching JSONPlaceholder schema"
```

---

## Task 4: JsonPlaceholderClient (RestClient 래퍼)

**Files:**
- Create: `02-external/mcp-server/src/main/java/com/example/external/JsonPlaceholderClient.java`

- [ ] **Step 1: JsonPlaceholderClient 작성**

Create `02-external/mcp-server/src/main/java/com/example/external/JsonPlaceholderClient.java`:

```java
package com.example.external;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Optional;

@Component
public class JsonPlaceholderClient {

    private final RestClient client;

    public JsonPlaceholderClient(@Value("${jsonplaceholder.base-url}") String baseUrl) {
        this.client = RestClient.builder().baseUrl(baseUrl).build();
    }

    public List<TodoView> listAll(int limit) {
        return client.get()
                .uri(uri -> uri.path("/todos").queryParam("_limit", limit).build())
                .retrieve()
                .body(new ParameterizedTypeReference<List<TodoView>>() {});
    }

    public List<TodoView> listByUser(long userId) {
        return client.get()
                .uri(uri -> uri.path("/todos").queryParam("userId", userId).build())
                .retrieve()
                .body(new ParameterizedTypeReference<List<TodoView>>() {});
    }

    public Optional<TodoView> get(long id) {
        TodoView body = client.get()
                .uri("/todos/{id}", id)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, resp) -> { })
                .body(TodoView.class);
        return Optional.ofNullable(body);
    }

    public TodoView add(String title, long userId) {
        return client.post()
                .uri("/todos")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new AddBody(userId, title, false))
                .retrieve()
                .body(TodoView.class);
    }

    public TodoView complete(long id) {
        return client.patch()
                .uri("/todos/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new PatchBody(true))
                .retrieve()
                .body(TodoView.class);
    }

    record AddBody(Long userId, String title, boolean completed) {}
    record PatchBody(boolean completed) {}
}
```

> **주의**: JSONPlaceholder의 POST/PATCH는 **실제 저장 안 함** — 응답만 옴 (next request에서 같은 id로 조회해도 변경 없음). 학생에게 명시.

- [ ] **Step 2: Commit**

```powershell
git add -A
git commit -m "external: add JsonPlaceholderClient with list/get/add/complete operations"
```

---

## Task 5: TodoTools (@Tool 5개, TDD)

**Files:**
- Create: `02-external/mcp-server/src/main/java/com/example/external/TodoTools.java`
- Test: `02-external/mcp-server/src/test/java/com/example/external/TodoToolsTest.java`

- [ ] **Step 1: 실패하는 테스트 작성**

Create `02-external/mcp-server/src/test/java/com/example/external/TodoToolsTest.java`:

```java
package com.example.external;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TodoToolsTest {

    private JsonPlaceholderClient client;
    private TodoTools tools;

    @BeforeEach
    void setUp() {
        client = mock(JsonPlaceholderClient.class);
        tools = new TodoTools(client);
    }

    @Test
    void listTodos_passesLimitToClient() {
        when(client.listAll(20)).thenReturn(List.of(new TodoView(1L, 1L, "a", false)));

        List<TodoView> result = tools.listTodos();

        assertEquals(1, result.size());
        verify(client).listAll(20);
    }

    @Test
    void listTodosByUser_delegates() {
        when(client.listByUser(3L)).thenReturn(List.of());
        tools.listTodosByUser(3L);
        verify(client).listByUser(3L);
    }

    @Test
    void getTodo_returnsFoundFalseWhenAbsent() {
        when(client.get(404L)).thenReturn(Optional.empty());
        TodoTools.GetResult r = tools.getTodo(404L);
        assertFalse(r.found());
    }

    @Test
    void getTodo_returnsFoundWhenPresent() {
        TodoView t = new TodoView(1L, 1L, "x", false);
        when(client.get(1L)).thenReturn(Optional.of(t));
        TodoTools.GetResult r = tools.getTodo(1L);
        assertTrue(r.found());
        assertEquals(1L, r.todo().id());
    }

    @Test
    void addTodo_includesNoteAboutFakePersistence() {
        TodoView created = new TodoView(1L, 201L, "new", false);
        when(client.add("new", 1L)).thenReturn(created);

        TodoTools.AddResult result = tools.addTodo("new", 1L);

        assertEquals(201L, result.todo().id());
        assertTrue(result.note().contains("실제 저장되지 않습니다"),
                "JSONPlaceholder fake POST 경고를 응답에 포함해야 함");
    }

    @Test
    void completeTodo_includesFakePersistenceNote() {
        TodoView completed = new TodoView(1L, 1L, "x", true);
        when(client.complete(1L)).thenReturn(completed);

        TodoTools.CompleteResult result = tools.completeTodo(1L);

        assertTrue(result.todo().completed());
        assertTrue(result.note().contains("실제 저장되지 않습니다"));
    }

    @Test
    void searchTodos_filtersByTitleSubstring() {
        when(client.listAll(200)).thenReturn(List.of(
                new TodoView(1L, 1L, "buy milk", false),
                new TodoView(1L, 2L, "read MCP spec", false),
                new TodoView(2L, 3L, "milk shopping", true)
        ));

        List<TodoView> result = tools.searchTodos("milk");

        assertEquals(2, result.size());
    }
}
```

- [ ] **Step 2: 테스트 실행해서 실패 확인**

```powershell
Push-Location 02-external\mcp-server; .\gradlew test --console=plain 2>&1 | Select-Object -Last 10; Pop-Location
```

Expected: 컴파일 에러 — `TodoTools`, `TodoTools.GetResult`, `TodoTools.AddResult`, `TodoTools.CompleteResult` 클래스 없음.

- [ ] **Step 3: TodoTools 작성 (@Tool 5개)**

Create `02-external/mcp-server/src/main/java/com/example/external/TodoTools.java`:

```java
package com.example.external;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TodoTools {

    private static final String FAKE_NOTE =
            "참고: JSONPlaceholder는 데모 API라 POST/PATCH가 실제 저장되지 않습니다. 응답만 돌아옵니다.";

    private final JsonPlaceholderClient client;

    public TodoTools(JsonPlaceholderClient client) {
        this.client = client;
    }

    @Tool(name = "list_todos", description = "JSONPlaceholder fake Todo 목록을 반환합니다 (기본 20건).")
    public List<TodoView> listTodos() {
        return client.listAll(20);
    }

    @Tool(name = "list_todos_by_user",
          description = "특정 사용자(userId)의 할일 목록을 반환합니다.")
    public List<TodoView> listTodosByUser(
            @ToolParam(description = "사용자 ID (1~10)", required = true) Long userId) {
        return client.listByUser(userId);
    }

    @Tool(name = "get_todo", description = "ID로 할일 한 건을 조회합니다.")
    public GetResult getTodo(
            @ToolParam(description = "할일 ID (1~200)", required = true) Long id) {
        return client.get(id)
                .map(t -> new GetResult(true, t))
                .orElseGet(() -> new GetResult(false, null));
    }

    @Tool(name = "add_todo",
          description = "새 할일을 추가합니다. JSONPlaceholder는 fake API라 실제 저장되지 않습니다.")
    public AddResult addTodo(
            @ToolParam(description = "제목", required = true) String title,
            @ToolParam(description = "사용자 ID", required = true) Long userId) {
        TodoView created = client.add(title, userId);
        return new AddResult(created, FAKE_NOTE);
    }

    @Tool(name = "complete_todo",
          description = "할일을 완료 처리합니다. JSONPlaceholder는 fake API라 실제 저장되지 않습니다.")
    public CompleteResult completeTodo(
            @ToolParam(description = "할일 ID", required = true) Long id) {
        TodoView completed = client.complete(id);
        return new CompleteResult(completed, FAKE_NOTE);
    }

    @Tool(name = "search_todos",
          description = "제목에 키워드가 포함된 할일을 검색합니다 (전체 200건 중).")
    public List<TodoView> searchTodos(
            @ToolParam(description = "검색 키워드", required = true) String query) {
        String q = query == null ? "" : query.toLowerCase();
        return client.listAll(200).stream()
                .filter(t -> t.title() != null && t.title().toLowerCase().contains(q))
                .toList();
    }

    public record GetResult(boolean found, TodoView todo) {}
    public record AddResult(TodoView todo, String note) {}
    public record CompleteResult(TodoView todo, String note) {}
}
```

- [ ] **Step 4: 테스트 통과 확인**

```powershell
Push-Location 02-external\mcp-server; .\gradlew test --console=plain 2>&1 | Select-Object -Last 6; Pop-Location
```

Expected: `BUILD SUCCESSFUL`, 7 tests pass.

- [ ] **Step 5: Commit**

```powershell
git add -A
git commit -m "external: add TodoTools with 5 @Tool annotations (fake-persistence notes included)"
```

---

## Task 6: BootJar 빌드 + 수동 시연 검증 (선택)

**Files:** (코드 변경 없음)

- [ ] **Step 1: bootJar 빌드**

```powershell
Push-Location 02-external\mcp-server; .\gradlew bootJar --console=plain 2>&1 | Select-Object -Last 5; Pop-Location
```

Expected: `BUILD SUCCESSFUL`, `02-external/mcp-server/build/libs/mcp-server-external.jar` 생성.

```powershell
Test-Path 02-external\mcp-server\build\libs\mcp-server-external.jar
```

- [ ] **Step 2: (선택) JSONPlaceholder 직접 호출 확인**

JSONPlaceholder가 인터넷 접근 가능한지 확인:

```powershell
curl https://jsonplaceholder.typicode.com/todos/1
```

Expected:
```json
{"userId": 1, "id": 1, "title": "delectus aut autem", "completed": false}
```

이 step은 검증용이라 commit 변경 없음.

---

## Task 7: .mcp.json 작성

**Files:**
- Create: `02-external/.mcp.json`

- [ ] **Step 1: .mcp.json 작성**

Create `02-external/.mcp.json`:

```json
{
  "mcpServers": {
    "todo-external": {
      "command": "java",
      "args": [
        "-jar",
        "C:\\Users\\G\\workspace\\mcp-exam\\02-external\\mcp-server\\build\\libs\\mcp-server-external.jar"
      ]
    }
  }
}
```

> 학생이 받으면 자기 환경의 절대경로로 수정 필요. README에 명시.

- [ ] **Step 2: Commit**

```powershell
git add -A
git commit -m "external: add Claude Code MCP registration (.mcp.json) for external scenario"
```

---

## Task 8: 슬라이드 (Marp)

**Files:**
- Create: `02-external/docs/slides/mcp-lecture-external.md`

- [ ] **Step 1: 디렉토리 + 슬라이드 작성**

```powershell
New-Item -ItemType Directory -Force -Path "02-external\docs\slides" | Out-Null
```

Create `02-external/docs/slides/mcp-lecture-external.md`:

```markdown
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
- 내가 만든 MCP Server (@Tool 5개)
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

## tool 5개

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

> "MCP 공부하기" 추가해줘
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
```

- [ ] **Step 2: Commit**

```powershell
git add -A
git commit -m "external: add Marp slides for 02-external scenario"
```

---

## Task 9: README

**Files:**
- Create: `02-external/README.md`

- [ ] **Step 1: README 작성**

Create `02-external/README.md`:

```markdown
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
"MCP 공부하기" 추가해줘
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
- 회사 방화벽이 차단하는 경우 → 대안: `https://dummyjson.com` 등 다른 fake API

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
```

- [ ] **Step 2: Commit**

```powershell
git add -A
git commit -m "external: add 02-external README with run guide and 01-personal comparison"
```

---

## Task 10: 최종 검증

**Files:** (검증만)

- [ ] **Step 1: 디렉토리 구조 확인**

```powershell
Get-ChildItem 02-external -Recurse -Force | Where-Object { -not $_.PSIsContainer -and $_.FullName -notmatch '\\(\.gradle|build|gradle\\wrapper)\\' } | Select-Object FullName
```

Expected:
- `02-external/mcp-server/build.gradle.kts`
- `02-external/mcp-server/settings.gradle.kts`
- `02-external/mcp-server/gradlew`, `gradlew.bat`
- `02-external/mcp-server/src/main/java/com/example/external/*.java` (4 files: Application, TodoView, JsonPlaceholderClient, TodoTools)
- `02-external/mcp-server/src/main/resources/application.yml`
- `02-external/mcp-server/src/test/java/com/example/external/TodoToolsTest.java`
- `02-external/docs/slides/mcp-lecture-external.md`
- `02-external/.mcp.json`
- `02-external/README.md`

- [ ] **Step 2: 테스트 + 빌드**

```powershell
Push-Location 02-external\mcp-server; .\gradlew test bootJar --console=plain 2>&1 | Select-Object -Last 6; Pop-Location
```

Expected: `BUILD SUCCESSFUL`, 7 tests pass, jar 빌드됨.

- [ ] **Step 3: git log 검토**

```powershell
git log --oneline -10
```

Expected: 최근 ~9개 commit이 02-external 관련.

- [ ] **Step 4: (사용자 위임) Claude Code 자연어 시연**

```powershell
cd 02-external
claude
> 사용자 3의 할일 보여줘
```

확인이 어렵다면 다음 plan(03-organization)으로 진행.

---

## Self-Review 체크리스트

- ✅ Spec coverage: 설계서 §5(시나리오 02-external 전체) 매핑됨
- ✅ Placeholder 없음
- ✅ tool 5개 + 데이터 모델 JSONPlaceholder와 일치
- ✅ 01-personal과의 차이점이 명시적으로 비교 가능 (`diff -r`)

---

## 다음 plan

- `2026-05-28-03-organization-new.md` — HTTP transport + Bearer 인증 + 사용자별 격리
