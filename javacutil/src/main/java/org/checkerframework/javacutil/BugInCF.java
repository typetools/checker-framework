package org.checkerframework.javacutil;

import com.sun.source.tree.Tree;
import org.checkerframework.checker.formatter.qual.FormatMethod;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Exception type indicating a bug in the framework.
 *
 * <p>To indicate a bug in a checker implementation, use {@link TypeSystemError}. To indicate that
 * an end user made a mistake, use {@link UserError}.
 */
@SuppressWarnings("serial")
public class BugInCF extends RuntimeException {

  /** Tree at which to report the bug. */
  private @Nullable Tree location;

  /**
   * Constructs a new BugInCF with the specified detail message and no cause (use this at the root
   * cause).
   *
   * @param message the detail message
   */
  public BugInCF(String message) {
    this(message, new Throwable());
  }

  /**
   * Constructs a new BugInCF with a detail message composed from the given arguments, and with no
   * cause (use the current callstack as the root cause).
   *
   * @param fmt the format string
   * @param args the arguments for the format string
   */
  @FormatMethod
  public BugInCF(String fmt, @Nullable Object... args) {
    this(String.format(fmt, args), new Throwable());
  }

  /**
   * Constructs a new BugInCF with the specified cause.
   *
   * @param cause the cause; its detail message will be used and must be non-null
   */
  public BugInCF(Throwable cause) {
    this((Tree) null, cause);
  }

  /**
   * Constructs a new BugInCF with the specified cause.
   *
   * @param location where to report the bug
   * @param cause the cause; its detail message will be used and must be non-null
   */
  public BugInCF(@Nullable Tree location, Throwable cause) {
    this(
        location,
        cause.getMessage() != null ? cause.getMessage() : cause.getClass().getSimpleName(),
        cause);
  }

  /**
   * Constructs a new BugInCF with the specified cause and with a detail message composed from the
   * given arguments.
   *
   * @param cause the cause
   * @param fmt the format string
   * @param args the arguments for the format string
   */
  @FormatMethod
  public BugInCF(Throwable cause, String fmt, @Nullable Object... args) {
    this(String.format(fmt, args), cause);
  }

  /**
   * Constructs a new BugInCF with the specified detail message and cause.
   *
   * @param message the detail message
   * @param cause the cause
   */
  public BugInCF(String message, Throwable cause) {
    this(null, message, cause);
  }

  /**
   * Constructs a new BugInCF with the specified detail message and cause.
   *
   * @param location where to report the bug
   * @param message the detail message
   * @param cause the cause
   */
  public BugInCF(@Nullable Tree location, String message, Throwable cause) {
    super(message, cause);
    if (cause instanceof BugInCF && ((BugInCF) cause).getLocation() != null) {
      this.location = ((BugInCF) cause).getLocation();
    } else {
      this.location = location;
    }
    if (message == null) {
      throw new BugInCF("Must have a detail message.");
    }
    if (cause == null) {
      throw new BugInCF("Must have a cause throwable.");
    }
  }

  /**
   * Returns the tree at which to report the exception.
   *
   * @return the tree at which to report the exception
   */
  public @Nullable Tree getLocation() {
    return location;
  }

  /**
   * Adds the location to {@code throwable}. If {@code throwable} is a {@code BugInCF} that does not
   * have a location, then its location is set to {@code location}.
   *
   * <p>If {@code throwable} is not a{@code BugInCF}, then a {@code BugInCF} is created with {@code
   * location} and cause {@code throwable}.
   *
   * @param location the location at which to report this bug
   * @param throwable a throwable whose location is set to {@code location} if it does not already
   *     have a location
   * @return {@code throwable} if its a {@code BugInCF} otherwise a new {@code BugInCF} object
   */
  public static BugInCF addLocation(Tree location, Throwable throwable) {
    if (throwable instanceof BugInCF) {
      BugInCF bugInCF = (BugInCF) throwable;
      if (bugInCF.location == null) {
        bugInCF.location = location;
      }
      return bugInCF;
    }
    return new BugInCF(location, throwable);
  }
}
