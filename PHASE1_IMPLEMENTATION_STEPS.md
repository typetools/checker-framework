# Phase 1 (POC) 实现步骤详解

## 步骤 1: 添加命令行选项（仅参数，无实现）

### 目标
添加 `-AsarifOutput` 命令行选项，当启用时输出日志信息。

### 具体操作

1. **在 `SourceChecker.java` 的 `@SupportedOptions` 注解中添加选项**
   - 文件：`framework/src/main/java/org/checkerframework/framework/source/SourceChecker.java`
   - 位置：找到 `@SupportedOptions({...})` 注解（大约第 111 行）
   - 添加：
     ```java
     // Generate SARIF report file
     // -AsarifOutput=path/to/report.sarif
     "sarifOutput",
     ```

2. **在 `SourceChecker` 类中添加字段**
   - 位置：在类的字段声明区域（大约第 640 行附近）
   - 添加：
     ```java
     /** True if the -AsarifOutput command-line argument was passed. */
     private boolean sarifOutputEnabled = false;

     /** Path to SARIF output file. */
     private @Nullable String sarifOutputPath = null;
     ```

3. **在 `initChecker()` 方法中读取选项**
   - 位置：`initChecker()` 方法中（大约第 1103 行附近，在设置其他选项的地方）
   - 添加：
     ```java
     sarifOutputEnabled = hasOption("sarifOutput");
     if (sarifOutputEnabled) {
       sarifOutputPath = getOption("sarifOutput");
       if (sarifOutputPath == null) {
         throw new UserError("Must supply an argument to -AsarifOutput");
       }
       // TODO: 临时日志输出，验证选项是否生效
       message(Diagnostic.Kind.NOTE,
               "SARIF output enabled: " + sarifOutputPath);
     }
     ```

### 验证方法

运行测试命令：
```bash
javac -processor NullnessChecker -AsarifOutput=test.sarif Test.java
```

预期输出：应该看到 NOTE 消息："SARIF output enabled: test.sarif"

### 调试提示

- 如果看不到 NOTE 消息，检查 `hasOption("sarifOutput")` 是否正确
- 如果抛出 UserError，检查选项值是否正确传递

---

## 步骤 2: 添加依赖并创建空的 SarifReportGenerator 类

### 目标
添加 java-sarif 依赖，创建 SarifReportGenerator 类的骨架（不实现功能）。

### 具体操作

1. **添加 Maven/Gradle 依赖**
   - 文件：`framework/build.gradle`
   - 在 `dependencies` 块中添加：
     ```gradle
     dependencies {
         // ... 现有依赖 ...
         implementation 'com.contrastsecurity:java-sarif:2.0'
         implementation 'com.fasterxml.jackson.core:jackson-databind:2.15.0'
     }
     ```

2. **创建 SarifReportGenerator 类文件**
   - 文件：`framework/src/main/java/org/checkerframework/framework/report/SarifReportGenerator.java`
   - 创建基本类结构：
     ```java
     package org.checkerframework.framework.report;

     import javax.annotation.processing.ProcessingEnvironment;

     /**
      * Generates SARIF report files from checker diagnostics.
      *
      * <p>This is a POC implementation for Phase 1.
      */
     public class SarifReportGenerator {

         private final ProcessingEnvironment processingEnv;

         public SarifReportGenerator(ProcessingEnvironment processingEnv) {
             this.processingEnv = processingEnv;
         }

         /**
          * Add a diagnostic result to the report.
          *
          * @param kind the diagnostic kind
          * @param message the message text
          * @param messageKey the message key (rule ID)
          */
         public void addResult(
                 javax.tools.Diagnostic.Kind kind,
                 String message,
                 String messageKey) {
             // TODO: Phase 1 - 暂时不实现，只记录日志
             System.out.println("[SARIF] Would add result: " + messageKey + " - " + message);
         }

         /**
          * Write the SARIF report to file.
          *
          * @param outputPath the output file path
          */
         public void writeReport(String outputPath) {
             // TODO: Phase 1 - 暂时不实现，只记录日志
             System.out.println("[SARIF] Would write report to: " + outputPath);
         }
     }
     ```

