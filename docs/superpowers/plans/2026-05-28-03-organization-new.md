# 03-organization 시나리오 신규 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 사내 공식 통합 시나리오 — Resource Server는 Bearer 토큰 기반 사용자별 Todo 격리, MCP Server는 HTTP transport로 여러 사용자(alice/bob) Claude Code 세션이 동시에 접속. 시나리오 03-organization의 강의 자료(슬라이드 + README + .mcp.json 2개)를 작성한다.

**Architecture:** Resource Server는 01-personal/resource-server의 확장 — Todo에 userId 필드, `HandlerInterceptor`로 Bearer 검증 + userId 추출, Repository가 `Map<UserId, Map<TodoId, Todo>>`. MCP Server는 01-personal/mcp-server의 확장 — `web-application-type: servlet` (HTTP transport), incoming `Authorization` 헤더를 downstream Resource Server 호출 시 그대로 forward.

**Tech Stack:** Java 21, Gradle Kotlin DSL, Spring Boot 3.3.x, Spring AI 1.0.3 (`spring-ai-starter-mcp-server`), Spring `RestClient`, Spring MVC `HandlerInterceptor`, `RequestContextHolder`.

**참고:**
- 설계서: `docs/superpowers/specs/2026-05-28-mcp-lecture-3scenarios.md` §6
- 모델 코드: `01-personal/resource-server/`, `01-personal/mcp-server/`

---

## File Structure

```
03-organization/
├── resource-server/
│   ├── build.gradle.kts          # 01-personal과 동일 (의존성 같음)
│   ├── settings.gradle.kts
│   ├── gradle/wrapper/*          # 01-personal에서 복사
│   ├── gradlew, gradlew.bat
│   └── src/
│       ├── main/java/com/example/org/
│       │   ├── ResourceServerApplication.java
│       │   ├── Todo.java                  # {id, userId, title, memo, completed}
│       │   ├── TodoRepository.java        # Map<UserId, Map<TodoId, Todo>>
│       │   ├── TodoController.java        # /todos, AuthContext.userId 사용
│       │   ├── AuthContext.java           # ThreadLocal<String> userId
│       │   ├── AuthInterceptor.java       # HandlerInterceptor — Bearer 검증
│       │   └── WebConfig.java             # Interceptor 등록
│       ├── main/resources/application.yml
│       └── test/java/com/example/org/
│           ├── TodoRepositoryTest.java
│           └── TodoControllerTest.java    # alice/bob 격리 검증
│
├── mcp-server/
│   ├── build.gradle.kts          # web-application-type 활성, HTTP transport
│   ├── settings.gradle.kts
│   ├── gradle/wrapper/*, gradlew, gradlew.bat
│   └── src/
│       ├── main/java/com/example/orgmcp/
│       │   ├── McpServerApplication.java
│       │   ├── TodoView.java              # {id, userId, title, memo, completed}
│       │   ├── TodoRestClient.java        # incoming Auth 헤더 forward
│       │   ├── TodoTools.java             # @Tool 5개
│       │   └── RequestAuthHeaderHolder.java  # ThreadLocal로 incoming Authorization 보관
│       ├── main/resources/application.yml # HTTP/SSE transport
│       └── test/java/com/example/orgmcp/
│           └── TodoToolsTest.java
│
├── docs/slides/mcp-lecture-organization.md
├── .mcp.alice.json
├── .mcp.bob.json
└── README.md
```

### 토큰 매핑 (전 컴포넌트 공통, 하드코딩)
- `alice-token` → user `alice`
- `bob-token` → user `bob`
- 그 외 토큰 → 401 Unauthorized

---

## 공통 규칙

- Windows 11, PowerShell.
- `git add -A` 항상. hook skip X. 새 commit (amend X).
- 한 태스크 = 한 commit. prefix: `org-res:`, `org-mcp:`, `docs:` 등.

---

## Task 1: 03-organization/resource-server 스캐폴드

**Files:**
- Create: `03-organization/resource-server/` (디렉토리)
- Copy: gradle wrapper from `01-personal/resource-server/`
- Create: `build.gradle.kts`, `settings.gradle.kts`, `application.yml`, `ResourceServerApplication.java`

- [ ] **Step 1: 디렉토리 + wrapper 복사**

```powershell
New-Item -ItemType Directory -Force -Path "03-organization/resource-server" | Out-Null
Copy-Item -Recurse 01-personal/resource-server/gradle 03-organization/resource-server/gradle
Copy-Item 01-personal/resource-server/gradlew 03-organization/resource-server/
Copy-Item 01-personal/resource-server/gradlew.bat 03-organization/resource-server/
```

- [ ] **Step 2: settings.gradle.kts**

Create `03-organization/resource-server/settings.gradle.kts`:

```kotlin
rootProject.name = "resource-server-org"
```

- [ ] **Step 3: build.gradle.kts** (01-personal/resource-server와 동일)

