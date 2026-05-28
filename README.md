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