3. **在 `SourceChecker` 中添加字段和初始化**
   - 文件：`framework/src/main/java/org/checkerframework/framework/source/SourceChecker.java`
   - 在字段声明区域添加：
     ```java
     /** SARIF report generator, if enabled. */
     private @Nullable SarifReportGenerator sarifReportGenerator = null;
     ```
   - 在 `initChecker()` 中（步骤 1 的代码之后）添加：
     ```java
     if (sarifOutputEnabled) {
       sarifOutputPath = getOption("sarifOutput");
       if (sarifOutputPath == null) {
         throw new UserError("Must supply an argument to -AsarifOutput");
       }
       sarifReportGenerator = new SarifReportGenerator(processingEnv);
       message(Diagnostic.Kind.NOTE,
               "SARIF report generator initialized: " + sarifOutputPath);
     }
     ```

### 验证方法

运行测试命令：
```bash
javac -processor NullnessChecker -AsarifOutput=test.sarif Test.java
```

预期输出：
- NOTE 消息："SARIF report generator initialized: test.sarif"
- 编译应该成功（没有错误）

### 调试提示

- 如果编译失败，检查依赖是否正确添加
- 如果找不到类，检查包名和导入语句

---

## 步骤 3: 使用 Mock 数据生成 SARIF 文件

### 目标
使用硬编码的 mock 数据生成一个有效的 SARIF JSON 文件，验证整个流程。

### 具体操作

1. **更新 `SarifReportGenerator` 类，添加 mock 数据生成**
   - 文件：`framework/src/main/java/org/checkerframework/framework/report/SarifReportGenerator.java`
   - 添加必要的导入：
     ```java
     import com.contrastsecurity.sarif.*;
     import com.fasterxml.jackson.databind.ObjectMapper;
     import java.io.IOException;
     import java.nio.file.Files;
     import java.nio.file.Path;
     import java.nio.file.Paths;
     import java.util.Arrays;
     import java.util.Collections;
     ```
   - 更新 `writeReport()` 方法：
     ```java
     public void writeReport(String outputPath) throws IOException {
         // Phase 1: 使用 mock 数据生成 SARIF 文件
         SarifLog sarifLog = new SarifLog()
             .withVersion("2.1.0")
             .withRuns(Collections.singletonList(
                 new Run()
                     .withTool(new Tool()
                         .withDriver(new ToolComponent()
                             .withName("Checker Framework")
                             .withVersion("3.51.2-SNAPSHOT")))
                     .withResults(Collections.singletonList(
                         new Result()
                             .withRuleId("mock.rule.id")
                             .withLevel("error")
                             .withMessage(new Message()
                                 .withText("This is a mock SARIF result for testing"))
                             .withLocations(Collections.singletonList(
                                 new Location()
                                     .withPhysicalLocation(new PhysicalLocation()
                                         .withArtifactLocation(new ArtifactLocation()
                                             .withUri("file:///mock/Test.java"))
                                         .withRegion(new Region()
                                             .withStartLine(10)
                                             .withStartColumn(5))))))
             ));

         // 写入 JSON 文件
         ObjectMapper mapper = new ObjectMapper();
         Path path = Paths.get(outputPath);
         mapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), sarifLog);

         System.out.println("[SARIF] Mock report written to: " + outputPath);
     }
     ```

2. **在 `SourceChecker.typeProcessingOver()` 中调用写入**
   - 文件：`framework/src/main/java/org/checkerframework/framework/source/SourceChecker.java`
   - 位置：`typeProcessingOver()` 方法（大约第 1055 行）
   - 修改：
     ```java
     @Override
     public void typeProcessingOver() {
       for (SourceChecker checker : getSubcheckers()) {
         checker.typeProcessingOver();
       }

       // Phase 1: 生成 SARIF 报告（仅在根 checker）
       if (parentChecker == null && sarifReportGenerator != null) {
         try {
           sarifReportGenerator.writeReport(sarifOutputPath);
           message(Diagnostic.Kind.NOTE, "SARIF report written to: " + sarifOutputPath);
         } catch (IOException e) {
           message(Diagnostic.Kind.WARNING,
                   "Failed to write SARIF report: " + e.getMessage());
         }
       }

       super.typeProcessingOver();
     }
     ```

