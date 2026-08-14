package org.checkerframework.framework.type;

import org.checkerframework.checker.formatter.qual.FormatMethod;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.javacutil.BugInCF;

/**
 * Thrown when the target type of a lambda expression or a method reference is not a functional
 * interface.
 *
 * <p>Usually that indicates a bug, which is why this class extends {@link BugInCF}. During Java 8
 * type argument inference it is expected, though: the target type may be an inference variable that
 * has not been resolved yet. {@link
 * org.checkerframework.framework.util.typeinference8.InvocationTypeInference} catches this
 * exception and defers the work that requires the target type.
 */
@SuppressWarnings("serial")
public class NotFunctionalInterfaceException extends BugInCF {

  /**
   * Constructs a new {@code NotFunctionalInterfaceException} with a detail message composed from
   * the given arguments.
   *
   * @param fmt the format string
   * @param args the arguments for the format string
   */
  @FormatMethod
  public NotFunctionalInterfaceException(String fmt, @Nullable Object... args) {
    super(fmt, args);
  }
}
