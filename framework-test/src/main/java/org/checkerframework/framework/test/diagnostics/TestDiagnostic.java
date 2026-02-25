package org.checkerframework.framework.test.diagnostics;

import java.util.Objects;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Represents an expected error/warning message in a Java test file or an error/warning reported by
 * the Javac compiler. By contrast, {@link TestDiagnosticLine} represents a set of TestDiagnostics,
 * all of which were read from the same line of a file.
 *
 * @see JavaDiagnosticReader
 */
public class TestDiagnostic {

  private final String filename;
  private final long lineNumber;
  private final DiagnosticKind kind;

  /**
   * An error key or full error message that usually appears between parentheses in diagnostic
   * messages.
   */
  private final String key;

  /** The full error message, without the key. Null if the key is the whole message. */
  private final @Nullable String message;

  /** Returns true if this diagnostic should no longer be reported after whole program inference. */
  private final boolean isFixable;

  /** True if the toString representation should omit the parentheses around the message key. */
  private final boolean omitParentheses;

  /** Basic constructor that sets the immutable fields of this diagnostic. */
  public TestDiagnostic(
      String filename,
      long lineNumber,
      DiagnosticKind kind,
      String key,
      @Nullable String message,
      boolean isFixable,
      boolean omitParentheses) {
    this.filename = filename;
    this.lineNumber = lineNumber;
    this.kind = kind;
    this.key = key;
    this.message = message;
    this.isFixable = isFixable;
    this.omitParentheses = omitParentheses;
  }

  public String getFilename() {
    return filename;
  }

  public long getLineNumber() {
    return lineNumber;
  }

  public DiagnosticKind getKind() {
    return kind;
  }

  /**
   * Returns the error message key.
   *
   * @return the error message key
   */
  public String getKey() {
    return key;
  }

  /**
   * Returns the full error message, without the key.
   *
   * @return the full error message, without the key
   */
  public @Nullable String getMessage() {
    return message;
  }

  public boolean isFixable() {
    return isFixable;
  }

  /**
   * Returns true if the printed representation should omit parentheses around the message key.
   *
   * @return true if the printed representation should omit parentheses around the message key
   */
  public boolean shouldOmitParentheses() {
    return omitParentheses;
  }

  /**
   * Equality is compared without fields {@code message}, {@code isFixable}, and {@code
   * omitParentheses}.
   *
   * @return true if this and otherObj are equal according to filename, lineNumber, kind, and
   *     message key
   */
  @Override
  public boolean equals(@Nullable Object otherObj) {
    if (otherObj == null || otherObj.getClass() != TestDiagnostic.class) {
      return false;
    }

    TestDiagnostic other = (TestDiagnostic) otherObj;
    return other.filename.equals(this.filename)
        && other.lineNumber == lineNumber
        && other.kind == this.kind
        && other.key.equals(this.key);
  }

  @Override
  public int hashCode() {
    return Objects.hash(filename, lineNumber, kind, key);
  }

  /**
   * Returns a representation of this diagnostic as if it appeared in a diagnostics file.
   *
   * @return a representation of this diagnostic as if it appeared in a diagnostics file
   */
  @Override
  public String toString() {
    String loc = filename + ":" + lineNumber + ": ";
    String key = omitParentheses ? this.key : "(" + this.key + ")";
    String msg = message == null ? "" : " " + message;
    if (kind == DiagnosticKind.JSpecify) {
      return loc + key;
    } else {
      return loc + kind.parseString + ": " + key + msg;
    }
  }

  /**
   * Returns the internal representation of this, formatted.
   *
   * @return the internal representation of this, formatted
   */
  public String repr() {
    return String.format(
        "[TestDiagnostic: filename=%s, lineNumber=%d, kind=%s, key=%s]",
        filename, lineNumber, kind, key);
  }
}
