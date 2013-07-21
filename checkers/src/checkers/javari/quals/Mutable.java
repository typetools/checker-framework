package checkers.javari.quals;

import checkers.javari.JavariChecker;
import checkers.quals.DefaultQualifierInHierarchy;
import checkers.quals.SubtypeOf;
import checkers.quals.TypeQualifier;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

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
@TypeQualifier
@SubtypeOf({ThisMutable.class, QReadOnly.class})
@DefaultQualifierInHierarchy
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface Mutable {}
