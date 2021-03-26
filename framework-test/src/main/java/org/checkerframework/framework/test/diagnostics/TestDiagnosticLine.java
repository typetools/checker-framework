package org.checkerframework.framework.test.diagnostics;

import java.util.List;

/** Represents a list of TestDiagnostics, which was read from a one line of a file. */
public class TestDiagnosticLine {
  private final String filename;
  private final long lineNumber;
  private final String originalLine;
  private final List<TestDiagnostic> diagnostics;

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
