package checkers.javari.quals;

import java.lang.annotation.*;

import checkers.javari.JavariChecker;
import checkers.quals.SubtypeOf;
import checkers.quals.TypeQualifier;

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
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@TypeQualifier
@SubtypeOf(ReadOnly.class)
public @interface QReadOnly {}
