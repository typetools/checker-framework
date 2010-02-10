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
 * <p>
 *
 * If a method parameter is annotated with {@code @Nullable}, then passing
 * {@code null} as an argument should not cause the method to throw an
 * exception, including a {@code NullPointerException}.  A similar argument
 * applies to public fields that are annotated with {@code @Nullable}.
 * <p>
 *
 * This annotation is associated with the {@link NullnessChecker}.
 *
 * @see NonNull
 * @see NullnessChecker
 * @checker.framework.manual #nullness-checker Nullness Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
//@Target({FIELD, LOCAL_VARIABLE, METHOD, PARAMETER, TYPE})
@TypeQualifier
@SubtypeOf({})
@ImplicitFor(trees={Tree.Kind.NULL_LITERAL})
public @interface Nullable {

}
