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
 * Indicates that the annotated type behaves as the most restrictive of
 * {@link ReadOnly} and {@link Mutable}: only {@link Mutable} can be assigned
 * to it, and it can only be assigned to {@link ReadOnly}.
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
//@Target({FIELD, LOCAL_VARIABLE, METHOD, PARAMETER, TYPE})
@TypeQualifier
@SubtypeOf(ReadOnly.class)
public @interface QReadOnly {

}
