package org.checkerframework.framework.test.diagnostics;

import java.util.List;
import org.checkerframework.dataflow.qual.SideEffectFree;

/** Represents a list of TestDiagnostics, which were read from one line of a file. */
public class TestDiagnosticLine {
  /** The name of the file that the line was read from. */
  private final String filename;

  /** The 1-based number of the line within the file. */
  private final long lineNumber;

  /** The text of the line, as read from the file. */
  private final String originalLine;

  /** The diagnostics that were read from the line. */
  private final List<TestDiagnostic> diagnostics;

  /**
   * Creates a TestDiagnosticLine.
   *
   * @param filename the name of the file that the line was read from
   * @param lineNumber the number of the line within the file
   * @param originalLine the text of the line, as read from the file
   * @param diagnostics the diagnostics that were read from the line
   */
  @SideEffectFree
  public TestDiagnosticLine(
      String filename, long lineNumber, String originalLine, List<TestDiagnostic> diagnostics) {
    this.filename = filename;
    this.lineNumber = lineNumber;
    this.originalLine = originalLine;
    this.diagnostics = diagnostics;
  }

  public String getFilename() {
    return filename;
  }

  public boolean hasDiagnostics() {
    return !diagnostics.isEmpty();
  }

  public long getLineNumber() {
    return lineNumber;
  }

  public String getOriginalLine() {
    return originalLine;
  }

  public List<TestDiagnostic> getDiagnostics() {
    return diagnostics;
  }
}
