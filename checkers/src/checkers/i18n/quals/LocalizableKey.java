package checkers.i18n.quals;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import checkers.quals.SubtypeOf;
import checkers.quals.TypeQualifier;
import checkers.quals.Unqualified;

/**
 * Indicates that the {@Code String} type has been localized and
 * formatted for the target output locale
 */
@TypeQualifier
@SubtypeOf(Unqualified.class)
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface LocalizableKey { }
