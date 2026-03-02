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

  /** The file to which the diagnostic applies. */
  private final String filename;

  /** The line number to which the diagnostic applies. */
  private final long lineNumber;

  /** The kind of diagnostic. */
  private final DiagnosticKind kind;

  /**
   * An error key or full error message that usually appears between square brackets in diagnostic
   * messages.
   */
  private final String key;

  /** The full error message, without the key. Null if the key is the whole message. */
  private final @Nullable String message;

  /** True if this diagnostic should no longer be reported after whole-program inference. */
  private final boolean isFixable;

  /**
   * Basic constructor that sets the immutable fields of this diagnostic.
   *
   * @param filename the file to which the diagnostic applies
   * @param lineNumber the line number to which the diagnostic applies
   * @param kind kind of diagnostic
   * @param key an error key or full error message
   * @param message the full error message, without the key; null if the key is the whole message
   * @param isFixable true if this diagnostic should no longer be reported after whole-program
   *     inference
   */
  public TestDiagnostic(
      String filename,
      long lineNumber,
      DiagnosticKind kind,
      String key,
      @Nullable String message,
      boolean isFixable) {
    this.filename = filename;
    this.lineNumber = lineNumber;
    this.kind = kind;
    this.key = key;
    this.message = message;
    this.isFixable = isFixable;
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

  /**
   * Returns true if this diagnostic should no longer be reported after whole-program inference.
   *
   * @return true if this diagnostic should no longer be reported after whole-program inference
   */
  public boolean isFixable() {
    return isFixable;
  }

  /**
   * Equality is compared without fields {@code message} and {@code isFixable}.
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
    String key = "(" + this.key + ")";
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
