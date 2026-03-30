# Velocity Parser & Simulator

Velocity Parser & Simulator는 Apache Velocity 템플릿을 분석하고 시뮬레이션하기 위한 독립 실행형 Java 애플리케이션입니다. 이 프로젝트는 CLI를 통해 JSON 기반의 요청을 받고 처리하여 TypeScript/Node.js 등 다양한 환경에서 쉽게 연동할 수 있도록 설계되었습니다.

## 주요 기능

1. **변수 추출 (Variable Extraction)**
   - `VelocityVariableExtractor`: 템플릿 내에서 사용된 모든 변수명(`$variable`, `$obj.field`, `#foreach` 등)을 AST(Abstract Syntax Tree)를 파싱하여 추출합니다.

2. **단순 시뮬레이션 (Simple Simulation)**
   - `VelocitySimulator`: 주어진 템플릿과 JSON 형태의 변수 맵 데이터를 결합하여 최종 렌더링된 결과 문자열을 반환합니다.

3. **분기 시뮬레이션 (Branch Simulation)**
   - `VelocityBranchSimulator`: 템플릿 내의 `#if`, `#elseif` 등 분기 조건을 분석하고, 모든 조건 조합(2^n)에 따른 결과를 시뮬레이션하여 목록 형태로 제공합니다.

## 기술 스택 및 빌드 환경

- **Java**: 1.8 이상
- **Build Tool**: Gradle (Offline 환경 지원)
- **주요 라이브러리**: Apache Velocity Engine, Gson

## 오프라인 빌드 (Offline Build Support)

이 프로젝트는 인터넷 연결 없이도 빌드할 수 있도록 구성되어 있습니다.
- 모든 필수 종속성(`jar` 파일)은 `lib/` 디렉토리에 포함되어 있습니다.
- `build.gradle`에서 `mavenCentral()` 대신 `flatDir` 리포지토리를 사용하여 로컬 디렉토리에서 라이브러리를 로드합니다.

```bash
# 외부 통신 없이 오프라인으로 빌드 및 테스트 수행
./gradlew build
```

빌드가 성공하면 `build/libs/velocity-parser-1.0.0-all.jar` 경로에 실행 가능한 Fat JAR 파일이 생성됩니다.

## CLI 인터페이스 사용 방법

CLI는 표준 입력(stdin)을 통해 JSON 페이로드를 받아 결과를 표준 출력(stdout)으로 반환합니다.

### 요청 형식 (JSON)
```json
{
  "action": "simulate",
  "template": "Hello $user!",
  "variables": {
    "user": "Alice"
  }
}
```
* `action`: "extract", "simulate", "branch" 중 하나를 지정합니다.
* `template`: 파싱할 Velocity 템플릿 문자열입니다.
* `variables`: "simulate" 수행 시 필요한 변수 데이터입니다.

### 실행 예시 (Node.js 환경)
```javascript
const { exec } = require('child_process');

const requestPayload = JSON.stringify({
  action: "simulate",
  template: "Hello $user!",
  variables: { user: "Alice" }
});

const child = exec('java -jar build/libs/velocity-parser-1.0.0-all.jar', (error, stdout, stderr) => {
    if (error) {
        console.error("Exec error:", error);
        return;
    }
    const response = JSON.parse(stdout);
    console.log("Result:", response.data); // "Hello Alice!"
});

child.stdin.write(requestPayload);
child.stdin.end();
```

### 응답 형식 (JSON)
성공 시:
```json
{
  "success": true,
  "data": "Hello Alice!"
}
```

실패 시:
```json
{
  "success": false,
  "errorMessage": "오류 내용 설명"
}
```