### 验证方法

1. **运行测试命令：**
   ```bash
   javac -processor NullnessChecker -AsarifOutput=test.sarif Test.java
   ```

2. **检查输出文件：**
   ```bash
   cat test.sarif
   ```

预期结果：
- 生成 `test.sarif` 文件
- 文件包含有效的 JSON
- JSON 符合 SARIF 2.1.0 格式
- 包含一个 mock 结果

3. **验证 SARIF 格式（可选）：**
   - 使用在线 SARIF 验证器：https://sarifweb.azurewebsites.net/Validator
   - 或使用 VS Code SARIF Viewer 插件查看

### 调试提示

- 如果文件没有生成，检查文件路径和权限
- 如果 JSON 格式错误，检查 ObjectMapper 配置
- 如果抛出异常，检查依赖是否正确加载

---

## 步骤 4: 在 printOrStoreMessage 中收集消息（不写入文件）

### 目标
在消息打印/存储时，同时收集到 SarifReportGenerator，但不立即写入文件。

### 具体操作

1. **修改 `CheckerMessage` 类，添加 `messageKey` 字段**
   - 文件：`framework/src/main/java/org/checkerframework/framework/source/SourceChecker.java`
   - 位置：`CheckerMessage` 内部类（大约第 3479 行）
   - 添加字段：
     ```java
     /** The message key (rule ID) for this message. */
     final String messageKey;
     ```
   - 更新构造函数：
     ```java
     protected CheckerMessage(
         Diagnostic.Kind kind,
         String message,
         @FindDistinct Tree source,
         @FindDistinct SourceChecker checker,
         StackTraceElement[] trace,
         String messageKey) {  // 新增参数
       this.kind = kind;
       this.message = message;
       this.source = source;
       this.checker = checker;
       this.trace = trace;
       this.messageKey = messageKey;  // 新增赋值
     }
     ```

2. **修改 `printOrStoreMessage()` 方法，传递 messageKey**
   - 文件：`framework/src/main/java/org/checkerframework/framework/source/SourceChecker.java`
   - 位置：`printOrStoreMessage()` 方法（大约第 1559 行）
   - 问题：当前方法没有 messageKey 参数
   - 解决方案：暂时使用 "unknown" 作为占位符，后续步骤会修复
   - 修改：
     ```java
     protected void printOrStoreMessage(
         javax.tools.Diagnostic.Kind kind,
         String message,
         Tree source,
         CompilationUnitTree root) {
       assert this.currentRoot == root;
       StackTraceElement[] trace = Thread.currentThread().getStackTrace();
       if (messageStore == null) {
         printOrStoreMessage(kind, message, source, root, trace);
       } else {
         // Phase 1: 暂时使用 "unknown" 作为 messageKey
         String messageKey = "unknown";
         CheckerMessage checkerMessage = new CheckerMessage(
             kind, message, source, this, trace, messageKey);
         messageStore.add(checkerMessage);
       }

       // Phase 1: 收集消息到 SARIF（如果启用）
       if (sarifReportGenerator != null && parentChecker == null) {
         // 暂时使用 "unknown" 作为 messageKey
         sarifReportGenerator.addResult(kind, message, "unknown");
       }
     }
     ```

3. **更新 `SarifReportGenerator.addResult()` 方法签名**
   - 文件：`framework/src/main/java/org/checkerframework/framework/report/SarifReportGenerator.java`
   - 修改方法：
     ```java
     public void addResult(
             javax.tools.Diagnostic.Kind kind,
             String message,
             String messageKey) {
         // Phase 1: 只收集，不处理
         // 暂时只记录日志，验证消息是否被收集
         System.out.println("[SARIF] Collected: " + messageKey + " - " +
                           kind + " - " + message.substring(0, Math.min(50, message.length())));
     }
     ```

