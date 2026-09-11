package org.checkerframework.javacutil;

import org.checkerframework.checker.formatter.qual.FormatMethod;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Exception type indicating a mistake by an end user in using the Checker Framework, such as
 * incorrect command-line arguments.
 *
 * <p>To indicate a bug in the framework, use {@link BugInCF}. To indicate a bug in a checker
 * implementation, use {@link TypeSystemError}.
 */
@SuppressWarnings("serial")
public class UserError extends RuntimeException {

  /**
   * Constructs a new CheckerError with the specified detail message.
   *
   * @param message the detail message
   */
  public UserError(String message) {
    super(message);
    if (message == null) {
      throw new BugInCF("Must have a detail message.");
    }
  }

  /**
   * Constructs a new CheckerError with a detail message composed from the given arguments.
   *
   * <p>Beware: if the only argument is a {@code Throwable}, then Java instead selects {@link
   * #UserError(String, Throwable)}, which uses {@code fmt} literally rather than as a format
   * string, and uses the throwable as the cause. To format a throwable into the detail message,
   * pass it as an explicit cause too, as in {@code new UserError(t, "Cannot read %s", t)}, or pass
   * {@code t.getMessage()} as the format argument.
   *
   * @param fmt the format string
   * @param args the arguments for the format string
   */
  @FormatMethod
  public UserError(String fmt, @Nullable Object... args) {
    this(String.format(fmt, args));
  }

  /**
   * Constructs a new CheckerError with the specified detail message and cause.
   *
   * @param message the detail message
   * @param cause the cause
   */
  public UserError(String message, Throwable cause) {
    super(message, cause);
    if (message == null) {
      throw new BugInCF("Must have a detail message.");
    }
    if (cause == null) {
      throw new BugInCF("Must have a cause throwable.");
    }
  }

  /**
   * Constructs a new CheckerError with the specified cause and with a detail message composed from
   * the given arguments.
   *
   * @param cause the cause
   * @param fmt the format string
   * @param args the arguments for the format string
   */
  @FormatMethod
  public UserError(Throwable cause, String fmt, @Nullable Object... args) {
    this(String.format(fmt, args), cause);
  }
}
