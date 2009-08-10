package checkers.nullness.quals;

import static java.lang.annotation.ElementType.*;

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
//@Target({FIELD, LOCAL_VARIABLE, METHOD, PARAMETER, TYPE})
@TypeQualifier
@DefaultQualifierInHierarchy
@SubtypeOf( Raw.class )
public @interface NonRaw {
}
