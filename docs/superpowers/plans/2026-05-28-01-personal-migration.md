# 01-personal 시나리오 Migration Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 현재 16 commits에 있는 단일 시나리오 코드를 3 시나리오 구조의 `01-personal/` 폴더로 이동하고, 학습용 Client(`03-client/`)를 제거한다. 슬라이드와 README를 시나리오 분리에 맞게 갱신한다.

**Architecture:** 코드는 그대로 (이동만). 변경되는 것은 디렉토리 경로, `.mcp.json`의 jar 경로, 슬라이드의 "학습용 Client" 챕터 삭제, README 재작성. 빌드/테스트 깨지지 않게 단계마다 검증.

**Tech Stack:** Git mv (history 보존), PowerShell/Bash 명령, Markdown.

**참고 설계서:** `docs/superpowers/specs/2026-05-28-mcp-lecture-3scenarios.md`

---

## File Structure

**이동 (`git mv`로 history 보존)**:
- `01-resource-server/` → `01-personal/resource-server/`
- `02-mcp-server/` → `01-personal/mcp-server/`
- `.mcp.json` → `01-personal/.mcp.json` (jar 경로 갱신)
- `docs/slides/mcp-lecture.md` → `01-personal/docs/slides/mcp-lecture-personal.md`

**삭제**:
- `03-client/` (전체)

**신규 작성**:
- `README.md` (루트) — 세 시나리오 라우팅 (기존 placeholder 교체)
- `01-personal/README.md` — B-1 시나리오 강의 가이드

**유지**:
- `.gitignore` (루트)
- `docs/superpowers/specs/*` (이전 설계서 + 새 설계서)
- `docs/superpowers/plans/*` (이전 plan + 이 plan)

---

## 공통 규칙

- Windows 11, PowerShell 환경. Bash도 사용 가능.
- 사용자 글로벌 CLAUDE.md 룰: **`git add -A` 항상** 사용. hook skip X. amend X.
- 한 태스크 = 한 commit. 커밋 메시지 prefix: `migrate:`, `docs:`, `chore:` 적절히.
- 각 이동/수정 후 빌드 동작 확인 (`./gradlew test` 또는 `bootJar`).

---

## Task 1: 01-personal 디렉토리 + Resource Server 이동

**Files:**
- Create directory: `01-personal/`
- Move: `01-resource-server/` → `01-personal/resource-server/`

- [ ] **Step 1: 디렉토리 생성**

Run in `C:\Users\G\workspace\mcp-exam`:

```powershell
New-Item -ItemType Directory -Force -Path "01-personal" | Out-Null
```

- [ ] **Step 2: Resource Server를 git mv로 이동**

```powershell
git mv 01-resource-server 01-personal/resource-server
```

`git status`로 rename 확인 — Git이 자동으로 rename으로 추적해야 함 (file content 변경 0).

- [ ] **Step 3: 빌드 검증**

```powershell
Push-Location 01-personal\resource-server; .\gradlew test --console=plain 2>&1 | Select-Object -Last 15; Pop-Location
```

Expected: `BUILD SUCCESSFUL`, 13 tests pass (이전과 동일).

만약 build 실패하면: 경로 의존 코드 있는지 확인. `01-personal/resource-server/build.gradle.kts`, `settings.gradle.kts` 안에 절대경로 참조 없는지.

- [ ] **Step 4: Commit**

```powershell
git add -A
git commit -m "migrate: move resource-server into 01-personal/"
```

---

## Task 2: MCP Server 이동

**Files:**
- Move: `02-mcp-server/` → `01-personal/mcp-server/`

- [ ] **Step 1: git mv**

```powershell
git mv 02-mcp-server 01-personal/mcp-server
```

- [ ] **Step 2: 빌드 검증 (test + bootJar)**

```powershell
Push-Location 01-personal\mcp-server; .\gradlew test --console=plain 2>&1 | Select-Object -Last 15; Pop-Location
```

Expected: 7 tests pass (`TodoToolsTest`).

```powershell
Push-Location 01-personal\mcp-server; .\gradlew bootJar --console=plain 2>&1 | Select-Object -Last 10; Pop-Location
```

Expected: `BUILD SUCCESSFUL`. 산출물 `01-personal/mcp-server/build/libs/mcp-server.jar` 존재 확인.

```powershell
Test-Path 01-personal\mcp-server\build\libs\mcp-server.jar
```

Expected: `True`.

- [ ] **Step 3: Commit**

```powershell
git add -A
git commit -m "migrate: move mcp-server into 01-personal/"
```

---

## Task 3: 03-client/ 삭제

**Files:**
- Delete: `03-client/` (전체)

설계서에 따라 학습용 Client는 강의에서 제거. Claude Code 내장 Client만 사용.

