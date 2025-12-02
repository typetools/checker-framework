package org.checkerframework.framework.report;

import com.contrastsecurity.sarif.*;
import com.contrastsecurity.sarif.Result.Level;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.ProcessingEnvironment;

/**
 * Generates SARIF report files from checker diagnostics.
 *
 * <p>This is a POC implementation for Phase 1.
 */
public class SarifReportGenerator {

//  private final ProcessingEnvironment processingEnv;
  private final List<Result> results = new ArrayList<>();
  private final Map<String, Artifact> artifacts = new HashMap<>();


  public SarifReportGenerator(ProcessingEnvironment processingEnv) {
//    this.processingEnv = processingEnv;
System.out.println("SarifReportGenerator constructor");
  }


  /**
   * Add a diagnostic result to the report.
   *
   * @param kind       the diagnostic kind
   * @param message    the message text
   * @param messageKey the message key (rule ID)
   */
  public void addResult(
      javax.tools.Diagnostic.Kind kind,
      String message,
      String messageKey) {
    // TODO: For POC, just collect error and warning log
    if (kind != javax.tools.Diagnostic.Kind.ERROR
        && kind != javax.tools.Diagnostic.Kind.MANDATORY_WARNING) {
      return;
    }

    Result result = new Result()
        .withRuleId(messageKey)
        .withLevel(kind == javax.tools.Diagnostic.Kind.ERROR ? Level.ERROR : Level.WARNING)
        .withMessage(new Message().withText(message));

    results.add(result);
  }

  private String getCheckerVersion() {
    // Phase 1: 简化版本，返回固定值
    return "3.51.2-SNAPSHOT";
  }

  /**
   * Write the SARIF report to file.
   *
   * @param outputPath the output file path
   */
  public void writeReport(String outputPath) throws IOException {

    SarifSchema210 sarifLog = new SarifSchema210()
        .withVersion(SarifSchema210.Version._2_1_0)
        .withRuns(Collections.singletonList(new Run()
            .withTool(new Tool().withDriver(new ToolComponent()
                .withName("Checker Framework")
                .withVersion(getCheckerVersion())))
            .withResults(results)
            .withArtifacts(new HashSet<>(artifacts.values()))));

    // write into json file
    ObjectMapper mapper = new ObjectMapper();
    Path path = Paths.get(outputPath);
    mapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), sarifLog);
//    results.clear();
//    artifacts.clear();
  }
}
