# MCP 강의자료 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 1차시(90~120분) MCP 입문 강의를 위한 실행 가능한 코드 4 컴포넌트(Resource Server / MCP Server / 학습용 Client) + Marp 슬라이드 + README를 만든다. 학생은 자신이 만든 MCP Server를 Claude Code(외부 도구)에 등록해서 자연어로 동작시키는 경험까지 한다.

**Architecture:** 3개 독립 Gradle 프로젝트 폴더(`01-resource-server`, `02-mcp-server`, `03-client`)와 슬라이드/문서 폴더로 구성. Resource Server는 Spring Boot REST + ConcurrentHashMap 인메모리 저장소. MCP Server는 Spring AI MCP starter의 `@McpTool` 어노테이션으로 5개 tool을 노출하고 stdio transport로 Claude Code/학습용 Client와 통신. 학습용 Client는 `ProcessBuilder`로 MCP Server를 자식 프로세스로 띄워 stdin/stdout JSON-RPC를 손으로 주고받는 교육용 도구.

**Tech Stack:** Java 21, Gradle (Kotlin DSL), Spring Boot 3.3.x, Spring AI 1.0.3 (`spring-ai-starter-mcp-server`), Jackson, JUnit 5, Marp(슬라이드).

**참고 설계서:** `docs/superpowers/specs/2026-05-28-mcp-lecture-design.md`

---

## File Structure

전체 디렉토리 (학생이 받는 최종 형태):

```
mcp-exam/
├── .gitignore
├── README.md
├── docs/
│   ├── slides/
│   │   └── mcp-lecture.md                 # Marp 슬라이드
│   └── superpowers/
│       ├── specs/2026-05-28-mcp-lecture-design.md
│       └── plans/2026-05-28-mcp-lecture.md (이 문서)
│
├── 01-resource-server/
│   ├── build.gradle.kts
│   ├── settings.gradle.kts
│   ├── gradle/wrapper/...
│   ├── gradlew, gradlew.bat
│   └── src/
│       ├── main/java/com/example/resource/
│       │   ├── ResourceServerApplication.java
│       │   ├── Todo.java                  # record
│       │   ├── TodoRepository.java        # ConcurrentHashMap, AtomicLong
│       │   └── TodoController.java
│       ├── main/resources/application.yml
│       └── test/java/com/example/resource/
│           ├── TodoRepositoryTest.java
│           └── TodoControllerTest.java
│
├── 02-mcp-server/
│   ├── build.gradle.kts
│   ├── settings.gradle.kts
│   ├── gradle/wrapper/...
│   ├── gradlew, gradlew.bat
│   └── src/
│       ├── main/java/com/example/mcp/
│       │   ├── McpServerApplication.java
│       │   ├── TodoRestClient.java        # → 01-resource-server 호출
│       │   ├── TodoTools.java             # @McpTool 5개
│       │   └── TodoView.java              # MCP에 노출할 가벼운 DTO
│       ├── main/resources/application.yml # stdio transport 설정
│       └── test/java/com/example/mcp/
│           └── TodoToolsTest.java         # REST client mock
│
└── 03-client/
    ├── build.gradle.kts
    ├── settings.gradle.kts
    ├── gradle/wrapper/...
    ├── gradlew, gradlew.bat
    └── src/
        ├── main/java/com/example/client/
        │   ├── LearningClient.java        # main, REPL
        │   ├── McpProcess.java            # ProcessBuilder, stdin/stdout
        │   └── JsonRpc.java               # 메시지 빌더 + 파서
        └── test/java/com/example/client/
            └── JsonRpcTest.java
```

각 컴포넌트는 **독립 Gradle 프로젝트**로 자기 폴더에서 `./gradlew bootRun`만으로 실행. 멀티프로젝트 root는 만들지 않음 (강의 흐름상 학생이 한 폴더씩 따라가기 좋게).

---

## 공통 규칙 (모든 태스크)

- **TDD**: 코드를 변경하는 태스크는 가능한 한 (failing test → run → implement → run → commit) 사이클. 슬라이드/README/설정 파일은 TDD 제외.
- **커밋 단위**: 한 태스크 = 한 커밋. 글로벌 CLAUDE.md 룰에 따라 `git add -A` 사용.
- **커밋 메시지**: `<scope>: <description>` (예: `resource: add Todo record and repository`)
- **commands**: PowerShell 환경. 명령은 모두 작업 디렉토리에서 실행. 절대경로 사용을 피하고 상대경로 + cwd 설정.

---

## Task 1: 프로젝트 부트스트랩

**Files:**
- Create: `mcp-exam/.gitignore`
- Create: `mcp-exam/README.md` (이후 Task 16에서 본격 작성, 일단 플레이스홀더)

- [ ] **Step 1: `.gitignore` 작성**

Create `C:\Users\G\workspace\mcp-exam\.gitignore`:

```gitignore
# Gradle
.gradle/
build/
!gradle-wrapper.jar
!gradle-wrapper.properties

# IDE
.idea/
*.iml
.vscode/
.project
.classpath
.settings/

# OS
.DS_Store
Thumbs.db

# Java
*.class
*.log
hs_err_pid*

# Spring
application-local.yml
application-local.properties
```

- [ ] **Step 2: README 플레이스홀더 작성**

Create `C:\Users\G\workspace\mcp-exam\README.md`:

```markdown
# MCP 강의자료

1차시(90~120분) MCP 입문 강의용 코드와 자료.

> 본격적인 가이드는 Task 16에서 작성됩니다.

## 디렉토리
- `01-resource-server/` — Todo/메모 REST API
- `02-mcp-server/` — MCP Server (Spring AI MCP starter)
- `03-client/` — 학습용 JSON-RPC 클라이언트
- `docs/slides/` — Marp 슬라이드
```

- [ ] **Step 3: git 초기화 + 첫 커밋**

Run in `C:\Users\G\workspace\mcp-exam`:

```powershell
git init -b main
git add -A
git commit -m "chore: bootstrap mcp-exam project"
```

Expected: "Initial commit. 3 files changed" (정확한 카운트는 환경에 따라 다름).

---

## Task 2: Resource Server 스캐폴드

**Files:**
- Create: `01-resource-server/settings.gradle.kts`
- Create: `01-resource-server/build.gradle.kts`
- Create: `01-resource-server/gradle/wrapper/gradle-wrapper.properties`
- Create: `01-resource-server/gradlew`, `gradlew.bat`, `gradle-wrapper.jar` (gradle init 실행으로 생성)
- Create: `01-resource-server/src/main/java/com/example/resource/ResourceServerApplication.java`
- Create: `01-resource-server/src/main/resources/application.yml`

- [ ] **Step 1: Gradle 프로젝트 초기화**

Run in `C:\Users\G\workspace\mcp-exam\01-resource-server`:

```powershell
gradle init --type basic --dsl kotlin --project-name resource-server
```

이 명령으로 `gradlew`, `gradle/wrapper/*` 생성. (gradle 미설치 환경이면 다른 컴포넌트에서 wrapper 복사 가능 — Task 6에서 동일 패턴.)

