package checkers.nullness.quals;

import java.lang.annotation.*;

/**
 * Indicates a method precondition:  the method expects the specified
 * variables (typically field references) to be non-null when the annotated
 * method is invoked.
 *
 * <p>For example:
 * <!-- The "&nbsp;" is to hide the at-signs from Javadoc. -->
 * <pre>
 * &nbsp;@Nullable Object field1;
 * &nbsp;@Nullable Object field2;
 *
 * &nbsp;@NonNullOnEntry("field1")
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
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
public @interface NonNullOnEntry {
    /**
     * Java expression(s) that are non-null when the method is entered --
     * that is, when it is invoked.
     * @see <a href="http://types.cs.washington.edu/checker-framework/current/checkers-manual.html#java-expressions-as-arguments">Syntax of Java expressions</a>
     */
    String[] value();
}