Create `03-organization/resource-server/build.gradle.kts`:

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

- [ ] **Step 4: Application + application.yml**

Create `03-organization/resource-server/src/main/java/com/example/org/ResourceServerApplication.java`:

```java
package com.example.org;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ResourceServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(ResourceServerApplication.class, args);
    }
}
```

Create `03-organization/resource-server/src/main/resources/application.yml`:

```yaml
server:
  port: 8080      # 01-personal과 같은 포트 — 동시에 띄우려면 한 시나리오만 활성

spring:
  application:
    name: resource-server-org

logging:
  level:
    com.example.org: DEBUG
```

- [ ] **Step 5: 빌드 확인 + commit**

```powershell
Push-Location 03-organization\resource-server; .\gradlew build -x test --console=plain 2>&1 | Select-Object -Last 4; Pop-Location
```

Expected: `BUILD SUCCESSFUL`.

```powershell
git add -A
git commit -m "org-res: scaffold 03-organization/resource-server"
```

---

## Task 2: Todo record (with userId) + AuthContext

**Files:**
- Create: `Todo.java` (userId 필드 추가)
- Create: `AuthContext.java` (ThreadLocal)

- [ ] **Step 1: Todo record**

Create `03-organization/resource-server/src/main/java/com/example/org/Todo.java`:

```java
package com.example.org;

public record Todo(
        Long id,
        String userId,
        String title,
        String memo,
        boolean completed
) {
    public Todo complete() {
        return new Todo(id, userId, title, memo, true);
    }
}
```

- [ ] **Step 2: AuthContext (ThreadLocal로 현재 요청의 userId)**

Create `03-organization/resource-server/src/main/java/com/example/org/AuthContext.java`:

```java
package com.example.org;

/**
 * 현재 HTTP 요청의 인증된 userId를 ThreadLocal로 보관.
 * HandlerInterceptor가 set, Controller/Repository가 read, finally에서 clear.
 *
 * 단일 토큰 매핑 (in-memory, 강의용):
 *   alice-token -> alice
 *   bob-token   -> bob
 */
public final class AuthContext {

    private static final ThreadLocal<String> USER_ID = new ThreadLocal<>();

    private AuthContext() {}

    public static void set(String userId) {
        USER_ID.set(userId);
    }

    public static String getOrThrow() {
        String id = USER_ID.get();
        if (id == null) {
            throw new IllegalStateException("No authenticated userId in context");
        }
        return id;
    }

    public static void clear() {
        USER_ID.remove();
    }

    /** 강의용 단순 토큰 매핑. 운영에서는 JWT/OAuth 검증으로 대체. */
    public static String resolveUserId(String bearerToken) {
        if (bearerToken == null) return null;
        return switch (bearerToken) {
            case "alice-token" -> "alice";
            case "bob-token" -> "bob";
            default -> null;
        };
    }
}
```

- [ ] **Step 3: Commit**

```powershell
git add -A
git commit -m "org-res: add Todo record (with userId) and AuthContext (ThreadLocal)"
```

---

## Task 3: TodoRepository (사용자별 격리) — TDD

**Files:**
- Create: `TodoRepository.java`
- Test: `TodoRepositoryTest.java`

- [ ] **Step 1: 실패하는 테스트**

Create `03-organization/resource-server/src/test/java/com/example/org/TodoRepositoryTest.java`:

```java
package com.example.org;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class TodoRepositoryTest {

    private TodoRepository repo;

    @BeforeEach
    void setUp() {
        repo = new TodoRepository();
    }

    @Test
    void add_assignsPerUserId() {
        Todo a1 = repo.add("alice", "first", "m");
        Todo b1 = repo.add("bob", "first", "m");

        assertEquals(1L, a1.id());
        assertEquals(1L, b1.id());  // alice와 bob은 각자 1번부터
        assertEquals("alice", a1.userId());
        assertEquals("bob", b1.userId());
    }

    @Test
    void findAll_returnsOnlyOwnTodos() {
        repo.add("alice", "a1", null);
        repo.add("alice", "a2", null);
        repo.add("bob", "b1", null);

        assertEquals(2, repo.findAll("alice").size());
        assertEquals(1, repo.findAll("bob").size());
    }

    @Test
    void findById_isolatesByUser() {
        Todo a = repo.add("alice", "x", null);
        Todo b = repo.add("bob", "y", null);

        // alice가 1번 조회 → alice의 1번
        Optional<Todo> aliceView = repo.findById("alice", a.id());
        assertTrue(aliceView.isPresent());
        assertEquals("alice", aliceView.get().userId());

        // bob이 1번 조회 → bob의 1번 (alice 것 X)
        Optional<Todo> bobView = repo.findById("bob", b.id());
        assertTrue(bobView.isPresent());
        assertEquals("bob", bobView.get().userId());
    }

    @Test
    void findById_returnsEmptyForOtherUser() {
        Todo a = repo.add("alice", "x", null);
        Optional<Todo> bobView = repo.findById("bob", a.id());
        assertTrue(bobView.isEmpty(),
                "bob은 alice의 todo를 못 봐야 함");
    }

    @Test
    void complete_isolatesByUser() {
        Todo a = repo.add("alice", "x", null);
        Optional<Todo> bobComplete = repo.complete("bob", a.id());
        assertTrue(bobComplete.isEmpty(),
                "bob은 alice의 todo를 완료처리 못 함");

        Optional<Todo> aliceComplete = repo.complete("alice", a.id());
        assertTrue(aliceComplete.isPresent());
        assertTrue(aliceComplete.get().completed());
    }

    @Test
    void search_isolatesByUser() {
        repo.add("alice", "MCP study", null);
        repo.add("bob", "MCP design", null);

        List<Todo> aliceResults = repo.search("alice", "MCP");
        assertEquals(1, aliceResults.size());
        assertEquals("alice", aliceResults.get(0).userId());
    }
}
```