- [ ] **Step 1: git rm**

```powershell
git rm -r 03-client
```

`git status`로 deletion 확인.

- [ ] **Step 2: Commit**

```powershell
git add -A
git commit -m "chore: remove learning JSON-RPC client (replaced by Claude Code built-in)"
```

---

## Task 4: .mcp.json 이동 + jar 경로 갱신

**Files:**
- Move: `.mcp.json` → `01-personal/.mcp.json`
- Modify: jar 절대경로 `02-mcp-server/...` → `01-personal/mcp-server/...`

- [ ] **Step 1: 현재 .mcp.json 내용 확인**

```powershell
Get-Content .mcp.json
```

Expected: 다음과 같은 내용 (절대경로 포함)

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

- [ ] **Step 2: git mv**

```powershell
git mv .mcp.json 01-personal/.mcp.json
```

- [ ] **Step 3: jar 경로 갱신**

`01-personal/.mcp.json` 내용을 다음으로 교체:

```json
{
  "mcpServers": {
    "todo": {
      "command": "java",
      "args": [
        "-jar",
        "C:\\Users\\G\\workspace\\mcp-exam\\01-personal\\mcp-server\\build\\libs\\mcp-server.jar"
      ]
    }
  }
}
```

(`02-mcp-server` → `01-personal\\mcp-server` 한 곳만 바뀜.)

- [ ] **Step 4: Commit**

```powershell
git add -A
git commit -m "migrate: move .mcp.json into 01-personal/ with updated jar path"
```

---

## Task 5: 슬라이드 이동 + "학습용 Client" 챕터 제거

**Files:**
- Move: `docs/slides/mcp-lecture.md` → `01-personal/docs/slides/mcp-lecture-personal.md`
- Modify: 슬라이드에서 학습용 Client 관련 내용 제거 및 강의 흐름 시간 재할당

- [ ] **Step 1: 디렉토리 생성 + git mv**

```powershell
New-Item -ItemType Directory -Force -Path "01-personal\docs\slides" | Out-Null
git mv docs\slides\mcp-lecture.md 01-personal\docs\slides\mcp-lecture-personal.md
```

빈 `docs/slides/` 폴더가 남으면 그대로 두기 (다른 시나리오가 자기 slides 폴더를 따로 가질 거고, 루트 `docs/slides/`는 의미 없어짐). git은 빈 폴더 추적 안 하니 자연스럽게 사라짐.

- [ ] **Step 2: 슬라이드의 헤더 라인 5 변경 (footer 그대로, header만 시나리오 명시)**

`01-personal/docs/slides/mcp-lecture-personal.md` 5번째 줄 (`header: 'MCP 입문 — 1차시'`)을 다음으로 교체:

```markdown
header: 'MCP 입문 (01-personal) — 내 시스템 + 1인 사용'
```

- [ ] **Step 3: 학습 목표 슬라이드 (Line 19-22) 수정**

원본:

```markdown
1. MCP **4컴포넌트**를 자기 말로 설명할 수 있다
2. Client가 **LLM이 아니라는 사실**을 코드로 확인했다
3. 자기가 만든 MCP Server를 **Claude Code에 등록**해서 자연어로 동작시킬 수 있다
4. JSON-RPC `tools/list`, `tools/call` 메시지가 실제로 어떻게 흐르는지 봤다
```

변경 후:

```markdown
1. MCP **4컴포넌트**를 자기 말로 설명할 수 있다
2. Client가 **LLM이 아니라는 사실**을 이해한다 (Claude Code 내장)
3. 자기가 만든 MCP Server를 **Claude Code에 등록**해서 자연어로 동작시킬 수 있다
4. stdio transport의 **부모-자식 프로세스 모델**을 이해한다
```

- [ ] **Step 4: "Client는 LLM이 아닙니다" 슬라이드 (Line 59-68) 살짝 조정**

`"우리가 만들 Client 코드에 LLM 호출 0줄"` 라인을 다음으로 교체:

```markdown
- Claude Code의 **내장 MCP Client** 코드에 LLM 호출 **0줄**
```

(우리가 직접 Client를 만들지 않으므로 표현 조정.)

- [ ] **Step 5: "오늘 만들 것" 슬라이드 (Line 116-127) 재작성**

기존 4행 표(① 학습용 Client / ② MCP Server / ③ Resource Server / ④ Host)를 다음 3행 표로 교체:

