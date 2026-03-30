# Migration & CLI Integration Walkthrough

## 1. Maven to Gradle Migration
- Created a standard JDK 1.8 [build.gradle](file:///c:/Users/s-wpa/Desktop/velocity-parser/build.gradle) and [settings.gradle](file:///c:/Users/s-wpa/Desktop/velocity-parser/settings.gradle).
- Eliminated [pom.xml](file:///c:/Users/s-wpa/Desktop/velocity-parser/pom.xml).
- Configured a `fatJar` task to bundle all compiled code and external dependencies into a single deployable artifact ([build/libs/velocity-parser-1.0.0-all.jar](file:///c:/Users/s-wpa/Desktop/velocity-parser/build/libs/velocity-parser-1.0.0-all.jar)).

## 2. Offline Build Support
- Successfully isolated the project from `mavenCentral()`.
- Downloaded and placed all required transitive dependencies (`velocity-engine-core`, `gson`, `junit-jupiter`, `slf4j-api`, etc.) into the internal `lib/` directory.
- Defined a `flatDir` repository in [build.gradle](file:///c:/Users/s-wpa/Desktop/velocity-parser/build.gradle) parsing the `lib/` directory via `fileTree`. 
- **Validation**: Executed `gradle build` successfully using only local `*.jar` copies.

## 3. TypeScript Integration Facade
- Created `com.velocityparser.cli.VelocityCliFacade` to serve as a command-line wrapper.
- The facade accepts a JSON payload over `stdin` providing an `action` ([extract](file:///c:/Users/s-wpa/Desktop/velocity-parser/src/main/java/com/velocityparser/VelocityVariableExtractor.java#58-82), [simulate](file:///c:/Users/s-wpa/Desktop/velocity-parser/src/main/java/com/velocityparser/VelocitySimulator.java#45-72), `branch`), a `template`, and `variables` (if simulating).
- It executes the requisite logic and returns a strict JSON payload on `stdout` containing `{ "success": true/false, "data": ... }`.
- Note: External logs (like SLF4J missing binder) output to `stderr` to ensure `stdout` stays pure JSON.

### Execution Example (TypeScript / Node.js Context)
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

// Pass the JSON payload through stdin
child.stdin.write(requestPayload);
child.stdin.end();
```
