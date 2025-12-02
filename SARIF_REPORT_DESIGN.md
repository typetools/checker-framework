# SARIF Report Generation Design Document

## 背景和目标

### 当前问题
1. Checker Framework 目前只将警告和错误输出到控制台
2. 在使用 Maven 或 Gradle 等构建系统时，输出顺序可能不一致（已知 bug）
3. 缺乏可解析的报告文件，难以构建通用的抑制系统

### 目标
- 生成可解析的 SARIF 格式报告文件
- 最小侵入地集成到现有代码
- 保持向后兼容（不影响现有控制台输出）
- 支持后续扩展（如通用抑制系统）

## 当前代码架构分析

### 消息输出流程

```
report() / reportError() / reportWarning()
    ↓
printOrStoreMessage()
    ↓
[如果有 messageStore] → CheckerMessage → messageStore (TreeSet)
    ↓
[处理完编译单元后] → printStoredMessages()
    ↓
Trees.printMessage() → 控制台输出
```

### 关键类和字段

1. **SourceChecker**
   - `messageStore: TreeSet<CheckerMessage>` - 存储消息（仅 compound checker 使用）
   - `printOrStoreMessage()` - 决定是存储还是立即打印
   - `printStoredMessages()` - 打印存储的消息

2. **CheckerMessage**
   - `kind: Diagnostic.Kind` - 消息类型（ERROR, WARNING 等）
   - `message: String` - 消息文本
   - `source: Tree` - 源代码位置
   - `checker: SourceChecker` - 发出消息的 checker
   - `trace: StackTraceElement[]` - 堆栈跟踪

3. **关键方法调用时机**
   - `typeProcessingStart()` - 初始化
   - `typeProcess()` - 处理每个编译单元
   - `printStoredMessages()` - 每个编译单元处理完后打印
   - `typeProcessingOver()` - 所有处理完成

## 设计方案

### 方案概述

采用类似 Error Prone 的非侵入式方法：
1. 在消息存储/打印时，同时收集到 SARIF 数据结构
2. 在 `typeProcessingOver()` 时生成 SARIF 报告文件
3. 通过命令行选项控制是否生成报告

### 设计原则

1. **最小侵入**：不改变现有的消息输出逻辑
2. **可选功能**：通过 `-AsarifOutput` 选项启用
3. **向后兼容**：默认不生成报告，不影响现有行为
4. **统一收集**：无论消息是立即打印还是存储，都收集到 SARIF

### 实现方案

#### 1. 新增命令行选项

在 `@SupportedOptions` 中添加：
```java
// Generate SARIF report file
// -AsarifOutput=path/to/report.sarif
"sarifOutput",
```

#### 2. 创建 SARIF 报告生成器

新建类：`org.checkerframework.framework.report.SarifReportGenerator`

**职责：**
- 收集所有诊断消息
- 转换为 SARIF 格式
- 写入文件

**关键方法（POC 简化版）：**
```java
public class SarifReportGenerator {
    private final List<Result> results = new ArrayList<>();
    private final Map<String, Artifact> artifacts = new HashMap<>();
    private final ProcessingEnvironment processingEnv;

    // 添加消息到报告（简化版：只收集 ERROR 和 WARNING）
    public void addResult(Diagnostic.Kind kind, String message,
                         Tree source, CompilationUnitTree root,
                         SourceChecker checker, String messageKey) {
        // 只处理 ERROR 和 WARNING
        if (kind != Diagnostic.Kind.ERROR && kind != Diagnostic.Kind.MANDATORY_WARNING) {
            return;
        }

        // 创建 Result 对象
        Result result = new Result()
            .withRuleId(messageKey)
            .withLevel(kind == Diagnostic.Kind.ERROR ? "error" : "warning")
            .withMessage(new Message().withText(message))
            .withLocations(Arrays.asList(createLocation(source, root)));

        results.add(result);

        // 记录文件信息
        addArtifact(root);
    }

    // 生成并写入 SARIF 文件
    public void writeReport(Path outputPath) throws IOException {
        SarifLog sarifLog = new SarifLog()
            .withVersion("2.1.0")
            .withRuns(Arrays.asList(
                new Run()
                    .withTool(createTool())
                    .withArtifacts(new ArrayList<>(artifacts.values()))
                    .withResults(results)
            ));

        // 使用 Jackson 序列化为 JSON
        ObjectMapper mapper = new ObjectMapper();
        mapper.writerWithDefaultPrettyPrinter().writeValue(outputPath.toFile(), sarifLog);
    }

    // 创建位置信息
    private Location createLocation(Tree source, CompilationUnitTree root);

    // 添加文件信息（包含源代码内容）
    private void addArtifact(CompilationUnitTree root);

    // 创建工具信息
    private Tool createTool();
}
```

