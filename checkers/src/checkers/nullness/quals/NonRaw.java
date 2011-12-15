package checkers.nullness.quals;

import java.lang.annotation.*;

import checkers.nullness.NullnessChecker;
import checkers.quals.*;

/**
 * An annotation that indicates a type that contains an initialized object
 * &mdash; that is, the object's constructor has completed, either in full
 * or in part.
 *
 * <p>
 *
 * When no argument is given, as in {@code @NonRaw}, then the object is
 * fully initialized; this is the default, so there is little need for a
 * programmer to write this explicitly.
 *
 * <p>
 *
 * When an argument is given, as in {@code @NonRaw(MyClass.class)}, then
 * the object's {@code MyClass} constructor has completed.
 * All {@code @NonNull} fields declared in {@code MyClass} or in any of its
 * superclasses have a non-{@code null} value.
 * Thus, when a constructor in class {@code C} completes, {@code this} has
 * type {@code @NonRaw(C.class) C}.
 *
 * <p>
 *
 * This annotation is associated with the {@link NullnessChecker}.
 *
 * @see Raw
 * @see NonNull
 * @see NullnessChecker
 * @checker.framework.manual #nullness-checker Nullness Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@TypeQualifier
@DefaultQualifierInHierarchy
@SubtypeOf(Raw.class)
public @interface NonRaw {
  // TODO: implement this
  // Class<?> upFrom() default Object.class;
}