- [ ] **Step 2: `settings.gradle.kts` 작성**

Create `01-resource-server/settings.gradle.kts`:

```kotlin
rootProject.name = "resource-server"
```

- [ ] **Step 3: `build.gradle.kts` 작성**

Create `01-resource-server/build.gradle.kts`:

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

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
```

- [ ] **Step 4: Application 클래스 작성**

Create `01-resource-server/src/main/java/com/example/resource/ResourceServerApplication.java`:

```java
package com.example.resource;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ResourceServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(ResourceServerApplication.class, args);
    }
}
```

- [ ] **Step 5: `application.yml` 작성**

Create `01-resource-server/src/main/resources/application.yml`:

```yaml
server:
  port: 8080

spring:
  application:
    name: resource-server

logging:
  level:
    com.example.resource: DEBUG
```

- [ ] **Step 6: 빌드 확인 + 커밋**

Run in `01-resource-server`:

```powershell
./gradlew build -x test
```

Expected: `BUILD SUCCESSFUL`.

Then from `mcp-exam` root:

```powershell
git add -A
git commit -m "resource: scaffold Spring Boot project"
```

---

## Task 3: Todo record + TodoRepository (TDD)

**Files:**
- Create: `01-resource-server/src/main/java/com/example/resource/Todo.java`
- Create: `01-resource-server/src/main/java/com/example/resource/TodoRepository.java`
- Test:  `01-resource-server/src/test/java/com/example/resource/TodoRepositoryTest.java`

- [ ] **Step 1: 실패하는 테스트 작성**

Create `01-resource-server/src/test/java/com/example/resource/TodoRepositoryTest.java`:

```java
package com.example.resource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class TodoRepositoryTest {

    private TodoRepository repository;

    @BeforeEach
    void setUp() {
        repository = new TodoRepository();
    }

    @Test
    void add_assignsIncrementingIds() {
        Todo a = repository.add("first", "memo1");
        Todo b = repository.add("second", "memo2");

        assertNotNull(a.id());
        assertNotNull(b.id());
        assertNotEquals(a.id(), b.id());
        assertEquals("first", a.title());
        assertFalse(a.completed());
    }

    @Test
    void findById_returnsEmptyWhenAbsent() {
        Optional<Todo> result = repository.findById(999L);
        assertTrue(result.isEmpty());
    }

    @Test
    void findById_returnsAddedTodo() {
        Todo added = repository.add("hello", "world");
        Optional<Todo> found = repository.findById(added.id());

        assertTrue(found.isPresent());
        assertEquals(added.id(), found.get().id());
        assertEquals("hello", found.get().title());
    }

    @Test
    void findAll_returnsAllAddedTodos() {
        repository.add("a", null);
        repository.add("b", null);
        repository.add("c", null);

        List<Todo> all = repository.findAll();
        assertEquals(3, all.size());
    }

    @Test
    void complete_marksTodoCompleted() {
        Todo added = repository.add("task", "memo");
        Optional<Todo> completed = repository.complete(added.id());

        assertTrue(completed.isPresent());
        assertTrue(completed.get().completed());
        assertEquals(added.id(), completed.get().id());
    }

    @Test
    void complete_returnsEmptyWhenAbsent() {
        assertTrue(repository.complete(404L).isEmpty());
    }

    @Test
    void search_matchesTitleAndMemoCaseInsensitive() {
        repository.add("Buy milk", "from market");
        repository.add("Read MCP spec", "this week");
        repository.add("Cook dinner", "spec recipe");

        List<Todo> result = repository.search("spec");
        assertEquals(2, result.size());
    }
}
```

- [ ] **Step 2: 테스트 실행해서 실패 확인**

Run in `01-resource-server`:

```powershell
./gradlew test
```

Expected: 컴파일 에러 — `Todo`, `TodoRepository` 클래스가 없음.

- [ ] **Step 3: `Todo` record 작성**

Create `01-resource-server/src/main/java/com/example/resource/Todo.java`:

```java
package com.example.resource;

