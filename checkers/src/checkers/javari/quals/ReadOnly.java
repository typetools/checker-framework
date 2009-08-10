package checkers.javari.quals;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import checkers.javari.JavariChecker;
import checkers.quals.SubtypeOf;
import checkers.quals.TypeQualifier;
import static java.lang.annotation.ElementType.*;

/**
 * Indicates that, for the variable on which this annotation appears,
 * the object to which this variable refers will not be modified via
 * this reference, except its fields explicitly marked as
 * {@link Mutable}.
 *
 * <p>
 *
 * This annotation is part of the Javari language.
 *
 * @see Mutable
 * @see JavariChecker
 * @checker.framework.manual #javari-checker Javari Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
//@Target({FIELD, LOCAL_VARIABLE, METHOD, PARAMETER, TYPE})
@TypeQualifier
@SubtypeOf({})
public @interface ReadOnly {

}