#### 3. 集成点选择

**选项 A：在 `printOrStoreMessage()` 中收集（推荐）**

优点：
- 统一收集点，无论消息是存储还是立即打印
- 最小代码修改
- 可以获取完整的消息信息

修改点：
```java
protected void printOrStoreMessage(
    javax.tools.Diagnostic.Kind kind,
    String message,
    Tree source,
    CompilationUnitTree root) {
  // ... 现有代码 ...

  // 新增：收集到 SARIF
  if (sarifReportGenerator != null) {
    sarifReportGenerator.addResult(kind, message, source, root, this, messageKey);
  }
}
```

**选项 B：在 `report()` 方法中收集**

优点：
- 更早的收集点
- 可以获取原始 messageKey 和 args

缺点：
- 需要传递更多参数
- 可能收集到被抑制的消息

#### 4. 初始化 SARIF 报告生成器

在 `initChecker()` 中：
```java
public void initChecker() {
  // ... 现有代码 ...

  // 初始化 SARIF 报告生成器
  if (hasOption("sarifOutput")) {
    String outputPath = getOption("sarifOutput");
    if (outputPath == null) {
      throw new UserError("Must supply an argument to -AsarifOutput");
    }
    sarifReportGenerator = new SarifReportGenerator(processingEnv);
  }
}
```

#### 5. 生成报告文件

在 `typeProcessingOver()` 中：
```java
@Override
public void typeProcessingOver() {
  for (SourceChecker checker : getSubcheckers()) {
    checker.typeProcessingOver();
  }

  // 生成 SARIF 报告（仅在根 checker）
  if (parentChecker == null && sarifReportGenerator != null) {
    String outputPath = getOption("sarifOutput");
    try {
      sarifReportGenerator.writeReport(Paths.get(outputPath));
    } catch (IOException e) {
      logBugInCF(new BugInCF("Failed to write SARIF report", e));
    }
  }

  super.typeProcessingOver();
}
```

### SARIF 数据结构映射

| Checker Framework | SARIF |
|-----------------|-------|
| `Diagnostic.Kind.ERROR` | `result.level: "error"` |
| `Diagnostic.Kind.WARNING` | `result.level: "warning"` |
| `messageKey` | `result.ruleId` |
| `message` | `result.message.text` |
| `Tree source` | `result.locations[0].physicalLocation` |
| `SourceChecker` | `run.tool.driver.name` |
| `CompilationUnitTree` | `run.artifacts[].location` |

### 需要收集的信息

1. **结果信息 (Result)**
   - 消息类型（ERROR/WARNING）
   - 消息文本
   - 消息键（messageKey）
   - 源代码位置（文件、行号、列号）
   - 发出消息的 checker

2. **工具信息 (Tool)**
   - Checker Framework 版本
   - Checker 名称
   - 规则信息（从 messages.properties 提取）

3. **文件信息 (Artifact)**
   - 文件 URI
   - 文件内容（可选，用于 SARIF viewer）

## 实现细节讨论

### 问题 1：消息键 (messageKey) 的获取

**当前情况：**
- `printOrStoreMessage()` 只接收格式化后的 `message` 字符串
- 原始的 `messageKey` 在 `report()` 方法中

**解决方案：**
- 方案 A：修改 `printOrStoreMessage()` 签名，添加 `messageKey` 参数
- 方案 B：在 `CheckerMessage` 中添加 `messageKey` 字段
- 方案 C：从 `message` 中解析（不推荐，不可靠）

**推荐：方案 B** - 修改 `CheckerMessage` 类，添加 `messageKey` 字段

### 问题 2：子 checker 的消息收集

**当前情况：**
- Compound checker 有多个子 checker
- 每个子 checker 共享 `messageStore`
- 只有根 checker 的 `messageStore` 不为 null

**解决方案：**
- 所有 checker 共享同一个 `SarifReportGenerator` 实例
- 在根 checker 初始化，传递给子 checker
- 或者在根 checker 统一收集所有消息

**推荐：** 在根 checker 初始化 `SarifReportGenerator`，子 checker 通过 `parentChecker` 访问

### 问题 3：文件 URI 格式

SARIF 要求使用 URI 格式的文件路径。

**需要考虑：**
- 相对路径 vs 绝对路径
- Windows vs Unix 路径格式
- 工作目录的处理

