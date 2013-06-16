package checkers.formatter.quals;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Attach this annotation to a method with the following properties:
 * - The first parameter is a format string.
 * - The second parameter is a vararg that takes conversion categories.
 * - The method throws an exception if the format string's
 *   format specifiers do not match the passed conversion categories.
 * - On success, the method returns the passed format string unmodifed.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ReturnsFormat {}