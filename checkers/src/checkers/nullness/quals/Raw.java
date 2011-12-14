package checkers.nullness.quals;

import java.lang.annotation.*;

import checkers.nullness.NullnessChecker;
import checkers.quals.*;

/**
 * An annotation that indicates that the object may not be fully initialized.
 * In particular, non-null fields might be
 * null within the body of the method, e.g., if {@code this} is {@code Raw},
 * {@code this.field} might be null even if {@code field} was declared to be
 * {@link NonNull}.  This can happen because the code where the initializer
 * sets {@code field} has not yet been executed.
 *
 * <p>
 *
 * Suppose the {@code @Raw} annotation is placed on type {@code T}.  Then
 * fields declared in {@code T} and any subclasses might not yet be
 * initialized, but fields declared in superclasses of {@code T} have
 * already been initialized.
 * 
 * <p>
 *
 * This annotation is associated with the {@link NullnessChecker}.
 *
 * @see NonRaw
 * @see NonNull
 * @see NullnessChecker
 * @checker.framework.manual #nullness-checker Nullness Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@TypeQualifier
@SubtypeOf({})
public @interface Raw {}
