package org.checkerframework.framework.test.diagnostics;

import java.util.LinkedHashMap;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;

/** The kinds of errors that can be encountered during typechecking. */
public enum DiagnosticKind {
  /** A warning. */
  Warning("warning"),
  /** An error. */
  Error("error"),
  /** A JSpecify diagnostic. */
  JSpecify("jspecify"),
  /** Something else. */
  Other("other");

  /** How this DiagnosticKind appears in error messages or source code. */
  public final String parseString;

  DiagnosticKind(String parseString) {
    this.parseString = parseString;
  }

  private static final Map<String, DiagnosticKind> stringToCategory = new LinkedHashMap<>();

  static {
    for (DiagnosticKind cat : values()) {
      stringToCategory.put(cat.parseString, cat);
    }
  }

  /** Convert a string as it would appear in error messages or source code into a DiagnosticKind. */
  public static @Nullable DiagnosticKind fromParseString(String parseStr) {
    return stringToCategory.get(parseStr);
  }
}
