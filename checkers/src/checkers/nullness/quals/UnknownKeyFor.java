package checkers.nullness.quals;

import java.lang.annotation.*;

import checkers.quals.*;

/**
 * TODO: document that this is the top type for the KeyFor system.
 *
 * @checker_framework_manual #nullness-checker Nullness Checker
 */
@TypeQualifier
@InvisibleQualifier
@SubtypeOf({})
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface UnknownKeyFor {}
