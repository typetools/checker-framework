package org.checkerframework.checker.nullness.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.PreconditionAnnotation;

// TODO: In a fix for https://tinyurl.com/cfissue/1917, add the text:  Every prefix expression must
// also be non-null; for example, {@code @RequiresNonNull(expression="a.b.c")} implies that both
// {@code a.b} and {@code a.b.c} must be non-null.
/**
 * Indicates a method precondition: the method expects the specified expressions to be non-null when
 * the annotated method is invoked.
 *
 * <p>For example:
 * <!-- The "&nbsp;" is to hide the at-signs from Javadoc. -->
 *
 * <pre>
 * import org.checkerframework.checker.nullness.qual.RequiresNonNull;
 * import org.checkerframework.checker.nullness.qual.NonNull;
 * import org.checkerframework.checker.nullness.qual.Nullable;
 *
 * class MyClass {
 * &nbsp; @Nullable Object field1;
 * &nbsp; @Nullable Object field2;
 *
 * &nbsp; @RequiresNonNull({"field1", "#1.field1"})
 *   void method1(@NonNull MyClass other) {
 *     field1.toString();           // OK, this.field1 is known to be non-null
 *     field2.toString();           // error, might throw NullPointerException
 *     other.field1.toString();     // OK, other.field1 is known to be non-null
 *     other.field2.toString();     // error, might throw NullPointerException
 *   }
 *
 *   void method2() {
 *     MyClass other = new MyClass();
 *
 *     field1 = new Object();
 *     other.field1 = new Object();
 *     method1(other);                   // OK, satisfies method precondition
 *
 *     field1 = null;
 *     other.field1 = new Object();
 *     method1(other);                   // error, does not satisfy this.field1 method precondition
 *
 *     field1 = new Object();
 *     other.field1 = null;
 *     method1(other);                   // error, does not satisfy other.field1 method precondition
 *   }
 * }
 * </pre>
 *
 * Do not use this annotation for formal parameters (instead, give them a {@code @NonNull} type,
 * which is the default and need not be written). The {@code @RequiresNonNull} annotation is
 * intended for non-parameter, such as field accesses or method calls.
 *
 * @checker_framework.manual #nullness-checker Nullness Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
@Repeatable(RequiresNonNull.List.class)
@PreconditionAnnotation(qualifier = NonNull.class)
public @interface RequiresNonNull {
  /**
   * The Java expressions that need to be {@link
   * org.checkerframework.checker.nullness.qual.NonNull}.
   *
   * @return the Java expressions that need to be {@link
   *     org.checkerframework.checker.nullness.qual.NonNull}
   * @checker_framework.manual #java-expressions-as-arguments Syntax of Java expressions
   */
  String[] value();

  /**
   * A wrapper annotation that makes the {@link RequiresNonNull} annotation repeatable.
   *
   * <p>Programmers generally do not need to write this. It is created by Java when a programmer
   * writes more than one {@link RequiresNonNull} annotation at the same location.
   */
  @Documented
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
  @PreconditionAnnotation(qualifier = NonNull.class)
  public static @interface List {
    /**
     * Returns the repeatable annotations.
     *
     * @return the repeatable annotations
     */
    RequiresNonNull[] value();
  }
}
