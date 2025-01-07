package org.checkerframework.checker.calledmethods.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Indicates that the method, if it terminates successfully, always invokes the given methods on all
 * of the arguments passed in the varargs position.
 *
 * <p>Consider the following method:
 *
 * <pre>
 * &#64;EnsuresCalledMethodsVarargs("m")
 * public void callMOnAll(S s, T t...) { ... }
 * </pre>
 *
 * <p>This method guarantees that {@code m()} is always called on every {@code T} object passed in
 * the {@code t} varargs argument before the method returns.
 *
 * <p>This annotation is not checked. An error will always be issued when it is used.
 */
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
public @interface EnsuresCalledMethodsVarargs {

  /**
   * Returns the methods guaranteed to be invoked on the varargs parameters.
   *
   * @return the methods guaranteed to be invoked on the varargs parameters
   */
  String[] value();
}
