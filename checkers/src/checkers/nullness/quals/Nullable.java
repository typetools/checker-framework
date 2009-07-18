package checkers.nullness.quals;

import static java.lang.annotation.ElementType.*;

import java.lang.annotation.*;

import checkers.nullness.NullnessChecker;
import checkers.quals.ImplicitFor;
import checkers.quals.SubtypeOf;
import checkers.quals.TypeQualifier;

import com.sun.source.tree.Tree;


/**
 * Indicates that a variable may have a null value.
 *
 * <p>
 *
 * This annotation is associated with the {@link NullnessChecker}.
 *
 * @see NonNull
 * @see NullnessChecker
 * @manual #nullness-checker Nullness Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
//@Target({FIELD, LOCAL_VARIABLE, METHOD, PARAMETER, TYPE})
@TypeQualifier
@SubtypeOf({})
@ImplicitFor(trees={Tree.Kind.NULL_LITERAL})
public @interface Nullable {

}
