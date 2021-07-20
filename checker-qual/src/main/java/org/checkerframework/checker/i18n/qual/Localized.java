package org.checkerframework.checker.i18n.qual;

import org.checkerframework.framework.qual.LiteralKind;
import org.checkerframework.framework.qual.QualifierForLiterals;
import org.checkerframework.framework.qual.SubtypeOf;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the {@code String} type has been localized and formatted for the target output
 * locale.
 *
 * @checker_framework.manual #i18n-checker Internationalization Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf(UnknownLocalized.class)
@QualifierForLiterals({
    // All literals except chars and strings, which may need to be localized.
    // (null is bottom by default.)
    LiteralKind.INT,
    LiteralKind.LONG,
    LiteralKind.FLOAT,
    LiteralKind.DOUBLE,
    LiteralKind.BOOLEAN
})
public @interface Localized {}
