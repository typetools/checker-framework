package checkers.nullness.quals;

import java.lang.annotation.*;

import checkers.nullness.NullnessChecker;
import checkers.quals.ImplicitFor;
import checkers.quals.SubtypeOf;
import checkers.quals.TypeQualifier;

import com.sun.source.tree.Tree;


/**
 * {@code @Nullable} is a type annotation that indicates that the value is
 * not known to be non-null (see {@link NonNull}).  Another perspective is
 * that if a type is annotated with {@code Nullable}, then it can be
 * legal/expected for a value of that type to be null.
 * <p>
 *
 * For example, if a method parameter's type is annotated with
 * {@code @Nullable}, then passing {@code null} as an argument should not
 * by itself cause the method to throw an exception, including a
 * {@code NullPointerException}.  Similarly, if a field's type is
 * {@code @Nullable}, then setting it to null should not by itself cause a
 * run-time exception.
 * <p>
 *
 * No more than one of {@link Nullable} and {@code NonNull} may be
 * written on a given type.
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
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@TypeQualifier
@SubtypeOf({})
@ImplicitFor(trees={Tree.Kind.NULL_LITERAL})
public @interface Nullable {}
