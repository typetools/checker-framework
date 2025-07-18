package org.checkerframework.checker.nonempty.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.PreconditionAnnotation;

/**
 * Indicates a method precondition: the specified expressions that may be a {@link
 * java.util.Collection collection}, {@link java.util.Iterator iterator}, {@link java.lang.Iterable
 * iterable}, or {@link java.util.Map map} must be non-empty when the annotated method is invoked.
 *
 * <p>For example:
 *
 * <pre>
 * import java.util.LinkedList;
 * import java.util.List;
 * import org.checkerframework.checker.nonempty.qual.NonEmpty;
 * import org.checkerframework.checker.nonempty.qual.RequiresNonEmpty;
 * import org.checkerframework.dataflow.qual.Pure;
 *
 * class MyClass {
 *
 *   List&lt;String&gt; list1 = new LinkedList&lt;&gt;();
 *   List&lt;String&gt; list2;
 *
 * &nbsp; @RequiresNonEmpty("list1")
 * &nbsp; @Pure
 *   void m1() {}
 *
 * &nbsp; @RequiresNonEmpty({"list1", "list2"})
 * &nbsp; @Pure
 *   void m2() {}
 *
 * &nbsp; @RequiresNonEmpty({"list1", "list2"})
 *   void m3() {}
 *
 *   void m4() {}
 *
 *   void test(@NonEmpty List&lt;String&gt; l1, @NonEmpty List&lt;String&gt; l2) {
 *     MyClass testClass = new MyClass();
 *
 *     testClass.m1(); // Compile-time error: m1 requires that list1 is @NonEmpty.
 *
 *     testClass.list1 = l1;
 *     testClass.m1(); // OK
 *
 *     testClass.m2(); // Compile-time error: m2 requires that list2 is @NonEmpty
 *
 *     testClass.list2 = l2;
 *     testClass.m2(); // OK
 *
 *     testClass.m4();
 *
 *     testClass.m2(); // Compile-time error: m4 is not pure and might have assigned a field.
 *   }
 * }
 * </pre>
 *
 * This annotation should not be used for formal parameters (instead, give them a {@code @NonEmpty}
 * type). The {@code @RequiresNonEmpty} annotation is intended for non-parameter expressions, such
 * as field accesses or method calls.
 *
 * @checker_framework.manual #non-empty-checker Non-Empty Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.PARAMETER})
@PreconditionAnnotation(qualifier = NonEmpty.class)
public @interface RequiresNonEmpty {

  /**
   * The Java {@link java.util.Collection collection}, {@link java.util.Iterator iterator}, {@link
   * java.lang.Iterable iterable}, or {@link java.util.Map map} that must be non-empty.
   *
   * @return the Java expression that must be non-empty
   */
  String[] value();

  /**
   * A wrapper annotation that makes the {@link RequiresNonEmpty} annotation repeatable.
   *
   * <p>Programmers generally do not need to write this. It is created by Java when a programmer
   * writes more than one {@link RequiresNonEmpty} annotation at the same location.
   */
  @interface List {
    /**
     * Returns the repeatable annotations.
     *
     * @return the repeatable annotations
     */
    RequiresNonEmpty[] value();
  }
}
