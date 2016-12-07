package org.checkerframework.checker.nullness.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.PreconditionAnnotation;

/**
 * Indicates a method precondition: the method expects the specified expressions to be non-null when
 * the annotated method is invoked.
 *
 * <p>For example:
 * <!-- The "&nbsp;" is to hide the at-signs from Javadoc. -->
 *
 * <pre>
 * &nbsp;@Nullable Object field1;
 * &nbsp;@Nullable Object field2;
 *
 * &nbsp;@RequiresNonNull("field1")
 *  void method1() {
 *    field1.toString();        // OK, field1 is known to be non-null
 *    field2.toString();        // error, might throw NullPointerException
 *  }
 *
 *  void method2() {
 *    field1 = new Object();
 *    method1();                // OK, satisfies method precondition
 *    field1 = null;
 *    method1();                // error, does not satisfy method precondition
 *  }
 * </pre>
 *
 * @checker_framework.manual #nullness-checker Nullness Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
@PreconditionAnnotation(qualifier = NonNull.class)
public @interface RequiresNonNull {
    /**
     * The Java expressions that need to be {@link
     * org.checkerframework.checker.nullness.qual.NonNull}.
     *
     * @checker_framework.manual #java-expressions-as-arguments Syntax of Java expressions
     */
    String[] value();
}
