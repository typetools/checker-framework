package org.checkerframework.checker.optional.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.PreconditionAnnotation;

/**
 * Indicates a method precondition: the specified expressions of type Optional must be present
 * (i.e., non-empty) when the annotated method is invoked.
 *
 * <p>For example:
 *
 * <pre>
 * import java.util.Optional;
 *
 * import org.checkerframework.checker.optional.qual.RequiresPresent;
 * import org.checkerframework.checker.optional.qual.Present;
 *
 * class MyClass {
 *   Optional&lt;String&gt; optId1;
 *   Optional&lt;String&gt; optId2;
 *
 * &nbsp; @RequiresPresent({"optId1", "#1.optId1"})
 *   void method1(MyClass other) {
 *     optId1.get().length()       // OK, this.optID1 is known to be present.
 *     optId2.get().length()       // error, might throw NoSuchElementException.
 *
 *     other.optId1.get().length() // OK, this.optID1 is known to be present.
 *     other.optId2.get().length() // error, might throw NoSuchElementException.
 *   }
 *
 *   void method2() {
 *     MyClass other = new MyClass();
 *
 *     optId1 = Optional.of("abc");
 *     other.optId1 = Optional.of("def")
 *     method1(other);                       // OK, satisfies method precondition.
 *
 *     optId1 = Optional.empty();
 *     other.optId1 = Optional.empty("abc");
 *     method1(other);                       // error, does not satisfy this.optId1 method precondition.
 *
 *     optId1 = Optional.empty("abc");
 *     other.optId1 = Optional.empty();
 *     method1(other);                       // error. does not satisfy other.optId1 method precondition.
 *   }
 * }
 * </pre>
 *
 * Do not use this annotation for formal parameters (instead, give them a {@code @Present} type).
 * The {@code @RequiresNonNull} annotation is intended for non-parameter expressions, such as field
 * accesses or method calls.
 *
 * @checker_framework.manual #optional-checker Optional Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
@PreconditionAnnotation(qualifier = Present.class)
public @interface RequiresPresent {

  /**
   * The Java expressions that that need to be {@link Present}.
   *
   * @return the Java expressions that need to be {@link Present}
   * @checker_framework.manual #java-expressions-as-arguments Syntax of Java expressions
   */
  String[] value();

  /**
   * A wrapper annotation that makes the {@link RequiresPresent} annotation repeatable.
   *
   * <p>Programmers generally do not need to write this. It is created by Java when a programmer
   * writes more than one {@link RequiresPresent} annotation at the same location.
   */
  @Documented
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
  @PreconditionAnnotation(qualifier = Present.class)
  public static @interface List {
    /**
     * Returns the repeatable annotations.
     *
     * @return the repeatable annotations
     */
    RequiresPresent[] value();
  }
}
