package checkers.javari.quals;

import java.lang.annotation.*;

import checkers.javari.JavariChecker;
import checkers.quals.SubtypeOf;
import checkers.quals.TypeQualifier;

/**
 * Indicates that, for the variable on which this annotation appears,
 * the object to which this variable refers can be modified via this
 * reference, except its fields explicitly marked as {@link ReadOnly}.
 *
 * <p>
 *
 * This annotation is part of the Javari language.
 *
 * @see ReadOnly
 * @see JavariChecker
 * @checker.framework.manual #javari-checker Javari Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@TypeQualifier
@SubtypeOf({ThisMutable.class, QReadOnly.class})
public @interface Mutable {}