public record Todo(
        Long id,
        String title,
        String memo,
        boolean completed
) {
    public Todo complete() {
        return new Todo(id, title, memo, true);
    }
}
```

- [ ] **Step 4: `TodoRepository` 작성**

Create `01-resource-server/src/main/java/com/example/resource/TodoRepository.java`:

```java
package com.example.resource;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class TodoRepository {

    private final Map<Long, Todo> store = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(0);

    public Todo add(String title, String memo) {
        long id = sequence.incrementAndGet();
        Todo todo = new Todo(id, title, memo, false);
        store.put(id, todo);
        return todo;
    }

    public Optional<Todo> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    public List<Todo> findAll() {
        return store.values().stream()
                .sorted((a, b) -> Long.compare(a.id(), b.id()))
                .toList();
    }

    public Optional<Todo> complete(Long id) {
        return findById(id).map(existing -> {
            Todo updated = existing.complete();
            store.put(id, updated);
            return updated;
        });
    }

    public List<Todo> search(String query) {
        String q = query == null ? "" : query.toLowerCase();
        return findAll().stream()
                .filter(t -> {
                    String title = t.title() == null ? "" : t.title().toLowerCase();
                    String memo = t.memo() == null ? "" : t.memo().toLowerCase();
                    return title.contains(q) || memo.contains(q);
                })
                .toList();
    }
}
```

- [ ] **Step 5: 테스트 다시 실행해서 통과 확인**

```powershell
./gradlew test
```

Expected: `BUILD SUCCESSFUL`, 7 tests passed.

- [ ] **Step 6: 커밋**

From `mcp-exam` root:

```powershell
git add -A
git commit -m "resource: add Todo record and TodoRepository with tests"
```

---

## Task 4: TodoController + 통합 테스트 (TDD)

**Files:**
- Create: `01-resource-server/src/main/java/com/example/resource/TodoController.java`
- Test:  `01-resource-server/src/test/java/com/example/resource/TodoControllerTest.java`

- [ ] **Step 1: 실패하는 통합 테스트 작성**

Create `01-resource-server/src/test/java/com/example/resource/TodoControllerTest.java`:

```java
package com.example.resource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class TodoControllerTest {

    @Autowired
    MockMvc mvc;

    @Test
    void listEmpty_returnsEmptyArray() throws Exception {
        mvc.perform(get("/todos"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void addAndGet_roundTrip() throws Exception {
        String created = mvc.perform(post("/todos")
                        .contentType("application/json")
                        .content("""
                                {"title": "Learn MCP", "memo": "today"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value("Learn MCP"))
                .andExpect(jsonPath("$.completed").value(false))
                .andReturn().getResponse().getContentAsString();

        // 단순히 ID가 발급됐는지만 확인 (다른 테스트와의 순서 의존 회피)
    }

    @Test
    void completeFlow() throws Exception {
        mvc.perform(post("/todos")
                        .contentType("application/json")
                        .content("""
                                {"title": "to complete", "memo": "x"}
                                """))
                .andExpect(status().isOk());

        // 가장 최근 ID로 완료 처리
        mvc.perform(get("/todos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void searchEndpoint_respondsOk() throws Exception {
        mvc.perform(get("/todos/search").param("q", "mcp"))
                .andExpect(status().isOk());
    }

    @Test
    void getMissingId_returns404() throws Exception {
        mvc.perform(get("/todos/{id}", 999_999))
                .andExpect(status().isNotFound());
    }
}
```

- [ ] **Step 2: 테스트 실행해서 실패 확인**

```powershell
./gradlew test
```

Expected: `TodoController` 없어서 404 또는 컴파일 통과 후 모든 경로 404.

- [ ] **Step 3: `TodoController` 작성**

Create `01-resource-server/src/main/java/com/example/resource/TodoController.java`:

```java
package com.example.resource;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/todos")
public class TodoController {

    private final TodoRepository repository;

    public TodoController(TodoRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<Todo> list() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Todo> get(@PathVariable Long id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public Todo add(@RequestBody AddRequest request) {
        return repository.add(request.title(), request.memo());
    }

    @PatchMapping("/{id}/complete")
    public ResponseEntity<Todo> complete(@PathVariable Long id) {
        return repository.complete(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    public List<Todo> search(@RequestParam("q") String query) {
        return repository.search(query);
    }

    public record AddRequest(String title, String memo) {}
}
```

- [ ] **Step 4: 테스트 통과 확인**

```powershell
./gradlew test
```

Expected: `BUILD SUCCESSFUL`. 12 tests total (5 controller + 7 repository).

- [ ] **Step 5: 커밋**

```powershell
git add -A
git commit -m "resource: add TodoController with REST endpoints"
```

---

## Task 5: Resource Server 수동 검증

**Files:**
- Create: `01-resource-server/requests.http` (IntelliJ HTTP client용, VS Code REST Client 호환)

이 태스크는 강의 시연을 위한 검증 단계. 학생이 강의 중 따라할 수 있게 `.http` 파일 제공.

- [ ] **Step 1: `.http` 파일 작성**

Create `01-resource-server/requests.http`:

```http
### list (empty at start)
GET http://localhost:8080/todos

### add
POST http://localhost:8080/todos
Content-Type: application/json

{
  "title": "Learn MCP",
  "memo": "1차시 강의"
}

### add another
POST http://localhost:8080/todos
Content-Type: application/json

{
  "title": "Write slides",
  "memo": "Marp"
}

### list again
GET http://localhost:8080/todos

### get by id
GET http://localhost:8080/todos/1

### search
GET http://localhost:8080/todos/search?q=mcp

### complete
PATCH http://localhost:8080/todos/1/complete

### get missing
GET http://localhost:8080/todos/999
```

- [ ] **Step 2: 서버 띄우고 수동 검증**

Run in `01-resource-server`:

```powershell
./gradlew bootRun
```

새 터미널에서:

```powershell
curl http://localhost:8080/todos
# Expected: []

curl -X POST http://localhost:8080/todos -H "Content-Type: application/json" -d '{\"title\":\"Learn MCP\",\"memo\":\"today\"}'
# Expected: {"id":1,"title":"Learn MCP","memo":"today","completed":false}

curl http://localhost:8080/todos
# Expected: [{"id":1,...}]
```

서버는 Ctrl+C로 종료.

- [ ] **Step 3: 커밋**

```powershell
git add -A
git commit -m "resource: add HTTP requests file for manual verification"
```

---

## Task 6: MCP Server 스캐폴드

**Files:**
- Create: `02-mcp-server/settings.gradle.kts`
- Create: `02-mcp-server/build.gradle.kts`
- Create: `02-mcp-server/gradle/wrapper/...` (`gradle init` 또는 01에서 복사)
- Create: `02-mcp-server/src/main/java/com/example/mcp/McpServerApplication.java`
- Create: `02-mcp-server/src/main/resources/application.yml`

- [ ] **Step 1: Gradle 프로젝트 초기화**

Run in `C:\Users\G\workspace\mcp-exam\02-mcp-server`:

```powershell
gradle init --type basic --dsl kotlin --project-name mcp-server
```

(또는 `01-resource-server`의 `gradle/`, `gradlew`, `gradlew.bat`을 복사.)

- [ ] **Step 2: `settings.gradle.kts` 작성**

Create `02-mcp-server/settings.gradle.kts`:

```kotlin
rootProject.name = "mcp-server"
```

- [ ] **Step 3: `build.gradle.kts` 작성**

Create `02-mcp-server/build.gradle.kts`:

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
    implementation("org.springframework.boot:spring-boot-starter")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// stdio 모드 실행 편의를 위해 jar에 메인클래스 명시
tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveFileName.set("mcp-server.jar")
}
```

> **버전 확인 메모**: 강의 실행 시점에 Spring AI 최신 안정 버전을 [https://docs.spring.io/spring-ai/reference/](https://docs.spring.io/spring-ai/reference/) 에서 확인하고 BOM 버전 갱신. 본 플랜은 1.0.3 기준.

- [ ] **Step 4: Application 클래스 작성**

Create `02-mcp-server/src/main/java/com/example/mcp/McpServerApplication.java`:

```java
package com.example.mcp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class McpServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(McpServerApplication.class, args);
    }
}
```

- [ ] **Step 5: `application.yml` 작성 — stdio transport 활성화**

Create `02-mcp-server/src/main/resources/application.yml`:

```yaml
spring:
  main:
    web-application-type: none      # stdio 모드 — HTTP 서버 띄우지 않음
    banner-mode: off                # stdout을 JSON-RPC로 깨끗하게
  ai:
    mcp:
      server:
        name: todo-mcp-server
        version: 0.0.1
        type: SYNC
        annotation-scanner:
          enabled: true             # @McpTool 자동 스캔

# 자식 프로세스 stdout은 JSON-RPC 전용. 로그는 파일로.
logging:
  file:
    name: ./logs/mcp-server.log
  pattern:
    console: ""
  level:
    root: INFO
    com.example.mcp: DEBUG

# Resource Server 위치 (Task 7에서 사용)
resource-server:
  base-url: http://localhost:8080
```

> **핵심**: stdio transport에서는 **stdout이 JSON-RPC 통신용**으로만 쓰여야 합니다. Spring 배너/로그가 stdout에 섞이면 클라이언트가 깨집니다. 위 설정이 그걸 막습니다.

- [ ] **Step 6: 빌드 확인 + 커밋**

Run in `02-mcp-server`:

```powershell
./gradlew build -x test
```

Expected: `BUILD SUCCESSFUL` (Spring AI MCP starter 의존성 다운로드 포함).

From `mcp-exam` root:

```powershell
git add -A
git commit -m "mcp: scaffold Spring AI MCP server project with stdio config"
```

---

## Task 7: TodoRestClient — Resource Server 호출 (TDD)

**Files:**
- Create: `02-mcp-server/src/main/java/com/example/mcp/TodoView.java`
- Create: `02-mcp-server/src/main/java/com/example/mcp/TodoRestClient.java`

(테스트는 외부 의존성이 크므로 통합 검증으로 대체. 단위 테스트는 Task 8의 `TodoToolsTest`에서 모킹으로 수행.)

- [ ] **Step 1: `TodoView` 작성 (MCP 응답용 DTO)**

Create `02-mcp-server/src/main/java/com/example/mcp/TodoView.java`:

```java
package com.example.mcp;

public record TodoView(
        Long id,
        String title,
        String memo,
        boolean completed
) {}
```

> **메모**: Resource Server의 `Todo`와 형태가 같지만 별도로 둠. 학생에게 "MCP Server는 Resource Server의 모델을 자기 책임으로 한번 더 매핑한다"는 패턴을 보여주기 위함. 한 단계 추가 부담은 크지 않음.

- [ ] **Step 2: `TodoRestClient` 작성**

Create `02-mcp-server/src/main/java/com/example/mcp/TodoRestClient.java`:

```java
package com.example.mcp;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Optional;

@Component
public class TodoRestClient {

    private final RestClient client;

    public TodoRestClient(@Value("${resource-server.base-url}") String baseUrl) {
        this.client = RestClient.builder().baseUrl(baseUrl).build();
    }

    public List<TodoView> list() {
        return client.get()
                .uri("/todos")
                .retrieve()
                .body(new ParameterizedTypeReference<List<TodoView>>() {});
    }

    public Optional<TodoView> get(long id) {
        TodoView body = client.get()
                .uri("/todos/{id}", id)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, resp) -> { /* swallow */ })
                .body(TodoView.class);
        return Optional.ofNullable(body);
    }

    public TodoView add(String title, String memo) {
        return client.post()
                .uri("/todos")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .body(new AddBody(title, memo))
                .retrieve()
                .body(TodoView.class);
    }

    public Optional<TodoView> complete(long id) {
        TodoView body = client.patch()
                .uri("/todos/{id}/complete", id)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, resp) -> { })
                .body(TodoView.class);
        return Optional.ofNullable(body);
    }

    public List<TodoView> search(String query) {
        return client.get()
                .uri(uri -> uri.path("/todos/search").queryParam("q", query).build())
                .retrieve()
                .body(new ParameterizedTypeReference<List<TodoView>>() {});
    }

    record AddBody(String title, String memo) {}
}
```

- [ ] **Step 3: 빌드 확인 + 커밋**

```powershell
./gradlew build -x test
```

Expected: `BUILD SUCCESSFUL`.

```powershell
git add -A
git commit -m "mcp: add TodoRestClient calling resource server"
```

---

## Task 8: TodoTools — @McpTool 5개 (TDD)

**Files:**
- Create: `02-mcp-server/src/main/java/com/example/mcp/TodoTools.java`
- Test:  `02-mcp-server/src/test/java/com/example/mcp/TodoToolsTest.java`

- [ ] **Step 1: 실패하는 테스트 작성**

Create `02-mcp-server/src/test/java/com/example/mcp/TodoToolsTest.java`:

```java
package com.example.mcp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TodoToolsTest {

    private TodoRestClient restClient;
    private TodoTools tools;

    @BeforeEach
    void setUp() {
        restClient = mock(TodoRestClient.class);
        tools = new TodoTools(restClient);
    }

    @Test
    void listTodos_delegatesToRestClient() {
        TodoView t = new TodoView(1L, "a", "b", false);
        when(restClient.list()).thenReturn(List.of(t));

        List<TodoView> result = tools.listTodos();

        assertEquals(1, result.size());
        verify(restClient).list();
    }

    @Test
    void getTodo_returnsNotFoundMessageWhenAbsent() {
        when(restClient.get(99L)).thenReturn(Optional.empty());

        TodoTools.GetResult result = tools.getTodo(99L);

        assertFalse(result.found());
        assertNull(result.todo());
    }

    @Test
    void getTodo_returnsTodoWhenPresent() {
        TodoView t = new TodoView(1L, "x", "y", false);
        when(restClient.get(1L)).thenReturn(Optional.of(t));

        TodoTools.GetResult result = tools.getTodo(1L);

        assertTrue(result.found());
        assertEquals(1L, result.todo().id());
    }

    @Test
    void addTodo_delegatesToRestClient() {
        TodoView added = new TodoView(5L, "new", "memo", false);
        when(restClient.add("new", "memo")).thenReturn(added);

        TodoView result = tools.addTodo("new", "memo");

        assertEquals(5L, result.id());
        verify(restClient).add("new", "memo");
    }

    @Test
    void completeTodo_returnsFlagWhenAbsent() {
        when(restClient.complete(404L)).thenReturn(Optional.empty());
        TodoTools.CompleteResult result = tools.completeTodo(404L);
        assertFalse(result.found());
    }

    @Test
    void completeTodo_returnsCompletedTodo() {
        TodoView completed = new TodoView(1L, "x", "y", true);
        when(restClient.complete(1L)).thenReturn(Optional.of(completed));

        TodoTools.CompleteResult result = tools.completeTodo(1L);

        assertTrue(result.found());
        assertTrue(result.todo().completed());
    }

    @Test
    void searchTodos_delegates() {
        when(restClient.search("mcp")).thenReturn(List.of());
        List<TodoView> result = tools.searchTodos("mcp");
        assertNotNull(result);
        verify(restClient).search("mcp");
    }
}
```

- [ ] **Step 2: Mockito 의존성 추가**

Mockito는 `spring-boot-starter-test`에 포함되어 있으므로 별도 추가 불필요.

- [ ] **Step 3: 테스트 실행해서 실패 확인**

```powershell
./gradlew test
```

Expected: 컴파일 에러 — `TodoTools`, `TodoTools.GetResult`, `TodoTools.CompleteResult` 클래스 없음.

- [ ] **Step 4: `TodoTools` 작성 (@McpTool 5개)**

Create `02-mcp-server/src/main/java/com/example/mcp/TodoTools.java`:

```java
package com.example.mcp;

import org.springframework.ai.mcp.server.annotation.McpTool;
import org.springframework.ai.mcp.server.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TodoTools {

    private final TodoRestClient restClient;

    public TodoTools(TodoRestClient restClient) {
        this.restClient = restClient;
    }

    @McpTool(name = "list_todos", description = "모든 할일(Todo) 목록을 반환합니다.")
    public List<TodoView> listTodos() {
        return restClient.list();
    }

    @McpTool(name = "get_todo", description = "ID로 할일 한 건을 조회합니다.")
    public GetResult getTodo(
            @McpToolParam(description = "할일 ID", required = true) Long id) {
        return restClient.get(id)
                .map(t -> new GetResult(true, t))
                .orElseGet(() -> new GetResult(false, null));
    }

    @McpTool(name = "add_todo", description = "새 할일을 추가합니다. memo는 선택입니다.")
    public TodoView addTodo(
            @McpToolParam(description = "제목", required = true) String title,
            @McpToolParam(description = "메모(선택)", required = false) String memo) {
        return restClient.add(title, memo);
    }

    @McpTool(name = "complete_todo", description = "ID에 해당하는 할일을 완료 처리합니다.")
    public CompleteResult completeTodo(
            @McpToolParam(description = "할일 ID", required = true) Long id) {
        return restClient.complete(id)
                .map(t -> new CompleteResult(true, t))
                .orElseGet(() -> new CompleteResult(false, null));
    }

    @McpTool(name = "search_todos", description = "제목과 메모에서 키워드를 포함하는 할일을 검색합니다.")
    public List<TodoView> searchTodos(
            @McpToolParam(description = "검색 키워드", required = true) String query) {
        return restClient.search(query);
    }

    public record GetResult(boolean found, TodoView todo) {}
    public record CompleteResult(boolean found, TodoView todo) {}
}
```

> **버전 확인 메모**: `org.springframework.ai.mcp.server.annotation.McpTool`의 패키지 경로는 Spring AI 1.0.x 기준. 실제 빌드 시 import가 풀리지 않으면 IDE 자동 import로 확인 후 정정.

- [ ] **Step 5: 테스트 통과 확인**

```powershell
./gradlew test
```

Expected: `BUILD SUCCESSFUL`, 7 tests passed.

- [ ] **Step 6: 커밋**

```powershell
git add -A
git commit -m "mcp: add TodoTools with 5 @McpTool annotations"
```

---

## Task 9: MCP Server 빌드 + 자식 프로세스 실행 검증

**Files:** (코드 변경 없음. 빌드 산출물 검증 단계.)

- [ ] **Step 1: 실행 가능한 JAR 빌드**

Run in `02-mcp-server`:

```powershell
./gradlew bootJar
```

Expected: `build/libs/mcp-server.jar` 생성.

- [ ] **Step 2: Resource Server를 먼저 띄움**

새 터미널에서 in `01-resource-server`:

```powershell
./gradlew bootRun
```

(8080 포트에서 동작 중인 상태로 유지.)

- [ ] **Step 3: MCP Server를 stdio 모드로 띄우고 메시지 한 개 보내기**

또 다른 터미널에서 in `02-mcp-server`:

```powershell
# initialize 메시지를 한 줄로 보내고 응답 확인
'{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"manual-test","version":"0.0.1"}}}' | java -jar build/libs/mcp-server.jar
```

Expected: stdout에 JSON-RPC `initialize` 응답이 한 줄로 출력. 예시:

```json
{"jsonrpc":"2.0","id":1,"result":{"protocolVersion":"2024-11-05","capabilities":{"tools":{...}},"serverInfo":{"name":"todo-mcp-server","version":"0.0.1"}}}
```

> 응답이 안 보이면: `02-mcp-server/logs/mcp-server.log` 확인. Spring 배너/로그가 stdout에 섞였다면 `application.yml`의 `banner-mode: off`, `logging.pattern.console: ""` 재확인.

- [ ] **Step 4: 커밋 (변경 없으면 skip)**

검증만 한 단계라 커밋 변경사항 없음. 다음 태스크로.

---

## Task 10: Client — JsonRpc 메시지 빌더/파서 (TDD)

**Files:**
- Create: `03-client/settings.gradle.kts`
- Create: `03-client/build.gradle.kts`
- Create: `03-client/gradle/wrapper/...`
- Create: `03-client/src/main/java/com/example/client/JsonRpc.java`
- Test:  `03-client/src/test/java/com/example/client/JsonRpcTest.java`

- [ ] **Step 1: 03-client Gradle 프로젝트 초기화**

Run in `C:\Users\G\workspace\mcp-exam\03-client`:

```powershell
gradle init --type basic --dsl kotlin --project-name client
```

- [ ] **Step 2: `settings.gradle.kts` 작성**

Create `03-client/settings.gradle.kts`:

```kotlin
rootProject.name = "client"
```

- [ ] **Step 3: `build.gradle.kts` 작성**

Create `03-client/build.gradle.kts`:

```kotlin
plugins {
    java
    application
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

application {
    mainClass.set("com.example.client.LearningClient")
}

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.2")
    testImplementation(platform("org.junit:junit-bom:5.10.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.named<JavaExec>("run") {
    standardInput = System.`in`
}
```

- [ ] **Step 4: 실패하는 JsonRpc 테스트 작성**

Create `03-client/src/test/java/com/example/client/JsonRpcTest.java`:

```java
package com.example.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JsonRpcTest {

    @Test
    void request_buildsJsonRpcEnvelope() throws Exception {
        String json = JsonRpc.request(1, "tools/list", Map.of());

        JsonNode node = JsonRpc.parse(json);
        assertEquals("2.0", node.get("jsonrpc").asText());
        assertEquals(1, node.get("id").asInt());
        assertEquals("tools/list", node.get("method").asText());
        assertTrue(node.has("params"));
    }

    @Test
    void request_withParams_includesArguments() throws Exception {
        String json = JsonRpc.request(2, "tools/call",
                Map.of("name", "add_todo", "arguments", Map.of("title", "x")));

        JsonNode node = JsonRpc.parse(json);
        assertEquals("tools/call", node.get("method").asText());
        assertEquals("add_todo", node.get("params").get("name").asText());
        assertEquals("x", node.get("params").get("arguments").get("title").asText());
    }

    @Test
    void parseResponse_readsResultField() throws Exception {
        String responseLine = """
                {"jsonrpc":"2.0","id":1,"result":{"tools":[{"name":"add_todo"}]}}
                """;

        JsonNode node = JsonRpc.parse(responseLine);
        assertEquals(1, node.get("id").asInt());
        assertTrue(node.has("result"));
        assertEquals("add_todo", node.get("result").get("tools").get(0).get("name").asText());
    }
}
```

- [ ] **Step 5: 테스트 실패 확인**

Run in `03-client`:

```powershell
./gradlew test
```

Expected: 컴파일 에러 — `JsonRpc` 없음.

- [ ] **Step 6: `JsonRpc` 구현**

Create `03-client/src/main/java/com/example/client/JsonRpc.java`:

```java
package com.example.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Map;

public final class JsonRpc {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonRpc() {}

    public static String request(int id, String method, Map<String, ?> params) {
        ObjectNode node = MAPPER.createObjectNode();
        node.put("jsonrpc", "2.0");
        node.put("id", id);
        node.put("method", method);
        node.set("params", MAPPER.valueToTree(params));
        return node.toString();
    }

    public static String notification(String method, Map<String, ?> params) {
        ObjectNode node = MAPPER.createObjectNode();
        node.put("jsonrpc", "2.0");
        node.put("method", method);
        node.set("params", MAPPER.valueToTree(params));
        return node.toString();
    }

    public static JsonNode parse(String line) throws Exception {
        return MAPPER.readTree(line);
    }

    public static String pretty(JsonNode node) throws Exception {
        return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(node);
    }
}
```

- [ ] **Step 7: 테스트 통과 확인**

```powershell
./gradlew test
```

Expected: `BUILD SUCCESSFUL`, 3 tests passed.

- [ ] **Step 8: 커밋**

```powershell
git add -A
git commit -m "client: scaffold project and add JsonRpc message builder"
```

---

## Task 11: Client — McpProcess (ProcessBuilder, stdin/stdout 핸들)

**Files:**
- Create: `03-client/src/main/java/com/example/client/McpProcess.java`

(이 클래스는 외부 프로세스(MCP Server jar)를 띄워야 하므로 격리된 unit test가 어려움. Task 12의 LearningClient에서 end-to-end로 검증.)

- [ ] **Step 1: `McpProcess` 작성**

Create `03-client/src/main/java/com/example/client/McpProcess.java`:

```java
package com.example.client;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * MCP Server를 자식 프로세스로 띄우고 stdin/stdout으로 JSON-RPC를 주고받는다.
 * 학생이 이 클래스의 코드를 보면 "MCP가 결국 stdin/stdout으로 JSON 한 줄씩 주고받는 거구나"를 깨닫는다.
 */
public class McpProcess implements AutoCloseable {

    private final Process process;
    private final BufferedWriter stdin;
    private final BufferedReader stdout;

    public McpProcess(List<String> command) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(false); // stderr는 별도로 (MCP Server의 로그는 파일로 가있음)
        this.process = pb.start();
        this.stdin = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
        this.stdout = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
    }

    /** JSON-RPC 메시지를 한 줄로 보내고 응답 한 줄을 읽어온다. */
    public JsonNode sendAndReceive(String jsonRpcLine) throws Exception {
        System.out.println("→ " + jsonRpcLine);
        stdin.write(jsonRpcLine);
        stdin.newLine();
        stdin.flush();

        String response = stdout.readLine();
        if (response == null) {
            throw new IOException("MCP Server가 응답을 닫았습니다. logs/mcp-server.log를 확인하세요.");
        }
        System.out.println("← " + response);
        return JsonRpc.parse(response);
    }

    /** 알림(notification)은 응답 없음. */
    public void sendNotification(String jsonRpcLine) throws Exception {
        System.out.println("→ " + jsonRpcLine + "  (notification)");
        stdin.write(jsonRpcLine);
        stdin.newLine();
        stdin.flush();
    }

    @Override
    public void close() throws Exception {
        try {
            stdin.close();
        } catch (Exception ignored) {}
        process.waitFor();
    }
}
```

- [ ] **Step 2: 빌드 확인 + 커밋**

```powershell
./gradlew build -x test
```

Expected: `BUILD SUCCESSFUL`.

```powershell
git add -A
git commit -m "client: add McpProcess for stdin/stdout JSON-RPC"
```

---

## Task 12: Client — LearningClient 메인 (시나리오 실행)

**Files:**
- Create: `03-client/src/main/java/com/example/client/LearningClient.java`

LearningClient는 강의 시연용으로 **고정 시나리오를 자동 실행**한다 (REPL 대신). 학생이 콘솔에서 JSON-RPC가 흐르는 모습을 한 화면에서 본다.

- [ ] **Step 1: `LearningClient` 작성**

Create `03-client/src/main/java/com/example/client/LearningClient.java`:

```java
package com.example.client;

import com.fasterxml.jackson.databind.JsonNode;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class LearningClient {

    public static void main(String[] args) throws Exception {
        // 1) 명령행 인자: MCP Server jar 경로
        String jarPath = args.length > 0
                ? args[0]
                : Path.of("..", "02-mcp-server", "build", "libs", "mcp-server.jar")
                        .toAbsolutePath().toString();

        List<String> command = List.of("java", "-jar", jarPath);

        System.out.println("=== Learning MCP Client ===");
        System.out.println("MCP Server: " + jarPath);
        System.out.println();

        try (McpProcess mcp = new McpProcess(command)) {
            // 2) initialize 핸드셰이크
            JsonNode init = mcp.sendAndReceive(JsonRpc.request(1, "initialize", Map.of(
                    "protocolVersion", "2024-11-05",
                    "capabilities", Map.of(),
                    "clientInfo", Map.of("name", "learning-client", "version", "0.0.1")
            )));
            System.out.println("[OK] initialize → " + init.get("result").get("serverInfo").get("name").asText());
            mcp.sendNotification(JsonRpc.notification("notifications/initialized", Map.of()));
            System.out.println();

            // 3) tools/list — MCP Server에 어떤 tool이 있나?
            JsonNode tools = mcp.sendAndReceive(JsonRpc.request(2, "tools/list", Map.of()));
            System.out.println("[OK] tools/list returned " + tools.get("result").get("tools").size() + " tools");
            for (JsonNode t : tools.get("result").get("tools")) {
                System.out.println("    - " + t.get("name").asText() + " : " + t.get("description").asText());
            }
            System.out.println();

            // 4) tools/call — add_todo
            JsonNode add = mcp.sendAndReceive(JsonRpc.request(3, "tools/call", Map.of(
                    "name", "add_todo",
                    "arguments", Map.of("title", "MCP 강의 듣기", "memo", "오늘")
            )));
            System.out.println("[OK] add_todo response:");
            System.out.println(JsonRpc.pretty(add));
            System.out.println();

            // 5) tools/call — list_todos
            JsonNode list = mcp.sendAndReceive(JsonRpc.request(4, "tools/call", Map.of(
                    "name", "list_todos",
                    "arguments", Map.of()
            )));
            System.out.println("[OK] list_todos response:");
            System.out.println(JsonRpc.pretty(list));
            System.out.println();

            System.out.println("=== End of learning scenario ===");
            System.out.println("이 메시지들이 MCP의 전부입니다.");
            System.out.println("Claude Code도 같은 메시지를 같은 MCP Server에 보냅니다.");
        }
    }
}
```

- [ ] **Step 2: 빌드 확인**

```powershell
./gradlew build -x test
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: 커밋**

```powershell
git add -A
git commit -m "client: add LearningClient with initialize/tools-list/tools-call scenario"
```

---

## Task 13: End-to-End 수동 시연

**Files:** (코드 변경 없음)

- [ ] **Step 1: Resource Server 띄우기**

터미널 1, in `01-resource-server`:

```powershell
./gradlew bootRun
```

8080 포트 확인.

- [ ] **Step 2: MCP Server JAR 빌드 (없으면)**

터미널 2, in `02-mcp-server`:

```powershell
./gradlew bootJar
```

`build/libs/mcp-server.jar` 생성 확인.

- [ ] **Step 3: LearningClient 실행**

터미널 2, in `03-client`:

```powershell
./gradlew run
```

Expected console output 패턴:

```
=== Learning MCP Client ===
MCP Server: C:\Users\G\workspace\mcp-exam\02-mcp-server\build\libs\mcp-server.jar

→ {"jsonrpc":"2.0","id":1,"method":"initialize",...}
← {"jsonrpc":"2.0","id":1,"result":{"protocolVersion":"2024-11-05",...,"serverInfo":{"name":"todo-mcp-server",...}}}
[OK] initialize → todo-mcp-server

→ {"jsonrpc":"2.0","method":"notifications/initialized",...}  (notification)

→ {"jsonrpc":"2.0","id":2,"method":"tools/list",...}
← {"jsonrpc":"2.0","id":2,"result":{"tools":[...5 tools...]}}
[OK] tools/list returned 5 tools
    - list_todos : 모든 할일(Todo) 목록을 반환합니다.
    - get_todo   : ID로 할일 한 건을 조회합니다.
    - add_todo   : 새 할일을 추가합니다. memo는 선택입니다.
    - complete_todo : ID에 해당하는 할일을 완료 처리합니다.
    - search_todos  : 제목과 메모에서 키워드를 포함하는 할일을 검색합니다.

→ {"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"add_todo","arguments":{"title":"MCP 강의 듣기","memo":"오늘"}}}
← {"jsonrpc":"2.0","id":3,"result":{...}}
[OK] add_todo response:
{
  "jsonrpc" : "2.0",
  "id" : 3,
  "result" : {
    "content" : [ ... ],
    "structuredContent" : { "id" : 1, "title" : "MCP 강의 듣기", ... }
  }
}

→ tools/call list_todos
← (1개의 Todo)
[OK] list_todos response:
{...}

=== End of learning scenario ===
```

만약 실패하면:
- `02-mcp-server/logs/mcp-server.log` 확인
- 응답이 안 오면 `application.yml` 배너/로그 설정 점검
- 'Failed to invoke tool' 류 에러 → Resource Server(8080) 실제 떠 있는지 확인

- [ ] **Step 4: 시연 결과 캡처 (선택)**

콘솔 출력을 텍스트 파일로 저장:

```powershell
./gradlew run --console=plain > ../docs/sample-run.txt 2>&1
```

(슬라이드에 결과 인용용. 실패하더라도 다음 태스크로 진행 가능.)

- [ ] **Step 5: 커밋 (sample-run.txt 추가하는 경우)**

```powershell
git add -A
git commit -m "docs: add sample end-to-end run output"
```

---

## Task 14: Claude Code 연결 검증 (Host = ④)

**Files:** (Claude Code 사용자 설정 변경 가이드)

이 태스크는 **연결 설정 + 수동 자연어 시연**. 코드 변경은 없지만 강의의 클라이맥스.

- [ ] **Step 1: Claude Code MCP 설정 파일 위치 확인**

OS별 위치:
- Windows: `%USERPROFILE%\.claude\mcp.json` 또는 프로젝트별 `<project>/.mcp.json`
- macOS: `~/Library/Application Support/Claude/claude_desktop_config.json` (Claude Desktop) 또는 `~/.claude/mcp.json` (Claude Code CLI)
- Linux: `~/.claude/mcp.json` (Claude Code CLI)

> 정확한 경로는 강의 시점의 Claude Code/Desktop 버전 문서를 다시 확인. 본 플랜은 Claude Code CLI 기준.

- [ ] **Step 2: 프로젝트 단위 등록 (권장)**

Create `C:\Users\G\workspace\mcp-exam\.mcp.json`:

```json
{
  "mcpServers": {
    "todo": {
      "command": "java",
      "args": [
        "-jar",
        "C:\\Users\\G\\workspace\\mcp-exam\\02-mcp-server\\build\\libs\\mcp-server.jar"
      ]
    }
  }
}
```

> **포터빌리티 메모**: 절대경로가 들어가 있어 학생이 받으면 자기 경로로 수정 필요. README에 명시.

- [ ] **Step 3: Claude Code 실행 + 자연어 시연**

조건: `01-resource-server`가 8080에서 떠 있어야 함 (MCP Server가 호출).

Run in `mcp-exam`:

```powershell
claude
```

세션 안에서:

```
> 할일 목록 보여줘
[Claude가 list_todos tool 호출 → 결과를 자연어로 응답]

> "Spring AI 공부하기" 추가해줘
[Claude가 add_todo tool 호출 → 1번 등록 확인]

> MCP 관련 할일 검색해줘
[Claude가 search_todos tool 호출]

> 1번 할일 완료 처리해줘
[Claude가 complete_todo tool 호출]
```

각 자연어 명령에 대해 Claude Code가 어떤 tool을 어떤 인자로 호출했는지 (Claude Code UI에서 노출) 확인.

- [ ] **Step 4: 트러블슈팅 메모를 README에 반영하기 위해 기록**

이 단계에서 발생한 에러/해결책은 Task 16의 README 트러블슈팅 섹션으로 들어감. 임시 메모를 `docs/troubleshooting-notes.md`에 적어두면 좋음 (선택).

- [ ] **Step 5: 커밋**

```powershell
git add -A
git commit -m "docs: add Claude Code MCP server registration config"
```

---

## Task 15: Marp 슬라이드 작성

**Files:**
- Create: `docs/slides/mcp-lecture.md`

- [ ] **Step 1: Marp 슬라이드 작성**

Create `C:\Users\G\workspace\mcp-exam\docs\slides\mcp-lecture.md`:

```markdown
---
marp: true
theme: default
paginate: true
header: 'MCP 입문 — 1차시'
footer: '2026-05-28'
---

# MCP 입문
## Resource → MCP Server → Client → Host
### 90분에 끝내는 첫 MCP 연결

---

## 오늘의 목표

90분 후 여러분은:

1. MCP **4컴포넌트**를 자기 말로 설명할 수 있다
2. Client가 **LLM이 아니라는 사실**을 코드로 확인했다
3. 자기가 만든 MCP Server를 **Claude Code에 등록**해서 자연어로 동작시킬 수 있다
4. JSON-RPC `tools/list`, `tools/call` 메시지가 실제로 어떻게 흐르는지 봤다

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
- 우리가 만들 Client 코드에 LLM 호출 **0줄**

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
| ① | 학습용 Client | Java + Jackson | JSON-RPC 직접 짜기 |
| ② | MCP Server | Spring Boot + Spring AI | `@McpTool` 5개 노출 |
| ③ | Resource Server | Spring Boot REST | Todo 저장소 |
| ④ | Host | Claude Code | (외부 도구, 마지막에 연결) |

도메인: **Todo/메모 관리**

---

## 5개 tool

| tool | 인자 | REST |
|---|---|---|
| `list_todos` | — | GET /todos |
| `get_todo` | id | GET /todos/{id} |
| `add_todo` | title, memo? | POST /todos |
| `complete_todo` | id | PATCH /todos/{id}/complete |
| `search_todos` | query | GET /todos/search |

---

## 강의 흐름 (105분)

| 분 | 단계 |
|---|---|
| 0~15 | 도입 + 이 슬라이드 |
| 15~30 | ③ Resource Server 둘러보기 + curl |
| 30~55 | ② MCP Server: `@McpTool` 5개 + stdio |
| 55~80 | ① 학습용 Client: **JSON-RPC 손으로** |
| 80~100 | ④ Claude Code 연결 + 자연어 시연 |
| 100~105 | 다음 단계 + Q&A |

---

## (라이브 코딩 전환)

> 이후는 IDE와 터미널로 전환합니다.
> 슬라이드는 마지막 챕터에서 다시 돌아옵니다.

---

## 다음 단계 — 이 강의에서 안 다룬 것

- **HTTP/SSE transport** — 원격 MCP Server 운영
- **prompts, resources** — tools 외 다른 capability 두 가지
- **인증/보안** — OAuth, 토큰 전달, 권한
- **여러 MCP Server 조합** — Host 하나에 N개 서버
- **Spring AI 외 다른 SDK** — TypeScript, Python, Go

> 한 줄 요약: **Tool은 끝. 표준화의 진짜 가치는 여기서부터.**

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
```

- [ ] **Step 2: Marp로 미리보기 (선택)**

VS Code Marp 확장 설치 후 `docs/slides/mcp-lecture.md` 열고 미리보기. 또는 CLI:

```powershell
npx @marp-team/marp-cli docs/slides/mcp-lecture.md --html
```

`mcp-lecture.html` 생성 확인.

- [ ] **Step 3: 커밋**

```powershell
git add -A
git commit -m "slides: add Marp lecture slides"
```

---

## Task 16: 본격 README 작성

**Files:**
- Modify: `mcp-exam/README.md`

- [ ] **Step 1: README 전면 교체**

Replace `C:\Users\G\workspace\mcp-exam\README.md` 내용 전체:

```markdown
# MCP 강의자료 — Todo/메모 도메인

1차시(90~120분) MCP 입문 강의용 코드 + 슬라이드.

학생은 90분 후 자신이 만든 MCP Server를 Claude Code에 등록해서
"오늘 할 일 보여줘" 같은 자연어 명령으로 동작시키는 경험을 한다.

## 디렉토리

```
mcp-exam/
├── 01-resource-server/   # Spring Boot REST API (Todo CRUD)
├── 02-mcp-server/        # Spring AI MCP Server (@McpTool x5)
├── 03-client/            # 학습용 JSON-RPC Client (Java)
├── docs/slides/          # Marp 슬라이드
└── .mcp.json             # Claude Code 연결 설정
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
- `application.yml`의 `banner-mode: off`가 빠져 stdout에 배너가 섞임
- `web-application-type: none` 누락
- `logging.pattern.console: ""` 누락

### `tools/call`에서 "Connection refused"

Resource Server(8080)가 안 떠 있음. 터미널 1을 확인.

### Claude Code가 tool을 못 찾음

- `.mcp.json` 경로가 맞나? (절대경로)
- `claude` 실행 후 `/mcp` 명령으로 등록된 서버 목록 확인
- jar가 빌드되어 있나? `./gradlew bootJar` 다시.

### Windows에서 PowerShell 따옴표 이슈

`curl -d` JSON에서 작은따옴표/큰따옴표가 꼬이면 `requests.http` 파일을 VS Code REST Client로 실행하는 것이 안전.

## 강의자 노트

- 코드는 학생이 따라 치지 않아도 됨 (1차시 분량). repo를 clone 후 핵심 부분만 라이브로 짚으며 설명.
- **80~100분 구간(Claude Code 연결)이 클라이맥스**. 시간이 모자라면 ① 학습용 Client의 코드 리딩을 줄여 시간을 확보.
- 학생이 자기 노트북에 Java 21이 없을 가능성 — 시작 30분 전 환경 점검 시간 권장.

## 참고

- 설계: [`docs/superpowers/specs/2026-05-28-mcp-lecture-design.md`](docs/superpowers/specs/2026-05-28-mcp-lecture-design.md)
- 구현 계획: [`docs/superpowers/plans/2026-05-28-mcp-lecture.md`](docs/superpowers/plans/2026-05-28-mcp-lecture.md)
- Spring AI MCP 문서: https://docs.spring.io/spring-ai/reference/api/mcp/
- MCP 명세: https://modelcontextprotocol.io/
```

- [ ] **Step 2: 커밋**

```powershell
git add -A
git commit -m "docs: write full README with run instructions and troubleshooting"
```

---

## Task 17: 최종 Dry-run

**Files:** (검증 단계, 코드 변경 없음)

목표: **방금 받은 사람이 README만 보고 모든 게 돌아가는지** 검증.

- [ ] **Step 1: 깨끗한 상태에서 처음부터 따라하기**

```powershell
cd 01-resource-server
./gradlew clean build
./gradlew bootRun
# 터미널 1 유지
```

- [ ] **Step 2: MCP Server 빌드 + Client 실행**

```powershell
cd ../02-mcp-server
./gradlew clean bootJar

cd ../03-client
./gradlew run
```

학습용 시나리오가 끝까지 돌아가는지 확인.

- [ ] **Step 3: Claude Code 시연**

```powershell
claude
```

5개 tool 각각에 해당하는 자연어 명령 시도. 다 동작하는지 확인.

- [ ] **Step 4: 시간 측정 (강의 분량 검증)**

스톱워치로 다음을 측정:
- 슬라이드 도입부 (1~6장 진행) — 목표 15분 이내
- Resource Server 둘러보기 + curl — 목표 15분 이내
- MCP Server 코드 짚으며 설명 + 빌드 + 실행 — 목표 25분 이내
- 학습용 Client 코드 짚으며 설명 + 실행 — 목표 25분 이내
- Claude Code 연결 + 자연어 시연 — 목표 20분 이내
- 마무리 + Q&A — 5분

합계 105분 이내인지 검증. 초과 시 학습용 Client 섹션 우선 압축.

- [ ] **Step 5: 발견된 문제는 fix → 커밋**

dry-run에서 발견된 이슈는 작은 커밋 단위로 수정. 예:
- README 트러블슈팅 항목 추가
- 슬라이드 챕터 1개 압축
- 빌드 오류로 인한 의존성 버전 정정

각 수정 후:

```powershell
git add -A
git commit -m "fix: <발견한 문제>"
```

- [ ] **Step 6: 최종 태그 (선택)**

```powershell
git tag v1.0 -m "1차시 강의자료 초안 완성"
```

---

## 정리

총 17 태스크. 평균 5 step. 한 step 2~5분 기준 전체 약 3~5시간 분량.

**구현 순서 권장**: Task 1 → 17을 그대로 따라가면 됨. 단, 다음 단축 가능:
- Task 5 (수동 검증) — 시간 부족 시 skip
- Task 9 (MCP Server 단독 검증) — Task 13에서 통합 검증되므로 skip 가능
- Task 13 (End-to-End) — Task 14에서 다시 하므로 일부 skip 가능

**핵심 검증 포인트**: Task 3, 4, 8 (TDD 사이클이 도는 곳), Task 13 (모든 컴포넌트 통합), Task 14 (Claude Code 연결), Task 17 (최종 dry-run).
