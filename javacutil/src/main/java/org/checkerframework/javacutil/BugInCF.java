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

  @FormatMethod
  public BugInCF(Tree location, String fmt, @Nullable Object... args) {
    this(location, String.format(fmt, args), new Throwable());
  }

  /**
   * Constructs a new BugInCF with the specified cause.
   *
   * @param cause the cause; its detail message will be used and must be non-null
   */
  @SuppressWarnings("nullness")
  public BugInCF(Throwable cause) {
    this((Tree) null, cause);
  }

  public BugInCF(Tree location, Throwable cause) {
    this(location, cause.getMessage(), cause);
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

  @FormatMethod
  public BugInCF(Tree location, Throwable cause, String fmt, @Nullable Object... args) {
    this(location, String.format(fmt, args), cause);
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
   * @param message the detail message
   * @param cause the cause
   */
  public BugInCF(Tree location, String message, Throwable cause) {
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

  public Tree getLocation() {
    return location;
  }

  public BugInCF setLocation(Tree location) {
    if (this.location == null) {
      this.location = location;
    }
    return this;
  }

  public static BugInCF addLocation(Throwable throwable, Tree location) {
    if (throwable instanceof BugInCF) {
      return ((BugInCF) throwable).setLocation(location);
    }
    return new BugInCF(location, throwable);
  }
}