### 验证方法

运行测试命令：
```bash
javac -processor NullnessChecker -AsarifOutput=test.sarif Test.java
```

预期输出：
- 应该看到多个 `[SARIF] Collected:` 日志消息
- 每个诊断消息都应该被收集
- 文件仍然包含 mock 数据（因为还没实现真实数据写入）

### 调试提示

- 如果没有看到收集日志，检查 `sarifReportGenerator != null` 条件
- 如果只看到部分消息，检查 `parentChecker == null` 条件（可能子 checker 也在收集）

---

## 步骤 5: 实现真实数据收集和 SARIF 生成

### 目标
使用真实收集的消息数据生成 SARIF 文件，替换 mock 数据。

### 具体操作

1. **在 `SarifReportGenerator` 中添加数据存储**
   - 文件：`framework/src/main/java/org/checkerframework/framework/report/SarifReportGenerator.java`
   - 添加字段：
     ```java
     import java.util.ArrayList;
     import java.util.HashMap;
     import java.util.List;
     import java.util.Map;

     // 在类中添加字段
     private final List<Result> results = new ArrayList<>();
     private final Map<String, Artifact> artifacts = new HashMap<>();
     ```
   - 更新 `addResult()` 方法：
     ```java
     public void addResult(
             javax.tools.Diagnostic.Kind kind,
             String message,
             String messageKey) {
         // Phase 1: 只收集 ERROR 和 WARNING
         if (kind != javax.tools.Diagnostic.Kind.ERROR
             && kind != javax.tools.Diagnostic.Kind.MANDATORY_WARNING) {
             return;
         }

         // 创建 Result 对象
         String level = kind == javax.tools.Diagnostic.Kind.ERROR ? "error" : "warning";
         Result result = new Result()
             .withRuleId(messageKey)
             .withLevel(level)
             .withMessage(new Message().withText(message));

         results.add(result);
     }
     ```

2. **更新 `writeReport()` 方法，使用真实数据**
   - 文件：`framework/src/main/java/org/checkerframework/framework/report/SarifReportGenerator.java`
   - 修改方法：
     ```java
     public void writeReport(String outputPath) throws IOException {
         // 创建 Tool 信息
         ToolComponent driver = new ToolComponent()
             .withName("Checker Framework")
             .withVersion(getCheckerVersion());  // 需要实现这个方法

         // 创建 Run
         Run run = new Run()
             .withTool(new Tool().withDriver(driver))
             .withResults(results)
             .withArtifacts(new ArrayList<>(artifacts.values()));

         // 创建 SarifLog
         SarifLog sarifLog = new SarifLog()
             .withVersion("2.1.0")
             .withRuns(Collections.singletonList(run));

         // 写入文件
         ObjectMapper mapper = new ObjectMapper();
         Path path = Paths.get(outputPath);
         mapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), sarifLog);
     }

     private String getCheckerVersion() {
         // Phase 1: 简化版本，返回固定值
         return "3.51.2-SNAPSHOT";
     }
     ```

3. **清空结果列表（在写入后）**
   - 在 `writeReport()` 方法末尾添加：
     ```java
     // 清空结果，为下次运行做准备
     results.clear();
     artifacts.clear();
     ```

### 验证方法

1. **运行测试命令：**
   ```bash
   javac -processor NullnessChecker -AsarifOutput=test.sarif Test.java
   ```

2. **检查生成的 SARIF 文件：**
   ```bash
   cat test.sarif | jq '.runs[0].results | length'
   ```

预期结果：
- SARIF 文件包含真实的结果（不再是 mock 数据）
- 结果数量应该与收集的消息数量一致
- 每个结果都有正确的 ruleId、level 和 message

### 调试提示

- 如果结果数量为 0，检查 `addResult()` 是否被正确调用
- 如果 ruleId 都是 "unknown"，需要继续下一步获取真实的 messageKey
- 如果 JSON 格式错误，检查 ObjectMapper 序列化

---

## 步骤 6: 获取真实的 messageKey

