package checkers.initialization.quals;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import checkers.nullness.NullnessChecker;
import checkers.nullness.quals.NonNull;
import checkers.quals.DefaultQualifierInHierarchy;
import checkers.quals.SubtypeOf;
import checkers.quals.TypeQualifier;

/**
 * This type qualifier belongs to the freedom-before-commitment initialization
 * tracking type-system. This type-system is not used on its own, but in
 * conjunction with some other type-system that wants to ensure safe
 * initialization. For instance, {@link NullnessChecker} uses
 * freedom-before-commitment to track initialization of {@link NonNull} fields.
 *
 * <p>
 * This type qualifier indicates that the object has been fully initialized;
 * reading fields from such objects is fully safe and yields objects of the
 * correct type.
 */
@Documented
@TypeQualifier
@DefaultQualifierInHierarchy
@SubtypeOf(UnkownInitialization.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE_USE, ElementType.TYPE_PARAMETER })
public @interface Initialized {
}
