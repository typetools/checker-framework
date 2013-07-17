package checkers.i18n.quals;

import checkers.quals.ImplicitFor;
import checkers.quals.SubtypeOf;
import checkers.quals.TypeQualifier;
import checkers.quals.Unqualified;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.sun.source.tree.Tree.Kind;

/**
 * Indicates that the {@code String} type has been localized and
 * formatted for the target output locale.
 */
@TypeQualifier
@SubtypeOf(Unqualified.class)
@ImplicitFor( trees = {
        /* All integer literals */
        Kind.INT_LITERAL,
        Kind.LONG_LITERAL,
        Kind.FLOAT_LITERAL,
        Kind.DOUBLE_LITERAL,
        Kind.BOOLEAN_LITERAL,

        /* null should be the bottom type */
        Kind.NULL_LITERAL

        //CHAR_LITERAL,
        //STRING_LITERAL,
    }
)
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface Localized {}