### 目标
从 `report()` 方法传递真实的 messageKey 到 `printOrStoreMessage()`。

### 具体操作

1. **修改 `report()` 方法，传递 messageKey**
   - 文件：`framework/src/main/java/org/checkerframework/framework/source/SourceChecker.java`
   - 位置：`report()` 方法（大约第 1426 行）
   - 问题：需要将 messageKey 传递到 `printOrStoreMessage()`
   - 解决方案：修改 `printOrStoreMessage()` 方法签名，添加 messageKey 参数
   - 修改 `report()` 方法中调用 `printOrStoreMessage()` 的地方：
     ```java
     if (source instanceof Tree) {
       printOrStoreMessage(kind, messageText, (Tree) source, currentRoot, messageKey);
     }
     ```

2. **更新 `printOrStoreMessage()` 方法签名**
   - 文件：`framework/src/main/java/org/checkerframework/framework/source/SourceChecker.java`
   - 位置：`printOrStoreMessage()` 方法（大约第 1559 行）
   - 修改方法签名：
     ```java
     protected void printOrStoreMessage(
         javax.tools.Diagnostic.Kind kind,
         String message,
         Tree source,
         CompilationUnitTree root,
         String messageKey) {  // 新增参数
       assert this.currentRoot == root;
       StackTraceElement[] trace = Thread.currentThread().getStackTrace();
       if (messageStore == null) {
         printOrStoreMessage(kind, message, source, root, trace, messageKey);
       } else {
         CheckerMessage checkerMessage = new CheckerMessage(
             kind, message, source, this, trace, messageKey);
         messageStore.add(checkerMessage);
       }

       // 收集消息到 SARIF
       if (sarifReportGenerator != null && parentChecker == null) {
         sarifReportGenerator.addResult(kind, message, messageKey);
       }
     }
     ```

3. **更新另一个 `printOrStoreMessage()` 重载方法**
   - 位置：`printOrStoreMessage()` 的另一个重载（大约第 1584 行）
   - 修改：
     ```java
     protected void printOrStoreMessage(
         javax.tools.Diagnostic.Kind kind,
         String message,
         Tree source,
         CompilationUnitTree root,
         StackTraceElement[] trace,
         String messageKey) {  // 新增参数
       Trees.instance(processingEnv).printMessage(kind, message, source, root);
       printStackTrace(trace);
     }
     ```

4. **更新 `printStoredMessages()` 方法**
   - 位置：`printStoredMessages()` 方法（大约第 2263 行）
   - 修改：
     ```java
     protected void printStoredMessages(CompilationUnitTree unit) {
       if (messageStore == null || parentChecker != null) {
         return;
       }
       for (CheckerMessage msg : messageStore) {
         printOrStoreMessage(msg.kind, msg.message, msg.source, unit, msg.trace, msg.messageKey);
       }
     }
     ```

### 验证方法

运行测试命令：
```bash
javac -processor NullnessChecker -AsarifOutput=test.sarif Test.java
```

检查 SARIF 文件中的 ruleId：
```bash
cat test.sarif | jq '.runs[0].results[].ruleId'
```

预期结果：
- ruleId 应该是真实的 messageKey（如 "assignment.type.incompatible"）
- 不再是 "unknown"

### 调试提示

- 如果 ruleId 仍然是 "unknown"，检查方法调用链是否正确传递参数
- 如果编译错误，检查所有调用 `printOrStoreMessage()` 的地方是否都更新了

---

## 步骤 7: 添加位置信息（文件 URI、行号、列号）

### 目标
在 SARIF 结果中添加源代码位置信息（文件 URI、行号、列号）。

### 具体操作

1. **修改 `SarifReportGenerator.addResult()` 方法，添加位置参数**
   - 文件：`framework/src/main/java/org/checkerframework/framework/report/SarifReportGenerator.java`
   - 添加必要的导入：
     ```java
     import com.sun.source.tree.Tree;
     import com.sun.source.tree.CompilationUnitTree;
     import com.sun.source.util.SourcePositions;
     import com.sun.source.util.Trees;
     import javax.annotation.processing.ProcessingEnvironment;
     ```
   - 修改方法签名：
     ```java
     public void addResult(
             javax.tools.Diagnostic.Kind kind,
             String message,
             String messageKey,
             Tree source,
             CompilationUnitTree root) {  // 新增参数
     ```

