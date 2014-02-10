package checkers.propkey.quals;

import java.lang.annotation.*;

import checkers.quals.DefaultQualifierInHierarchy;
import checkers.quals.SubtypeOf;
import checkers.quals.TypeQualifier;

/**
 * Indicates that the {@code String} type has an unknown
 * property key property.
 *
 * @checker_framework_manual #propkey-checker Property File Checker
 */
@TypeQualifier
@SubtypeOf({})
@DefaultQualifierInHierarchy
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface UnknownPropertyKey {}
