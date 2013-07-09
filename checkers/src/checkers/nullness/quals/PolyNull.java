package checkers.nullness.quals;

import java.lang.annotation.*;

import checkers.quals.*;

/**
 * A polymorphic qualifier for the non-null type system.
 *
 * <p>
 * Any method written using {@link PolyNull} conceptually has two versions: one
 * in which every instance of {@link PolyNull} has been replaced by
 * {@link NonNull}, and one in which every instance of {@link PolyNull} has been
 * replaced by {@link Nullable}.
 */
@Documented
@TypeQualifier
@PolymorphicQualifier(Nullable.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE_USE, ElementType.TYPE_PARAMETER })
public @interface PolyNull {
}