**解决方案：**
- 使用 `File.toURI()` 或 `Path.toUri()` 转换为 URI
- 或者使用相对路径（相对于工作目录）

### 问题 4：报告文件写入时机

**选项 A：每个编译单元处理完后写入**
- 优点：增量更新，可以看到实时进度
- 缺点：多次文件 I/O，可能影响性能

**选项 B：所有处理完成后一次性写入（推荐）**
- 优点：性能好，文件一致
- 缺点：如果崩溃，可能丢失数据

**推荐：选项 B**，但可以考虑添加选项支持增量写入

### 问题 5：与现有 `-Adetailedmsgtext` 选项的关系

`-Adetailedmsgtext` 已经提供了可解析的输出格式。

**关系：**
- SARIF 是更标准化的格式
- `-Adetailedmsgtext` 是自定义格式
- 两者可以共存，服务于不同场景

**建议：** 保持两者独立，用户可以选择使用哪种格式

## 依赖管理

### 需要添加的依赖

在 `framework/build.gradle` 中添加：
```gradle
dependencies {
    implementation 'com.contrastsecurity:java-sarif:2.0'
    // Jackson 用于 JSON 序列化（java-sarif 依赖 Jackson）
    implementation 'com.fasterxml.jackson.core:jackson-databind:2.15.0' // 或兼容版本
}
```