- [ ] **Step 2: 실행 → 실패 확인**

```powershell
Push-Location 03-organization\resource-server; .\gradlew test --console=plain 2>&1 | Select-Object -Last 6; Pop-Location
```

Expected: 컴파일 에러 — `TodoRepository` 없음.

- [ ] **Step 3: TodoRepository 구현**

Create `03-organization/resource-server/src/main/java/com/example/org/TodoRepository.java`:

```java
package com.example.org;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class TodoRepository {

    /** userId -> (todoId -> todo) */
    private final Map<String, Map<Long, Todo>> stores = new ConcurrentHashMap<>();

    /** userId별 ID 시퀀스 */
    private final Map<String, AtomicLong> sequences = new ConcurrentHashMap<>();

    public Todo add(String userId, String title, String memo) {
        long id = sequences.computeIfAbsent(userId, k -> new AtomicLong(0)).incrementAndGet();
        Todo todo = new Todo(id, userId, title, memo, false);
        stores.computeIfAbsent(userId, k -> new ConcurrentHashMap<>()).put(id, todo);
        return todo;
    }

    public Optional<Todo> findById(String userId, Long id) {
        return Optional.ofNullable(storeOf(userId).get(id));
    }

    public List<Todo> findAll(String userId) {
        return storeOf(userId).values().stream()
                .sorted((a, b) -> Long.compare(a.id(), b.id()))
                .toList();
    }

    public Optional<Todo> complete(String userId, Long id) {
        return findById(userId, id).map(t -> {
            Todo updated = t.complete();
            storeOf(userId).put(id, updated);
            return updated;
        });
    }

    public List<Todo> search(String userId, String query) {
        String q = query == null ? "" : query.toLowerCase();
        return findAll(userId).stream()
                .filter(t -> {
                    String title = t.title() == null ? "" : t.title().toLowerCase();
                    String memo = t.memo() == null ? "" : t.memo().toLowerCase();
                    return title.contains(q) || memo.contains(q);
                })
                .toList();
    }

    private Map<Long, Todo> storeOf(String userId) {
        return stores.getOrDefault(userId, Map.of());
    }
}
```

- [ ] **Step 4: 테스트 통과 + commit**

```powershell
Push-Location 03-organization\resource-server; .\gradlew test --console=plain 2>&1 | Select-Object -Last 6; Pop-Location
```

Expected: 6 tests pass.

```powershell
git add -A
git commit -m "org-res: add TodoRepository with per-user isolation (6 tests)"
```

---

## Task 4: AuthInterceptor + WebConfig

**Files:**
- Create: `AuthInterceptor.java`
- Create: `WebConfig.java`

- [ ] **Step 1: AuthInterceptor**

Create `03-organization/resource-server/src/main/java/com/example/org/AuthInterceptor.java`:

```java
package com.example.org;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.getWriter().write("Missing or invalid Authorization header");
            return false;
        }
        String token = header.substring(BEARER_PREFIX.length()).trim();
        String userId = AuthContext.resolveUserId(token);
        if (userId == null) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.getWriter().write("Unknown bearer token");
            return false;
        }
        AuthContext.set(userId);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex) {
        AuthContext.clear();
    }
}
```

- [ ] **Step 2: WebConfig (인터셉터 등록)**

Create `03-organization/resource-server/src/main/java/com/example/org/WebConfig.java`:

```java
package com.example.org;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    public WebConfig(AuthInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/todos/**", "/todos");
    }
}
```

- [ ] **Step 3: Commit**

```powershell
git add -A
git commit -m "org-res: add AuthInterceptor (Bearer validation) and WebConfig"
```

---

## Task 5: TodoController (사용자별 응답) — TDD

**Files:**
- Create: `TodoController.java`
- Test: `TodoControllerTest.java`

- [ ] **Step 1: 통합 테스트**

