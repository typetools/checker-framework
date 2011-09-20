package checkers.javari.quals;

import java.lang.annotation.*;

import checkers.javari.JavariChecker;

import static java.lang.annotation.ElementType.*;

/**
 * Indicates that a field is assignable, even if it is inside a {@link ReadOnly}
 * instance.
 *
 * <p>
 *
 * This annotation is part of the Javari language.
 *
 * @see JavariChecker
 * @checker.framework.manual #javari-checker Javari Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({FIELD})
public @interface Assignable {

}
