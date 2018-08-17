package org.checkerframework.checker.i18n.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.ImplicitFor;
import org.checkerframework.framework.qual.LiteralKind;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * Indicates that the {@code String} type has been localized and formatted for the target output
 * locale.
 *
 * @checker_framework.manual #i18n-checker Internationalization Checker
 */
@SubtypeOf(UnknownLocalized.class)
@ImplicitFor(
        literals = {
            /* All integer literals */
            LiteralKind.INT,
            LiteralKind.LONG,
            LiteralKind.FLOAT,
            LiteralKind.DOUBLE,
            LiteralKind.BOOLEAN,

            /* null should be the bottom type */
            LiteralKind.NULL

            // CHAR_LITERAL,
            // STRING_LITERAL,
        })
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface Localized {}