**依赖确认：**
- ✅ 使用 [java-sarif](https://github.com/Contrast-Security-OSS/java-sarif) 库
- ✅ MIT License，与 Checker Framework 兼容
- ✅ 版本 2.0，支持 SARIF 2.1.0 规范

## 测试策略

### 单元测试
1. 测试 SARIF 报告生成器
2. 测试消息收集逻辑
3. 测试文件写入

### 集成测试
1. 使用真实 checker 生成报告
2. 验证 SARIF 文件格式正确性
3. 验证与现有功能的兼容性

### 验证工具
- 使用 SARIF 验证工具验证生成的报告
- 使用 GitHub 或其他支持 SARIF 的工具查看报告

## 后续扩展

### 可能的扩展方向

1. **抑制系统集成**
   - 从 SARIF 报告生成抑制文件
   - 从抑制文件过滤 SARIF 结果

2. **增量报告**
   - 支持只报告新增问题
   - 支持问题追踪

3. **报告格式扩展**
   - 支持其他格式（JSON、XML 等）
   - 支持自定义格式

4. **构建系统集成**
   - Maven 插件支持
   - Gradle 插件支持

## 问题解答

### 1. SARIF JSON 需要什么信息？MessageKey 里面带着吗？

SARIF JSON 的基本结构：

```json
{
  "version": "2.1.0",
  "$schema": "https://raw.githubusercontent.com/oasis-tcs/sarif-spec/master/Schemata/sarif-schema-2.1.0.json",
  "runs": [{
    "tool": {
      "driver": {
        "name": "Checker Framework",
        "version": "3.51.2",
        "rules": [...]
      }
    },
    "artifacts": [...],
    "results": [{
      "ruleId": "assignment.type.incompatible",  // ← messageKey 映射到这里
      "level": "error",
      "message": {
        "text": "incompatible types in assignment"
      },
      "locations": [{
        "physicalLocation": {
          "artifactLocation": {
            "uri": "file:///path/to/File.java"
          },
          "region": {
            "startLine": 10,
            "startColumn": 5
          }
        }
      }]
    }]
  }]
}
```

**关键字段说明：**
- `ruleId`: **messageKey 映射到这里**，用于标识触发该消息的规则
- `level`: 消息级别（error/warning/note）
- `message.text`: 格式化后的消息文本
- `locations[].physicalLocation`: 源代码位置（文件 URI、行号、列号）
- `artifacts[]`: 分析的文件列表（包含文件内容和 URI）

### 2. 能否使用 java-sarif 库？

**答案：可以！**

根据 [java-sarif 库](https://github.com/Contrast-Security-OSS/java-sarif) 的信息：
- **许可证**：MIT License（与 Checker Framework 兼容）
- **版本**：2.0（最新版本）
- **Maven 坐标**：
  ```xml
  <dependency>
    <groupId>com.contrastsecurity</groupId>
    <artifactId>java-sarif</artifactId>
    <version>2.0</version>
  </dependency>
  ```
- **特点**：
  - 使用 Jackson 进行 JSON 序列化/反序列化
  - 提供方法链式构建 API
  - 符合 SARIF 2.1.0 规范

**使用示例：**
```java
import com.contrastsecurity.sarif.*;

SarifLog sarifLog = new SarifLog()
    .withVersion("2.1.0")
    .withRuns(Arrays.asList(
        new Run()
            .withTool(new Tool()
                .withDriver(new ToolComponent()
                    .withName("Checker Framework")
                    .withVersion("3.51.2")))
            .withResults(Arrays.asList(
                new Result()
                    .withRuleId("assignment.type.incompatible")
                    .withLevel("error")
                    .withMessage(new Message().withText("incompatible types"))
            ))
    ));
```

### 3. SARIF 生成完了怎么呈现？是写在一个文件里吗？

**答案：是的，SARIF 是一个 JSON 文件。**

**生成方式：**
- 通过 `-AsarifOutput=path/to/report.sarif` 选项指定输出路径
- 在 `typeProcessingOver()` 时一次性写入 JSON 文件
- 文件格式：标准的 JSON，符合 SARIF 2.1.0 规范

**呈现方式：**
1. **GitHub Code Scanning**：上传 SARIF 文件到 GitHub，可以在 PR 中看到问题
2. **VS Code SARIF Viewer**：使用 VS Code 插件查看
3. **Azure DevOps**：集成到 CI/CD 流程
4. **其他工具**：任何支持 SARIF 格式的工具都可以读取

**文件示例：**
```
$ javac -processor NullnessChecker -AsarifOutput=report.sarif MyFile.java
$ cat report.sarif
{
  "version": "2.1.0",
  "runs": [...]
}
```

### 4. 错误处理：SARIF 生成失败是否影响编译？

**答案：不影响编译。**

- SARIF 报告生成是**可选功能**，失败不应该影响编译
- 如果生成失败，记录错误但不抛出异常
- 可以输出警告信息，但编译继续进行

### 5. 报告内容：是否包含源代码内容？

**答案：需要包含。**

原因：
- 开发者需要源代码内容来定位问题
- SARIF viewer 需要源代码来高亮显示问题位置
- 便于离线查看报告

实现方式：
- 在 `artifacts[]` 中包含文件内容
- 使用 `artifactLocation.uri` 指向文件
- 使用 `artifact.contents.text` 存储源代码内容

### 6. 初始实现：POC 版本，越简单越好

**简化方案：**

1. **最小实现范围**：
   - 只收集基本的错误和警告
   - 不处理 NOTE 类型的消息
   - 简化规则信息（暂时不解析 messages.properties）

2. **简化数据结构**：
   - 只包含必需字段：ruleId, level, message, location
   - 文件内容可选（先实现基本版本）

3. **分阶段实现**：
   - **Phase 1 (POC)**：基本消息收集和 SARIF 文件生成
   - **Phase 2**：完善规则信息、文件内容等
   - **Phase 3**：优化和扩展功能

## 参考资源

1. SARIF 规范：https://docs.oasis-open.org/sarif/sarif/v2.1.0/sarif-v2.1.0.html
2. Error Prone 讨论：https://github.com/google/error-prone/issues/3766
3. Java SARIF 库：https://github.com/Contrast-Security-OSS/java-sarif
4. GitHub SARIF 支持：https://docs.github.com/en/code-security/code-scanning/integrating-with-code-scanning/sarif-support-for-code-scanning

## POC 实现计划（简化版）

### Phase 1: 基础实现（POC）

**目标：** 能够生成基本的 SARIF 报告文件

**任务清单：**
1. ✅ 添加依赖：`com.contrastsecurity:java-sarif:2.0`
2. 创建 `SarifReportGenerator` 类（简化版）
3. 在 `SourceChecker` 中添加 `sarifReportGenerator` 字段
4. 修改 `CheckerMessage` 添加 `messageKey` 字段
5. 在 `printOrStoreMessage()` 中收集消息
6. 在 `typeProcessingOver()` 中写入文件
7. 添加 `-AsarifOutput` 选项

**简化点：**
- 只收集 ERROR 和 WARNING（忽略 NOTE）
- 不解析 messages.properties（ruleId 直接用 messageKey）
- 文件内容可选（先实现基本位置信息）
- 不处理子 checker 的复杂情况（先支持单个 checker）

### Phase 2: 完善功能

- 支持所有消息类型
- 完善规则信息（从 messages.properties 提取）
- 支持 compound checker
- 包含源代码内容

### Phase 3: 优化和扩展

- 性能优化
- 增量报告
- 抑制系统集成

## 下一步行动

1. ✅ 确认设计方案和依赖库
2. 实现 POC 版本（Phase 1）
3. 测试基本功能
4. 验证 SARIF 文件格式
5. 迭代完善（Phase 2 & 3）

