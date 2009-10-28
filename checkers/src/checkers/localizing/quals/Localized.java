package checkers.localizing.quals;

import static com.sun.source.tree.Tree.Kind.*;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import checkers.quals.*;

/**
 * Indicates that the {@Code String} type has been localized and
 * formatted for the target output locale
 */
@TypeQualifier
@SubtypeOf(Unqualified.class)
@ImplicitFor( trees = {
        /* All integer literals */
        INT_LITERAL,
        LONG_LITERAL,
        FLOAT_LITERAL,
        DOUBLE_LITERAL,
        BOOLEAN_LITERAL,

        //CHAR_LITERAL,
        //STRING_LITERAL,
        //NULL_LITERAL
        }
)
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface Localized { }