```markdown
## 오늘 만들 것

| # | 컴포넌트 | 기술 | 역할 |
|---|---|---|---|
| ① | Resource Server | Spring Boot REST | Todo 저장소 (내 시스템) |
| ② | MCP Server | Spring Boot + Spring AI | `@Tool` 5개 노출 (stdio) |
| ③ | Host | Claude Code | (외부 도구, ②를 자식 프로세스로 spawn) |

도메인: **Todo/메모 관리** (단일 사용자, 내 노트북)

> Spring AI는 일반 tool 어노테이션을 그대로 MCP tool로 노출합니다.
```

- [ ] **Step 6: "강의 흐름" 슬라이드 (Line 165-174) 시간 재할당**

기존 표:

```markdown
| 분 | 단계 |
|---|---|
| 0~15 | 도입 + 이 슬라이드 |
| 15~30 | ③ Resource Server 둘러보기 + curl |
| 30~55 | ② MCP Server: `@Tool` 5개 + stdio |
| 55~80 | ① 학습용 Client: **JSON-RPC 손으로** |
| 80~100 | ④ Claude Code 연결 + 자연어 시연 |
| 100~105 | 다음 단계 + Q&A |
```

다음으로 교체:

```markdown
| 분 | 단계 |
|---|---|
| 0~15 | 도입 + 이 슬라이드 |
| 15~35 | ① Resource Server 둘러보기 + curl |
| 35~70 | ② MCP Server: `@Tool` 5개 + stdio 깊게 |
| 70~95 | ③ Claude Code 연결 + 자연어 시연 (클라이맥스) |
| 95~105 | 다음 시나리오 예고 + Q&A |
```

- [ ] **Step 7: 마지막 "다음 단계" 슬라이드 갱신**

기존 슬라이드 본문에서 "다음 단계 — 이 강의에서 안 다룬 것" 부분을 찾아 다음으로 교체 (Grep으로 위치 찾기):

```markdown
## 다음 시나리오 — `02-external`, `03-organization`

**02-external**: 외부 공개 API (JSONPlaceholder)를 LLM에 연결
- "MCP Server는 어댑터"라는 사실을 외부 API로 확인
- 오늘 만든 MCP Server와 **거의 같은 코드** — base-url만 다름

**03-organization**: 사내 공식 통합 (HTTP transport + 인증)
- 여러 사용자에게 LLM 통합 제공
- Bearer 토큰 → 사용자별 Todo 격리
- MCP Server도 멀티유저 서비스가 될 수 있음

오늘 배운 패턴이 그대로 확장됩니다.
```

- [ ] **Step 8: Commit**

```powershell
git add -A
git commit -m "docs: relocate slides into 01-personal and remove learning client chapter"
```

---

## Task 6: 01-personal/README.md 작성

**Files:**
- Create: `01-personal/README.md`

- [ ] **Step 1: 새 README 작성**

Create `01-personal/README.md`:

```markdown
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
```

- [ ] **Step 2: Commit**

```powershell
git add -A
git commit -m "docs: add 01-personal README with run guide and troubleshooting"
```

---

## Task 7: 루트 README.md 재작성

**Files:**
- Modify: `README.md` (루트, 현재 placeholder)

- [ ] **Step 1: 루트 README 전면 교체**

Replace `C:\Users\G\workspace\mcp-exam\README.md` 전체 내용:

```markdown
# MCP 입문 강의자료 — 3 시나리오

주니어 백엔드 개발자 대상 MCP(Model Context Protocol) 강의자료.
세 가지 운영 시나리오를 독립 폴더로 구성. 각각 90~120분 1차시 분량.

## 시나리오 한눈에

| 시나리오 | Resource | Transport | 사용자 | 핵심 깨달음 |
|---|---|---|---|---|
| [`01-personal/`](./01-personal/) | 내 것 | stdio | 1명 (나) | "Host와 같은 머신, 부모-자식 프로세스" |
| [`02-external/`](./02-external/) | 외부 (JSONPlaceholder) | stdio | 1명 (나) | "MCP Server는 외부 API의 어댑터" |
| [`03-organization/`](./03-organization/) | 내 것 + 격리 | HTTP/SSE | 여러 명 (alice/bob) | "MCP Server도 멀티유저 SaaS가 될 수 있음" |

## 권장 학습 순서

**01-personal → 02-external → 03-organization** (폴더 번호 순)

이유:
1. **01-personal**: 자기가 만든 시스템을 자기가 LLM에 연결 — 가장 기본 멘탈 모델
2. **02-external**: 01에서 만든 MCP Server 패턴이 외부 API에도 그대로 적용된다는 사실 확인
3. **03-organization**: 운영 관점(HTTP transport, 인증, 멀티유저)으로 확장

각 시나리오는 자기 폴더 안에 자체 슬라이드·README·실행 가이드가 있다. 강사가 하나만 골라서 수업해도 OK.

## 필요 환경 (전 시나리오 공통)

- Java 21 (JDK)
- Gradle 8.x (각 폴더 wrapper 포함, 별도 설치 불필요)
- Claude Code CLI (Pro 요금제 권장)
- 선택: Marp (슬라이드 미리보기), VS Code REST Client (`.http` 파일)

## 참고

- 설계서: [`docs/superpowers/specs/2026-05-28-mcp-lecture-3scenarios.md`](docs/superpowers/specs/2026-05-28-mcp-lecture-3scenarios.md)
- 구현 계획서: `docs/superpowers/plans/2026-05-28-*.md`
- Spring AI MCP 문서: https://docs.spring.io/spring-ai/reference/api/mcp/
- MCP 명세: https://modelcontextprotocol.io/
```

