package checkers.nullness.quals;

import java.lang.annotation.*;

import checkers.nullness.NullnessChecker;
import checkers.quals.SubtypeOf;
import checkers.quals.TypeQualifier;

/**
 * Indicates that a field is lazily initialized to a non-null value.  Once
 * the field becomes non-null, it never becomes {@code null} again.  There
 * is no guarantee that the field ever becomes non-null, however.
 * <p>
 *
 * Lazily initialized fields have these two properties:
 * <ol>
 * <li>The field may be assigned only non-null values.</li>
 * <li>The field may be re-assigned as often as desired.</li>
 *
 * </ol>
 *
 * When the field is first read within a method, the field cannot be assumed
 * to be non-null.  The benefit of LazyNonNull over Nullable is its
 * different interaction with flow-sensitive type qualifier refinement.
 * After a check of a LazyNonNull field, all subsequent accesses <em>within
 * that method</em> can be assumed to be NonNull, even after arbitrary
 * external method calls that have access to the given field.
 * <p>
 *
 * Note that LazyNonNull is a field declaration annotation, not a type annotation.
 * Final fields are treated as LazyNonNull by default.
 * <p>
 *
 * This annotation is associated with the {@link NullnessChecker}.
 *
 * @see Nullable
 * @see NullnessChecker
 * @checker.framework.manual #nullness-checker Nullness Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@TypeQualifier
@SubtypeOf( Nullable.class )
public @interface LazyNonNull {

}
