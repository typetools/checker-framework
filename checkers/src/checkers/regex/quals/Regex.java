package checkers.regex.quals;

import java.lang.annotation.*;

import com.sun.source.tree.Tree;

import checkers.quals.*;

/**
 * For char, char[], {@link Character} and subtypes of {@link CharSequence}
 * indicates a valid regular expression and holds the number of groups in
 * the regular expression.
 */
@Documented
@TypeQualifier
@Inherited
@ImplicitFor(trees={Tree.Kind.NULL_LITERAL})
@SubtypeOf(Unqualified.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface Regex {
  
    /**
     * The number of groups in the regular expression.
     * Defaults to 0.
     */
    int value() default 0;
}
