package org.checkerframework.framework.report;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.LineMap;
import com.sun.source.tree.Tree;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.Trees;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
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
 *
 * <p>The JSON is written directly, rather than via a JSON library, so that checker.jar does not
 * need to bundle such a library.
 */
public class SarifReportGenerator {

  /** The processing environment, used to obtain source positions of trees. */
  private final ProcessingEnvironment processingEnv;

  /**
   * The SARIF results collected so far, one per reported diagnostic. Each is a SARIF {@code result}
   * object, represented as described at {@link #appendJson}.
   */
  private final List<Map<String, Object>> results = new ArrayList<>();

  /**
   * Creates a new SarifReportGenerator.
   *
   * @param processingEnv the processing environment
   */
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
      Diagnostic.Kind kind,
      String message,
      String messageKey,
      Tree source,
      CompilationUnitTree root) {
    if (kind != Diagnostic.Kind.ERROR && kind != Diagnostic.Kind.MANDATORY_WARNING) {
      return;
    }

    SourcePositions sourcePositions = Trees.instance(processingEnv).getSourcePositions();
    long startPos = sourcePositions.getStartPosition(root, source);
    long endPos = sourcePositions.getEndPosition(root, source);

    int startLine = 1;
    int startColumn = 1;
    int endLine = 1;
    int endColumn = 1;
    if (startPos != Diagnostic.NOPOS && endPos != Diagnostic.NOPOS) {
      LineMap lineMap = root.getLineMap();
      startLine = (int) lineMap.getLineNumber(startPos);
      startColumn = (int) lineMap.getColumnNumber(startPos);
      endLine = (int) lineMap.getLineNumber(endPos);
      endColumn = (int) lineMap.getColumnNumber(endPos);
    }

    addResult(
        kind, message, messageKey, getFileUri(root), startLine, startColumn, endLine, endColumn);
  }

  /**
   * Add a diagnostic result to the report, given its location as a file URI and a region.
   *
   * <p>Only ERROR and MANDATORY_WARNING diagnostics are collected. Other diagnostic kinds (NOTE,
   * etc.) are ignored.
   *
   * @param kind the diagnostic kind
   * @param message the message text
   * @param messageKey the message key (rule ID)
   * @param fileUri the URI of the file containing the diagnostic
   * @param startLine the 1-based line on which the region starts
   * @param startColumn the 1-based column at which the region starts
   * @param endLine the 1-based line on which the region ends
   * @param endColumn the 1-based column at which the region ends
   */
  /*package-private*/ void addResult(
      Diagnostic.Kind kind,
      String message,
      String messageKey,
      String fileUri,
      int startLine,
      int startColumn,
      int endLine,
      int endColumn) {
    if (kind != Diagnostic.Kind.ERROR && kind != Diagnostic.Kind.MANDATORY_WARNING) {
      return;
    }

    Map<String, Object> region = new LinkedHashMap<>();
    region.put("startLine", startLine);
    region.put("startColumn", startColumn);
    region.put("endLine", endLine);
    region.put("endColumn", endColumn);

    Map<String, Object> physicalLocation = new LinkedHashMap<>();
    physicalLocation.put("artifactLocation", Collections.singletonMap("uri", fileUri));
    physicalLocation.put("region", region);

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("ruleId", messageKey);
    result.put("level", kind == Diagnostic.Kind.ERROR ? "error" : "warning");
    result.put("message", Collections.singletonMap("text", message));
    result.put(
        "locations",
        Collections.singletonList(Collections.singletonMap("physicalLocation", physicalLocation)));
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
   * Write the SARIF report to file, in UTF-8.
   *
   * @param outputPath the output file path
   * @throws IOException if the file cannot be written
   */
  public void writeReport(String outputPath) throws IOException {
    Files.write(
        Paths.get(outputPath), toJson(getCheckerVersion()).getBytes(StandardCharsets.UTF_8));
  }

  /**
   * Returns the SARIF report as pretty-printed JSON text.
   *
   * @param checkerVersion the Checker Framework version to record in the report
   * @return the SARIF report as JSON text, ending with a newline
   */
  /*package-private*/ String toJson(String checkerVersion) {
    Map<String, Object> driver = new LinkedHashMap<>();
    driver.put("name", "Checker Framework");
    driver.put("version", checkerVersion);

    Map<String, Object> run = new LinkedHashMap<>();
    run.put("tool", Collections.singletonMap("driver", driver));
    run.put("results", results);

    Map<String, Object> sarifLog = new LinkedHashMap<>();
    sarifLog.put("version", "2.1.0");
    sarifLog.put("runs", Collections.singletonList(run));

    StringBuilder sb = new StringBuilder();
    appendJson(sb, sarifLog, 0);
    sb.append('\n');
    return sb.toString();
  }

  /**
   * Appends the JSON representation of a value to {@code sb}, pretty-printed with 2-space
   * indentation.
   *
   * @param sb where to append the JSON text
   * @param value the value to append: a {@code Map<String, ?>} (a JSON object, whose entries are
   *     written in iteration order), a {@code List<?>} (a JSON array), a {@code String}, a {@code
   *     Number}, or a {@code Boolean}
   * @param indent the indentation level of the line on which {@code value} starts
   */
  private static void appendJson(StringBuilder sb, Object value, int indent) {
    if (value instanceof Map<?, ?> map) {
      if (map.isEmpty()) {
        sb.append("{}");
        return;
      }
      sb.append('{');
      String separator = "\n";
      for (Map.Entry<?, ?> entry : map.entrySet()) {
        sb.append(separator);
        sb.append("  ".repeat(indent + 1));
        appendJsonString(sb, (String) entry.getKey());
        sb.append(": ");
        appendJson(sb, entry.getValue(), indent + 1);
        separator = ",\n";
      }
      sb.append('\n');
      sb.append("  ".repeat(indent));
      sb.append('}');
    } else if (value instanceof List<?> list) {
      if (list.isEmpty()) {
        sb.append("[]");
        return;
      }
      sb.append('[');
      String separator = "\n";
      for (Object element : list) {
        sb.append(separator);
        sb.append("  ".repeat(indent + 1));
        appendJson(sb, element, indent + 1);
        separator = ",\n";
      }
      sb.append('\n');
      sb.append("  ".repeat(indent));
      sb.append(']');
    } else if (value instanceof String string) {
      appendJsonString(sb, string);
    } else if (value instanceof Number || value instanceof Boolean) {
      sb.append(value);
    } else {
      throw new IllegalArgumentException("Cannot convert to JSON: " + value);
    }
  }

  /**
   * Appends a string to {@code sb} as a JSON string literal, quoted and escaped per RFC 8259.
   * Non-ASCII characters are appended as-is.
   *
   * @param sb where to append the JSON string literal
   * @param s the string to append
   */
  private static void appendJsonString(StringBuilder sb, String s) {
    sb.append('"');
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      switch (c) {
        case '"' -> sb.append("\\\"");
        case '\\' -> sb.append("\\\\");
        case '\b' -> sb.append("\\b");
        case '\f' -> sb.append("\\f");
        case '\n' -> sb.append("\\n");
        case '\r' -> sb.append("\\r");
        case '\t' -> sb.append("\\t");
        default -> {
          if (c < 0x20) {
            sb.append(String.format("\\u%04x", (int) c));
          } else {
            sb.append(c);
          }
        }
      }
    }
    sb.append('"');
  }
}
