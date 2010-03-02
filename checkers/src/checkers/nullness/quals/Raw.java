package checkers.nullness.quals;

import java.lang.annotation.*;

import checkers.nullness.NullnessChecker;
import checkers.quals.*;

/**
 * A method receiver annotation that indicates that non-null fields might be
 * null within the body of the method, e.g., if {@code this} is {@code Raw},
 * {@code this.field} might be null even if {@code field} was declared to be
 * {@link NonNull}.
 *
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
//@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@TypeQualifier
@SubtypeOf( {} )
public @interface Raw {
    //Class<?> upTo() default Object.class;
}