2. **实现位置信息提取**
   - 在 `addResult()` 方法中添加：
     ```java
     // 获取文件 URI
     String fileUri = getFileUri(root);

     // 获取位置信息（行号、列号）
     Region region = getRegion(source, root);

     // 创建 Location
     Location location = new Location()
         .withPhysicalLocation(new PhysicalLocation()
             .withArtifactLocation(new ArtifactLocation().withUri(fileUri))
             .withRegion(region));

     // 创建 Result
     String level = kind == javax.tools.Diagnostic.Kind.ERROR ? "error" : "warning";
     Result result = new Result()
         .withRuleId(messageKey)
         .withLevel(level)
         .withMessage(new Message().withText(message))
         .withLocations(Collections.singletonList(location));

     results.add(result);
     ```

3. **实现辅助方法**
   - 在 `SarifReportGenerator` 类中添加：
     ```java
     private String getFileUri(CompilationUnitTree root) {
         // Phase 1: 简化版本，使用文件路径转换为 URI
         try {
             java.io.File file = new java.io.File(root.getSourceFile().getName());
             return file.toURI().toString();
         } catch (Exception e) {
             return "file:///unknown";
         }
     }

     private Region getRegion(Tree source, CompilationUnitTree root) {
         Trees trees = Trees.instance(processingEnv);
         SourcePositions sourcePositions = trees.getSourcePositions();

         long startPos = sourcePositions.getStartPosition(root, source);
         long endPos = sourcePositions.getEndPosition(root, source);

         if (startPos == -1 || endPos == -1) {
             // 无法获取位置，返回默认值
             return new Region().withStartLine(1).withStartColumn(1);
         }

         // 计算行号和列号（简化版本）
         // Phase 1: 使用简单的行号计算
         String sourceText = root.getSourceFile().getCharContent(true).toString();
         int lineNumber = 1;
         int columnNumber = 1;

         for (int i = 0; i < startPos && i < sourceText.length(); i++) {
             if (sourceText.charAt(i) == '\n') {
                 lineNumber++;
                 columnNumber = 1;
             } else {
                 columnNumber++;
             }
         }

         return new Region()
             .withStartLine(lineNumber)
             .withStartColumn(columnNumber)
             .withEndLine(lineNumber)  // Phase 1: 简化，结束位置同开始位置
             .withEndColumn(columnNumber);
     }
     ```

4. **更新 `SourceChecker` 中的调用**
   - 文件：`framework/src/main/java/org/checkerframework/framework/source/SourceChecker.java`
   - 在 `printOrStoreMessage()` 方法中：
     ```java
     // 收集消息到 SARIF
     if (sarifReportGenerator != null && parentChecker == null) {
       sarifReportGenerator.addResult(kind, message, messageKey, source, root);
     }
     ```

### 验证方法

1. **运行测试命令：**
   ```bash
   javac -processor NullnessChecker -AsarifOutput=test.sarif Test.java
   ```

2. **检查 SARIF 文件中的位置信息：**
   ```bash
   cat test.sarif | jq '.runs[0].results[0].locations[0].physicalLocation'
   ```

预期结果：
- 每个结果都有 `physicalLocation`
- `artifactLocation.uri` 包含文件 URI
- `region` 包含 `startLine` 和 `startColumn`

### 调试提示

- 如果 URI 是 "file:///unknown"，检查文件路径获取逻辑
- 如果行号/列号不正确，检查 `getRegion()` 方法的计算逻辑
- 如果位置信息缺失，检查 `source` 和 `root` 参数是否正确传递

---

## 步骤 8: 添加文件内容到 artifacts（可选，但推荐）

### 目标
在 SARIF 的 `artifacts` 中包含源代码文件内容，方便 SARIF viewer 显示。