Create `03-organization/resource-server/src/test/java/com/example/org/TodoControllerTest.java`:

```java
package com.example.org;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
class TodoControllerTest {

    @Autowired MockMvc mvc;

    @Test
    void missingAuthHeader_returns401() throws Exception {
        mvc.perform(get("/todos"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidToken_returns401() throws Exception {
        mvc.perform(get("/todos").header("Authorization", "Bearer wrong-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void aliceCanListEmpty() throws Exception {
        mvc.perform(get("/todos").header("Authorization", "Bearer alice-token"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void aliceAndBobAreIsolated() throws Exception {
        // alice가 1건 추가
        mvc.perform(post("/todos")
                        .header("Authorization", "Bearer alice-token")
                        .contentType("application/json")
                        .content("""
                                {"title": "alice's task", "memo": null}
                                """))
                .andExpect(status().isCreated());

        // bob의 목록은 비어있어야
        mvc.perform(get("/todos").header("Authorization", "Bearer bob-token"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        // alice의 목록은 1건
        mvc.perform(get("/todos").header("Authorization", "Bearer alice-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].userId").value("alice"));
    }

    @Test
    void completeFlow() throws Exception {
        String body = mvc.perform(post("/todos")
                        .header("Authorization", "Bearer alice-token")
                        .contentType("application/json")
                        .content("""
                                {"title": "to complete", "memo": null}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long id = new ObjectMapper().readTree(body).get("id").asLong();

        mvc.perform(patch("/todos/{id}/complete", id)
                        .header("Authorization", "Bearer alice-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").value(true));
    }
}
```

- [ ] **Step 2: 테스트 실행 → 실패 확인**

```powershell
Push-Location 03-organization\resource-server; .\gradlew test --console=plain 2>&1 | Select-Object -Last 10; Pop-Location
```

Expected: 컴파일 에러 또는 `TodoController` 없어서 모든 endpoint 404 (인터셉터 로직은 동작).

- [ ] **Step 3: TodoController 구현**

Create `03-organization/resource-server/src/main/java/com/example/org/TodoController.java`:

```java
package com.example.org;

import org.springframework.http.HttpStatus;
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
        return repository.findAll(AuthContext.getOrThrow());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Todo> get(@PathVariable Long id) {
        return repository.findById(AuthContext.getOrThrow(), id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Todo add(@RequestBody AddRequest request) {
        return repository.add(AuthContext.getOrThrow(), request.title(), request.memo());
    }

    @PatchMapping("/{id}/complete")
    public ResponseEntity<Todo> complete(@PathVariable Long id) {
        return repository.complete(AuthContext.getOrThrow(), id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    public List<Todo> search(@RequestParam(value = "q", required = false) String query) {
        return repository.search(AuthContext.getOrThrow(), query);
    }

    public record AddRequest(String title, String memo) {}
}
```

- [ ] **Step 4: 테스트 통과 + commit**

```powershell
Push-Location 03-organization\resource-server; .\gradlew test --console=plain 2>&1 | Select-Object -Last 6; Pop-Location
```

Expected: 5 controller + 6 repository + 1 context-loads = 12 tests pass.

```powershell
git add -A
git commit -m "org-res: add TodoController with auth-context isolation (5 tests)"
```

---

## Task 6: 03-organization/mcp-server 스캐폴드 (HTTP transport)

**Files:**
- Create: `03-organization/mcp-server/` 디렉토리, wrapper, gradle 파일, application.yml, McpServerApplication

- [ ] **Step 1: 디렉토리 + wrapper**

```powershell
New-Item -ItemType Directory -Force -Path "03-organization/mcp-server" | Out-Null
Copy-Item -Recurse 01-personal/mcp-server/gradle 03-organization/mcp-server/gradle
Copy-Item 01-personal/mcp-server/gradlew 03-organization/mcp-server/
Copy-Item 01-personal/mcp-server/gradlew.bat 03-organization/mcp-server/
```

- [ ] **Step 2: settings.gradle.kts**

Create `03-organization/mcp-server/settings.gradle.kts`:

```kotlin
rootProject.name = "mcp-server-org"
```

- [ ] **Step 3: build.gradle.kts** (01-personal/mcp-server와 거의 동일, jar 이름만 다름)

Create `03-organization/mcp-server/build.gradle.kts`:

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
    implementation("io.modelcontextprotocol.sdk:mcp-spring-webmvc:0.10.0")  // HTTP/SSE transport (transitively `optional`, must declare)
    implementation("org.springframework.boot:spring-boot-starter-web")  // RestClient + Tomcat
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveFileName.set("mcp-server-org.jar")
}
```

- [ ] **Step 4: application.yml (HTTP transport)**

Create `03-organization/mcp-server/src/main/resources/application.yml`:

```yaml
server:
  port: 8081      # Resource Server(8080)와 분리

