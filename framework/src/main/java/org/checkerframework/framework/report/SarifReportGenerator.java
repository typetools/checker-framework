package org.checkerframework.framework.report;

import com.contrastsecurity.sarif.Artifact;
import com.contrastsecurity.sarif.ArtifactContent;
import com.contrastsecurity.sarif.ArtifactLocation;
import com.contrastsecurity.sarif.Location;
import com.contrastsecurity.sarif.Message;
import com.contrastsecurity.sarif.PhysicalLocation;
import com.contrastsecurity.sarif.Region;
import com.contrastsecurity.sarif.Result;
import com.contrastsecurity.sarif.Result.Level;
import com.contrastsecurity.sarif.Run;
import com.contrastsecurity.sarif.SarifSchema210;
import com.contrastsecurity.sarif.Tool;
import com.contrastsecurity.sarif.ToolComponent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.LineMap;
import com.sun.source.tree.Tree;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.Trees;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.annotation.processing.ProcessingEnvironment;
import javax.tools.Diagnostic;

/**
 * Generates SARIF (Static Analysis Results Interchange Format) report files from checker
 * diagnostics.
 *
 * <p>This class collects diagnostic messages during type checking and generates a SARIF 2.1.0
 * compliant JSON report file. The report includes error and warning messages with their locations
 * in the source code.
 */
public class SarifReportGenerator {

  private final ProcessingEnvironment processingEnv;
  private final List<Result> results = new ArrayList<>();

  public SarifReportGenerator(ProcessingEnvironment processingEnv) {
    this.processingEnv = processingEnv;
  }

  /**
   * Converts the source file path to a file URI.
   *
   * @param root the compilation unit
   * @return the file URI as a string, or "file:///unknown" if conversion fails
   */
  private String getFileUri(CompilationUnitTree root) {
    try {
      return root.getSourceFile().toUri().toString();
    } catch (IllegalArgumentException | SecurityException e) {
      return "file:///unknown";
    }
  }

  /**
   * Extracts the source code region (line and column numbers) for a tree node.
   *
   * @param source the tree node
   * @param root the compilation unit containing the tree
   * @return a Region with start and end line/column numbers, or default values if position cannot
   *     be determined
   */
  private Region getRegion(Tree source, CompilationUnitTree root) {
    Trees trees = Trees.instance(processingEnv);
    SourcePositions sourcePositions = trees.getSourcePositions();

    long startPos = sourcePositions.getStartPosition(root, source);
    long endPos = sourcePositions.getEndPosition(root, source);

    if (startPos == Diagnostic.NOPOS || endPos == Diagnostic.NOPOS) {
      return new Region().withStartLine(1).withStartColumn(1).withEndLine(1).withEndColumn(1);
    }

    LineMap lineMap = root.getLineMap();
    long startLine = lineMap.getLineNumber(startPos);
    long startCol = lineMap.getColumnNumber(startPos);
    long endLine = lineMap.getLineNumber(endPos);
    long endCol = lineMap.getColumnNumber(endPos);

    return new Region()
        .withStartLine((int) startLine)
        .withStartColumn((int) startCol)
        .withEndLine((int) endLine)
        .withEndColumn((int) endCol);
  }

  /**
   * Add a diagnostic result to the report.
   *
   * <p>Only ERROR and MANDATORY_WARNING diagnostics are collected. Other diagnostic kinds (NOTE,
   * etc.) are ignored.
   *
   * @param kind the diagnostic kind
   * @param message the message text
   * @param messageKey the message key (rule ID)
   * @param source the source tree node where the diagnostic was reported
   * @param root the compilation unit containing the source
   */
  public void addResult(
      javax.tools.Diagnostic.Kind kind,
      String message,
      String messageKey,
      Tree source,
      CompilationUnitTree root) {
    // Only collect ERROR and WARNING diagnostics
    if (kind != javax.tools.Diagnostic.Kind.ERROR
        && kind != javax.tools.Diagnostic.Kind.MANDATORY_WARNING) {
      return;
    }
    String fileUri = getFileUri(root);

    Region region = getRegion(source, root);

    Result result =
        new Result()
            .withRuleId(messageKey)
            .withLevel(kind == javax.tools.Diagnostic.Kind.ERROR ? Level.ERROR : Level.WARNING)
            .withMessage(new Message().withText(message))
            .withLocations(
                Collections.singletonList(
                    new Location()
                        .withPhysicalLocation(
                            new PhysicalLocation()
                                .withArtifactLocation(new ArtifactLocation().withUri(fileUri))
                                .withRegion(region))));
    results.add(result);
  }

  /**
   * Returns the Checker Framework version from git.properties resource file.
   *
   * <p>If the version cannot be read from git.properties, returns a default fallback version.
   *
   * @return the Checker Framework version string
   */
  private String getCheckerVersion() {
    try (InputStream in = getClass().getResourceAsStream("/git.properties")) {
      if (in != null) {
        Properties gitProperties = new Properties();
        gitProperties.load(in);
        String version = gitProperties.getProperty("git.build.version");
        if (version != null && !version.isEmpty()) {
          return version;
        }
      }
    } catch (IOException e) {
      // Fall through to return default version
    }
    return "Unknown";
  }

  /**
   * Write the SARIF report to file.
   *
   * @param outputPath the output file path
   */
  public void writeReport(String outputPath) throws IOException {

    SarifSchema210 sarifLog =
        new SarifSchema210()
            .withVersion(SarifSchema210.Version._2_1_0)
            .withRuns(
                Collections.singletonList(
                    new Run()
                        .withTool(
                            new Tool()
                                .withDriver(
                                    new ToolComponent()
                                        .withName("Checker Framework")
                                        .withVersion(getCheckerVersion())))
                        .withResults(results)));

    // Write SARIF log to JSON file
    ObjectMapper mapper = new ObjectMapper();
    Path path = Paths.get(outputPath);
    mapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), sarifLog);
  }
}