### 具体操作

1. **在 `addResult()` 中记录文件信息**
   - 文件：`framework/src/main/java/org/checkerframework/framework/report/SarifReportGenerator.java`
   - 在 `addResult()` 方法开始处添加：
     ```java
     // 记录文件信息到 artifacts
     String fileUri = getFileUri(root);
     if (!artifacts.containsKey(fileUri)) {
         addArtifact(root, fileUri);
     }
     ```

2. **实现 `addArtifact()` 方法**
   - 在 `SarifReportGenerator` 类中添加：
     ```java
     private void addArtifact(CompilationUnitTree root, String fileUri) {
         try {
             // 读取文件内容
             String content = root.getSourceFile().getCharContent(true).toString();

             // 创建 ArtifactContent
             ArtifactContent artifactContent = new ArtifactContent()
                 .withText(content);

             // 创建 Artifact
             Artifact artifact = new Artifact()
                 .withLocation(new ArtifactLocation().withUri(fileUri))
                 .withContents(artifactContent);

             artifacts.put(fileUri, artifact);
         } catch (Exception e) {
             // Phase 1: 如果读取失败，创建不包含内容的 artifact
             Artifact artifact = new Artifact()
                 .withLocation(new ArtifactLocation().withUri(fileUri));
             artifacts.put(fileUri, artifact);
         }
     }
     ```

3. **确保 artifacts 在写入时包含**
   - 在 `writeReport()` 方法中已经包含了 artifacts，无需修改

### 验证方法

1. **运行测试命令：**
   ```bash
   javac -processor NullnessChecker -AsarifOutput=test.sarif Test.java
   ```

2. **检查 SARIF 文件中的 artifacts：**
   ```bash
   cat test.sarif | jq '.runs[0].artifacts[0].contents.text' | head -20
   ```

预期结果：
- `artifacts` 数组包含分析的文件
- 每个 artifact 的 `contents.text` 包含源代码内容

### 调试提示

- 如果文件内容为空，检查文件读取逻辑
- 如果文件很大，考虑是否限制内容大小（Phase 1 可以暂时不限制）

---

## 步骤 9: 清理和优化

### 目标
移除调试日志，优化代码，确保 POC 版本稳定。

### 具体操作

1. **移除所有 `System.out.println` 调试日志**
   - 在 `SarifReportGenerator` 中移除所有调试输出
   - 在 `SourceChecker` 中移除临时的 NOTE 消息（可选，可以保留一个确认消息）

2. **错误处理优化**
   - 确保所有异常都被正确处理
   - 在 `typeProcessingOver()` 中，如果写入失败，只记录警告，不影响编译

3. **代码清理**
   - 移除 TODO 注释中的 "Phase 1" 标记（如果不再需要）
   - 添加必要的 JavaDoc 注释

4. **最终验证**
   - 运行完整的测试套件
   - 验证生成的 SARIF 文件可以被标准工具读取

### 验证方法

1. **运行多个测试用例：**
   ```bash
   javac -processor NullnessChecker -AsarifOutput=test.sarif Test1.java Test2.java
   ```

2. **使用 SARIF 验证器验证文件格式**

3. **使用 VS Code SARIF Viewer 查看报告**

### 完成标准

- ✅ 可以生成有效的 SARIF 2.1.0 格式文件
- ✅ 包含真实的诊断结果（ruleId、level、message）
- ✅ 包含位置信息（文件 URI、行号、列号）
- ✅ 包含源代码内容（可选但推荐）
- ✅ 不影响现有功能
- ✅ 错误处理完善

---

## 总结

Phase 1 (POC) 实现完成后的功能：
- 通过 `-AsarifOutput` 选项启用 SARIF 报告生成
- 收集所有 ERROR 和 WARNING 类型的诊断消息
- 生成符合 SARIF 2.1.0 标准的 JSON 文件
- 包含基本的位置信息和源代码内容

下一步（Phase 2）可以完善：
- 支持所有消息类型
- 完善规则信息（从 messages.properties 提取）
- 支持 compound checker
- 性能优化