spring:
  main:
    web-application-type: servlet   # Tomcat 활성 (HTTP transport)
    banner-mode: console            # HTTP 모드에선 배너 OK (stdout JSON-RPC X)
  ai:
    mcp:
      server:
        name: todo-mcp-server-org
        version: 0.0.1
        type: SYNC
        # stdio 키 없음 — HTTP/SSE가 starter의 다른 활성 경로

logging:
  level:
    root: INFO
    com.example.orgmcp: DEBUG

resource-server:
  base-url: http://localhost:8080
```

> **HTTP/SSE transport — Phase B 검증 결과** (Spring AI 1.0.3):
> - 의존성 `io.modelcontextprotocol.sdk:mcp-spring-webmvc:0.10.0` **반드시 명시** (transitive `optional`이라 자동 안 들어옴 → 없으면 endpoint 404)
> - 엔드포인트: `GET /sse` (long-lived, sessionId 발급) → 후속 `POST /mcp/message?sessionId=<uuid>` (실제 JSON-RPC)
> - 기본 path 변경 키: `spring.ai.mcp.server.sse-endpoint`, `sse-message-endpoint`
> - HandlerInterceptor가 MVC chain에서 fire — `AuthForwardInterceptor` 동작 OK (e2e에서 최종 검증)

- [ ] **Step 5: McpServerApplication**

Create `03-organization/mcp-server/src/main/java/com/example/orgmcp/McpServerApplication.java`:

```java
package com.example.orgmcp;

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

- [ ] **Step 6: 빌드 확인 (의존성 해소)**

```powershell
Push-Location 03-organization\mcp-server; .\gradlew tasks --console=plain 2>&1 | Select-Object -Last 5; Pop-Location
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 7: Commit**

```powershell
git add -A
git commit -m "org-mcp: scaffold 03-organization/mcp-server with HTTP transport config"
```

---

## Task 7: MCP Server — Auth header forward + TodoRestClient + TodoView

**Files:**
- Create: `RequestAuthHeaderHolder.java` — incoming HTTP 요청의 Authorization 헤더를 ThreadLocal로 보관
- Create: `AuthForwardInterceptor.java` — incoming Authorization 헤더를 추출해서 holder에 저장
- Create: `WebConfig.java` (mcp-server측) — interceptor 등록
- Create: `TodoView.java`
- Create: `TodoRestClient.java` — RestClient의 모든 요청에 holder의 Authorization 헤더 추가

- [ ] **Step 1: RequestAuthHeaderHolder**

Create `03-organization/mcp-server/src/main/java/com/example/orgmcp/RequestAuthHeaderHolder.java`:

```java
package com.example.orgmcp;

/**
 * incoming MCP HTTP 요청의 Authorization 헤더를 downstream Resource Server 호출까지 전달하기 위한 holder.
 * AuthForwardInterceptor가 set, TodoRestClient가 read, finally에서 clear.
 */
public final class RequestAuthHeaderHolder {
    private static final ThreadLocal<String> AUTH = new ThreadLocal<>();

    private RequestAuthHeaderHolder() {}

    public static void set(String header) { AUTH.set(header); }
    public static String get() { return AUTH.get(); }
    public static void clear() { AUTH.remove(); }
}
```

- [ ] **Step 2: AuthForwardInterceptor + WebConfig**

Create `03-organization/mcp-server/src/main/java/com/example/orgmcp/AuthForwardInterceptor.java`:

```java
package com.example.orgmcp;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthForwardInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {
        String header = request.getHeader("Authorization");
        if (header != null) {
            RequestAuthHeaderHolder.set(header);
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex) {
        RequestAuthHeaderHolder.clear();
    }
}
```

Create `03-organization/mcp-server/src/main/java/com/example/orgmcp/WebConfig.java`:

```java
package com.example.orgmcp;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AuthForwardInterceptor authForwardInterceptor;

    public WebConfig(AuthForwardInterceptor authForwardInterceptor) {
        this.authForwardInterceptor = authForwardInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authForwardInterceptor);  // 모든 path
    }
}
```

- [ ] **Step 3: TodoView record**

Create `03-organization/mcp-server/src/main/java/com/example/orgmcp/TodoView.java`:

```java
package com.example.orgmcp;

public record TodoView(
        Long id,
        String userId,
        String title,
        String memo,
        boolean completed
) {}
```

- [ ] **Step 4: TodoRestClient (Auth 헤더 forward)**

Create `03-organization/mcp-server/src/main/java/com/example/orgmcp/TodoRestClient.java`:

```java
package com.example.orgmcp;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Optional;

@Component
public class TodoRestClient {

    private final RestClient client;

    public TodoRestClient(@Value("${resource-server.base-url}") String baseUrl) {
        this.client = RestClient.builder()
                .baseUrl(baseUrl)
                .requestInterceptor((request, body, execution) -> {
                    String auth = RequestAuthHeaderHolder.get();
                    if (auth != null) {
                        request.getHeaders().set("Authorization", auth);
                    }
                    return execution.execute(request, body);
                })
                .build();
    }

