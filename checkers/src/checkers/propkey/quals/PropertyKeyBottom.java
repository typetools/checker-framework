package checkers.propkey.quals;

import java.lang.annotation.*;

import checkers.quals.DefaultQualifierInHierarchy;
import checkers.quals.SubtypeOf;
import checkers.quals.TypeQualifier;

/**
 * The bottom qualifier.
 *
 * @checker_framework_manual #propkey-checker Property File Checker
 */
@TypeQualifier
@SubtypeOf(PropertyKey.class)
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface PropertyKeyBottom {}