- [ ] **Step 2: Commit**

```powershell
git add -A
git commit -m "docs: rewrite root README as 3-scenario hub with routing"
```

---

## Task 8: 최종 검증

**Files:** (코드 변경 없음, 검증만)

- [ ] **Step 1: 루트 디렉토리 구조 확인**

```powershell
Get-ChildItem -Force | Where-Object { $_.Name -notlike ".git*" } | Select-Object Name
```

Expected 출력 (대략):

```
.gitignore
.mcp.json (?)   ← 더 이상 루트에 없어야 함
01-personal
02-external (?) ← 아직 없음 (다음 plan)
03-organization (?) ← 아직 없음 (다음 plan)
docs
README.md
```

확인:
- 루트에 `01-resource-server/`, `02-mcp-server/`, `03-client/`, `.mcp.json` **없어야 함** (모두 이동/삭제됨)
- `01-personal/` 안에 `resource-server/`, `mcp-server/`, `docs/slides/`, `.mcp.json`, `README.md` 모두 있어야 함

```powershell
Get-ChildItem 01-personal -Force | Select-Object Name
```

- [ ] **Step 2: 01-personal/resource-server test 통과**

```powershell
Push-Location 01-personal\resource-server; .\gradlew test --console=plain 2>&1 | Select-Object -Last 10; Pop-Location
```

Expected: `BUILD SUCCESSFUL`, 13 tests.

- [ ] **Step 3: 01-personal/mcp-server test + bootJar**

```powershell
Push-Location 01-personal\mcp-server; .\gradlew test bootJar --console=plain 2>&1 | Select-Object -Last 10; Pop-Location
```

Expected: `BUILD SUCCESSFUL`, 7 tests, jar 빌드됨.

```powershell
Test-Path 01-personal\mcp-server\build\libs\mcp-server.jar
```

Expected: `True`.

- [ ] **Step 4: .mcp.json 경로 확인**

```powershell
Get-Content 01-personal\.mcp.json
```

jar 절대경로가 `01-personal\mcp-server\build\libs\mcp-server.jar` 인지 확인 (`02-mcp-server` 흔적 없어야).

- [ ] **Step 5: git log 검토**

```powershell
git log --oneline -10
```

Expected: 최근 7개 정도 commit이 migration 관련 (`migrate:`, `chore:`, `docs:`).

- [ ] **Step 6: (선택) Claude Code 연결 시연**

이건 controller가 사용자에게 위임. 사용자가 자기 환경에서:

```powershell
cd 01-personal
# Resource Server 띄우기 (터미널 1)
cd resource-server; ./gradlew bootRun

# Claude Code (터미널 2)
cd ..; claude
> 할일 목록 보여줘
```

자연어 시연이 동작하면 migration 성공. 동작 안 하면 트러블슈팅 (README 참조).

검증 단계라 commit 없음.

---

## Self-Review 체크리스트 (plan 작성자가 본 후 다음 단계로)

- ✅ Spec coverage: 설계서 §8 "기존 코드 처리"의 항목 모두 task에 매핑 (Resource 이동→Task 1, MCP 이동→Task 2, Client 삭제→Task 3, .mcp.json 이동/갱신→Task 4, 슬라이드→Task 5, README→Task 6/7)
- ✅ Placeholder 없음
- ✅ 단계 단순 (이동 + 텍스트 수정만, 새 코드 0)
- ✅ 검증 단계 포함 (Task 8)

---

## 다음 plan 예고

이 migration이 끝나면:
- `2026-05-28-02-external-new.md` — JSONPlaceholder MCP Server 신규 (01-personal 패턴 거의 그대로 + base-url 변경)
- `2026-05-28-03-organization-new.md` — HTTP transport + Bearer 인증 + 사용자별 격리 신규

세 plan은 독립이라 어떤 순서로도 가능. 그러나 강의 권장 순서(01 → 02 → 03)와 구현 난이도(쉬움 → 중간 → 어려움)가 일치하므로 그대로 진행 권장.