    public List<TodoView> list() {
        return client.get().uri("/todos").retrieve()
                .body(new ParameterizedTypeReference<List<TodoView>>() {});
    }

    public Optional<TodoView> get(long id) {
        TodoView body = client.get().uri("/todos/{id}", id)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, resp) -> { })
                .body(TodoView.class);
        return Optional.ofNullable(body);
    }

    public TodoView add(String title, String memo) {
        return client.post().uri("/todos")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new AddBody(title, memo))
                .retrieve().body(TodoView.class);
    }

    public Optional<TodoView> complete(long id) {
        TodoView body = client.patch().uri("/todos/{id}/complete", id)
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

- [ ] **Step 5: Commit**

```powershell
git add -A
git commit -m "org-mcp: add auth-forward interceptor and TodoRestClient with Bearer pass-through"
```

---

## Task 8: TodoTools (@Tool 5개) — TDD

**Files:**
- Create: `TodoTools.java`
- Test: `TodoToolsTest.java`

- [ ] **Step 1: TodoToolsTest**

Create `03-organization/mcp-server/src/test/java/com/example/orgmcp/TodoToolsTest.java`:

```java
package com.example.orgmcp;

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
    void listTodos_delegates() {
        TodoView t = new TodoView(1L, "alice", "a", null, false);
        when(restClient.list()).thenReturn(List.of(t));

        assertEquals(1, tools.listTodos().size());
        verify(restClient).list();
    }

    @Test
    void getTodo_returnsFoundFalseWhenAbsent() {
        when(restClient.get(99L)).thenReturn(Optional.empty());
        assertFalse(tools.getTodo(99L).found());
    }

    @Test
    void getTodo_returnsFoundWhenPresent() {
        TodoView t = new TodoView(1L, "alice", "x", null, false);
        when(restClient.get(1L)).thenReturn(Optional.of(t));
        TodoTools.GetResult r = tools.getTodo(1L);
        assertTrue(r.found());
        assertEquals(1L, r.todo().id());
    }

    @Test
    void addTodo_delegates() {
        TodoView created = new TodoView(5L, "alice", "new", "m", false);
        when(restClient.add("new", "m")).thenReturn(created);
        assertEquals(5L, tools.addTodo("new", "m").id());
    }

    @Test
    void completeTodo_returnsFlagWhenAbsent() {
        when(restClient.complete(404L)).thenReturn(Optional.empty());
        assertFalse(tools.completeTodo(404L).found());
    }

    @Test
    void completeTodo_returnsCompleted() {
        TodoView c = new TodoView(1L, "alice", "x", null, true);
        when(restClient.complete(1L)).thenReturn(Optional.of(c));
        TodoTools.CompleteResult r = tools.completeTodo(1L);
        assertTrue(r.found());
        assertTrue(r.todo().completed());
    }

    @Test
    void searchTodos_delegates() {
        when(restClient.search("mcp")).thenReturn(List.of());
        assertNotNull(tools.searchTodos("mcp"));
        verify(restClient).search("mcp");
    }
}
```

- [ ] **Step 2: 실행 → 실패 확인**

```powershell
Push-Location 03-organization\mcp-server; .\gradlew test --console=plain 2>&1 | Select-Object -Last 6; Pop-Location
```

Expected: 컴파일 에러 — `TodoTools` 없음.

- [ ] **Step 3: TodoTools 구현**

Create `03-organization/mcp-server/src/main/java/com/example/orgmcp/TodoTools.java`:

```java
package com.example.orgmcp;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TodoTools {

    private final TodoRestClient restClient;

    public TodoTools(TodoRestClient restClient) {
        this.restClient = restClient;
    }

    @Tool(name = "list_todos", description = "현재 사용자의 할일 목록을 반환합니다.")
    public List<TodoView> listTodos() {
        return restClient.list();
    }

    @Tool(name = "get_todo", description = "ID로 할일 한 건을 조회합니다.")
    public GetResult getTodo(
            @ToolParam(description = "할일 ID", required = true) Long id) {
        return restClient.get(id)
                .map(t -> new GetResult(true, t))
                .orElseGet(() -> new GetResult(false, null));
    }

    @Tool(name = "add_todo", description = "현재 사용자에게 새 할일을 추가합니다.")
    public TodoView addTodo(
            @ToolParam(description = "제목", required = true) String title,
            @ToolParam(description = "메모(선택)", required = false) String memo) {
        return restClient.add(title, memo);
    }

    @Tool(name = "complete_todo", description = "현재 사용자의 할일을 완료 처리합니다.")
    public CompleteResult completeTodo(
            @ToolParam(description = "할일 ID", required = true) Long id) {
        return restClient.complete(id)
                .map(t -> new CompleteResult(true, t))
                .orElseGet(() -> new CompleteResult(false, null));
    }

    @Tool(name = "search_todos", description = "현재 사용자의 할일 중 키워드 검색.")
    public List<TodoView> searchTodos(
            @ToolParam(description = "검색 키워드", required = true) String query) {
        return restClient.search(query);
    }

    public record GetResult(boolean found, TodoView todo) {}
    public record CompleteResult(boolean found, TodoView todo) {}
}
```

- [ ] **Step 4: 테스트 통과 + commit**

```powershell
Push-Location 03-organization\mcp-server; .\gradlew test --console=plain 2>&1 | Select-Object -Last 6; Pop-Location
```

Expected: 7 tests pass.

```powershell
git add -A
git commit -m "org-mcp: add TodoTools with 5 @Tool methods (7 tests)"
```

---

## Task 9: bootJar + HTTP transport endpoint 확인

**Files:** (검증만)

- [ ] **Step 1: bootJar**

```powershell
Push-Location 03-organization\mcp-server; .\gradlew bootJar --console=plain 2>&1 | Select-Object -Last 5; Pop-Location
Test-Path 03-organization\mcp-server\build\libs\mcp-server-org.jar
```

Expected: jar 존재.

- [ ] **Step 2: MCP Server 실행 + endpoint 확인**

```powershell
# 백그라운드로 실행
$proc = Start-Process -PassThru -FilePath "java" -ArgumentList "-jar","03-organization\mcp-server\build\libs\mcp-server-org.jar" -WindowStyle Hidden
Start-Sleep -Seconds 10

# 어떤 endpoint가 노출되는지 확인 (Spring Boot actuator나 기본 endpoints)
try { curl http://localhost:8081/sse 2>&1 } catch { Write-Output "Trying /sse — error or 404 is OK" }
try { curl http://localhost:8081/mcp 2>&1 } catch { Write-Output "Trying /mcp" }

Stop-Process -Id $proc.Id -Force
```

Expected: 둘 중 하나의 경로가 응답해야 함. SSE 모드라면 `/sse`가 일반적. 응답 형식이 SSE stream인지, JSON-RPC 직접인지 implementer가 관찰 후 record. 만약 둘 다 404면 Spring AI HTTP transport 키 추가 필요 — application.yml에 명시적 활성 키 (예: `spring.ai.mcp.server.transport: SSE` 또는 비슷한 키) 추가 시도. **implementer가 Spring AI docs 확인 후 plan 갱신 + .mcp.json도 정확한 URL로 갱신.**

이 단계는 검증이라 commit 없음. 다음 task의 .mcp.json 작성에 정보 제공.

---

## Task 10: .mcp.alice.json + .mcp.bob.json

**Files:**
- Create: `03-organization/.mcp.alice.json`
- Create: `03-organization/.mcp.bob.json`

- [ ] **Step 1: alice용 설정**

(Task 9에서 확인한 endpoint URL 사용. 기본 가정: `http://localhost:8081/sse`)

Create `03-organization/.mcp.alice.json`:

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

- [ ] **Step 2: bob용 설정**

Create `03-organization/.mcp.bob.json`:

```json
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

- [ ] **Step 3: Commit**

```powershell
git add -A
git commit -m "org-mcp: add .mcp.alice.json and .mcp.bob.json for multi-tenant demo"
```

---

## Task 11: 슬라이드

**Files:**
- Create: `03-organization/docs/slides/mcp-lecture-organization.md`

- [ ] **Step 1: 디렉토리 + 슬라이드**

```powershell
New-Item -ItemType Directory -Force -Path "03-organization\docs\slides" | Out-Null
```

Create `03-organization/docs/slides/mcp-lecture-organization.md`:

```markdown
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

Spring AI MCP starter는 stdio/HTTP 둘 다 지원 — application.yml만 다름.

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

## 운영 고려사항 (질문 받는 자리)

- **JWT/OAuth2**: 강의 토큰 매핑을 어떻게 실제 인증 시스템으로?
  → `AuthContext.resolveUserId(token)` 자리에 JWT decoder/Spring Security
- **토큰 저장**: `.mcp.json`에 평문은 학습용. 실전은 OS keychain/secret manager.
- **HTTPS**: 로컬은 HTTP, 운영은 HTTPS 필수 (Bearer 평문 전송)
- **Rate limiting**: 토큰별 호출량 제한 (Spring Cloud Gateway 등)

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
```

- [ ] **Step 2: Commit**

```powershell
git add -A
git commit -m "org-docs: add Marp slides for 03-organization scenario"
```

---

## Task 12: README

**Files:**
- Create: `03-organization/README.md`

- [ ] **Step 1: README 작성**

Create `03-organization/README.md`:

```markdown
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

### 2) MCP Server jar (한 번)

```powershell
cd ..\mcp-server
./gradlew bootJar
```

### 3) MCP Server 실행 (터미널 2)

```powershell
java -jar build\libs\mcp-server-org.jar
# → http://localhost:8081 (SSE endpoint)
```

### 4) Claude Code 두 세션

터미널 3 (alice):

```powershell
cd ..
cp .mcp.alice.json .mcp.json
claude
> 내 할일 보여줘
> "alice의 비밀 task" 추가
> 1번 할일 완료
```

터미널 4 (bob):

```powershell
cd ..
cp .mcp.bob.json .mcp.json
claude
> 내 할일 보여줘   # alice 것 안 보임!
> "bob의 task" 추가
```

> **`.mcp.json` 충돌**: Claude Code는 cwd의 `.mcp.json`을 인식. alice/bob 같은 폴더에서 둘 다 띄우면 충돌. 해결: 두 사용자를 다른 폴더로 분리하거나, 시간 분리 (alice 먼저 → 끝내고 → bob).

## 강의 흐름 (105분)

| 분 | 단계 |
|---|---|
| 0~10 | 01/02 복습 + 오늘 목표 |
| 10~35 | Resource Server: userId, Interceptor, 격리 |
| 35~70 | MCP Server HTTP transport + Auth forward |
| 70~95 | 두 Claude Code 세션 시연 |
| 95~105 | 운영 고려사항(JWT/OAuth, HTTPS, rate limit) + Q&A |

## 트러블슈팅

### MCP Server endpoint를 못 찾음 (`.mcp.json`의 url이 404)

Spring AI 1.0.3의 HTTP/SSE endpoint 경로 확인:

```powershell
java -jar mcp-server\build\libs\mcp-server-org.jar
# 로그에 "Started ... on port 8081"
# Tomcat 매핑 메시지에서 endpoint 경로 확인
```

기본 가정은 `/sse`. 만약 다르면 `.mcp.alice.json`/`.mcp.bob.json`의 url을 수정.

### 401 Unauthorized

- `.mcp.*.json`의 토큰이 정확한가? (`alice-token` 또는 `bob-token` 정확히)
- Resource Server가 Bearer 검증 중인지 확인 (인터셉터 로그)

### alice/bob이 같은 데이터 보임

- Resource Server의 `TodoRepository`가 사용자별 격리 되었나? 테스트 6개 통과 확인.
- MCP Server의 `AuthForwardInterceptor`가 동작하나? 요청 로그 확인.

### Two Claude Code sessions 동시 운영 어려움

대안: 시간 분리. alice로 한 라운드 → 끝 → bob으로 한 라운드. 같은 데이터가 격리되는 것을 보여주는 게 핵심.

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
```

- [ ] **Step 2: Commit**

```powershell
git add -A
git commit -m "org-docs: add 03-organization README with run guide and troubleshooting"
```

---

## Task 13: 최종 검증

- [ ] **Step 1: 모든 빌드 + 테스트**

```powershell
Push-Location 03-organization\resource-server; .\gradlew test --console=plain 2>&1 | Select-Object -Last 4; Pop-Location
Push-Location 03-organization\mcp-server; .\gradlew test --console=plain 2>&1 | Select-Object -Last 4; Pop-Location
```

Expected: 각자 BUILD SUCCESSFUL.

- [ ] **Step 2: 디렉토리 구조 확인**

```powershell
Get-ChildItem 03-organization -Recurse -Force | Where-Object { -not $_.PSIsContainer -and $_.FullName -notmatch '\\(\.gradle|build)\\' } | Select-Object FullName
```

Expected: 모든 plan에 명시된 파일들 존재.

- [ ] **Step 3: git log**

```powershell
git log --oneline -15
```

Expected: 최근 ~12개 commit이 03-organization 관련.

- [ ] **Step 4: (사용자 위임) Multi-tenant 자연어 시연**

Resource Server + MCP Server 띄우고 alice → bob 순서로 Claude Code. 격리 확인.

---

## Self-Review 체크리스트

- ✅ Spec coverage: 설계서 §6(시나리오 03-organization 전체) 매핑됨
- ✅ Placeholder 없음 (TBD/TODO 검색 0)
- ✅ Resource ↔ MCP ↔ Host의 Bearer 헤더 흐름 명시
- ✅ alice/bob 격리 시연 시나리오 명시
- ⚠️ **HTTP transport endpoint 경로(`/sse`)는 Spring AI 1.0.3 실제 동작 확인 필요** — Task 9에서 implementer가 검증 후 .mcp.json url 수정. 또는 추가 application.yml 키가 필요할 수도. 이건 deferred verification으로 처리.

---

## 3 시나리오 완성 후 마무리 (별도 plan 없음, 사용자 위임)

- 루트 README는 이미 3 시나리오 라우팅으로 갱신됨 (01-personal-migration plan Task 7)
- 각 시나리오 폴더는 독립적으로 강의 가능
- 사용자가 직접 e2e 시연 + 시간 측정 → 발견된 문제는 별도 fix commit
