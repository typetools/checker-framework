package org.checkerframework.checker.lock.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.ConditionalPostconditionAnnotation;
import org.checkerframework.framework.qual.InheritedAnnotation;

/**
 * Indicates that the given expressions are held if the method terminates successfully and returns
 * the given result (either true or false).
 *
 * @see EnsuresLockHeld
 * @checker_framework.manual #lock-checker Lock Checker
 * @checker_framework.manual #ensureslockheld-examples Example use of @EnsuresLockHeldIf
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
@ConditionalPostconditionAnnotation(qualifier = LockHeld.class)
@InheritedAnnotation
@Repeatable(EnsuresLockHeldIf.List.class)
public @interface EnsuresLockHeldIf {
  /**
   * Returns the return value of the method under which the postconditions hold.
   *
   * @return the return value of the method under which the postconditions hold
   */
  boolean result();

  /**
   * Returns Java expressions whose values are locks that are held after the method returns the
   * given result.
   *
   * @return Java expressions whose values are locks that are held after the method returns the
   *     given result
   * @see <a href="https://checkerframework.org/manual/#java-expressions-as-arguments">Syntax of
   *     Java expressions</a>
   */
  // It would be clearer for users if this field were named "lock".
  // However, method ContractsFromMethod.getConditionalPostconditions in the CF implementation
  // assumes that conditional postconditions have a field named "expression".
  String[] expression();

  /**
   * A wrapper annotation that makes the {@link EnsuresLockHeldIf} annotation repeatable.
   *
   * <p>Programmers generally do not need to write this. It is created by Java when a programmer
   * writes more than one {@link EnsuresLockHeldIf} annotation at the same location.
   */
  @Documented
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
  @ConditionalPostconditionAnnotation(qualifier = LockHeld.class)
  @InheritedAnnotation
  public static @interface List {
    /**
     * Return the repeatable annotations.
     *
     * @return the repeatable annotations
     */
    EnsuresLockHeldIf[] value();
  }
}
