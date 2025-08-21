package org.checkerframework.framework.util;

// TODO: Should this be in package org.checkerframework.dataflow.expression?

import java.util.Arrays;
import javax.tools.Diagnostic;
import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.source.DiagMessage;
import org.checkerframework.javacutil.BugInCF;

/**
 * An exception that indicates a parse error. Call {@link #getDiagMessage} to obtain a {@link
 * DiagMessage} that can be used for error reporting.
 */
public class JavaExpressionParseException extends Exception {
  /** The serial version identifier. */
  private static final long serialVersionUID = 2L;

  /** The error message key. */
  private final @CompilerMessageKey String errorKey;

  /** The arguments to the error message key. */
  @SuppressWarnings("serial") // I do not intend to serialize JavaExpressionParseException objects
  public final Object[] args;

  /**
   * Create a new JavaExpressionParseException.
   *
   * @param errorKey the error message key
   * @param args the arguments to the error message key
   */
  public JavaExpressionParseException(@CompilerMessageKey String errorKey, Object... args) {
    this(null, errorKey, args);
  }

  /**
   * Create a new JavaExpressionParseException.
   *
   * @param cause cause
   * @param errorKey the error message key
   * @param args the arguments to the error message key
   */
  public JavaExpressionParseException(
      @Nullable Throwable cause, @CompilerMessageKey String errorKey, Object... args) {
    super(cause);
    this.errorKey = errorKey;
    this.args = args;
  }

  /**
   * Returns a {@link JavaExpressionParseException} with error key "flowexpr.parse.error" for the
   * expression {@code exprString} with explanation {@code explanation}.
   *
   * @param exprString the string that could not be parsed
   * @param explanation an explanation of the parse failure
   * @return a {@link JavaExpressionParseException} for the expression {@code exprString} with
   *     explanation {@code explanation}
   */
  public static JavaExpressionParseException construct(String exprString, String explanation) {
    if (exprString == null) {
      throw new BugInCF("Must have an expression.");
    }
    if (explanation == null) {
      throw new BugInCF("Must have an explanation.");
    }
    return new JavaExpressionParseException(
        (Throwable) null,
        "flowexpr.parse.error",
        "Invalid '" + exprString + "' because " + explanation);
  }

  @Override
  public String getMessage() {
    return errorKey + " " + Arrays.toString(args);
  }

  /**
   * Returns a DiagMessage that can be used for error reporting.
   *
   * @return a DiagMessage that can be used for error reporting
   */
  public DiagMessage getDiagMessage() {
    return new DiagMessage(Diagnostic.Kind.ERROR, errorKey, args);
  }

  /**
   * Returns true if this is a flowexpr.parse.error.
   *
   * @return true if this is a flowexpr.parse.error
   */
  public boolean isFlowParseError() {
    return errorKey.startsWith("flowexpr.parse.error");
  }

  @Override
  public String toString() {
    Throwable cause = getCause();
    if (cause == null) {
      return String.format("JavaExpressionParseException([null cause]: %s)", getMessage());
    } else {
      return String.format(
          "JavaExpressionParseException(%s [%s]: %s)",
          cause.toString(), cause.getClass(), getMessage());
    }
  }

  /**
   * The unchecked exception equivalent of checked exception {@link JavaExpressionParseException}.
   */
  static class JavaExpressionParseExceptionUnchecked extends RuntimeException {
    private static final long serialVersionUID = 2L;
    private final JavaExpressionParseException exception;

    public JavaExpressionParseExceptionUnchecked(JavaExpressionParseException exception) {
      this.exception = exception;
    }

    public JavaExpressionParseException getCheckedException() {
      return exception;
    }

    @Override
    public String getMessage() {
      return "JavaExpressionParseException(" + exception + ")";
    }
  }
}
